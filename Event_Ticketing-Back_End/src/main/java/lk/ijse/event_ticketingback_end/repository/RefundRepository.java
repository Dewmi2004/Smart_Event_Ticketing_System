package lk.ijse.event_ticketingback_end.repository;

import lk.ijse.event_ticketingback_end.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
    Optional<Refund> findByPayment_PaymentId(int paymentId);
    List<Refund> findAllByOrderByRequestedAtDesc();
}