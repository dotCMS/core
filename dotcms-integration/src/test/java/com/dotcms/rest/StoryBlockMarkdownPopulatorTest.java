package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
 * Integration tests for the Story Block save path: the shared ingestion seam
 * ({@link MapToContentletPopulator#populate}) converts a Markdown (#36002) or HTML (#36470) Story
 * Block value to Tiptap/ProseMirror JSON, leaves already-JSON untouched, sanitizes HTML, routes
 * ambiguous values correctly, and ignores a Markdown/HTML overwrite of rich content (preserving the
 * existing document rather than destroying it).
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

    /**
     * HTML supplied on the save path is converted to ProseMirror JSON and reads back as structured
     * content through checkin + GraphQL (#36470 — the HTML twin of the Markdown read-back test).
     */
    @Test
    public void html_fired_reads_back_as_prosemirror_json() throws Exception {
        final Contentlet base = new ContentletDataGen(contentType.id()).next();
        final Contentlet populated = new MapToContentletPopulator()
                .populate(base, propsWith("<h2>Title</h2><p>Hello <strong>world</strong>.</p>"));

        final Contentlet saved = APILocator.getContentletAPI().checkin(populated, systemUser, false);
        final Contentlet readBack = APILocator.getContentletAPI()
                .find(saved.getInode(), systemUser, false);

        final String stored = readBack.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("Field must read back as a Tiptap doc", TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("Heading structure must survive", stored.contains("\"heading\""));
        assertTrue("Bold mark must survive", stored.contains("\"bold\""));
        assertTrue("Text must survive", stored.contains("world"));

        final DataFetchingEnvironment env = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(env.getSource()).thenReturn(readBack);
        Mockito.when(env.getField()).thenReturn(new graphql.language.Field(STORY_BLOCK_VAR));

        final Map<String, Object> fetched = new StoryBlockFieldDataFetcher().get(env);
        final Object json = fetched.get("json");
        assertTrue("GraphQL must return a structured JSON object", json instanceof Map);
        assertEquals("Must read back as a ProseMirror doc", "doc", ((Map<?, ?>) json).get("type"));
    }

    /** An HTML overwrite of a rich document is ignored, preserving the existing content (AC #5). */
    @Test
    public void html_overwrite_of_rich_content_is_ignored() {
        final String richDoc = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":{\"title\":\"Embedded\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("<p>Trying to overwrite</p>"));

        assertEquals("Existing rich document must be preserved untouched", richDoc,
                result.getStringProperty(STORY_BLOCK_VAR));
    }

    /**
     * End-to-end round trip for the primitive (no rich blocks) case the editor produces: a
     * contentlet is saved with editor-authored ProseMirror JSON, then the SAME field is updated
     * with HTML through the resource's merge-then-populate mechanism (copy the existing map, then
     * populate the client value). The HTML must convert and persist as valid structured content —
     * the existing primitive doc replaced, not corrupted, and still readable through checkin + find.
     */
    @Test
    public void html_update_of_editor_saved_primitive_reads_back_valid() throws Exception {
        // 1. The editor saves primitive JSON and it is checked in.
        final String editorJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"original\"}]}]}";
        final Contentlet base = new ContentletDataGen(contentType.id()).next();
        base.setProperty(STORY_BLOCK_VAR, editorJson);
        final Contentlet saved = APILocator.getContentletAPI().checkin(base, systemUser, false);
        assertEquals("Editor JSON is stored unchanged", editorJson,
                saved.getStringProperty(STORY_BLOCK_VAR));

        // 2. Update the field as HTML, reproducing how the fire/content resources build the update:
        //    the existing stored map is copied in first, then the client value is populated over it.
        final Contentlet update = new Contentlet();
        update.getMap().putAll(saved.getMap());
        update.setInode("");
        final Contentlet populated = new MapToContentletPopulator()
                .populate(update, propsWith("<h2>Rewritten</h2><p>via <strong>HTML</strong></p>"));

        final Contentlet resaved = APILocator.getContentletAPI().checkin(populated, systemUser, false);
        final Contentlet readBack = APILocator.getContentletAPI()
                .find(resaved.getInode(), systemUser, false);

        // 3. Reads back as valid structured content: the new HTML material converted, the old gone.
        final String stored = readBack.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("Must read back as a Tiptap doc", TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("Heading from the HTML update survives", stored.contains("\"heading\""));
        assertTrue("Bold mark from the HTML update survives", stored.contains("\"bold\""));
        assertTrue("New content present", stored.contains("Rewritten"));
        assertFalse("Old primitive content replaced", stored.contains("original"));
    }

    /** Executable markup in an HTML value never reaches stored content (sanitization). */
    @Test
    public void html_with_script_is_sanitized_on_save() {
        final Contentlet result = new MapToContentletPopulator()
                .populate(newContentlet(), propsWith("<p>safe<script>alert(1)</script></p>"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue(TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("Legitimate text survives", stored.contains("safe"));
        assertFalse("Script content must be dropped", stored.contains("alert"));
    }

    /**
     * A value that merely starts with '<' but is not HTML — a CommonMark autolink — must route to
     * the Markdown converter, not the HTML one, so the URL is not lost (regression guard for the
     * detection ladder).
     */
    @Test
    public void autolink_value_routes_to_markdown_not_html() {
        final Contentlet result = new MapToContentletPopulator()
                .populate(newContentlet(), propsWith("<https://dotcms.com> is live"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue(TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("Autolink URL must be preserved via the Markdown path",
                stored.contains("https://dotcms.com"));
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

    /**
     * Markdown must NOT clobber a document containing rich blocks. Per the documented contract,
     * Markdown is for plain content only; an attempt to overwrite rich content with Markdown is
     * ignored (the existing document is preserved) and the save is not interrupted.
     */
    @Test
    public void markdown_overwrite_of_rich_content_is_ignored() {
        final String richDoc = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":{\"title\":\"Embedded\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("# Trying to overwrite"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertEquals("Existing rich document must be preserved untouched", richDoc, stored);
        assertFalse("Markdown overwrite must be ignored", stored.contains("Trying to overwrite"));
    }
}
