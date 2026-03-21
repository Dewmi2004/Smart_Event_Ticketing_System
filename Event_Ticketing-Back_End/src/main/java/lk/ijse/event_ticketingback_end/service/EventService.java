package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.EventDto;
import java.util.List;

public interface EventService {
    void saveEvent(EventDto eventDto);
    void updateEvent(EventDto eventDto);
    void deleteEvent(int eventId);
    List<EventDto> getAllEvents();
    EventDto getEventById(int eventId);  // ← new — needed for QR generation
}