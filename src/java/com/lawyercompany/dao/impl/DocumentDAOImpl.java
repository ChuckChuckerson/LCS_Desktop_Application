package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.DocumentDAO;
import com.lawyercompany.entity.Document;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DocumentDAOImpl implements DocumentDAO {

    @Override
    public List<Document> findByCaseId(UUID caseId) {
        List<Document> list = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, caseId);
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
    public Document findById(UUID id) {
        String sql = "SELECT * FROM documents WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
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
    public List<Document> findAll() {
        List<Document> list = new ArrayList<>();
        String sql = "SELECT * FROM documents";
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
    public void save(Document doc) {
        String sql = "INSERT INTO documents (id, case_id, filename, uploaded_by, uploaded_at, file_data) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, doc.getId() != null ? doc.getId() : UUID.randomUUID());
            stmt.setObject(2, doc.getCaseId());
            stmt.setString(3, doc.getFilename());
            stmt.setObject(4, doc.getUploadedBy());
            stmt.setObject(5, doc.getUploadedAt() != null ? doc.getUploadedAt() : OffsetDateTime.now());
            stmt.setBytes(6, doc.getFileData());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Document doc) {
        String sql = "UPDATE documents SET case_id=?, filename=?, uploaded_by=?, uploaded_at=?, file_data=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, doc.getCaseId());
            stmt.setString(2, doc.getFilename());
            stmt.setObject(3, doc.getUploadedBy());
            stmt.setObject(4, doc.getUploadedAt());
            stmt.setBytes(5, doc.getFileData());
            stmt.setObject(6, doc.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Document mapRow(ResultSet rs) throws SQLException {
        Document doc = new Document();
        doc.setId((UUID) rs.getObject("id"));
        doc.setCaseId((UUID) rs.getObject("case_id"));
        doc.setFilename(rs.getString("filename"));
        doc.setUploadedBy((UUID) rs.getObject("uploaded_by"));
        doc.setUploadedAt(rs.getObject("uploaded_at", OffsetDateTime.class));
        doc.setFileData(rs.getBytes("file_data"));
        return doc;
    }
}