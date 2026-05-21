package com.lawyercompany.util;

public class PasswordEncoderFactory {
    public static PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder();
    }
}