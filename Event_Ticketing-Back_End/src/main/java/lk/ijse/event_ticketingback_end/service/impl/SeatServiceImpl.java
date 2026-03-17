package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.SeatDto;
import lk.ijse.event_ticketingback_end.entity.Event;
import lk.ijse.event_ticketingback_end.entity.Seat;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.EventRepository;
import lk.ijse.event_ticketingback_end.repository.SeatRepository;
import lk.ijse.event_ticketingback_end.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    @Override
    public void saveSeat(SeatDto dto) {
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + dto.getEventId()));
        Seat seat = new Seat();
        seat.setSeatId(0);
        seat.setEvent(event);
        seat.setSeatNumber(dto.getSeat_number());
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

    private SeatDto toDto(Seat s) {
        SeatDto dto = new SeatDto();
        dto.setSeat_id(s.getSeatId());
        dto.setEventId(s.getEvent().getEventId());
        dto.setSeat_number(s.getSeatNumber());
        dto.setSeat_type(s.getSeatType());
        dto.setPrice(s.getPrice());
        dto.setStatus(s.getStatus());
        return dto;
    }
}