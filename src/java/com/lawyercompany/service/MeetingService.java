package com.lawyercompany.service;

import com.lawyercompany.dao.MeetingDAO;
import com.lawyercompany.entity.Meeting;
import com.lawyercompany.factory.DAOFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class MeetingService {

    private final MeetingDAO meetingDAO;
    private final AuditService auditService;

    public MeetingService() {
        //встречи привязаны к делу, а создавать их может адвокат, поэтому audit тут прям в тему
        this.meetingDAO = DAOFactory.getMeetingDAO();
        this.auditService = new AuditService();
    }

    /**
     * Создать встречу (назначает адвокат).
     */
    public Meeting createMeeting(UUID caseId, OffsetDateTime meetingDateTime, String location, String notes, UUID createdBy) {
        Meeting meeting = new Meeting();
        //id заранее, чтобы audit_log не потерял связь
        UUID meetingId = UUID.randomUUID();
        meeting.setId(meetingId);
        meeting.setCaseId(caseId);
        meeting.setMeetingDate(meetingDateTime);
        meeting.setLocation(location);
        meeting.setNotes(notes);
        meeting.setCreatedBy(createdBy);
        meeting.setCreatedAt(OffsetDateTime.now());
        meetingDAO.save(meeting);

        //в audit пишем локальное время для человека (в бд хранится OffsetDateTime/UTC)
        String whenLocal = meetingDateTime
                .atZoneSameInstant(ZoneId.systemDefault())
                .toOffsetDateTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        auditService.log("meetings", meetingId, "CREATE", createdBy, null,
                "Назначена встреча: " + whenLocal + " | " + location);
        return meeting;
    }

    /**
     * Получить список встреч по делу.
     */
    public List<Meeting> getMeetingsByCase(UUID caseId) {
        return meetingDAO.findByCaseId(caseId);
    }

    /**
     * Получить встречу по ID.
     */
    public Meeting getMeetingById(UUID meetingId) {
        return meetingDAO.findById(meetingId);
    }

    /**
     * Обновить данные встречи.
     */
    public void updateMeeting(UUID meetingId, OffsetDateTime meetingDateTime, String location, String notes) {
        Meeting meeting = meetingDAO.findById(meetingId);
        if (meeting == null) {
            throw new IllegalArgumentException("Встреча не найдена");
        }
        meeting.setMeetingDate(meetingDateTime);
        meeting.setLocation(location);
        meeting.setNotes(notes);
        meetingDAO.update(meeting);
    }

    /**
     * Удалить встречу из БД.
     */
    public void deleteMeeting(UUID meetingId) {
        meetingDAO.delete(meetingId);
    }
}