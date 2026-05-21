package com.lawyercompany.service;

import com.lawyercompany.dao.DocumentDAO;
import com.lawyercompany.entity.Document;
import com.lawyercompany.factory.DAOFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для работы с документами дела (метаданные и содержимое файла).
 *
 * <p>Файлы сохраняются в PostgreSQL как двоичные данные.</p>
 */
public class DocumentService {

    private static final String AUDIT_ENTITY_DOCUMENTS = "documents";

    private final DocumentDAO documentDAO;
    private final AuditService auditService;

    public DocumentService() {
        //документы = метаданные + байты, поэтому удобнее держать всё в одном сервисе
        this.documentDAO = DAOFactory.getDocumentDAO();
        this.auditService = new AuditService();
    }

    /**
     * Сохранить документ (файл) для указанного дела.
     *
     * @param caseId идентификатор дела
     * @param filename исходное имя файла
     * @param data содержимое файла
     * @param uploadedBy идентификатор пользователя, загрузившего файл
     */
    public void saveDocument(UUID caseId, String filename, byte[] data, UUID uploadedBy) {
        Document doc = new Document();
        //id нужен заранее, чтобы audit_log ссылался на конкретный документ
        UUID docId = UUID.randomUUID();
        doc.setId(docId);
        doc.setCaseId(caseId);
        doc.setFilename(filename);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadedAt(OffsetDateTime.now());
        doc.setFileData(data);
        documentDAO.save(doc);

        //аудит CREATE: кто загрузил и какой файл
        auditService.log(AUDIT_ENTITY_DOCUMENTS, docId, "CREATE", uploadedBy, null,
                "Загружен документ: " + filename);
    }

    /**
     * Получить содержимое файла документа.
     *
     * @param docId идентификатор документа
     * @return байты файла или {@code null}, если документ не найден
     */
    public byte[] getDocumentData(UUID docId) {
        Document doc = documentDAO.findById(docId);
        return doc != null ? doc.getFileData() : null;
    }

    /**
     * Получить содержимое файла документа и зафиксировать скачивание в аудит-логе.
     *
     * @param docId идентификатор документа
     * @param downloadedBy идентификатор пользователя, скачавшего документ
     * @return байты файла или {@code null}, если документ не найден
     */
    public byte[] getDocumentData(UUID docId, UUID downloadedBy) {
        //скачивание тоже логируем, чтобы админ видел кто уносил документы
        Document doc = documentDAO.findById(docId);
        if (doc == null) return new byte[0];
        auditService.log(AUDIT_ENTITY_DOCUMENTS, docId, "DOWNLOAD", downloadedBy, null,
                "Скачан документ: " + doc.getFilename());
        return doc.getFileData();
    }

    /**
     * Получить метаданные документа (без гарантии наличия {@code fileData}).
     *
     * @param docId идентификатор документа
     * @return документ или {@code null}, если не найден
     */
    public Document getDocumentMetadata(UUID docId) {
        return documentDAO.findById(docId);
    }

    /**
     * Получить список документов дела.
     *
     * @param caseId идентификатор дела
     * @return список документов
     */
    public List<Document> getDocumentsByCase(UUID caseId) {
        return documentDAO.findByCaseId(caseId);
    }

    /**
     * Удалить документ из БД.
     *
     * @param docId идентификатор документа
     */
    public void deleteDocument(UUID docId) {
        documentDAO.delete(docId);
    }

    /**
     * Удалить документ из БД с записью действия в аудит.
     *
     * @param docId идентификатор документа
     * @param filename имя файла (для читаемого аудита)
     * @param deletedBy идентификатор пользователя, удалившего документ
     */
    public void deleteDocument(UUID docId, String filename, UUID deletedBy) {
        documentDAO.delete(docId);
        //oldValue = имя файла, newValue = короткое описание (чтобы в админке было читаемо)
        auditService.log(AUDIT_ENTITY_DOCUMENTS, docId, "DELETE", deletedBy, filename, "Удалён документ");
    }
}