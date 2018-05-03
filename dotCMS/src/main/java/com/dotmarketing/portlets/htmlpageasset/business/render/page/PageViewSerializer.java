package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JsonSerializer of {@link PageView}
 */
public class PageViewSerializer extends JsonSerializer<PageView> {

    @Override
    public void serialize(PageView pageView, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
        builder.putAll(getObjectMap(pageView));
        String json = objectWriter.writeValueAsString(builder.build());
        jsonGenerator.writeRaw(json);
    }

    protected Map<String, Object> getObjectMap(PageView pageView) {
        final Template template = pageView.getTemplate();

        Map pageViewMap = new HashMap<String, Object>();
        pageViewMap.put("page", this.asMap(pageView.getPageInfo()));
        pageViewMap.put("containers", pageView.getContainers()
                .stream()
                .collect(Collectors.toMap(
                        containerRendered -> containerRendered.getContainer().getIdentifier(),
                        containerRendered -> containerRendered
                ))
        );
        pageViewMap.put("template", this.asMap(template));
        pageViewMap.put("site", pageView.getSite());

        if (pageView.getLayout() != null) {
            pageViewMap.put("layout", pageView.getLayout());
        }
        return pageViewMap;
    }

    private Map<Object, Object> asMap(final Object object)  {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

        try {
            String json = objectWriter.writeValueAsString(object);
            Map map = JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);
            map.values().removeIf(Objects::isNull);
            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }
}
