package com.lawyercompany.dao;

import com.lawyercompany.entity.Client;

import java.util.Optional;
import java.util.UUID;

/**
 * DAO для клиентов (таблица {@code clients}).
 */
public interface ClientDAO extends DAO<Client> {
    /**
     * Найти профиль клиента по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя (UUID)
     * @return профиль клиента, если найден
     */
    Optional<Client> findByUserId(UUID userId);
}
