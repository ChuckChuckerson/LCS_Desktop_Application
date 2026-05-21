package com.lawyercompany.service;

import com.lawyercompany.dao.UserDAO;
import com.lawyercompany.entity.User;
import com.lawyercompany.factory.DAOFactory;
import com.lawyercompany.up.util.LoginResult;
import com.lawyercompany.up.util.PasswordEncoder;
import com.lawyercompany.up.util.BCryptPasswordEncoder;

import java.util.Optional;

/**
 * Сервис аутентификации и регистрации пользователей.
 *
 * <p>Использует BCrypt для проверки паролей.</p>
 */
public class AuthService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuditService auditService;

    public AuthService() {
        this.userDAO = DAOFactory.getUserDAO();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.userService = new UserService();
        this.auditService = new AuditService();
    }

    /**
     * Выполнить вход пользователя по логину и паролю.
     */
    public LoginResult login(String username, String rawPassword) {
        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new LoginResult(false, "Неверный логин или пароль", 0, null);
        }

        User user = userOpt.get();
        if (user.isBlocked()) {
            return new LoginResult(false, "Ваш аккаунт заблокирован администратором.", 0, null);
        }

        boolean success = passwordEncoder.matches(rawPassword, user.getPasswordHash());

        if (success) {
            return new LoginResult(true, "Успешный вход", 0, user);
        }
        return new LoginResult(false, "Неверный логин или пароль", 0, null);
    }

    /**
     * Зарегистрировать нового клиента.
     */
    public boolean register(String username, String rawPassword, String fullName, String email,
                            String phone, String address) {
        if (userDAO.findByUsername(username).isPresent()) {
            return false;
        }

        String hashed = passwordEncoder.encode(rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashed);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(User.ROLE_CLIENT);
        user.setDeleted(false);
        user.setBlocked(false);

        userDAO.save(user);

        userService.createClientProfile(user.getId(), phone, address);

        auditService.log("users", user.getId(), "CREATE", user.getId(), null,
                "Регистрация клиента: " + username);

        return true;
    }
}
