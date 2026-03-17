package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.EventDto;

import java.util.List;

public interface EventService {
    public void saveEvent(EventDto eventDto);
    public void updateEvent(EventDto eventDto);
    public void deleteEvent(int eventId);
    public List<EventDto> getAllEvents();
}
