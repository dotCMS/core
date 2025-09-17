package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rendering.velocity.RecycledHttpServletRequest;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class ContentMapTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static Host defaultHost;
    private static FieldAPI fieldAPI;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI  = APILocator.getHostAPI();
        userAPI  = APILocator.getUserAPI();
        user     = userAPI.getSystemUser();
        fieldAPI = APILocator.getContentTypeFieldAPI();

        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        languageAPI    = APILocator.getLanguageAPI();
        defaultHost    = hostAPI.findDefaultHost(user, false);
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    /**
     * Method to test: {@link ContentMap#getFieldVariables(String)}
     * Given Scenario: Creates a field variable over the generic content called format, which value is 'txt'.
     * ExpectedResult: The ContentMap should retrieve that field variable
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGet_Field_Variable() throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final ContentType contentType = contentTypeAPI.find("webPageContent");
        final Field field = contentType.fieldMap().get("title");
        final FieldVariable fieldVariable = ImmutableFieldVariable.builder()
                .fieldId(field.inode())
                .name("format").key("format").value("txt").userId(APILocator.systemUser().getUserId())
                .build();

        APILocator.getContentTypeFieldAPI().save(fieldVariable, APILocator.systemUser());

        // Create dummy "News" content
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .structure(contentType.id())
                .host(defaultHost);

        contentletDataGen.setProperty("title", "test");
        contentletDataGen.setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT);

        // Persist dummy "News" contents to ensure at least one result will be returned
        final Contentlet contentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(contentlet);

        final Context velocityContext = mock(Context.class);

        final ContentMap contentMap = new ContentMap(contentlet, userAPI.getAnonymousUser(),
                PageMode.LIVE,defaultHost,velocityContext);

        final Map<String, FieldVariable> fieldVariableMap = (Map<String, FieldVariable>) contentMap.getFieldVariables("title");

        assertNotNull(fieldVariableMap);
        assertEquals("txt", fieldVariableMap.get("format").value());
    }

    /**
     * This test is for issue https://github.com/dotCMS/core/issues/16409
     * Categories should be pulled in Front End
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGet_showCategories_AsAnonUser() throws DotDataException, DotSecurityException {

        // save proper permissions to SYSTEM_HOST

        final Permission catsPermsSystemHost = new Permission();
        final Role anonUserRole = APILocator.getRoleAPI().loadRoleByKey("CMS Anonymous");
        catsPermsSystemHost.setRoleId(anonUserRole.getId());
        catsPermsSystemHost.setInode(Host.SYSTEM_HOST);
        catsPermsSystemHost.setBitPermission(true);
        catsPermsSystemHost.setType(PermissionType.CATEGORY.getKey());
        catsPermsSystemHost.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(catsPermsSystemHost, APILocator.systemHost(),
                APILocator.systemUser(), false);

        //Create Categories
        final Category categoryChild1 = new CategoryDataGen().setCategoryName("RoadBike-"+System.currentTimeMillis()).setKey("RoadBike").setKeywords("RoadBike").setCategoryVelocityVarName("roadBike").next();
        final Category categoryChild2 = new CategoryDataGen().setCategoryName("MTB-"+System.currentTimeMillis()).setKey("MTB").setKeywords("MTB").setCategoryVelocityVarName("mtb").next();
        final Category rootCategory = new CategoryDataGen().setCategoryName("Bikes-"+System.currentTimeMillis())
                .setKey("Bikes").setKeywords("Bikes").setCategoryVelocityVarName("bikes").children(categoryChild1,categoryChild2).nextPersisted();

        // Get "News" content-type
        final ContentType contentType = TestDataUtils.getNewsLikeContentType("newsCategoriesTest"+System.currentTimeMillis(),rootCategory.getInode());

        // Create dummy "News" content
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .host(defaultHost);

        contentletDataGen.setProperty("title", "Bicycle");
        contentletDataGen.setProperty("byline", "Bicycle");
        contentletDataGen.setProperty("story", "BicycleBicycleBicycle");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/bicycle");
        contentletDataGen.addCategory(categoryChild1);
        contentletDataGen.addCategory(categoryChild2);

        // Persist dummy "News" contents to ensure at least one result will be returned
        final Contentlet contentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(contentlet);

        final Context velocityContext = mock(Context.class);

        final ContentMap contentMap = new ContentMap(contentlet, userAPI.getAnonymousUser(),
                PageMode.LIVE,defaultHost,velocityContext);

        //If is null no categories were pulled
        assertNotNull(contentMap.get("categories"));

        APILocator.getContentTypeAPI(user).delete(contentType);
        APILocator.getCategoryAPI().delete(categoryChild2,user,false);
        APILocator.getCategoryAPI().delete(categoryChild1,user,false);
        APILocator.getCategoryAPI().delete(rootCategory,user,false);


    }

    /**
     * Test {@link ContentMap#get(String)} method applied on a relationship field of a content that
     * has multilingual related content. Only versions of related content in the parent language
     * should be retrieved
     */
    @Test
    public void testGetRelationshipFieldFromContentMapWithMultilingualRelatedContent()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            createTextField(textFieldString, parentContentType.id());
            createTextField(textFieldString, childContentType.id());

            final Field relationshipField = createRelationshipField("newRel",
                    parentContentType.id(),
                    childContentType.variable(), String.valueOf(
                            RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));

            //Create Contentlet
            final ContentletDataGen parentContentletDataGen = new ContentletDataGen(
                    parentContentType.id());
            final ContentletDataGen childContentletDataGen = new ContentletDataGen(
                    childContentType.id());

            Contentlet contentletParent = parentContentletDataGen
                    .setProperty(textFieldString, "parent Contentlet").next();

            //Children in English
            Contentlet contentletChild = childContentletDataGen
                    .setProperty(textFieldString, "child Contentlet").nextPersisted();
            contentletChild = ContentletDataGen.publish(contentletChild);

            Contentlet contentletChild2 = childContentletDataGen
                    .setProperty(textFieldString, "child Contentlet 2").nextPersisted();
            contentletChild2 = ContentletDataGen.publish(contentletChild2);

            //Children in Spanish
            Contentlet contentletChild3 = childContentletDataGen
                    .setProperty(textFieldString, "child Contentlet 3")
                    .setProperty(Contentlet.LANGUAGEID_KEY,
                            TestDataUtils.getSpanishLanguage().getId()).nextPersisted();
            contentletChild3 = ContentletDataGen.publish(contentletChild3);

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(relationshipField, user);

            //Relate contentlets
            final ContentletRelationships contentletRelationships = new ContentletRelationships(
                    contentletParent);

            final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords = contentletRelationships.new ContentletRelationshipRecords(
                    relationship, true);
            contentletRelationshipRecords.setRecords(
                    CollectionsUtils.list(contentletChild, contentletChild2, contentletChild3));
            contentletRelationships.getRelationshipsRecords().add(contentletRelationshipRecords);

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI
                    .checkin(contentletParent, contentletRelationships, null, null, user, false);
            contentletParent = ContentletDataGen.publish(contentletParent);

            final Context velocityContext = mock(Context.class);

            final ContentMap contentMap = new ContentMap(contentletParent,
                    userAPI.getAnonymousUser(),
                    PageMode.LIVE, defaultHost, velocityContext);

            final List<ContentMap> result = (List) contentMap.get(relationshipField.variable());
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(contentletChild.getIdentifier(), result.get(0).get("identifier"));
            assertEquals(contentletChild2.getIdentifier(), result.get(1).get("identifier"));

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testGetDateFieldFromContentMap() throws DotDataException, DotSecurityException {
        ContentType contentType = null;
        try {
            final Date contentDate = new Date();

            contentType = createContentType("testContentTypeWithDateField");

            // create date field
            createDateField("testDateField", contentType.id());

            // create contentlet
            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
            final Contentlet contentlet = contentletDataGen
                    .setProperty("testDateField", contentDate).next();

            // verify contentlet date field format
            final Context velocityContext = mock(Context.class);

            final ContentMap contentMap = new ContentMap(contentlet,
                    userAPI.getAnonymousUser(),
                    PageMode.LIVE, defaultHost, velocityContext);

            final Date resultDate = (Date) contentMap.get("testDateField");
            assertNotNull(resultDate);

            final SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS");
            final String formattedExpectedDate = dateFormat.format(contentDate);
            final String dateWithTrailingDigits = resultDate.toString();

            final int startIndex = dateWithTrailingDigits.lastIndexOf('.');
            String formattedResultDate = dateWithTrailingDigits;
            if (startIndex > 1) {
                final int endIndex = Math.min(startIndex + 4, dateWithTrailingDigits.length());
                BigDecimal trailingDigits = new BigDecimal(
                        dateWithTrailingDigits.substring(startIndex - 1, endIndex));
                DecimalFormat decimalFormat = new DecimalFormat("0.000");
                formattedResultDate =
                        dateWithTrailingDigits.substring(0, startIndex - 1) +
                        decimalFormat.format(trailingDigits);
            }

            assertEquals(formattedExpectedDate, formattedResultDate);

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     *
     * @param name
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    /**
     *
     * @param fieldName
     * @param contentTypeId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createTextField(final String fieldName, final String contentTypeId)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(TextField.class).name(fieldName).contentTypeId(contentTypeId).build();

        return fieldAPI.save(field, user);
    }

    /**
     *
     * @param relationshipName
     * @param parentTypeId
     * @param childTypeVar
     * @param cardinality
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createRelationshipField(final String relationshipName, final String parentTypeId,
            final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality).relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    /**
     *
     * @param fieldName
     * @param contentTypeId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createDateField(final String fieldName, final String contentTypeId)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(DateField.class).name(fieldName)
                .contentTypeId(contentTypeId).build();

        return fieldAPI.save(field, user);
    }

    /**
     * Method to test: {@link ContentMap#get(String)} applied on a key-value field
     * Given Scenario: Creates a key-value field with keys containing non-word characters (spaces, hyphens, dots, etc.)
     * ExpectedResult: The ContentMap should preserve the original keys with non-word characters
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGetKeyValueFieldWithNonWordCharacters() throws DotDataException, DotSecurityException {
        ContentType contentType = null;
        try {
            // Create content type
            contentType = createContentType("testContentTypeWithKeyValueField");

            // Create key-value field
            final Field keyValueField = createKeyValueField("testKeyValueField", contentType.id());

            // Create content with key-value data containing non-word characters
            final String keyValueData = "{\"my-key\": \"value1\", \"my key.with spaces\": \"value2\", \"my@special#key\": \"value3\"}";
            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
            final Contentlet contentlet = contentletDataGen
                    .setProperty("testKeyValueField", keyValueData).next();

            // Test ContentMap behavior
            final Context velocityContext = mock(Context.class);
            final ContentMap contentMap = new ContentMap(contentlet, userAPI.getAnonymousUser(),
                    PageMode.LIVE, defaultHost, velocityContext);

            // Get the key-value field result
            final Map<String, Object> result = (Map<String, Object>) contentMap.get("testKeyValueField");
            assertNotNull(result);

            // Verify that keys with non-word characters are preserved
            assertEquals("value1", result.get("my-key"));
            assertEquals("value2", result.get("my key.with spaces"));
            assertEquals("value3", result.get("my@special#key"));

            // Verify that the keys set contains the original keys
            final java.util.Set<String> keys = (java.util.Set<String>) result.get("keys");
            assertNotNull(keys);
            assertEquals(true, keys.contains("my-key"));
            assertEquals(true, keys.contains("my key.with spaces"));
            assertEquals(true, keys.contains("my@special#key"));

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * Helper method to create a key-value field
     * @param fieldName
     * @param contentTypeId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createKeyValueField(final String fieldName, final String contentTypeId)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(KeyValueField.class).name(fieldName)
                .contentTypeId(contentTypeId).build();

        return fieldAPI.save(field, user);
    }

    /**
     * Method to test: {@link ContentMap#get(String)} applied on a request on context recycled
     * Given Scenario: Tries to recover a property, but the request is already recycled
     * ExpectedResult: The recycled request should not broke the get
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGetRecycledRequest() throws DotDataException, DotSecurityException {

        final List<Contentlet> contentlets = APILocator.getContentletAPI().findAllContent(1, 5);
        if(contentlets.isEmpty()) {
            throw new DotDataException("No contentlets found");
        }

        final Contentlet content = contentlets.get(0);
        final User user = APILocator.systemUser();
        final boolean EDIT_OR_PREVIEW_MODE = true;
        final Host host = APILocator.systemHost();
        final Context context = new VelocityContext(Map.of("request", new RecycledHttpServletRequest(null)));
        final ContentMap contentMap = new ContentMap(content, user, EDIT_OR_PREVIEW_MODE, host, context);

        final Object title = contentMap.get("title");
        Assert.assertNotNull(title);
    }

}
