package com.lawyercompany.dao;

import java.util.List;
import java.util.UUID;

/**
 * Базовый контракт CRUD-операций для доступа к данным.
 *
 * <p>Реализации работают с PostgreSQL через JDBC и используют UUID в качестве идентификаторов сущностей.</p>
 *
 * @param <T> тип сущности
 */
public interface DAO<T> {
    /**
     * Найти сущность по идентификатору.
     *
     * @param id идентификатор (UUID)
     * @return сущность или {@code null}, если не найдена (в зависимости от реализации)
     */
    T findById(UUID id);

    /**
     * Получить все записи для сущности.
     *
     * @return список сущностей (может быть пустым)
     */
    List<T> findAll();

    /**
     * Сохранить новую сущность.
     *
     * @param entity сущность для сохранения
     */
    void save(T entity);

    /**
     * Обновить существующую сущность.
     *
     * @param entity сущность с изменёнными полями
     */
    void update(T entity);

    /**
     * Удалить сущность по идентификатору (жёстко или мягко — зависит от реализации).
     *
     * @param id идентификатор (UUID)
     */
    void delete(UUID id);
}
