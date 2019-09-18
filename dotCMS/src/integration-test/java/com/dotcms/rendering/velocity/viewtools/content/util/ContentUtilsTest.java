package com.dotcms.rendering.velocity.viewtools.content.util;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.LANGUAGE_TYPE;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.PUBLISH_TYPE;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ContentUtilsTest {

    private static User user;
    private static LanguageAPI languageAPI;

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static RelationshipAPI relationshipAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment.
        IntegrationTestInitService.getInstance().init();

        user = APILocator.getUserAPI().getSystemUser();
        languageAPI = APILocator.getLanguageAPI();

        contentletAPI = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user, false);
        fieldAPI = APILocator.getContentTypeFieldAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    public static class TestCase {

        enum LANGUAGE_TYPE {ENGLISH, SPANISH, ALL}

        enum PUBLISH_TYPE {LIVE, WORKING, ALL}

        LANGUAGE_TYPE languageType;
        PUBLISH_TYPE publishType;
        boolean pullByParent;
        boolean addCondition;
        int resultsSize;

        public TestCase(final LANGUAGE_TYPE languageType, final PUBLISH_TYPE publishType,
                final boolean pullByParent, final boolean addCondition, final int resultsSize) {
            this.languageType = languageType;
            this.publishType = publishType;
            this.pullByParent = pullByParent;
            this.addCondition = addCondition;
            this.resultsSize = resultsSize;
        }
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.ALL, false, false, 2),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.ALL, false, true, 2),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.ALL, true, false, 2),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.ALL, true, true, 2),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.LIVE, false, false, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.LIVE, false, true, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.LIVE, true, false, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.LIVE, true, true, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.WORKING, false, false, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.WORKING, false, true, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.WORKING, true, false, 1),
                new TestCase(LANGUAGE_TYPE.ALL, PUBLISH_TYPE.WORKING, true, true, 1),

                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.ALL, false, false, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.ALL, false, true, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.ALL, true, false, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.ALL, true, true, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.LIVE, false, false, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.LIVE, false, true, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.LIVE, true, false, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.LIVE, true, true, 1),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.WORKING, false, false, 0),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.WORKING, false, true, 0),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.WORKING, true, false, 0),
                new TestCase(LANGUAGE_TYPE.ENGLISH, PUBLISH_TYPE.WORKING, true, true, 0),

                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.ALL, false, false, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.ALL, false, true, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.ALL, true, false, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.ALL, true, true, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.LIVE, false, false, 0),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.LIVE, false, true, 0),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.LIVE, true, false, 0),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.LIVE, true, true, 0),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.WORKING, false, false, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.WORKING, false, true, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.WORKING, true, false, 1),
                new TestCase(LANGUAGE_TYPE.SPANISH, PUBLISH_TYPE.WORKING, true, true, 1)

        };
    }

    @Test
    @UseDataProvider("testCases")
    public void testPullRelated(TestCase testCase) throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType = null;
        Language defaultLanguage = languageAPI.getDefaultLanguage();
        Language spanishLanguage = TestDataUtils.getSpanishLanguage();
        try {

            //Create parent and child content types
            parentContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());
            childContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("childContentType" + time)
                            .owner(user.getUserId()).build());

            //Add a relationship field
            Field parentTypeRelationshipField = FieldBuilder.builder(RelationshipField.class)
                    .name("newRel")
                    .contentTypeId(parentContentType.id())
                    .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                    .relationType(childContentType.id()).build();

            parentTypeRelationshipField = fieldAPI.save(parentTypeRelationshipField, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField
                            .variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //Save children content
            Contentlet childInEnglish = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            ContentletDataGen.publish(childInEnglish);

            Contentlet childInSpanish = new ContentletDataGen(childContentType.id())
                    .languageId(spanishLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            //Save parents content
            Contentlet parentInEnglish = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).next();

            parentInEnglish = contentletAPI.checkin(parentInEnglish,
                    map(relationship, list(childInEnglish, childInSpanish)),
                    null, user, false);

            Contentlet parentInSpanish = contentletAPI
                    .checkout(parentInEnglish.getInode(), user, false);

            parentInSpanish.setInode("");
            parentInSpanish.setLanguageId(spanishLanguage.getId());
            parentInSpanish.setIndexPolicy(IndexPolicy.FORCE);

            parentInSpanish = contentletAPI.checkin(parentInSpanish, user, false);

            parentInEnglish = ContentletDataGen.publish(parentInEnglish);

            //Clean up cache
            CacheLocator.getContentletCache().remove(childInEnglish);
            CacheLocator.getContentletCache().remove(childInSpanish);
            CacheLocator.getContentletCache().remove(parentInEnglish);
            CacheLocator.getContentletCache().remove(parentInSpanish);

            //Define content to be sent as param for the pullRelated method
            Contentlet contentletToPullFrom;
            if (testCase.pullByParent) {
                if (testCase.languageType == LANGUAGE_TYPE.SPANISH) {
                    contentletToPullFrom = parentInSpanish;
                } else {
                    contentletToPullFrom = parentInEnglish;
                }
            } else {
                if (testCase.languageType == LANGUAGE_TYPE.SPANISH) {
                    contentletToPullFrom = childInSpanish;
                } else {
                    contentletToPullFrom = childInEnglish;
                }
            }

            //Define language and live params for the pullRelated call
            long languageParam = -1;
            Boolean liveParam = null;

            if (testCase.languageType == LANGUAGE_TYPE.SPANISH) {
                languageParam = spanishLanguage.getId();
            } else if (testCase.languageType == LANGUAGE_TYPE.ENGLISH) {
                languageParam = defaultLanguage.getId();
            }

            if (testCase.publishType != PUBLISH_TYPE.ALL) {
                liveParam = testCase.publishType == PUBLISH_TYPE.LIVE;
            }

            List<Contentlet> results = ContentUtils
                    .pullRelated(fullFieldVar, contentletToPullFrom.getIdentifier(),
                            testCase.addCondition ? "+working:true" : null,
                            testCase.pullByParent, -1, null, user, null,
                            languageParam, liveParam);

            //Validate results
            assertNotNull(results);
            assertEquals(testCase.resultsSize, results.size());

            if (testCase.languageType != LANGUAGE_TYPE.ALL) {
                assertTrue(results.stream().allMatch(contentlet -> contentlet.getLanguageId() == (
                        testCase.languageType == LANGUAGE_TYPE.ENGLISH ? defaultLanguage.getId()
                                : spanishLanguage.getId())));
            }

            if (testCase.publishType != PUBLISH_TYPE.ALL) {
                assertTrue(results.stream().allMatch(contentlet -> {
                    try {
                        return testCase.publishType == PUBLISH_TYPE.LIVE && contentlet.isLive()
                                || testCase.publishType == PUBLISH_TYPE.WORKING && !contentlet
                                .isLive();
                    } catch (DotDataException e) {
                        e.printStackTrace();
                    } catch (DotSecurityException e) {
                        e.printStackTrace();
                    }
                    return false;
                }));
            }
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

}
