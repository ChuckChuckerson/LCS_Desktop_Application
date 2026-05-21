package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.Case;
import com.lawyercompany.entity.Client;
import com.lawyercompany.entity.Lawyer;
import com.lawyercompany.entity.User;
import com.lawyercompany.service.*;
import com.lawyercompany.up.api.NotificationApiClient;
import com.lawyercompany.up.api.UpHttpServer;
import com.lawyercompany.up.dto.NotificationDTO;
import com.lawyercompany.up.util.BCryptPasswordEncoder;
import com.lawyercompany.up.util.InputValidators;
import com.lawyercompany.up.util.PasswordEncoder;
import com.lawyercompany.up.util.Toast;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LawyerDashboardController {

    @FXML
    private TextField caseSearchField;
    @FXML
    private TableView<Case> casesTable;
    @FXML
    private TableColumn<Case, String> caseNumberColumn;
    @FXML
    private TableColumn<Case, String> caseTitleColumn;
    @FXML
    private TableColumn<Case, String> clientNameColumn;
    @FXML
    private TableColumn<Case, String> caseStatusColumn;
    @FXML
    private TableColumn<Case, String> createdAtColumn;
    @FXML
    private TableColumn<Case, Button> actionColumn;
    @FXML
    private TabPane dashboardTabPane;
    @FXML
    private TextField profileUsernameField;
    @FXML
    private TextField profileFullNameField;
    @FXML
    private TextField profileEmailField;
    @FXML
    private TextField profilePhoneField;
    @FXML
    private TextField profileSpecializationField;
    @FXML
    private TextField profileLicenseField;
    @FXML
    private TextField profileOfficeField;
    @FXML
    private TextField profileExperienceField;
    @FXML
    private PasswordField profileCurrentPasswordField;
    @FXML
    private PasswordField profileNewPasswordField;
    @FXML
    private PasswordField profileConfirmPasswordField;
    @FXML
    private TableView<NotificationDTO> notificationsTable;
    @FXML
    private TableColumn<NotificationDTO, String> notifTypeColumn;
    @FXML
    private TableColumn<NotificationDTO, String> notifDateColumn;
    @FXML
    private TableColumn<NotificationDTO, String> notifMessageColumn;
    @FXML
    private TableColumn<NotificationDTO, String> notifCaseColumn;

    private final CaseService caseService = new CaseService();
    private final ClientService clientService = new ClientService();
    private final UserService userService = new UserService();
    private final LawyerService lawyerService = new LawyerService();
    private User currentUser;
    private Lawyer lawyerProfile;
    private MainApp mainApp;

    private final NotificationApiClient notificationApiClient = new NotificationApiClient(UpHttpServer.localBaseUrl());
    private final ObservableList<NotificationDTO> notificationsList = FXCollections.observableArrayList();
    private static final int TAB_NOTIFICATIONS_INDEX = 2;

    private final ObservableList<Case> allCasesMaster = FXCollections.observableArrayList();
    private FilteredList<Case> filteredCases;
    private final Map<UUID, String> clientNameByClientId = new HashMap<>();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        //это главный экран адвоката: список дел и быстрый поиск, всё грузим в фоне
        filteredCases = new FilteredList<>(allCasesMaster, c -> true);
        casesTable.setItems(filteredCases);
        casesTable.setPlaceholder(new Label("Нет данных"));
        caseSearchField.textProperty().addListener((obs, prev, q) -> applyCaseSearchFilter(q));

        caseNumberColumn.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));
        caseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        caseStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        clientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(clientNameByClientId.getOrDefault(cellData.getValue().getClientId(), "Неизвестно")));
        createdAtColumn.setCellValueFactory(cellData -> {
            OffsetDateTime utcTime = cellData.getValue().getCreatedAt();
            if (utcTime != null) {
                OffsetDateTime localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                return new SimpleStringProperty(localTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            }
            return new SimpleStringProperty("");
        });
        actionColumn.getStyleClass().add("table-action-column");
        actionColumn.setMinWidth(108);
        actionColumn.setPrefWidth(120);
        actionColumn.setMaxWidth(140);
        actionColumn.setCellValueFactory(cellData -> {
            Button btn = new Button("Открыть");
            btn.getStyleClass().add("table-row-button");
            btn.setMinWidth(96);
            btn.setPrefWidth(112);
            btn.setMaxWidth(130);
            btn.setOnAction(e -> {
                try {
                    handleOpenCase(cellData.getValue());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        });

        casesTable.setRowFactory(tv -> {
            TableRow<Case> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    try {
                        handleOpenCase(row.getItem());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return row;
        });

        setupNotificationsTable();
        if (dashboardTabPane != null) {
            dashboardTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
                if (n != null && n.intValue() == TAB_NOTIFICATIONS_INDEX && currentUser != null) {
                    handleRefreshNotifications();
                }
            });
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.lawyerProfile = null;
        configureCasesTable();
        loadProfileFieldsFromUser();
        clearLawyerOnlyFields();
        loadCases();
        loadLawyerProfile();
    }

    private void configureCasesTable() {
        casesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        caseNumberColumn.setMinWidth(90);
        caseNumberColumn.setPrefWidth(120);
        caseTitleColumn.setMinWidth(120);
        caseTitleColumn.setPrefWidth(200);
        clientNameColumn.setMinWidth(110);
        clientNameColumn.setPrefWidth(170);
        caseStatusColumn.setMinWidth(90);
        caseStatusColumn.setPrefWidth(110);
        createdAtColumn.setMinWidth(130);
        createdAtColumn.setPrefWidth(150);
        createdAtColumn.setMaxWidth(180);
    }

    private void applyCaseSearchFilter(String query) {
        if (filteredCases == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            filteredCases.setPredicate(c -> true);
            casesTable.setPlaceholder(new Label("Нет данных"));
            return;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        filteredCases.setPredicate(c -> {
            if (contains(c.getCaseNumber(), q)) return true;
            if (contains(c.getTitle(), q)) return true;
            if (contains(c.getStatus(), q)) return true;
            String client = clientNameByClientId.getOrDefault(c.getClientId(), "");
            if (contains(client, q)) return true;
            return false;
        });
        casesTable.setPlaceholder(new Label("Ничего не найдено"));
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void loadCases() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        casesTable.setDisable(true);

        Task<CasesVM> task = new Task<>() {
            @Override
            protected CasesVM call() {
                //тут кроме дел вытягиваем имена клиентов, чтобы таблица выглядела нормально
                List<Case> cases = caseService.getCasesByLawyerId(currentUser.getId());
                Map<UUID, String> clientNames = new HashMap<>();
                for (Case c : cases) {
                    UUID clientId = c.getClientId();
                    if (clientId == null || clientNames.containsKey(clientId)) continue;
                    Optional<Client> clientOpt = clientService.getClientById(clientId);
                    String name = clientOpt
                            .map(Client::getUserId)
                            .flatMap(userService::getUserById)
                            .map(User::getFullName)
                            .orElse("Неизвестно");
                    clientNames.put(clientId, name);
                }
                return new CasesVM(cases, clientNames);
            }
        };
        task.setOnSucceeded(e -> {
            CasesVM vm = task.getValue();
            clientNameByClientId.clear();
            clientNameByClientId.putAll(vm.clientNames);
            allCasesMaster.setAll(vm.cases);
            applyCaseSearchFilter(caseSearchField.getText());
            casesTable.setDisable(false);
        });
        task.setOnFailed(e -> {
            casesTable.setDisable(false);
            showError("Ошибка загрузки дел: " + task.getException().getMessage());
        });
        new Thread(task, "load-lawyer-cases").start();
    }

    private void handleOpenCase(Case caseEntity) throws Exception {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        mainApp.showCaseWindow(caseEntity, currentUser);
    }

    @FXML
    private void handleLogout() {
        try {
            mainApp.showLoginWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        if (currentUser == null || lawyerProfile == null) {
            showProfileError("Профиль адвоката ещё не загружен");
            return;
        }
        String fullName = profileFullNameField != null ? profileFullNameField.getText() : "";
        String email = profileEmailField != null ? profileEmailField.getText() : "";
        if (fullName == null || fullName.isBlank()) {
            showProfileError("Укажите ФИО");
            return;
        }
        if (!InputValidators.isValidEmail(email)) {
            showProfileError("Укажите корректный email");
            return;
        }
        String newPw = profileNewPasswordField != null ? profileNewPasswordField.getText() : "";
        String confirmPw = profileConfirmPasswordField != null ? profileConfirmPasswordField.getText() : "";
        String curPw = profileCurrentPasswordField != null ? profileCurrentPasswordField.getText() : "";
        if (newPw != null && !newPw.isBlank()) {
            if (curPw == null || curPw.isBlank()) {
                showProfileError("Для смены пароля введите текущий пароль");
                return;
            }
            if (!newPw.equals(confirmPw)) {
                showProfileError("Новый пароль и повтор не совпадают");
                return;
            }
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            User fresh = userService.getUserById(currentUser.getId()).orElse(currentUser);
            if (!encoder.matches(curPw, fresh.getPasswordHash())) {
                showProfileError("Неверный текущий пароль");
                return;
            }
        }

        Integer expYears = null;
        String expText = profileExperienceField != null ? profileExperienceField.getText() : null;
        if (expText != null && !expText.isBlank()) {
            try {
                expYears = Integer.parseInt(expText.trim());
                if (expYears < 0) {
                    showProfileError("Стаж не может быть отрицательным");
                    return;
                }
            } catch (NumberFormatException ex) {
                showProfileError("Стаж укажите целым числом");
                return;
            }
        }

        String phone = profilePhoneField != null ? profilePhoneField.getText() : "";
        String spec = profileSpecializationField != null ? profileSpecializationField.getText() : "";
        String license = profileLicenseField != null ? profileLicenseField.getText() : "";
        String office = profileOfficeField != null ? profileOfficeField.getText() : "";
        final Integer experienceYearsToSave = expYears;
        final String newPasswordToSave = (newPw != null && !newPw.isBlank()) ? newPw : null;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                User u = userService.getUserById(currentUser.getId()).orElseThrow();
                u.setFullName(fullName.trim());
                u.setEmail(email.trim());
                if (newPasswordToSave != null) {
                    u.setPasswordHash(new BCryptPasswordEncoder().encode(newPasswordToSave));
                }
                userService.updateUser(u);

                lawyerProfile.setPhone(phone != null ? phone.trim() : "");
                lawyerProfile.setSpecialization(spec != null ? spec.trim() : "");
                lawyerProfile.setLicenseNumber(license != null ? license.trim() : "");
                lawyerProfile.setOfficeAddress(office != null ? office.trim() : "");
                lawyerProfile.setExperienceYears(experienceYearsToSave);
                lawyerService.updateLawyer(lawyerProfile);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            currentUser = userService.getUserById(currentUser.getId()).orElse(currentUser);
            loadProfileFieldsFromUser();
            clearPasswordFields();
            Toast.show(notificationsTable != null ? notificationsTable : casesTable, "Профиль сохранён", Toast.Type.SUCCESS);
        });
        task.setOnFailed(e -> showProfileError("Ошибка сохранения: " + task.getException().getMessage()));
        new Thread(task, "save-lawyer-profile").start();
    }

    @FXML
    private void handleRefreshNotifications() {
        if (currentUser == null) return;
        if (notificationsTable != null) {
            notificationsTable.setDisable(true);
        }
        Task<List<NotificationDTO>> task = new Task<>() {
            @Override
            protected List<NotificationDTO> call() throws Exception {
                return notificationApiClient.fetchNotifications(currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            notificationsList.setAll(task.getValue());
            if (notificationsTable != null) {
                notificationsTable.setDisable(false);
            }
        });
        task.setOnFailed(e -> {
            if (notificationsTable != null) {
                notificationsTable.setDisable(false);
            }
            showError("Уведомления: " + task.getException().getMessage());
        });
        new Thread(task, "load-notifications-lawyer").start();
    }

    private void loadLawyerProfile() {
        if (currentUser == null) return;
        Task<Optional<Lawyer>> task = new Task<>() {
            @Override
            protected Optional<Lawyer> call() {
                return lawyerService.getLawyerByUserId(currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Lawyer> opt = task.getValue();
            if (opt.isEmpty()) {
                showError("Профиль адвоката не найден");
                return;
            }
            lawyerProfile = opt.get();
            loadLawyerProfileFields();
        });
        task.setOnFailed(ev -> showError("Ошибка загрузки профиля: " + task.getException().getMessage()));
        new Thread(task, "load-lawyer-profile").start();
    }

    private void setupNotificationsTable() {
        if (notificationsTable == null) {
            return;
        }
        notifTypeColumn.setCellValueFactory(c -> {
            String t = c.getValue().getType();
            return new SimpleStringProperty(t != null ? t : "");
        });
        notifDateColumn.setCellValueFactory(c -> {
            if (c.getValue().getCreatedAt() == null) {
                return new SimpleStringProperty("");
            }
            var local = c.getValue().getCreatedAt().atZoneSameInstant(ZoneId.systemDefault());
            return new SimpleStringProperty(local.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        });
        notifMessageColumn.setCellValueFactory(c -> {
            String m = c.getValue().getMessage();
            return new SimpleStringProperty(m != null ? m : "");
        });
        notifCaseColumn.setCellValueFactory(c -> {
            if (c.getValue().getCaseId() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(c.getValue().getCaseId().toString());
        });
        notificationsTable.setItems(notificationsList);
        notificationsTable.setPlaceholder(new Label("Нет уведомлений — нажмите «Обновить»"));
    }

    private void loadProfileFieldsFromUser() {
        if (currentUser == null || profileUsernameField == null) {
            return;
        }
        profileUsernameField.setText(nullToEmpty(currentUser.getUsername()));
        profileFullNameField.setText(nullToEmpty(currentUser.getFullName()));
        profileEmailField.setText(nullToEmpty(currentUser.getEmail()));
    }

    private void loadLawyerProfileFields() {
        if (lawyerProfile == null || profilePhoneField == null) {
            return;
        }
        profilePhoneField.setText(nullToEmpty(lawyerProfile.getPhone()));
        profileSpecializationField.setText(nullToEmpty(lawyerProfile.getSpecialization()));
        profileLicenseField.setText(nullToEmpty(lawyerProfile.getLicenseNumber()));
        profileOfficeField.setText(nullToEmpty(lawyerProfile.getOfficeAddress()));
        if (lawyerProfile.getExperienceYears() != null) {
            profileExperienceField.setText(String.valueOf(lawyerProfile.getExperienceYears()));
        } else if (profileExperienceField != null) {
            profileExperienceField.clear();
        }
    }

    private void clearLawyerOnlyFields() {
        if (profilePhoneField != null) profilePhoneField.clear();
        if (profileSpecializationField != null) profileSpecializationField.clear();
        if (profileLicenseField != null) profileLicenseField.clear();
        if (profileOfficeField != null) profileOfficeField.clear();
        if (profileExperienceField != null) profileExperienceField.clear();
    }

    private void clearPasswordFields() {
        if (profileCurrentPasswordField != null) profileCurrentPasswordField.clear();
        if (profileNewPasswordField != null) profileNewPasswordField.clear();
        if (profileConfirmPasswordField != null) profileConfirmPasswordField.clear();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void showProfileError(String message) {
        Node anchor = profileFullNameField != null ? profileFullNameField : casesTable;
        Toast.show(anchor, message, Toast.Type.ERROR);
    }

    private void showError(String message) {
        Toast.show(casesTable, message, Toast.Type.ERROR);
    }

    private boolean refreshSelfOrLogoutIfBlocked() {
        if (currentUser == null) return true;
        User refreshed = userService.getUserById(currentUser.getId()).orElse(currentUser);
        currentUser = refreshed;
        if (refreshed.isBlocked()) {
            //это чтобы заблокированный пользователь не мог дальше ходить по окнам
            Toast.show(casesTable, "Ваш аккаунт заблокирован администратором. Выполните вход снова.", Toast.Type.INFO);
            try {
                if (mainApp != null) {
                    mainApp.showLoginWindow();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private static final class CasesVM {
        private final List<Case> cases;
        private final Map<UUID, String> clientNames;

        private CasesVM(List<Case> cases, Map<UUID, String> clientNames) {
            this.cases = cases;
            this.clientNames = clientNames;
        }
    }
}
