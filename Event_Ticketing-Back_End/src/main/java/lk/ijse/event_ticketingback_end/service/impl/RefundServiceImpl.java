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

    // ── Configure ModelMapper once at startup ──────────────────────────────────
    // Root cause of ConfigurationException:
    //   Payment has paymentId, payherePaymentId and orderId — all ending in "Id".
    //   ModelMapper's default STANDARD strategy tokenises "paymentId" and tries to
    //   match the DTO field setPaymentId() against ALL of them, finding three
    //   candidates → ambiguity error at startup.
    //
    // Fix:
    //   1. Switch to STRICT matching so ModelMapper only maps properties whose
    //      full token sequence matches exactly (refundId→refundId, amount→amount,
    //      etc.) and skips anything it cannot resolve without ambiguity.
    //   2. Use skip() + explicit map() for every nested / ambiguous field so
    //      there is exactly one source for each destination property.
    @PostConstruct
    private void configureModelMapper() {
        // Guard: only register once (safe for test contexts that reuse the bean)
        if (modelMapper.getTypeMap(Refund.class, RefundDto.class) != null) return;

        // STRICT eliminates implicit ambiguous matches
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.createTypeMap(Refund.class, RefundDto.class)
                .addMappings(mapper -> {

                    // ── Skip all fields that ModelMapper must NOT touch implicitly ──
                    // Without these skips, STRICT mode leaves them null but may still
                    // attempt a match during TypeMap validation.
                    mapper.skip(RefundDto::setPaymentId);
                    mapper.skip(RefundDto::setBookingId);
                    mapper.skip(RefundDto::setRequestedAt);
                    mapper.skip(RefundDto::setProcessedAt);

                    // ── Explicit mappings for the skipped fields ───────────────────
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

    // ── toDto: single ModelMapper call, zero manual setters ───────────────────
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

        try {
            String payhereRefundUrl = sandbox
                    ? "https://sandbox.payhere.lk/merchant/v1/refund"
                    : "https://www.payhere.lk/merchant/v1/refund";

            String hashedSecret = md5(merchantSecret).toUpperCase();
            String credentials  = merchantId + ":" + hashedSecret;
            String basicAuth    = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String body = "payment_id=" + payment.getPayherePaymentId()
                    + "&description=" + java.net.URLEncoder.encode(
                    refund.getReason(), StandardCharsets.UTF_8);

            log.info("[Refund] Calling PayHere API — paymentId={} url={}",
                    payment.getPayherePaymentId(), payhereRefundUrl);

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
                refund.setStatus("APPROVED");
                refund.setProcessedAt(LocalDateTime.now());

                String responseBody = response.body();
                if (responseBody.contains("refund_id")) {
                    int idx = responseBody.indexOf("refund_id");
                    if (idx > -1) {
                        String sub             = responseBody.substring(idx + 12);
                        String refundPayhereId = sub.substring(0, sub.indexOf("\""));
                        refund.setPayhereRefundId(refundPayhereId);
                    }
                }
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

                log.info("[Refund] APPROVED — refundId={} bookingId={}",
                        refundId, booking.getBookingId());

            } else {
                refund.setStatus("REJECTED");
                refund.setProcessedAt(LocalDateTime.now());
                refundRepository.saveAndFlush(refund);
                log.warn("[Refund] REJECTED by PayHere — status={} body={}",
                        response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[Refund] API call failed: {}", e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }

        return toDto(refund);
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