package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.LawyerDAO;
import com.lawyercompany.entity.Lawyer;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LawyerDAOImpl implements LawyerDAO {

    @Override
    public Optional<Lawyer> findByUserId(UUID userId) {
        String sql = "SELECT * FROM lawyers WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToLawyer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Lawyer findById(UUID id) {
        String sql = "SELECT * FROM lawyers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToLawyer(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Lawyer> findAll() {
        List<Lawyer> lawyers = new ArrayList<>();
        String sql = "SELECT * FROM lawyers";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                lawyers.add(mapResultSetToLawyer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lawyers;
    }

    @Override
    public void save(Lawyer lawyer) {
        String sql = "INSERT INTO lawyers (id, user_id, phone, specialization, license_number, office_address, experience_years) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, lawyer.getId() != null ? lawyer.getId() : UUID.randomUUID());
            ps.setObject(2, lawyer.getUserId());
            ps.setString(3, lawyer.getPhone());
            ps.setString(4, lawyer.getSpecialization());
            ps.setString(5, lawyer.getLicenseNumber());
            ps.setString(6, lawyer.getOfficeAddress());
            if (lawyer.getExperienceYears() != null) {
                ps.setInt(7, lawyer.getExperienceYears());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Lawyer lawyer) {
        String sql = "UPDATE lawyers SET user_id=?, phone=?, specialization=?, license_number=?, office_address=?, experience_years=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, lawyer.getUserId());
            ps.setString(2, lawyer.getPhone());
            ps.setString(3, lawyer.getSpecialization());
            ps.setString(4, lawyer.getLicenseNumber());
            ps.setString(5, lawyer.getOfficeAddress());
            if (lawyer.getExperienceYears() != null) {
                ps.setInt(6, lawyer.getExperienceYears());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setObject(7, lawyer.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM lawyers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Lawyer mapResultSetToLawyer(ResultSet rs) throws SQLException {
        Lawyer l = new Lawyer();
        l.setId((UUID) rs.getObject("id"));
        l.setUserId((UUID) rs.getObject("user_id"));
        l.setPhone(rs.getString("phone"));
        l.setSpecialization(rs.getString("specialization"));
        l.setLicenseNumber(rs.getString("license_number"));
        l.setOfficeAddress(rs.getString("office_address"));
        int exp = rs.getInt("experience_years");
        l.setExperienceYears(rs.wasNull() ? null : exp);
        return l;
    }
}