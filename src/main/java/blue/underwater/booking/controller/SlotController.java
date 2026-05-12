package blue.underwater.booking.controller;

import blue.underwater.booking.dto.BookSlotRequest;
import blue.underwater.booking.dto.CreateSlotRequest;
import blue.underwater.booking.dto.SlotResponse;
import blue.underwater.booking.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @GetMapping
    public List<SlotResponse> getAvailable() {
        return service.getAvailable();
    }

    @GetMapping("/all")
    public List<SlotResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@RequestBody CreateSlotRequest req) {
        return service.create(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @PostMapping("/{id}/book")
    public SlotResponse book(@PathVariable String id, @RequestBody BookSlotRequest req) {
        return service.book(id, req);
    }
}
