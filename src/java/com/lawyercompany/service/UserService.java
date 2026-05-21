package com.lawyercompany.service;

import com.lawyercompany.dao.ClientDAO;
import com.lawyercompany.dao.UserDAO;
import com.lawyercompany.entity.Client;
import com.lawyercompany.entity.User;
import com.lawyercompany.factory.DAOFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис операций над пользователями и профилями клиентов.
 *
 * <p>Используется контроллерами JavaFX и другими сервисами для работы с {@code users} и {@code clients}.</p>
 */
public class UserService {

    private final UserDAO userDAO;
    private final ClientDAO clientDAO;

    public UserService() {
        this.userDAO = DAOFactory.getUserDAO();
        this.clientDAO = DAOFactory.getClientDAO();
    }

    /**
     * Получить пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return пользователь, если найден
     */
    public Optional<User> getUserById(UUID id) {
        return Optional.ofNullable(userDAO.findById(id));
    }

    /**
     * Найти пользователя по логину.
     *
     * @param username логин
     * @return пользователь, если найден
     */
    public Optional<User> getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    /**
     * Создать профиль клиента для указанного пользователя.
     *
     * @param userId идентификатор пользователя
     * @param phone телефон
     * @param address адрес
     */
    public void createClientProfile(UUID userId, String phone, String address) {
        Client client = new Client();
        client.setUserId(userId);
        client.setPhone(phone);
        client.setAddress(address);
        clientDAO.save(client);
    }

    /**
     * Получить профиль клиента по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return профиль клиента, если найден
     */
    public Optional<Client> getClientByUserId(UUID userId) {
        return clientDAO.findByUserId(userId);
    }

    /**
     * Обновить аватар пользователя.
     *
     * @param userId идентификатор пользователя
     * @param avatarData данные аватара (byte[])
     */
    public void updateAvatar(UUID userId, byte[] avatarData) {
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setAvatarData(avatarData);
            userDAO.update(user);
        }
    }

    /**
     * Получить данные аватара пользователя.
     *
     * @param userId идентификатор пользователя
     * @return данные аватара или {@code null}, если не задан
     */
    public byte[] getAvatar(UUID userId) {
        User user = userDAO.findById(userId);
        return user != null ? user.getAvatarData() : null;
    }

    /**
     * Получить список всех пользователей.
     *
     * @return список пользователей
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /**
     * Заблокировать пользователя (признак {@code is_blocked}).
     *
     * @param userId идентификатор пользователя
     */
    public void blockUser(UUID userId) {
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setBlocked(true);
            userDAO.update(user);
        }
    }

    /**
     * Разблокировать пользователя (признак {@code is_blocked}).
     *
     * @param userId идентификатор пользователя
     */
    public void unblockUser(UUID userId) {
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setBlocked(false);
            userDAO.update(user);
        }
    }

    /**
     * Обновить пользователя.
     *
     * @param user пользователь с изменениями
     */
    public void updateUser(User user) {
        userDAO.update(user);
    }
}