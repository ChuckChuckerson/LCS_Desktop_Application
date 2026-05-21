package com.lawyercompany.service;

import com.lawyercompany.dao.AuditDAO;
import com.lawyercompany.entity.AuditEntry;
import com.lawyercompany.factory.DAOFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Запись в {@code audit_log}. Допустимые {@code operation}: CREATE, UPDATE, DELETE, BLOCK (как в БД).
 */
public class AuditService {

    private final AuditDAO auditDAO;

    public AuditService() {
        //это маленький сервис-логгер: просто складываем события в audit_log
        this.auditDAO = DAOFactory.getAuditDAO();
    }

    public void log(String entityType, UUID entityId, String operation, UUID changedBy,
                    String oldValue, String newValue) {
        //важно: changedAt ставим здесь, чтобы все записи аудита были единообразны
        AuditEntry entry = new AuditEntry();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOperation(operation);
        entry.setChangedBy(changedBy);
        entry.setChangedAt(OffsetDateTime.now());
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        auditDAO.save(entry);
    }

    public List<AuditEntry> getAllEntries() {
        return auditDAO.findAll();
    }
}
