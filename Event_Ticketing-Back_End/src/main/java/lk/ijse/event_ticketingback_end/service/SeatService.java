package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.SeatDto;

import java.util.List;

public interface SeatService {
    void saveSeat(SeatDto seatDto);
    void updateSeat(SeatDto seatDto);
    void deleteSeat(SeatDto seatDto);
    List<SeatDto> getAllSeats();
    List<SeatDto> getSeatsByEvent(int eventId);
}