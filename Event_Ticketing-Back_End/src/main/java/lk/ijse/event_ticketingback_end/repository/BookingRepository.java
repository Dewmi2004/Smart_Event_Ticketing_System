package lk.ijse.event_ticketingback_end.repository;

import lk.ijse.event_ticketingback_end.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByUserEmail(String userEmail);
    List<Booking> findByUserId(int userId);
}