package com.dotcms.content.elasticsearch.business;

import static com.dotcms.datagen.TestDataUtils.getCommentsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.relateContentTypes;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Date;
import java.util.List;

import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author nollymar
 */
public class ESContentletAPIImplTest extends IntegrationTestBase {

    private static ContentTypeAPI contentTypeAPI;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static ContentletAPI contentletAPI;
    private static FieldAPI fieldAPI;
    private static User user;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();

        contentTypeAPI = APILocator.getContentTypeAPI(user);
        languageAPI = APILocator.getLanguageAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
        contentletAPI = APILocator.getContentletAPI();
        fieldAPI = APILocator.getContentTypeFieldAPI();
    }

    @Test
    public void testGetRelatedContentFromMultilingualContent()
            throws DotSecurityException, DotDataException {

        Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet newsContentletInSpanish = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen
                    .languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News")
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    map(relationship, list(commentsContentlet)),
                    null, user, false);


            //Spanish version
            newsContentletInSpanish = contentletAPI.checkout(newsContentlet.getInode(), user, false);
            newsContentletInSpanish.setIndexPolicy(IndexPolicy.FORCE);
            newsContentletInSpanish.setInode("");
            newsContentletInSpanish.setLanguageId(spanishLanguage.getId());

            newsContentletInSpanish = contentletAPI.checkin(newsContentletInSpanish,  user, false);

            CacheLocator.getContentletCache().remove(commentsContentlet);
            CacheLocator.getContentletCache().remove(newsContentlet);

            ESContentletAPIImpl contentletAPIImpl = new ESContentletAPIImpl();
            //Pull related content from comment child
            List<Contentlet> result = contentletAPIImpl
                    .filterRelatedContent(commentsContentlet, relationship, user, false, false, -1,
                            -1);
            assertNotNull(result);
            assertTrue(result.size() == 1);
            assertEquals(newsContentlet.getIdentifier(), result.get(0).getIdentifier());

            //pulling content from parent (English version)
            result = contentletAPIImpl
                    .filterRelatedContent(newsContentlet, relationship, user, false, false, -1,
                            -1);

            assertNotNull(result);
            assertTrue(result.size() == 1);
            assertEquals(commentsContentlet.getIdentifier(), result.get(0).getIdentifier());

            //pulling content from parent (Spanish version)
            result = contentletAPIImpl
                    .filterRelatedContent(newsContentletInSpanish, relationship, user, false, false, -1,
                            -1);

            assertNotNull(result);
            assertTrue(result.size() == 1);
            assertEquals(commentsContentlet.getIdentifier(), result.get(0).getIdentifier());
        } finally {
            if (newsContentlet != null && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (newsContentletInSpanish != null && UtilMethods.isSet(newsContentletInSpanish.getInode())) {
                ContentletDataGen.remove(newsContentletInSpanish);
            }

            if (commentsContentlet != null && UtilMethods.isSet(commentsContentlet.getInode())) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    @Test()
    public void testCheckInWithLegacyRelationshipsAndReadOnlyClusterShouldThrowAnException()
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType contentType = null;
        final ESReadOnlyMonitor esReadOnlyMonitor = mock(ESReadOnlyMonitor.class);
        final ESContentletAPIImpl contentletAPIImpl = new ESContentletAPIImpl(esReadOnlyMonitor);

        try {
            contentType = createContentType("test" + time);

            final Structure structure = new StructureTransformer(contentType).asStructure();

            final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();
            final ContentletRelationships contentletRelationship = new ContentletRelationships(
                    contentlet);

            final Relationship relationship = new Relationship(structure, structure,
                    "parent" + contentType.variable(), "child" + contentType.variable(),
                    RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), false, false);

            final ContentletRelationshipRecords relationshipsRecord = contentletRelationship.new ContentletRelationshipRecords(
                    relationship,
                    false);

            contentletRelationship
                    .setRelationshipsRecords(CollectionsUtils.list(relationshipsRecord));

            setClusterAsReadOnly(true);

            contentletAPIImpl.checkin(contentlet, contentletRelationship, null, null, user, false);

            throw new  AssertionError("DotContentletStateException Expected");
        } catch(DotContentletStateException e) {
            verify(esReadOnlyMonitor).start();
        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }

            setClusterAsReadOnly(false);
        }
    }

    /**
     * Test for isCheckInSafe method with legacy relationships. It should return false when the cluster
     * is in read only mode
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testIsCheckInSafeWithLegacyRelationshipsShouldReturnFalse()
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType contentType = null;

        try {
            contentType = createContentType("test" + time);

            final Structure structure = new StructureTransformer(contentType).asStructure();

            final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();
            final ContentletRelationships contentletRelationship = new ContentletRelationships(
                    contentlet);

            final Relationship relationship = new Relationship(structure, structure,
                    "parent" + contentType.variable(), "child" + contentType.variable(),
                    RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), false, false);

            final ContentletRelationshipRecords relationshipsRecord = contentletRelationship.new ContentletRelationshipRecords(
                    relationship,
                    false);

            contentletRelationship
                    .setRelationshipsRecords(CollectionsUtils.list(relationshipsRecord));

            setClusterAsReadOnly(true);

            assertFalse(
                    new ESContentletAPIImpl().isCheckInSafe(contentletRelationship));

        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }

            setClusterAsReadOnly(false);
        }
    }

    /**
     * Test for isCheckInSafe method with relationship fields. It should return true when the cluster
     * is in read only mode
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testIsCheckInSafeWithRelationshipsFieldsShouldReturnTrue()
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType contentType = null;

        try {
            contentType = createContentType("test" + time);

            final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();
            final ContentletRelationships contentletRelationship = new ContentletRelationships(
                    contentlet);

            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(contentType.id()).values(String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                    .relationType(contentType.variable()).build();

            field = fieldAPI.save(field, user);

            final String fullFieldVar = contentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            final ContentletRelationshipRecords relationshipsRecord = contentletRelationship.new ContentletRelationshipRecords(
                    relationship,false);

            contentletRelationship
                    .setRelationshipsRecords(CollectionsUtils.list(relationshipsRecord));

            setClusterAsReadOnly(true);

            assertTrue(
                    new ESContentletAPIImpl().isCheckInSafe(contentletRelationship));

        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }

            setClusterAsReadOnly(false);
        }
    }

    /**
     * Test for isCheckInSafe method without relationships. It should return true no matter the cluster status
     */
    @Test
    public void testIsCheckInSafeWithoutRelationshipsShouldReturnTrue() {
        setClusterAsReadOnly(true);

        try {
            assertTrue(
                    new ESContentletAPIImpl().isCheckInSafe(null));
        } finally {
            setClusterAsReadOnly(false);
        }
    }


    /**
     * Method to test: {@link ESContentletAPIImpl#lock(Contentlet, User, boolean)}
     * Given Scenario: A user without permission try to lock a contentlet
     * ExpectedResult: Should throw a DotSecurityException
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void whenTryToLockShouldThrowDotSecurityException() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();

        final ESContentletAPIImpl esContentletAPI = new ESContentletAPIImpl();
        final Contentlet contentletSaved = esContentletAPI.checkin(contentlet, APILocator.systemUser(), false);
        esContentletAPI.lock(contentletSaved, user, false);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#lock(Contentlet, User, boolean)}
     * Given Scenario: A user with {@link PermissionLevel#EDIT} permission try to lock a contentlet
     * ExpectedResult: Should work
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenTryToLockShouldWork() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();

        final ESContentletAPIImpl esContentletAPI = new ESContentletAPIImpl();
        final Contentlet contentletSaved = esContentletAPI.checkin(contentlet, APILocator.systemUser(), false);

        addPermission(role, contentType, PermissionLevel.WRITE);
        addPermission(role, contentletSaved, PermissionLevel.WRITE);

        esContentletAPI.lock(contentletSaved, user, false);

        checkLock(user, contentletSaved);
    }

    private void checkLock(final User user, final Contentlet contentletSaved) throws DotDataException {
        final ContentletVersionInfo info = APILocator.getVersionableAPI().
                getContentletVersionInfo(contentletSaved.getIdentifier(), contentletSaved.getLanguageId());

        assertNotNull(info.getLockedBy());
        assertNotNull(info.getLockedOn());
        assertEquals(user.getUserId(), info.getLockedBy());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#lock(Contentlet, User, boolean)}
     * Given Scenario: The contentlet's owner without permission to EDIT try to lock the contebtlet
     * ExpectedResult: Should work
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test()
    public void whenOwnerTryToLockShouldWork() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                 .next();

        addPermission(role, contentType, PermissionLevel.WRITE);

        final ESContentletAPIImpl esContentletAPI = new ESContentletAPIImpl();
        final Contentlet contentletSaved = esContentletAPI.checkin(contentlet, user, false);

        final Role ownerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        addPermission(ownerRole, contentletSaved, PermissionLevel.WRITE);

        esContentletAPI.lock(contentletSaved, user, false);

        checkLock(user, contentletSaved);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#filterRelatedContent(Contentlet, Relationship, User, boolean, Boolean, int, int)}
     * Given Scenario: When a related content is obtained from the index and in database this content
     *                  doesn't exist (the index hasn't been updated yet), the returned list should not
     *                  contain this "dirty" related content. Only applies for legacy relationships
     * ExpectedResult: The method should return an empty list
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGetRelatedContentWhenIndexIsMessedUp()
            throws DotDataException, DotSecurityException {

        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen
                    .languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News")
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    map(relationship, list(commentsContentlet)),
                    null, user, false);

            CacheLocator.getContentletCache().remove(commentsContentlet);
            CacheLocator.getContentletCache().remove(newsContentlet);

            final ESContentletAPIImpl contentletAPIImpl = Mockito.spy(new ESContentletAPIImpl());

            Mockito.doReturn(new Contentlet()).when(contentletAPIImpl).
                    findContentletByIdentifierAnyLanguage(commentsContentlet.getIdentifier(), true);

            Mockito.doReturn(new Contentlet()).when(contentletAPIImpl)
                    .findContentletByIdentifierAnyLanguage(newsContentlet.getIdentifier(), true);

            //Pull related content from comment child
            List<Contentlet> result = contentletAPIImpl
                    .filterRelatedContent(commentsContentlet, relationship, user, false, false, -1,
                            -1);

            assertNotNull(result);
            assertEquals(0,result.size());

            //pulling content from parent
            result = contentletAPIImpl
                    .filterRelatedContent(newsContentlet, relationship, user, false, true, -1,
                            -1);

            assertNotNull(result);
            assertEquals(0,result.size());

        } finally {
            if (newsContentlet != null && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (commentsContentlet != null && UtilMethods.isSet(commentsContentlet.getInode())) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#getRelatedContent(Contentlet, Relationship, User, boolean)}
     * Given Scenario: A child is related to an archived parent. Only applies for legacy relationships
     * ExpectedResult: The method should return a contentlet list with the archived parent
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGetArchivedRelatedParent()
            throws DotDataException, DotSecurityException {

        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen
                    .languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News")
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    map(relationship, list(commentsContentlet)),
                    null, user, false);

            //archive parent
            contentletAPI.archive(newsContentlet, user, false);

            CacheLocator.getContentletCache().remove(commentsContentlet);
            CacheLocator.getContentletCache().remove(newsContentlet);

            //Pull related content from comment child
            List<Contentlet> result = contentletAPI
                    .getRelatedContent(commentsContentlet, relationship, user, false);

            assertNotNull(result);
            assertEquals(1,result.size());
            assertEquals(newsContentlet.getIdentifier(), result.get(0).getIdentifier());

        } finally {
            if (newsContentlet != null && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (commentsContentlet != null && UtilMethods.isSet(commentsContentlet.getInode())) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#getRelatedContent(Contentlet, Relationship, User, boolean)}
     * Given Scenario: A parent is related to an archived child. Only applies for legacy relationships
     * ExpectedResult: The method should return a contentlet list with the archived child
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGetArchivedRelatedChild()
            throws DotDataException, DotSecurityException {

        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen
                    .languageId(languageAPI.getDefaultLanguage().getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News")
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    map(relationship, list(commentsContentlet)),
                    null, user, false);

            //archive parent
            contentletAPI.archive(commentsContentlet, user, false);

            CacheLocator.getContentletCache().remove(commentsContentlet);
            CacheLocator.getContentletCache().remove(newsContentlet);

            //Pull related content from comment child
            List<Contentlet> result = contentletAPI
                    .getRelatedContent(newsContentlet, relationship, user, false);

            assertNotNull(result);
            assertEquals(1,result.size());
            assertEquals(commentsContentlet.getIdentifier(), result.get(0).getIdentifier());

        } finally {
            if (newsContentlet != null && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (commentsContentlet != null && UtilMethods.isSet(commentsContentlet.getInode())) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    private void addPermission(
            final Role role,
            final Permissionable contentType,
            final PermissionLevel permissionLevel)

            throws DotDataException, DotSecurityException {

        APILocator.getPermissionAPI().save(
                getPermission(role, contentType, permissionLevel.getType()),
                contentType, APILocator.systemUser(), false);
    }

    @NotNull
    private Permission getPermission(
            final Role role,
            final Permissionable permissionable,
            final int permissionPublish) {

        final Permission publishPermission = new Permission();
        publishPermission.setInode(permissionable.getPermissionId());
        publishPermission.setRoleId(role.getId());
        publishPermission.setPermission(permissionPublish);
        return publishPermission;
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private static AcknowledgedResponse setClusterAsReadOnly(final boolean value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        final Settings.Builder settingBuilder = Settings.builder()
                .put("cluster.blocks.read_only", value);

        request.persistentSettings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
