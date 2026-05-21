package com.lawyercompany.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Scanner;

public class PasswordGen {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        System.out.print("Введите желаемый пароль: ");
        String rawPassword = in.nextLine();

        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        System.out.println("Хеш пароль: " + hashed);
        System.out.println("Проверка: " + BCrypt.checkpw(rawPassword, hashed));
    }
}