package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.RefundDto;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.service.RefundService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/refund")
@CrossOrigin
public class RefundController {

    private final RefundService refundService;

    record RefundRequest(Integer bookingId, String reason) {}

    @PostMapping("/request")
    public ResponseEntity<APIResponse<RefundDto>> requestRefund(
            @RequestBody RefundRequest body) {

        if (body == null || body.bookingId() == null) {
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(400, "bookingId is required", null));
        }

        String reason = (body.reason() != null && !body.reason().isBlank())
                ? body.reason()
                : "Customer requested refund";

        RefundDto dto = refundService.requestRefund(body.bookingId(), reason);
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


    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleNotFound(EventNotFoundException ex) {
        log.warn("[Refund] Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new APIResponse<>(404, ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<APIResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("[Refund] Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new APIResponse<>(400, ex.getMessage(), null));
    }
}