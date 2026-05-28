package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.SaveContentActionletTest;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.UtilMethods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test a save and publish with a content type with non-required tags
 */
public class SaveContentActionletWithTagsTest extends BaseWorkflowIntegrationTest {

    private static WorkflowAPI workflowAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static final int LIMIT = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);

        // associated the scheme to the type
        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                List.of(systemWorkflowScheme));

        setDebugMode(false);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotSaveActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotSaveActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TagField.class).name("tag").variable("tag").required(false)
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException {

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        cleanupDebug(SaveContentActionletTest.class);
    } // cleanup


    /**
     * Method to test: {@link Contentlet#setTags()}
     * Given Scenario: Contentlet with persona tag is getting appended :persona
     * Expected Result: tag should not have :persona, should be the same as the tag name.
     */
    @Test
    public void test_TagsShouldNotIncludePersona() throws DotDataException, DotSecurityException {
        //Create persona Tag
        final String tagName = "personaTag" + System.currentTimeMillis();
        final Tag tag = new TagDataGen().name(tagName).site(APILocator.getHostAPI().findSystemHost()).persona(true).nextPersisted();

        //Add persona Tag to a contentlet
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", tag.getTagName());
        contentlet.setProperty("txt", tag.getTagName());
        contentlet.setProperty("tag", tag.getTagName());

        final Contentlet contentletSaved =
                workflowAPI.fireContentWorkflow(contentlet,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved);
        Assert.assertEquals(tag.getTagName(), contentletSaved.getStringProperty("title"));
        Assert.assertEquals(tag.getTagName(), contentletSaved.getStringProperty("txt"));
        contentletSaved.setTags();
        //Check that tag do not include :persona
        Assert.assertEquals(tag.getTagName(), contentletSaved.getStringProperty("tag"));

    }

    @Test
    public void test_Save_Contentlet_Actionlet_Tags () throws DotSecurityException, DotDataException {

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test");
        contentlet.setProperty("tag", "test");

        final Contentlet contentletSaved =
                workflowAPI.fireContentWorkflow(contentlet,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved);
        Assert.assertEquals("Test", contentletSaved.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved.getStringProperty("txt"));
        contentletSaved.setTags();
        Assert.assertEquals("test", contentletSaved.getStringProperty("tag"));

        //// save 2 override - adding a new tag
        final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByInode(contentletSaved.getInode());
        Assert.assertNotNull(tagInodes);
        Assert.assertFalse(tagInodes.isEmpty());

        contentletSaved.setProperty("tag", "test,testing");

        final Contentlet contentletSaved2 =
                        workflowAPI.fireContentWorkflow(contentletSaved,
                                new ContentletDependencies.Builder()
                                        .modUser(APILocator.systemUser())
                                        .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                        .build());

        Assert.assertNotNull(contentletSaved2);
        Assert.assertEquals("Test", contentletSaved2.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved2.getStringProperty("txt"));
        contentletSaved2.setTags();
        Assert.assertTrue( contentletSaved2.getStringProperty("tag").contains("testing"));
        Assert.assertTrue( contentletSaved2.getStringProperty("tag").contains("test"));

        //// save 3 override to just one
        contentletSaved2.setProperty("tag", "testing");

        final Contentlet contentletSaved3 =
                workflowAPI.fireContentWorkflow(contentletSaved2,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved3);
        Assert.assertEquals("Test", contentletSaved3.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved3.getStringProperty("txt"));
        contentletSaved3.setTags();
        Assert.assertEquals("testing", contentletSaved3.getStringProperty("tag"));

        ////save 4 cleaning all tags
        contentletSaved3.setProperty("tag", "");

        final Contentlet contentletSaved4 =
                workflowAPI.fireContentWorkflow(contentletSaved3,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved4);
        Assert.assertEquals("Test", contentletSaved4.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved4.getStringProperty("txt"));
        contentletSaved4.setTags();
        Assert.assertNull(contentletSaved4.getStringProperty("tag"));
    }

    // ---------------------------------------------------------------------------------------------
    // Tag field clearing — issue #35861: tag values must not survive once all tags are removed,
    // and a secondary-language version must not inherit the source language's tags.
    //
    // The cross-language leak originates in ESContentletAPIImpl#copyProperties(), which blindly
    // copies the tag field string into a new language version whose tag_inode has no rows;
    // Contentlet#setTags() then leaves that copied string in place because it only ever adds tag
    // values, never clears them. The correct fix is to stop copyProperties() from carrying tag
    // fields over, NOT to make setTags() clear on an empty tag_inode — setTags() must keep
    // preserving an un-persisted value the user just typed (see
    // test_setTagsPreservesUnpersistedUserInput, which would break under such a change).
    // ---------------------------------------------------------------------------------------------

    /**
     * Fires the system SAVE workflow action with WAIT_FOR indexing so assertions can read back
     * the persisted state deterministically.
     */
    private static Contentlet fireSave(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        return workflowAPI.fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .modUser(APILocator.systemUser())
                        .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                        .build());
    }

    /**
     * Builds (but does not persist) a contentlet of {@link #customContentType}. A null
     * {@code tagValue} leaves the tag field unset.
     */
    private static Contentlet newContentlet(final long languageId, final String tagValue) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(customContentType);
        contentlet.setLanguageId(languageId);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test");
        if (tagValue != null) {
            contentlet.setProperty("tag", tagValue);
        }
        return contentlet;
    }

    private static long defaultLanguageId() {
        return APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

    /**
     * Method to test: {@link Contentlet#setTags()}
     * Given Scenario: A contentlet is saved with two tags and then freshly loaded from the API.
     * Expected Result: The tag field is populated with both tag names and tag_inode has 2 rows.
     */
    @Test
    public void test_loadTagsPopulatesTagFieldFromTagInode() throws Exception {

        final Contentlet saved = fireSave(newContentlet(defaultLanguageId(), "alpha,beta"));
        Assert.assertNotNull(saved);

        final Contentlet loaded =
                APILocator.getContentletAPI().find(saved.getInode(), APILocator.systemUser(), false);
        loaded.setTags();

        final String tags = loaded.getStringProperty("tag");
        Assert.assertNotNull(tags);
        Assert.assertTrue(tags.contains("alpha"));
        Assert.assertTrue(tags.contains("beta"));
        Assert.assertEquals(2,
                APILocator.getTagAPI().getTagInodesByInode(loaded.getInode()).size());
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#copyProperties}
     * Given Scenario: copyProperties() copies the map of a tagged source contentlet into a brand-new
     * contentlet. This is the path that seeds a new language version, and where the cross-language
     * tag leak originates.
     * Expected Result: The tag field value is NOT carried over into the target contentlet.
     */
    @Test
    public void test_copyPropertiesDoesNotPropagateTagFieldValue() throws Exception {

        final Contentlet source = fireSave(newContentlet(defaultLanguageId(), "alpha"));
        source.setTags();
        Assert.assertEquals("alpha", source.getStringProperty("tag"));

        final Contentlet target = new Contentlet();
        target.setContentType(customContentType);
        APILocator.getContentletAPI().copyProperties(target, source.getMap());

        Assert.assertTrue("copyProperties must not propagate the tag field string into a new contentlet",
                UtilMethods.isEmpty(target.getStringProperty("tag")));
    }

    /**
     * Method to test: {@link Contentlet#setTags()} via the multi-language create path.
     * Given Scenario: An English contentlet has a tag. A second-language version of the SAME
     * identifier is created via copyProperties() without the user setting any tag, then saved and
     * freshly loaded.
     * Expected Result: The second-language version does NOT inherit the English tag — no tag_inode
     * rows and no displayed tag value.
     */
    @Test
    public void test_secondaryLanguageVersionDoesNotInheritTags() throws Exception {

        final long defaultLang = defaultLanguageId();
        final Language secondLang = new LanguageDataGen().nextPersisted();

        final Contentlet english = fireSave(newContentlet(defaultLang, "english-only-tag"));
        english.setTags();
        Assert.assertEquals("english-only-tag", english.getStringProperty("tag"));

        // Build a second-language version of the same identifier via copyProperties, without a tag.
        final Contentlet secondLangVersion = new Contentlet();
        secondLangVersion.setContentType(customContentType);
        APILocator.getContentletAPI().copyProperties(secondLangVersion, english.getMap());
        secondLangVersion.setHost(english.getHost());
        secondLangVersion.setIdentifier(english.getIdentifier());
        secondLangVersion.setInode("");
        secondLangVersion.setLanguageId(secondLang.getId());

        final Contentlet saved = fireSave(secondLangVersion);

        final Contentlet loaded = APILocator.getContentletAPI()
                .find(saved.getInode(), APILocator.systemUser(), false);
        loaded.setTags();

        Assert.assertTrue("Second-language version must not inherit the source-language tags in tag_inode",
                APILocator.getTagAPI().getTagInodesByInode(loaded.getInode()).isEmpty());
        Assert.assertNull("Second-language version must not display an inherited source-language tag",
                loaded.getStringProperty("tag"));
    }

    /**
     * Method to test: {@link Contentlet#setTags()} — language-version independence.
     * Given Scenario: An English contentlet has tag "shared"; a second-language version of the same
     * identifier has its own tag "shared-es"; the second-language tags are then cleared.
     * Expected Result: The second-language version ends with no tags, while the English version keeps
     * "shared".
     */
    @Test
    public void test_clearingTagsOnSecondaryLanguageDoesNotAffectSourceLanguage() throws Exception {

        final long defaultLang = defaultLanguageId();
        final Language secondLang = new LanguageDataGen().nextPersisted();

        final Contentlet english = fireSave(newContentlet(defaultLang, "shared"));
        final String identifier = english.getIdentifier();

        // Second-language version of the same identifier with its OWN tag.
        final Contentlet secondLangVersion = new Contentlet();
        secondLangVersion.setContentType(customContentType);
        secondLangVersion.setHost(english.getHost());
        secondLangVersion.setIdentifier(identifier);
        secondLangVersion.setLanguageId(secondLang.getId());
        secondLangVersion.setProperty("title", "Test");
        secondLangVersion.setProperty("txt", "Test");
        secondLangVersion.setProperty("tag", "shared-es");
        Contentlet secondLangSaved = fireSave(secondLangVersion);

        // Clear the second-language tags.
        secondLangSaved.setProperty("tag", "");
        secondLangSaved = fireSave(secondLangSaved);

        final Contentlet secondLangReloaded = APILocator.getContentletAPI()
                .findContentletByIdentifier(identifier, false, secondLang.getId(),
                        APILocator.systemUser(), false);
        secondLangReloaded.setTags();

        final Contentlet englishReloaded = APILocator.getContentletAPI()
                .findContentletByIdentifier(identifier, false, defaultLang,
                        APILocator.systemUser(), false);
        englishReloaded.setTags();

        Assert.assertNull("Second-language tags should be cleared",
                secondLangReloaded.getStringProperty("tag"));
        Assert.assertTrue("Second-language tag_inode should be empty",
                APILocator.getTagAPI().getTagInodesByInode(secondLangReloaded.getInode()).isEmpty());

        Assert.assertEquals("English tag must be unaffected by clearing the second-language version",
                "shared", englishReloaded.getStringProperty("tag"));
        Assert.assertFalse("English tag_inode must remain intact",
                APILocator.getTagAPI().getTagInodesByInode(englishReloaded.getInode()).isEmpty());
    }

    /**
     * Method to test: {@link Contentlet#setTags()} after a workflow SAVE with an empty tag value.
     * Given Scenario: A contentlet with a tag is saved, then the SAVE action is fired again with
     * tags="" and the contentlet is freshly loaded from the API.
     * Expected Result: The freshly loaded contentlet reports no tag and tag_inode has no rows.
     */
    @Test
    public void test_clearingAllTagsViaSaveActionClearsThemOnFreshLoad() throws Exception {

        final Contentlet saved = fireSave(newContentlet(defaultLanguageId(), "new edit content"));
        Assert.assertFalse(
                APILocator.getTagAPI().getTagInodesByInode(saved.getInode()).isEmpty());

        saved.setProperty("tag", "");
        final Contentlet cleared = fireSave(saved);

        final Contentlet loaded = APILocator.getContentletAPI()
                .find(cleared.getInode(), APILocator.systemUser(), false);
        loaded.setTags();

        Assert.assertNull(loaded.getStringProperty("tag"));
        Assert.assertTrue(
                APILocator.getTagAPI().getTagInodesByInode(loaded.getInode()).isEmpty());
    }

    /**
     * Method to test: {@link Contentlet#setTags()} — no stale cached tag value after clearing.
     * Given Scenario: A tagged contentlet is read (warming caches), then the tags are cleared via a
     * SAVE action, then the contentlet is read again.
     * Expected Result: The post-mutation read returns no tag — no stale cached value is served.
     */
    @Test
    public void test_clearingTagsInvalidatesCachedTagValue() throws Exception {

        final long lang = defaultLanguageId();
        final Contentlet saved = fireSave(newContentlet(lang, "cached"));
        final String identifier = saved.getIdentifier();

        // Warm the cache.
        final Contentlet warm = APILocator.getContentletAPI()
                .findContentletByIdentifier(identifier, false, lang, APILocator.systemUser(), false);
        warm.setTags();
        Assert.assertEquals("cached", warm.getStringProperty("tag"));

        // Mutate: clear all tags.
        saved.setProperty("tag", "");
        fireSave(saved);

        // Re-read fresh.
        final Contentlet after = APILocator.getContentletAPI()
                .findContentletByIdentifier(identifier, false, lang, APILocator.systemUser(), false);
        after.setTags();

        Assert.assertNull(after.getStringProperty("tag"));
        Assert.assertTrue(
                APILocator.getTagAPI().getTagInodesByInode(after.getInode()).isEmpty());
    }

    /**
     * Method to test: {@link Contentlet#setTags()} — un-persisted user input is preserved.
     * Given Scenario: A contentlet with no persisted tags (empty tag_inode) is being edited in-flight:
     * a freshly built contentlet has a user-typed tag value in its map that has not yet been saved,
     * and setTags() is invoked (as happens on the edit-load path).
     * Expected Result: The user-typed value is preserved — setTags() must NOT wipe it. This is why
     * the cross-language leak must be fixed in copyProperties() rather than by having setTags() clear
     * the tag whenever tag_inode is empty.
     */
    @Test
    public void test_setTagsPreservesUnpersistedUserInput() throws Exception {

        // A contentlet with no tags persisted (tag_inode empty for its inode).
        final Contentlet saved = fireSave(newContentlet(defaultLanguageId(), null));
        Assert.assertTrue(
                APILocator.getTagAPI().getTagInodesByInode(saved.getInode()).isEmpty());

        // Simulate an in-flight edit: fresh object, user-typed value, tag_inode still empty.
        final Contentlet inFlight = new Contentlet();
        inFlight.setContentType(customContentType);
        inFlight.setInode(saved.getInode());
        inFlight.setStringProperty("tag", "user-typed-not-yet-saved");
        inFlight.setTags();

        Assert.assertEquals("setTags() must not wipe a value the user typed but has not yet saved",
                "user-typed-not-yet-saved", inFlight.getStringProperty("tag"));
    }

    // Note: dotCMS forbids more than one TagField per content type
    // (FieldFactoryImpl#validateDbColumn rejects it), so a "clear one tag field, keep the other"
    // scenario cannot be built. Per-field reconciliation against the single allowed tag field is
    // already exercised by test_clearingAllTagsViaSaveActionClearsThemOnFreshLoad and the existing
    // test_Save_Contentlet_Actionlet_Tags.

    /**
     * Method to test: a non-tag field (Text) can be fully cleared and the cleared value persisted.
     * Given Scenario: A contentlet with txt="Test" is saved, then saved again with txt="".
     * Expected Result: The freshly loaded contentlet returns an empty/absent txt value.
     */
    @Test
    public void test_textFieldCanBeFullyCleared() throws Exception {

        final Contentlet saved = fireSave(newContentlet(defaultLanguageId(), null));
        Assert.assertEquals("Test", saved.getStringProperty("txt"));

        saved.setProperty("txt", "");
        final Contentlet cleared = fireSave(saved);

        final Contentlet loaded = APILocator.getContentletAPI()
                .find(cleared.getInode(), APILocator.systemUser(), false);

        Assert.assertTrue("txt should be cleared",
                UtilMethods.isEmpty(loaded.getStringProperty("txt")));
    }

    /**
     * Method to test: {@link Contentlet#resetLoadedTags()} on the load path.
     * Given Scenario: A contentlet's tags are loaded (which sets the loadedTags guard), then the
     * underlying tag_inode rows change. A second setTags() is a no-op because of the guard.
     * Expected Result: After resetLoadedTags(), setTags() re-reads tag_inode and reflects the new
     * tag. Without the reset the change never surfaces on this instance — the gap the bulk workflow
     * path hits when setTags() runs before checkin reconciles the tags.
     */
    @Test
    public void test_resetLoadedTagsEnablesRereadOfTagInode() throws Exception {

        final Contentlet saved = fireSave(newContentlet(defaultLanguageId(), null));
        final String inode = saved.getInode();

        final Contentlet loaded =
                APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
        loaded.setTags();                       // loadedTags guard is now set; no tag yet
        Assert.assertNull(loaded.getStringProperty("tag"));

        // Change tag_inode out-of-band, then prove the guard blocks the re-read.
        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        final Tag tag = new TagDataGen().name("added-out-of-band" + System.currentTimeMillis())
                .site(systemHost).nextPersisted();
        APILocator.getTagAPI().addContentletTagInode(tag, inode, "tag");

        loaded.setTags();                       // skipped: loadedTags still true
        Assert.assertNull("guard must block re-read until reset", loaded.getStringProperty("tag"));

        loaded.resetLoadedTags();
        loaded.setTags();                       // now re-reads the reconciled tag_inode
        Assert.assertEquals(tag.getTagName(), loaded.getStringProperty("tag"));
    }
}
