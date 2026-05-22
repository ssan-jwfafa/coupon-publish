package com.couponpublish.order.event;

public interface OrderEventPublisher {

    void publish(OrderLifecycleEvent event);
}
