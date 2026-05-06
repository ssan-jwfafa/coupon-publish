package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    Optional<CouponIssue> findByUserId(String userId);

    List<CouponIssue> findAllByStatus(CouponStatus status);

    long countByStatus(CouponStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponIssue c where c.userId = :userId")
    Optional<CouponIssue> findByUserIdForUpdate(@Param("userId") String userId);
}
