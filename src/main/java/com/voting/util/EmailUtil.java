package com.voting.util;

import com.voting.config.AppConfig;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public final class EmailUtil {
    private EmailUtil() {
    }

    public static void sendOtp(String recipientEmail, String recipientName, String otp) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalStateException("No registered email address is available for OTP delivery.");
        }

        String username = AppConfig.get("email.username", "EMAIL_USER");
        String password = AppConfig.get("email.password", "EMAIL_PASS");
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalStateException("Email OTP service is not configured. Set EMAIL_USER and EMAIL_PASS.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", valueOrDefault(AppConfig.get("email.smtp.host"), "smtp.gmail.com"));
        props.put("mail.smtp.port", valueOrDefault(AppConfig.get("email.smtp.port"), "587"));
        props.put("mail.smtp.ssl.trust", valueOrDefault(AppConfig.get("email.smtp.host"), "smtp.gmail.com"));

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "SEC Voting Portal"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()));
            message.setSubject("Your SEC Voting OTP");
            message.setText(buildBody(recipientName, otp));
            Transport.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send OTP email. Please check SMTP configuration.", ex);
        }
    }

    public static void sendNotification(String recipientEmail, String recipientName, String subject, String body) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }
        send(recipientEmail, recipientName, subject, body);
    }

    private static void send(String recipientEmail, String recipientName, String subject, String body) {
        String username = AppConfig.get("email.username", "EMAIL_USER");
        String password = AppConfig.get("email.password", "EMAIL_PASS");
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalStateException("Email service is not configured. Set EMAIL_USER and EMAIL_PASS.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", valueOrDefault(AppConfig.get("email.smtp.host"), "smtp.gmail.com"));
        props.put("mail.smtp.port", valueOrDefault(AppConfig.get("email.smtp.port"), "587"));
        props.put("mail.smtp.ssl.trust", valueOrDefault(AppConfig.get("email.smtp.host"), "smtp.gmail.com"));

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "SEC Voting Portal"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()));
            message.setSubject(subject == null ? "SEC Voting Portal Notification" : subject);
            String displayName = isBlank(recipientName) ? "Voter" : recipientName.trim();
            message.setText("Hello " + displayName + ",\n\n" + (body == null ? "" : body) + "\n\nState Election Commission");
            Transport.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send notification email. Please check SMTP configuration.", ex);
        }
    }

    private static String buildBody(String recipientName, String otp) {
        String displayName = isBlank(recipientName) ? "Voter" : recipientName.trim();
        return "Hello " + displayName + ",\n\n"
                + "Your OTP for SEC Voting Portal is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes. Do not share it with anyone.\n\n"
                + "State Election Commission";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }
}
