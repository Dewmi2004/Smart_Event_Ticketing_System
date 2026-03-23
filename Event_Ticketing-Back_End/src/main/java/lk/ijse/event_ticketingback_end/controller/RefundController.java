package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.RefundDto;
import lk.ijse.event_ticketingback_end.service.RefundService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/refund")
@CrossOrigin
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/request")
    public ResponseEntity<APIResponse<RefundDto>> requestRefund(
            @RequestBody Map<String, String> body) {

        int    bookingId = Integer.parseInt(body.get("bookingId"));
        String reason    = body.getOrDefault("reason", "Customer requested refund");

        RefundDto dto = refundService.requestRefund(bookingId, reason);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Refund request submitted", dto),
                HttpStatus.CREATED);
    }

    @PostMapping("/process/{refundId}")
    public ResponseEntity<APIResponse<RefundDto>> processRefund(
            @PathVariable int refundId) {

        RefundDto dto = refundService.processRefund(refundId);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Refund processed — status: " + dto.getStatus(), dto),
                HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<RefundDto>>> getAllRefunds() {
        return new ResponseEntity<>(
                new APIResponse<>(200, "Success", refundService.getAllRefunds()),
                HttpStatus.OK);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<APIResponse<RefundDto>> getRefundByBooking(
            @PathVariable int bookingId) {

        RefundDto dto = refundService.getRefundByBooking(bookingId);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Success", dto),
                HttpStatus.OK);
    }
}