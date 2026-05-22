package com.couponpublish.order.flink;

import com.couponpublish.order.entity.OrderEventType;
import com.couponpublish.order.entity.OrderStatus;
import com.couponpublish.order.entity.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

public class OrderLifecycleEventDeserializationSchema
    implements DeserializationSchema<OrderLifecycleStatisticsEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Override
    public OrderLifecycleStatisticsEvent deserialize(byte[] message) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(message);
        return new OrderLifecycleStatisticsEvent(
            node.path("eventId").isMissingNode() || node.path("eventId").isNull()
                ? null
                : node.path("eventId").longValue(),
            node.path("orderId").asText(),
            enumValue(OrderEventType.class, node.path("type")),
            node.path("message").asText(),
            enumValue(OrderStatus.class, node.path("previousStatus")),
            enumValue(OrderStatus.class, node.path("status")),
            enumValue(PaymentMethod.class, node.path("paymentMethod")),
            node.path("amount").longValue(),
            dateTime(node.path("occurredAt"))
        );
    }

    @Override
    public boolean isEndOfStream(OrderLifecycleStatisticsEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<OrderLifecycleStatisticsEvent> getProducedType() {
        return TypeInformation.of(OrderLifecycleStatisticsEvent.class);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonNode node) {
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return Enum.valueOf(type, node.asText());
    }

    private static String dateTime(JsonNode dateTimeNode) {
        if (dateTimeNode.isArray()) {
            return LocalDateTime.of(
                dateTimeNode.get(0).asInt(),
                dateTimeNode.get(1).asInt(),
                dateTimeNode.get(2).asInt(),
                dateTimeNode.get(3).asInt(),
                dateTimeNode.get(4).asInt(),
                dateTimeNode.get(5).asInt(),
                dateTimeNode.size() > 6 ? dateTimeNode.get(6).asInt() : 0
            ).toString();
        }
        return dateTimeNode.asText();
    }
}
