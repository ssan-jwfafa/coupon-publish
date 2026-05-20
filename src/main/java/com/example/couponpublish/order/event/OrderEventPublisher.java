package com.example.couponpublish.order.event;

public interface OrderEventPublisher {

    void publish(OrderLifecycleEvent event);
}
