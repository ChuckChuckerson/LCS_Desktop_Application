package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.AuditDAO;
import com.lawyercompany.entity.AuditEntry;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuditDAOImpl implements AuditDAO {

    @Override
    public List<AuditEntry> findByEntity(UUID entityId) {
        List<AuditEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE entity_id = ? ORDER BY changed_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entityId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<AuditEntry> findByUser(UUID userId) {
        List<AuditEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE changed_by = ? ORDER BY changed_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public AuditEntry findById(Long id) {
        String sql = "SELECT * FROM audit_log WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<AuditEntry> findAll() {
        List<AuditEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY changed_at DESC";
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
    public void save(AuditEntry entry) {
        String sql = "INSERT INTO audit_log (entity_type, entity_id, operation, changed_by, changed_at, old_value, new_value) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entry.getEntityType());
            stmt.setObject(2, entry.getEntityId());
            stmt.setString(3, entry.getOperation());
            stmt.setObject(4, entry.getChangedBy());
            stmt.setObject(5, entry.getChangedAt() != null ? entry.getChangedAt() : OffsetDateTime.now());
            if (entry.getOldValue() != null) {
                stmt.setString(6, entry.getOldValue());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            if (entry.getNewValue() != null) {
                stmt.setString(7, entry.getNewValue());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            stmt.executeUpdate();
            ResultSet genKeys = stmt.getGeneratedKeys();
            if (genKeys.next()) {
                entry.setId(genKeys.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(AuditEntry entry) {
        //обычно записи аудита не обновляют, но для полноты интерфейса оставим
        String sql = "UPDATE audit_log SET entity_type=?, entity_id=?, operation=?, changed_by=?, changed_at=?, old_value=?, new_value=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getEntityType());
            stmt.setObject(2, entry.getEntityId());
            stmt.setString(3, entry.getOperation());
            stmt.setObject(4, entry.getChangedBy());
            stmt.setObject(5, entry.getChangedAt());
            stmt.setString(6, entry.getOldValue());
            stmt.setString(7, entry.getNewValue());
            stmt.setLong(8, entry.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Long id) {
        //обычно аудит не удаляется, но метод нужен по интерфейсу
        String sql = "DELETE FROM audit_log WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        AuditEntry entry = new AuditEntry();
        entry.setId(rs.getLong("id"));
        entry.setEntityType(rs.getString("entity_type"));
        entry.setEntityId((UUID) rs.getObject("entity_id"));
        entry.setOperation(rs.getString("operation"));
        entry.setChangedBy((UUID) rs.getObject("changed_by"));
        entry.setChangedAt(rs.getObject("changed_at", OffsetDateTime.class));
        entry.setOldValue(rs.getString("old_value"));
        entry.setNewValue(rs.getString("new_value"));
        return entry;
    }
}