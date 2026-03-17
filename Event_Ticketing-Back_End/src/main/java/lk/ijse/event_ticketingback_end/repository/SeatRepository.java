package lk.ijse.event_ticketingback_end.repository;

import lk.ijse.event_ticketingback_end.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    List<Seat> findByEvent_EventId(int eventId);
}