package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.BookingDto;
import lk.ijse.event_ticketingback_end.service.BookingService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/booking")
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<APIResponse<BookingDto>> saveBooking(@RequestBody BookingDto bookingDto) {
        BookingDto saved = bookingService.saveBooking(bookingDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Booking Created", saved), HttpStatus.CREATED);
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<APIResponse<String>> updateStatus(
            @PathVariable int bookingId,
            @RequestBody Map<String, String> body) {
        bookingService.updateBookingStatus(bookingId, body.get("status"));
        return new ResponseEntity<>(new APIResponse<>(200, "Status Updated", null), HttpStatus.OK);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<APIResponse<String>> deleteBooking(@PathVariable int bookingId) {
        bookingService.deleteBooking(bookingId);
        return new ResponseEntity<>(new APIResponse<>(200, "Booking Deleted", null), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<BookingDto>>> getAllBookings() {
        return new ResponseEntity<>(new APIResponse<>(200, "Success", bookingService.getAllBookings()), HttpStatus.OK);
    }
}