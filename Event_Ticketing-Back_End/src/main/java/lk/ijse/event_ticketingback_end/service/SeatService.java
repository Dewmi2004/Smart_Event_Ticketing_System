package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.SeatDto;
import java.util.List;

public interface SeatService {
    void saveSeat(SeatDto dto);
    void updateSeat(SeatDto dto);
    void deleteSeat(SeatDto dto);
    List<SeatDto> getAllSeats();
    List<SeatDto> getSeatsByEvent(int eventId);

    int generateSeatsForEvent(int eventId);
}