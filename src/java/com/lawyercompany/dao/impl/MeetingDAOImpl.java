package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.MeetingDAO;
import com.lawyercompany.entity.Meeting;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MeetingDAOImpl implements MeetingDAO {

    @Override
    public List<Meeting> findByCaseId(UUID caseId) {
        List<Meeting> list = new ArrayList<>();
        String sql = "SELECT * FROM meetings WHERE case_id = ? ORDER BY meeting_date";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, caseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Meeting findById(UUID id) {
        String sql = "SELECT * FROM meetings WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Meeting> findAll() {
        List<Meeting> list = new ArrayList<>();
        String sql = "SELECT * FROM meetings";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void save(Meeting meeting) {
        String sql = "INSERT INTO meetings (id, case_id, meeting_date, location, notes, created_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, meeting.getId() != null ? meeting.getId() : UUID.randomUUID());
            ps.setObject(2, meeting.getCaseId());
            ps.setObject(3, meeting.getMeetingDate());
            ps.setString(4, meeting.getLocation());
            ps.setString(5, meeting.getNotes());
            ps.setObject(6, meeting.getCreatedBy());
            ps.setObject(7, meeting.getCreatedAt() != null ? meeting.getCreatedAt() : OffsetDateTime.now());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Meeting meeting) {
        String sql = "UPDATE meetings SET case_id=?, meeting_date=?, location=?, notes=?, created_by=?, created_at=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, meeting.getCaseId());
            ps.setObject(2, meeting.getMeetingDate());
            ps.setString(3, meeting.getLocation());
            ps.setString(4, meeting.getNotes());
            ps.setObject(5, meeting.getCreatedBy());
            ps.setObject(6, meeting.getCreatedAt());
            ps.setObject(7, meeting.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM meetings WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Meeting mapRow(ResultSet rs) throws SQLException {
        Meeting m = new Meeting();
        m.setId((UUID) rs.getObject("id"));
        m.setCaseId((UUID) rs.getObject("case_id"));
        m.setMeetingDate(rs.getObject("meeting_date", OffsetDateTime.class));
        m.setLocation(rs.getString("location"));
        m.setNotes(rs.getString("notes"));
        m.setCreatedBy((UUID) rs.getObject("created_by"));
        m.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return m;
    }
}