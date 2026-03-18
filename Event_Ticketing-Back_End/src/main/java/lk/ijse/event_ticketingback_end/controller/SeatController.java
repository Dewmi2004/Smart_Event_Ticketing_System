package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.SeatDto;
import lk.ijse.event_ticketingback_end.service.SeatService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/seat")
@CrossOrigin
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<APIResponse<String>> saveSeat(@RequestBody SeatDto seatDto) {
        seatService.saveSeat(seatDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Seat Saved", null), HttpStatus.CREATED);
    }

    @PostMapping("/generate/{eventId}")
    public ResponseEntity<APIResponse<String>> generateSeats(@PathVariable int eventId) {
        int count = seatService.generateSeatsForEvent(eventId);
        return new ResponseEntity<>(
                new APIResponse<>(200, count + " seats generated for event " + eventId, null),
                HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<APIResponse<String>> updateSeat(@RequestBody SeatDto seatDto) {
        seatService.updateSeat(seatDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Seat Updated", null), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<APIResponse<String>> deleteSeat(@RequestBody SeatDto seatDto) {
        seatService.deleteSeat(seatDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Seat Deleted", null), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<SeatDto>>> getAllSeats() {
        return new ResponseEntity<>(new APIResponse<>(200, "Success", seatService.getAllSeats()), HttpStatus.OK);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<APIResponse<List<SeatDto>>> getSeatsByEvent(@PathVariable int eventId) {
        return new ResponseEntity<>(new APIResponse<>(200, "Success", seatService.getSeatsByEvent(eventId)), HttpStatus.OK);
    }
}