package com.example.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private String googleClientId;
    private List<String> adminEmails = new ArrayList<>();
    private List<String> staffEmails = new ArrayList<>();

    public String getGoogleClientId() {
        return googleClientId;
    }

    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public List<String> getAdminEmails() {
        return adminEmails;
    }

    public void setAdminEmails(List<String> adminEmails) {
        this.adminEmails = normalizeEmails(adminEmails);
    }

    public List<String> getStaffEmails() {
        return staffEmails;
    }

    public void setStaffEmails(List<String> staffEmails) {
        this.staffEmails = normalizeEmails(staffEmails);
    }

    public boolean isAdmin(String email) {
        return containsEmail(adminEmails, email);
    }

    public boolean isStaff(String email) {
        return containsEmail(staffEmails, email);
    }

    public String resolveRole(String email) {
        if (!StringUtils.hasText(email)) {
            return "USER";
        }
        if (isAdmin(email)) {
            return "ADMIN";
        }
        if (isStaff(email)) {
            return "STAFF";
        }
        return "USER";
    }

    private List<String> normalizeEmails(List<String> emails) {
        List<String> normalized = new ArrayList<>();
        if (emails == null) {
            return normalized;
        }
        for (String email : emails) {
            if (StringUtils.hasText(email)) {
                normalized.add(email.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private boolean containsEmail(List<String> emails, String email) {
        return StringUtils.hasText(email)
                && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
