package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.datagen.TestDataUtils.getCommentsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.relateContentTypes;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.ContentletTransformer;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
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
import java.util.Calendar;
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
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a unique {@link TextField}, with the {@link ESContentletAPIImpl#UNIQUE_PER_SITE_FIELD_VARIABLE_NAME}
     * {@link com.dotcms.contenttype.model.field.FieldVariable} set to true
     * - Create a ContentType and add the previous created field to it
     * - Create two {@link Contentlet} with the same value in the unique field in the same host
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInTheSameHost() throws DotDataException, DotSecurityException {

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                 .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
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
            final String expectedMessage = String.format("Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).\n"
                    + "List of non valid fields\n"
                    + "UNIQUE: %s/%s\n\n", uniqueTextField.variable(), uniqueTextField.name());

            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     *
     * - Create a unique {@link TextField}, with the {@link ESContentletAPIImpl#UNIQUE_PER_SITE_FIELD_VARIABLE_NAME}
     * {@link com.dotcms.contenttype.model.field.FieldVariable} set to false
     * - Create a ContentType and add the previous created field to it
     * - Create two {@link Contentlet} with the same value in the unique field in the same host
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInTheSameHostUniquePerSiteToFalse() throws DotDataException, DotSecurityException {

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
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
            final String expectedMessage = String.format("Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).\n"
                    + "List of non valid fields\n"
                    + "UNIQUE: %s/%s\n\n", uniqueTextField.variable(), uniqueTextField.name());

            assertEquals(expectedMessage, e.getMessage());
        }
    }


    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)}  }
     * When:
     * - Create a unique {@link TextField}, with the {@link ESContentletAPIImpl#UNIQUE_PER_SITE_FIELD_VARIABLE_NAME}
     * {@link com.dotcms.contenttype.model.field.FieldVariable} set to true
     * - Create a ContentType and add the previous created field to it
     * - Create two {@link Contentlet} with the same value in the unique field in the different host
     *
     * Should: Save successfully the two {@link Contentlet}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInDifferentHost() throws DotDataException, DotSecurityException {
        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
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
     *
     * - Create a unique {@link TextField}, with the {@link ESContentletAPIImpl#UNIQUE_PER_SITE_FIELD_VARIABLE_NAME}
     * {@link com.dotcms.contenttype.model.field.FieldVariable} set to false
     * - Create a ContentType and add the previous created field to it
     * - Create two {@link Contentlet} with the same value in the unique field in the different host
     *
     * Should: Throw a RuntimeException with the message: "Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s)."
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingFieldWithUniqueFieldInDifferentHostUniquePerSiteToFalse() throws DotDataException, DotSecurityException {

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("false")
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
            final String expectedMessage = String.format("Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).\n"
                    + "List of non valid fields\n"
                    + "UNIQUE: %s/%s\n\n", uniqueTextField.variable(), uniqueTextField.name());

            assertEquals(expectedMessage, e.getMessage());
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

        checkout.setProperty(Contentlet.TO_BE_PUBLISH, true);
        ContentletDataGen.checkin(checkout);
        ContentletDataGen.publish(checkout);

        final VanityUrl vanityUrlUpdated = APILocator.getVanityUrlAPI().fromContentlet(checkout);
        checkFilter(host_1, vanityUrlUpdated, HttpStatus.SC_MOVED_PERMANENTLY);
        checkFilter(host_2, vanityUrlUpdated, -1);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create a ContentType with publish date
     * - Create two Language
     * - Create a {@link Contentlet} with the first language and publish date for tomorrow
     * - Create a {@link Contentlet} with the second language and publish date for after tomorrow
     * Should: Each {@link Contentlet} be saved and index with the right publish date
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void differentVersionWithDifferentPublishDate() throws DotDataException, DotSecurityException {

        final Language language1 = new LanguageDataGen().nextPersisted();
        final Language language2 = new LanguageDataGen().nextPersisted();


        final Field publishField = new FieldDataGen().defaultValue(null)
                .type(DateTimeField.class).next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(publishField)
                .publishDateFieldVarName(publishField.variable())
                .nextPersisted();

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;

        try {

            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            final Date tomorrow = calendar.getTime();

            calendar.add(Calendar.DATE, 1);
            final Date afterTomorrow = calendar.getTime();

            contentlet1 = new ContentletDataGen(contentType)
                    .setProperty(publishField.variable(), tomorrow)
                    .languageId(language1.getId())
                    .nextPersisted();

            contentlet2 = ContentletDataGen.checkout(contentlet1);
            contentlet2.setLanguageId(language2.getId());
            contentlet2.setProperty(publishField.variable(), afterTomorrow);
            ContentletDataGen.checkin(contentlet2);

            checkFromElasticSearch(publishField, tomorrow, afterTomorrow, contentlet1);
            checkFromDataBase(publishField, tomorrow, afterTomorrow, contentlet1);
        }finally {
            LanguageDataGen.remove(language1);
            LanguageDataGen.remove(language2);
            
            ContentTypeDataGen.remove(contentType);

            ContentletDataGen.remove(contentlet1);
            ContentletDataGen.remove(contentlet2);
        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Set uniquePublishExpireDate to true
     * - Create a ContentType with publish date
     * - Create two Language
     * - Create a {@link Contentlet} with the first language and publish date for tomorrow
     * - Create a {@link Contentlet} with the second language and publish date for after tomorrow
     * Should: Both {@link Contentlet} have after tomorrow as publish date
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void differentVersionWithDifferentPublishDateAndUniquePublishExpireDate() throws DotDataException, DotSecurityException {
        final boolean uniquePublishExpireDate = ContentletTransformer.isUniquePublishExpireDatePerLanguages();
        ContentletTransformer.setUniquePublishExpireDatePerLanguages(true);

        try {
            final Language language1 = new LanguageDataGen().nextPersisted();
            final Language language2 = new LanguageDataGen().nextPersisted();

            final Field publishField = new FieldDataGen().defaultValue(null)
                    .type(DateTimeField.class).next();

            final ContentType contentType = new ContentTypeDataGen()
                    .field(publishField)
                    .publishDateFieldVarName(publishField.variable())
                    .nextPersisted();

            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            final Date tomorrow = calendar.getTime();

            calendar.add(Calendar.DATE, 1);
            final Date afterTomorrow = calendar.getTime();

            final Contentlet contentlet1 = new ContentletDataGen(contentType)
                    .setProperty(publishField.variable(), tomorrow)
                    .languageId(language1.getId())
                    .nextPersisted();

            final Contentlet contentlet2 = ContentletDataGen.checkout(contentlet1);
            contentlet2.setLanguageId(language2.getId());
            contentlet2.setProperty(publishField.variable(), afterTomorrow);
            ContentletDataGen.checkin(contentlet2);

            checkFromElasticSearch(publishField, afterTomorrow, afterTomorrow, contentlet1);
            checkFromDataBase(publishField, afterTomorrow, afterTomorrow, contentlet1);
        }finally {
            ContentletTransformer.setUniquePublishExpireDatePerLanguages(uniquePublishExpireDate);
        }
    }

    /**
     * This method check the contentlet versions from the Data Base, step by step it does the follow:
     *
     * - Get all the version to the contentlet from DataBase
     * - Throw an {@link AssertionError} if the contenlet has not two versions
     * - Check the value for  <code>field</code>, if the value is different to date1 or date2 throw a {@link AssertionError}
     */
    private void checkFromDataBase(Field field, Date date1, Date date2,
            Contentlet contentlet1) throws DotDataException, DotSecurityException {
        final List<Versionable> allVersions = APILocator.getVersionableAPI()
                .findAllVersions(contentlet1.getIdentifier());

        assertEquals(2, allVersions.size());
        for (Versionable versionable : allVersions) {
            if (versionable.getInode().equals(contentlet1.getInode())) {
                assertEquals(date1,
                        ((Contentlet) versionable).getDateProperty(field.variable()));
            } else {
                assertEquals(date2,
                        ((Contentlet) versionable).getDateProperty(field.variable()));
            }
        }
    }

    /**
     * This method check the contentlet versions from the Elasticsearch, step by step it does the follow:
     *
     * - Get all the version to the contentlet from DataBase
     * - Throw an {@link AssertionError} if the contenlet has not two versions
     * - Check the value for  <code>field</code>, if the value is different to date1 or date2 throw a {@link AssertionError}
     */
    private void checkFromElasticSearch(final Field field, final Date date1, final Date date2,
            final Contentlet contentletToCheck) throws DotDataException, DotSecurityException {

        final List<Contentlet> search = APILocator.getContentletAPI()
                .search("+identifier:" + contentletToCheck.getIdentifier(), 0, 0, null,
                        APILocator.systemUser(), false);

        assertEquals(2, search.size());
        for (Contentlet contentlet : search) {
            if (contentlet.getInode().equals(contentletToCheck.getInode())) {
                assertEquals(date1, contentlet.getDateProperty(field.variable()));
            } else {
                assertEquals(date2, contentlet.getDateProperty(field.variable()));
            }
        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Set uniquePublishExpireDate to true
     * - Create a ContentType with expire date
     * - Create two Language
     * - Create a {@link Contentlet} with the first language and expire date for tomorrow
     * - Create a {@link Contentlet} with the second language and expire date for after tomorrow
     * Should: Both {@link Contentlet} have after tomorrow as expire date
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void differentVersionWithDifferentExpireDateAndUniquePublishExpireDat() throws DotDataException, DotSecurityException {

        final boolean uniquePublishExpireDate = ContentletTransformer.isUniquePublishExpireDatePerLanguages();
        ContentletTransformer.setUniquePublishExpireDatePerLanguages(true);

        try {
            final Language language1 = new LanguageDataGen().nextPersisted();
            final Language language2 = new LanguageDataGen().nextPersisted();

            final Field expireField = new FieldDataGen().defaultValue(null).type(DateTimeField.class).next();

            final ContentType contentType = new ContentTypeDataGen()
                    .field(expireField)
                    .expireDateFieldVarName(expireField.variable())
                    .nextPersisted();

            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            final Date tomorrow = calendar.getTime();

            calendar.add(Calendar.DATE, 1);
            final Date afterTomorrow = calendar.getTime();

            final Contentlet contentlet1 = new ContentletDataGen(contentType)
                    .setProperty(expireField.variable(), tomorrow)
                    .languageId(language1.getId())
                    .nextPersisted();

            final Contentlet contentlet2 = ContentletDataGen.checkout(contentlet1);
            contentlet2.setLanguageId(language2.getId());
            contentlet2.setProperty(expireField.variable(), afterTomorrow);
            ContentletDataGen.checkin(contentlet2);

            checkFromElasticSearch(expireField, afterTomorrow, afterTomorrow, contentlet1);
            checkFromDataBase(expireField, afterTomorrow, afterTomorrow, contentlet1);
        }finally {
            ContentletTransformer.setUniquePublishExpireDatePerLanguages(uniquePublishExpireDate);
        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create a ContentType with expire date
     * - Create two Language
     * - Create a {@link Contentlet} with the first language and expire date for tomorrow
     * - Create a {@link Contentlet} with the second language and expire date for after tomorrow
     * Should: Each {@link Contentlet} be saved and index with the right expire date
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void differentVersionWithDifferentExpireDate() throws DotDataException, DotSecurityException {

        final Language language1 = new LanguageDataGen().nextPersisted();
        final Language language2 = new LanguageDataGen().nextPersisted();

        final Field expireField = new FieldDataGen().defaultValue(null).type(DateTimeField.class).next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(expireField)
                .expireDateFieldVarName(expireField.variable())
                .nextPersisted();

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        final Date tomorrow = calendar.getTime();

        calendar.add(Calendar.DATE, 1);
        final Date afterTomorrow = calendar.getTime();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(expireField.variable(), tomorrow)
                .languageId(language1.getId())
                .nextPersisted();

        final Contentlet contentlet2 = ContentletDataGen.checkout(contentlet1);
        contentlet2.setLanguageId(language2.getId());
        contentlet2.setProperty(expireField.variable(), afterTomorrow);
        ContentletDataGen.checkin(contentlet2);

        checkFromElasticSearch(expireField, tomorrow, afterTomorrow, contentlet1);

        checkFromDataBase(expireField, tomorrow, afterTomorrow, contentlet1);
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
        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
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
     * - Create two {@link Contentlet} with the same value in the unique field in the same host, but one of the contentlet not have the host set
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
            final String expectedMessage = String.format("Contentlet with id:`Unknown/New` and title:`` has invalid / missing field(s).\n"
                    + "List of non valid fields\n"
                    + "UNIQUE: %s/%s\n\n", uniqueTextField.variable(), uniqueTextField.name());

            assertEquals(expectedMessage, e.getMessage());
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

    /**
     * Method to test: {@link ESContentletAPIImpl#findContentletByIdentifierAnyLanguage(String)}
     * When: The contentlet had just one version not in the DEFAULT variant
     * Should: return {@link Optional#empty()}
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findContentletByIdentifierAnyLanguageNoDefaultVersion() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();

        final Contentlet contentletByIdentifierAnyLanguage = APILocator.getContentletAPI()
               .findContentletByIdentifierAnyLanguage(contentlet.getIdentifier());

        assertNull(contentletByIdentifierAnyLanguage);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#findContentletByIdentifierAnyLanguageAnyVariant(String)} (String)}
     * When: The contentlet had just one version not in the DEFAULT variant
     * Should: return the {@link Contentlet} anyway
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findContentletByIdentifierAnyLanguageAndVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();

        final Contentlet contentletByIdentifierAnyLanguage = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguageAnyVariant(contentlet.getIdentifier());

        assertNotNull(contentletByIdentifierAnyLanguage);
        assertEquals(contentlet.getIdentifier(), contentletByIdentifierAnyLanguage.getIdentifier());
        assertEquals(contentlet.getInode(), contentletByIdentifierAnyLanguage.getInode());
    }


    /**
     * Method to test: {@link ESContentletAPIImpl#findContentletByIdentifierAnyLanguage(String)}
     * When: The contentlet had just one version not in the DEFAULT variant but it was archived
     * Should: return {@link Optional#empty()}
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findDeletedContentletByIdentifierAnyLanguage()
            throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();

        final String identifier = contentlet.getIdentifier();
        APILocator.getContentletAPI().archive(contentlet, APILocator.systemUser(), false);

        Contentlet contentletByIdentifierAnyLanguage = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(identifier);

        assertNull(contentletByIdentifierAnyLanguage);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create two {@link Language}: A and B
     * - Create two {@link Template}: A and B
     * - Create a {@link HTMLPageAsset} with {@link Language} A and {@link Template} A.
     * - Create a new version of the same {@link HTMLPageAsset} but with {@link Language} B and {@link Template} B.
     * Should: Create a new version in the first {@link Language} using the {@link Template} from the second language version.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void shouldUpdateAllTheDifferentLangVersions()  {
        final Language language_A = new LanguageDataGen().nextPersisted();
        final Language language_B = new LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template_A = new TemplateDataGen().host(host).nextPersisted();
        final Template template_B = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = createHtmlPageAsset(VariantAPI.DEFAULT_VARIANT, language_A, host, template_A);
        final HTMLPageAsset htmlPageAsset_2 = createNewVersion(htmlPageAsset_1, VariantAPI.DEFAULT_VARIANT, language_B, template_B);

        assertEquals(template_B.getIdentifier(), getLastWorkingVersion(htmlPageAsset_1).getTemplateId());
        assertEquals(template_B.getIdentifier(), getLastWorkingVersion(htmlPageAsset_2).getTemplateId());
    }


    private HTMLPageAsset getLastWorkingVersion(final HTMLPageAsset htmlPageAsset_1) {
        final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(htmlPageAsset_1.getIdentifier(),
                        htmlPageAsset_1.getLanguageId(),
                        VariantAPI.DEFAULT_VARIANT.name())
                .orElseThrow(() -> new AssertionError());
        final HTMLPageAsset htmlPageAssetFromDataBase = getFromDataBase(
                contentletVersionInfo.getWorkingInode());
        return htmlPageAssetFromDataBase;
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create two {@link Variant}: A and B
     * - Create two {@link Template}: A and B.
     * - Create a {@link HTMLPageAsset} using {@link Variant} A and the {@link Template} A.
     * - Create a new version of the same {@link HTMLPageAsset} but with the {@link Variant} B
     * and {@link Template} B.
     * Should: Each Page's Version has its own {@link Template}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void shouldUpdateAllTheDifferentVariantsVersions() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template_A = new TemplateDataGen().host(host).nextPersisted();
        final Template template_B = new TemplateDataGen().host(host).nextPersisted();

        final Variant variant_A = new VariantDataGen().nextPersisted();
        final Variant variant_B = new VariantDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = (HTMLPageAsset) new HTMLPageDataGen(host, template_A)
                .variant(variant_A)
                .nextPersisted();

        final Contentlet checkout = HTMLPageDataGen.checkout(htmlPageAsset_1);
        checkout.setVariantId(variant_B.name());
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, template_B.getIdentifier());
        final HTMLPageAsset htmlPageAsset_2 = APILocator.getHTMLPageAssetAPI().fromContentlet(
                HTMLPageDataGen.checkin(checkout));

        assertEquals(template_A.getIdentifier(), getFromDataBase(htmlPageAsset_1.getInode()).getTemplateId());
        assertEquals(template_B.getIdentifier(), getFromDataBase(htmlPageAsset_2.getInode()).getTemplateId());

        final ContentletVersionInfo contentletVersionInfo = ((VersionableFactoryImpl) FactoryLocator.getVersionableFactory())
                .findContentletVersionInfoInDB(htmlPageAsset_1.getIdentifier(),
                        htmlPageAsset_1.getLanguageId(),
                        variant_A.name()).orElseThrow(() -> new AssertionError());

        assertEquals(htmlPageAsset_1.getInode(), contentletVersionInfo.getWorkingInode());
        assertEquals(template_A.getIdentifier(), getFromDataBase(contentletVersionInfo.getWorkingInode()).getTemplateId());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#checkin(Contentlet, User, boolean)}
     * When:
     * - Create two {@link Variant}: A and B
     * - Create two {@link Language}: A and B
     * - Create 3 {@link Template}: A, B and C
     * - Create a {@link HTMLPageAsset} in {@link Variant} 'A' and {@link Language} 'A' with {@link Template} 'A' (First Page's Version).
     * - Create a new version of the same {@link HTMLPageAsset} and {@link Language} 'A' but using the  {@link Variant} 'B'  with {@link Template} 'B'(Second Page's version).
     * Should: Both version have different {@link Template}, 'A' for the first page's version and 'B' for the second page's version.
     *
     * - Create a new version of the same {@link HTMLPageAsset} and {@link Variant} 'A' and {@link Language} B  with {@link Template} 'C' (Third Page's version).
     * Should: Create a new version for {@link Variant} 'A' and {@link Language} 'A'  with the {@link Template} 'C'
     * and keep the {@link Variant} 'B' and {@link Language} 'A' version with the {@link Template} B.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void shouldUpdateAllTheDifferentVariantsVersionsInTheSameLang() throws DotDataException {
        final Variant variant_A = new VariantDataGen().nextPersisted();
        final Variant variant_B = new VariantDataGen().nextPersisted();

        final Language language_A = new LanguageDataGen().nextPersisted();
        final Language language_B = new LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template_A = new TemplateDataGen().host(host).nextPersisted();
        final Template template_B = new TemplateDataGen().host(host).nextPersisted();
        final Template template_c = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = createHtmlPageAsset(variant_A, language_A, host, template_A);
        final HTMLPageAsset htmlPageAsset_2 = createNewVersion(htmlPageAsset_1, variant_B, language_A, template_B);

        assertEquals(template_A.getIdentifier(), getFromDataBase(htmlPageAsset_1.getInode()).getTemplateId());
        assertEquals(template_B.getIdentifier(), getFromDataBase(htmlPageAsset_2.getInode()).getTemplateId());

        final HTMLPageAsset htmlPageAsset_3 = createNewVersion(htmlPageAsset_1, variant_A, language_B, template_c);

        final ContentletVersionInfo contentletVersionInfo_1 = APILocator.getVersionableAPI()
                .getContentletVersionInfo(htmlPageAsset_1.getIdentifier(),
                        language_A.getId(),
                        variant_A.name()).orElseThrow(() -> new AssertionError());

        assertEquals(template_c.getIdentifier(), getFromDataBase(contentletVersionInfo_1.getWorkingInode()).getTemplateId());
        assertEquals(template_c.getIdentifier(), getFromDataBase(htmlPageAsset_3.getInode()).getTemplateId());

        final ContentletVersionInfo contentletVersionInfo_2 = ((VersionableFactoryImpl) FactoryLocator.getVersionableFactory())
                .findContentletVersionInfoInDB(htmlPageAsset_1.getIdentifier(),
                        language_A.getId(),
                        variant_B.name()).orElseThrow(() -> new AssertionError());

        assertEquals(template_B.getIdentifier(), getFromDataBase(contentletVersionInfo_2.getWorkingInode()).getTemplateId());
    }

    @WrapInTransaction
    private HTMLPageAsset createNewVersion(final HTMLPageAsset htmlPageAsset,
            final Variant variant, final Language language, final Template template) {

        final Contentlet checkout = HTMLPageDataGen.checkout(htmlPageAsset);
        checkout.setLanguageId(language.getId());
        checkout.setVariantId(variant.name());
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, template.getIdentifier());
        return APILocator.getHTMLPageAssetAPI().fromContentlet(
                HTMLPageDataGen.checkin(checkout));
    }

    @WrapInTransaction
    private HTMLPageAsset createHtmlPageAsset(final Variant variant,
            final Language language, final Host host, final Template template) {
        return (HTMLPageAsset) new HTMLPageDataGen(host, template)
                .variant(variant)
                .languageId(language.getId())
                .nextPersisted();
    }

    private HTMLPageAsset getFromDataBase(final String inode) {

        final Contentlet contentlet = FactoryLocator.getContentletFactory()
                .findInDb(inode).orElseThrow(() -> new AssertionError("Contentlet should exists:" + inode));

        return APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
    }


    private Contentlet createNewLangVersionAndPublish(final Language language,
            final Contentlet contentlet) throws DotDataException, DotSecurityException {
        final Contentlet newLangVersion = createNewLangVersion(language, contentlet);
        return ContentletDataGen.publish(newLangVersion);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} into the
     * DEFAULT {@link Variant} and a specific {@link Variant}.
     * Should: return all the versions for the DEFAULT {@link Variant} and the specific {@link Variant}
     */
    @Test
    public void findAllVersionsByVariant() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1DefaultVariant = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersisted();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_2.getId());
        final Contentlet contentletLanguage2DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_3.getId());
        final Contentlet contentletLanguage3DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet contentletLang1SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage1DefaultVariant,
                variant, map());

        final Contentlet contentletLang2SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage2DefaultVariant,
                variant, map());

        final Contentlet contentletLang3SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage3DefaultVariant,
                variant, map());

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1DefaultVariant.getIdentifier());

        final List<Contentlet> contentlets = APILocator.getContentletAPI()
                .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT, APILocator.systemUser(), false);

        assertNotNull(contentlets);
        assertEquals(3, contentlets.size());

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage1DefaultVariant.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage2DefaultVariant.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage3DefaultVariant.getIdentifier())));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} and {@link Variant}
     * Also they have  old versions
     * Should: return all the versions even the old ones into the DEFAULT {@link Variant}
     */
    @Test
    public void findAllVersionsWithOldVersionsByVariant() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1Live = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang1 = createNewlyWorkingAndLiveVersion(
                contentletLanguage1Live);

        final Contentlet contentletLanguage2Live = createNewLangVersion(language_2, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang2 = createNewlyWorkingAndLiveVersion(
                contentletLanguage2Live);

        final Contentlet contentletLanguage3Live = createNewLangVersion(language_3, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang3 = createNewlyWorkingAndLiveVersion(
                contentletLanguage3Live);

        final Contentlet contentletLang1SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage1Live,
                variant, map());

        createNewlyWorkingAndLiveVersion(contentletLang1SpecificVariant);

        final Contentlet contentletLang2SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage2Live,
                variant, map());

        createNewlyWorkingAndLiveVersion(contentletLang2SpecificVariant);

        final Contentlet contentletLang3SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage3Live,
                variant, map());

        createNewlyWorkingAndLiveVersion(contentletLang3SpecificVariant);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1Live.getIdentifier());

        final List<Contentlet> contentlets = APILocator.getContentletAPI()
                .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT, APILocator.systemUser(), false);

        assertNotNull(contentlets);
        assertEquals(9, contentlets.size());

        final List<String> expectedInodes = list(
                contentletLanguage1Live,
                newlyWorkingAndLiveVersionLang1.get("WORKING"),
                newlyWorkingAndLiveVersionLang1.get("LIVE"),
                contentletLanguage2Live,
                newlyWorkingAndLiveVersionLang2.get("WORKING"),
                newlyWorkingAndLiveVersionLang2.get("LIVE"),
                contentletLanguage3Live,
                newlyWorkingAndLiveVersionLang3.get("WORKING"),
                newlyWorkingAndLiveVersionLang3.get("LIVE")
        ).stream().map(Contentlet::getInode).collect(Collectors.toList());

        expectedInodes.forEach(inode -> assertTrue(contentlets.stream()
                .anyMatch(contentlet -> contentlet.getInode().equals(inode))));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} but with a limit User,
     * and try to get all the versions using another user.
     * Should: throw a {@link DotSecurityException}
     */
    @Test
    public void findAllVersionsByVariantWithNoAllowUser()
            throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final User user_1 = new UserDataGen().nextPersisted();
        final User user_2 = new UserDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1DefaultVariant = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .user(user_1)
                .nextPersisted();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_2.getId());
        final Contentlet contentletLanguage2DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_3.getId());
        final Contentlet contentletLanguage3DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet contentletLang1SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage1DefaultVariant,
                variant, map());

        final Contentlet contentletLang2SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage2DefaultVariant,
                variant, map());

        final Contentlet contentletLang3SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage3DefaultVariant,
                variant, map());

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1DefaultVariant.getIdentifier());

        try {
            final List<Contentlet> contentlets = APILocator.getContentletAPI()
                    .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT, user_2, false);

            throw new AssertionError("Should throw a DotSecurityException");
        } catch (DotSecurityException e) {
            //Expected
        }
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: find for a contentlet that does not exist
     * Should: return a empty list
     */
    @Test
    public void findAllVersionsByVariantWithNoContentlet()
            throws DotDataException, DotSecurityException {
        final Identifier identifier = new Identifier();
        identifier.setId("fakeId");

        final List<Contentlet> contentlets = APILocator.getContentletAPI()
                .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT, APILocator.systemUser(), false);

        assertNotNull(contentlets);
        assertTrue(contentlets.isEmpty());
    }

    private static Map<String, Contentlet> createNewlyWorkingAndLiveVersion(final Contentlet contentletLanguage1Live) {
        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1Live);
        final Contentlet contentletWorking = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet newlyContentleLive = ContentletDataGen.publish(contentletWorking);
        contentlet1Checkout = ContentletDataGen.checkout(newlyContentleLive);
        final Contentlet contentletWorking2 = ContentletDataGen.checkin(contentlet1Checkout);

        return map("LIVE", newlyContentleLive, "WORKING", contentletWorking2);
    }

    private static Contentlet createNewLangVersion(final Language language,
            final Contentlet contentlet) throws DotDataException, DotSecurityException {
        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentlet);
        contentlet1Checkout.setLanguageId(language.getId());
        return ContentletDataGen.checkin(contentlet1Checkout);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#copyContentToVariant(Contentlet, String, User)}
     * When:
     * - Create a {@link Contentlet} in the DEFAULT Variant.
     * - Create a new {@link Variant}.
     * - Create a new Version of the newly created {@link Contentlet} into the newly {@link Variant}
     * - Save a new version os the {@link Contentlet} in the specific {@link Variant} version.
     * Should: Create  a copy from the specific {@link Variant} {@link Contentlet} Version to the DEFAULT {@link Variant}
     */
    @Test
    public void saveContentToSpecificVariant() throws DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        final Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .dataType(DataTypes.TEXT)
                .indexed(true)
                .next();

        final ContentType contentType = new ContentTypeDataGen().field(titleField).nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .setProperty("title", "Default Version")
                .nextPersisted();

        ContentletDataGen.createNewVersion(contentlet, variant, map("title", "Variant Version"));

        APILocator.getContentletAPI().copyContentToVariant(contentlet, variant.name(),
                APILocator.systemUser());

        final Contentlet contentletByIdentifierSpecificVariant = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        language.getId(), variant.name(), APILocator.systemUser(), false);

        assertNotNull(contentletByIdentifierSpecificVariant);

        assertEquals(contentlet.getIdentifier(), contentletByIdentifierSpecificVariant.getIdentifier());
        assertEquals(contentlet.getLanguageId(), contentletByIdentifierSpecificVariant.getLanguageId());
        assertEquals(variant.name(), contentletByIdentifierSpecificVariant.getVariantId());
        assertEquals("Default Version", contentletByIdentifierSpecificVariant.getStringProperty("title"));

        final Contentlet contentletByIdentifierDefaultVariant = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        language.getId(), VariantAPI.DEFAULT_VARIANT.name(),
                        APILocator.systemUser(), false);

        assertNotNull(contentletByIdentifierDefaultVariant);

        assertEquals(contentlet.getIdentifier(), contentletByIdentifierDefaultVariant.getIdentifier());
        assertEquals(contentlet.getLanguageId(), contentletByIdentifierDefaultVariant.getLanguageId());
        assertEquals(contentlet.getVariantId(), contentletByIdentifierDefaultVariant.getVariantId());
        assertEquals("Default Version", contentletByIdentifierSpecificVariant.getStringProperty("title"));
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#copyContentToVariant(Contentlet, String, User)}
     * When:
     * - Creata a {@link Variant} let call it variant_1
     * - Create a {@link Contentlet} with a version in variant_1, but not any version in DEFAULT Variant.
     * - Create another {@link Variant}, let call it variant_2.
     * - Save a new version os the {@link Contentlet} in variant_2.
     * Should: Create  a copy from the variant_1 {@link Contentlet} Version to the DEFAULT {@link Variant}
     */
    @Test
    public void saveContentToAnotherVariant() throws DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .dataType(DataTypes.TEXT)
                .indexed(true)
                .next();

        final ContentType contentType = new ContentTypeDataGen().field(titleField).nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .setProperty("title", "Variant 1 Version")
                .variant(variant_1)
                .nextPersisted();
        APILocator.getContentletAPI().copyContentToVariant(contentlet, variant_2.name(),
                APILocator.systemUser());

        final Contentlet contentletByIdentifierVariant1 = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        language.getId(), variant_1.name(), APILocator.systemUser(), false);

        assertNotNull(contentletByIdentifierVariant1);

        assertEquals(contentlet.getIdentifier(), contentletByIdentifierVariant1.getIdentifier());
        assertEquals(contentlet.getLanguageId(), contentletByIdentifierVariant1.getLanguageId());
        assertEquals(variant_1.name(), contentletByIdentifierVariant1.getVariantId());
        assertEquals("Variant 1 Version", contentletByIdentifierVariant1.getStringProperty("title"));

        final Contentlet contentletByIdentifierDefaultVariant = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        language.getId(), VariantAPI.DEFAULT_VARIANT.name(),
                        APILocator.systemUser(), false);

        assertNull(contentletByIdentifierDefaultVariant);

        final Contentlet contentletByIdentifierVariant2 = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        language.getId(), variant_2.name(), APILocator.systemUser(), false);

        assertNotNull(contentletByIdentifierVariant2);

        assertEquals(contentlet.getIdentifier(), contentletByIdentifierVariant2.getIdentifier());
        assertEquals(contentlet.getLanguageId(), contentletByIdentifierVariant2.getLanguageId());
        assertEquals(variant_2.name(), contentletByIdentifierVariant2.getVariantId());
        assertEquals("Variant 1 Version", contentletByIdentifierVariant2.getStringProperty("title"));
    }

}
