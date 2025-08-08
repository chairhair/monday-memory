package com.monday.monday_backend.auth.validation;

public class ValidationUtils {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=<>?]).{8,}$";

    public static boolean isEmailLegitimate(String emailAddress) {
        return emailAddress != null && EMAIL_REGEX.matches(emailAddress);
    }

    public static boolean isPasswordLegitimate(String password) {
        return password != null && PASSWORD_REGEX.matches(password);
    }
}
