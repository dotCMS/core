package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
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
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
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

    /**
     * The <i>same</i> flat terms aggregation as {@link #AGG_QUERY} but with the field name in
     * camelCase ({@code contentType}) — exactly how the customer wrote it. The physical index field
     * is the all-lowercase {@code contenttype}.
     *
     * <p>After the fix, <b>both</b> paths resolve this: each lowercases the whole query (via
     * {@code StringUtils.lowercaseStringExceptMatchingTokens}) before executing it, so
     * {@code contentType} folds to {@code contenttype}. Before the fix, {@code raw()} forwarded the
     * query untouched, so {@code contentType} aggregated over a non-existent field and yielded zero
     * buckets — the customer's empty {@code getBuckets=[]}. The aggregation mapping itself was never
     * at fault (it is shared by both paths via
     * {@link ContentSearchResponse#from(org.elasticsearch.action.search.SearchResponse)}).
     */
    private static final String CAMELCASE_AGG_QUERY =
            "{\"aggs\":{\"content_types\":{\"terms\":{\"field\":\"contentType\",\"size\":5}}},"
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

    /**
     * The customer-style aggregation walk driven through {@code $estool.raw($esQuery)} instead of
     * {@code search()}. Two shapes are exercised against the same response:
     * <ul>
     *   <li>the full tree {@code $tree.content_types.buckets} (mirrors the {@code search()} template,
     *       including the nested {@code top_content} {@code top_hits});</li>
     *   <li>the flat first-level map {@code $flat.content_types}, which on a
     *       {@link ContentSearchResponse} is a {@code List<AggregationBucket>} you iterate directly
     *       (no {@code .buckets}) — the raw-specific shape that differs from {@code search()}.</li>
     * </ul>
     *
     * <p><b>Record accessor note:</b> {@code raw()} returns a {@link ContentSearchResponse}
     * <i>record</i>, whose accessors are {@code aggregations()} / {@code aggregationTree()} — there
     * are no {@code get}-prefixed methods. Velocity's property syntax ({@code $results.aggregations})
     * only resolves {@code getX()}/{@code isX()}/public fields, so on a record it would silently
     * yield {@code null}. The template therefore calls them with explicit method syntax
     * ({@code $results.aggregationTree()}). Everything downstream stays property-style because
     * {@link Aggregation}'s components are named {@code getBuckets}/{@code getHits} and
     * {@link AggregationBucket} exposes bean getters.</p>
     *
     * <p>Uses the camelCase {@code contentType} field name on purpose: after the fix, {@code raw()}
     * lowercases the whole query (via {@code StringUtils.lowercaseStringExceptMatchingTokens}, the
     * same helper {@code search()} uses), so this resolves to the physical {@code contenttype} field
     * and renders buckets just like {@code search()} — exercising the fix end-to-end through
     * Velocity.</p>
     */
    private static final String RAW_VTL = """
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
                "size": 0,
                "query": {
                    "bool": {
                        "filter": [ { "term": { "live": true } } ]
                    }
                }
            }')

            ## --- raw() --- record accessors need explicit () (see javadoc)
            #set($results = $estool.raw($esQuery))
            #set($tree = $results.aggregationTree())
            #set($flat = $results.aggregations())
            aggregations: $!{flat}
            tree CT: $!{tree.content_types}

            ## tree walk — same shape as the search() customer template
            #set($contentTypeGroups = $tree.content_types.buckets)
            tree buckets: $!{contentTypeGroups}
            #foreach($group in $contentTypeGroups)
              tree key: $!{group.getKeyAsString()}
              tree docCount: $!{group.getDocCount()}
              #set($topHits = $group.getAggregations().get("top_content"))
              #foreach($hit in $topHits.getHits().getHits())
                tree hit id: $!{hit.id}
              #end
            #end

            ## flat first-level map — raw-specific: content_types is a List<AggregationBucket>
            #foreach($bucket in $flat.content_types)
              flat key: $!{bucket.getKeyAsString()}
              flat docCount: $!{bucket.getDocCount()}
            #end
            """;

    /**
     * A plain document query ({@code size > 0}, no aggregations) so the response carries actual
     * search hits. Filters on {@code live:true} so it resolves against the live index in the
     * LIVE-mode tool, matching the contentlets published in {@link #prepare()}.
     */
    private static final String HITS_QUERY =
            "{\"size\":5,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"live\":true}}]}}}";

    /**
     * Locks the non-aggregation accessor surface of {@code $estool.search(...)} —
     * {@link ContentSearchResults} — that customer VTL relies on: top-level timing/count getters
     * ({@code totalResults}, {@code queryTook}, {@code scrollId}) and the hit walk
     * ({@code $results.hits.hits} → {@code $hit.id} / {@code $hit.index} / {@code $hit.sourceAsMap}).
     *
     * <p>{@link ContentSearchResults} keeps JavaBean getters ({@code getHits}, {@code getTotalResults},
     * {@code getQueryTook}, {@code getScrollId}), so property syntax ({@code $results.hits}) resolves.
     * The neutral hit records also name their components {@code getId}/{@code getIndex}/
     * {@code getSourceAsMap}, so {@code $hit.id} resolves too. {@code TotalHits.value()} is a bare
     * record accessor, hence the explicit {@code ()}. This is the back-compat safety net for
     * {@code $dotcontent.search(...)} templates that read hits/timing, not just aggregations.</p>
     */
    private static final String SEARCH_HITS_VTL = """
            #set($esQuery = '{"size":5,"query":{"bool":{"filter":[{"term":{"live":true}}]}}}')
            #set($results = $estool.search($esQuery))
            totalResults: $!{results.totalResults}
            queryTook: $!{results.queryTook}
            scrollId: [$!{results.scrollId}]
            totalHits: $!{results.hits.totalHits.value()}
            #foreach($hit in $results.hits.hits)
              hit id: $!{hit.id}
              hit index: $!{hit.index}
              hit source: $!{hit.sourceAsMap}
            #end
            """;

    /**
     * A field-sorted {@code $estool.search(...)} whose per-hit {@code sort} values the template reads
     * via {@code $hit.sortValues} (property → {@code getSortValues()}) and
     * {@code $hit.getSortValues().get(0)}. Guards the Velocity-facing side of
     * <a href="https://github.com/dotCMS/core/issues/36581">#36581</a>: the neutral {@link SearchHit}
     * now carries the sort values and exposes them under a bean-style {@code get}-accessor, so VTL
     * templates that read the per-hit sort value (e.g. the {@code moddate} sort key, or a
     * {@code _geo_distance} distance) resolve it instead of silently getting {@code null}.
     */
    private static final String SEARCH_SORT_VTL = """
            #set($esQuery = '{"size":5,"query":{"bool":{"filter":[{"term":{"live":true}}]}},"sort":[{"moddate":"desc"}]}')
            #set($results = $estool.search($esQuery))
            #foreach($hit in $results.hits.hits)
              hit id: $!{hit.id}
              sortValues: $!{hit.sortValues}
              firstSort: $!{hit.getSortValues().get(0)}
            #end
            """;

    /**
     * Locks the same non-aggregation accessor surface for {@code $estool.raw(...)} —
     * {@link ContentSearchResponse}. Because {@code raw()} returns a <i>record</i>, its top-level
     * accessors need explicit method syntax ({@code $results.tookMillis()}, {@code $results.hits()},
     * {@code $results.scrollId()}); property syntax ({@code $results.took}, {@code $results.hits})
     * silently yields {@code null} on a record. Downstream the neutral {@link SearchHits} /
     * {@link SearchHit} components are {@code get}-named ({@code getHits}, {@code getTotalHits},
     * {@code getId}...), so those resolve either way; {@code TotalHits.value()} needs the explicit
     * {@code ()}. This guards {@code $dotcontent.raw(...)} templates that read hits/timing.
     */
    private static final String RAW_HITS_VTL = """
            #set($esQuery = '{"size":5,"query":{"bool":{"filter":[{"term":{"live":true}}]}}}')
            #set($results = $estool.raw($esQuery))
            tookMillis: $!{results.tookMillis()}
            scrollId: [$!{results.scrollId()}]
            #set($hits = $results.hits())
            totalHits: $!{hits.getTotalHits().value()}
            #foreach($hit in $hits.getHits())
              raw hit id: $!{hit.id}
              raw hit index: $!{hit.index}
              raw hit source: $!{hit.sourceAsMap}
            #end
            """;

    /**
     * Legacy ES-style <b>property syntax</b> (no parentheses) on {@code $estool.raw(...)}. Before the
     * back-compat accessors were added to {@link ContentSearchResponse} / {@link TotalHits}, these
     * would silently yield {@code null} on the record. They must resolve now:
     * {@code $r.tookInMillis} → {@code getTookInMillis()}, {@code $r.scrollId} → {@code getScrollId()},
     * {@code $r.hits.totalHits.value} → {@code getValue()}, and {@code $r.aggregations.<name>.buckets}
     * → {@code getAggregations()} (the tree, matching {@code search()}).
     */
    private static final String RAW_LEGACY_PROPERTY_VTL = """
            #set($esQuery = '{"size":5,"aggs":{"content_types":{"terms":{"field":"contenttype","size":5}}},"query":{"bool":{"filter":[{"term":{"live":true}}]}}}')
            #set($results = $estool.raw($esQuery))
            tookInMillis: $!{results.tookInMillis}
            scrollId: [$!{results.scrollId}]
            totalHits: $!{results.hits.totalHits.value}
            #foreach($group in $results.aggregations.content_types.buckets)
              legacy key: $!{group.getKeyAsString()}
              legacy docCount: $!{group.getDocCount()}
            #end
            """;

    /**
     * The customer's <b>other</b> idiom (issue #36435): instead of navigating the Velocity object
     * directly (the path {@link #verbatimCustomerVtl_rendersAfterFix()} covers), the template first
     * round-trips the raw response through {@code $json.generate($rawResults.response)} and then walks
     * the resulting {@code JSONObject}: {@code $results.aggregations.<name>.buckets}.
     *
     * <p>{@code $rawResults.response} is the {@link ContentSearchResponse} exposed by
     * {@link ContentSearchResults#getResponse()}. {@code $json.generate(Object)} is literally
     * {@code new com.dotmarketing.util.json.JSONObject(response)} — a reflection/bean serializer that
     * only sees {@code getX()} accessors and honours {@code @JsonIgnore}. Before the fix,
     * {@code getAggregations()} carried {@code @JsonIgnore}, so the aggregations vanished from the
     * generated JSON and this loop iterated zero times (silent empty sitemap). The fix moves that
     * suppression to a class-level {@code @JsonIgnoreProperties} that only Jackson honours, so the
     * reflection serializer keeps {@code aggregations} — this loop must now emit buckets.</p>
     *
     * <p>Note: the legacy {@code .get("asMap")} hop the customer used only ever existed because the
     * pre-migration object was an ES {@code Aggregations} (which had {@code getAsMap()}); the neutral
     * tree is flatter, so the correct navigation is {@code aggregations.<name>.buckets} directly.</p>
     */
    private static final String JSON_GENERATE_VTL = """
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

            ## The #36435 idiom: JSON-ify the raw response FIRST, then navigate the JSONObject.
            #set($rawResults = $estool.search($esQuery))
            #set($results = $json.generate($rawResults.response))
            aggregations: $!{results.aggregations}
            #foreach($group in $results.aggregations.content_types.buckets)
              json key: $!{group.key}
              json docCount: $!{group.docCount}
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

    /**
     * Builds a Velocity context with the live {@code $estool}, a mock {@code $response} and the
     * {@code $json} tool bound. {@link JSONTool} needs no real init ({@code init(Object)} is a no-op
     * and {@code generate(Object)} is stateless), so a plain instance mirrors what the Velocity
     * toolbox binds as {@code $json}.
     */
    private Context velocityContext() {
        final Context ctx = new VelocityContext();
        ctx.put("estool", liveContentTool());
        ctx.put("response", mock(HttpServletResponse.class));
        ctx.put("json", new JSONTool());
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
     * End-to-end regression for <a href="https://github.com/dotCMS/core/issues/36435">#36435</a>:
     * drives the customer's {@code $json.generate($rawResults.response)}-then-navigate idiom through
     * the real dotCMS Velocity engine and asserts the aggregation buckets survive the JSON round-trip
     * (the {@code json key:} / {@code json docCount:} loop must execute). This is the path #36026/#36027
     * did NOT cover — those navigate the Velocity object directly; this one serializes first.
     */
    @Test
    public void jsonGenerateThenNavigateVtl_emitsBuckets() throws Exception {
        final String output = VelocityUtil.eval(JSON_GENERATE_VTL, velocityContext());
        Logger.info(this, "\n===== $json.generate VTL output =====\n" + output + "\n================================");

        assertTrue("FIX (#36435): $json.generate($rawResults.response) must keep the aggregations, so "
                        + "the bucket loop executes and emits 'json key:' lines",
                output.contains("json key:"));
        assertTrue("FIX (#36435): bucket doc counts must render with real numbers after the JSON round-trip",
                Pattern.compile("json docCount:\\s*\\d+").matcher(output).find());
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
     * Drives {@code $estool.raw($esQuery)} through the dotCMS Velocity engine (the {@code search()}
     * sibling is {@link #verbatimCustomerVtl_rendersAfterFix()}). Asserts both VTL surfaces of a
     * {@link ContentSearchResponse} render real data: the tree walk
     * ({@code $results.aggregationTree.content_types.buckets} with nested {@code top_hits}) and the
     * flat first-level map ({@code $results.aggregations.content_types}).
     */
    @Test
    public void rawVtl_rendersTreeAndFlatAggregations() throws Exception {
        final String output = VelocityUtil.eval(RAW_VTL, velocityContext());
        Logger.info(this, "\n===== raw VTL output =====\n" + output + "\n================================");

        assertTrue("Aggregation data should be present in the response",
                output.contains("content_types"));
        assertTrue("raw() tree walk must execute, emitting 'tree key:' lines",
                output.contains("tree key:"));
        assertTrue("raw() tree bucket doc counts must render with real numbers",
                Pattern.compile("tree docCount:\\s*\\d+").matcher(output).find());
        assertTrue("raw() nested top_hits must be reachable, emitting 'tree hit id:' lines",
                output.contains("tree hit id:"));
        assertTrue("raw() flat aggregations map must iterate, emitting 'flat key:' lines",
                output.contains("flat key:"));
        assertTrue("raw() flat bucket doc counts must render with real numbers",
                Pattern.compile("flat docCount:\\s*\\d+").matcher(output).find());
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

    /**
     * Regression guard for the fix: {@code raw()} now lowercases the whole query before executing it
     * (via {@code StringUtils.lowercaseStringExceptMatchingTokens}, the same helper {@code search()}
     * uses), so a camelCase aggregation field such as {@code contentType} resolves to the physical
     * lower-case index field {@code contenttype} and yields populated buckets — reaching parity with
     * {@code search()} for the customer's query.
     *
     * <ul>
     *   <li>{@code raw(camelCase)} now populates buckets (previously empty — the customer's bug).</li>
     *   <li>{@code raw(lowercase)} populates the same buckets — proving parity within {@code raw}.</li>
     *   <li>{@code search(camelCase)} populates buckets (unchanged) and matches the {@code raw} count.</li>
     * </ul>
     *
     * <p>The aggregation mapping was never at fault: both paths map the response through
     * {@link ContentSearchResponse#from(org.elasticsearch.action.search.SearchResponse)}. The fix
     * only adds the same query normalization {@code search()} already had to the {@code raw} path.</p>
     */
    @Test
    public void rawNormalizesQuery_reachingParityWithSearch() throws Exception {
        final ESContentTool tool = liveContentTool();

        // raw(): the whole query is now lowercased -> camelCase 'contentType' resolves to
        // 'contenttype' -> buckets populated. This is the fix: the query that used to return empty
        // getBuckets=[] now works.
        final ContentSearchResponse rawCamel = tool.raw(CAMELCASE_AGG_QUERY);
        Logger.info(this, "raw(camelCase) aggregations=" + rawCamel.aggregations());
        assertTrue("raw() must expose the content_types aggregation",
                rawCamel.aggregations().containsKey("content_types"));
        assertFalse("FIX: raw() now lowercases the query, so camelCase 'contentType' resolves to "
                        + "'contenttype' and populates buckets (was empty before the fix)",
                rawCamel.aggregations().get("content_types").isEmpty());

        // raw(): the already-lowercase field name resolves to the same result -> parity within raw().
        final ContentSearchResponse rawLower = tool.raw(AGG_QUERY);
        Logger.info(this, "raw(lowercase) aggregations=" + rawLower.aggregations());
        assertEquals("raw() must yield the same bucket count for camelCase and lowercase field names",
                rawLower.aggregations().get("content_types").size(),
                rawCamel.aggregations().get("content_types").size());

        // search(): same camelCase query, same bucket count -> raw() reaches parity with search().
        final ContentSearchResults<Contentlet> searchCamel =
                APILocator.getSearchAPI().search(CAMELCASE_AGG_QUERY, true, systemUser, true);
        final Aggregation searchAgg = searchCamel.getResponse().aggregationTree().get("content_types");
        Logger.info(this, "search(camelCase) content_types=" + searchAgg);
        assertNotNull("search() must expose the content_types aggregation", searchAgg);
        assertEquals("raw() and search() must produce the same bucket count for the same camelCase query",
                searchAgg.getBuckets().size(),
                rawCamel.aggregations().get("content_types").size());
    }

    /**
     * Gap closer: exercises the <b>hit &amp; timing</b> accessor surface of {@code $estool.search(...)}
     * through the Velocity engine — the part not covered by the aggregation-focused tests. Asserts a
     * VTL template that reads {@code $results.totalResults}, {@code $results.queryTook},
     * {@code $results.scrollId} and walks {@code $results.hits.hits} (each hit's {@code id},
     * {@code index}, {@code sourceAsMap}) renders real data. Locks the {@link ContentSearchResults}
     * getter contract that keeps {@code $dotcontent.search(...)} templates working after the ES→OS
     * migration.
     */
    @Test
    public void searchVtl_rendersHitFieldsAndTiming() throws Exception {
        final String output = VelocityUtil.eval(SEARCH_HITS_VTL, velocityContext());
        Logger.info(this, "\n===== search hits VTL output =====\n" + output + "\n================================");

        assertTrue("search() totalResults getter must render a number",
                Pattern.compile("totalResults:\\s*\\d+").matcher(output).find());
        assertTrue("search() queryTook getter must render a number",
                Pattern.compile("queryTook:\\s*\\d+").matcher(output).find());
        assertTrue("search() scrollId accessor must resolve without error",
                output.contains("scrollId:"));
        assertTrue("search() hits.totalHits.value() must render a number",
                Pattern.compile("totalHits:\\s*\\d+").matcher(output).find());
        assertTrue("search() hit walk must reach hit ids ($hit.id -> getId())",
                Pattern.compile("hit id:\\s*\\S+").matcher(output).find());
        assertTrue("search() hit sourceAsMap ($hit.sourceAsMap -> getSourceAsMap()) must render the "
                        + "_source (identifier/inode) map",
                output.contains("inode"));
    }

    /**
     * Velocity-facing regression for <a href="https://github.com/dotCMS/core/issues/36581">#36581</a>:
     * a field-sorted {@code $estool.search(...)} must expose the per-hit sort value in VTL. Asserts
     * {@code $hit.sortValues} renders a non-empty array and {@code $hit.getSortValues().get(0)} renders
     * the sort key (the {@code moddate} epoch here) — proving the neutral {@link SearchHit} both carries
     * the value and exposes it under a bean {@code get}-accessor that Velocity resolves.
     */
    @Test
    public void searchVtl_rendersPerHitSortValues() throws Exception {
        final String output = VelocityUtil.eval(SEARCH_SORT_VTL, velocityContext());
        Logger.info(this, "\n===== search sort VTL output =====\n" + output + "\n================================");

        assertTrue("a field-sorted search() must expose a non-empty per-hit sort array via "
                        + "$hit.sortValues (getSortValues())",
                Pattern.compile("sortValues:\\s*\\[[^\\]]+\\]").matcher(output).find());
        assertTrue("$hit.getSortValues().get(0) must render the moddate sort key as a number",
                Pattern.compile("firstSort:\\s*\\d+").matcher(output).find());
    }

    /**
     * Gap closer for the {@code raw()} sibling: exercises the hit &amp; timing accessor surface of
     * {@code $estool.raw(...)} — a {@link ContentSearchResponse} record — through Velocity. Because
     * the top-level accessors are record-style, the template uses explicit method syntax
     * ({@code $results.tookMillis()}, {@code $results.hits()}, {@code $results.scrollId()}); the
     * downstream {@link SearchHits}/{@link SearchHit} components are {@code get}-named so they resolve
     * as properties. Asserts timing, total-hits count and per-hit {@code id}/{@code index}/
     * {@code sourceAsMap} all render, locking the accessor contract for {@code $dotcontent.raw(...)}
     * templates that read hits/timing rather than aggregations.
     */
    @Test
    public void rawVtl_rendersHitFieldsAndTiming() throws Exception {
        final String output = VelocityUtil.eval(RAW_HITS_VTL, velocityContext());
        Logger.info(this, "\n===== raw hits VTL output =====\n" + output + "\n================================");

        assertTrue("raw() tookMillis() record accessor must render a number",
                Pattern.compile("tookMillis:\\s*\\d+").matcher(output).find());
        assertTrue("raw() scrollId() record accessor must resolve without error",
                output.contains("scrollId:"));
        assertTrue("raw() hits().getTotalHits().value() must render a number",
                Pattern.compile("totalHits:\\s*\\d+").matcher(output).find());
        assertTrue("raw() hit walk must reach hit ids ($hit.id -> getId())",
                Pattern.compile("raw hit id:\\s*\\S+").matcher(output).find());
        assertTrue("raw() hit sourceAsMap ($hit.sourceAsMap -> getSourceAsMap()) must render the "
                        + "_source (identifier/inode) map",
                output.contains("inode"));
    }

    /**
     * Back-compat proof for the Velocity accessors added to {@link ContentSearchResponse} /
     * {@link TotalHits}: legacy {@code $dotcontent.raw(...)} templates using ES-style property syntax
     * (no parentheses) must resolve on the record again — not silently yield {@code null}.
     */
    @Test
    public void rawVtl_legacyPropertyAccessorsResolve() throws Exception {
        final String output = VelocityUtil.eval(RAW_LEGACY_PROPERTY_VTL, velocityContext());
        Logger.info(this, "\n===== raw legacy-property VTL output =====\n" + output + "\n==============================");

        assertTrue("$results.tookInMillis (property) must resolve via getTookInMillis()",
                Pattern.compile("tookInMillis:\\s*\\d+").matcher(output).find());
        assertTrue("$results.scrollId (property) must resolve via getScrollId() without error",
                output.contains("scrollId:"));
        assertTrue("$results.hits.totalHits.value (property) must resolve via TotalHits.getValue()",
                Pattern.compile("totalHits:\\s*\\d+").matcher(output).find());
        assertTrue("$results.aggregations.<name>.buckets (property) must walk via getAggregations() (tree)",
                output.contains("legacy key:"));
        assertTrue("legacy bucket doc counts must render with real numbers",
                Pattern.compile("legacy docCount:\\s*\\d+").matcher(output).find());
    }
}
