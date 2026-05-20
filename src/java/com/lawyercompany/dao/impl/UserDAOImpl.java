package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.UserDAO;
import com.lawyercompany.entity.User;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserDAOImpl implements UserDAO {

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public User findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ? AND is_deleted = false";
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
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_deleted = false";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public void save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO users (id, username, password_hash, full_name, email, role, avatar_data, created_at, updated_at, is_deleted, is_blocked) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getRole());
            stmt.setBytes(7, user.getAvatarData());
            stmt.setObject(8, user.getCreatedAt() != null ? user.getCreatedAt() : OffsetDateTime.now());
            stmt.setObject(9, user.getUpdatedAt() != null ? user.getUpdatedAt() : OffsetDateTime.now());
            stmt.setBoolean(10, user.isDeleted());
            stmt.setBoolean(11, user.isBlocked());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username=?, password_hash=?, full_name=?, email=?, role=?, avatar_data=?, updated_at=?, is_deleted=?, is_blocked=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole());
            stmt.setBytes(6, user.getAvatarData());
            stmt.setObject(7, OffsetDateTime.now());
            stmt.setBoolean(8, user.isDeleted());
            stmt.setBoolean(9, user.isBlocked());
            stmt.setObject(10, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "UPDATE users SET is_deleted = true, updated_at = now() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId((UUID) rs.getObject("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setAvatarData(rs.getBytes("avatar_data"));
        user.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        user.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        user.setDeleted(rs.getBoolean("is_deleted"));
        user.setBlocked(rs.getBoolean("is_blocked"));
        return user;
    }
}