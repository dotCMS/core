package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * This JSON Serializer is used to transform a {@link PageView} object into a JSON string.
 *
 * @author Freddy Rodriguez
 * @since May 4th, 2018
 */
public class PageViewSerializer extends JsonSerializer<PageView> {

    @Override
    public void serialize(final PageView pageView, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
        builder.putAll(getObjectMap(pageView));
        final String json = objectWriter.writeValueAsString(builder.build());
        jsonGenerator.writeRawValue(json);
    }

    /**
     * Generates the Map with the expected properties from the HTML Page that will be exposed in the
     * JSON response.
     *
     * @param pageView The {@link PageView} object to serialize.
     *
     * @return A Map with the HTML Page properties.
     */
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

        if (pageView.getRunningExperiment() != null) {
            pageViewMap.put("runningExperimentId", pageView.getRunningExperiment().getIdentifier());
        }
        if (null != pageView.getVanityUrl()) {
            pageViewMap.put("vanityUrl", pageView.getVanityUrl());
        }
        return pageViewMap;
    }

    protected void createObjectMapUrlContent(final Contentlet urlContent, final Map<String, Object> pageViewMap) {

        Try.run(()->addRelationships(urlContent))
                .onFailure(e -> Logger.error(PageViewSerializer.class, e.getMessage(), e));

        final DotContentletTransformer transformer   = new DotTransformerBuilder().urlContentMapTransformer().content(urlContent).build();
        final Map<String, Object>   urlContentletMap = transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);

        pageViewMap.put("urlContentMap", urlContentletMap);
    }

    private static void addRelationships(final Contentlet urlContent) {

        final HttpServletRequest request   = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        if (null != request && null != response) {

            final User user         = WebAPILocator.getUserWebAPI().getUser(request);
            final PageMode mode     = PageMode.get(request);
            final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);

            if (null != user && null != mode && null != language) {

                ContentUtils.addRelationships(urlContent, user, mode, language.getId());
            }
        }
    }


    private Map<Object, Object> asPageMap(final PageView pageView)  {

        addRelationships(pageView.getPage());

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
