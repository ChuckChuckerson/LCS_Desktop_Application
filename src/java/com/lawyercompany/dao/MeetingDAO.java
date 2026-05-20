package com.lawyercompany.dao;

import com.lawyercompany.entity.Meeting;

import java.util.List;
import java.util.UUID;

/**
 * DAO для встреч по делам (таблица {@code meetings}).
 */
public interface MeetingDAO extends DAO<Meeting> {
    /**
     * Найти встречи, назначенные в рамках дела.
     *
     * @param caseId идентификатор дела (UUID)
     * @return список встреч по делу (может быть пустым)
     */
    List<Meeting> findByCaseId(UUID caseId);
}