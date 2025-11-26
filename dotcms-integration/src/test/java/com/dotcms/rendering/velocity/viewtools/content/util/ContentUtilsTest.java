package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.LANGUAGE_TYPE_FILTER;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.PUBLISH_TYPE_FILTER;
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
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dotcms.datagen.TestDataUtils.getCommentsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.relateContentTypes;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ContentUtilsTest {

    public static final String QUERY_BY_STRUCTURE_NAME = "+live:true +structureName:%s";
    public static final String SYS_PUBLISH_DATE = "sysPublishDate";
    public static final String SYS_EXPIRE_DATE = "sysExpireDate";
    private static User user;
    private static LanguageAPI languageAPI;

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static RelationshipAPI relationshipAPI;

    private static Language defaultLanguage;

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

        defaultLanguage = languageAPI.getDefaultLanguage();
    }

    public static class SortTestCase {

        boolean pullParents;
        boolean addCondition;
        boolean selfRelated;

        public SortTestCase(
                final boolean pullParents, final boolean addCondition, final boolean selfRelated) {
            this.pullParents = pullParents;
            this.addCondition = addCondition;
            this.selfRelated = selfRelated;
        }
    }

    public static class TestCase {

        enum LANGUAGE_TYPE_FILTER {ENGLISH, SPANISH, DEFAULT}

        enum PUBLISH_TYPE_FILTER {LIVE, WORKING, DEFAULT}

        LANGUAGE_TYPE_FILTER languageType;
        PUBLISH_TYPE_FILTER publishType;
        boolean pullParents;
        boolean addCondition;
        boolean publishAll;
        int resultsSize;

        public TestCase(final LANGUAGE_TYPE_FILTER languageType,
                final PUBLISH_TYPE_FILTER publishType,
                final boolean pullParents, final boolean addCondition, final int resultsSize,
                final boolean publishAll) {
            this.languageType = languageType;
            this.publishType = publishType;
            this.pullParents = pullParents;
            this.addCondition = addCondition;
            this.resultsSize = resultsSize;
            this.publishAll = publishAll;
        }
    }

    @DataProvider
    public static Object[] sortTestCases() {
        return new SortTestCase[]{
                new SortTestCase(true, true, true),
                new SortTestCase(true, false, true),
                new SortTestCase(false, true, true),
                new SortTestCase(false, true, false),
                new SortTestCase(false, false, true),
                new SortTestCase(false, false, false)
        };
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, false, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, true, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, true, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        1, false),

                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 1,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        0, false),

                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 0,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0,
                        false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        1, false),

                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, false, 2,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        0, true),

                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        0, true),

                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false,
                        false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0,
                        true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, false,
                        1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, true,
                        0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false,
                        false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, true,
                        0, true)
        };
    }

    public static class LegacyTestCase {

        boolean selfRelated;

        public LegacyTestCase(final boolean selfRelated) {
            this.selfRelated = selfRelated;
        }
    }

    @DataProvider
    public static Object[] legacyTestCases() {
        return new LegacyTestCase[]{
                new LegacyTestCase(true),
                new LegacyTestCase(false)
        };
    }

    @Test
    @UseDataProvider("testCases")
    public void testPullRelated(final TestCase testCase)
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType = null;

        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();
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

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("myChildren",
                    parentContentType.id(), childContentType.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD
                            + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            createAndSaveRelationshipField("myParents",
                    childContentType.id(), fullFieldVar,
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //Save children content
            final Contentlet childInEnglish = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            ContentletDataGen.publish(childInEnglish);

            final Contentlet childInSpanish = new ContentletDataGen(childContentType.id())
                    .languageId(spanishLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            if (testCase.publishAll) {
                ContentletDataGen.publish(childInSpanish);
            }

            //Save parents content
            Contentlet parentInEnglish = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).next();

            parentInEnglish = contentletAPI.checkin(parentInEnglish,
                    Map.of(relationship, list(childInEnglish, childInSpanish)),
                    null, user, false);

            Contentlet parentInSpanish = contentletAPI
                    .checkout(parentInEnglish.getInode(), user, false);

            parentInSpanish.setLanguageId(spanishLanguage.getId());
            parentInSpanish.setIndexPolicy(IndexPolicy.FORCE);

            parentInSpanish = contentletAPI.checkin(parentInSpanish, user, false);

            parentInEnglish = ContentletDataGen.publish(parentInEnglish);

            if (testCase.publishAll) {
                ContentletDataGen.publish(parentInSpanish);
            }

            //Clean up cache
            CacheLocator.getContentletCache().remove(childInEnglish);
            CacheLocator.getContentletCache().remove(childInSpanish);
            CacheLocator.getContentletCache().remove(parentInEnglish);
            CacheLocator.getContentletCache().remove(parentInSpanish);

            //Define content to be sent as param for the pullRelated method
            Contentlet contentletToPullFrom;
            if (!testCase.pullParents) {
                if (testCase.languageType == LANGUAGE_TYPE_FILTER.SPANISH) {
                    contentletToPullFrom = parentInSpanish;
                } else {
                    contentletToPullFrom = parentInEnglish;
                }
            } else {
                if (testCase.languageType == LANGUAGE_TYPE_FILTER.SPANISH) {
                    contentletToPullFrom = childInSpanish;
                } else {
                    contentletToPullFrom = childInEnglish;
                }
            }

            //Define language and live params for the pullRelated call
            long languageParam = -1;
            Boolean liveParam = null;

            if (testCase.languageType == LANGUAGE_TYPE_FILTER.SPANISH) {
                languageParam = spanishLanguage.getId();
            } else if (testCase.languageType == LANGUAGE_TYPE_FILTER.ENGLISH) {
                languageParam = defaultLanguage.getId();
            }

            if (testCase.publishType != PUBLISH_TYPE_FILTER.DEFAULT) {
                liveParam = testCase.publishType == PUBLISH_TYPE_FILTER.LIVE;
            }

            String condition = null;
            if (testCase.addCondition) {
                condition = testCase.publishAll ? "+live:false" : "+working:true";
            }

            final List<Contentlet> results = ContentUtils
                    .pullRelated(fullFieldVar, contentletToPullFrom.getIdentifier(),
                            condition,
                            testCase.pullParents, -1, null, user, null,
                            languageParam, liveParam);

            //Validate results
            validateResults(testCase, defaultLanguage, spanishLanguage, results);

            //Validate results for cached results
            if (!testCase.addCondition) {
                final List<Contentlet> cachedResults = ContentUtils
                        .pullRelated(fullFieldVar, contentletToPullFrom.getIdentifier(), null,
                                testCase.pullParents, -1, null, user, null,
                                languageParam, liveParam);

                validateResults(testCase, defaultLanguage, spanishLanguage, cachedResults);
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

    /**
     * @param relationshipName
     * @param parentTypeId
     * @param childTypeVar
     * @param cardinality
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createAndSaveRelationshipField(final String relationshipName,
            final String parentTypeId,
            final String childTypeVar, final int cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(String.valueOf(cardinality))
                .relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    /**
     * @param testCase
     * @param defaultLanguage
     * @param spanishLanguage
     * @param results
     */
    private void validateResults(final TestCase testCase, final Language defaultLanguage,
            final Language spanishLanguage, final List<Contentlet> results) {
        assertNotNull(results);
        assertEquals(testCase.resultsSize, results.size());

        if (testCase.languageType != LANGUAGE_TYPE_FILTER.DEFAULT) {
            assertTrue(results.stream().allMatch(contentlet -> contentlet.getLanguageId() == (
                    testCase.languageType == LANGUAGE_TYPE_FILTER.ENGLISH ? defaultLanguage.getId()
                            : spanishLanguage.getId())));
        }

        if (testCase.publishType != PUBLISH_TYPE_FILTER.DEFAULT) {
            assertTrue(results.stream().allMatch(contentlet -> {
                try {
                    return testCase.publishType == PUBLISH_TYPE_FILTER.LIVE && contentlet.isLive()
                            || testCase.publishType == PUBLISH_TYPE_FILTER.WORKING && contentlet
                            .isWorking();
                } catch (DotDataException | DotSecurityException e) {
                    e.printStackTrace();
                }
                return false;
            }));
        }
    }

    @UseDataProvider("legacyTestCases")
    @Test
    public void testPullRelatedForLegacyRelationship(final LegacyTestCase testCase)
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());

            if (!testCase.selfRelated) {
                childContentType = contentTypeAPI
                        .save(ContentTypeBuilder.builder(SimpleContentType.class)
                                .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                                .name("childContentType" + time)
                                .owner(user.getUserId()).build());
            }

            final Relationship relationship = createLegacyRelationship(parentContentType.id(),
                    testCase.selfRelated ? parentContentType.id() : childContentType.id(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            //Save children content
            final Contentlet firstChild = new ContentletDataGen(
                    testCase.selfRelated ? parentContentType.id() : childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            ContentletDataGen.publish(firstChild);

            final Contentlet secondChild = new ContentletDataGen(
                    testCase.selfRelated ? parentContentType.id() : childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            ContentletDataGen.publish(secondChild);

            //Save parent content and relate children
            Contentlet parent = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).next();

            parent = contentletAPI.checkin(parent,
                    Map.of(relationship, list(firstChild, secondChild)),
                    null, user, false);

            ContentletDataGen.publish(parent);

            //Pulling children
            List<Contentlet> results = ContentUtils
                    .pullRelated(relationship.getRelationTypeValue(),
                            parent.getIdentifier(), false, -1, user, null);

            assertEquals(2, results.size());
            assertEquals(firstChild.getInode(), results.get(0).getInode());
            assertEquals(secondChild.getInode(), results.get(1).getInode());

            //Pulling parent
            results = ContentUtils
                    .pullRelated(relationship.getRelationTypeValue(),
                            firstChild.getIdentifier(),
                            true, -1, user, null);

            assertEquals(1, results.size());
            assertEquals(parent.getInode(), results.get(0).getInode());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }

    }

    private Relationship createLegacyRelationship(String parentStructureInode,
            String childStructureInode, int cardinality) throws DotDataException {

        final long time = System.currentTimeMillis();
        final Relationship relationship = new Relationship();
        //Set Parent Info
        relationship.setParentStructureInode(parentStructureInode);
        relationship.setParentRelationName("parent");
        //Set Child Info
        relationship.setChildStructureInode(childStructureInode);
        relationship.setChildRelationName("child");
        //Set general info
        relationship.setRelationTypeValue("parent-child" + time);
        relationship.setCardinality(cardinality);

        //Save it
        APILocator.getRelationshipAPI().save(relationship);

        return relationship;
    }

    @Test
    public void testPullLiveRelatedWhenLatestVersionIsWorking()
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {

            final String childInode;
            final String parentInode;

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

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("myChild",
                    parentContentType.id(), childContentType.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final String fullFieldVar = parentContentType.variable() + StringPool.PERIOD
                    + parentTypeRelationshipField.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //Save and publish child content
            Contentlet childContent = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            childContent = ContentletDataGen.publish(childContent);

            //Keep parent inode of the live version to validate results
            childInode = childContent.getInode();

            //Create child working version
            childContent = contentletAPI
                    .checkout(childContent.getInode(), user, false);

            childContent.setLanguageId(defaultLanguage.getId());
            childContent.setIndexPolicy(IndexPolicy.FORCE);

            childContent = contentletAPI.checkin(childContent, user, false);

            //Save and publish parent content
            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).next();

            parentContent = contentletAPI.checkin(parentContent,
                    Map.of(relationship, list(childContent)),
                    null, user, false);

            parentContent = ContentletDataGen.publish(parentContent);

            //Keep parent inode of the live version to validate results
            parentInode = parentContent.getInode();

            //Create parent working version
            parentContent = contentletAPI
                    .checkout(parentContent.getInode(), user, false);

            parentContent.setLanguageId(defaultLanguage.getId());
            parentContent.setIndexPolicy(IndexPolicy.FORCE);

            parentContent = contentletAPI.checkin(parentContent, user, false);

            validateCacheResults(childInode, fullFieldVar, childContent, parentContent, false,
                    false);
            validateCacheResults(childInode, fullFieldVar, childContent, parentContent, false,
                    true);

            validateCacheResults(parentInode, fullFieldVar, parentContent, childContent, true,
                    false);
            validateCacheResults(parentInode, fullFieldVar, parentContent, childContent, true,
                    true);

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * @param childInode
     * @param fullFieldVar
     * @param childContent
     * @param parentContent
     * @param pullParents
     * @param isAnonymous
     * @throws DotDataException
     */
    private void validateCacheResults(final String childInode, final String fullFieldVar,
            final Contentlet childContent, final Contentlet parentContent,
            final boolean pullParents, final boolean isAnonymous) throws DotDataException {
        //Clean up cache
        CacheLocator.getContentletCache().remove(childContent);
        CacheLocator.getContentletCache().remove(parentContent);

        //Validate non-cached results (from parent)
        validateLiveResults(childInode, fullFieldVar, parentContent, pullParents, isAnonymous);

        //Validate cached results (from parent)
        validateLiveResults(childInode, fullFieldVar, parentContent, pullParents, isAnonymous);
    }

    /**
     * @param expectedInode
     * @param relationshipName
     * @param contentlet
     */
    private void validateLiveResults(final String expectedInode, final String relationshipName,
            final Contentlet contentlet, final boolean pullParents, final boolean isAnonymous)
            throws DotDataException {
        List<Contentlet> results = ContentUtils
                .pullRelated(relationshipName, contentlet.getIdentifier(), null, pullParents, -1,
                        null,
                        isAnonymous ? APILocator.getUserAPI().getAnonymousUser() : user, null, -1,
                        isAnonymous ? null : true);
        assertEquals(1, results.size());
        assertEquals(expectedInode, results.get(0).getInode());
    }

    @Test
    @UseDataProvider("sortTestCases")
    public void testSortPullRelated(final SortTestCase testCase)
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {

            //Create parent and child content types
            parentContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());
            childContentType = testCase.selfRelated ? parentContentType : contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("childContentType" + time)
                            .owner(user.getUserId()).build());

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField(
                    "newRel", parentContentType.id(), childContentType.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            Field customField = FieldBuilder.builder(DateField.class)
                    .contentTypeId(childContentType.id()).name("myCustomDate")
                    .variable("myCustomDate").indexed(true).build();
            fieldAPI.save(customField, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD
                            + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar,
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            Contentlet contentlet;
            Contentlet relatedContent1;
            Contentlet relatedContent2;
            Contentlet relatedContent3;

            contentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE).next();

            //Save related content
            relatedContent1 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE)
                    .setProperty("myCustomDate",
                            new Date(System.currentTimeMillis()))
                    .nextPersisted();

            relatedContent2 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE)
                    .setProperty("myCustomDate",
                            new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                    .nextPersisted();

            relatedContent3 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE)
                    .setProperty("myCustomDate",
                            new Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))
                    .nextPersisted();

            final ContentletRelationships contentletRelationships = new ContentletRelationships(
                    contentlet);

            final ContentletRelationships.ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                    relationship, !testCase.pullParents);
            records.setRecords(list(relatedContent1, relatedContent2, relatedContent3));
            contentletRelationships.getRelationshipsRecords().add(records);

            contentlet = contentletAPI.checkin(contentlet, contentletRelationships,
                    null, null, user, false);

            //Clean up cache
            CacheLocator.getContentletCache().remove(contentlet);
            CacheLocator.getContentletCache().remove(relatedContent1);
            CacheLocator.getContentletCache().remove(relatedContent2);
            CacheLocator.getContentletCache().remove(relatedContent3);

            //Validate non cached results
            final String condition = testCase.addCondition ? "+working:true" : null;
            List<Contentlet> results = ContentUtils
                    .pullRelated(fullFieldVar, contentlet.getIdentifier(), condition,
                            testCase.pullParents, -1,
                            childContentType.variable() + ".myCustomDate desc", user, null, -1,
                            false);

            assertNotNull(results);
            assertEquals(3, results.size());
            assertEquals(relatedContent3.getIdentifier(), results.get(0).getIdentifier());
            assertEquals(relatedContent2.getIdentifier(), results.get(1).getIdentifier());
            assertEquals(relatedContent1.getIdentifier(), results.get(2).getIdentifier());

            //Validate cached results
            results = ContentUtils
                    .pullRelated(fullFieldVar, contentlet.getIdentifier(), condition,
                            testCase.pullParents, -1,
                            (testCase.pullParents && !testCase.selfRelated) ? parentContentType
                                    .variable()
                                    : childContentType.variable() + ".myCustomDate desc", user,
                            null, -1,
                            false);

            assertNotNull(results);
            assertEquals(3, results.size());
            assertEquals(relatedContent3.getIdentifier(), results.get(0).getIdentifier());
            assertEquals(relatedContent2.getIdentifier(), results.get(1).getIdentifier());
            assertEquals(relatedContent1.getIdentifier(), results.get(2).getIdentifier());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (!testCase.selfRelated && UtilMethods.isSet(childContentType) && UtilMethods
                    .isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * Method to test: {@link ContentUtils#pull(String, int, String, User, String)} When: there is a
     * content with a publish date in the future and the time machine parameter in null Should: Not
     * return the content
     */
    @Test
    public void whenTheTimeMachineDateIsNullAndPublishDateInFutureShouldNotReturnAnything() {
        final String timeMachine = null;
        final Calendar contentPublishDate = Calendar.getInstance();
        contentPublishDate.add(Calendar.DATE, 1);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, contentPublishDate.getTime())
                .nextPersisted();

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final List<Contentlet> contentlets = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(), timeMachine);

        assertEquals(0, contentlets.size());

        final List<Contentlet> workingContent = ContentUtils.pull(query.replace("live", "working"),
                10, null, APILocator.systemUser(), null);
        assertEquals(1, workingContent.size());
    }

    /**
     * Method to test: {@link ContentUtils#pull(String, int, String, User, String)} When: there is a
     * content with a publish date set to tomorrow and the time machine date is the date after
     * tomorrow Should: return one content
     */
    @Test
    public void whenTheTimeMachineDateAndPublishDateAreTomorrowShouldReturnOneContent() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 2);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .nextPersisted();

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final List<Contentlet> contentlets = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(),
                String.valueOf(timeMachine.getTime().getTime()));

        assertEquals(1, contentlets.size());
        assertEquals(contentlet.getIdentifier(), contentlets.get(0).getIdentifier());

        assertEquals(0, ContentUtils.pull(query, 10, null, APILocator.systemUser(), null).size());
    }

    /**
     * Method to test: {@link ContentUtils#pull(String, int, String, User, String)} When: there is a
     * content with a expire  date set to tomorrow and the time machine date is the date after
     * tomorrow Should: return one content
     */
    @Test
    public void whenTheTimeMachineDateIsAfterTomorrowAndExpireDateIsTomorrowShouldNotReturnContent() {
        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 2);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar afterTomorrow = Calendar.getInstance();
        afterTomorrow.add(Calendar.DATE, 3);
        final String timeMachine = String.valueOf(afterTomorrow.getTime().getTime());

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final List<Contentlet> contentlets = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(), timeMachine);

        assertEquals(0, contentlets.size());

        final List<Contentlet> workingContent = ContentUtils.pull(query.replace("live", "working"),
                10, null, APILocator.systemUser(), null);
        assertEquals(1, workingContent.size());
    }

    /**
     * Method to test: {@link ContentUtils#pull(String, int, String, User, String)} When: there is a
     * content with a publish date set to tomorrow and expire date set in the future and the time
     * machine date is set to tomorrow Should: return one content
     */
    @Test
    public void whenTheTimeMachineDateIsAfterTomorrowAndExpireDateIsInFutureShouldReturnContent() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 3);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 2);

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final List<Contentlet> contentlets = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(),
                String.valueOf(timeMachine.getTime().getTime()));

        assertEquals(1, contentlets.size());
        assertEquals(contentlet.getIdentifier(), contentlets.get(0).getIdentifier());

        final List<Contentlet> contentNotTM = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(), null);
        assertEquals(0, contentNotTM.size());
    }

    /**
     * Method to test: {@link ContentUtils#pull(String, int, String, User, String)} When: there is a
     * content with a publish date set to tomorrow and expire date set to after tomorrow and the
     * time machine date set after that Should: return no one content
     */
    @Test
    public void whenPublishAndExpireDatesAreInTheFutureAndTimeMachineIsAfterBoth() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 2);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 3);

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final List<Contentlet> contentlets = ContentUtils.pull(query, 10, null,
                APILocator.systemUser(),
                String.valueOf(timeMachine.getTime().getTime()));

        assertEquals(0, contentlets.size());

        final List<Contentlet> contentNoTM = ContentUtils.pull(
                String.format("+structureName:%s", contentType.variable()), 10, null,
                APILocator.systemUser(), null);
        assertEquals(1, contentNoTM.size());
    }

    /**
     * Method to test: {@link ContentUtils#find(String, User, boolean, long)} When: there is a
     * contentlet in multiligual we should get the one with the sessionLang Should: contentlet with
     * the sessionLang
     */
    @Test
    public void test_find_multilingual_contentlet_getContentletSuccessfully()
            throws DotSecurityException,
            DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final long spanishLangId = TestDataUtils.getSpanishLanguage().getId();
        final int sessionLanguage = 1;
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true, sessionLanguage,
                host);
        final Contentlet contentletNewLang = APILocator.getContentletAPI()
                .checkout(contentlet.getInode(), user, false);
        contentletNewLang.setLanguageId(spanishLangId);
        APILocator.getContentletAPI().checkin(contentletNewLang, user, false);
        final boolean workingVersion = Boolean.TRUE;
        final Contentlet contentletFound = ContentUtils.find(contentlet.getIdentifier(), user,
                workingVersion,
                sessionLanguage);

        assertEquals(
                "Contentlet created with session language must match the one retrieved via the API",
                contentlet.getInode(), contentletFound.getInode());
        assertEquals(
                "Contentlet created with session language and the one retrieved via the API must have the same "
                        +
                        "language ID", contentlet.getLanguageId(), contentletFound.getLanguageId());
    }

    /**
     * Method to test:
     * {@link ContentUtils#pullRelatedField(Relationship, String, String, int, int, String, User,
     * String, boolean, long, Boolean)} Given Scenario: Pulling related content should consider the
     * order defined in the relationship if the sort criteria is empty or not set ExpectedResult:
     * The relationship order is used when a sort criteria is not set
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testPullRelatedFieldShouldRespectDefaultOrder()
            throws DotDataException, DotSecurityException {

        final long languageId = languageAPI.getDefaultLanguage().getId();
        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet;
        final List<Contentlet> relatedComments = new ArrayList<>();

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageId)
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());

            for (int i = 1; i < 5; i++) {
                commentsContentlet = dataGen
                        .languageId(languageId)
                        .setProperty("title", "Comment for News " + i)
                        .setProperty("email", "testing@dotcms.com")
                        .setProperty("comment", "Comment for News " + i)
                        .setPolicy(IndexPolicy.FORCE).nextPersisted();
                relatedComments.add(commentsContentlet);

            }

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    Map.of(relationship, relatedComments),
                    null, user, false);

            //Pull related content from comment child
            List<Contentlet> result = ContentUtils.
                    pullRelatedField(relationship, newsContentlet.getIdentifier(), "+languageId:1",
                            3, 0, "", user, null, false, languageId, false);

            assertNotNull(result);
            assertEquals(3, result.size());

            //related content should be returned in the default order
            assertEquals(relatedComments.get(0).getIdentifier(), result.get(0).getIdentifier());
            assertEquals(relatedComments.get(1).getIdentifier(), result.get(1).getIdentifier());
            assertEquals(relatedComments.get(2).getIdentifier(), result.get(2).getIdentifier());

            //let's reorder the related content and pull them again
            newsContentlet = contentletAPI.checkin(newsContentlet,
                    Map.of(relationship, list(relatedComments.get(3), relatedComments.get(1),
                            relatedComments.get(0), relatedComments.get(2))),
                    null, user, false);

            //now, we pull the related content again. Limit and offset were set to invalid values to make sure that it keeps working as expected
            result = ContentUtils.
                    pullRelatedField(relationship, newsContentlet.getIdentifier(), "+languageId:1",
                            5, -1000, "", user, null, false, languageId, false);

            assertNotNull(result);
            assertEquals(relatedComments.size(), result.size());

            //the new order should be preserved
            assertEquals(relatedComments.get(3).getIdentifier(), result.get(0).getIdentifier());
            assertEquals(relatedComments.get(1).getIdentifier(), result.get(1).getIdentifier());
            assertEquals(relatedComments.get(0).getIdentifier(), result.get(2).getIdentifier());
            assertEquals(relatedComments.get(2).getIdentifier(), result.get(3).getIdentifier());

        } finally {
            if (null != newsContentlet && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            for (final Contentlet contentlet : relatedComments) {
                ContentletDataGen.remove(contentlet);
            }
        }
    }


    /**
     * Method to test:
     * {@link ContentUtils#pullRelatedField(Relationship, String, String, int, int, String, User,
     * String, boolean, long, Boolean)} Given Scenario: Pulling related content when the offset is
     * greater than the limit ExpectedResult: Should return results, if they're available.
     * <p>
     * In this test 5 items are related, and we're passing offset = 3 and limit = 1, so 1 item
     * should be returned.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testPullRelatedField_withOffsetGreaterThanLimit()
            throws DotDataException, DotSecurityException {

        final long languageId = languageAPI.getDefaultLanguage().getId();
        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet;
        final List<Contentlet> relatedComments = new ArrayList<>();

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());

            //English version
            newsContentlet = dataGen.languageId(languageId)
                    .setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());

            for (int i = 1; i < 5; i++) {
                commentsContentlet = dataGen
                        .languageId(languageId)
                        .setProperty("title", "Comment for News " + i)
                        .setProperty("email", "testing@dotcms.com")
                        .setProperty("comment", "Comment for News " + i)
                        .setPolicy(IndexPolicy.FORCE).nextPersisted();
                relatedComments.add(commentsContentlet);

            }

            //Saving relationship
            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet.setIndexPolicy(IndexPolicy.FORCE);

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    Map.of(relationship, relatedComments),
                    null, user, false);

            //Pull related content from comment child
            List<Contentlet> result = ContentUtils.
                    pullRelatedField(relationship, newsContentlet.getIdentifier(), "+languageId:1",
                            1, 3, "", user, null, false, languageId, false);

            assertNotNull(result);
            assertEquals(1, result.size());

        } finally {
            if (null != newsContentlet && UtilMethods.isSet(newsContentlet.getInode())) {
                ContentletDataGen.remove(newsContentlet);
            }

            for (final Contentlet contentlet : relatedComments) {
                ContentletDataGen.remove(contentlet);
            }
        }
    }


    /**
     * Method to test:
     * {@link ContentUtils#addRelationships(Contentlet, User, PageMode, long, int,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: Creates a content parent with a
     * children many to many relationship, create a few instances of the child type and related to
     * the parent ExpectedResult: Calling the parameter with depth in 0 should retrieve the children
     * contentlets
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_add_relationships_on_parent_children_related_contents()
            throws DotDataException, DotSecurityException {

        // 1) create the child content type
        final ContentType childContentType = new ContentTypeDataGen().velocityVarName(
                "child" + System.currentTimeMillis()).nextPersisted();
        // 2) create parent content type and add a relationship to the child content type
        final ContentType parentContentType = new ContentTypeDataGen().velocityVarName(
                "parent" + System.currentTimeMillis()).nextPersisted();
        Field field = FieldBuilder.builder(RelationshipField.class).name("children")
                .contentTypeId(parentContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childContentType.variable()).build();

        APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
        // 3) create a few child instances

        final Contentlet child1 = new ContentletDataGen(childContentType.id()).nextPersisted();
        final Contentlet child2 = new ContentletDataGen(childContentType.id()).nextPersisted();
        final Contentlet child3 = new ContentletDataGen(childContentType.id()).nextPersisted();
        // 4) create a instance of the parent and add the child instances to the relationship
        // 5) save it
        final Contentlet parent = new ContentletDataGen(parentContentType.id()).setProperty(
                "children", Arrays.asList(child1, child2, child3)).nextPersisted();

        // 6) retrieve again
        final Contentlet parentRetrieved = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(parent.getIdentifier());
        Assert.assertNotNull(parentRetrieved);
        Assert.assertEquals(parent.getIdentifier(), parentRetrieved.getIdentifier());
        // 7) call the addRelationships method with depth = 1
        final int depth = 0;  // only ids
        final HttpServletRequest request = new MockHttpRequestIntegrationTest("localhost",
                "/api/v1/test").request();
        final HttpServletResponse response = new MockHttpResponse().response();
        ContentUtils.addRelationships(parentRetrieved, user, PageMode.EDIT_MODE,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(), depth, request, response);
        // 8) check the children contentlets are there
        Assert.assertTrue(parentRetrieved.getMap().containsKey("children"));
        final Object children = parentRetrieved.get("children");
        Assert.assertNotNull(children);
        Assert.assertTrue(children instanceof Collection);
        final Set<String> childrenIds = new HashSet<>((Collection<String>) children);
        Assert.assertTrue(childrenIds.contains(child1.getIdentifier()));
        Assert.assertTrue(childrenIds.contains(child2.getIdentifier()));
        Assert.assertTrue(childrenIds.contains(child3.getIdentifier()));
    }

    /**
     * Test method: testFuturePublishDateContentRetrieval
     * <p>
     * Given: - Two versions of the same content (same identifier, different inodes) - V1 is
     * published immediately - V2 has a publish date set 10 days in the future - A query is made
     * with a date after V2's publish date
     * <p>
     * Scenario: - Content is searched by identifier and by inode with a specific future date -
     * Content is searched in EDIT mode without specifying a date - Content is searched in LIVE mode
     * without specifying a date
     * <p>
     * Expected Results: - When searching with a future date (after the publish date), it should
     * return V2 regardless of whether searching by identifier or inode - When searching in EDIT
     * mode without a date, it should return V2 (most recent version) - When searching in LIVE mode
     * without a date, it should return V1 (currently published version)
     */
    @Test
    public void testFuturePublishDateContentRetrieval() {
        final TimeZone defaultZone = TimeZone.getDefault();
        try {
            final TimeZone utc = TimeZone.getTimeZone("UTC");
            TimeZone.setDefault(utc);

            final Host host = new SiteDataGen().nextPersisted();
            final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
            final ContentType blogLikeContentType = TestDataUtils.getBlogLikeContentType();
            final ContentletDataGen blogsDataGen = new ContentletDataGen(blogLikeContentType.id())
                    .languageId(language.getId())
                    .host(host)
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .languageId(language.getId())
                    .setProperty(Contentlet.IS_TEST_MODE, true);

            //Ten days from now
            final LocalDateTime relativeDate = LocalDateTime.now().plusDays(10).plusHours(1);
            final Instant relativeInstant = relativeDate.atZone(utc.toZoneId()).toInstant();
            final Date publishDate = Date.from(relativeInstant);

            //Any date after publish date
            final LocalDateTime relativeDate2 = LocalDateTime.now().plusDays(11).plusHours(1);
            final Instant relativeInstant2 = relativeDate2.atZone(utc.toZoneId()).toInstant();
            final Date futureDate = Date.from(relativeInstant2);

            //Let's create two versions of a content they must share the same identifier
            blogsDataGen
                    .setProperty("title", "v1");
            final Contentlet blogV1 =
                    blogsDataGen.nextPersistedAndPublish();

            blogsDataGen
                    .setProperty("identifier", blogV1.getIdentifier())
                    .setProperty("title", "v2")
                    .setProperty("publishDate", publishDate);  // Set the publish-date in the future
            final Contentlet blogV2 = blogsDataGen.nextPersisted();

            final MockAttributeRequest mockAttributeRequest = new MockAttributeRequest(
                    new MockHttpRequestIntegrationTest("localhost", "/?mode=LIVE").request());
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockAttributeRequest);

            //Let's make sure that the identifier is the same
            Assert.assertEquals(blogV1.getIdentifier(), blogV2.getIdentifier());

            //Now, lets for the future version of the content. That's V2
            final String dateAsString = String.valueOf(futureDate.getTime());

            //Find by identifier and inode
            final Contentlet blogV1ByIdentifier = ContentUtils.find(blogV1.getIdentifier(), user,
                    dateAsString, true, language.getId());
            final Contentlet blogV1ByInode = ContentUtils.find(blogV1.getInode(), user,
                    dateAsString, true, language.getId());
            Assert.assertNotNull(blogV1ByIdentifier);
            Assert.assertNotNull(blogV1ByInode);
            //Here we should always get v2 regardless of the identifier or inode because they both have a future version matching the publish-date
            Assert.assertEquals("v2", blogV1ByIdentifier.getTitle());
            Assert.assertEquals("v2", blogV1ByInode.getTitle());
            //Here again we should always get v2 regardless of the identifier or inode because they both have a future version matching the publish-date
            final Contentlet blogV2ByIdentifier = ContentUtils.find(blogV2.getIdentifier(), user,
                    dateAsString, true, language.getId());
            final Contentlet blogV2ByInode = ContentUtils.find(blogV2.getInode(), user,
                    dateAsString, true, language.getId());
            Assert.assertNotNull(blogV2ByIdentifier);
            Assert.assertNotNull(blogV2ByInode);
            Assert.assertEquals("v2", blogV2ByIdentifier.getTitle());
            Assert.assertEquals("v2", blogV2ByInode.getTitle());

            //If we request Preview or Edit we should continue to get the same unpublished version
            final Contentlet blogByIdentifierNoDateEdit = ContentUtils.find(blogV2.getIdentifier(),
                    user, null, true, language.getId());
            Assert.assertNotNull(blogByIdentifierNoDateEdit);
            Assert.assertEquals("v2", blogByIdentifierNoDateEdit.getTitle());

            //Now if we request the live version we should get the v1
            final Contentlet blogByIdentifierNoDateLive = ContentUtils.find(blogV2.getIdentifier(),
                    user, null, false, language.getId());
            Assert.assertNotNull(blogByIdentifierNoDateLive);
            Assert.assertEquals("v1", blogByIdentifierNoDateLive.getTitle());

        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }

    /**
     * Test method: ContentUtils.pull(String, int, String, User, String)
     * Given Scenario: - We're simply testing that a query that is meant to 1 result, returns 1 result plus a future version of the content when passing a tmDate
     * Expected Results: - The query should return 2 result
     */
    @Test
    public void testContentPullLimit() {
        final TimeZone defaultZone = TimeZone.getDefault();
        try {
            final TimeZone utc = TimeZone.getTimeZone("UTC");
            TimeZone.setDefault(utc);

            final Host host = new SiteDataGen().nextPersisted();
            final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
            final ContentType blogLikeContentType = TestDataUtils.getBlogLikeContentType();
            final ContentletDataGen blogsDataGen = new ContentletDataGen(blogLikeContentType.id())
                    .languageId(language.getId())
                    .host(host)
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .languageId(language.getId())
                    .setProperty(Contentlet.IS_TEST_MODE, true);

            //Ten days from now
            final LocalDateTime relativeDate = LocalDateTime.now().plusDays(10).plusHours(1);
            final Instant relativeInstant = relativeDate.atZone(utc.toZoneId()).toInstant();
            final Date publishDate = Date.from(relativeInstant);

            //Any date after publish date
            final LocalDateTime relativeDate2 = LocalDateTime.now().plusDays(11).plusHours(1);
            final Instant elevenDaysInTheFuture = relativeDate2.atZone(utc.toZoneId()).toInstant();

            //Let's create two versions of a content they must share the same identifier
            blogsDataGen
                    .setProperty("title", "v1");
            final Contentlet blogV1 =
                    blogsDataGen.nextPersistedAndPublish();

            blogsDataGen
                    .setProperty("identifier", blogV1.getIdentifier())
                    .setProperty("title", "v2")
                    .setProperty("publishDate", publishDate);  // Set the publish-date in the future
            //And persist the future version
            final Contentlet blogV2 = blogsDataGen.nextPersisted();
            assertNotNull(blogV2);

            final String tmDate = elevenDaysInTheFuture.toEpochMilli() + "";
            final String query = "+contentType:" + blogLikeContentType.variable() + " +live:true ";
            final List<Contentlet> listNoLimit = ContentUtils.pull(query,-1,0,"modDate desc", user,
                    tmDate, false);
            assertEquals("We expect the live version plus the future version since we're passing the tmDate", 2, listNoLimit.size());
            final Contentlet contentlet1 = listNoLimit.get(0);
            assertEquals(blogV1.getIdentifier(), contentlet1.getIdentifier());

            final List<Contentlet> listWithLimit1 = ContentUtils.pull(query,-1,1,"modDate desc", user,
                    tmDate, false);
            assertEquals("We expect only 1 entry here. Since we used a limit 1", 1, listWithLimit1.size());
            //We don't know which one will be returned, but it should be one of the two

        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }
}