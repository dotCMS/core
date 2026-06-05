package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
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
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reproduction for <a href="https://github.com/dotCMS/core/issues/36026">#36026</a>:
 * the Elasticsearch → OpenSearch migration changed the return type of the aggregations exposed to
 * Velocity, silently breaking existing VTL templates that read aggregations from
 * {@link ESContentTool#search(String)} ({@code $estool.search(...)}).
 *
 * <ul>
 *   <li><b>Before:</b> {@code $results.aggregations} was an
 *       {@code org.elasticsearch.search.aggregations.Aggregations}. Templates walked
 *       {@code aggregations.<name>.buckets} → {@code bucket.getKeyAsNumber()} /
 *       {@code bucket.getDocCount()} → {@code bucket.getAggregations().get("top_content")} →
 *       {@code topHits.getHits().getHits()}.</li>
 *   <li><b>After:</b> it is a {@code Map<String, List<}{@link AggregationBucket}{@code >>}.
 *       {@link AggregationBucket} exposes only {@code key()} (String) and {@code docCount()} (long),
 *       and nested sub-aggregations are dropped.</li>
 * </ul>
 *
 * <p>Three distinct regressions, all exercised here:</p>
 * <ol>
 *   <li>{@code .buckets} no longer exists — {@code aggregations.<name>} <i>is</i> the bucket list.</li>
 *   <li>{@code getKeyAsNumber()} / {@code getDocCount()} are gone; the fluent {@code key()} /
 *       {@code docCount()} are not reachable as Velocity properties (no {@code get/is} prefix).</li>
 *   <li>nested {@code top_hits} per bucket is unrecoverable through the neutral model.</li>
 * </ol>
 *
 * <p>This class is the entry point for the fix: {@link #verbatimCustomerVtl_reproducesRegression()}
 * encodes the broken behaviour, and {@link #raw_aggregationDataIsPresent()} proves the bucket data
 * <i>is</i> in the response — so the template simply can no longer reach it.</p>
 */
public class ContentSearchToolTest extends IntegrationTestBase {

    private static User systemUser;
    private static Host defaultHost;
    private static Language defaultLanguage;

    /** Flat terms aggregation on the {@code contentType} system field — same shape the customer uses. */
    private static final String AGG_QUERY =
            "{\"aggs\":{\"content_types\":{\"terms\":{\"field\":\"contentType\",\"size\":5}}},"
                    + "\"size\":0,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"live\":true}}]}}}";

    /**
     * The customer template, <b>verbatim</b>, walking the aggregation the legacy way
     * ({@code .buckets}, {@code getKeyAsNumber()}, {@code getDocCount()}, nested {@code top_content}).
     */
    private static final String CUSTOMER_VTL = """
            $response.setContentType("text/plain")
            $response.setHeader("Cache-Control", "no-cache")

            #set($esQuery = '{"aggs":{"content_types":{"terms":{"field":"contentType","size":5},"aggs":{"top_content":{"top_hits":{"size":3}}}}},"size":5,"query":{"bool":{"filter":[{"term":{"live":true}}]}}}')

            ## --- search() ---
            #set($results = $estool.search($esQuery))
            totalResults: $!{results.totalResults}
            aggregations: $!{results.aggregations}
            CT: $!{results.aggregations.content_types}

            #set($contentTypeGroups = $results.aggregations.content_types.buckets)
            buckets: $!{contentTypeGroups}

            #foreach($group in $contentTypeGroups)
              key: $!{group.getKeyAsNumber()}
              docCount: $!{group.getDocCount()}
              #set($topHits = $group.getAggregations().get("top_content"))
              #foreach($hit in $topHits.getHits().getHits())
                hit id: $!{hit.id}
              #end
            #end
            """;

    /**
     * The same walk <b>migrated to the neutral API</b>: {@code aggregations.content_types} is the
     * {@code List<AggregationBucket>} directly (no {@code .buckets}), and a bucket exposes
     * {@code key()} / {@code docCount()} called explicitly. There is no neutral equivalent for the
     * nested {@code top_content} top_hits.
     */
    private static final String MIGRATED_VTL = """
            #set($esQuery = '{"aggs":{"content_types":{"terms":{"field":"contentType","size":5}}},"size":0,"query":{"bool":{"filter":[{"term":{"live":true}}]}}}')
            #set($results = $estool.search($esQuery))
            #set($contentTypeGroups = $results.aggregations.content_types)
            #foreach($group in $contentTypeGroups)
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
        // `contentType` returns at least one bucket with a doc count.
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
     * Executes the customer's VTL verbatim through the dotCMS Velocity engine and asserts the
     * regression: the aggregation data is present ({@code content_types} renders), yet the
     * {@code .buckets} access returns nothing, so the {@code #foreach} over the buckets never runs and
     * no {@code key:} / {@code docCount:} / {@code hit id:} lines are emitted.
     */
    @Test
    public void verbatimCustomerVtl_reproducesRegression() throws Exception {
        final String output = VelocityUtil.eval(CUSTOMER_VTL, velocityContext());
        Logger.info(this, "\n===== customer VTL output =====\n" + output + "\n================================");

        assertTrue("Aggregation data should be present in the response (the map renders content_types)",
                output.contains("content_types"));
        assertFalse("REGRESSION (#36026): 'aggregations.content_types' is now a List<AggregationBucket>, "
                        + "so '.buckets' resolves to null, the bucket loop runs 0 times and no 'key:' "
                        + "line is emitted. The data is there but the legacy template cannot reach it.",
                output.contains("key:"));
    }

    /**
     * Proves the neutral API is usable for the flat terms aggregation once the template drops
     * {@code .buckets} and calls {@code key()} / {@code docCount()} directly. (Documents the migration
     * path; the nested {@code top_hits} still has no neutral equivalent — see #36026 AC.)
     */
    @Test
    public void migratedVtl_emitsBuckets() throws Exception {
        final String output = VelocityUtil.eval(MIGRATED_VTL, velocityContext());
        Logger.info(this, "\n===== migrated VTL output =====\n" + output + "\n================================");

        assertTrue("Migrated template should emit at least one bucket key", output.contains("key:"));
        assertTrue("Migrated template should emit doc counts", output.contains("docCount:"));
    }

    /**
     * Confirms the bucket data really is in the response, so the regression is purely an access/typing
     * problem in VTL — not missing data or a failed query.
     */
    @Test
    public void raw_aggregationDataIsPresent() throws Exception {
        final ContentSearchResponse response = liveContentTool().raw(AGG_QUERY);
        final Map<String, List<AggregationBucket>> aggregations = response.aggregations();
        Logger.info(this, "raw aggregations=" + aggregations);

        assertTrue("Response must contain the content_types aggregation",
                aggregations.containsKey("content_types"));
        assertFalse("content_types aggregation must have at least one bucket",
                aggregations.get("content_types").isEmpty());
    }
}
