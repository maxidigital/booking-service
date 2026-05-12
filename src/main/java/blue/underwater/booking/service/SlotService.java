package blue.underwater.booking.service;

import blue.underwater.booking.dto.BookSlotRequest;
import blue.underwater.booking.dto.CreateSlotRequest;
import blue.underwater.booking.dto.SlotResponse;
import blue.underwater.booking.model.Slot;
import blue.underwater.booking.repository.SlotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SlotService {

    private final SlotRepository repository;
    private final EmailNotificationService emailService;
    private final CalendarNotificationService calendarService;

    public SlotService(SlotRepository repository, EmailNotificationService emailService,
                       CalendarNotificationService calendarService) {
        this.repository = repository;
        this.emailService = emailService;
        this.calendarService = calendarService;
    }

    public List<SlotResponse> getAvailable() {
        return repository.findByBookedFalseOrderByDateTimeAsc()
            .stream().map(SlotResponse::from).toList();
    }

    public List<SlotResponse> getAll() {
        return repository.findAllByOrderByDateTimeAsc()
            .stream().map(SlotResponse::from).toList();
    }

    public SlotResponse create(CreateSlotRequest req) {
        if (repository.existsByDateTime(req.getDateTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already exists");
        }
        Slot slot = new Slot();
        slot.setDateTime(req.getDateTime());
        try {
            return SlotResponse.from(repository.save(slot));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already exists");
        }
    }

    @Transactional
    public void delete(UUID id) {
        Slot slot = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already booked");
        }
        repository.delete(slot);
    }

    @Transactional
    public SlotResponse book(UUID id, BookSlotRequest req) {
        Slot slot = repository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already booked");
        }
        slot.setBooked(true);
        slot.setClientName(req.getName());
        slot.setClientEmail(req.getEmail());
        SlotResponse response = SlotResponse.from(repository.save(slot));
        emailService.notifyBooking(req.getName(), req.getEmail(), slot.getDateTime());
        calendarService.notifyCalendar(req.getName(), req.getEmail(), slot.getDateTime());
        return response;
    }
}
