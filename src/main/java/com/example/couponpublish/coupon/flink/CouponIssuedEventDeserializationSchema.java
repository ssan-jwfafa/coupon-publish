package com.example.couponpublish.coupon.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

public class CouponIssuedEventDeserializationSchema implements DeserializationSchema<CouponIssuedStatisticsEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Override
    public CouponIssuedStatisticsEvent deserialize(byte[] message) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(message);
        return new CouponIssuedStatisticsEvent(
            node.path("couponIssueId").longValue(),
            node.path("couponId").longValue(),
            node.path("userId").asText(),
            issuedAt(node.path("issuedAt"))
        );
    }

    @Override
    public boolean isEndOfStream(CouponIssuedStatisticsEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<CouponIssuedStatisticsEvent> getProducedType() {
        return TypeInformation.of(CouponIssuedStatisticsEvent.class);
    }

    private static String issuedAt(JsonNode issuedAtNode) {
        if (issuedAtNode.isArray()) {
            return LocalDateTime.of(
                issuedAtNode.get(0).asInt(),
                issuedAtNode.get(1).asInt(),
                issuedAtNode.get(2).asInt(),
                issuedAtNode.get(3).asInt(),
                issuedAtNode.get(4).asInt(),
                issuedAtNode.get(5).asInt(),
                issuedAtNode.size() > 6 ? issuedAtNode.get(6).asInt() : 0
            ).toString();
        }
        return issuedAtNode.asText();
    }
}
