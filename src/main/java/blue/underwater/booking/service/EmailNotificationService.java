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

@Service
public class EmailNotificationService {

    @Value("${owner.email}")
    private String ownerEmail;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void notifyBooking(String clientName, String clientEmail, LocalDateTime dateTime) {
        try {
            String formattedDate = dateTime.format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", new Locale("es"))
            );

            String html = "<p>Nueva reserva recibida:</p>"
                + "<p><strong>Nombre:</strong> " + escape(clientName) + "<br>"
                + "<strong>Email:</strong> " + escape(clientEmail) + "<br>"
                + "<strong>Fecha:</strong> " + formattedDate + "</p>";

            String body = "{"
                + "\"app\":\"booking-service\","
                + "\"from\":\"noreply@maxibottazzi.de\","
                + "\"fromName\":\"Maxi Bottazzi Booking\","
                + "\"to\":\"" + ownerEmail + "\","
                + "\"subject\":\"Nueva reserva — " + escape(clientName) + " el " + formattedDate + "\","
                + "\"html\":\"" + escape(html) + "\""
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(emailServiceUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Email failure must not affect the booking response
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
