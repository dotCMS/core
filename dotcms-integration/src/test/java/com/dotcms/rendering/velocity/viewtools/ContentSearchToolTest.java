package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Fix verification for <a href="https://github.com/dotCMS/core/issues/36026">#36026</a>:
 * the Elasticsearch → OpenSearch migration changed the type exposed to Velocity as
 * {@code $results.aggregations} ({@code $estool.search(...)}), silently breaking existing VTL
 * templates that walked the aggregation tree the Elasticsearch way.
 *
 * <p>The fix restores a vendor-neutral aggregation tree that mirrors the legacy ES API shape:</p>
 * <ul>
 *   <li>{@code $results.aggregations.<name>} → {@link Aggregation} (via {@code Aggregations.get}).</li>
 *   <li>{@code .buckets} / {@code getKeyAsString()} / {@code getKeyAsNumber()} / {@code getDocCount()}
 *       on each {@link AggregationBucket}.</li>
 *   <li>nested {@code getAggregations().get("...")} and the {@code top_hits} metric aggregation,
 *       reachable as {@code $topHits.getHits().getHits()} → {@code $hit.id}.</li>
 * </ul>
 *
 * <p>{@link #verbatimCustomerVtl_rendersAfterFix()} runs the customer's template <b>unchanged</b>
 * and asserts it now renders buckets and nested hits. {@link #aggregationTreePreservesTopHits()}
 * locks the same guarantee at the API level, independent of Velocity.</p>
 */
public class ContentSearchToolTest extends IntegrationTestBase {

    private static User systemUser;
    private static Host defaultHost;
    private static Language defaultLanguage;

    // The raw() path does NOT normalize the query (unlike search(), which lowercases field names),
    // so these constants use the already-lowercase index field name `contenttype` directly.

    /** Flat terms aggregation on the {@code contenttype} system field — same shape the customer uses. */
    private static final String AGG_QUERY =
            "{\"aggs\":{\"content_types\":{\"terms\":{\"field\":\"contenttype\",\"size\":5}}},"
                    + "\"size\":0,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"live\":true}}]}}}";

    /** Terms aggregation with a nested {@code top_hits} sub-aggregation, as in the customer's query. */
    private static final String NESTED_QUERY =
            "{\"aggs\":{\"content_types\":{\"terms\":{\"field\":\"contenttype\",\"size\":5},"
                    + "\"aggs\":{\"top_content\":{\"top_hits\":{\"size\":3}}}}},"
                    + "\"size\":0,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"live\":true}}]}}}";

    /**
     * The customer template, <b>verbatim</b>, walking the aggregation the legacy Elasticsearch way
     * ({@code .buckets}, {@code getKeyAsNumber()}, {@code getDocCount()}, nested {@code top_content}
     * {@code top_hits}). After the fix this must render without any template change.
     */
    private static final String CUSTOMER_VTL = """
            $response.setContentType("text/plain")
            $response.setHeader("Cache-Control", "no-cache")

            #set($esQuery = '{
                "aggs": {
                    "content_types": {
                        "terms": { "field": "contentType", "size": 5 },
                        "aggs": {
                            "top_content": {
                                "top_hits": { "size": 3 }
                            }
                        }
                    }
                },
                "size": 5,
                "query": {
                    "bool": {
                        "filter": [ { "term": { "live": true } } ]
                    }
                }
            }')

            ## --- search() ---
            #set($results = $estool.search($esQuery))
            totalResults: $!{results.totalResults}
            aggregations: $!{results.aggregations}
            CT: $!{results.aggregations.content_types}

            #set($contentTypeGroups = $results.aggregations.content_types.buckets)
            buckets: $!{contentTypeGroups}

            #foreach($group in $contentTypeGroups)
              key: $!{group.getKeyAsString()}
              docCount: $!{group.getDocCount()}
              #set($topHits = $group.getAggregations().get("top_content"))
              #foreach($hit in $topHits.getHits().getHits())
                hit id: $!{hit.id}
              #end
            #end
            """;

    /**
     * The same walk using the fluent neutral accessors: iterate the {@link Aggregation} directly
     * (it is {@link Iterable} over its buckets) and call {@code key()} / {@code docCount()}.
     */
    private static final String NEUTRAL_VTL = """
            #set($esQuery = '{
                "aggs": {
                    "content_types": {
                        "terms": { "field": "contentType", "size": 5 }
                    }
                },
                "size": 0,
                "query": {
                    "bool": {
                        "filter": [ { "term": { "live": true } } ]
                    }
                }
            }')

            #set($results = $estool.search($esQuery))

            #foreach($group in $results.aggregations.content_types)
                key: $!{group.key()}
                docCount: $!{group.docCount()}
            #end
            """;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        defaultHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        // A content type with a few published, live contentlets guarantees the terms aggregation on
        // `contentType` returns at least one bucket with a doc count and nested top_hits.
        final Field title = new FieldDataGen()
                .name("title").velocityVarName("title").type(TextField.class).indexed(true).next();
        final ContentType contentType = new ContentTypeDataGen()
                .name("AggRegression QA " + System.currentTimeMillis())
                .host(defaultHost)
                .field(title)
                .nextPersisted();

        for (int i = 0; i < 3; i++) {
            final Contentlet contentlet = new ContentletDataGen(contentType)
                    .host(defaultHost)
                    .languageId(defaultLanguage.getId())
                    .setProperty("title", "agg-doc-" + i)
                    .nextPersisted();
            ContentletDataGen.publish(contentlet);
            APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(), true);
        }
    }

    /** Initializes an {@link ESContentTool} in LIVE mode with the system user (à la ContentToolTest). */
    private ESContentTool liveContentTool() {
        final ViewContext viewContext = mock(ViewContext.class);
        final Context velocityContext = mock(Context.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getParameter("host_id")).thenReturn(defaultHost.getInode());
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.name());
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final ESContentTool tool = new ESContentTool();
        tool.init(viewContext);
        return tool;
    }

    /** Builds a Velocity context with the live {@code $estool} and a mock {@code $response} bound. */
    private Context velocityContext() {
        final Context ctx = new VelocityContext();
        ctx.put("estool", liveContentTool());
        ctx.put("response", mock(HttpServletResponse.class));
        return ctx;
    }

    /**
     * Executes the customer's VTL <b>verbatim</b> through the dotCMS Velocity engine and asserts the
     * regression is fixed: the bucket loop now runs ({@code key:} / {@code docCount:} lines are
     * emitted with real counts) and the nested {@code top_hits} hits are reachable ({@code hit id:}
     * lines are emitted).
     */
    @Test
    public void verbatimCustomerVtl_rendersAfterFix() throws Exception {
        final String output = VelocityUtil.eval(CUSTOMER_VTL, velocityContext());
        Logger.info(this, "\n===== customer VTL output =====\n" + output + "\n================================");

        assertTrue("Aggregation data should be present in the response",
                output.contains("content_types"));
        assertTrue("FIX (#36026): the buckets loop must now execute, emitting 'key:' lines",
                output.contains("key:"));
        assertTrue("FIX (#36026): bucket doc counts must render with real numbers",
                Pattern.compile("docCount:\\s*\\d+").matcher(output).find());
        assertTrue("FIX (#36026): nested top_hits must be reachable, emitting 'hit id:' lines",
                output.contains("hit id:"));
    }

    /**
     * The fluent neutral form also works: iterating the {@link Aggregation} directly and calling
     * {@code key()} / {@code docCount()}.
     */
    @Test
    public void neutralFluentVtl_emitsBuckets() throws Exception {
        final String output = VelocityUtil.eval(NEUTRAL_VTL, velocityContext());
        Logger.info(this, "\n===== neutral VTL output =====\n" + output + "\n================================");

        assertTrue("Neutral template should emit at least one bucket key", output.contains("key:"));
        assertTrue("Neutral template should emit doc counts", output.contains("docCount:"));
    }

    /**
     * API-level guarantee (independent of Velocity): {@link ContentSearchResponse#aggregationTree()}
     * preserves the full tree — first-level terms buckets <i>and</i> the nested {@code top_hits}
     * with its hits.
     */
    @Test
    public void aggregationTreePreservesTopHits() throws Exception {
        final ContentSearchResponse response = liveContentTool().raw(NESTED_QUERY);
        final Map<String, Aggregation> tree = response.aggregationTree();
        Logger.info(this, "aggregationTree=" + tree);

        assertTrue("Tree must contain the content_types aggregation", tree.containsKey("content_types"));

        final Aggregation contentTypes = tree.get("content_types");
        assertNotNull("content_types aggregation must be present", contentTypes);
        assertFalse("content_types must have at least one bucket", contentTypes.getBuckets().isEmpty());

        final AggregationBucket firstBucket = contentTypes.getBuckets().get(0);
        assertTrue("bucket doc count must be positive", firstBucket.getDocCount() > 0);

        final Aggregation topContent = firstBucket.getAggregations().get("top_content");
        assertNotNull("nested top_hits sub-aggregation must be preserved", topContent);
        assertNotNull("top_hits must carry a SearchHits", topContent.getHits());
        assertFalse("top_hits must carry at least one hit", topContent.getHits().getHits().isEmpty());
    }

    /**
     * The flat first-level terms map ({@link ContentSearchResponse#aggregations()}) used by Java
     * callers remains populated and correct — the fix did not regress it.
     */
    @Test
    public void raw_flatAggregationsMapStillPopulated() throws Exception {
        final ContentSearchResponse response = liveContentTool().raw(AGG_QUERY);
        Logger.info(this, "raw aggregations=" + response.aggregations());

        assertTrue("Flat map must contain the content_types aggregation",
                response.aggregations().containsKey("content_types"));
        assertFalse("content_types aggregation must have at least one bucket",
                response.aggregations().get("content_types").isEmpty());
    }
}
