package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.EventDto;
import lk.ijse.event_ticketingback_end.service.EventService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/event")
@CrossOrigin
public class EventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<APIResponse<String>> saveEvent(@RequestBody EventDto eventDto) {
        eventService.saveEvent(eventDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Event Saved", null), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<APIResponse<String>> updateEvent(@RequestBody EventDto eventDto) {
        eventService.updateEvent(eventDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Event Updated", null), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<APIResponse<String>> deleteEvent(@RequestBody EventDto eventDto) {
        eventService.deleteEvent(eventDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Event Deleted", null), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<EventDto>>> getAllEvents() {
        List<EventDto> events = eventService.getAllEvents();
        return new ResponseEntity<>(new APIResponse<>(200, "Success", events), HttpStatus.OK);
    }
}
