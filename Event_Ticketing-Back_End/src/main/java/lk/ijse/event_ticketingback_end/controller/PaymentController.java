package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.PaymentDto;
import lk.ijse.event_ticketingback_end.service.PaymentService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/payment")
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payment/initiate
     * Frontend calls this when user clicks "Pay Now".
     * Returns all the params needed to build the PayHere form.
     *
     * Request body example:
     * {
     *   "bookingId": 5,
     *   "customerName": "Imasha Dewmi",
     *   "customerEmail": "imasha@gmail.com",
     *   "customerPhone": "0771234567"
     * }
     */
    @PostMapping("/initiate")
    public ResponseEntity<APIResponse<Map<String, String>>> initiatePayment(
            @RequestBody Map<String, String> body) {

        int    bookingId     = Integer.parseInt(body.get("bookingId"));
        String customerName  = body.getOrDefault("customerName",  "Attendee");
        String customerEmail = body.getOrDefault("customerEmail", "");
        String customerPhone = body.getOrDefault("customerPhone", "0771234567");

        Map<String, String> params = paymentService.initiatePayment(
                bookingId, customerName, customerEmail, customerPhone);

        return new ResponseEntity<>(
                new APIResponse<>(200, "Payment initiated", params),
                HttpStatus.OK);
    }

    /**
     * POST /api/v1/payment/notify
     * PayHere calls this URL directly (server-to-server) after payment.
     * This is the webhook — DO NOT add auth filter to this endpoint.
     * Must return HTTP 200 OK with no body.
     */
    @PostMapping("/notify")
    public ResponseEntity<String> handleNotify(
            @RequestParam Map<String, String> params) {

        log.info("[PayHere Notify] Received: {}", params);
        try {
            paymentService.handleNotify(params);
        } catch (Exception e) {
            log.error("[PayHere Notify] Error: {}", e.getMessage());
            // Still return 200 so PayHere doesn't keep retrying with bad data
            return ResponseEntity.ok("error");
        }
        return ResponseEntity.ok("OK");
    }

    /**
     * GET /api/v1/payment/booking/{bookingId}
     * Frontend can poll this to check payment status.
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<APIResponse<PaymentDto>> getPaymentByBooking(
            @PathVariable int bookingId) {

        PaymentDto dto = paymentService.getPaymentByBooking(bookingId);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Success", dto),
                HttpStatus.OK);
    }
}