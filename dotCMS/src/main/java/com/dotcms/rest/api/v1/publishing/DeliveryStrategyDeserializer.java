package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Custom deserializer for {@link DeliveryStrategy} that provides a clean,
 * user-friendly error message without exposing internal Java class names.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
public class DeliveryStrategyDeserializer extends JsonDeserializer<DeliveryStrategy> {

    @Override
    public DeliveryStrategy deserialize(final JsonParser parser,
                                        final DeserializationContext context) throws IOException {

        final String value = parser.getValueAsString();
        try {
            return DeliveryStrategy.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            final String validValues = Arrays.stream(DeliveryStrategy.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(
                    "Invalid deliveryStrategy: '%s'. Valid values: %s", value, validValues));
        }
    }
}
