package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for {@link StoryBlockFieldDataFetcher}.
 * <p>
 * Covers the two read paths a typed Block Editor GraphQL field can take:
 * <ul>
 *     <li><b>Page queries</b>: {@code PageRenderUtil} hydrates contentlets with
 *     {@code DotTransformerBuilder().defaultOptions()}, so {@code StoryBlockViewStrategy} has
 *     already turned the field's JSON String into a Map before the fetcher runs.</li>
 *     <li><b>Collection queries</b>: the contentlet is raw and the field still holds its stored
 *     JSON String.</li>
 * </ul>
 * Both paths must return the same JSON structure for content whose text contains commas, colons
 * and hyperlinks (see issue #37087).
 */
public class StoryBlockFieldDataFetcherIntegrationTest {

    private static final String STORY_BLOCK_JSON =
            "{"
            + "\"type\":\"doc\","
            + "\"content\":["
            + "  {\"type\":\"paragraph\",\"content\":["
            + "    {\"type\":\"text\",\"text\":\"Run your entire digital ecosystem - securely, visually, and without the chaos. \"},"
            + "    {\"type\":\"text\",\"marks\":[{\"type\":\"link\",\"attrs\":{\"href\":\"https://dotcms.com/product\",\"target\":\"_blank\"}}],\"text\":\"Learn more, here\"}"
            + "  ]}"
            + "]"
            + "}";

    private static ContentType storyBlockType;
    private static Field storyBlockField;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        final long timestamp = System.currentTimeMillis();
        storyBlockType = new ContentTypeDataGen()
                .name("storyBlockFetcher" + timestamp)
                .velocityVarName("storyBlockFetcher" + timestamp)
                .nextPersisted();
        storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .contentTypeId(storyBlockType.id())
                .nextPersisted();
    }

    @AfterClass
    public static void cleanup() {
        if (storyBlockType != null) {
            ContentTypeDataGen.remove(storyBlockType);
        }
    }

    private static Contentlet persistContentlet(final String storyBlockValue) {
        return new ContentletDataGen(storyBlockType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty(storyBlockField.variable(), storyBlockValue)
                .nextPersisted();
    }

    private static DataFetchingEnvironment environmentFor(final Contentlet contentlet) {
        final DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(environment.getSource()).thenReturn(contentlet);
        Mockito.when(environment.getField())
                .thenReturn(graphql.language.Field.newField(storyBlockField.variable()).build());
        return environment;
    }

    @SuppressWarnings("unchecked")
    private static void assertStoryBlockJson(final Map<String, Object> result) {
        assertNotNull(result);
        final Map<String, Object> json = (Map<String, Object>) result.get("json");
        assertNotNull("The 'json' entry must be present", json);
        assertEquals("doc", json.get("type"));

        final Map<String, Object> paragraph =
                (Map<String, Object>) ((List<Object>) json.get("content")).get(0);
        final List<Map<String, Object>> textNodes = (List<Map<String, Object>>) paragraph.get("content");
        assertEquals("Run your entire digital ecosystem - securely, visually, and without the chaos. ",
                textNodes.get(0).get("text"));
        assertEquals("Learn more, here", textNodes.get(1).get("text"));

        final Map<String, Object> linkMark =
                ((List<Map<String, Object>>) textNodes.get(1).get("marks")).get(0);
        assertEquals("link", linkMark.get("type"));
        assertEquals("https://dotcms.com/product",
                ((Map<String, Object>) linkMark.get("attrs")).get("href"));
    }

    /**
     * MethodToTest {@link StoryBlockFieldDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: a contentlet hydrated through {@code DotTransformerBuilder().defaultOptions()}
     * — exactly what {@code PageRenderUtil} does for page GraphQL queries — whose Block Editor
     * content includes commas and a hyperlink. The field value reaching the fetcher is a Map.
     * ExpectedResult: the field resolves to its correct JSON structure instead of failing with a
     * JSONException from parsing {@code Map.toString()}.
     */
    @Test
    public void pagePath_hydratedContentletWithCommasAndLink_returnsJson() throws Exception {
        final Contentlet persisted = persistContentlet(STORY_BLOCK_JSON);

        final DotContentletTransformer transformer = new DotTransformerBuilder()
                .defaultOptions().content(persisted).build();
        final Contentlet hydrated = transformer.hydrate().get(0);
        assertTrue("Precondition: defaultOptions must hydrate the field into a Map",
                hydrated.get(storyBlockField.variable()) instanceof Map);

        final Map<String, Object> result =
                new StoryBlockFieldDataFetcher().get(environmentFor(hydrated));

        assertStoryBlockJson(result);
    }

    /**
     * MethodToTest {@link StoryBlockFieldDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: a raw contentlet — the shape GraphQL collection queries resolve — whose
     * Block Editor field still holds its stored JSON String.
     * ExpectedResult: the field resolves to the same JSON structure as the page path.
     */
    @Test
    public void collectionPath_rawContentlet_returnsSameJsonAsPagePath() throws Exception {
        final Contentlet persisted = persistContentlet(STORY_BLOCK_JSON);
        assertTrue("Precondition: the raw contentlet must hold the field as a String",
                persisted.get(storyBlockField.variable()) instanceof String);

        final Map<String, Object> rawResult =
                new StoryBlockFieldDataFetcher().get(environmentFor(persisted));
        assertStoryBlockJson(rawResult);

        final Contentlet hydrated = new DotTransformerBuilder()
                .defaultOptions().content(persisted).build().hydrate().get(0);
        final Map<String, Object> hydratedResult =
                new StoryBlockFieldDataFetcher().get(environmentFor(hydrated));

        assertEquals("Both read paths must resolve to the same JSON",
                hydratedResult.get("json"), rawResult.get("json"));
    }

    /**
     * MethodToTest {@link StoryBlockFieldDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: the field holds a value that cannot be resolved into JSON.
     * ExpectedResult: the field resolves to null and no exception is thrown, so a single bad
     * field cannot fail the whole GraphQL query (see #36297).
     */
    @Test
    public void malformedValue_resolvesToNullWithoutFailing() throws Exception {
        final Contentlet contentlet = persistContentlet(STORY_BLOCK_JSON);
        contentlet.getMap().put(storyBlockField.variable(),
                "{type=doc, content=[{type=paragraph, text=securely, visually}]}");

        assertNull(new StoryBlockFieldDataFetcher().get(environmentFor(contentlet)));
    }
}
