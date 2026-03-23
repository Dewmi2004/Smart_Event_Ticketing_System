package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.SeatDto;
import lk.ijse.event_ticketingback_end.entity.Event;
import lk.ijse.event_ticketingback_end.entity.Seat;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.EventRepository;
import lk.ijse.event_ticketingback_end.repository.SeatRepository;
import lk.ijse.event_ticketingback_end.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository  seatRepository;
    private final EventRepository eventRepository;
    private final ModelMapper     modelMapper;

    private static final char[] ROW_LETTERS = {'A','B','C','D','E','F','G','H','I','J'};

    private String toMapSeatNumber(int sequentialNumber) {
        int cols     = 12;
        int rowIndex = (sequentialNumber - 1) / cols;
        int col      = (sequentialNumber - 1) % cols + 1;
        char row     = rowIndex < ROW_LETTERS.length ? ROW_LETTERS[rowIndex] : (char)('A' + rowIndex);
        return String.valueOf(row) + col;
    }

    @Override
    public void saveSeat(SeatDto dto) {
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + dto.getEventId()));

        Seat seat = new Seat();
        seat.setSeatId(0);
        seat.setEvent(event);

        String seatNum = dto.getSeat_number();
        if (seatNum != null && seatNum.matches("\\d+")) {
            seatNum = toMapSeatNumber(Integer.parseInt(seatNum));
        }
        seat.setSeatNumber(seatNum);
        seat.setSeatType(dto.getSeat_type());
        seat.setPrice(dto.getPrice());
        seat.setStatus(dto.getStatus() != null ? dto.getStatus() : "Available");
        seatRepository.save(seat);
    }

    @Override
    public void updateSeat(SeatDto dto) {
        Seat seat = seatRepository.findById(dto.getSeat_id())
                .orElseThrow(() -> new EventNotFoundException("Seat not found: " + dto.getSeat_id()));
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + dto.getEventId()));
        seat.setEvent(event);
        seat.setSeatNumber(dto.getSeat_number());
        seat.setSeatType(dto.getSeat_type());
        seat.setPrice(dto.getPrice());
        seat.setStatus(dto.getStatus());
        seatRepository.saveAndFlush(seat);
    }

    @Override
    public void deleteSeat(SeatDto dto) {
        Seat seat = seatRepository.findById(dto.getSeat_id())
                .orElseThrow(() -> new EventNotFoundException("Seat not found: " + dto.getSeat_id()));
        seatRepository.delete(seat);
        seatRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDto> getAllSeats() {
        return seatRepository.findAll().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatsByEvent(int eventId) {
        return seatRepository.findByEvent_EventId(eventId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public int generateSeatsForEvent(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        List<Seat> existing = seatRepository.findByEvent_EventId(eventId);
        if (!existing.isEmpty()) {
            return 0;
        }

        int total = event.getTotal_seats() != null ? event.getTotal_seats() : 120;
        int cols  = 12;
        String[] types = {"VIP","VIP","VIP","Premium","Premium","Premium","Standard","Standard","Standard","Standard"};

        for (int i = 1; i <= total; i++) {
            int  rowIndex  = (i - 1) / cols;
            int  col       = (i - 1) % cols + 1;
            char rowLetter = rowIndex < ROW_LETTERS.length ? ROW_LETTERS[rowIndex] : (char)('A' + rowIndex);
            String seatNum = String.valueOf(rowLetter) + col;
            String type    = rowIndex < types.length ? types[rowIndex] : "Standard";

            Seat seat = new Seat();
            seat.setSeatId(0);
            seat.setEvent(event);
            seat.setSeatNumber(seatNum);
            seat.setSeatType(type);
            seat.setPrice(event.getTicket_price() != null ? event.getTicket_price() : 0.0);
            seat.setStatus("Available");
            seatRepository.save(seat);
        }
        return total;
    }

    private SeatDto toDto(Seat s) {
        SeatDto dto = modelMapper.map(s, SeatDto.class);
        dto.setSeat_id(s.getSeatId());
        dto.setEventId(s.getEvent().getEventId());
        dto.setSeat_number(s.getSeatNumber());
        dto.setSeat_type(s.getSeatType());
        dto.setPrice(s.getPrice());
        dto.setStatus(s.getStatus());
        return dto;
    }
}