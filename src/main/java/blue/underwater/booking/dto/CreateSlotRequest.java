package blue.underwater.booking.dto;

import java.time.LocalDateTime;

public class CreateSlotRequest {
    private LocalDateTime dateTime;

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
}
