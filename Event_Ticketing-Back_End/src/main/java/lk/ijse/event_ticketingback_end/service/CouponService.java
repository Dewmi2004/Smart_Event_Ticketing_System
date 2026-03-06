package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.CouponDto;

import java.util.List;

public interface CouponService {
    public void saveCoupon(CouponDto couponDto);
    public void updateCoupon(CouponDto couponDto);
    public void deleteCoupon(CouponDto couponDto);
    public List<CouponDto> getAllCoupons();
}
