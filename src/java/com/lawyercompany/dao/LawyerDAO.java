package com.lawyercompany.dao;

import com.lawyercompany.entity.Lawyer;

import java.util.Optional;
import java.util.UUID;

/**
 * DAO для адвокатов (таблица {@code lawyers}).
 */
public interface LawyerDAO extends DAO<Lawyer> {
    /**
     * Найти профиль адвоката по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя (UUID)
     * @return профиль адвоката, если найден
     */
    Optional<Lawyer> findByUserId(UUID userId);
}