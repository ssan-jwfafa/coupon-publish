package com.example.couponpublish.coupon.flink;

import com.example.couponpublish.coupon.event.CouponIssuedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

public class CouponIssuedEventDeserializationSchema implements DeserializationSchema<CouponIssuedEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Override
    public CouponIssuedEvent deserialize(byte[] message) throws IOException {
        return OBJECT_MAPPER.readValue(message, CouponIssuedEvent.class);
    }

    @Override
    public boolean isEndOfStream(CouponIssuedEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<CouponIssuedEvent> getProducedType() {
        return TypeInformation.of(CouponIssuedEvent.class);
    }
}
