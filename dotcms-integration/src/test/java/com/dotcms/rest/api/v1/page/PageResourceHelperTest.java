package com.dotcms.rest.api.v1.page;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.*;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PageResourceHelperTest {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Template that is not advanced and the Multi_tree has a
     * relation_type legacy value
     * should: copy the Contentlet anyway
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void copyContentlet() throws DotDataException, DotSecurityException {

        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final Field field_2 = new FieldDataGen()
                .name("field2")
                .velocityVarName("field2")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .field(field_2)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .setProperty("field2", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen().setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType("1")
                .contentId(contentlet.getIdentifier())
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Contentlet contentletCopy = PageResourceHelper.getInstance().copyContentlet(copyContentletForm,
                APILocator.systemUser(), PageMode.PREVIEW_MODE, language);

        assertEquals(contentlet.getStringProperty("field1"), contentletCopy.getStringProperty("field1"));
        assertEquals(contentlet.getStringProperty("field2"), contentletCopy.getStringProperty("field2"));

        assertNotEquals(contentlet.getIdentifier(), contentletCopy.getIdentifier());
        assertNotEquals(contentlet.getInode(), contentletCopy.getInode());
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Template that is not advanced and the Multi_tree has a
     * relation_type legacy value and the variantId is empty
     * should: copy the Contentlet anyway
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void copyContentletWithEmptyVariantId() throws DotDataException, DotSecurityException {

        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final Field field_2 = new FieldDataGen()
                .name("field2")
                .velocityVarName("field2")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .field(field_2)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .setProperty("field2", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen().setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType("1")
                .contentId(contentlet.getIdentifier())
                .variantId("")
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Contentlet contentletCopy = PageResourceHelper.getInstance().copyContentlet(copyContentletForm,
                APILocator.systemUser(), PageMode.PREVIEW_MODE, language);

        assertEquals(contentlet.getStringProperty("field1"), contentletCopy.getStringProperty("field1"));
        assertEquals(contentlet.getStringProperty("field2"), contentletCopy.getStringProperty("field2"));

        assertNotEquals(contentlet.getIdentifier(), contentletCopy.getIdentifier());
        assertNotEquals(contentlet.getInode(), contentletCopy.getInode());
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Multi_tree does not exist
     * should: throw DoesNotExistException
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = DoesNotExistException.class)
    public void copyContentletThrowsDoesNotExistException() throws DotDataException, DotSecurityException {

        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType("1")
                .contentId(contentlet.getIdentifier())
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        PageResourceHelper.getInstance().copyContentlet(copyContentletForm,
                APILocator.systemUser(), PageMode.PREVIEW_MODE, language);
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Template is advanced and the Multi_tree has a
     * relation_type legacy value
     * should: copy the Contentlet anyway
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void copyContentletAdvancedTemplate() throws DotDataException, DotSecurityException {
        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final Field field_2 = new FieldDataGen()
                .name("field2")
                .velocityVarName("field2")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .field(field_2)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .setProperty("field2", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .drawedBody(String.format("#parseContainer('%s')", container.getIdentifier()))
                .drawed(false)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen().setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType(ParseContainer.PARSE_CONTAINER_UUID_PREFIX + "1")
                .contentId(contentlet.getIdentifier())
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Contentlet contentletCopy =  PageResourceHelper.getInstance().copyContentlet(copyContentletForm, APILocator.systemUser(),
                PageMode.PREVIEW_MODE, language);

        assertEquals(contentlet.getStringProperty("field1"), contentletCopy.getStringProperty("field1"));
        assertEquals(contentlet.getStringProperty("field2"), contentletCopy.getStringProperty("field2"));

        assertNotEquals(contentlet.getIdentifier(), contentletCopy.getIdentifier());
        assertNotEquals(contentlet.getInode(), contentletCopy.getInode());
    }

    /**
     * Method to test: {@link PageResourceHelper#getStyleEditorSchemasInPage(String)}
     * Given Scenario: The page has no contentlets (no MultiTree entries exist for it)
     * Should: Return an empty list
     */
    @Test
    public void getStyleEditorSchemasInPage_whenPageHasNoContentlets_returnsEmpty()
            throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final List<JsonNode> result = PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());

        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PageResourceHelper#getStyleEditorSchemasInPage(String)}
     * Given Scenario: The page has contentlets but none of their content types define DOT_STYLE_EDITOR_SCHEMA
     * Should: Return an empty list
     */
    @Test
    public void getStyleEditorSchemasInPage_whenContentTypeHasNoSchema_returnsEmpty()
            throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final List<JsonNode> result = PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());

        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PageResourceHelper#getStyleEditorSchemasInPage(String)}
     * Given Scenario: The page has a contentlet whose content type defines DOT_STYLE_EDITOR_SCHEMA
     * Should: Return one parsed schema containing the content type variable
     */
    @Test
    public void getStyleEditorSchemasInPage_whenContentTypeHasSchema_returnsSchema()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String schema = String.format(
                "{\"contentType\":\"%s\",\"sections\":[]}", contentType.variable());
        contentType = ContentTypeBuilder.builder(contentType)
                .metadata(Map.of("DOT_STYLE_EDITOR_SCHEMA", schema))
                .build();
        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);

        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final List<JsonNode> schemas = PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());

        assertEquals(1, schemas.size());
        assertTrue(schemas.get(0).toString().contains(contentType.variable()));
    }

    /**
     * Method to test: {@link PageResourceHelper#getStyleEditorSchemasInPage(String)}
     * Given Scenario: The page has multiple contentlets of the same content type (which defines a schema)
     * Should: Return only one schema entry — no duplicates per content type
     */
    @Test
    public void getStyleEditorSchemasInPage_whenDuplicateContentType_returnsOneSchema()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String schema = String.format(
                "{\"contentType\":\"%s\",\"sections\":[]}", contentType.variable());
        contentType = ContentTypeBuilder.builder(contentType)
                .metadata(Map.of("DOT_STYLE_EDITOR_SCHEMA", schema))
                .build();
        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);

        final Contentlet contentlet1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setContentlet(contentlet1)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();
        new MultiTreeDataGen()
                .setContentlet(contentlet2)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final List<JsonNode> schemas = PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());

        assertEquals(1, schemas.size());
    }

    /**
     * Method to test: {@link PageResourceHelper#getStyleEditorSchemas(List)}
     * Given Scenario: Three contentlets are provided — two share the same content type (both with a
     *                 schema) and one belongs to a different content type (also with a schema)
     * Should: Return exactly two schemas, one per distinct content type (no duplicates)
     */
    @Test
    public void getStyleEditorSchemas_whenTwoTypesThreeContentlets_returnsTwoSchemas()
            throws DotDataException, DotSecurityException {
        ContentType typeA = new ContentTypeDataGen().nextPersisted();
        ContentType typeB = new ContentTypeDataGen().nextPersisted();

        final String schemaA = String.format(
                "{\"contentType\":\"%s\",\"sections\":[]}", typeA.variable());
        final String schemaB = String.format(
                "{\"contentType\":\"%s\",\"sections\":[]}", typeB.variable());

        typeA = ContentTypeBuilder.builder(typeA)
                .metadata(Map.of("DOT_STYLE_EDITOR_SCHEMA", schemaA))
                .build();
        typeA = APILocator.getContentTypeAPI(APILocator.systemUser()).save(typeA);

        typeB = ContentTypeBuilder.builder(typeB)
                .metadata(Map.of("DOT_STYLE_EDITOR_SCHEMA", schemaB))
                .build();
        typeB = APILocator.getContentTypeAPI(APILocator.systemUser()).save(typeB);

        final Contentlet contentletA1 = new ContentletDataGen(typeA.id()).nextPersisted();
        final Contentlet contentletA2 = new ContentletDataGen(typeA.id()).nextPersisted();
        final Contentlet contentletB  = new ContentletDataGen(typeB.id()).nextPersisted();

        final List<JsonNode> schemas = PageResourceHelper.getStyleEditorSchemas(
                List.of(contentletA1, contentletA2, contentletB));

        assertEquals(2, schemas.size());

        final String allSchemas = schemas.toString();
        assertTrue(allSchemas.contains(typeA.variable()));
        assertTrue(allSchemas.contains(typeB.variable()));
    }

    /**
     * Method to test: {@link PageResourceHelper#saveContentletStyles(String, List, User)}
     * Given Scenario: A container holds three contentlets in order (treeOrder 0, 1, 2).
     *                 Styles are applied only to the second contentlet (treeOrder 1).
     * Should: Preserve the original treeOrder for every contentlet — the style-only save
     *         must not reset the position of the edited contentlet to 0.
     *
     * Regression test for the bug where {@code saveMultiTrees} was called with a single-entry list,
     * causing the edited contentlet to receive {@code treeOrder = 0} and appear first in the container.
     */
    @Test
    public void saveContentletStyles_whenStyleAppliedToSecondContentlet_doesNotChangeOrder()
            throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final String containerUuid = ContainerUUID.UUID_LEGACY_VALUE;
        final Template template = new TemplateDataGen()
                .withContainer(container, containerUuid)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet first  = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet second = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet third  = new ContentletDataGen(contentType.id()).nextPersisted();

        // Save MultiTree entries with explicit treeOrder values.
        // MultiTreeDataGen always forces treeOrder=1, so we insert directly via the API.
        // setPersonalization() returns void so the chain must be broken into separate calls.
        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final MultiTree mt0 = new MultiTree()
                .setHtmlPage(page.getIdentifier())
                .setContainer(container.getIdentifier())
                .setContentlet(first.getIdentifier())
                .setInstanceId(containerUuid)
                .setVariantId(VariantAPI.DEFAULT_VARIANT.name())
                .setTreeOrder(0);
        mt0.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
        multiTreeAPI.saveMultiTree(mt0);

        final MultiTree mt1 = new MultiTree()
                .setHtmlPage(page.getIdentifier())
                .setContainer(container.getIdentifier())
                .setContentlet(second.getIdentifier())
                .setInstanceId(containerUuid)
                .setVariantId(VariantAPI.DEFAULT_VARIANT.name())
                .setTreeOrder(1);
        mt1.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
        multiTreeAPI.saveMultiTree(mt1);

        final MultiTree mt2 = new MultiTree()
                .setHtmlPage(page.getIdentifier())
                .setContainer(container.getIdentifier())
                .setContentlet(third.getIdentifier())
                .setInstanceId(containerUuid)
                .setVariantId(VariantAPI.DEFAULT_VARIANT.name())
                .setTreeOrder(2);
        mt2.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
        multiTreeAPI.saveMultiTree(mt2);

        // Apply styles to only the second contentlet (treeOrder=1).
        final ContainerEntry entry = new ContainerEntry(
                null,
                container.getIdentifier(),
                containerUuid,
                List.of(second.getIdentifier()),
                Map.of(second.getIdentifier(), Map.of("color", "red"))
        );

        PageResourceHelper.getInstance().saveContentletStyles(
                page.getIdentifier(), List.of(entry), APILocator.systemUser());

        // Verify original treeOrder is unchanged for every contentlet.
        final Map<String, Integer> orderByContentlet = multiTreeAPI
                .getMultiTrees(page.getIdentifier())
                .stream()
                .collect(Collectors.toMap(MultiTree::getContentlet, MultiTree::getTreeOrder));

        assertEquals("first contentlet must keep treeOrder 0",
                0, (int) orderByContentlet.get(first.getIdentifier()));
        assertEquals("second contentlet must keep treeOrder 1",
                1, (int) orderByContentlet.get(second.getIdentifier()));
        assertEquals("third contentlet must keep treeOrder 2",
                2, (int) orderByContentlet.get(third.getIdentifier()));
    }
}
