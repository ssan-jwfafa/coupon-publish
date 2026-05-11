package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    Page<CouponIssue> findAllByCoupon_Id(Long couponId, Pageable pageable);

    Page<CouponIssue> findAllByCoupon_IdAndStatus(Long couponId, CouponStatus status, Pageable pageable);

    List<CouponIssue> findAllByCoupon_IdAndStatus(Long couponId, CouponStatus status);

    long countByCoupon_IdAndStatus(Long couponId, CouponStatus status);

    Optional<CouponIssue> findByCoupon_IdAndUserId(Long couponId, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponIssue c where c.coupon.id = :couponId and c.userId = :userId")
    Optional<CouponIssue> findByCouponIdAndUserIdForUpdate(
        @Param("couponId") Long couponId,
        @Param("userId") String userId
    );
}
