package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
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
import com.dotmarketing.util.Config;
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
     * Customer regression dotCMS #37870 (follow-up to #36026), driven through {@code $estool.search()}.
     *
     * <p>The customer has a <b>custom</b> field whose velocity var name is {@code contentTypeId}
     * (an integer they use for their own categorization — not dotCMS's internal content-type id).
     * They read it per record as {@code $!{record.contentTypeId}}. After the ES→OS migration that
     * lookup started returning a 32-char hash (the content type's internal inode) instead of their
     * field value, and the working workaround became {@code $!{record.map.contentTypeId}}.</p>
     *
     * <p>That collision only happens when the iterated record is a <b>raw {@link Contentlet}</b>
     * (e.g. via the deprecated {@code $estool.esSearch()}): Velocity resolves {@code .contentTypeId}
     * to the built-in {@code Contentlet#getContentTypeId()} getter, which returns
     * {@code map.get("stInode")} — the type inode — shadowing the custom field of the same name.</p>
     *
     * <p>{@code $estool.search()} instead returns {@link com.dotcms.rendering.velocity.viewtools.content.ContentMap}
     * records. {@code ContentMap} has no {@code getContentTypeId()} getter, so Velocity resolves
     * {@code .contentTypeId} through {@code ContentMap.get("contentTypeId")}, which finds the custom
     * field and returns its value. This test locks that guarantee: through {@code search()} the
     * customer's verbatim {@code $!{record.contentTypeId}} lookup returns the field value
     * ({@code "10"}), never the content type inode — no {@code .map} workaround required.</p>
     *
     * <h3>Migration phase</h3>
     * <p>The test pins {@code FEATURE_FLAG_OPEN_SEARCH_PHASE=0} (ES write + ES read) explicitly, the
     * only phase this ES-suite test can exercise (the ES 7.10.2 client cannot index against the
     * OpenSearch container, which is why this class lives in {@code MainSuite1b} and not the OS
     * upgrade suite). <b>The phase does not affect this assertion's outcome:</b> {@code search()} is
     * DB-backed in every phase — both {@code ESSearchAPIImpl.search()} and
     * {@code OSSearchAPIImpl.search()} discover hit inodes from the index and then load the full
     * {@link Contentlet}s via {@code APILocator.getContentletAPIImpl().findContentlets(inodes)}. The
     * {@code contentTypeId} value therefore comes from the database, not from the ES/OS {@code _source},
     * so {@code ContentMap}'s field resolution is identical regardless of which store served the hit.
     * The OS read-path "does the query find the doc" guarantee is covered separately in
     * {@code OSSearchAPIImplIntegrationTest} (OpenSearch upgrade suite).</p>
     */
    @Test
    public void searchContentMap_resolvesCustomContentTypeIdField_notTheTypeInode() throws Exception {
        // A content type whose custom field collides by name with Contentlet#getContentTypeId().
        final Field customContentTypeId = new FieldDataGen()
                .name("contentTypeId").velocityVarName("contentTypeId")
                .type(TextField.class).indexed(true).next();
        final ContentType contentType = new ContentTypeDataGen()
                .name("ContentTypeIdCollision QA " + System.currentTimeMillis())
                .host(defaultHost)
                .field(customContentTypeId)
                .nextPersisted();

        // Pin the migration phase deterministically (PHASE_0 = ES write + ES read). See the javadoc:
        // search() is DB-backed in all phases, so the contentTypeId value is phase-independent; this
        // only removes the implicit dependency on the harness default and matches the ES suite env.
        final String previousPhase = Config.getStringProperty(FLAG_KEY, null);
        Config.setProperty(FLAG_KEY, "0");
        try {
            final Contentlet contentlet = new ContentletDataGen(contentType)
                    .host(defaultHost)
                    .languageId(defaultLanguage.getId())
                    .setProperty("contentTypeId", "10")
                    .nextPersisted();
            ContentletDataGen.publish(contentlet);
            APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(), true);

            // The customer reads $!{record.contentTypeId} verbatim. Query is lowercased by search(),
            // so the camelCase contentType var folds to the physical `contenttype` index field.
            final String customerVtl = """
                    #set($esQuery = '{
                        "query": { "query_string": { "query": "+contentType:%s +live:true" } },
                        "size": 5
                    }')
                    #set($results = $estool.search($esQuery))
                    totalResults: $!{results.totalResults}
                    #foreach($record in $results)
                      id: $!{record.identifier} ctid: $!{record.contentTypeId}
                    #end
                    """.formatted(contentType.variable());

            final String output = VelocityUtil.eval(customerVtl, velocityContext());
            Logger.info(this, "\n===== #37870 search() VTL output =====\n" + output + "\n================================");

            assertTrue("The search() result loop must run (the customer's content must be found)",
                    Pattern.compile("ctid:\\s*\\S").matcher(output).find());
            assertTrue("FIX/RECOMMENDATION (#37870): through search()/ContentMap, $record.contentTypeId "
                            + "must resolve to the custom field value '10'",
                    Pattern.compile("ctid:\\s*10\\b").matcher(output).find());
            assertFalse("$record.contentTypeId must NOT return a 32-char content-type inode hash "
                            + "(the raw-Contentlet getter-shadowing bug)",
                    Pattern.compile("ctid:\\s*[0-9a-fA-F]{32}\\b").matcher(output).find());
            assertFalse("$record.contentTypeId must NOT return the actual content type inode",
                    output.contains("ctid: " + contentType.id()));
        } finally {
            Config.setProperty(FLAG_KEY, previousPhase);
        }
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
}
