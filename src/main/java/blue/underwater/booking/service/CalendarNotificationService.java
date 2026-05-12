package blue.underwater.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CalendarNotificationService {

    private static final Logger LOG = Logger.getLogger(CalendarNotificationService.class.getName());

    @Value("${calendar.service.url}")
    private String calendarServiceUrl;

    @Value("${session.duration.minutes:120}")
    private int durationMinutes;

    @Value("${google.calendar.account:maxi}")
    private String calendarAccount;

    @Value("${google.calendar.name:masajes}")
    private String calendarName;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void notifyCalendar(String clientName, String clientEmail, LocalDateTime dateTime) {
        LOG.info("Notifying calendar-service at: " + calendarServiceUrl);
        try {
            String end = dateTime.plusMinutes(durationMinutes).toString();
            String body = "{"
                + "\"account\":\"" + escape(calendarAccount) + "\","
                + "\"calendar\":\"" + escape(calendarName) + "\","
                + "\"title\":\"*Masaje " + escape(clientName) + "\","
                + "\"description\":\"email:" + escape(clientEmail) + "\","
                + "\"start\":\"" + dateTime + "\","
                + "\"end\":\"" + end + "\","
                + "\"timeZone\":\"Europe/Berlin\""
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(calendarServiceUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Calendar notification failed", e);
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
