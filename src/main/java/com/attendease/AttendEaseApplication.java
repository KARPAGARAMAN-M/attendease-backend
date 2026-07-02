package com.attendease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SpringBootApplication
public class AttendEaseApplication {

    public static void main(String[] args) {
        System.setProperty("debug", System.getProperty("debug", "false"));
        validateRequiredEnvironment(args);
        SpringApplication.run(AttendEaseApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void validateRequiredEnvironment(String[] args) {
        if (isDevProfile(args)) {
            return;
        }

        List<String> requiredVariables = List.of(
                "DATABASE_HOST",
                "DATABASE_NAME",
                "DB_USERNAME",
                "DB_PASSWORD",
                "JWT_SECRET"
        );

        List<String> missingVariables = requiredVariables.stream()
                .filter(name -> readConfigValue(name).isBlank())
                .toList();

        if (!missingVariables.isEmpty()) {
            throw new IllegalStateException("Missing required Render environment variables: "
                    + String.join(", ", missingVariables));
        }

        if (readConfigValue("CORS_ALLOWED_ORIGINS").isBlank() && readConfigValue("FRONTEND_URL").isBlank()) {
            throw new IllegalStateException("Missing required Render environment variable: CORS_ALLOWED_ORIGINS or FRONTEND_URL");
        }

        if (readConfigValue("JWT_SECRET").getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT_SECRET must be at least 64 bytes for HS512 signing.");
        }
    }

    private static boolean isDevProfile(String[] args) {
        String activeProfiles = readConfigValue("SPRING_PROFILES_ACTIVE") + ","
                + System.getProperty("spring.profiles.active", "") + ","
                + String.join(",", args);

        return Arrays.stream(activeProfiles.split(","))
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .anyMatch(value -> value.equals("dev") || value.equals("--spring.profiles.active=dev"));
    }

    private static String readConfigValue(String name) {
        String systemProperty = System.getProperty(name);
        if (systemProperty != null) {
            return systemProperty.trim();
        }

        String envValue = System.getenv(name);
        return envValue == null ? "" : envValue.trim();
    }
}
