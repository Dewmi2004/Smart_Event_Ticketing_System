package lk.ijse.event_ticketingback_end.service.impl;

import lk.ijse.event_ticketingback_end.dto.CouponDto;
import lk.ijse.event_ticketingback_end.entity.Coupon;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.CouponRepository;
import lk.ijse.event_ticketingback_end.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final ModelMapper modelMapper;

    @Override
    public void saveCoupon(CouponDto couponDto) {
        couponDto.setCoupon_id(0);
        Coupon coupon = modelMapper.map(couponDto, Coupon.class);
        couponRepository.save(coupon);
    }

    @Override
    public void updateCoupon(CouponDto couponDto) {
        Coupon existingCoupon = couponRepository
                .findById(couponDto.getCoupon_id())
                .orElseThrow(() -> new EventNotFoundException(
                        "Coupon not found with ID: " + couponDto.getCoupon_id()));

        existingCoupon.setCoupon_code(couponDto.getCoupon_code());
        existingCoupon.setDiscount_type(couponDto.getDiscount_type());
        existingCoupon.setDiscount_value(couponDto.getDiscount_value());
        existingCoupon.setExpiration_date(couponDto.getExpiration_date());
        existingCoupon.setUsage_limit(couponDto.getUsage_limit());
        existingCoupon.setUsed_count(couponDto.getUsed_count());

        couponRepository.saveAndFlush(existingCoupon);
    }

    @Override
    public void deleteCoupon(CouponDto couponDto) {
        Coupon existingCoupon = couponRepository
                .findById(couponDto.getCoupon_id())
                .orElseThrow(() -> new EventNotFoundException(
                        "Coupon not found with ID: " + couponDto.getCoupon_id()));

        couponRepository.delete(existingCoupon);
        couponRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> getAllCoupons() {
        List<Coupon> list = couponRepository.findAll();
        return modelMapper.map(list, new TypeToken<List<CouponDto>>() {}.getType());
    }
}