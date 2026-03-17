package lk.ijse.event_ticketingback_end.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int coupon_id;
    @Column(name = "coupon_code")
    private String couponCode;
    private String discount_type;
    private int discount_value;
    private Date expiration_date;
    private int usage_limit;
    private int used_count;
}
