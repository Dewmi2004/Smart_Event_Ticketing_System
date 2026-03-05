package lk.ijse.event_ticketingback_end.repository;

import lk.ijse.event_ticketingback_end.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

}
