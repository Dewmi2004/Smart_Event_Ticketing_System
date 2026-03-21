package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.EventDto;
import lk.ijse.event_ticketingback_end.entity.Event;
import lk.ijse.event_ticketingback_end.entity.Seat;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.EventRepository;
import lk.ijse.event_ticketingback_end.repository.SeatRepository;
import lk.ijse.event_ticketingback_end.service.EventService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final SeatRepository  seatRepository;
    private final ModelMapper     modelMapper;

    private static final int      TOTAL_SEATS = 120;
    private static final int      COLS        = 12;
    private static final char[]   ROW_LETTERS = {'A','B','C','D','E','F','G','H','I','J'};
    private static final String[] SEAT_TYPES  = {
            "VIP","VIP","VIP",
            "Premium","Premium","Premium",
            "Standard","Standard","Standard","Standard"
    };

    @Override
    public void saveEvent(EventDto eventDto) {
        eventDto.setEventId(0);
        eventDto.setTotal_seats(TOTAL_SEATS);
        Event event = modelMapper.map(eventDto, Event.class);
        Event saved = eventRepository.save(event);
        generateSeats(saved);
    }

    private void generateSeats(Event event) {
        double basePrice = event.getTicket_price() != null ? event.getTicket_price() : 0.0;
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            int    rowIndex  = (i - 1) / COLS;
            int    col       = (i - 1) % COLS + 1;
            char   rowLetter = ROW_LETTERS[rowIndex];
            String seatNum   = String.valueOf(rowLetter) + col;
            String seatType  = SEAT_TYPES[rowIndex];
            double price;
            switch (seatType) {
                case "VIP":     price = basePrice * 2; break;
                case "Premium": price = basePrice;     break;
                default:        price = basePrice / 2; break;
            }
            Seat seat = new Seat();
            seat.setSeatId(0);
            seat.setEvent(event);
            seat.setSeatNumber(seatNum);
            seat.setSeatType(seatType);
            seat.setPrice(price);
            seat.setStatus("Available");
            seats.add(seat);
        }
        seatRepository.saveAll(seats);
    }

    @Override
    public void updateEvent(EventDto eventDto) {
        Event existingEvent = eventRepository.findById(eventDto.getEventId())
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found with ID: " + eventDto.getEventId()));
        existingEvent.setEvent_name(eventDto.getEvent_name());
        existingEvent.setLocation(eventDto.getLocation());
        existingEvent.setDate(eventDto.getDate());
        existingEvent.setTime(eventDto.getTime());
        existingEvent.setTicket_price(eventDto.getTicket_price());
        existingEvent.setTotal_seats(eventDto.getTotal_seats());
        existingEvent.setStatus(eventDto.getStatus());
        eventRepository.saveAndFlush(existingEvent);
    }

    @Override
    public void deleteEvent(int eventId) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found with ID: " + eventId));
        eventRepository.delete(existingEvent);
        eventRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return modelMapper.map(
                eventRepository.findAll(),
                new TypeToken<List<EventDto>>() {}.getType());
    }

    // ── NEW: needed by EventController for QR generation ─────────────────────
    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found with ID: " + eventId));
        return modelMapper.map(event, EventDto.class);
    }
}