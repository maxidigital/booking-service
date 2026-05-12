package blue.underwater.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

@Service
public class CalendarNotificationService {

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
        try {
            String end = dateTime.plusMinutes(durationMinutes).toString();
            String body = "{"
                + "\"account\":\"" + escape(calendarAccount) + "\","
                + "\"calendar\":\"" + escape(calendarName) + "\","
                + "\"title\":\"Bodywork — " + escape(clientName) + "\","
                + "\"description\":\"Email: " + escape(clientEmail) + "\","
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
            // Calendar failure must not affect the booking response
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
