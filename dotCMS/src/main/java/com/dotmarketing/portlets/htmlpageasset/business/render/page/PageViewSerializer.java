package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.contentlet.transform.strategy.KeyValueViewStrategy;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;

/**
 * JsonSerializer of {@link PageView}
 */
public class PageViewSerializer extends JsonSerializer<PageView> {

    @Override
    public void serialize(final PageView pageView, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
        builder.putAll(getObjectMap(pageView));
        final String json = objectWriter.writeValueAsString(builder.build());
        jsonGenerator.writeRawValue(json);
    }

    protected Map<String, Object> getObjectMap(final PageView pageView) {
        final Template template = pageView.getTemplate();

        final Map<String, Object> pageViewMap = new TreeMap<>();
        pageViewMap.put("page", this.asPageMap(pageView));
        pageViewMap.put("containers", pageView.getContainersMap());

        final Map<Object, Object> templateMap = this.asMap(template);
        templateMap.put("canEdit", pageView.canEditTemplate());
        pageViewMap.put("template", templateMap);
        
        pageViewMap.put("site", pageView.getSite());
        pageViewMap.put("viewAs", pageView.getViewAs());
        pageViewMap.put("canCreateTemplate", pageView.canCreateTemplate());
        pageViewMap.put("numberContents", pageView.getNumberContents());

        if (pageView.getLayout() != null) {
            pageViewMap.put("layout", pageView.getLayout());
        }

        if (null != pageView.getUrlContent()) {

            this.createObjectMapUrlContent(pageView.getUrlContent(), pageViewMap);
        }

        return pageViewMap;
    }

    protected void createObjectMapUrlContent(final Contentlet urlContent, final Map<String, Object> pageViewMap) {

        final DotContentletTransformer transformer   = new DotTransformerBuilder().urlContentMapTransformer().content(urlContent).build();
        final Map<String, Object>   urlContentletMap = transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);

        pageViewMap.put("urlContentMap", urlContentletMap);
    }

    private Map<Object, Object> asPageMap(final PageView pageView)  {
        final Map<Object, Object> pageMap = this.asMap(pageView.getPage());

        final String pageUrlMapper = pageView.getPageUrlMapper();

        if (pageUrlMapper != null) {
            pageMap.put("pageURI", pageUrlMapper);
        }

        pageMap.put("live", pageView.live);

        if (!pageView.live) {
            pageMap.remove("liveInode");
        }

        return pageMap;
    }

    private Map<Object, Object> asMap(final Object object)  {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

        try {
            final String json = objectWriter.writeValueAsString(object);
            final Map map = JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);

            map.values().removeIf(Objects::isNull);
            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }
}
