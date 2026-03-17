package lk.ijse.event_ticketingback_end.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponDto {
    private int coupon_id;
    private String couponCode;
    private String discount_type;
    private int discount_value;
    private Date expiration_date;
    private int usage_limit;
    private int used_count;
}
