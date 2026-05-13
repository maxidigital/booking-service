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
        String formattedDateEs = capitalize(dateTime.format(
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", new Locale("es"))));
        String formattedDateEn = dateTime.format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' HH:mm", Locale.ENGLISH));
        String formattedDate = en ? formattedDateEn : formattedDateEs;

        sendOwnerNotification(clientName, clientEmail, formattedDateEs);
        sendClientConfirmation(clientName, clientEmail, formattedDate, en);
    }

    private void sendOwnerNotification(String clientName, String clientEmail, String formattedDate) {
        try {
            String html = "<p>Nueva reserva recibida:</p>"
                + "<p><strong>Nombre:</strong> " + escape(clientName) + "<br>"
                + "<strong>Email:</strong> " + escape(clientEmail) + "<br>"
                + "<strong>Fecha:</strong> " + formattedDate + "</p>";
            send(ownerEmail, "Nueva reserva — " + escape(clientName) + " el " + formattedDate, html);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Owner email notification failed", e);
        }
    }

    private void sendClientConfirmation(String clientName, String clientEmail, String formattedDate, boolean en) {
        try {
            String subject, html;
            if (en) {
                subject = "Booking confirmed — " + formattedDate;
                html = "<p>Hi " + escape(clientName) + ",</p>"
                    + "<p>Your bodywork session is confirmed for <strong>" + formattedDate + "</strong>.</p>"
                    + "<p>See you soon!</p>"
                    + "<p>Maxi Bottazzi · Bodywork Berlin</p>";
            } else {
                subject = "Reserva confirmada — " + formattedDate;
                html = "<p>Hola " + escape(clientName) + ",</p>"
                    + "<p>Tu sesión de bodywork está confirmada para el <strong>" + formattedDate + "</strong>.</p>"
                    + "<p>¡Hasta pronto!</p>"
                    + "<p>Maxi Bottazzi · Bodywork Berlin</p>";
            }
            send(clientEmail, subject, html);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Client email confirmation failed", e);
        }
    }

    private void send(String to, String subject, String html) throws Exception {
        String body = "{"
            + "\"app\":\"booking-service\","
            + "\"from\":\"noreply@maxibottazzi.de\","
            + "\"fromName\":\"Maxi Bottazzi Booking\","
            + "\"to\":\"" + escape(to) + "\","
            + "\"subject\":\"" + escape(subject) + "\","
            + "\"html\":\"" + escape(html) + "\""
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

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
