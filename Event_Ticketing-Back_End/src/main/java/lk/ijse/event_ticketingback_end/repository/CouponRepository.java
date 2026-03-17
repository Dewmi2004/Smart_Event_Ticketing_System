package lk.ijse.event_ticketingback_end.repository;

import lk.ijse.event_ticketingback_end.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon,Integer> {
    Optional<Coupon> findByCouponCode(String couponCode);

}
