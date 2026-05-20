package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.MessageDAO;
import com.lawyercompany.entity.Message;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageDAOImpl implements MessageDAO {

    @Override
    public List<Message> findByCaseId(UUID caseId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE case_id = ? ORDER BY sent_at";
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
    public Message findById(UUID id) {
        String sql = "SELECT * FROM messages WHERE id = ?";
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
    public List<Message> findAll() {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM messages";
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
    public void save(Message msg) {
        String sql = "INSERT INTO messages (id, case_id, sender_id, message_text, sent_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, msg.getId() != null ? msg.getId() : UUID.randomUUID());
            ps.setObject(2, msg.getCaseId());
            ps.setObject(3, msg.getSenderId());
            ps.setString(4, msg.getMessageText());
            ps.setObject(5, msg.getSentAt() != null ? msg.getSentAt() : OffsetDateTime.now());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Message msg) {
        String sql = "UPDATE messages SET case_id=?, sender_id=?, message_text=?, sent_at=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, msg.getCaseId());
            ps.setObject(2, msg.getSenderId());
            ps.setString(3, msg.getMessageText());
            ps.setObject(4, msg.getSentAt());
            ps.setObject(5, msg.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId((UUID) rs.getObject("id"));
        m.setCaseId((UUID) rs.getObject("case_id"));
        m.setSenderId((UUID) rs.getObject("sender_id"));
        m.setMessageText(rs.getString("message_text"));
        m.setSentAt(rs.getObject("sent_at", OffsetDateTime.class));
        return m;
    }
}