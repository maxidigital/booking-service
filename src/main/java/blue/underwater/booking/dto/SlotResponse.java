package blue.underwater.booking.dto;

import blue.underwater.booking.model.Slot;

import java.time.LocalDateTime;
import java.util.UUID;

public class SlotResponse {

    private UUID id;
    private LocalDateTime dateTime;
    private boolean booked;
    private String clientName;
    private String clientEmail;

    public static SlotResponse from(Slot slot) {
        SlotResponse r = new SlotResponse();
        r.id = slot.getId();
        r.dateTime = slot.getDateTime();
        r.booked = slot.isBooked();
        r.clientName = slot.getClientName();
        r.clientEmail = slot.getClientEmail();
        return r;
    }

    public UUID getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public boolean isBooked() { return booked; }
    public String getClientName() { return clientName; }
    public String getClientEmail() { return clientEmail; }
}
