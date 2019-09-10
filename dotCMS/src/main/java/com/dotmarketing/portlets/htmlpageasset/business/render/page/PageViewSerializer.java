package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;

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
        pageViewMap.put("page", this.asMap(pageView.getPageInfo()));
        pageViewMap.put("containers", getContainersMap(pageView));

        final Map<Object, Object> templateMap = this.asMap(template);
        templateMap.put("canEdit", pageView.canEditTemplate());
        pageViewMap.put("template", templateMap);
        
        pageViewMap.put("site", pageView.getSite());
        pageViewMap.put("viewAs", pageView.getViewAs());
        pageViewMap.put("canCreateTemplate", pageView.canCreateTemplate());

        if(LicenseManager.getInstance().isEnterprise()) {
            pageViewMap.put("personalizationNumber", pageView.getPersonalizationNumber());
        }

        if (pageView.getLayout() != null) {
            pageViewMap.put("layout", pageView.getLayout());
            pageViewMap.put("numberContents", pageView.getNumberContents());
        }
        return pageViewMap;
    }

    private Map<String, Map> getContainersMap(final PageView pageView) {
        return pageView.getContainersMap().entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            final ContainerRaw containerRaw = entry.getValue();

                            return map(
                                    "container", containerRaw.getContainer(),
                                    "containerStructures", containerRaw.getContainerStructures(),
                                    "contentlets", PageViewSerializer.getContentsAsMap(containerRaw, pageView.getUser())
                                );
                        }
                )
        );
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

    private static Map<String, Object> getContentsAsMap (final ContainerRaw containerRaw, final User user) {
        return containerRaw.getContentlets().entrySet().stream().collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            contentEntry ->
                                    PageViewSerializer.getContentsAsMap(contentEntry.getValue(), user)
                    )
                );
    }

    private static List<Map<String, Object>> getContentsAsMap(
            final Collection<Contentlet> contents,
            final User user) {

        try {
            final List<Map<String, Object>> result = new ArrayList<>();

            for (final Contentlet contentlet : contents) {
                try {
                    final Map<String, Object> contentPrintableMap = ContentletUtil.getContentPrintableMap(user, contentlet);
                    contentPrintableMap.put("contentType", contentlet.getContentType().variable());
                    result.add(contentPrintableMap);
                } catch (IOException e) {
                    throw new DotStateException(e);
                }
            }

            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}
