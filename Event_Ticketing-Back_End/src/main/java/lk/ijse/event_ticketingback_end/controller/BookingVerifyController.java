package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.entity.Booking;
import lk.ijse.event_ticketingback_end.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingVerifyController {

    private final BookingRepository bookingRepository;

    @PostMapping("/verify/{bookingId}")
    public ResponseEntity<Map<String, Object>> verifyTicket(@PathVariable int bookingId) {

        log.info("[Verify] Scanning bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElse(null);


        if (booking == null) {
            log.warn("[Verify] Booking not found: {}", bookingId);
            return ResponseEntity.ok(Map.of(
                    "valid",   false,
                    "message", "Booking not found"
            ));
        }

        String status = booking.getStatus();
        log.info("[Verify] bookingId={} currentStatus={}", bookingId, status);


        if ("Refunded".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(Map.of(
                    "valid",   false,
                    "message", "Ticket has been refunded",
                    "status",  status
            ));
        }


        if ("Cancelled".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(Map.of(
                    "valid",   false,
                    "message", "Ticket has been cancelled",
                    "status",  status
            ));
        }


        if ("Used".equalsIgnoreCase(status)) {
            log.warn("[Verify] Ticket already used: {}", bookingId);
            return ResponseEntity.ok(Map.of(
                    "valid",   false,
                    "message", "Ticket already used",
                    "status",  status
            ));
        }


        if ("Confirmed".equalsIgnoreCase(status)) {
            booking.setStatus("Used");
            bookingRepository.save(booking);
            log.info("[Verify] Entry allowed, marked as Used: {}", bookingId);
            return ResponseEntity.ok(Map.of(
                    "valid",   true,
                    "message", "Entry allowed",
                    "status",  "Used"
            ));
        }


        log.warn("[Verify] Invalid status '{}' for bookingId={}", status, bookingId);
        return ResponseEntity.ok(Map.of(
                "valid",   false,
                "message", "Invalid ticket status: " + status,
                "status",  status
        ));
    }
}