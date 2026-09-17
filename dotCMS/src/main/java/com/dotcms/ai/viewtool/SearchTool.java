package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The SearchTool class provides methods for querying and retrieving related content based on embeddings.
 * It uses the EmbeddingsAPI to perform these operations.
 *
 * This class is a ViewTool, meaning it can be used in Velocity templates to provide functionality related to embeddings.
 *
 * <p>Output is HTML-escaped by default (#37153): the echoed {@code query} and each match's
 * {@code extractedText} are encoded (the latter because the embeddings API stores every visitor's query
 * text in the {@code cache} index, which a template can search); error payloads are escaped in full.
 * {@code title} and the contentlet fields under {@code dotCMSResults} are returned as stored. Templates
 * that need the unescaped payload go through {@code $ai.unsafe.search}, which constructs this tool with
 * escaping off and behaves exactly as before #37153.
 */
public class SearchTool implements ViewTool {

    private static final String STACKTRACE_KEY = "stackTrace";

    private final HttpServletRequest request;
    private final Host host;
    private final boolean escapeOutput;

    SearchTool(final Object initData) {
        this(initData, true);
    }

    /**
     * @param initData     the Velocity {@link ViewContext}
     * @param escapeOutput {@code true} to HTML-escape results (the {@code $ai} default),
     *                     {@code false} for the {@code $ai.unsafe} behaviour
     */
    SearchTool(final Object initData, final boolean escapeOutput) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = host();
        this.escapeOutput = escapeOutput;
    }

    @Override
    public void init(final Object initData) {
        /* unneeded because of constructor */
    }

    /**
     * Queries for content related to the provided query string and index name.
     * The method uses the EmbeddingsAPI to perform the search.
     *
     * @param query the query string
     * @param indexName the name of the index to search in
     * @return the search results
     */
    public Object query(final String query, final String indexName) {
        final User user = PortalUtil.getUser(request);
        final EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                .withQuery(query)
                .withIndexName(indexName)
                .withUser(user)
                .withLimit(50)
                .withThreshold(.5f)
                .build();

        try {
            return escapeSearchShaped(APILocator.getDotAIAPI().getEmbeddingsAPI(host).searchForContent(searcher));
        } catch (Exception e) {
            return escape(Map.of(AiKeys.ERROR, e.getMessage(), STACKTRACE_KEY, Arrays.asList(e.getStackTrace())));
        }
    }

    /**
     * Queries for content related to the provided map of parameters.
     * The method uses the EmbeddingsAPI to perform the search.
     *
     * @param mapIn the map of parameters
     * @return the search results
     */
    public Object query(final Map<String, Object> mapIn) {
        final User user = PortalUtil.getUser(request);
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(mapIn).withUser(user).build();

        try {
            return escapeSearchShaped(APILocator.getDotAIAPI().getEmbeddingsAPI(host).searchForContent(searcher));
        } catch (Exception e) {
            return escape(Map.of(AiKeys.ERROR, e.getMessage(), STACKTRACE_KEY, Arrays.asList(e.getStackTrace())));
        }
    }

    /**
     * Queries for content related to the provided query string.
     * The method uses the EmbeddingsAPI to perform the search.
     *
     * @param query the query string
     * @return the search results
     */
    public Object query(final String query) {
        return query(query, AiKeys.DEFAULT);
    }

    /**
     * Retrieves content related to the provided ContentMap and index name.
     * The method uses the EmbeddingsAPI to perform the search.
     *
     * @param contentMap the ContentMap to find related content for
     * @param indexName the name of the index to search in
     * @return the search results
     */
    public Object related(final ContentMap contentMap, final String indexName) {
        return related(contentMap.getContentObject(), indexName);
    }

    /**
     * Retrieves content related to the provided Contentlet and index name.
     * The method uses the EmbeddingsAPI to perform the search.
     *
     * @param contentlet the Contentlet to find related content for
     * @param indexName the name of the index to search in
     * @return the search results
     */
    public Object related(final Contentlet contentlet, final String indexName) {
        try {
            final User user = PortalUtil.getUser(request);
            final List<Field> fields = ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet);

            final Optional<String> contentToRelate = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
            if (contentToRelate.isEmpty()) {
                return new JSONObject();
            }

            final EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                    .withQuery(contentToRelate.get())
                    .withIndexName(indexName)
                    .withExcludeIndentifiers(new String[] {contentlet.getIdentifier()})
                    .withUser(user)
                    .withLimit(50)
                    .withThreshold(.5f)
                    .build();
            return escapeSearchShaped(APILocator.getDotAIAPI().getEmbeddingsAPI(host).searchForContent(searcher));
        } catch (Exception e) {
            return escape(Map.of(AiKeys.ERROR, e.getMessage(), STACKTRACE_KEY, Arrays.asList(e.getStackTrace())));
        }
    }

    /** Whole-payload escaping (error payloads), or pass-through when unsafe. */
    private Object escape(final Object payload) {
        return escapeOutput ? AIViewToolOutputEscaper.deepEscape(payload) : payload;
    }

    /** By-source escaping for the search shape, or pass-through when unsafe. */
    private Object escapeSearchShaped(final Object payload) {
        return escapeOutput ? AIViewToolOutputEscaper.escapeSearchShaped(payload) : payload;
    }

    @VisibleForTesting
    Host host() {
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
    }

}
