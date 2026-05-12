package blue.underwater.booking.dto;

import java.time.LocalDateTime;

public class SlotResponse {

    private String id;
    private LocalDateTime dateTime;
    private boolean booked;
    private String clientName;
    private String clientEmail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
}
