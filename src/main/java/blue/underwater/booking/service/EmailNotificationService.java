package blue.underwater.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailNotificationService {

    private static final Logger LOG = Logger.getLogger(EmailNotificationService.class.getName());

    @Value("${owner.email}")
    private String ownerEmail;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void notifyBooking(String clientName, String clientEmail, LocalDateTime dateTime, String lang) {
        boolean en = "en".equals(lang);

        String dateEs = capitalize(dateTime.format(
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es"))));
        String dateEn = dateTime.format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH));
        String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        sendOwnerNotification(clientName, clientEmail, dateEs, time);
        sendClientConfirmation(clientName, clientEmail, en ? dateEn : dateEs, time, en);
    }

    private void sendOwnerNotification(String clientName, String clientEmail, String date, String time) {
        try {
            String subject = "Nueva reserva — " + clientName + ", " + date + " " + time + "h";
            String html = buildHtml(
                "// nueva reserva",
                null, date, time,
                "// nombre", clientName,
                clientEmail,
                "Hasta pronto."
            );
            send(ownerEmail, subject, html);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Owner email notification failed", e);
        }
    }

    private void sendClientConfirmation(String clientName, String clientEmail, String date, String time, boolean en) {
        try {
            String subject = en
                ? "Your booking — " + date + " " + time + "h"
                : "Reserva confirmada — " + date + " " + time + "h";
            String greeting = en ? "Thank you for your booking." : "Muchas gracias por tu reserva.";
            String closing = en
                ? "See you on " + date + "."
                : "Nos vemos el " + date.toLowerCase() + ".";
            String html = buildHtml(
                en ? "// booking confirmed" : "// reserva confirmada",
                greeting, date, time,
                en ? "// name" : "// nombre", clientName,
                null,
                closing
            );
            send(clientEmail, subject, html);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Client email confirmation failed", e);
        }
    }

    private String buildHtml(String eyebrow, String greeting, String date, String time,
                              String nameLabel, String name, String email,
                              String closing) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<link href='https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,300&family=IBM+Plex+Mono:wght@300;400&display=swap' rel='stylesheet'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:#f8f5f0;color:#3a2e22;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f8f5f0;'><tr>" +
            "<td align='center' style='padding:48px 24px;'>" +
            "<table width='100%' style='max-width:480px;' cellpadding='0' cellspacing='0'>" +

            // Gold divider
            "<tr><td style='padding-bottom:24px;'>" +
            "<div style='height:1px;width:36px;background:#b8902a;'></div></td></tr>" +

            // Eyebrow
            "<tr><td style='padding-bottom:16px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:11px;letter-spacing:0.22em;text-transform:uppercase;color:#a08878;'>" +
            esc(eyebrow) + "</span></td></tr>" +

            // Greeting
            (greeting != null ? (
            "<tr><td style='padding-bottom:28px;'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-style:italic;font-weight:300;font-size:22px;color:#6e6050;'>" +
            esc(greeting) + "</span></td></tr>"
            ) : "") +

            // Date
            "<tr><td style='padding-bottom:8px;'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-style:italic;font-weight:300;font-size:38px;color:#3a2e22;line-height:1.2;text-transform:capitalize;'>" +
            esc(date) + "</span></td></tr>" +

            // Time
            "<tr><td style='padding-bottom:40px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:18px;color:#b8902a;'>" +
            esc(time) + " h</span></td></tr>" +

            // Divider
            "<tr><td style='height:1px;background:rgba(60,50,40,0.10);padding:0;font-size:0;'>&nbsp;</td></tr>" +

            // Name label
            "<tr><td style='padding-top:28px;padding-bottom:6px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:10px;letter-spacing:0.18em;text-transform:uppercase;color:#a08878;'>" +
            esc(nameLabel) + "</span></td></tr>" +

            // Name value
            "<tr><td style='" + (email != null ? "padding-bottom:20px;" : "padding-bottom:40px;") + "'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-size:20px;color:#4a3e32;'>" +
            esc(name) + "</span></td></tr>" +

            // Email (optional)
            (email != null ? (
            "<tr><td style='padding-bottom:6px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:10px;letter-spacing:0.18em;text-transform:uppercase;color:#a08878;'>// email</span></td></tr>" +
            "<tr><td style='padding-bottom:40px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:14px;color:#4a3e32;'>" +
            esc(email) + "</span></td></tr>"
            ) : "") +

            // Divider
            "<tr><td style='height:1px;background:rgba(60,50,40,0.10);padding:0;font-size:0;'>&nbsp;</td></tr>" +

            // Closing
            "<tr><td style='padding-top:32px;padding-bottom:48px;'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-style:italic;font-size:24px;color:#3a2e22;'>" +
            esc(closing) + "</span></td></tr>" +

            // Footer
            "<tr><td style='border-top:1px solid rgba(60,50,40,0.10);padding-top:20px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:9px;letter-spacing:0.1em;color:#a08878;'>maxibottazzi.de</span>" +
            "</td></tr>" +

            "</table></td></tr></table></body></html>";
    }

    private void send(String to, String subject, String html) throws Exception {
        String body = "{"
            + "\"app\":\"booking-service\","
            + "\"from\":\"noreply@maxibottazzi.de\","
            + "\"fromName\":\"maxibottazzi.de\","
            + "\"to\":\"" + escJson(to) + "\","
            + "\"subject\":\"" + escJson(subject) + "\","
            + "\"html\":\"" + escJson(html) + "\""
            + "}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(emailServiceUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // HTML-safe: only used inside HTML text nodes, no special chars expected
    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    // JSON string escaping
    private String escJson(String s) {
        return s == null ? "" : s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
    }
}
