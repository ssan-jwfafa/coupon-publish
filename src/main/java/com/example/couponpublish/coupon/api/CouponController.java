package com.example.couponpublish.coupon.api;

import com.example.couponpublish.coupon.application.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponIssueResponse issue(@Valid @RequestBody CouponIssueRequest request) {
        return couponService.issue(request.userId());
    }

    @GetMapping("/issues/{userId}")
    public CouponIssueResponse getIssue(@PathVariable String userId) {
        return couponService.getIssue(userId);
    }

    @GetMapping("/remaining")
    public CouponRemainingResponse getRemainingCount() {
        return couponService.getRemainingCount();
    }

    @DeleteMapping("/issues/{userId}")
    public CouponIssueResponse cancel(@PathVariable String userId) {
        return couponService.cancel(userId);
    }
}
