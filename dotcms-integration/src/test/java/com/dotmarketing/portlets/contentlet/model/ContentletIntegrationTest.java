package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ContentletIntegrationTest {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static Language defaultLanguage;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static RoleAPI roleAPI;
    private static UserAPI userAPI;
    private static User user;
    private static Host site;

    private final static String CARDINALITY = String
            .valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

    public static class SetRelatedTestCase {

        String filterType;
        boolean checkIn;

        public SetRelatedTestCase(final String filterType) {
            this.filterType = filterType;
            this.checkIn = true;
        }

        public SetRelatedTestCase(final String filterType, final boolean checkIn) {
            this.filterType = filterType;
            this.checkIn = checkIn;
        }
    }

    @DataProvider
    public static Object[] setRelatedTestCases(){
        return new SetRelatedTestCase[]{
                //Setting related content by setProperty method
                new SetRelatedTestCase("property"),
                //Setting related content by setRelatedByQuery method
                new SetRelatedTestCase("query"),
                //Setting related content by setRelatedById method
                new SetRelatedTestCase("id"),
                //Setting related content by setRelated method
                new SetRelatedTestCase("method"),
                //Setting related content by setProperty method without saving the content should
                //not save related content on cache
                new SetRelatedTestCase("property", false)
        };
    }


    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        fieldAPI    = APILocator.getContentTypeFieldAPI();
        languageAPI = APILocator.getLanguageAPI();

        userAPI = APILocator.getUserAPI();
        roleAPI = APILocator.getRoleAPI();
        user    = userAPI.getSystemUser();

        contentletAPI   = APILocator.getContentletAPI();
        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
        defaultLanguage = languageAPI.getDefaultLanguage();
        site = new SiteDataGen().nextPersisted();
    }


    @Test
    public void testGetContentTypeAlwaysReturnsTheLatestCachedVersion()
            throws DotSecurityException, DotDataException {

        Field field;

        //Create Content Type.
        final ContentType contentType = getNewContentType();

        try {
            //Creating new Text Field.
            field = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            fieldAPI.save(field, user);

            ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());

            final Contentlet contentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            assertNotNull(contentlet.getContentType());

            //Adding a new field in the content type
            field = ImmutableTextField.builder()
                    .name("Description")
                    .variable("Description")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.LONG_TEXT)
                    .build();

            fieldAPI.save(field, user);

            final ContentType cachedContentType = contentTypeAPI.find(contentType.inode());

            //Both content types (the one contained in the contentlet and the cached one) must be the same
            assertEquals(cachedContentType.fields().size(),
                    contentlet.getContentType().fields().size());

            assertEquals(cachedContentType.inode(), contentlet.getContentType().inode());

            assertEquals(cachedContentType.modDate(), contentlet.getContentType().modDate());

        }finally{
            contentTypeAPI.delete(contentType);
        }
    }

    @Test
    public void testGetRelatedForOneSidedRelationship()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try{

            //Create Content Types
            parentContentType = getNewContentType();
            childContentType = getNewContentType();

            //Create Content
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).next();

            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).nextPersisted();

            //Create Relationship
            final Field field = createAndSaveRelationshipField("myChild", parentContentType.id(),
                    childContentType.variable(), CARDINALITY);

            final Relationship relationship = relationshipAPI.getRelationshipFromField(field, user);

            //Save related content
            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, CollectionsUtils.list(childContentlet)), user,
                    false);

            //No cached value
            List<Contentlet> result = parentContentlet.getRelated(field.variable(), user, false);

            assertEquals(1, result.size());
            assertEquals(childContentlet.getIdentifier(), result.get(0).getIdentifier());

            //Cached value
            result = parentContentlet.getRelated(field.variable(), user, false);

            assertEquals(1, result.size());
            assertEquals(childContentlet.getIdentifier(), result.get(0).getIdentifier());

        }finally{

            if (parentContentType !=null && parentContentType.id() != null){
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType !=null && childContentType.id() != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testGetRelatedForOneSidedRelationshipWhenLimitedUserShouldReturnEmptyList()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType = null;
        Role newRole = null;

        try {

            //Create Content Types
            parentContentType = getNewContentType();
            childContentType = getNewContentType();

            //Create Content
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).next();

            final ContentletDataGen childContentletDataGen = new ContentletDataGen(childContentType.id());

            final Contentlet childContentlet1 = childContentletDataGen
                    .languageId(defaultLanguage.getId()).nextPersisted();

            final Contentlet childContentlet2 = childContentletDataGen
                    .languageId(defaultLanguage.getId()).nextPersisted();


            //Create Relationship
            final Field field = createAndSaveRelationshipField("myChild", parentContentType.id(),
                    childContentType.variable(), CARDINALITY);

            final Relationship relationship = relationshipAPI.getRelationshipFromField(field, user);

            //Save related content
            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, CollectionsUtils.list(childContentlet1, childContentlet2)),
                    user,
                    false);

            newRole = createRole();
            User createdLimitedUser = TestUserUtils
                    .getUser(newRole, "email" + System.currentTimeMillis() + "@dotcms.com",
                            "name" + System.currentTimeMillis(),
                            "lastName" + System.currentTimeMillis(),
                            "password" + System.currentTimeMillis());

            //set individual permissions to the child
            APILocator.getPermissionAPI()
                    .save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                            childContentlet1.getPermissionId(),
                            newRole.getId(),
                            PermissionAPI.PERMISSION_READ, true), childContentlet1, user, false);

            //Get related content with anonymous user
            List<Contentlet> result = parentContentlet
                    .getRelated(field.variable(), createdLimitedUser, false);

            assertEquals(1, result.size());
            assertEquals(childContentlet1.getIdentifier(), result.get(0).getIdentifier());

            //Get related content with system user
            result = parentContentlet.getRelated(field.variable(), user, false);

            assertEquals(2, result.size());
            assertEquals(childContentlet1.getIdentifier(), result.get(0).getIdentifier());
            assertEquals(childContentlet2.getIdentifier(), result.get(1).getIdentifier());

        } finally {

            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }

            if (newRole != null){
                roleAPI.delete(newRole);
            }
        }
    }

    @UseDataProvider("setRelatedTestCases")
    @Test
    public void testSetRelatedForOneSidedRelationship(final SetRelatedTestCase testCase)
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try{

            //Create Content Types
            parentContentType = getNewContentType();
            childContentType = getNewContentType();

            //Create Content
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).next();

            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).nextPersisted();

            //Create Relationship
            final Field field = createAndSaveRelationshipField("myChild", parentContentType.id(),
                    childContentType.variable(), CARDINALITY);

            //case: related child is saved
            parentContentlet = validateSetRelated(testCase, parentContentlet, CollectionsUtils.list(childContentlet),
                    field);

            if (testCase.checkIn) {
                parentContentlet = contentletAPI.checkout(parentContentlet.getInode(), user, false);
            }
            //case: related child is kept when property is set as null
            parentContentlet = validateSetRelated(testCase, parentContentlet, null,
                    field);

            if (testCase.checkIn) {
                parentContentlet = contentletAPI.checkout(parentContentlet.getInode(), user, false);
            }
            //case: related child is wiped out when property is set as an empty list
            validateSetRelated(testCase, parentContentlet, Collections.EMPTY_LIST,
                    field);
        }finally{

            if (parentContentType !=null && parentContentType.id() != null){
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType !=null && childContentType.id() != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @UseDataProvider("setRelatedTestCases")
    @Test
    public void testSetRelatedForSelfRelationship(final SetRelatedTestCase testCase)
            throws DotDataException, DotSecurityException {

        ContentType contentType = null;

        try{
            //Create Content Type
            contentType = getNewContentType();

            //Create Content
            Contentlet parentContentlet = new ContentletDataGen(contentType.id())
                    .languageId(defaultLanguage.getId()).next();

            Contentlet childContentlet = new ContentletDataGen(contentType.id())
                    .languageId(defaultLanguage.getId()).nextPersisted();

            //Create Relationship
            final Field parentField = createAndSaveRelationshipField("myChild", contentType.id(),
                    contentType.variable(), CARDINALITY);

            final Field childField = createAndSaveRelationshipField("myParent", contentType.id(),
                    contentType.variable() + StringPool.PERIOD + parentField.variable(), CARDINALITY);

            //case: related child is saved
            parentContentlet = validateSetRelated(testCase, parentContentlet, CollectionsUtils.list(childContentlet),
                    parentField);

            if (testCase.checkIn) {
                parentContentlet = contentletAPI.checkout(parentContentlet.getInode(), user, false);
            }
            //case: related child is kept when property is set as null
            parentContentlet = validateSetRelated(testCase, parentContentlet, null,
                    parentField);

            if (testCase.checkIn) {
                parentContentlet = contentletAPI.checkout(parentContentlet.getInode(), user, false);
            }

            //case: related child is wiped out when property is set as an empty list
            parentContentlet = validateSetRelated(testCase, parentContentlet, Collections.EMPTY_LIST,
                    parentField);

            if (testCase.checkIn) {
                childContentlet = contentletAPI.checkout(childContentlet.getInode(), user, false);
            }

            //case: related parent is saved
            childContentlet = validateSetRelated(testCase, childContentlet, CollectionsUtils.list(parentContentlet),
                    childField);

            if (testCase.checkIn) {
                childContentlet = contentletAPI.checkout(childContentlet.getInode(), user, false);
            }
            //case: related parent is kept when property is set as null
            childContentlet = validateSetRelated(testCase, childContentlet, null,
                    childField);

            if (testCase.checkIn) {
                childContentlet = contentletAPI.checkout(childContentlet.getInode(), user, false);
            }
            //case: related parent is wiped out when property is set as an empty list
            validateSetRelated(testCase, childContentlet, Collections.EMPTY_LIST,
                    childField);
        }finally{

            if (contentType !=null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * Perform validations over all setRelated variants
     * @param testCase
     * @param parentContentlet
     * @param childContentletList
     * @param field
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Contentlet validateSetRelated(final SetRelatedTestCase testCase, final Contentlet parentContentlet,
            final List<Contentlet> childContentletList, final Field field)
            throws DotDataException, DotSecurityException {
        switch (testCase.filterType) {
            case "property":
                parentContentlet
                        .setProperty(field.variable(), childContentletList);
                break;

            case "id":
                parentContentlet.setRelatedById(field,
                        childContentletList != null ? childContentletList.stream().map(
                                contentlet -> contentlet.getIdentifier())
                                .collect(Collectors.toList()) : null, user, false);
                break;

            case "query":
                String query = null;

                if (childContentletList != null){
                    if (!childContentletList.isEmpty()){
                        query = childContentletList.get(0).getIdentifier();
                    } else{
                        query = "";
                    }
                }
                parentContentlet.setRelatedByQuery(field, query, null, user, false);
                break;

            case "method":
                parentContentlet.setRelated(field, childContentletList);
                break;
        }

        final Contentlet savedContentlet;
        if (testCase.checkIn) {
            parentContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            parentContentlet.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            //Save related content
            savedContentlet = contentletAPI.checkin(parentContentlet, user, false);
            final List<Contentlet> result = contentletAPI.getRelatedContent(savedContentlet,
                    relationshipAPI.getRelationshipFromField(field, user), user, false);

            if (childContentletList != null) {
                assertEquals(childContentletList.size(), result.size());

                //validate related content is saved correctly
                if (!childContentletList.isEmpty()) {
                    assertEquals(childContentletList.get(0).getIdentifier(),
                            result.get(0).getIdentifier());
                }

            } else {
                //when calling checkin with null related, related content should keep the same
                assertFalse(result.isEmpty());
            }
        } else {
            savedContentlet = parentContentlet;
            //validate none setter is modifying contentlet.relatedIds cache if the contentlet is not saved
            assertTrue(parentContentlet.getRelated(field.variable(), user).isEmpty());
        }

        return savedContentlet;
    }

    private Role createRole() throws DotDataException {
        final long millis =  System.currentTimeMillis();

        // Create Role.
        Role newRole = new Role();
        newRole.setName("Role" +  millis);
        newRole.setEditUsers(true);
        newRole.setEditPermissions(true);
        newRole.setSystem(false);
        newRole.setEditLayouts(true);
        newRole.setParent(newRole.getId());
        newRole = roleAPI.save(newRole);

        return newRole;
    }


    @Test
    public void testGetRelatedWhenNoRelatedContentShouldReturnEmptyList()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            //Create Content Types
            parentContentType = getNewContentType();
            childContentType = getNewContentType();

            //Create Content
            final Contentlet contentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).nextPersisted();

            //Create Relationship
            final Field field = createAndSaveRelationshipField("myChild", parentContentType.id(),
                    childContentType.variable(), CARDINALITY);

            final List<Contentlet> result = contentlet.getRelated(field.variable(), user, false);

            assertTrue(result.isEmpty());

        }finally {

            if (parentContentType !=null && parentContentType.id() != null){
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType !=null && childContentType.id() != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test(expected = DotStateException.class)
    public void testGetRelatedWhenInvalidVarFieldShouldThrowAnException()
            throws DotDataException, DotSecurityException {

        //Create Content Type.
        final ContentType contentType = getNewContentType();

        try {
            //Create Content
            final Contentlet contentlet = new ContentletDataGen(contentType.id())
                    .languageId(defaultLanguage.getId()).nextPersisted();

            contentlet.getRelated("AnyField", user, false);

        }finally{
            try {
                contentTypeAPI.delete(contentType);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ContentType getNewContentType() throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        return contentTypeAPI.save(ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Test ContentType " + time)
                .host(site.getIdentifier())
                .name("Test ContentType "+ time)
                .owner("owner")
                .variable("testContentType" + time)
                .build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName, final String parentTypeId,
            final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return fieldAPI.save(field, user);
    }

}
