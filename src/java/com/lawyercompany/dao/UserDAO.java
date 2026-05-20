package com.lawyercompany.dao;

import com.lawyercompany.entity.User;

import java.util.Optional;

/**
 * DAO для работы с таблицей пользователей.
 */
public interface UserDAO extends DAO<User> {
    /**
     * Найти пользователя по логину.
     *
     * @param username логин
     * @return пользователь, если найден
     */
    Optional<User> findByUsername(String username);
}