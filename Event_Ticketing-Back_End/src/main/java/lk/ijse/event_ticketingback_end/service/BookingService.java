package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto saveBooking(BookingDto bookingDto);
    void updateBookingStatus(int bookingId, String status);
    void deleteBooking(int bookingId);
    List<BookingDto> getAllBookings();
}