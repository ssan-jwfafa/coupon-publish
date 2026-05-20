package com.example.couponpublish.order.dto;

import com.example.couponpublish.order.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    @NotBlank(message = "고객명은 필수입니다.")
    @Size(max = 40, message = "고객명은 40자 이하여야 합니다.")
    String customerName,

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 120, message = "상품명은 120자 이하여야 합니다.")
    String productName,

    @NotNull(message = "결제수단은 필수입니다.")
    PaymentMethod paymentMethod,

    @Min(value = 1, message = "주문 금액은 1원 이상이어야 합니다.")
    long amount,

    @Size(max = 40, message = "쿠폰 코드는 40자 이하여야 합니다.")
    String couponCode,

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 160, message = "주소는 160자 이하여야 합니다.")
    String address
) {
}
