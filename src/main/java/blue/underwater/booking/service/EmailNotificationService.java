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
                date, time,
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
                ? "Booking confirmed — " + date + " " + time + "h"
                : "Reserva confirmada — " + date + " " + time + "h";
            String html = buildHtml(
                en ? "// booking confirmed" : "// reserva confirmada",
                date, time,
                en ? "// name" : "// nombre", clientName,
                clientEmail,
                en ? "See you soon." : "¡Hasta pronto."
            );
            send(clientEmail, subject, html);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Client email confirmation failed", e);
        }
    }

    private String buildHtml(String eyebrow, String date, String time,
                              String nameLabel, String name, String email,
                              String closing) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<link href='https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,300&family=IBM+Plex+Mono:wght@300;400&display=swap' rel='stylesheet'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:#111010;color:#f0ebe3;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#111010;'><tr>" +
            "<td align='center' style='padding:48px 24px;'>" +
            "<table width='100%' style='max-width:460px;' cellpadding='0' cellspacing='0'>" +

            // Gold divider
            "<tr><td style='padding-bottom:24px;'>" +
            "<div style='height:1px;width:36px;background:#b8902a;'></div></td></tr>" +

            // Eyebrow
            "<tr><td style='padding-bottom:24px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:9px;letter-spacing:0.22em;text-transform:uppercase;color:#6a5a48;'>" +
            esc(eyebrow) + "</span></td></tr>" +

            // Date
            "<tr><td style='padding-bottom:6px;'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-style:italic;font-weight:300;font-size:30px;color:#f0ebe3;line-height:1.2;text-transform:capitalize;'>" +
            esc(date) + "</span></td></tr>" +

            // Time
            "<tr><td style='padding-bottom:36px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:15px;color:#b8902a;'>" +
            esc(time) + " h</span></td></tr>" +

            // Divider
            "<tr><td style='height:1px;background:#1e1c1a;padding:0;font-size:0;'>&nbsp;</td></tr>" +

            // Name label
            "<tr><td style='padding-top:24px;padding-bottom:5px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:8px;letter-spacing:0.18em;text-transform:uppercase;color:#6a5a48;'>" +
            esc(nameLabel) + "</span></td></tr>" +

            // Name value
            "<tr><td style='padding-bottom:18px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:13px;color:#c8bfb0;'>" +
            esc(name) + "</span></td></tr>" +

            // Email label
            "<tr><td style='padding-bottom:5px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:8px;letter-spacing:0.18em;text-transform:uppercase;color:#6a5a48;'>// email</span></td></tr>" +

            // Email value
            "<tr><td style='padding-bottom:36px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:13px;color:#c8bfb0;'>" +
            esc(email) + "</span></td></tr>" +

            // Divider
            "<tr><td style='height:1px;background:#1e1c1a;padding:0;font-size:0;'>&nbsp;</td></tr>" +

            // Closing
            "<tr><td style='padding-top:28px;padding-bottom:48px;'>" +
            "<span style='font-family:Cormorant Garamond,Georgia,serif;font-style:italic;font-size:20px;color:#f0ebe3;'>" +
            esc(closing) + "</span></td></tr>" +

            // Footer
            "<tr><td style='border-top:1px solid #1e1c1a;padding-top:20px;'>" +
            "<span style='font-family:IBM Plex Mono,monospace;font-size:8px;letter-spacing:0.1em;color:#4a3e30;'>maxibottazzi.de</span>" +
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
