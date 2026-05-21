package com.lawyercompany.controller;

import com.lawyercompany.MainApp;
import com.lawyercompany.service.AuthService;
import com.lawyercompany.up.util.Toast;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private Label successLabel;

    private final AuthService authService = new AuthService();

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleRegister() {
        //это форма регистрации: валидируем поля тут, а саму БД/хеш пароля делаем в фоне через Task
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
            //это мягкая подсказка, не критическая ошибка
            Toast.show(usernameField, "Заполните все обязательные поля", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (username.length() < 3 || username.length() > 32) {
            Toast.show(usernameField, "Логин должен быть от 3 до 32 символов", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            //упрощаем жизнь: логин только латиница, чтобы потом не ловить сюрпризы в БД и в поиске
            Toast.show(usernameField, "Логин: только латиница, цифры, точка, подчёркивание, дефис", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (fullName.length() < 3) {
            Toast.show(fullNameField, "Введите ФИО (минимум 3 символа)", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
            //тут без "жёсткой" проверки доменов, просто базовый формат
            Toast.show(emailField, "Введите email в формате name@example.com", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^(?:8|\\+7)\\s*\\(?\\d{3}\\)?\\s*\\d{3}[\\s-]*\\d{2}[\\s-]*\\d{2}$")) {
            //телефон делаем только РФ, чтобы не городить 1000 форматов
            Toast.show(phoneField, "Телефон: РФ формат 8XXXXXXXXXX или +7XXXXXXXXXX", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (!password.equals(confirm)) {
            Toast.show(usernameField, "Пароли не совпадают", Toast.Type.ERROR);
            successLabel.setText("");
            return;
        }
        if (password.length() < 6) {
            Toast.show(usernameField, "Пароль должен быть не менее 6 символов", Toast.Type.ERROR);
            successLabel.setText("");
            return;
        }
        if (password.length() > 72) {
            //это ограничение под bcrypt: после 72 символов часть пароля может не учитываться
            Toast.show(usernameField, "Пароль слишком длинный (максимум 72 символа)", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }
        if (password.equalsIgnoreCase(username)) {
            Toast.show(usernameField, "Пароль не должен совпадать с логином", Toast.Type.INFO);
            successLabel.setText("");
            return;
        }

        usernameField.setDisable(true);
        passwordField.setDisable(true);
        confirmPasswordField.setDisable(true);
        fullNameField.setDisable(true);
        emailField.setDisable(true);
        phoneField.setDisable(true);
        addressField.setDisable(true);
        successLabel.setText("Регистрация…");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                //это БД + хеширование пароля, поэтому строго в фоне
                return authService.register(username, password, fullName, email, phone, address);
            }
        };
        task.setOnSucceeded(e -> {
            //onSucceeded идёт в FX thread: можно трогать поля/лейблы
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            confirmPasswordField.setDisable(false);
            fullNameField.setDisable(false);
            emailField.setDisable(false);
            phoneField.setDisable(false);
            addressField.setDisable(false);
            Boolean success = task.getValue();
            if (Boolean.TRUE.equals(success)) {
                Toast.show(usernameField, "Регистрация успешна", Toast.Type.SUCCESS);
                successLabel.setText("");
                clearFields();
            } else {
                //это обычно значит, что логин уже занят
                Toast.show(usernameField, "Пользователь с таким логином уже существует", Toast.Type.ERROR);
                successLabel.setText("");
            }
        });
        task.setOnFailed(e -> {
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            confirmPasswordField.setDisable(false);
            fullNameField.setDisable(false);
            emailField.setDisable(false);
            phoneField.setDisable(false);
            addressField.setDisable(false);
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Ошибка регистрации";
            Toast.show(usernameField, msg, Toast.Type.ERROR);
            successLabel.setText("");
        });
        Thread t = new Thread(task, "register");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleGoToLogin() throws Exception {
        if (mainApp != null) {
            mainApp.showLoginWindow();
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();
    }
}