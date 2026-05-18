package com.lawyercompany.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Meeting {
    private UUID id;
    private UUID caseId;
    private OffsetDateTime meetingDate;
    private String location;
    private String notes;
    private UUID createdBy;
    private OffsetDateTime createdAt;

    public Meeting() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public OffsetDateTime getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(OffsetDateTime meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}