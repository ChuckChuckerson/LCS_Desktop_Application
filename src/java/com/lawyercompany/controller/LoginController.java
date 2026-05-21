package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.entity.User;
import com.lawyercompany.service.AuthService;
import com.lawyercompany.up.util.LoginResult;
import com.lawyercompany.up.util.Toast;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label successLabel;

    private final AuthService authService = new AuthService();
    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleLogin() {
        //это чисто UI-логика: читаем поля, валидируем и запускаем фоновый Task (чтобы не фризить FX thread)
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            //это "мягкая" ошибка, без модального окна
            Toast.show(usernameField, "Введите логин и пароль", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }

        //это чтобы пользователь не нажимал "Войти" 10 раз подряд
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        successLabel.setText("Вход…");

        Task<LoginResult> task = new Task<>() {
            @Override
            protected LoginResult call() {
                //это БД + проверка пароля, поэтому только в фоне
                return authService.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            //onSucceeded вызывается в FX thread, поэтому тут можно трогать UI
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            LoginResult result = task.getValue();
            if (result == null) {
                Toast.show(usernameField, "Ошибка входа", Toast.Type.ERROR);
                successLabel.setText("");
                return;
            }
            if (result.isSuccess()) {
                //успех: переходим на нужный дашборд по роли
                successLabel.setText("");
                User user = result.getUser();
                try {
                    if (User.ROLE_CLIENT.equals(user.getRole())) {
                        mainApp.showClientDashboard(user);
                    } else if (User.ROLE_LAWYER.equals(user.getRole())) {
                        mainApp.showLawyerDashboard(user);
                    } else if (User.ROLE_ADMIN.equals(user.getRole())) {
                        mainApp.showAdminDashboard(user);
                    } else {
                        Toast.show(usernameField, "Неизвестная роль", Toast.Type.ERROR);
                        successLabel.setText("");
                    }
                } catch (Exception ex) {
                    //на всякий случай ловим ошибки перехода, чтобы UI не "молчал"
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Ошибка открытия окна";
                    Toast.show(usernameField, msg, Toast.Type.ERROR);
                    successLabel.setText("");
                }
            } else {
                //ошибка авторизации: показываем причину только тостом, без дубля в лейбле
                Toast.show(usernameField, result.getMessage(), Toast.Type.ERROR);
                successLabel.setText("");
            }
        });
        task.setOnFailed(e -> {
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Ошибка входа";
            Toast.show(usernameField, msg, Toast.Type.ERROR);
            successLabel.setText("");
        });
        Thread loginThread = new Thread(task, "login");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    @FXML
    private void handleRegister() throws Exception {
        if (mainApp != null) {
            mainApp.showRegisterWindow();
        }
    }
}