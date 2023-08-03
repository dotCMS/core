package com.dotcms.util.jackson;

import static com.dotcms.util.ReflectionUtils.getClassFor;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.function.Function;

public class PayloadDeserializer extends JsonDeserializer<Payload> {

    public static final String TYPE = "type";
    public static final String DATA = "data";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_VALUE = "visibilityValue";
    public static final String VISIBILITY_TYPE = "visibilityType";

    @Override
    public Payload deserialize(final JsonParser parser, final DeserializationContext context)
            throws IOException {

        final JsonNode node = parser.readValueAsTree();
        final String payLoadType = Try.of(()-> node.get(TYPE).asText()).getOrElseThrow((Function<Throwable, IOException>) IOException::new);
        final Class<?> payLoadClazz = getClassFor(payLoadType);

        final Object payLoadData = readValueAs(node.get(DATA), payLoadClazz, parser);

        Object visibilityValue = null;

        final Visibility visibility = Try.of(()->  Visibility.valueOf(node.get(VISIBILITY).asText())).getOrElse(Visibility.GLOBAL);
        final String visibilityType = Try.of(()-> node.get(VISIBILITY_TYPE).asText()).getOrNull();
        if(null != visibilityType) {
            final Class<?> visibilityClazz = getClassFor(visibilityType);
            visibilityValue = readValueAs(node.get(VISIBILITY_VALUE), visibilityClazz, parser);

        }
        return new Payload(payLoadData, visibility, visibilityValue);
    }

    /**
     *  This fires the deserialization of a sub-json block into a separate class as we instruct
     * @param node sub-json block
     * @param clazz Target Bean Class
     * @param parser Parser
     * @param <T>
     * @return Bean instance
     * @throws IOException
     */
    private <T> T readValueAs (final JsonNode node, final Class<T> clazz, final JsonParser parser)
            throws IOException {
        final JsonParser nodeParser = node.traverse();
        nodeParser.setCodec(parser.getCodec());
        return nodeParser.readValueAs(clazz);
    }

}
