package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.BookingDto;
import lk.ijse.event_ticketingback_end.entity.Booking;
import lk.ijse.event_ticketingback_end.entity.Event;
import lk.ijse.event_ticketingback_end.entity.Seat;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.BookingRepository;
import lk.ijse.event_ticketingback_end.repository.CouponRepository;
import lk.ijse.event_ticketingback_end.repository.EventRepository;
import lk.ijse.event_ticketingback_end.repository.SeatRepository;
import lk.ijse.event_ticketingback_end.service.BookingService;
import lk.ijse.event_ticketingback_end.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository    seatRepository;
    private final EventRepository   eventRepository;
    private final CouponRepository  couponRepository;
    private final EmailService      emailService;

    @Override
    public BookingDto saveBooking(BookingDto dto) {

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + dto.getEventId()));

        List<Seat> seats = dto.getSeatIds().stream().map(id -> {
            Seat seat = seatRepository.findById(id)
                    .orElseThrow(() -> new EventNotFoundException("Seat not found: " + id));
            if (!"Available".equalsIgnoreCase(seat.getStatus())) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is not available.");
            }
            seat.setStatus("Booked");
            return seatRepository.saveAndFlush(seat);
        }).collect(Collectors.toList());

        if (dto.getCouponCode() != null && !dto.getCouponCode().isBlank()) {
            couponRepository.findByCouponCode(dto.getCouponCode()).ifPresent(coupon -> {
                coupon.setUsed_count(coupon.getUsed_count() + seats.size());
                couponRepository.saveAndFlush(coupon);
            });
        }

        Booking booking = new Booking();
        booking.setUserId(dto.getUserId());
        booking.setEvent(event);
        booking.setBookingDate(Date.valueOf(LocalDate.now()));
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCouponCode(dto.getCouponCode());
        booking.setStatus("Pending");
        booking.setSeats(seats);
        Booking saved = bookingRepository.save(booking);

        String seatNumbers = seats.stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.joining(", "));

        String qrData = String.format(
                "{\"bookingId\":%d,\"userId\":%d,\"event\":\"%s\",\"seats\":\"%s\",\"amount\":%.2f,\"status\":\"Pending\"}",
                saved.getBookingId(), dto.getUserId(),
                event.getEvent_name(), seatNumbers, dto.getTotalAmount()
        );
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=M&data="
                + java.net.URLEncoder.encode(qrData, java.nio.charset.StandardCharsets.UTF_8);

        if (dto.getUserEmail() != null && !dto.getUserEmail().isBlank()) {
            try {
                emailService.sendBookingConfirmation(
                        dto.getUserEmail(),
                        "Attendee",
                        saved.getBookingId(),
                        event.getEvent_name(),
                        event.getDate() != null ? event.getDate().toString() : "TBA",
                        event.getLocation(),
                        seatNumbers,
                        dto.getTotalAmount(),
                        qrUrl
                );
            } catch (Exception e) {
                System.err.println("[EmailService] Failed to send booking email: " + e.getMessage());
            }
        }

        return toDto(saved);
    }

    @Override
    public void updateBookingStatus(int bookingId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EventNotFoundException("Booking not found: " + bookingId));
        booking.setStatus(status);
        bookingRepository.saveAndFlush(booking);
    }

    @Override
    public void deleteBooking(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EventNotFoundException("Booking not found: " + bookingId));
        booking.getSeats().forEach(seat -> {
            seat.setStatus("Available");
            seatRepository.saveAndFlush(seat);
        });
        bookingRepository.delete(booking);
        bookingRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    private BookingDto toDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.setBookingId(b.getBookingId());
        dto.setUserId(b.getUserId());
        dto.setEventId(b.getEvent().getEventId());
        dto.setEventName(b.getEvent().getEvent_name());
        dto.setEventLocation(b.getEvent().getLocation());
        dto.setBookingDate(b.getBookingDate());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setCouponCode(b.getCouponCode());
        dto.setStatus(b.getStatus());
        dto.setSeatIds(b.getSeats().stream()
                .map(Seat::getSeatId).collect(Collectors.toList()));
        return dto;
    }
}