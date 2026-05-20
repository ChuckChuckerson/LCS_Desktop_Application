package com.lawyercompany.dao;

import com.lawyercompany.entity.Message;

import java.util.List;
import java.util.UUID;

/**
 * DAO для сообщений по делам (таблица {@code messages}).
 */
public interface MessageDAO extends DAO<Message> {
    /**
     * Найти сообщения, относящиеся к делу.
     *
     * @param caseId идентификатор дела (UUID)
     * @return список сообщений по делу (может быть пустым)
     */
    List<Message> findByCaseId(UUID caseId);
}