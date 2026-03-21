package lk.ijse.event_ticketingback_end.service.impl;

import jakarta.mail.MessagingException;
import lk.ijse.event_ticketingback_end.dto.PaymentDto;
import lk.ijse.event_ticketingback_end.entity.Booking;
import lk.ijse.event_ticketingback_end.entity.Payment;
import lk.ijse.event_ticketingback_end.entity.Seat;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.BookingRepository;
import lk.ijse.event_ticketingback_end.repository.PaymentRepository;
import lk.ijse.event_ticketingback_end.service.EmailService;
import lk.ijse.event_ticketingback_end.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EmailService      emailService;

    // No QRCodeGenerator here — email QR uses api.qrserver.com
    // ZXing is only used in EventController for the Event admin page QR

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @Value("${payhere.sandbox:true}")
    private boolean sandbox;

    @Value("${app.base.url}")
    private String appBaseUrl;

    @Value("${app.backend.url}")
    private String appBackendUrl;

    // ── INITIATE PAYMENT ──────────────────────────────────────────────────────
    @Override
    public Map<String, String> initiatePayment(int bookingId,
                                               String customerName,
                                               String customerEmail,
                                               String customerPhone) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EventNotFoundException("Booking not found: " + bookingId));

        String orderId = "EH-" + bookingId + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String amount = BigDecimal.valueOf(booking.getTotalAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();

        String hash = generateHash(merchantId, orderId, amount, "LKR");

        log.info("[PayHere] merchantId  = {}", merchantId);
        log.info("[PayHere] orderId     = {}", orderId);
        log.info("[PayHere] amount      = {}", amount);
        log.info("[PayHere] currency    = LKR");
        log.info("[PayHere] hash        = {}", hash);
        log.info("[PayHere] notify_url  = {}", appBackendUrl + "/api/v1/payment/notify");

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setOrderId(orderId);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("LKR");
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        String firstName = customerName.trim();
        String lastName  = "";
        if (customerName.contains(" ")) {
            String[] parts = customerName.trim().split(" ", 2);
            firstName = parts[0];
            lastName  = parts[1];
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("sandbox",     String.valueOf(sandbox));
        params.put("merchant_id", merchantId);
        params.put("return_url",  appBaseUrl + "/payment-success.html?bookingId=" + bookingId);
        params.put("cancel_url",  appBaseUrl + "/payment-cancel.html?bookingId=" + bookingId);
        params.put("notify_url",  appBackendUrl + "/api/v1/payment/notify");
        params.put("order_id",    orderId);
        params.put("items",       booking.getEvent().getEvent_name() + " Tickets");
        params.put("currency",    "LKR");
        params.put("amount",      amount);
        params.put("first_name",  firstName);
        params.put("last_name",   lastName);
        params.put("email",       customerEmail);
        params.put("phone",       customerPhone != null ? customerPhone : "0771234567");
        params.put("address",     "Colombo");
        params.put("city",        "Colombo");
        params.put("country",     "Sri Lanka");
        params.put("hash",        hash);

        return params;
    }

    // ── HANDLE NOTIFY (PayHere webhook) ───────────────────────────────────────
    @Override
    public void handleNotify(Map<String, String> params) {
        String orderId      = params.get("order_id");
        String payherePayId = params.get("payment_id");
        String statusCode   = params.get("status_code");
        String currency     = params.get("payhere_currency");
        String amount       = params.get("payhere_amount");
        String method       = params.get("method");
        String receivedHash = params.get("md5sig");

        log.info("[PayHere Notify] orderId={} statusCode={}", orderId, statusCode);

        String expectedHash = generateNotifyHash(merchantId, orderId, amount, currency, statusCode);
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            log.error("[PayHere Notify] Hash FAILED — expected={} received={}", expectedHash, receivedHash);
            throw new RuntimeException("PayHere hash verification failed");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Payment not found for orderId: " + orderId));

        if ("2".equals(statusCode)) {

            payment.setStatus("PAID");
            payment.setPayherePaymentId(payherePayId);
            payment.setMethod(method);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.saveAndFlush(payment);

            Booking booking = payment.getBooking();
            booking.setStatus("Confirmed");
            bookingRepository.saveAndFlush(booking);

            String seatNumbers = booking.getSeats().stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.joining(", "));

            // ── Email QR: api.qrserver.com ────────────────────────────────────
            // This is a public hosted URL — renders correctly in all email clients.
            // Gmail and Outlook block base64 data URIs and localhost/ngrok images.
            // ZXing is used only for the Event admin page (EventController).
            String qrData = String.format(
                    "{\"bookingId\":%d,\"event\":\"%s\",\"seats\":\"%s\",\"amount\":%.2f,\"status\":\"Confirmed\"}",
                    booking.getBookingId(),
                    booking.getEvent().getEvent_name(),
                    seatNumbers,
                    booking.getTotalAmount()
            );
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=M&data="
                    + java.net.URLEncoder.encode(qrData, StandardCharsets.UTF_8);

            if (booking.getUserEmail() != null && !booking.getUserEmail().isBlank()) {
                try {
                    emailService.sendBookingConfirmation(
                            booking.getUserEmail(),
                            "Attendee",
                            booking.getBookingId(),
                            booking.getEvent().getEvent_name(),
                            booking.getEvent().getDate() != null
                                    ? booking.getEvent().getDate().toString() : "TBA",
                            booking.getEvent().getLocation(),
                            seatNumbers,
                            booking.getTotalAmount(),
                            qrUrl
                    );
                    log.info("[PayHere] Ticket email sent for bookingId={}", booking.getBookingId());
                } catch (MessagingException e) {
                    log.error("[PayHere] Email failed: {}", e.getMessage());
                }
            }

        } else if ("0".equals(statusCode)) {
            payment.setStatus("PENDING");
            paymentRepository.saveAndFlush(payment);
            log.warn("[PayHere] Payment pending for orderId={}", orderId);

        } else {
            payment.setStatus("FAILED");
            paymentRepository.saveAndFlush(payment);
            log.warn("[PayHere] Payment failed for orderId={} statusCode={}", orderId, statusCode);
        }
    }

    // ── GET PAYMENT BY BOOKING ────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByBooking(int bookingId) {
        Payment p = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new EventNotFoundException(
                        "No payment found for bookingId: " + bookingId));
        return toDto(p);
    }

    // ── INITIATE HASH ─────────────────────────────────────────────────────────
    private String generateHash(String merchantId, String orderId,
                                String amount, String currency) {
        try {
            MessageDigest md        = MessageDigest.getInstance("MD5");
            byte[] secretBytes      = md.digest(merchantSecret.getBytes(StandardCharsets.UTF_8));
            String hashedSecret     = bytesToHex(secretBytes).toUpperCase();
            md.reset();
            String raw              = merchantId + orderId + amount + currency + hashedSecret;
            byte[] hashBytes        = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    // ── NOTIFY HASH ───────────────────────────────────────────────────────────
    private String generateNotifyHash(String merchantId, String orderId,
                                      String amount, String currency, String statusCode) {
        try {
            MessageDigest md        = MessageDigest.getInstance("MD5");
            byte[] secretBytes      = md.digest(merchantSecret.getBytes(StandardCharsets.UTF_8));
            String hashedSecret     = bytesToHex(secretBytes).toUpperCase();
            md.reset();
            String raw              = merchantId + orderId + amount + currency + statusCode + hashedSecret;
            byte[] hashBytes        = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── DTO MAPPER ────────────────────────────────────────────────────────────
    private PaymentDto toDto(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.setPaymentId(p.getPaymentId());
        dto.setBookingId(p.getBooking().getBookingId());
        dto.setOrderId(p.getOrderId());
        dto.setPayherePaymentId(p.getPayherePaymentId());
        dto.setAmount(p.getAmount());
        dto.setCurrency(p.getCurrency());
        dto.setStatus(p.getStatus());
        dto.setMethod(p.getMethod());
        return dto;
    }
}