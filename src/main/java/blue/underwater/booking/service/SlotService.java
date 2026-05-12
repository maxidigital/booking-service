package blue.underwater.booking.service;

import blue.underwater.booking.dto.BookSlotRequest;
import blue.underwater.booking.dto.CreateSlotRequest;
import blue.underwater.booking.dto.SlotResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SlotService {

    private static final Logger LOG = Logger.getLogger(SlotService.class.getName());

    @Value("${calendar.service.base.url}")
    private String calendarServiceBase;

    @Value("${session.duration.minutes:120}")
    private int durationMinutes;

    @Value("${google.calendar.account:maxi}")
    private String calendarAccount;

    @Value("${google.calendar.name:masajes}")
    private String calendarName;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final EmailNotificationService emailService;

    public SlotService(EmailNotificationService emailService, ObjectMapper mapper) {
        this.emailService = emailService;
        this.mapper = mapper;
    }

    public List<SlotResponse> getAvailable() {
        return fetchSlots(false);
    }

    public List<SlotResponse> getAll() {
        return fetchSlots(true);
    }

    private List<SlotResponse> fetchSlots(boolean all) {
        try {
            String url = calendarServiceBase + "/slots?account=" + calendarAccount
                + "&calendar=" + calendarName + "&all=" + all;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Calendar service error");
            return mapper.readValue(res.body(), new TypeReference<List<SlotResponse>>() {});
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch slots", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch slots");
        }
    }

    public SlotResponse create(CreateSlotRequest req) {
        try {
            LocalDateTime start = req.getDateTime();
            LocalDateTime end = start.plusMinutes(durationMinutes);
            String body = "{\"account\":\"" + calendarAccount + "\","
                + "\"calendar\":\"" + calendarName + "\","
                + "\"start\":\"" + start + "\","
                + "\"end\":\"" + end + "\","
                + "\"timeZone\":\"Europe/Berlin\"}";
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(calendarServiceBase + "/slots"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 409) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already exists");
            if (res.statusCode() != 201) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Calendar service error");
            return mapper.readValue(res.body(), SlotResponse.class);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create slot", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create slot");
        }
    }

    public void delete(String id) {
        try {
            String url = calendarServiceBase + "/slots/" + id
                + "?account=" + calendarAccount + "&calendar=" + calendarName;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).DELETE().build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 409) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a booked slot");
            if (res.statusCode() != 204) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to delete slot", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete slot");
        }
    }

    public SlotResponse book(String id, BookSlotRequest req) {
        try {
            String body = "{\"account\":\"" + escape(calendarAccount) + "\","
                + "\"calendar\":\"" + escape(calendarName) + "\","
                + "\"name\":\"" + escape(req.getName()) + "\","
                + "\"email\":\"" + escape(req.getEmail()) + "\"}";
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(calendarServiceBase + "/slots/" + id + "/book"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (res.statusCode() == 409) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already booked");
            if (res.statusCode() != 200) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Calendar service error");
            SlotResponse slotResponse = mapper.readValue(res.body(), SlotResponse.class);
            emailService.notifyBooking(req.getName(), req.getEmail(), slotResponse.getDateTime());
            return slotResponse;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to book slot", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to book slot");
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
