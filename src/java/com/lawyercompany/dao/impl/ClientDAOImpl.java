package com.lawyercompany.dao.impl;

import com.lawyercompany.dao.ClientDAO;
import com.lawyercompany.entity.Client;
import com.lawyercompany.up.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClientDAOImpl implements ClientDAO {
    public Optional<Client> findByUserId(UUID userId) {
        String sql = "SELECT * FROM clients WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToClient(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Client findById(UUID id) {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToClient(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Client> findAll() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clients;
    }

    @Override
    public void save(Client client) {
        String sql = "INSERT INTO clients (id, user_id, phone, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, client.getId() != null ? client.getId() : UUID.randomUUID());
            ps.setObject(2, client.getUserId());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Client client) {
        String sql = "UPDATE clients SET user_id=?, phone=?, address=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, client.getUserId());
            stmt.setString(2, client.getPhone());
            stmt.setString(3, client.getAddress());
            stmt.setObject(4, client.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Client mapResultSetToClient(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId((UUID) rs.getObject("id"));
        c.setUserId((UUID) rs.getObject("user_id"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        return c;
    }
}
