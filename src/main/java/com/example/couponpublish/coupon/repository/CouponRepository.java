package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
