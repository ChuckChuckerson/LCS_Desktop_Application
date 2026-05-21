package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.*;
import com.lawyercompany.service.*;
import com.lawyercompany.up.util.Toast;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CaseWindowController {

    private static final String MSG_ERROR = "Ошибка";
    private static final String UNKNOWN_USER = UNKNOWN_USER;
    private static final String DATETIME_PATTERN = "dd.MM.yyyy HH:mm";

    @FXML
    private Label caseTitleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Message> messagesListView;
    @FXML
    private TextArea messageInput;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TableView<Document> documentsTable;
    @FXML
    private TableColumn<Document, String> docNameColumn;
    @FXML
    private TableColumn<Document, String> docDateColumn;
    @FXML
    private TableColumn<Document, Button> docActionColumn;
    @FXML
    private TableColumn<Document, Button> docDeleteColumn;
    @FXML
    private ListView<Meeting> meetingsListView;
    @FXML
    private Button createMeetingButton;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private Button updateStatusButton;

    private Case currentCase;
    private User currentUser;
    private UUID caseId;
    private UUID otherUserId;
    private Stage stage;
    private MainApp mainApp;

    private final CaseService caseService = new CaseService();
    private final MessageService messageService = new MessageService();
    private final DocumentService documentService = new DocumentService();
    private final MeetingService meetingService = new MeetingService();
    private final UserService userService = new UserService();
    private final ClientService clientService = new ClientService();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void initData(Case caseEntity, User user, Stage stage) {
        //это "экран дела": сюда передаём текущего пользователя, само дело и stage окна, чтобы можно было закрыть при блокировке
        this.currentCase = caseEntity;
        this.currentUser = user;
        this.caseId = caseEntity.getId();
        this.stage = stage;

        if (!refreshSelfOrLogoutIfBlocked()) {
            if (this.stage != null) this.stage.close();
            return;
        }

        String title = caseEntity.getTitle() != null ? caseEntity.getTitle().trim() : "";
        caseTitleLabel.setText(title.isEmpty() ? "Дело" : "«" + title + "»");
        // Устанавливаем описание
        if (caseEntity.getDescription() != null && !caseEntity.getDescription().isEmpty()) {
            descriptionArea.setText(caseEntity.getDescription());
        } else {
            descriptionArea.setText("Описание отсутствует");
        }
        // определяем собеседника (в сообщениях показываем "вторую сторону" дела)
        if (User.ROLE_CLIENT.equals(user.getRole())) {
            otherUserId = caseEntity.getLawyerId();
        } else { // адвокат или администратор
            UUID clientId = caseEntity.getClientId();
            Optional<Client> clientOpt = clientService.getClientById(clientId);
            if (clientOpt.isPresent()) {
                otherUserId = clientOpt.get().getUserId();
            } else {
                otherUserId = null;
            }
        }

        // статус подтягиваем из бд, чтобы на разных окнах/пользователях было актуально
        refreshStatus();

        if (User.ROLE_LAWYER.equals(user.getRole())) {
            statusComboBox.setVisible(true);
            updateStatusButton.setVisible(true);
            statusLabel.setVisible(false);
            statusComboBox.getItems().addAll("Новое", "В работе", "На рассмотрении", "Завершено");
            statusComboBox.setValue(caseEntity.getStatus());
        } else {
            statusComboBox.setVisible(false);
            updateStatusButton.setVisible(false);
            statusLabel.setVisible(true);
            statusLabel.setText(caseEntity.getStatus());
        }

        // Показываем кнопку назначения встречи только адвокату
        createMeetingButton.setVisible(User.ROLE_LAWYER.equals(user.getRole()));

        configureDocumentsTable();
        configureMeetingsListView();

        loadMessages();
        loadDocuments();
        loadMeetings();
    }

    private void configureDocumentsTable() {
        documentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        documentsTable.setPlaceholder(new Label("Документы отсутствуют"));
        docNameColumn.setMinWidth(160);
        docNameColumn.setPrefWidth(360);
        docDateColumn.setMinWidth(130);
        docDateColumn.setPrefWidth(155);
        docDateColumn.setMaxWidth(220);
        docActionColumn.setMinWidth(108);
        docActionColumn.setPrefWidth(118);
        docActionColumn.setMaxWidth(140);
        docActionColumn.getStyleClass().add("table-action-column");
        docDeleteColumn.setMinWidth(108);
        docDeleteColumn.setPrefWidth(118);
        docDeleteColumn.setMaxWidth(140);
        docDeleteColumn.getStyleClass().add("table-action-column");

        boolean admin = User.ROLE_ADMIN.equals(currentUser.getRole());
        docDeleteColumn.setVisible(admin);

        docNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFilename()));
        docDateColumn.setCellValueFactory(cellData -> {
            OffsetDateTime utcTime = cellData.getValue().getUploadedAt();
            OffsetDateTime localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
            return new SimpleStringProperty(localTime.format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        });
        docActionColumn.setCellValueFactory(cellData -> {
            Button btn = new Button("Скачать");
            btn.getStyleClass().add("table-row-button");
            btn.setMinWidth(96);
            btn.setPrefWidth(112);
            btn.setMaxWidth(130);
            btn.setOnAction(e -> handleDownloadDocument(cellData.getValue()));
            return new SimpleObjectProperty<>(btn);
        });
        if (admin) {
            docDeleteColumn.setCellValueFactory(cellData -> {
                Button deleteBtn = new Button("Удалить");
                deleteBtn.getStyleClass().addAll("table-row-button", "danger-table-button");
                deleteBtn.setMinWidth(96);
                deleteBtn.setPrefWidth(112);
                deleteBtn.setMaxWidth(130);
                deleteBtn.setOnAction(e -> handleDeleteDocument(cellData.getValue()));
                return new SimpleObjectProperty<>(deleteBtn);
            });
        } else {
            docDeleteColumn.setCellFactory(null);
            docDeleteColumn.setCellValueFactory(null);
        }
    }

    private void configureMeetingsListView() {
        meetingsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Meeting m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                } else {
                    OffsetDateTime localDateTime = m.getMeetingDate().atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                    setText(localDateTime.format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)) +
                            " | " + m.getLocation() +
                            (m.getNotes() != null ? " | " + m.getNotes() : ""));
                    setWrapText(true);
                    setMaxWidth(Double.MAX_VALUE);
                }
            }
        });
    }

    private void refreshStatus() {
        //важно: запрос в бд идёт в фоне, а UI обновляется в onSucceeded (javafx сам вызывает его в FX thread)
        Task<Case> task = new Task<>() {
            @Override
            protected Case call() {
                return caseService.getCaseById(caseId);
            }
        };
        task.setOnSucceeded(e -> {
            Case refreshedCase = task.getValue();
            if (refreshedCase != null) {
                currentCase = refreshedCase;
                if (User.ROLE_LAWYER.equals(currentUser.getRole())) {
                    statusComboBox.setValue(refreshedCase.getStatus());
                } else {
                    statusLabel.setText(refreshedCase.getStatus());
                }
            }
        });
        task.setOnFailed(e -> showAlert(MSG_ERROR, "Не удалось обновить статус: " + task.getException().getMessage()));
        Thread rt = new Thread(task, "refresh-case-status");
        rt.setDaemon(true);
        rt.start();
    }

    @FXML
    private void handleUpdateStatus() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        String newStatus = statusComboBox.getValue();
        if (newStatus == null || newStatus.equals(currentCase.getStatus())) {
            return;
        }
        updateStatusButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                caseService.updateCaseStatus(caseId, newStatus, currentUser.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            updateStatusButton.setDisable(false);
            currentCase.setStatus(newStatus);
            refreshStatus();
            if (mainApp != null && User.ROLE_LAWYER.equals(currentUser.getRole())) {
                mainApp.refreshLawyerDashboard();
            }
        });
        task.setOnFailed(e -> {
            updateStatusButton.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось обновить статус: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "update-case-status");
        t.setDaemon(true);
        t.start();
    }

    private void loadMessages() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        messagesListView.setDisable(true);
        Task<MessagesVM> task = new Task<>() {
            @Override
            protected MessagesVM call() {
                //это немного дороже по запросам, поэтому собираем кэш senderId -> fullName
                List<Message> messages = messageService.getMessagesByCase(caseId);

                Map<UUID, String> userNames = new HashMap<>();
                Map<UUID, String> senderNameByMessageId = new HashMap<>();
                for (Message msg : messages) {
                    UUID senderId = msg.getSenderId();
                    if (senderId == null) {
                        senderNameByMessageId.put(msg.getId(), UNKNOWN_USER);
                        continue;
                    }
                    String name = userNames.computeIfAbsent(senderId,
                            sid -> userService.getUserById(sid).map(User::getFullName).orElse(UNKNOWN_USER));
                    senderNameByMessageId.put(msg.getId(), name);
                }
                return new MessagesVM(messages, senderNameByMessageId);
            }
        };
        task.setOnSucceeded(e -> {
            MessagesVM vm = task.getValue();
            messagesListView.setItems(FXCollections.observableArrayList(vm.messages));
            messagesListView.setDisable(false);

            messagesListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || msg == null) {
                        setText(null);
                    } else {
                        String senderName = vm.senderNameByMessageId.getOrDefault(msg.getId(), UNKNOWN_USER);

                        OffsetDateTime localTime = msg.getSentAt().atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                        String formatted = senderName + " (" + localTime.format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)) + "):\n" + msg.getMessageText();
                        setText(formatted);
                        setWrapText(true);
                        setMaxWidth(Double.MAX_VALUE);
                    }
                }
            });
        });
        task.setOnFailed(e -> {
            messagesListView.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось загрузить сообщения: " + task.getException().getMessage());
        });
        Thread msgThread = new Thread(task, "load-messages");
        msgThread.setDaemon(true);
        msgThread.start();
    }

    @FXML
    private void handleSendMessage() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        if (otherUserId == null) {
            showAlert(MSG_ERROR, "Не удалось определить собеседника");
            return;
        }
        if (text.length() > 1000) {
            showAlert(MSG_ERROR, "Сообщение не должно превышать 1000 символов.");
            return;
        }
        messageInput.setDisable(true);
        Task<Void> sendTask = new Task<>() {
            @Override
            protected Void call() {
                messageService.sendMessage(caseId, currentUser.getId(), text);
                return null;
            }
        };
        sendTask.setOnSucceeded(ev -> {
            messageInput.setDisable(false);
            messageInput.clear();
            loadMessages();
        });
        sendTask.setOnFailed(ev -> {
            messageInput.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось отправить сообщение: " + sendTask.getException().getMessage());
        });
        Thread st = new Thread(sendTask, "send-message");
        st.setDaemon(true);
        st.start();
    }

    private void loadDocuments() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        documentsTable.setDisable(true);
        Task<List<Document>> task = new Task<>() {
            @Override
            protected List<Document> call() {
                return documentService.getDocumentsByCase(caseId);
            }
        };
        task.setOnSucceeded(e -> {
            documentsTable.setItems(FXCollections.observableArrayList(task.getValue()));
            documentsTable.setDisable(false);
        });
        task.setOnFailed(e -> {
            documentsTable.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось загрузить документы: " + task.getException().getMessage());
        });
        Thread docThread = new Thread(task, "load-documents");
        docThread.setDaemon(true);
        docThread.start();
    }

    @FXML
    private void handleUploadDocument() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите документ");
        File file = fileChooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        String filename = file.getName();
        documentsTable.setDisable(true);
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] data = Files.readAllBytes(path);
                documentService.saveDocument(caseId, filename, data, currentUser.getId());
                return null;
            }
        };
        uploadTask.setOnSucceeded(ev -> {
            documentsTable.setDisable(false);
            loadDocuments();
        });
        uploadTask.setOnFailed(ev -> {
            documentsTable.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось загрузить документ: " + uploadTask.getException().getMessage());
        });
        Thread ut = new Thread(uploadTask, "upload-document");
        ut.setDaemon(true);
        ut.start();
    }

    private void handleDownloadDocument(Document doc) {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        // fileChooser только на FX thread, а чтение из бд и запись на диск делаем в фоне
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(doc.getFilename());
        File saveFile = fileChooser.showSaveDialog(null);
        if (saveFile == null) {
            return;
        }
        Path target = saveFile.toPath();
        UUID docId = doc.getId();
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] data = documentService.getDocumentData(docId, currentUser.getId());
                if (data == null) {
                    throw new IllegalStateException("Не удалось получить данные документа");
                }
                Files.write(target, data);
                return null;
            }
        };
        downloadTask.setOnFailed(ev -> showAlert(MSG_ERROR, downloadTask.getException().getMessage()));
        Thread dt = new Thread(downloadTask, "download-document");
        dt.setDaemon(true);
        dt.start();
    }

    private void handleDeleteDocument(Document doc) {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы действительно хотите удалить документ \"" + doc.getFilename() + "\"?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        UUID docId = doc.getId();
        String filename = doc.getFilename();
        documentsTable.setDisable(true);
        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() {
                documentService.deleteDocument(docId, filename, currentUser.getId());
                return null;
            }
        };
        deleteTask.setOnSucceeded(ev -> {
            documentsTable.setDisable(false);
            loadDocuments();
        });
        deleteTask.setOnFailed(ev -> {
            documentsTable.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось удалить документ: " + deleteTask.getException().getMessage());
        });
        Thread delT = new Thread(deleteTask, "delete-document");
        delT.setDaemon(true);
        delT.start();
    }

    private void loadMeetings() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        meetingsListView.setDisable(true);
        Task<List<Meeting>> task = new Task<>() {
            @Override
            protected List<Meeting> call() {
                return meetingService.getMeetingsByCase(caseId);
            }
        };
        task.setOnSucceeded(e -> {
            meetingsListView.setItems(FXCollections.observableArrayList(task.getValue()));
            meetingsListView.setDisable(false);
        });
        task.setOnFailed(e -> {
            meetingsListView.setDisable(false);
            showAlert(MSG_ERROR, "Не удалось загрузить встречи: " + task.getException().getMessage());
        });
        Thread mt = new Thread(task, "load-meetings");
        mt.setDaemon(true);
        mt.start();
    }

    private record MeetingDraft(OffsetDateTime meetingDateTime, String location, String notes) {
    }

    private static final class MessagesVM {
        private final List<Message> messages;
        private final Map<UUID, String> senderNameByMessageId;

        private MessagesVM(List<Message> messages, Map<UUID, String> senderNameByMessageId) {
            this.messages = messages;
            this.senderNameByMessageId = senderNameByMessageId;
        }
    }

    @FXML
    private void handleCreateMeeting() {
        if (!refreshSelfOrLogoutIfBlocked()) return;
        Dialog<MeetingDraft> dialog = new Dialog<>();
        dialog.setTitle("Назначить встречу");
        dialog.setHeaderText("Введите данные встречи");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );
        dialog.initOwner(stage);
        dialog.setOnShown(ev -> {
            try {
                Stage dlgStage = (Stage) dialog.getDialogPane().getScene().getWindow();
                if (dlgStage != null) {
                    var iconUrl = getClass().getResource("/icons/app.png");
                    if (iconUrl != null) {
                        dlgStage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
                    }
                }
            } catch (Exception ignored) {
                // иконка диалога опциональна, ошибку не показываем
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("ДД.ММ.ГГГГ");
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");
        TextField locationField = new TextField();
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Дата*:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Время*:"), 0, 1);
        grid.add(timeField, 1, 1);
        grid.add(new Label("Место*:"), 0, 2);
        grid.add(locationField, 1, 2);
        grid.add(new Label("Заметки:"), 0, 3);
        grid.add(notesArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Лёгкая стилизация: primary/secondary для кнопок диалога
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        if (saveBtn != null) saveBtn.getStyleClass().add("primary-button");
        if (cancelBtn != null) cancelBtn.getStyleClass().add("secondary-button");

        // Не используем resultConverter для валидации: иначе OK закрывает диалог.
        Button saveBtn2 = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveBtn2 != null) {
            saveBtn2.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                if (!refreshSelfOrLogoutIfBlocked()) return;

                LocalDate date = datePicker.getValue();
                String timeStr = timeField.getText().trim();
                String location = locationField.getText().trim();

                if (date == null) {
                    Toast.show(locationField, "Выберите дату.", Toast.Type.INFO);
                    return;
                }
                if (date.isBefore(LocalDate.now())) {
                    Toast.show(locationField, "Дата не может быть в прошлом.", Toast.Type.INFO);
                    return;
                }
                if (timeStr.isEmpty()) {
                    Toast.show(timeField, "Введите время в формате HH:mm.", Toast.Type.INFO);
                    return;
                }
                LocalTime time;
                try {
                    time = LocalTime.parse(timeStr);
                } catch (Exception e) {
                    Toast.show(timeField, "Некорректный формат времени. Используйте HH:mm (например, 14:30).", Toast.Type.ERROR);
                    return;
                }
                if (location.isEmpty()) {
                    Toast.show(locationField, "Введите место встречи.", Toast.Type.INFO);
                    return;
                }

                LocalDateTime dateTime = LocalDateTime.of(date, time);
                OffsetDateTime meetingDateTime = dateTime.atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toOffsetDateTime();
                String notes = notesArea.getText();

                MeetingDraft draft = new MeetingDraft(meetingDateTime, location, notes != null ? notes : "");
                dialog.close();

                createMeetingButton.setDisable(true);
                Task<Meeting> createTask = new Task<>() {
                    @Override
                    protected Meeting call() {
                        return meetingService.createMeeting(caseId, draft.meetingDateTime(), draft.location(), draft.notes(), currentUser.getId());
                    }
                };
                createTask.setOnSucceeded(ev2 -> {
                    createMeetingButton.setDisable(false);
                    loadMeetings();
                });
                createTask.setOnFailed(ev2 -> {
                    createMeetingButton.setDisable(false);
                    showAlert(MSG_ERROR, "Не удалось создать встречу: " + createTask.getException().getMessage());
                });
                Thread ct = new Thread(createTask, "create-meeting");
                ct.setDaemon(true);
                ct.start();
            });
        }

        dialog.showAndWait();
    }

    private void showAlert(String title, String message) {
        String msg = (title != null && !title.isBlank()) ? (title + ": " + message) : message;
        Toast.show(documentsTable != null ? documentsTable : caseTitleLabel, msg, Toast.Type.ERROR);
    }

    private boolean refreshSelfOrLogoutIfBlocked() {
        if (currentUser == null) return true;
        User refreshed = userService.getUserById(currentUser.getId()).orElse(currentUser);
        currentUser = refreshed;
        if (refreshed.isBlocked()) {
            showAlert("Доступ ограничен", "Ваш аккаунт заблокирован администратором. Выполните вход снова.");
            try {
                if (mainApp != null) {
                    mainApp.showLoginWindow();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (stage != null) stage.close();
            return false;
        }
        return true;
    }
}