package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.EventDto;
import lk.ijse.event_ticketingback_end.entity.Event;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.EventRepository;
import lk.ijse.event_ticketingback_end.service.EventService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;

    @Override
    public void saveEvent(EventDto eventDto) {
        eventDto.setEventId(0);
        Event event = modelMapper.map(eventDto, Event.class);
        eventRepository.save(event);
    }

    @Override
    public void updateEvent(EventDto eventDto) {
        Event existingEvent = eventRepository
                .findById(eventDto.getEventId())
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
        Event existingEvent = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found with ID: " + eventId));
        eventRepository.delete(existingEvent);
        eventRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        List<Event> list = eventRepository.findAll();
        return modelMapper.map(list, new TypeToken<List<EventDto>>() {}.getType());
    }
}