package com.lawyercompany.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_LAWYER = "lawyer";
    public static final String ROLE_CLIENT = "client";

    private UUID id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    /** Значения как в БД: admin, lawyer, client */
    private String role;
    private byte[] avatarData;           // новые данные
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private boolean isDeleted;
    private boolean isBlocked;

    // Конструкторы, геттеры и сеттеры
    public User() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public byte[] getAvatarData() { return avatarData; }
    public void setAvatarData(byte[] avatarData) { this.avatarData = avatarData; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}