package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.graphql.datafetcher.StoryBlockFieldDataFetcher;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.tiptap.TiptapMarkdown;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for the Story Block save path: the shared ingestion seam
 * ({@link MapToContentletPopulator#populate}) converts a Markdown (#36002) or HTML (#36470) Story
 * Block value to Tiptap/ProseMirror JSON, leaves already-JSON untouched, sanitizes HTML, routes
 * ambiguous values correctly, understands the {@code dotcms-*} rich-node fence vocabulary
 * (#36658), applies Markdown/HTML overwrites of rich content with an advisory warning (or keeps
 * the existing document when the protect flag is on), and never wipes a field on an
 * empty-converting value.
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

    /**
     * #36658 semantic change, HTML leg: an HTML overwrite of a rich document APPLIES
     * (full-replace, as the endpoint documents) and an advisory warning naming the replaced
     * rich blocks is stashed for the response envelope's {@code messages}.
     */
    @Test
    public void html_overwrite_of_rich_content_applies_with_warning() {
        final String richDoc = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":{\"identifier\":\"emb-1\",\"languageId\":1,\"title\":\"Embedded\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("<p>Rewritten via HTML</p>"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("The write must apply", stored.contains("Rewritten via HTML"));
        assertFalse("The rich block is replaced", stored.contains("dotContent"));

        final List<MessageEntity> messages =
                MapToContentletPopulator.popStoryBlockConversionMessages(result);
        assertTrue("A replacement warning must be surfaced",
                UtilMethods.isSet(messages) && messages.get(0).getMessage().contains("emb-1"));
        assertFalse("Popping must remove the transient key",
                result.getMap().containsKey(Contentlet.STORY_BLOCK_CONVERSION_WARNINGS_KEY));
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
     * A Markdown value OPENING with a {@code dotcms:attrs} decoration comment must route to the
     * Markdown converter even though it starts with {@code <!--} (which matches the HTML
     * detection regex). Found live: without the carve-out the HTML converter received the value
     * and silently dropped the decoration (#36658 §2.4).
     */
    @Test
    public void leading_attrs_comment_routes_to_markdown_and_decorates() {
        final Contentlet result = new MapToContentletPopulator()
                .populate(newContentlet(), propsWith(
                        "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\nCentered paragraph."));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue(TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("The decoration must survive as the block's attrs",
                stored.contains("\"textAlign\":\"center\""));
        assertFalse("The comment itself must not be stored as text", stored.contains("<!--"));
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
     * #36658 semantic change, Markdown leg: a Markdown overwrite of a rich document APPLIES
     * (full-replace) and the replaced rich blocks are reported through the transient warnings
     * key that the REST layer surfaces as response {@code messages}.
     */
    @Test
    public void markdown_overwrite_of_rich_content_applies_with_warning() {
        final String richDoc = "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"emb-1\",\"languageId\":1,\"title\":\"Embedded\"}}},"
                + "{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/v.mp4\",\"data\":{\"identifier\":\"vid-1\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("# Full rewrite"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("The write must apply", stored.contains("Full rewrite"));
        assertFalse("Rich blocks are replaced", stored.contains("dotContent"));

        final List<MessageEntity> messages =
                MapToContentletPopulator.popStoryBlockConversionMessages(result);
        assertTrue("A replacement warning must be surfaced", UtilMethods.isSet(messages));
        final String warning = messages.get(0).getMessage();
        assertTrue("Warning names the replaced contentlet", warning.contains("emb-1"));
        assertTrue("Warning names the replaced video", warning.contains("vid-1"));
    }

    /**
     * The lossless path (#36658 AC): a Markdown update that carries the stored rich blocks as
     * {@code dotcms-*} fences preserves them — the reference (identifier + languageId) survives
     * and no replacement warning is raised.
     */
    @Test
    public void markdown_update_with_matching_fences_preserves_rich_blocks() {
        final String richDoc = "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"old intro\"}]},"
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"emb-1\",\"languageId\":1,\"title\":\"Embedded\"}}}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("# New intro\n\n"
                        + "```dotcms-content\n{\"identifier\": \"emb-1\", \"languageId\": 1}\n```"));

        final String stored = result.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("New material applied", stored.contains("New intro"));
        assertTrue("Rich block preserved via its fence", stored.contains("\"dotContent\""));
        assertTrue("Reference identity preserved", stored.contains("emb-1"));
        assertFalse("Nothing was lost, so no warning is raised",
                result.getMap().containsKey(Contentlet.STORY_BLOCK_CONVERSION_WARNINGS_KEY));
    }

    /**
     * Emergency rollback: with {@code STORY_BLOCK_MARKDOWN_RICH_OVERWRITE_PROTECT=true} the
     * pre-#36658 behavior returns — the Markdown write to a rich document is discarded, the
     * existing document kept, and the caller is told via the warnings key.
     */
    @Test
    public void protect_flag_restores_keep_existing_behavior() {
        final String richDoc = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":{\"identifier\":\"emb-1\",\"languageId\":1,\"title\":\"Embedded\"}}}]}";
        final boolean previous = Config.getBooleanProperty(
                MapToContentletPopulator.STORY_BLOCK_RICH_OVERWRITE_PROTECT_PROP, false);
        Config.setProperty(MapToContentletPopulator.STORY_BLOCK_RICH_OVERWRITE_PROTECT_PROP, true);
        try {
            final Contentlet contentlet = newContentlet();
            contentlet.setProperty(STORY_BLOCK_VAR, richDoc);

            final Contentlet result = new MapToContentletPopulator()
                    .populate(contentlet, propsWith("# Trying to overwrite"));

            assertEquals("Existing rich document must be preserved untouched", richDoc,
                    result.getStringProperty(STORY_BLOCK_VAR));
            final List<MessageEntity> messages =
                    MapToContentletPopulator.popStoryBlockConversionMessages(result);
            assertTrue("The caller is told the value was ignored", UtilMethods.isSet(messages));
        } finally {
            Config.setProperty(MapToContentletPopulator.STORY_BLOCK_RICH_OVERWRITE_PROTECT_PROP, previous);
        }
    }

    /**
     * Creation by fence (#36658 AC #1): Markdown containing a {@code dotcms-content} fence
     * referencing a real contentlet stores a valid document whose reference reads back
     * hydrated (the read path rebuilds {@code attrs.data} from identifier + languageId) with
     * no re-save.
     */
    @Test
    public void fence_created_dotcontent_reads_back_hydrated() throws Exception {
        // A real, live target contentlet to embed (hydration resolves the live version when
        // running outside a request context).
        final Contentlet target = new ContentletDataGen(contentType.id()).nextPersisted();
        ContentletDataGen.publish(target);
        final String targetId = target.getIdentifier();

        final Contentlet base = new ContentletDataGen(contentType.id()).next();
        final Contentlet populated = new MapToContentletPopulator()
                .populate(base, propsWith("Intro paragraph.\n\n"
                        + "```dotcms-content\n{\"identifier\": \"" + targetId + "\", \"languageId\": "
                        + target.getLanguageId() + "}\n```"));

        final Contentlet saved = APILocator.getContentletAPI().checkin(populated, systemUser, false);
        final Contentlet readBack = APILocator.getContentletAPI()
                .find(saved.getInode(), systemUser, false);

        final String stored = readBack.getStringProperty(STORY_BLOCK_VAR);
        assertTrue("Field must read back as a Tiptap doc", TiptapMarkdown.isTiptapDoc(stored));
        assertTrue("The embedded reference survives", stored.contains("\"dotContent\""));
        assertTrue("The identifier survives", stored.contains(targetId));

        // The stored node carries only {identifier, languageId}; the read path's hydration
        // (StoryBlockAPI, invoked from the factory/transformer reads) rebuilds the full
        // attrs.data map from that thin reference — the contract that makes thin fence
        // payloads sufficient (#36658 §3.5). Exercise it directly for determinism; the API
        // tracks hydration depth on the thread-local request, so bind a mock one.
        final HttpServletRequest oldRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        try {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(
                    new MockAttributeRequest(Mockito.mock(HttpServletRequest.class)));
            HttpServletResponseThreadLocal.INSTANCE.setResponse(Mockito.mock(HttpServletResponse.class));

            final com.dotcms.contenttype.business.StoryBlockReferenceResult hydration =
                    APILocator.getStoryBlockAPI()
                            .refreshStoryBlockValueReferences(stored, saved.getIdentifier());
            assertTrue("The thin reference must hydrate", hydration.isRefreshed());
            assertTrue("Hydration rebuilds the full data map from identifier + languageId",
                    String.valueOf(hydration.getValue()).contains(target.getInode()));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldResponse);
        }
    }

    /**
     * Data-safety guard (#36470 review): HTML whose content is entirely stripped by sanitization
     * converts to an empty document. That must NOT silently clear a field that already holds content —
     * the existing value is preserved rather than wiped. A genuine field-clear arrives as a blank value
     * and is handled before conversion, so preserving here never blocks an intended clear.
     */
    @Test
    public void html_that_converts_to_empty_preserves_existing_content() {
        final String existing = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"keep me\"}]}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, existing);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("<iframe src=\"https://evil.example\"></iframe>"));

        assertEquals("Existing content must be preserved when HTML strips to an empty document",
                existing, result.getStringProperty(STORY_BLOCK_VAR));
    }

    /**
     * Same data-safety guard on the Markdown leg (shared code path): a value that parses to no
     * renderable content — a bare link reference definition — converts to an empty document and must
     * likewise preserve the existing field rather than clear it.
     */
    @Test
    public void markdown_that_converts_to_empty_preserves_existing_content() {
        final String existing = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"keep me\"}]}]}";
        final Contentlet contentlet = newContentlet();
        contentlet.setProperty(STORY_BLOCK_VAR, existing);

        final Contentlet result = new MapToContentletPopulator()
                .populate(contentlet, propsWith("[ref]: https://dotcms.com \"title\""));

        assertEquals("Existing content must be preserved when Markdown parses to an empty document",
                existing, result.getStringProperty(STORY_BLOCK_VAR));
    }
}
