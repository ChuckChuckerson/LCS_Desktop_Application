package com.lawyercompany.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Document {
    private UUID id;
    private UUID caseId;
    private String filename;
    private UUID uploadedBy;
    private OffsetDateTime uploadedAt;
    private byte[] fileData;

    public Document() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
}