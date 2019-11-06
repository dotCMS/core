package com.dotcms.content.elasticsearch.business;

import static com.dotcms.datagen.TestDataUtils.getCommentsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.relateContentTypes;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ESContentletAPIImplTest extends IntegrationTestBase {

    private static ContentTypeAPI contentTypeAPI;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static ContentletAPI contentletAPI;
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


}
