package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.CaseDAO;
import com.lawyercompany.entity.Case;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseDAOImpl implements CaseDAO {

    @Override
    public List<Case> findByClientId(UUID clientId) {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM cases WHERE client_id = ? AND is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, clientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cases.add(mapResultSetToCase(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cases;
    }

    @Override
    public List<Case> findByLawyerId(UUID lawyerId) {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM cases WHERE lawyer_id = ? AND is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, lawyerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cases.add(mapResultSetToCase(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cases;
    }

    @Override
    public Case findById(UUID id) {
        String sql = "SELECT * FROM cases WHERE id = ? AND is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToCase(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Case> findAll() {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM cases WHERE is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cases.add(mapResultSetToCase(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cases;
    }

    @Override
    public void save(Case c) {
        String sql = "INSERT INTO cases (id, case_number, title, description, client_id, lawyer_id, status, created_at, updated_at, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, c.getId() != null ? c.getId() : UUID.randomUUID());
            ps.setString(2, c.getCaseNumber());
            ps.setString(3, c.getTitle());
            ps.setString(4, c.getDescription());
            ps.setObject(5, c.getClientId());
            ps.setObject(6, c.getLawyerId());
            ps.setString(7, c.getStatus());
            ps.setObject(8, c.getCreatedAt() != null ? c.getCreatedAt() : OffsetDateTime.now());
            ps.setObject(9, c.getUpdatedAt() != null ? c.getUpdatedAt() : OffsetDateTime.now());
            ps.setBoolean(10, c.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Case c) {
        String sql = "UPDATE cases SET case_number=?, title=?, description=?, client_id=?, lawyer_id=?, status=?, updated_at=?, is_deleted=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCaseNumber());
            ps.setString(2, c.getTitle());
            ps.setString(3, c.getDescription());
            ps.setObject(4, c.getClientId());
            ps.setObject(5, c.getLawyerId());
            ps.setString(6, c.getStatus());
            ps.setObject(7, OffsetDateTime.now());
            ps.setBoolean(8, c.isDeleted());
            ps.setObject(9, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "UPDATE cases SET is_deleted = true, updated_at = now() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Case mapResultSetToCase(ResultSet rs) throws SQLException {
        Case c = new Case();
        c.setId((UUID) rs.getObject("id"));
        c.setCaseNumber(rs.getString("case_number"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setClientId((UUID) rs.getObject("client_id"));
        c.setLawyerId((UUID) rs.getObject("lawyer_id"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        c.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        c.setDeleted(rs.getBoolean("is_deleted"));
        return c;
    }
}