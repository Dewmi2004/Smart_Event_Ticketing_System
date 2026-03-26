package lk.ijse.event_ticketingback_end.service.impl;

import jakarta.annotation.PostConstruct;
import lk.ijse.event_ticketingback_end.dto.RefundDto;
import lk.ijse.event_ticketingback_end.entity.Booking;
import lk.ijse.event_ticketingback_end.entity.Payment;
import lk.ijse.event_ticketingback_end.entity.Refund;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.BookingRepository;
import lk.ijse.event_ticketingback_end.repository.PaymentRepository;
import lk.ijse.event_ticketingback_end.repository.RefundRepository;
import lk.ijse.event_ticketingback_end.repository.SeatRepository;
import lk.ijse.event_ticketingback_end.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefundServiceImpl implements RefundService {

    private final RefundRepository  refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository    seatRepository;
    private final ModelMapper       modelMapper;

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @Value("${payhere.sandbox:true}")
    private boolean sandbox;

    // ── FIX: Add a flag to bypass the PayHere API call entirely in sandbox/dev.
    // PayHere's sandbox environment does NOT support the refund endpoint —
    // it always returns a non-200 response, causing every refund to be REJECTED.
    //
    // Set  payhere.sandbox.mock-refund=true  in application.properties (or
    // application-dev.properties) to auto-approve refunds without calling PayHere.
    // Leave it false (default) in production so the real API is always called.
    @Value("${payhere.sandbox.mock-refund:false}")
    private boolean mockRefundInSandbox;

    // ── Pattern to safely extract refund_id from PayHere JSON response ─────────
    // The old code used fragile string-index arithmetic (substring + indexOf) which
    // breaks if the JSON field order changes or whitespace varies.
    // A compiled regex is safe, concise, and handles all formatting variants.
    private static final Pattern REFUND_ID_PATTERN =
            Pattern.compile("\"refund_id\"\\s*:\\s*\"([^\"]+)\"");

    // ── Configure ModelMapper once at startup ──────────────────────────────────
    @PostConstruct
    private void configureModelMapper() {
        if (modelMapper.getTypeMap(Refund.class, RefundDto.class) != null) return;

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.createTypeMap(Refund.class, RefundDto.class)
                .addMappings(mapper -> {
                    mapper.skip(RefundDto::setPaymentId);
                    mapper.skip(RefundDto::setBookingId);
                    mapper.skip(RefundDto::setRequestedAt);
                    mapper.skip(RefundDto::setProcessedAt);

                    mapper.map(
                            r -> r.getPayment().getPaymentId(),
                            RefundDto::setPaymentId
                    );
                    mapper.map(
                            r -> r.getPayment().getBooking().getBookingId(),
                            RefundDto::setBookingId
                    );
                    mapper.map(
                            r -> r.getRequestedAt() != null ? r.getRequestedAt().toString() : null,
                            RefundDto::setRequestedAt
                    );
                    mapper.map(
                            r -> r.getProcessedAt() != null ? r.getProcessedAt().toString() : null,
                            RefundDto::setProcessedAt
                    );
                });
    }

    private RefundDto toDto(Refund r) {
        return modelMapper.map(r, RefundDto.class);
    }

    // ── requestRefund ──────────────────────────────────────────────────────────
    @Override
    public RefundDto requestRefund(int bookingId, String reason) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EventNotFoundException("Booking not found: " + bookingId));

        if (!"Confirmed".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Only confirmed bookings can be refunded. Current status: " + booking.getStatus());
        }

        Payment payment = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new EventNotFoundException(
                        "No payment found for bookingId: " + bookingId));

        if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
            throw new RuntimeException(
                    "Payment is not in PAID status. Current status: " + payment.getStatus());
        }

        if (refundRepository.findByPayment_PaymentId(payment.getPaymentId()).isPresent()) {
            throw new RuntimeException("A refund request already exists for this booking.");
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setReason(reason != null ? reason : "Customer requested refund");
        refund.setStatus("PENDING");
        refund.setRequestedAt(LocalDateTime.now());

        Refund saved = refundRepository.save(refund);
        log.info("[Refund] Request created — refundId={} bookingId={} amount={}",
                saved.getRefundId(), bookingId, saved.getAmount());

        return toDto(saved);
    }

    // ── processRefund ──────────────────────────────────────────────────────────
    @Override
    public RefundDto processRefund(int refundId) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EventNotFoundException("Refund not found: " + refundId));

        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            throw new RuntimeException("Refund is already " + refund.getStatus());
        }

        Payment payment = refund.getPayment();

        if (payment.getPayherePaymentId() == null || payment.getPayherePaymentId().isBlank()) {
            throw new RuntimeException("No PayHere payment ID found — cannot process refund.");
        }

        // ── FIX: Sandbox mock bypass ───────────────────────────────────────────
        // PayHere sandbox does not support the refund API. When mock mode is
        // enabled we skip the HTTP call entirely and mark the refund APPROVED
        // immediately, so developers can test the full booking → refund → seat
        // release flow without a live merchant account.
        if (sandbox && mockRefundInSandbox) {
            log.warn("[Refund] SANDBOX MOCK — auto-approving refundId={} (no real API call)", refundId);
            approveRefund(refund, payment, "SANDBOX-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return toDto(refund);
        }

        // ── Live PayHere API call ──────────────────────────────────────────────
        try {
            String payhereRefundUrl = sandbox
                    ? "https://sandbox.payhere.lk/merchant/v1/refund"
                    : "https://www.payhere.lk/merchant/v1/refund";

            // PayHere Basic Auth: merchantId:MD5(merchantSecret).toUpperCase()
            String hashedSecret = md5(merchantSecret).toUpperCase();
            String credentials  = merchantId + ":" + hashedSecret;
            String basicAuth    = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String body = "payment_id=" + payment.getPayherePaymentId()
                    + "&description=" + java.net.URLEncoder.encode(
                    refund.getReason(), StandardCharsets.UTF_8);

            log.info("[Refund] Calling PayHere API — paymentId={} sandbox={} url={}",
                    payment.getPayherePaymentId(), sandbox, payhereRefundUrl);

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(payhereRefundUrl))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("[Refund] PayHere response status={} body={}",
                    response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                // ── FIX: Use regex instead of fragile substring arithmetic ─────
                // Old code used indexOf + manual offsets which breaks on any
                // whitespace or field-order variation in the JSON.
                String payhereRefundId = null;
                Matcher matcher = REFUND_ID_PATTERN.matcher(response.body());
                if (matcher.find()) {
                    payhereRefundId = matcher.group(1);
                }
                approveRefund(refund, payment, payhereRefundId);

            } else {
                refund.setStatus("REJECTED");
                refund.setProcessedAt(LocalDateTime.now());
                refundRepository.saveAndFlush(refund);
                log.warn("[Refund] REJECTED by PayHere — httpStatus={} body={}",
                        response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[Refund] API call failed: {}", e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }

        return toDto(refund);
    }

    // ── approveRefund: shared logic for mock and live approvals ───────────────
    // Extracted into its own method so both the mock path and the live 200-OK
    // path share the same state-transition code — no risk of them drifting apart.
    private void approveRefund(Refund refund, Payment payment, String payhereRefundId) {
        refund.setStatus("APPROVED");
        refund.setProcessedAt(LocalDateTime.now());
        if (payhereRefundId != null) refund.setPayhereRefundId(payhereRefundId);
        refundRepository.saveAndFlush(refund);

        payment.setStatus("REFUNDED");
        paymentRepository.saveAndFlush(payment);

        Booking booking = payment.getBooking();
        booking.setStatus("Refunded");
        bookingRepository.saveAndFlush(booking);

        booking.getSeats().forEach(seat -> {
            seat.setStatus("Available");
            seatRepository.saveAndFlush(seat);
        });

        log.info("[Refund] APPROVED — refundId={} bookingId={} payhereRefundId={}",
                refund.getRefundId(), booking.getBookingId(), payhereRefundId);
    }

    // ── getAllRefunds ──────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<RefundDto> getAllRefunds() {
        return refundRepository.findAllByOrderByRequestedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── getRefundByBooking ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public RefundDto getRefundByBooking(int bookingId) {
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new EventNotFoundException(
                        "No payment found for bookingId: " + bookingId));
        Refund refund = refundRepository.findByPayment_PaymentId(payment.getPaymentId())
                .orElseThrow(() -> new EventNotFoundException(
                        "No refund found for bookingId: " + bookingId));
        return toDto(refund);
    }

    // ── MD5 helper ─────────────────────────────────────────────────────────────
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 failed", e);
        }
    }
}