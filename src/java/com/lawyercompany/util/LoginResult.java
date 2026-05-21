package com.lawyercompany.util;

import com.lawyercompany.entity.User;

public class LoginResult {
    private final boolean success;
    private final String message;
    private final int remainingAttempts;
    private final User user;

    public LoginResult(boolean success, String message, int remainingAttempts, User user) {
        this.success = success;
        this.message = message;
        this.remainingAttempts = remainingAttempts;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getRemainingAttempts() { return remainingAttempts; }
    public User getUser() { return user; }
}