package com.lawyercompany.dao;

import com.lawyercompany.entity.Case;

import java.util.List;
import java.util.UUID;

/**
 * DAO для дел (cases).
 */
public interface CaseDAO extends DAO<Case> {
    /**
     * Найти дела, принадлежащие клиенту.
     *
     * @param clientId идентификатор клиента
     * @return список дел клиента
     */
    List<Case> findByClientId(UUID clientId);

    /**
     * Найти дела, назначенные адвокату.
     *
     * @param lawyerId идентификатор пользователя-адвоката
     * @return список дел адвоката
     */
    List<Case> findByLawyerId(UUID lawyerId);
}