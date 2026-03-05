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
import java.util.List;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    @Override
    public void saveEvent(EventDto eventDto) {
        Event event=modelMapper.map(eventDto,Event.class);
        eventRepository.save(event);
    }

    @Override
    public void updateEvent(EventDto eventDto) {
        Event existingEvent = eventRepository
                .findById(eventDto.getEvent_id())
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found with ID: " + eventDto.getEvent_id()));

        modelMapper.map(eventDto, existingEvent);
        eventRepository.save(existingEvent);
    }

    @Override
    public void deleteEvent(EventDto eventDto) {
        if (!eventRepository.existsById(eventDto.getEvent_id())) {
            throw new EventNotFoundException(
                    "Event not found with ID: " + eventDto.getEvent_id());
        }
        eventRepository.deleteById(eventDto.getEvent_id());
    }

    @Override
    public List<EventDto> getAllEvents() {
        List<Event> list = eventRepository.findAll();
        return modelMapper.map(list, new TypeToken<List<EventDto>>() {}.getType());
    }
}
