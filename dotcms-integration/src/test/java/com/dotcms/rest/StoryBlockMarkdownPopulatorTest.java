package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.graphql.datafetcher.StoryBlockFieldDataFetcher;
import com.dotcms.tiptap.TiptapMarkdown;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for the #36002 Story Block save path: the shared ingestion seam
 * ({@link MapToContentletPopulator#populate}) converts a Markdown Story Block value to
 * Tiptap/ProseMirror JSON, leaves already-JSON and (deferred) HTML untouched, and refuses a
 * Markdown overwrite that would destroy rich content.
 *
 * @author hassandotcms
 */
public class StoryBlockMarkdownPopulatorTest extends IntegrationTestBase {

    private static final String STORY_BLOCK_VAR = "body";

    private static User systemUser;
    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        contentType = new ContentTypeDataGen()
                .name("StoryBlockMarkdownPopulatorTest_" + System.currentTimeMillis())
                .nextPersisted();

        Field storyBlockField = ImmutableStoryBlockField.builder()
                .name("Body")
                .variable(STORY_BLOCK_VAR)
                .contentTypeId(contentType.id())
                .required(false)
                .build();
        APILocator.getContentTypeFieldAPI().save(storyBlockField, systemUser);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.id())) {
            APILocator.getContentTypeAPI(systemUser).delete(contentType);
        }
    }

    private Contentlet newContentlet() {
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        return contentlet;
    }

    private Map<String, Object> propsWith(final String storyBlockValue) {
        final Map<String, Object> props = new HashMap<>();
        props.put(Contentlet.STRUCTURE_INODE_KEY, contentType.id());
        props.put(STORY_BLOCK_VAR, storyBlockValue);
        return props;
    }

    /**
     * The defining acceptance test (#36002 AC #4): Markdown supplied on the save path is
     * converted, persisted through {@code checkin}, and READS BACK as structured ProseMirror
     * JSON — with no human editor round-trip. This exercises the full seam end to end:
     * populator conversion -> Story Block checkin validation -> store -> re-read from the DB.
     */
    @Test
    public void markdown_fired_reads_back_as_prosemirror_json() throws Exception {
        // The base contentlet carries host/folder/language so checkin runs realistically; the
        // Markdown body is applied (and converted) through the populator that is under test.
        final Contentlet base = new ContentletDataGen(contentType.id()).next();
        final Contentlet populated = new MapToContentletPopulator()
                .populate(base, propsWith("## Title\n\nHello **world**."));

        final Contentlet saved = APILocator.getContentletAPI().checkin(populated, systemUser, false);
        final Contentlet readBack = APILocator.getContentletAPI()
                .find(saved.getInode(), systemUser, false);

        final String stored = readBack.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("Field must read back as a Tiptap doc", TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("Heading structure must survive the round-trip", stored.contains("\"heading\""));
        assertTrue("Text must survive the round-trip", stored.contains("world"));

        // Confirm it surfaces through the GraphQL read path that headless clients consume
        // (DotStoryBlock.json). StoryBlockFieldDataFetcher parses the stored value as JSON, so a
        // raw-Markdown value would make this throw — the exact pre-#36002 "reads back broken" bug.
        final DataFetchingEnvironment env = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(env.getSource()).thenReturn(readBack);
        Mockito.when(env.getField()).thenReturn(new graphql.language.Field(STORY_BLOCK_VAR));

        final Map<String, Object> fetched = new StoryBlockFieldDataFetcher().get(env);
        final Object json = fetched.get("json");
        assertTrue("GraphQL must return a structured JSON object", json instanceof Map);
        assertEquals("Must read back as a ProseMirror doc", "doc", ((Map<?, ?>) json).get("type"));
    }

    /** Already-valid Tiptap JSON (the dominant editor traffic) is stored byte-identical. */
    @Test
    public void existing_prosemirror_json_passes_through_unchanged() {
        final String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}]}";

        final Contentlet result = new MapToContentletPopulator()
                .populate(newContentlet(), propsWith(json));

        assertEquals(json, result.getStringProperty(STORY_BLOCK_VAR));
    }

    /** HTML is deferred (#36002 follow-up): stored as-is, never mangled — no regression. */
    @Test
    public void legacy_html_passes_through_unchanged() {
        final String html = "<p>legacy <strong>WYSIWYG</strong> content</p>";

        final Contentlet result = new MapToContentletPopulator()
                .populate(newContentlet(), propsWith(html));

        assertEquals(html, result.getStringProperty(STORY_BLOCK_VAR));
    }

    /** Markdown may replace a primitive-only document. */
    @Test
    public void markdown_replaces_primitive_only_document() {
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"old\"}]}]}");

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("# Brand new"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue(TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("New content present", stored.contains("Brand new"));
        assertFalse("Old content replaced", stored.contains("old"));
    }

    /** Markdown must NOT clobber a document containing rich blocks; the existing doc is preserved. */
    @Test
    public void markdown_overwrite_of_rich_content_is_rejected() {
        final String richDoc = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":{\"title\":\"Embedded\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        try {
            new MapToContentletPopulator().populate(contentlet, propsWith("# Trying to overwrite"));
            fail("Expected an IllegalArgumentException rejecting the Markdown overwrite");
        } catch (final IllegalArgumentException e) {
            assertTrue("Message should explain the rejection: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("rich content"));
        }

        assertEquals("Existing rich document must be untouched", richDoc,
                contentlet.getStringProperty(STORY_BLOCK_VAR));
    }
}
