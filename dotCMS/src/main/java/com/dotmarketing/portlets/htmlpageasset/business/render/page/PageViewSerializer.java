package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
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
import java.util.function.Supplier;

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

    /**
     * Adds the URL-mapped content to the page map under {@code urlContentMap}, with the related
     * content the current request asks for. The given contentlet is left unchanged.
     *
     * @param urlContent  The contentlet the page URL maps to.
     * @param pageViewMap The page map being built.
     */
    protected void createObjectMapUrlContent(final Contentlet urlContent, final Map<String, Object> pageViewMap) {

        final Contentlet content = Try.of(() -> withRelationships(urlContent, Contentlet::new))
                .onFailure(e -> Logger.error(PageViewSerializer.class, e.getMessage(), e))
                .getOrElse(urlContent);

        final DotContentletTransformer transformer   = new DotTransformerBuilder().urlContentMapTransformer().content(content).build();
        final Map<String, Object>   urlContentletMap = transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);

        pageViewMap.put("urlContentMap", urlContentletMap);
    }

    /**
     * Returns the contentlet with the related content the current request asks for, as a copy
     * made by {@code newInstance}, or the contentlet itself when there is no current request,
     * user or language, or the request asks for no related content.
     *
     * @param contentlet  The contentlet to read the related content of.
     * @param newInstance Creates the instance that receives the copy.
     * @param <T>         The type of the contentlet, kept by the copy.
     *
     * @return The contentlet to serialize.
     */
    private static <T extends Contentlet> T withRelationships(final T contentlet,
                                                              final Supplier<T> newInstance) {

        final HttpServletRequest request   = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        if (null != request && null != response) {

            final User user         = WebAPILocator.getUserWebAPI().getUser(request);
            final PageMode mode     = PageMode.get(request);
            final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);

            if (null != user && null != mode && null != language) {

                return ContentUtils.copyWithRelationships(contentlet, newInstance, user, mode, language.getId());
            }
        }
        return contentlet;
    }


    /**
     * Generates the map of the HTML Page, with the related content the current request asks for.
     * The page object held by the {@link PageView} is left unchanged. If the related content
     * cannot be loaded, the error is logged and the page is returned without it, as for the
     * URL-mapped content.
     *
     * @param pageView The {@link PageView} whose page is serialized.
     *
     * @return A Map with the HTML Page properties.
     */
    private Map<Object, Object> asPageMap(final PageView pageView)  {

        final HTMLPageAsset page = Try.of(() -> withRelationships(pageView.getPage(), HTMLPageAsset::new))
                .onFailure(e -> Logger.error(PageViewSerializer.class, e.getMessage(), e))
                .getOrElse(pageView.getPage());

        final Map<Object, Object> pageMap = this.asMap(page);

        final String pageUrlMapper = pageView.getPageUrlMapper();

        if (pageUrlMapper != null) {
            pageMap.put("pageURI", pageUrlMapper);
        }

        pageMap.put("live", pageView.live);

        if (!pageView.live) {
            pageMap.remove("liveInode");
        }

        if (!pageView.getStyleEditorSchemas().isEmpty()) {
            pageMap.put("styleEditorSchemas", pageView.getStyleEditorSchemas());
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
