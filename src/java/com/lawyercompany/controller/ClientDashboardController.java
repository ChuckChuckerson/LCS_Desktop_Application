package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.*;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClientDashboardController {

    private static final String UNKNOWN_LAWYER = "Неизвестно";
    private static final String DATETIME_PATTERN = "dd.MM.yyyy HH:mm";

    @FXML
    private TilePane lawyersTilePane;
    @FXML
    private TextField caseSearchField;
    @FXML
    private TableView<Case> casesTable;
    @FXML
    private TableColumn<Case, String> caseNumberColumn;
    @FXML
    private TableColumn<Case, String> caseTitleColumn;
    @FXML
    private TableColumn<Case, String> caseStatusColumn;
    @FXML
    private TableColumn<Case, String> caseLawyerColumn;
    @FXML
    private TableColumn<Case, String> caseCreatedAtColumn;
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
    private TextField profileAddressField;
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

    private final LawyerService lawyerService = new LawyerService();
    private final CaseService caseService = new CaseService();
    private final ClientService clientService = new ClientService();
    private final UserService userService = new UserService();
    private User currentUser;
    private UUID clientId;
    private Client clientProfile;
    private MainApp mainApp;

    private final NotificationApiClient notificationApiClient = new NotificationApiClient(UpHttpServer.localBaseUrl());
    private final ObservableList<NotificationDTO> notificationsList = FXCollections.observableArrayList();
    private static final int TAB_NOTIFICATIONS_INDEX = 3;

    private final ObservableList<Case> allCasesMaster = FXCollections.observableArrayList();
    private FilteredList<Case> filteredCases;
    private final Map<UUID, String> lawyerNameByUserId = new HashMap<>();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        //это главный экран клиента: список дел + карточки адвокатов. данные грузим в фоне через Task
        filteredCases = new FilteredList<>(allCasesMaster, c -> true);
        casesTable.setItems(filteredCases);
        casesTable.setPlaceholder(new Label("Нет данных"));
        caseSearchField.textProperty().addListener((obs, prev, q) -> applyCaseSearchFilter(q));

        caseNumberColumn.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));
        caseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        caseStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        caseLawyerColumn.setCellValueFactory(cellData -> {
            UUID lawyerUserId = cellData.getValue().getLawyerId();
            return new SimpleStringProperty(lawyerNameByUserId.getOrDefault(lawyerUserId, UNKNOWN_LAWYER));
        });
        caseCreatedAtColumn.setCellValueFactory(cellData -> {
            OffsetDateTime utcTime = cellData.getValue().getCreatedAt();
            if (utcTime != null) {
                OffsetDateTime localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                return new SimpleStringProperty(localTime.format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
            }
            return new SimpleStringProperty("");
        });

        casesTable.setRowFactory(tv -> {
            TableRow<Case> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    //на всякий случай проверяем блокировку перед переходом в окно дела
                    if (!refreshSelfOrLogoutIfBlocked()) return;
                    Case selectedCase = row.getItem();
                    try {
                        if (mainApp != null) {
                            mainApp.showCaseWindow(selectedCase, currentUser);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Не удалось открыть окно дела", e);
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
        this.clientProfile = null;
        configureCasesTable();
        loadProfileFieldsFromUser();
        clearClientOnlyFields();
        loadClientId();
    }

    private void configureCasesTable() {
        casesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        caseNumberColumn.setMinWidth(90);
        caseNumberColumn.setPrefWidth(120);
        caseTitleColumn.setMinWidth(120);
        caseTitleColumn.setPrefWidth(220);
        caseStatusColumn.setMinWidth(90);
        caseStatusColumn.setPrefWidth(110);
        caseLawyerColumn.setMinWidth(120);
        caseLawyerColumn.setPrefWidth(180);
        caseCreatedAtColumn.setMinWidth(130);
        caseCreatedAtColumn.setPrefWidth(150);
        caseCreatedAtColumn.setMaxWidth(180);
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
        filteredCases.setPredicate(c ->
                contains(c.getCaseNumber(), q)
                        || contains(c.getTitle(), q)
                        || contains(c.getStatus(), q)
                        || contains(lawyerNameByUserId.getOrDefault(c.getLawyerId(), ""), q));
        casesTable.setPlaceholder(new Label("Ничего не найдено"));
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void loadClientId() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        Task<Optional<Client>> task = new Task<>() {
            @Override
            protected Optional<Client> call() {
                //идём в бд за client профилем
                return clientService.getClientByUserId(currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Client> clientOpt = task.getValue();
            if (clientOpt.isPresent()) {
                this.clientProfile = clientOpt.get();
                this.clientId = clientProfile.getId();
                loadClientProfileFields();
                loadLawyers();
                loadCases();
            } else {
                showError("Клиент не найден");
            }
        });
        task.setOnFailed(e -> showError("Ошибка загрузки клиента: " + task.getException().getMessage()));
        new Thread(task, "load-client-id").start();
    }

    private void loadLawyers() {
        Task<List<LawyerCardVM>> task = new Task<>() {
            @Override
            protected List<LawyerCardVM> call() {
                //тут собираем карточки адвокатов + подтягиваем аватар/фио
                List<Lawyer> lawyers = lawyerService.getAllLawyers();
                return lawyers.stream()
                        .map(lawyer -> {
                            String fullName = userService.getUserById(lawyer.getUserId())
                                    .map(User::getFullName)
                                    .orElse("Неизвестно");
                            byte[] avatarData = userService.getAvatar(lawyer.getUserId());
                            return new LawyerCardVM(lawyer, fullName, avatarData);
                        })
                        .toList();
            }
        };
        task.setOnSucceeded(e -> {
            lawyersTilePane.getChildren().clear();
            for (LawyerCardVM vm : task.getValue()) {
                lawyersTilePane.getChildren().add(createLawyerCard(vm.lawyer, vm.fullName, vm.avatarData));
            }
        });
        task.setOnFailed(e -> showError("Ошибка загрузки адвокатов: " + task.getException().getMessage()));
        new Thread(task, "load-lawyers").start();
    }

    private VBox createLawyerCard(Lawyer lawyer, String lawyerFullName, byte[] avatarData) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("lawyer-card");
        //это должно совпадать с prefTileWidth/prefTileHeight в client_dashboard.fxml, иначе tilepane будет странно переносить карточки
        card.setPrefWidth(220);
        card.setPrefHeight(260);

        double half = 48;
        Node avatarNode = createAvatarNode(lawyerFullName, avatarData, half);
        //это фиксирует проблему, когда рамка вокруг аватара растягивается на всю ширину карточки
        StackPane avatarWrap = new StackPane(avatarNode);
        avatarWrap.setAlignment(Pos.CENTER);
        double avatarSize = half * 2;
        avatarWrap.setMinSize(avatarSize, avatarSize);
        avatarWrap.setPrefSize(avatarSize, avatarSize);
        avatarWrap.setMaxSize(avatarSize, avatarSize);
        avatarWrap.getStyleClass().add("lawyer-avatar");

        Label nameLabel = new Label(lawyerFullName != null ? lawyerFullName : "Неизвестно");
        nameLabel.getStyleClass().add("lawyer-name");

        Label specLabel = new Label("Специализация: " + (lawyer.getSpecialization() != null ? lawyer.getSpecialization() : "не указана"));
        specLabel.getStyleClass().add("lawyer-meta");
        specLabel.setWrapText(true);
        specLabel.setMaxWidth(200);

        Label expLabel = new Label("Стаж: " + (lawyer.getExperienceYears() != null ? lawyer.getExperienceYears() : 0) + " лет");
        expLabel.getStyleClass().add("lawyer-meta");

        Button contactBtn = new Button("Связаться");
        contactBtn.getStyleClass().add("lawyer-contact-button");
        contactBtn.setOnAction(e -> {
            try {
                handleContactLawyer(lawyer);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        card.getChildren().addAll(avatarWrap, nameLabel, specLabel, expLabel, contactBtn);
        return card;
    }

    private void handleContactLawyer(Lawyer lawyer) throws Exception {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        if (mainApp != null) {
            mainApp.showCreateCaseWindow(currentUser, lawyer);
        }
    }

    private void loadCases() {
        if (clientId == null) return;
        if (!refreshSelfOrLogoutIfBlocked()) return;
        casesTable.setDisable(true);

        Task<CasesVM> task = new Task<>() {
            @Override
            protected CasesVM call() {
                //кейс-лист клиента + кэш имён адвокатов, чтобы потом красиво показать в таблице
                List<Case> cases = caseService.getCasesByClientId(clientId);
                Map<UUID, String> lawyerNames = new HashMap<>();
                for (Case c : cases) {
                    UUID lawyerUserId = c.getLawyerId();
                    if (lawyerUserId != null && !lawyerNames.containsKey(lawyerUserId)) {
                        String name = userService.getUserById(lawyerUserId)
                                .map(User::getFullName)
                                .orElse("Неизвестно");
                        lawyerNames.put(lawyerUserId, name);
                    }
                }
                return new CasesVM(cases, lawyerNames);
            }
        };
        task.setOnSucceeded(e -> {
            CasesVM vm = task.getValue();
            lawyerNameByUserId.clear();
            lawyerNameByUserId.putAll(vm.lawyerNames);
            allCasesMaster.setAll(vm.cases);
            applyCaseSearchFilter(caseSearchField.getText());
            casesTable.setDisable(false);
        });
        task.setOnFailed(e -> {
            casesTable.setDisable(false);
            showError("Ошибка загрузки дел: " + task.getException().getMessage());
        });
        new Thread(task, "load-client-cases").start();
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
        if (currentUser == null || clientProfile == null) {
            showProfileError("Профиль клиента ещё не загружен");
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

        String phone = profilePhoneField != null ? profilePhoneField.getText() : "";
        String address = profileAddressField != null ? profileAddressField.getText() : "";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                User u = userService.getUserById(currentUser.getId()).orElseThrow();
                u.setFullName(fullName.trim());
                u.setEmail(email.trim());
                if (newPw != null && !newPw.isBlank()) {
                    u.setPasswordHash(new BCryptPasswordEncoder().encode(newPw));
                }
                userService.updateUser(u);

                clientProfile.setPhone(phone != null ? phone.trim() : "");
                clientProfile.setAddress(address != null ? address.trim() : "");
                clientService.updateClient(clientProfile);
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
        new Thread(task, "save-client-profile").start();
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
        new Thread(task, "load-notifications").start();
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

    private void loadClientProfileFields() {
        if (clientProfile == null || profilePhoneField == null) {
            return;
        }
        profilePhoneField.setText(nullToEmpty(clientProfile.getPhone()));
        profileAddressField.setText(nullToEmpty(clientProfile.getAddress()));
    }

    private void clearClientOnlyFields() {
        if (profilePhoneField != null) {
            profilePhoneField.clear();
        }
        if (profileAddressField != null) {
            profileAddressField.clear();
        }
    }

    private void clearPasswordFields() {
        if (profileCurrentPasswordField != null) {
            profileCurrentPasswordField.clear();
        }
        if (profileNewPasswordField != null) {
            profileNewPasswordField.clear();
        }
        if (profileConfirmPasswordField != null) {
            profileConfirmPasswordField.clear();
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void showProfileError(String message) {
        Node anchor = profileFullNameField != null ? profileFullNameField : casesTable;
        Toast.show(anchor, message, Toast.Type.ERROR);
    }

    private void showError(String message) {
        Toast.show(casesTable != null ? casesTable : lawyersTilePane, message, Toast.Type.ERROR);
    }

    private boolean refreshSelfOrLogoutIfBlocked() {
        if (currentUser == null) return true;
        User refreshed = userService.getUserById(currentUser.getId()).orElse(currentUser);
        currentUser = refreshed;
        if (refreshed.isBlocked()) {
            Toast.show(casesTable != null ? casesTable : lawyersTilePane,
                    "Ваш аккаунт заблокирован администратором. Выполните вход снова.",
                    Toast.Type.INFO);
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

    private Node createAvatarNode(String fullName, byte[] avatarData, double halfSize) {
        double size = halfSize * 2;
        double arc = 16;

        if (avatarData != null && avatarData.length > 0) {
            try {
                ImageView imageView = new ImageView(new javafx.scene.image.Image(new ByteArrayInputStream(avatarData)));
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);

                Rectangle clip = new Rectangle(size, size);
                clip.setArcWidth(arc);
                clip.setArcHeight(arc);
                imageView.setClip(clip);
                return imageView;
            } catch (Exception ignored) {
                // fallback below
            }
        }

        String initials = getInitials(fullName);
        Rectangle rect = new Rectangle(size, size);
        rect.setArcWidth(arc);
        rect.setArcHeight(arc);
        rect.setFill(Color.web("#3F5E94"));

        Label label = new Label(initials);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        StackPane pane = new StackPane(rect, label);
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        return pane;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() == 2) break;
        }
        return !sb.isEmpty() ? sb.toString() : "?";
    }

    private static final class LawyerCardVM {
        private final Lawyer lawyer;
        private final String fullName;
        private final byte[] avatarData;

        private LawyerCardVM(Lawyer lawyer, String fullName, byte[] avatarData) {
            this.lawyer = lawyer;
            this.fullName = fullName;
            this.avatarData = avatarData;
        }
    }

    private static final class CasesVM {
        private final List<Case> cases;
        private final Map<UUID, String> lawyerNames;

        private CasesVM(List<Case> cases, Map<UUID, String> lawyerNames) {
            this.cases = cases;
            this.lawyerNames = lawyerNames;
        }
    }
}
