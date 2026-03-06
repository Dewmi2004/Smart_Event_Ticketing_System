package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.CouponDto;
import lk.ijse.event_ticketingback_end.service.CouponService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/coupon")
@CrossOrigin
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<APIResponse<String>> saveCoupon(@RequestBody CouponDto couponDto) {
        couponService.saveCoupon(couponDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Coupon Saved", null), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<APIResponse<String>> updateCoupon(@RequestBody CouponDto couponDto) {
        couponService.updateCoupon(couponDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Coupon Updated", null), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<APIResponse<String>> deleteCoupon(@RequestBody CouponDto couponDto) {
        couponService.deleteCoupon(couponDto);
        return new ResponseEntity<>(new APIResponse<>(200, "Coupon Deleted", null), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<CouponDto>>> getAllCoupons() {
        List<CouponDto> coupons = couponService.getAllCoupons();
        return new ResponseEntity<>(new APIResponse<>(200, "Success", coupons), HttpStatus.OK);
    }
}