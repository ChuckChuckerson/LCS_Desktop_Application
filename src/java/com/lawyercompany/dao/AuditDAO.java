package com.lawyercompany.dao;

import com.lawyercompany.entity.AuditEntry;

import java.util.List;
import java.util.UUID;

/**
 * DAO для аудит-логов (таблица {@code audit_log}).
 *
 * <p>Аудит использует {@link Long} как идентификатор записи журнала, в отличие от большинства остальных сущностей,
 * где применяются {@link UUID}.</p>
 */
public interface AuditDAO {
    /**
     * Найти запись аудита по идентификатору.
     *
     * @param id идентификатор записи аудита
     * @return запись аудита или {@code null}, если не найдена (в зависимости от реализации)
     */
    AuditEntry findById(Long id);

    /**
     * Получить все записи аудита.
     *
     * @return список записей (может быть пустым)
     */
    List<AuditEntry> findAll();

    /**
     * Сохранить новую запись аудита.
     *
     * @param entry запись аудита
     */
    void save(AuditEntry entry);

    /**
     * Обновить существующую запись аудита.
     *
     * @param entry запись с изменёнными полями
     */
    void update(AuditEntry entry);

    /**
     * Удалить запись аудита по идентификатору.
     *
     * @param id идентификатор записи аудита
     */
    void delete(Long id);

    /**
     * Найти записи аудита по идентификатору сущности, которую изменяли.
     *
     * @param entityId идентификатор сущности (UUID)
     * @return список записей аудита (может быть пустым)
     */
    List<AuditEntry> findByEntity(UUID entityId);

    /**
     * Найти записи аудита по пользователю, который вносил изменения.
     *
     * @param userId идентификатор пользователя (UUID)
     * @return список записей аудита (может быть пустым)
     */
    List<AuditEntry> findByUser(UUID userId);
}