package com.lawyercompany.service;

import com.lawyercompany.dao.CaseDAO;
import com.lawyercompany.entity.Case;
import com.lawyercompany.factory.DAOFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для работы с делами (создание, получение списков, изменение статуса).
 */
public class CaseService {

    private final CaseDAO caseDAO;
    private final AuditService auditService;

    public CaseService() {
        //это слой "между контроллером и DAO": тут удобно держать бизнес-логику + аудит
        this.caseDAO = DAOFactory.getCaseDAO();
        this.auditService = new AuditService();
    }

    /**
     * Создать новое дело.
     *
     * @param clientId идентификатор клиента
     * @param lawyerUserId идентификатор пользователя-адвоката
     * @param title название дела
     * @param description описание дела
     * @param currentUserId текущий пользователь (для аудита/контекста; в текущей реализации не используется)
     * @return созданное дело
     */
    public Case createCase(UUID clientId, UUID lawyerUserId, String title, String description, UUID currentUserId) {
        //caseNumber нужен чисто как человеко-читаемый номер, id всё равно UUID
        String caseNumber = generateCaseNumber();

        Case newCase = new Case();
        //это важно: генерим id заранее, чтобы корректно записать audit_log сразу после сохранения
        UUID caseId = UUID.randomUUID();
        newCase.setId(caseId);
        newCase.setCaseNumber(caseNumber);
        newCase.setTitle(title);
        newCase.setDescription(description);
        newCase.setClientId(clientId);
        newCase.setLawyerId(lawyerUserId);
        newCase.setStatus("Новое");
        newCase.setCreatedAt(OffsetDateTime.now());
        newCase.setUpdatedAt(OffsetDateTime.now());
        newCase.setDeleted(false);

        caseDAO.save(newCase);
        //аудит пишем здесь, а не в контроллере, чтобы не забывать (и чтобы UI не отвечал за логи)
        auditService.log("cases", caseId, "CREATE", currentUserId, null,
                "Создано дело: " + title + " (" + caseNumber + ")");
        return newCase;
    }

    /**
     * Получить список дел клиента.
     *
     * @param clientId идентификатор клиента
     * @return список дел
     */
    public List<Case> getCasesByClientId(UUID clientId) {
        return caseDAO.findByClientId(clientId);
    }

    /**
     * Получить список дел адвоката.
     *
     * @param lawyerUserId идентификатор пользователя-адвоката
     * @return список дел
     */
    public List<Case> getCasesByLawyerId(UUID lawyerUserId) {
        return caseDAO.findByLawyerId(lawyerUserId);
    }

    /**
     * Получить дело по идентификатору.
     *
     * @param caseId идентификатор дела
     * @return дело или {@code null}, если не найдено
     */
    public Case getCaseById(UUID caseId) {
        return caseDAO.findById(caseId);
    }

    /**
     * Изменить статус дела.
     *
     * @param caseId идентификатор дела
     * @param newStatus новый статус
     * @param currentUserId текущий пользователь (для аудита/контекста; в текущей реализации не используется)
     * @throws IllegalArgumentException если дело не найдено
     */
    public void updateCaseStatus(UUID caseId, String newStatus, UUID currentUserId) {
        Case caseEntity = caseDAO.findById(caseId);
        if (caseEntity == null) {
            throw new IllegalArgumentException("Дело не найдено");
        }
        String oldStatus = caseEntity.getStatus();
        caseEntity.setStatus(newStatus);
        caseEntity.setUpdatedAt(OffsetDateTime.now());
        caseDAO.update(caseEntity);
        //oldValue/newValue удобно смотреть в админке
        auditService.log("cases", caseId, "UPDATE", currentUserId, oldStatus, newStatus);
    }

    private String generateCaseNumber() {
        return "CASE-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 4);
    }
}