package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.User;
import com.lawyercompany.entity.AuditEntry;
import com.lawyercompany.service.*;
import com.lawyercompany.up.service.ExportService;
import com.lawyercompany.up.util.InputValidators;
import com.lawyercompany.up.util.Toast;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminDashboardController {

    private static final String MSG_ERROR = "Ошибка";
    private static final String LABEL_PHONE = "Телефон:";
    private static final String AUDIT_ENTITY_USERS = "users";
    private static final String AUDIT_OP_UPDATE = "UPDATE";
    private static final String AUDIT_SYSTEM_USER = "Система";
    private static final String FILTER_ALL = "Все";

    @FXML
    private TextField userSearchField;
    @FXML
    private TextField auditSearchField;
    @FXML
    private ComboBox<String> auditEntityFilterCombo;
    @FXML
    private ComboBox<String> auditOperationFilterCombo;
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, Boolean> blockedColumn;
    @FXML
    private TableColumn<User, Button> actionsColumn;
    @FXML
    private TableColumn<User, Button> editColumn;

    @FXML
    private TableView<AuditEntry> auditTable;
    @FXML
    private TableColumn<AuditEntry, String> auditTimeColumn;
    @FXML
    private TableColumn<AuditEntry, String> auditUserColumn;
    @FXML
    private TableColumn<AuditEntry, String> auditEntityColumn;
    @FXML
    private TableColumn<AuditEntry, String> auditOperationColumn;
    @FXML
    private TableColumn<AuditEntry, String> auditDetailsColumn;


    private final UserService userService = new UserService();
    private final LawyerService lawyerService = new LawyerService();
    private final ClientService clientService = new ClientService();
    private final AuditService auditService = new AuditService();
    private MainApp mainApp;
    private User currentUser;

    private final ObservableList<User> allUsersMaster = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;

    private final ObservableList<AuditEntry> allAuditMaster = FXCollections.observableArrayList();
    private FilteredList<AuditEntry> filteredAudit;
    private final Map<UUID, String> auditUsernamesCache = new HashMap<>();
    //Фильтры аудита: в UI показываем русские подписи, но фильтруем по "кодам" из БД
    private final Map<String, String> auditEntityRuToCode = new HashMap<>();
    private final Map<String, String> auditEntityCodeToRu = new HashMap<>();
    private final Map<String, String> auditOperationRuToCode = new HashMap<>();
    private final Map<String, String> auditOperationCodeToRu = new HashMap<>();

    @FXML
    private void initialize() {
        //таблица пользователей и аудит работают через FilteredList, поэтому поиск/фильтры идут без доп запросов в бд
        filteredUsers = new FilteredList<>(allUsersMaster, u -> true);
        usersTable.setItems(filteredUsers);
        filteredAudit = new FilteredList<>(allAuditMaster, a -> true);
        auditTable.setItems(filteredAudit);
        usersTable.setPlaceholder(new Label("Нет данных"));
        auditTable.setPlaceholder(new Label("Нет записей"));
        if (userSearchField != null) {
            userSearchField.textProperty().addListener((obs, prev, q) -> applyUserSearchFilter(q));
        }
        if (auditSearchField != null) {
            auditSearchField.textProperty().addListener((obs, prev, q) -> applyAuditFilter());
        }
        if (auditEntityFilterCombo != null) {
            auditEntityFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, prev, v) -> applyAuditFilter());
        }
        if (auditOperationFilterCombo != null) {
            auditOperationFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, prev, v) -> applyAuditFilter());
        }
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setCurrentUser(User user) {
        //currentUser нужен для аудита (changed_by) и для запрета "заблокировать самого себя"
        this.currentUser = user;
        configureTables();
        loadUsers();
        loadAudit();
    }

    private void configureTables() {
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadUsers() {
        usersTable.setDisable(true);

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                //тяжёлое из бд уходим в Task, чтобы не фризить интерфейс
                return userService.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> {
            List<User> users = task.getValue();
            // Админов не показываем в списке пользователей
            users.removeIf(u -> User.ROLE_ADMIN.equals(u.getRole()));
            allUsersMaster.setAll(users);
            applyUserSearchFilter(userSearchField != null ? userSearchField.getText() : null);
            usersTable.setDisable(false);
        });
        task.setOnFailed(e -> {
            usersTable.setDisable(false);
            showAlert("Ошибка загрузки пользователей: " + task.getException().getMessage());
        });
        new Thread(task, "load-admin-users").start();

        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(cellData -> {
            String r = cellData.getValue().getRole();
            String roleName = switch (r != null ? r : "") {
                case User.ROLE_ADMIN -> "Администратор";
                case User.ROLE_LAWYER -> "Адвокат";
                case User.ROLE_CLIENT -> "Клиент";
                default -> "Неизвестно";
            };
            return new SimpleStringProperty(roleName);
        });
        blockedColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isBlocked()));

        actionsColumn.setCellValueFactory(cellData -> {
            Button btn = new Button();
            User user = cellData.getValue();
            if (user.getId().equals(currentUser.getId())) {
                btn.setText("(Это вы)");
                btn.setDisable(true);
                return new javafx.beans.property.SimpleObjectProperty<>(btn);
            }
            if (user.isBlocked()) {
                btn.setText("Разблокировать");
                btn.setOnAction(e -> handleUnblock(user));
            } else {
                btn.setText("Заблокировать");
                btn.setOnAction(e -> handleBlock(user));
            }
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        });

        editColumn.setCellValueFactory(cellData -> {
            Button btn = new Button("Редактировать");
            User user = cellData.getValue();
            if (user.getId().equals(currentUser.getId())) {
                btn.setDisable(true);
            }
            btn.setOnAction(e -> handleEditUser(user));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        });
    }

    private void applyUserSearchFilter(String query) {
        if (filteredUsers == null) return;
        if (query == null || query.isBlank()) {
            filteredUsers.setPredicate(u -> true);
            usersTable.setPlaceholder(new Label("Нет данных"));
            return;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        filteredUsers.setPredicate(u -> contains(u.getUsername(), q)
                || contains(u.getFullName(), q)
                || contains(u.getEmail(), q));
        usersTable.setPlaceholder(new Label("Ничего не найдено"));
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void handleEditUser(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактирование пользователя");
        dialog.setHeaderText("Измените данные пользователя");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );
        applyDialogAppIcon(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fullNameField = new TextField(user.getFullName());
        TextField emailField = new TextField(user.getEmail());

        TextField phoneField = new TextField();
        TextField specializationField = new TextField();
        TextField licenseNumberField = new TextField();
        TextField officeAddressField = new TextField();
        TextField experienceYearsField = new TextField();
        TextField clientPhoneField = new TextField();
        TextField clientAddressField = new TextField();

        boolean isLawyer = User.ROLE_LAWYER.equals(user.getRole());
        boolean isClient = User.ROLE_CLIENT.equals(user.getRole());
        // Аватар: разрешаем менять только адвокату (у клиента пока нет кабинета)
        Label avatarLabel = new Label("Файл не выбран");
        Button avatarButton = new Button("Сменить аватар");
        ImageView avatarPreview = new ImageView();
        avatarPreview.setFitWidth(80);
        avatarPreview.setFitHeight(80);
        avatarPreview.setPreserveRatio(true);
        avatarPreview.setClip(new Circle(40, 40, 40));
        final byte[][] avatarBytes = new byte[1][];

        if (isLawyer) {
            lawyerService.getLawyerByUserId(user.getId()).ifPresent(lawyer -> {
                phoneField.setText(lawyer.getPhone());
                specializationField.setText(lawyer.getSpecialization());
                licenseNumberField.setText(lawyer.getLicenseNumber());
                officeAddressField.setText(lawyer.getOfficeAddress());
                experienceYearsField.setText(lawyer.getExperienceYears() != null ? String.valueOf(lawyer.getExperienceYears()) : "");
            });

            // Подтягиваем текущий аватар из БД для превью
            byte[] currentAvatar = userService.getAvatar(user.getId());
            avatarBytes[0] = currentAvatar;
            if (currentAvatar != null && currentAvatar.length > 0) {
                try {
                    avatarPreview.setImage(new Image(new ByteArrayInputStream(currentAvatar)));
                    avatarLabel.setText("Текущий аватар");
                } catch (Exception ignored) {
                    avatarPreview.setImage(null);
                }
            }

            avatarButton.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Выберите изображение для аватара");
                chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif")
                );
                File file = chooser.showOpenDialog(null);
                if (file == null) return;
                try {
                    avatarBytes[0] = readAvatarBytes(file);
                    avatarLabel.setText(file.getName());
                    setAvatarPreview(avatarPreview, avatarBytes[0]);
                } catch (Exception ex) {
                    showErrorInDialog(dialog, "Не удалось прочитать файл аватара: " + ex.getMessage());
                }
            });
        }
        if (isClient) {
            clientService.getClientByUserId(user.getId()).ifPresent(client -> {
                clientPhoneField.setText(client.getPhone() != null ? client.getPhone() : "");
                clientAddressField.setText(client.getAddress() != null ? client.getAddress() : "");
            });
        }

        int row = 0;
        grid.add(new Label("ФИО*:"), 0, row);
        grid.add(fullNameField, 1, row++);
        grid.add(new Label("Email*:"), 0, row);
        grid.add(emailField, 1, row++);

        if (isLawyer) {
            grid.add(new Label(LABEL_PHONE), 0, row);
            grid.add(phoneField, 1, row++);
            grid.add(new Label("Специализация:"), 0, row);
            grid.add(specializationField, 1, row++);
            grid.add(new Label("Номер удостоверения:"), 0, row);
            grid.add(licenseNumberField, 1, row++);
            grid.add(new Label("Адрес офиса:"), 0, row);
            grid.add(officeAddressField, 1, row++);
            grid.add(new Label("Стаж (лет):"), 0, row);
            grid.add(experienceYearsField, 1, row++);

            grid.add(new Label("Аватар:"), 0, row);
            grid.add(new javafx.scene.layout.HBox(10, avatarButton, avatarLabel), 1, row++);
            grid.add(avatarPreview, 1, row++);
        }
        if (isClient) {
            grid.add(new Label(LABEL_PHONE), 0, row);
            grid.add(clientPhoneField, 1, row++);
            grid.add(new Label("Адрес:"), 0, row);
            grid.add(clientAddressField, 1, row);
        }

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();

            if (fullName.isEmpty() || email.isEmpty()) {
                showErrorInDialog(dialog, "Заполните обязательные поля (ФИО, email).");
                event.consume();
                return;
            }
            if (!InputValidators.isValidEmail(email)) {
                showErrorInDialog(dialog, "Введите корректный email.");
                event.consume();
                return;
            }

            user.setFullName(fullName);
            user.setEmail(email);

            final int expYearsFinal;
            if (isLawyer) {
                user.setAvatarData(avatarBytes[0]);
                int expYears = 0;
                String expStr = experienceYearsField.getText().trim();
                if (!expStr.isEmpty()) {
                    try {
                        expYears = Integer.parseInt(expStr);
                        if (expYears < 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        showErrorInDialog(dialog, "Стаж должен быть целым неотрицательным числом.");
                        event.consume();
                        return;
                    }
                }
                expYearsFinal = expYears;
            } else {
                expYearsFinal = 0;
            }

            final String phone = phoneField.getText().trim();
            final String specialization = specializationField.getText().trim();
            final String licenseNumber = licenseNumberField.getText().trim();
            final String officeAddress = officeAddressField.getText().trim();
            final boolean lawyerFlag = isLawyer;
            final boolean clientFlag = isClient;
            final String clientPhone = clientPhoneField.getText().trim();
            final String clientAddress = clientAddressField.getText().trim();

            event.consume();

            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() {
                    userService.updateUser(user);
                    if (lawyerFlag) {
                        lawyerService.getLawyerByUserId(user.getId()).ifPresent(lawyer -> {
                            lawyer.setPhone(phone);
                            lawyer.setSpecialization(specialization);
                            lawyer.setLicenseNumber(licenseNumber);
                            lawyer.setOfficeAddress(officeAddress);
                            lawyer.setExperienceYears(expYearsFinal);
                            lawyerService.updateLawyer(lawyer);
                        });
                    }
                    if (clientFlag) {
                        clientService.getClientByUserId(user.getId()).ifPresent(client -> {
                            client.setPhone(clientPhone);
                            client.setAddress(clientAddress);
                            clientService.updateClient(client);
                        });
                    }
                    auditService.log(AUDIT_ENTITY_USERS, user.getId(), AUDIT_OP_UPDATE, currentUser.getId(), null,
                            "Обновлены данные пользователя");
                    return null;
                }
            };
            saveTask.setOnSucceeded(ev -> {
                loadUsers();
                dialog.close();
            });
            saveTask.setOnFailed(ev -> showErrorInDialog(dialog,
                    "Не удалось сохранить изменения: " + saveTask.getException().getMessage()));
            Thread st = new Thread(saveTask, "admin-edit-user-save");
            st.setDaemon(true);
            st.start();
        });

        dialog.showAndWait();
    }

    private void handleBlock(User user) {
        if (user.getId().equals(currentUser.getId())) {
            showAlert("Нельзя заблокировать самого себя");
            return;
        }
        UUID targetId = user.getId();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                userService.blockUser(targetId);
                auditService.log(AUDIT_ENTITY_USERS, targetId, "BLOCK", currentUser.getId(), "active", "blocked");
                return null;
            }
        };
        task.setOnSucceeded(e -> loadUsers());
        task.setOnFailed(e -> showErrorAlert("Не удалось заблокировать пользователя: " + task.getException().getMessage()));
        Thread t = new Thread(task, "admin-block-user");
        t.setDaemon(true);
        t.start();
    }

    private void handleUnblock(User user) {
        if (user.getId().equals(currentUser.getId())) {
            showAlert("Нельзя разблокировать самого себя");
            return;
        }
        UUID targetId = user.getId();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                userService.unblockUser(targetId);
                auditService.log(AUDIT_ENTITY_USERS, targetId, AUDIT_OP_UPDATE, currentUser.getId(), "blocked", "active");
                return null;
            }
        };
        task.setOnSucceeded(e -> loadUsers());
        task.setOnFailed(e -> showErrorAlert("Не удалось разблокировать пользователя: " + task.getException().getMessage()));
        Thread t = new Thread(task, "admin-unblock-user");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleCreateLawyer() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("LCS4admin");
        dialog.setHeaderText("Введите данные адвоката");

        // Подключаем общий стиль приложения к диалогу
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );
        applyDialogAppIcon(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField fullNameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        TextField specializationField = new TextField();
        TextField licenseNumberField = new TextField();
        TextField officeAddressField = new TextField();
        TextField experienceYearsField = new TextField();
        Label avatarLabel = new Label("Файл не выбран");
        Button avatarButton = new Button("Выбрать аватар");
        ImageView avatarPreview = new ImageView();
        avatarPreview.setFitWidth(80);
        avatarPreview.setFitHeight(80);
        avatarPreview.setPreserveRatio(true);
        avatarPreview.setClip(new Circle(40, 40, 40));
        final byte[][] avatarBytes = new byte[1][];

        grid.add(new Label("Логин*:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Пароль*:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("ФИО*:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Email*:"), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(new Label(LABEL_PHONE), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Специализация:"), 0, 5);
        grid.add(specializationField, 1, 5);
        grid.add(new Label("Номер удостоверения:"), 0, 6);
        grid.add(licenseNumberField, 1, 6);
        grid.add(new Label("Адрес офиса:"), 0, 7);
        grid.add(officeAddressField, 1, 7);
        grid.add(new Label("Стаж (лет):"), 0, 8);
        grid.add(experienceYearsField, 1, 8);
        grid.add(new Label("Аватар:"), 0, 9);
        grid.add(new javafx.scene.layout.HBox(10, avatarButton, avatarLabel), 1, 9);
        grid.add(avatarPreview, 1, 10);

        avatarButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите изображение для аватара");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = chooser.showOpenDialog(null);
            if (file == null) return;
            try {
                avatarBytes[0] = readAvatarBytes(file);
                avatarLabel.setText(file.getName());
                setAvatarPreview(avatarPreview, avatarBytes[0]);
            } catch (Exception ex) {
                avatarBytes[0] = null;
                avatarLabel.setText("Ошибка чтения файла");
                showErrorInDialog(dialog, "Не удалось прочитать файл аватара: " + ex.getMessage());
            }
        });

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                showErrorInDialog(dialog, "Заполните обязательные поля (логин, пароль, ФИО, email).");
                event.consume();
                return;
            }
            if (password.length() < 6) {
                showErrorInDialog(dialog, "Пароль должен содержать не менее 6 символов.");
                event.consume();
                return;
            }
            if (!InputValidators.isValidEmail(email)) {
                showErrorInDialog(dialog, "Введите корректный email.");
                event.consume();
                return;
            }

            int expYears = 0;
            String expStr = experienceYearsField.getText().trim();
            if (!expStr.isEmpty()) {
                try {
                    expYears = Integer.parseInt(expStr);
                    if (expYears < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showErrorInDialog(dialog, "Стаж должен быть целым неотрицательным числом.");
                    event.consume();
                    return;
                }
            }

            final int expYearsFinal = expYears;
            final String phone = phoneField.getText().trim();
            final String specialization = specializationField.getText().trim();
            final String licenseNumber = licenseNumberField.getText().trim();
            final String officeAddress = officeAddressField.getText().trim();
            final byte[] avatar = avatarBytes[0];

            event.consume();

            Task<Void> createTask = new Task<>() {
                @Override
                protected Void call() {
                    if (userService.getUserByUsername(username).isPresent()) {
                        throw new IllegalStateException("Пользователь с таким логином уже существует.");
                    }
                    lawyerService.createLawyer(
                            username, password, fullName, email,
                            phone,
                            specialization,
                            licenseNumber,
                            officeAddress,
                            expYearsFinal,
                            avatar
                    );
                    userService.getUserByUsername(username).ifPresent(u ->
                            auditService.log(AUDIT_ENTITY_USERS, u.getId(), "CREATE", currentUser.getId(), null,
                                    "Создан адвокат: " + username));
                    return null;
                }
            };
            createTask.setOnSucceeded(ev -> {
                loadUsers();
                dialog.close();
            });
            createTask.setOnFailed(ev -> {
                Throwable ex = createTask.getException();
                String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка";
                if (ex instanceof IllegalStateException && msg.contains("логином")) {
                    showErrorInDialog(dialog, msg);
                } else {
                    showErrorInDialog(dialog, "Не удалось создать адвоката: " + msg);
                }
            });
            Thread ct = new Thread(createTask, "admin-create-lawyer");
            ct.setDaemon(true);
            ct.start();
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleRefreshUsers() {
        loadUsers();
    }

    @FXML
    private void handleExportAuditCsv() {
        if (filteredAudit == null || auditTable == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт аудита в CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        fc.setInitialFileName("audit.csv");
        File f = fc.showSaveDialog(auditTable.getScene().getWindow());
        if (f == null) return;

        List<AuditEntry> rows = new ArrayList<>(auditTable.getItems()); // это уже отфильтрованный список

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new ExportService().exportAuditToCsv(rows, f.toPath());
                return null;
            }
        };

        task.setOnSucceeded(e -> Toast.show(auditTable, "Экспорт выполнен: " + f.getName(), Toast.Type.SUCCESS));
        task.setOnFailed(e -> Toast.show(auditTable, "Ошибка экспорта: " + task.getException().getMessage(), Toast.Type.ERROR));

        Thread t = new Thread(task, "export-audit-csv");
        t.setDaemon(true);
        t.start();
    }

    private void loadAudit() {
        auditTable.setDisable(true);

        Task<AuditVM> task = new Task<>() {
            @Override
            protected AuditVM call() {
                List<AuditEntry> entries = auditService.getAllEntries();

                //чтобы не дергать бд в каждой строке таблицы, готовим кэш changedBy -> username/role
                Map<UUID, String> usernames = new HashMap<>();
                Map<UUID, String> roles = new HashMap<>();
                for (AuditEntry entry : entries) {
                    UUID changedBy = entry.getChangedBy();
                    if (changedBy == null || usernames.containsKey(changedBy)) continue;
                    var userOpt = userService.getUserById(changedBy);
                    String username = userOpt.map(User::getUsername).orElse(AUDIT_SYSTEM_USER);
                    String role = userOpt.map(User::getRole).orElse("");
                    usernames.put(changedBy, username);
                    roles.put(changedBy, role);
                }
                return new AuditVM(entries, usernames, roles);
            }
        };
        task.setOnSucceeded(e -> {
            AuditVM vm = task.getValue();
            auditUsernamesCache.clear();
            auditUsernamesCache.putAll(vm.usernames);
            allAuditMaster.setAll(vm.entries);
            setupAuditFilterOptions(vm.entries);
            applyAuditFilter();
            auditUserColumn.setCellValueFactory(cellData -> {
                UUID changedBy = cellData.getValue().getChangedBy();
                if (changedBy == null) return new SimpleStringProperty(AUDIT_SYSTEM_USER);
                String username = vm.usernames.getOrDefault(changedBy, AUDIT_SYSTEM_USER);
                String role = vm.roles.getOrDefault(changedBy, "");
                String roleRu = roleToRu(role);
                return new SimpleStringProperty(roleRu.isEmpty() ? username : (username + " (" + roleRu + ")"));
            });
            auditTable.setDisable(false);
        });
        task.setOnFailed(e -> {
            auditTable.setDisable(false);
            showAlert("Ошибка загрузки аудита: " + task.getException().getMessage());
        });
        new Thread(task, "load-admin-audit").start();

        auditTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatAuditTime(cellData.getValue().getChangedAt())));
        auditEntityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(translateEntityType(cellData.getValue().getEntityType())));
        auditOperationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(translateOperation(cellData.getValue().getOperation())));
        auditDetailsColumn.setCellValueFactory(cellData -> {
            String oldV = cellData.getValue().getOldValue();
            String newV = cellData.getValue().getNewValue();
            String text;
            if (oldV != null && !oldV.isEmpty() && newV != null && !newV.isEmpty()) {
                text = oldV + " → " + newV;
            } else if (newV != null && !newV.isEmpty()) {
                text = newV;
            } else if (oldV != null && !oldV.isEmpty()) {
                text = oldV;
            } else {
                text = "";
            }
            return new SimpleStringProperty(text);
        });
    }

    private String translateEntityType(String entityType) {
        if (entityType == null) return "";
        return switch (entityType) {
            case AUDIT_ENTITY_USERS -> "Пользователи";
            case "cases" -> "Дела";
            case "documents" -> "Документы";
            case "meetings" -> "Встречи";
            case "messages" -> "Сообщения";
            default -> entityType;
        };
    }

    private String translateOperation(String operation) {
        if (operation == null) return "";
        return switch (operation) {
            case "CREATE" -> "Создание";
            case AUDIT_OP_UPDATE -> "Изменение";
            case "DELETE" -> "Удаление";
            case "BLOCK" -> "Блокировка";
            case "DOWNLOAD" -> "Скачивание";
            default -> operation;
        };
    }

    private void setupAuditFilterOptions(List<AuditEntry> entries) {
        if (auditEntityFilterCombo != null) {
            String selected = auditEntityFilterCombo.getValue();
            List<String> entityTypes = entries.stream()
                    .map(AuditEntry::getEntityType)
                    .filter(v -> v != null && !v.isBlank())
                    .distinct()
                    .toList();
            auditEntityRuToCode.clear();
            auditEntityCodeToRu.clear();
            for (String code : entityTypes) {
                String ru = translateEntityType(code);
                auditEntityRuToCode.put(ru, code);
                auditEntityCodeToRu.put(code, ru);
            }

            List<String> entityTypeLabels = entityTypes.stream()
                    .map(code -> auditEntityCodeToRu.getOrDefault(code, code))
                    .distinct()
                    .sorted()
                    .toList();
            auditEntityFilterCombo.getItems().setAll("Все");
            auditEntityFilterCombo.getItems().addAll(entityTypeLabels);
            if (selected == null) selected = "Все";
            if (!"Все".equals(selected)) {
                selected = auditEntityCodeToRu.getOrDefault(selected, selected);
            }
            auditEntityFilterCombo.setValue(selected);
        }

        if (auditOperationFilterCombo != null) {
            String selected = auditOperationFilterCombo.getValue();
            List<String> ops = entries.stream()
                    .map(AuditEntry::getOperation)
                    .filter(v -> v != null && !v.isBlank())
                    .distinct()
                    .toList();
            auditOperationRuToCode.clear();
            auditOperationCodeToRu.clear();
            for (String code : ops) {
                String ru = translateOperation(code);
                auditOperationRuToCode.put(ru, code);
                auditOperationCodeToRu.put(code, ru);
            }
            List<String> opLabels = ops.stream()
                    .map(code -> auditOperationCodeToRu.getOrDefault(code, code))
                    .distinct()
                    .sorted()
                    .toList();
            auditOperationFilterCombo.getItems().setAll("Все");
            auditOperationFilterCombo.getItems().addAll(opLabels);
            if (selected == null) selected = "Все";
            if (!"Все".equals(selected)) {
                selected = auditOperationCodeToRu.getOrDefault(selected, selected);
            }
            auditOperationFilterCombo.setValue(selected);
        }
    }

    private String auditEntityFilterToCode(String uiValue) {
        if (uiValue == null || uiValue.isBlank() || "Все".equals(uiValue)) return "Все";
        return auditEntityRuToCode.getOrDefault(uiValue, uiValue);
    }

    private String auditOperationFilterToCode(String uiValue) {
        if (uiValue == null || uiValue.isBlank() || "Все".equals(uiValue)) return "Все";
        return auditOperationRuToCode.getOrDefault(uiValue, uiValue);
    }

    private void applyAuditFilter() {
        if (filteredAudit == null) return;
        String q = auditSearchField != null ? auditSearchField.getText() : null;
        String entity = auditEntityFilterCombo != null ? auditEntityFilterCombo.getValue() : null;
        String op = auditOperationFilterCombo != null ? auditOperationFilterCombo.getValue() : null;

        String query = q != null ? q.trim().toLowerCase(Locale.ROOT) : "";
        String entityFilterUi = entity != null ? entity.trim() : FILTER_ALL;
        String opFilterUi = op != null ? op.trim() : FILTER_ALL;
        String entityFilterCode = auditEntityFilterToCode(entityFilterUi);
        String opFilterCode = auditOperationFilterToCode(opFilterUi);

        filteredAudit.setPredicate(a -> {
            if (a == null) return false;
            if (!FILTER_ALL.equals(entityFilterCode)
                    && (a.getEntityType() == null || !a.getEntityType().equalsIgnoreCase(entityFilterCode))) {
                return false;
            }
            if (!FILTER_ALL.equals(opFilterCode)
                    && (a.getOperation() == null || !a.getOperation().equalsIgnoreCase(opFilterCode))) {
                return false;
            }
            if (query.isEmpty()) return true;

            if (contains(a.getEntityType(), query)) return true;
            if (contains(a.getOperation(), query)) return true;
            if (contains(translateEntityType(a.getEntityType()), query)) return true;
            if (contains(translateOperation(a.getOperation()), query)) return true;
            if (contains(a.getOldValue(), query)) return true;
            if (contains(a.getNewValue(), query)) return true;
            UUID changedBy = a.getChangedBy();
            if (changedBy != null) {
                String username = auditUsernamesCache.get(changedBy);
                if (contains(username, query)) return true;
            }
            return false;
        });

        if (auditTable != null) {
            auditTable.setPlaceholder(new Label(query.isEmpty() && FILTER_ALL.equals(entityFilterUi) && FILTER_ALL.equals(opFilterUi)
                    ? "Нет записей"
                    : "Ничего не найдено"));
        }
    }

    private String roleToRu(String role) {
        if (role == null) return "";
        return switch (role) {
            case User.ROLE_ADMIN -> "Администратор";
            case User.ROLE_LAWYER -> "Адвокат";
            case User.ROLE_CLIENT -> "Клиент";
            default -> "";
        };
    }

    private String formatAuditTime(java.time.OffsetDateTime changedAt) {
        if (changedAt == null) return "";
        java.time.OffsetDateTime local = changedAt.atZoneSameInstant(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        return local.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    @FXML
    private void handleRefreshAudit() {
        loadAudit();
    }

    @FXML
    private void handleLogout() {
        try {
            mainApp.showLoginWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Toast.show(usersTable != null ? usersTable : auditTable, message, Toast.Type.INFO);
    }

    private void showErrorAlert(String message) {
        Toast.show(usersTable != null ? usersTable : auditTable, message, Toast.Type.ERROR);
    }

    private void showErrorInDialog(Dialog<?> dialog, String message) {
        showAlertInDialog(dialog, MSG_ERROR, message);
    }

    private void showAlertInDialog(Dialog<?> dialog, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(dialog.getOwner());
        alert.showAndWait();
    }

    private void applyDialogAppIcon(Dialog<?> dialog) {
        dialog.setOnShown(ev -> {
            try {
                Stage dlgStage = (Stage) dialog.getDialogPane().getScene().getWindow();
                if (dlgStage == null) return;
                var iconUrl = getClass().getResource("/icons/app.png");
                if (iconUrl != null) {
                    dlgStage.getIcons().add(new Image(iconUrl.toExternalForm()));
                }
            } catch (Exception ex) {
                // иконка диалога необязательна, сбой не влияет на работу формы
            }
        });
    }

    private static byte[] readAvatarBytes(File file) throws java.io.IOException {
        return Files.readAllBytes(file.toPath());
    }

    private static void setAvatarPreview(ImageView preview, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            preview.setImage(null);
            return;
        }
        try {
            preview.setImage(new Image(new ByteArrayInputStream(bytes)));
        } catch (Exception ex) {
            preview.setImage(null);
        }
    }

    private static final class AuditVM {
        private final List<AuditEntry> entries;
        private final Map<UUID, String> usernames;
        private final Map<UUID, String> roles;

        private AuditVM(List<AuditEntry> entries, Map<UUID, String> usernames, Map<UUID, String> roles) {
            this.entries = entries;
            this.usernames = usernames;
            this.roles = roles;
        }
    }
}