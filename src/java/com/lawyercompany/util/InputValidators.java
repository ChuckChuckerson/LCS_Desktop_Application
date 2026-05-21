package com.lawyercompany.util;

/**
 * Общие правила валидации входных данных (используются в UI и в тестах).
 */
public final class InputValidators {

    private static final String EMAIL_PATTERN = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    private InputValidators() {
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.trim().matches(EMAIL_PATTERN);
    }
}
