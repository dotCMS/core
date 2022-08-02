package com.dotcms.rendering.velocity.viewtools.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
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
import java.util.Date;
import java.util.List;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Assert.assertNotNull(contentMap.get("categories"));

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

}
