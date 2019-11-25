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

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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

    @Test(expected= DotContentletStateException.class)
    public void testCheckInWithLegacyRelationshipsAndReadOnlyClusterShouldThrowAnException()
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType contentType = null;
        final ESContentletAPIImpl contentletAPIImpl = new ESContentletAPIImpl();

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

            final ESIndexAPI esIndexAPI = Mockito.mock(ESIndexAPI.class);

            contentletAPIImpl.setEsIndexAPI(esIndexAPI);

            Mockito.when(esIndexAPI.isClusterInReadOnlyMode()).thenReturn(true);

            contentletAPIImpl.checkin(contentlet, contentletRelationship, null, null, user, false);

        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }
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

            final ESIndexAPI esIndexAPI = Mockito.mock(ESIndexAPI.class);

            Mockito.when(esIndexAPI.isClusterInReadOnlyMode()).thenReturn(true);

            assertFalse(
                    new ESContentletAPIImpl().isCheckInSafe(contentletRelationship, esIndexAPI));

        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }
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

            final ESIndexAPI esIndexAPI = Mockito.mock(ESIndexAPI.class);

            Mockito.when(esIndexAPI.isClusterInReadOnlyMode()).thenReturn(true);

            assertTrue(
                    new ESContentletAPIImpl().isCheckInSafe(contentletRelationship, esIndexAPI));

        }finally{
            if (contentType != null && contentType.id() != null){
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * Test for isCheckInSafe method without relationships. It should return true no matter the cluster status
     */
    @Test
    public void testIsCheckInSafeWithoutRelationshipsShouldReturnTrue() {
        final ESIndexAPI esIndexAPI = Mockito.mock(ESIndexAPI.class);

        Mockito.when(esIndexAPI.isClusterInReadOnlyMode()).thenReturn(true);

        assertTrue(
                new ESContentletAPIImpl().isCheckInSafe(null, esIndexAPI));
    }


    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

}
