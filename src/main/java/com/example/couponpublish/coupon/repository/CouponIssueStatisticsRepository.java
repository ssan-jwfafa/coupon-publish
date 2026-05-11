package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.CouponIssueStatistics;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponIssueStatisticsRepository extends JpaRepository<CouponIssueStatistics, Long> {

    Optional<CouponIssueStatistics> findByCouponId(Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CouponIssueStatistics s where s.couponId = :couponId")
    Optional<CouponIssueStatistics> findByCouponIdForUpdate(@Param("couponId") Long couponId);
}
