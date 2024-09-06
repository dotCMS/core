package com.dotcms.rendering.js.proxy;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

/**
 * Serializer for the JsContentMap
 * @author jsanca
 */
public class JsContentMapSerializer  extends JsonSerializer<JsContentMap> {

    static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    @Override
    public void serialize(final JsContentMap jsContentMap,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        final ObjectWriter objectWriter = MAPPER.writer().withDefaultPrettyPrinter();
        final Map<String, Object> map = WorkflowHelper.getInstance().contentletToMap(jsContentMap.getContentMapObject().getContentObject());
        final String json = objectWriter.writeValueAsString(map);
        jsonGenerator.writeRawValue(json);
    }
}
