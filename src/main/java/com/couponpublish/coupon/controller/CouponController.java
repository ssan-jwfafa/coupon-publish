package com.couponpublish.coupon.controller;

import com.couponpublish.coupon.dto.CouponCreateRequest;
import com.couponpublish.coupon.dto.CouponIssuePageResponse;
import com.couponpublish.coupon.dto.CouponIssueRequest;
import com.couponpublish.coupon.dto.CouponIssueResponse;
import com.couponpublish.coupon.dto.CouponIssueStatisticsResponse;
import com.couponpublish.coupon.dto.CouponRemainingResponse;
import com.couponpublish.coupon.dto.CouponResponse;
import com.couponpublish.coupon.entity.CouponStatus;
import com.couponpublish.coupon.service.CouponIssueStatisticsService;
import com.couponpublish.coupon.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueStatisticsService statisticsService;

    public CouponController(CouponService couponService, CouponIssueStatisticsService statisticsService) {
        this.couponService = couponService;
        this.statisticsService = statisticsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return couponService.createCoupon(request);
    }

    @GetMapping("/{couponId}")
    public CouponResponse getCoupon(@PathVariable Long couponId) {
        return couponService.getCoupon(couponId);
    }

    @GetMapping
    public List<CouponResponse> getCoupons() {
        return couponService.getCoupons();
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
    }

    @PostMapping("/{couponId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponIssueResponse issue(
        @PathVariable Long couponId,
        @Valid @RequestBody CouponIssueRequest request
    ) {
        return couponService.issue(couponId, request.userId());
    }

    @GetMapping("/{couponId}/issues/{userId}")
    public CouponIssueResponse getIssue(@PathVariable Long couponId, @PathVariable String userId) {
        return couponService.getIssue(couponId, userId);
    }

    @GetMapping("/{couponId}/issues")
    public CouponIssuePageResponse getIssues(
        @PathVariable Long couponId,
        @RequestParam(required = false) CouponStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return couponService.getIssues(couponId, status, page, size);
    }

    @GetMapping("/{couponId}/remaining")
    public CouponRemainingResponse getRemainingCount(@PathVariable Long couponId) {
        return couponService.getRemainingCount(couponId);
    }

    @GetMapping("/{couponId}/statistics")
    public CouponIssueStatisticsResponse getStatistics(@PathVariable Long couponId) {
        return statisticsService.getStatistics(couponId);
    }

    @DeleteMapping("/{couponId}/issues/{userId}")
    public CouponIssueResponse cancel(@PathVariable Long couponId, @PathVariable String userId) {
        return couponService.cancel(couponId, userId);
    }
}
