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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
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
        final ElasticReadOnlyCommand esReadOnlyMonitor = mock(ElasticReadOnlyCommand.class);
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

            contentlet.setProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPIImpl.checkin(contentlet, contentletRelationship, null, null, user, false);

            throw new  AssertionError("DotContentletStateException Expected");
        } catch(DotContentletStateException e) {
            verify(esReadOnlyMonitor).executeCheck();
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

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When: You have a {@link ContentType} with a {@link BinaryField}, First save it, and later Update it
     * with a different file
     * Should: Save contentlet with the right file's path and should copy the file to
     * <pre>
     *     [Abdolute asset root path]/[inode first character]/[inode second character]/[inode]/[field variable name]/[file_name]
     * </pre>
     *
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void saveAndUpdateContentletWithBinaryField() throws IOException, DotDataException, DotSecurityException {
        final Field binaryField = new FieldDataGen()
                .name("binary")
                .velocityVarName("binary")
                .type(BinaryField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .fields(list(binaryField))
                .nextPersisted();

        final File testFile = createFile("images/test.jpg", ".jpg");
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty(binaryField.variable(), testFile)
                .next();

        final ESContentletAPIImpl esContentletAPI = new ESContentletAPIImpl();
        esContentletAPI.checkin(contentlet, APILocator.systemUser(), false);

        Contentlet contentletFromDataBase = APILocator.getContentletAPI()
                .find(contentlet.getInode(), APILocator.systemUser(), false);

        assertFiles(binaryField, testFile, contentletFromDataBase);

        final File testFile_2 = createFile("images/test.png", "v");

        final Contentlet checkout = ContentletDataGen.checkout(contentletFromDataBase);
        checkout.setProperty(binaryField.variable(), testFile_2);

        esContentletAPI.checkin(checkout, APILocator.systemUser(), false);

        Contentlet contentletFromDataBase_2 = APILocator.getContentletAPI()
                .find(checkout.getInode(), APILocator.systemUser(), false);

        assertFiles(binaryField, testFile_2, contentletFromDataBase_2);
    }

    private void assertFiles(Field binaryField, File testFile, Contentlet contentletFromDataBase)
            throws IOException {
        final String inode = contentletFromDataBase.getInode();
        File newDir = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + inode.charAt(0)
                + File.separator
                + inode.charAt(1) + File.separator + inode);

        final String expectedPath =
                newDir.getAbsolutePath() + File.separator + binaryField.variable()
                        + File.separator + testFile.getName();
        assertEquals(expectedPath,
                contentletFromDataBase.getStringProperty(binaryField.variable()));

        final File newFile = new File(expectedPath);
        FileTestUtil.compare(testFile, newFile);
    }

    private static File createFile(final String path, final String suffix) throws IOException {
        final File originalFile = new File(Thread.currentThread()
                .getContextClassLoader().getResource(path).getFile());

        final String fileName = "test_" + System.currentTimeMillis() + suffix;
        final File testFile = new File(Files.createTempDir(), fileName);
        FileUtil.copyFile(originalFile, testFile);

        return testFile;
    }

    private void checkLock(final User user, final Contentlet contentletSaved) throws DotDataException {
        final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI().
                getContentletVersionInfo(contentletSaved.getIdentifier(), contentletSaved.getLanguageId());

        assertTrue(info.isPresent());
        assertNotNull(info.get().getLockedBy());
        assertNotNull(info.get().getLockedOn());
        assertEquals(user.getUserId(), info.get().getLockedBy());
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


    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * Given Scenario: Try to check-in a version of an archived page with {@link Contentlet#DONT_VALIDATE_ME} flag on
     * ExpectedResult: The method should not throw a {@link NullPointerException}, but a {@link DotDataException} because
     * we are trying to check-in a version of an archived content
     */
    @Test
    public void testCheckInArchivedPageShouldThrowDotDataException() {
        final Contentlet contentlet = TestDataUtils
                .getPageContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId());
        ContentletDataGen.archive(contentlet);

        final Contentlet newVersion = ContentletDataGen.checkout(contentlet);
        newVersion.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
        try {
            ContentletDataGen.checkin(newVersion);
            fail();
        } catch (Exception e){
            if (!(e.getCause() instanceof DotDataException)){
                fail();
            }
        }

    }

    /**
     * Method to test: {@link ESContentletAPIImpl#move(Contentlet, User, Host, Folder, boolean)}
     * Given Scenario: sends a null host and folder path
     * ExpectedResult: The method should not throw a {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_move_invalid_null_host() throws DotDataException, DotSecurityException {

        APILocator.getContentletAPI()
                .move(new Contentlet(), APILocator.systemUser(), null,false);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#move(Contentlet, User, Host, Folder, boolean)}
     * Given Scenario: sends a not null host and folder path, but invalid b/c does not starts with //
     * ExpectedResult: The method should not throw a {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_move_invalid_start_host() throws DotDataException, DotSecurityException {

        APILocator.getContentletAPI()
                .move(new Contentlet(), APILocator.systemUser(), "demo.dotcms.com/application",false);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#move(Contentlet, User, Host, Folder, boolean)}
     * Given Scenario: sends a not null host and folder path, but invalid b/c does not starts with //
     * ExpectedResult: The method should not throw a {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_move_invalid_path() throws DotDataException, DotSecurityException {

        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        APILocator.getContentletAPI() // no path
                .move(new Contentlet(), APILocator.systemUser(), host.getHostname(),false);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#move(Contentlet, User, Host, Folder, boolean)}
     * Given Scenario: sends a not null host and folder path, but invalid b/c does not starts with //
     * ExpectedResult: The method should not throw a {@link IllegalArgumentException}
     */
    @Test(expected = DoesNotExistException.class)
    public void test_move_not_exists_path() throws DotDataException, DotSecurityException {

        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final String unknownFolderPath = "//" + host.getHostname() + "/unknownFolder";
        APILocator.getContentletAPI()
                .move(new Contentlet(), APILocator.systemUser(), unknownFolderPath,false);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#move(Contentlet, User, Host, Folder, boolean)}
     * Given Scenario: sends a not null host and folder path, but invalid b/c does not starts with //
     * ExpectedResult: The method should not throw a {@link IllegalArgumentException}
     */
    @Test(expected = DotSecurityException.class)
    public void test_move_to_exists_path_invalid_user() throws DotDataException, DotSecurityException {

        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        Contentlet contentlet = null;
        final ContentType news = getNewsLikeContentType("News");

        final ContentletDataGen dataGen = new ContentletDataGen(news.id());

        contentlet = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                .setProperty("title", "News Test")
                .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                .next();

        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet = contentletAPI.checkin(contentlet, user, false);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        APILocator.getContentletAPI()
                .move(contentlet, null, host, folder,false);
    }


    /**
     * Method to test: {@link ESContentletAPIImpl#copyProperties(Contentlet, Map)}<br>
     * Given Scenario: {@link Long} properties set as {@link BigDecimal} are copied to the {@link Contentlet} object <br>
     * ExpectedResult: should success
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testCopyProperties() throws DotSecurityException {
        final Contentlet contentlet = TestDataUtils
                .getPageContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final Map<String, Object> propertiesToCopy = new HashMap<>();
        propertiesToCopy.put(Contentlet.LANGUAGEID_KEY, new BigDecimal(1));
        propertiesToCopy.put(Contentlet.SORT_ORDER_KEY, new BigDecimal(2));
        contentletAPI.copyProperties(contentlet, propertiesToCopy);

        assertEquals(1, contentlet.getLanguageId());
        assertEquals(2, contentlet.getSortOrder());
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

    /**
     * Method to Test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create a Content Type with a MANY_TO_MANY relationship to itself
     * - Create tree contents: A, B, C
     * - Related content A with B and C
     * - Related content B with A and C
     * - Related content C with A and B
     * Should: All the related content should be saved right
     */
    @Test
    public void selfRelatedContents() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(contentType)
                .parent(contentType)
                .nextPersisted();

        Contentlet contentletA = new ContentletDataGen(contentType)
                .host(host)
                .nextPersisted();

        Contentlet contentletB = new ContentletDataGen(contentType)
                .host(host)
                .nextPersisted();

        Contentlet contentletC = new ContentletDataGen(contentType)
                .host(host)
                .nextPersisted();

        contentletA = relateContent(relationship, contentletA, contentletB, contentletC);
        contentletB = relateContent(relationship, contentletB, contentletA, contentletC);
        contentletC = relateContent(relationship, contentletC, contentletA, contentletB);

        contentletA.setProperty(relationship.getChildRelationName(), Arrays.asList(contentletB, contentletC));

        contentletA = ContentletDataGen.checkout(contentletA);
        ContentletDataGen.checkin(contentletA);

        assertRelatedContents(relationship, contentletA, contentletB, contentletC);
        assertRelatedContents(relationship, contentletB, contentletA, contentletC);
        assertRelatedContents(relationship, contentletC, contentletA, contentletB);

    }

    /**
<<<<<<< HEAD
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a ContentType with a unique field
     * - Create two Contentlet with the same value in the unique field in the same host
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInTheSameHost() throws DotDataException, DotSecurityException {
        final Field uniqueTextField = new FieldDataGen()
                .unique(true)
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(uniqueTextField)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);

        try {
            APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);
            throw new AssertionError("DotRuntimeException Expected");
        }catch (DotRuntimeException e) {
            assertEquals(e.getMessage(), "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).");
        }
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a ContentType with a unique field
     * - Create two Contentlet with the same value in the unique field in the same host
     * - set the uniquePerSite properties to false
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInTheSameHostUniquePerSiteToFalse() throws DotDataException, DotSecurityException {

        final Boolean uniquePerSite = ESContentletAPIImpl.getUniquePerSite();
        ESContentletAPIImpl.setUniquePerSite(false);

        try {
            final Field uniqueTextField = new FieldDataGen()
                    .unique(true)
                    .type(TextField.class)
                    .next();

            final ContentType contentType = new ContentTypeDataGen()
                    .field(uniqueTextField)
                    .nextPersisted();

            final Host host = new SiteDataGen().nextPersisted();

            final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty(uniqueTextField.variable(), "unique-value")
                    .next();

            final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty(uniqueTextField.variable(), "unique-value")
                    .next();

            APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);

            try {
                APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);
                throw new AssertionError("DotRuntimeException Expected");
            } catch (DotRuntimeException e) {
                assertEquals(e.getMessage(),
                        "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).");
            }
        } finally {
            ESContentletAPIImpl.setUniquePerSite(uniquePerSite);
        }
    }


    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a {@link ContentType} with a unique field
     * - Create two  {@link Contentlet} with the same value in the unique field in different hosts
     *
     * Should: Save successfully the two {@link Contentlet}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInDifferentHost() throws DotDataException, DotSecurityException {
        final Field uniqueTextField = new FieldDataGen()
                .unique(true)
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(uniqueTextField)
                .nextPersisted();

        final Host host1 = new SiteDataGen().nextPersisted();
        final Host host2 = new SiteDataGen().nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host1)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host2)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);

        final Optional<Contentlet> contentlet1FromDB = APILocator.getContentletAPI()
                .findInDb(contentlet_1.getInode());

        assertTrue(contentlet1FromDB.isPresent());

        final Optional<Contentlet> contentlet2FromDB = APILocator.getContentletAPI()
                .findInDb(contentlet_2.getInode());

        assertTrue(contentlet2FromDB.isPresent());

    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a {@link ContentType} with a unique field
     * - Create two  {@link Contentlet} with the same value in the unique field in different hosts
     * - set the uniquePerSite properties to false
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInDifferentHostUniquePerSiteToFalse() throws DotDataException, DotSecurityException {

        final Boolean uniquePerSite = ESContentletAPIImpl.getUniquePerSite();
        ESContentletAPIImpl.setUniquePerSite(false);

        try {


            final Field uniqueTextField = new FieldDataGen()
                    .unique(true)
                    .type(TextField.class)
                    .next();

            final ContentType contentType = new ContentTypeDataGen()
                    .field(uniqueTextField)
                    .nextPersisted();

            final Host host1 = new SiteDataGen().nextPersisted();
            final Host host2 = new SiteDataGen().nextPersisted();

            final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                    .host(host1)
                    .setProperty(uniqueTextField.variable(), "unique-value")
                    .next();

            final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                    .host(host2)
                    .setProperty(uniqueTextField.variable(), "unique-value")
                    .next();

            APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);

            try {
                APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);
                throw new AssertionError("DotRuntimeException Expected");
            } catch (DotRuntimeException e) {
                assertEquals(e.getMessage(),
                        "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).");
            }
        } finally {
            ESContentletAPIImpl.setUniquePerSite(uniquePerSite);

        }
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}
     * When:
     * - Create two sites
     * - Create a VanityURL to all sites to return 301
     * - Check that it return 301 for each sites
     * - Update the Vanity URL to just one of the sites created in the first steps
     *
     * Should:
     * - Return 301 for the Site in the Vanity URL
     * - Call the {@link FilterChain#doFilter(ServletRequest, ServletResponse)} for the site out of the Vanity URL
     *
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void flushVanityURLCacheRight() throws ServletException, IOException {
        final Host host_1 = new SiteDataGen().nextPersisted();
        final Host host_2 = new SiteDataGen().nextPersisted();

        final String vanityURI = "/my-test_" + System.currentTimeMillis();

        final DefaultVanityUrl vanityURL = (DefaultVanityUrl) new VanityUrlDataGen()
                .allSites()
                .uri(vanityURI)
                .action(HttpStatus.SC_MOVED_PERMANENTLY)
                .forwardTo("/test-url.html")
                .nextPersisted();

        ContentletDataGen.publish(vanityURL);

        checkFilter(host_1, vanityURL, HttpStatus.SC_MOVED_PERMANENTLY);
        checkFilter(host_2, vanityURL, HttpStatus.SC_MOVED_PERMANENTLY);

        final Contentlet checkout = ContentletDataGen.checkout(vanityURL);
        checkout.setHost(host_1.getIdentifier());
        checkout.setProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, "/test-url_2.html");

        ContentletDataGen.checkin(checkout);

        checkout.setProperty(Contentlet.TO_BE_PUBLISH, true);
        ContentletDataGen.publish(checkout);

        final VanityUrl vanityUrlUpdated = APILocator.getVanityUrlAPI().fromContentlet(checkout);
        checkFilter(host_1, vanityUrlUpdated, HttpStatus.SC_MOVED_PERMANENTLY);
        checkFilter(host_2, vanityUrlUpdated, -1);
    }

    private void checkFilter(final Host host, final VanityUrl vanityURL, final int statusExpected)
            throws IOException, ServletException {

        final VanityURLFilter vanityURLFilter = new VanityURLFilter();

        final HttpServletRequest req = createMockRequest(host, vanityURL.getURI());
        final HttpServletResponse res = new MockHttpStatusAndHeadersResponse(
                mock(HttpServletResponse.class));
        final FilterChain filterChain = mock(FilterChain.class);

        vanityURLFilter.doFilter(req, res, filterChain);

        if (statusExpected != -1) {
            checkResponse(vanityURL, statusExpected, res);
        } else {
            verify(filterChain).doFilter(req, res);
        }

    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a {@link ContentType} with a unique field
     * - Create two  {@link Contentlet} with the same value in the unique field in different hosts, but one of the contentlet not have the host set
     *
     * Should: Save successfully the two {@link Contentlet}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInDifferentHostUsingContentTypeHost() throws DotDataException, DotSecurityException {
        final Field uniqueTextField = new FieldDataGen()
                .unique(true)
                .type(TextField.class)
                .next();

        final Host host1 = new SiteDataGen().nextPersisted();
        final Host host2 = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host2)
                .field(uniqueTextField)
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host1)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host2)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        contentlet_2.setHost(null);
        contentlet_2.setFolder(null);

        APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);

        final Optional<Contentlet> contentlet1FromDB = APILocator.getContentletAPI()
                .findInDb(contentlet_1.getInode());

        assertTrue(contentlet1FromDB.isPresent());

        final Optional<Contentlet> contentlet2FromDB = APILocator.getContentletAPI()
                .findInDb(contentlet_2.getInode());

        assertTrue(contentlet2FromDB.isPresent());

    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a ContentType with a unique field
     * - Create two Contentlet with the same value in the unique field in the same host, but one of the contentlet not have the host set
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInTheSameHostTakingContentTypeHost() throws DotDataException, DotSecurityException {
        final Field uniqueTextField = new FieldDataGen()
                .unique(true)
                .type(TextField.class)
                .next();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .field(uniqueTextField)
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(uniqueTextField.variable(), "unique-value")
                .next();

        contentlet_2.setHost(null);
        contentlet_2.setFolder(null);

        APILocator.getContentletAPI().checkin(contentlet_1, APILocator.systemUser(), false);

        try {
            APILocator.getContentletAPI().checkin(contentlet_2, APILocator.systemUser(), false);
            throw new AssertionError("DotRuntimeException Expected");
        } catch (DotRuntimeException e) {
            assertEquals(e.getMessage(),
                    "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).");
        }
    }

    @NotNull
    private HttpServletRequest createMockRequest(Host host_1, String vanityURI) {
        final HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(vanityURI);
        when(req.getParameter("host_id")).thenReturn(host_1.getIdentifier());
        return req;
    }

    private void checkResponse(final VanityUrl vanityURL, final int statusExpected, final HttpServletResponse res) {
        assertEquals(statusExpected, res.getStatus());
        assertEquals(vanityURL.getForwardTo(), res.getHeader("Location"));
        assertEquals(vanityURL.getIdentifier(), res.getHeader("X-DOT-VanityUrl"));
    }

    private void assertRelatedContents(Relationship relationship, Contentlet contentletParent,
            Contentlet... contentsRelated) throws DotDataException {
        final List<String> contentlets = relationshipAPI
                .dbRelatedContent(relationship, contentletParent, true)
                .stream()
                .map(contentlet -> contentlet.getIdentifier())
                .collect(Collectors.toList());

        assertEquals(contentsRelated.length, contentlets.size());

        for (Contentlet contentletRelated : contentsRelated) {
            assertTrue(contentlets.contains(contentletRelated.getIdentifier()));
        }
    }

    private Contentlet relateContent(
            Relationship relationship,
            Contentlet parentContent,
            Contentlet... contentletChilds) {
        final Contentlet parentCheckout = ContentletDataGen.checkout(parentContent);
        parentCheckout.setProperty(relationship.getChildRelationName(), Arrays.asList(contentletChilds));
        return  ContentletDataGen.checkin(parentCheckout);
    }
}
