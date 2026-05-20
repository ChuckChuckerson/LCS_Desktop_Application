package com.lawyercompany.dao;

import com.lawyercompany.entity.Document;

import java.util.List;
import java.util.UUID;

/**
 * DAO для документов по делам (таблица {@code documents}).
 */
public interface DocumentDAO extends DAO<Document> {
    /**
     * Найти документы, прикреплённые к делу.
     *
     * @param caseId идентификатор дела (UUID)
     * @return список документов дела (может быть пустым)
     */
    List<Document> findByCaseId(UUID caseId);
}