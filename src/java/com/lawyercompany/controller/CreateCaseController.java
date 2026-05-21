package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.Client;
import com.lawyercompany.entity.Lawyer;
import com.lawyercompany.entity.User;
import com.lawyercompany.service.CaseService;
import com.lawyercompany.service.ClientService;
import com.lawyercompany.service.UserService;
import com.lawyercompany.up.util.Toast;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.UUID;

public class CreateCaseController {

    @FXML
    private Label lawyerNameLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label successLabel;

    private final CaseService caseService = new CaseService();
    private final ClientService clientService = new ClientService();
    private final UserService userService = new UserService();
    private User currentUser;
    private MainApp mainApp;
    private Stage dialogStage;
    private Lawyer preselectedLawyer;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreselectedLawyer(Lawyer lawyer) {
        this.preselectedLawyer = lawyer;
        //это просто отображение выбранного адвоката, чтобы клиент видел кому пишет/создаёт дело
        if (lawyer != null) {
            String fullName = userService.getUserById(lawyer.getUserId())
                    .map(User::getFullName)
                    .orElse("Неизвестный адвокат");
            lawyerNameLabel.setText(fullName);
        } else {
            lawyerNameLabel.setText("Не выбран");
        }
    }

    @FXML
    private void handleCreate() {
        //перед созданием дела проверяем блокировку, потому что статус мог поменяться уже после логина
        User refreshed = userService.getUserById(currentUser.getId()).orElse(currentUser);
        currentUser = refreshed;
        if (refreshed.isBlocked()) {
            Toast.show(titleField, "Ваш аккаунт заблокирован администратором.", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }

        if (preselectedLawyer == null) {
            Toast.show(titleField, "Адвокат не выбран", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (title.isEmpty()) {
            Toast.show(titleField, "Введите название дела", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }

        Optional<Client> clientOpt = clientService.getClientByUserId(currentUser.getId());
        if (clientOpt.isEmpty()) {
            Toast.show(titleField, "Ошибка: профиль клиента не найден", Toast.Type.ERROR);
            successLabel.setText("");
            return;
        }
        UUID clientId = clientOpt.get().getId();
        UUID lawyerUserId = preselectedLawyer.getUserId();
        UUID actorId = currentUser.getId();

        successLabel.setText("Создание…");
        titleField.setDisable(true);
        descriptionArea.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                //это БД + аудит, поэтому не в UI потоке
                caseService.createCase(clientId, lawyerUserId, title, description, actorId);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            //успех: закрываем модалку и обновляем дашборд клиента
            titleField.setDisable(false);
            descriptionArea.setDisable(false);
            successLabel.setText("");
            dialogStage.close();
            if (mainApp != null) {
                mainApp.refreshClientDashboard();
            }
        });
        task.setOnFailed(e -> {
            titleField.setDisable(false);
            descriptionArea.setDisable(false);
            Throwable ex = task.getException();
            String msg = "Ошибка создания дела: " + (ex != null && ex.getMessage() != null ? ex.getMessage() : "неизвестная ошибка");
            Toast.show(titleField, msg, Toast.Type.ERROR);
            successLabel.setText("");
        });
        Thread t = new Thread(task, "create-case");
        t.setDaemon(true);
        t.start();
    }
}