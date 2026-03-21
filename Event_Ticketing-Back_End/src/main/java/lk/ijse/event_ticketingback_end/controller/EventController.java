package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.EventDto;
import lk.ijse.event_ticketingback_end.service.EventService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lk.ijse.event_ticketingback_end.util.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/event")
@CrossOrigin
public class EventController {

    private final EventService     eventService;
    private final QRCodeGenerator  qrCodeGenerator;

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

    @DeleteMapping("/{eventId}")
    public ResponseEntity<APIResponse<String>> deleteEvent(@PathVariable int eventId) {
        eventService.deleteEvent(eventId);
        return new ResponseEntity<>(new APIResponse<>(200, "Event Deleted", null), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<EventDto>>> getAllEvents() {
        List<EventDto> events = eventService.getAllEvents();
        return new ResponseEntity<>(new APIResponse<>(200, "Success", events), HttpStatus.OK);
    }

    /**
     * GET /api/v1/event/{eventId}/qr
     * Returns a PNG QR code image generated with ZXing.
     * Frontend calls this and sets <img src="..."> to display it.
     *
     * QR encodes: eventId, event_name, location, date
     */
    @GetMapping("/{eventId}/qr")
    public ResponseEntity<byte[]> getEventQR(@PathVariable int eventId) {
        try {
            EventDto event = eventService.getEventById(eventId);

            // Build QR data string
            String qrData = String.format(
                    "{\"eventId\":%d,\"name\":\"%s\",\"location\":\"%s\",\"date\":\"%s\"}",
                    event.getEventId(),
                    event.getEvent_name(),
                    event.getLocation(),
                    event.getDate() != null ? event.getDate().toString() : "TBA"
            );

            // Generate QR as PNG bytes using ZXing
            byte[] qrBytes = qrCodeGenerator.generateQRBytes(qrData, 300);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("[EventQR] Failed to generate QR for eventId={}: {}", eventId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}