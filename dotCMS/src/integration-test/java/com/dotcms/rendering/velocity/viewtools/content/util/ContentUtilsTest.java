package com.dotcms.rendering.velocity.viewtools.content.util;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.LANGUAGE_TYPE_FILTER;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.TestCase.PUBLISH_TYPE_FILTER;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
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
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
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

        public TestCase(final LANGUAGE_TYPE_FILTER languageType, final PUBLISH_TYPE_FILTER publishType,
                final boolean pullParents, final boolean addCondition, final int resultsSize, final boolean publishAll) {
            this.languageType = languageType;
            this.publishType = publishType;
            this.pullParents = pullParents;
            this.addCondition = addCondition;
            this.resultsSize = resultsSize;
            this.publishAll  = publishAll;
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
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, false, 2, false),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, true, 1, false),

                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, true, 0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, true, 0, false),

                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, false, 0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, true, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, false, 1, false),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, true, 1, false),

                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.LIVE, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, false, 2, true),
                new TestCase(LANGUAGE_TYPE_FILTER.DEFAULT, PUBLISH_TYPE_FILTER.WORKING, false, true, 0, true),

                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.ENGLISH, PUBLISH_TYPE_FILTER.WORKING, false, true, 0, true),

                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.DEFAULT, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.LIVE, false, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, true, true, 0, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, false, 1, true),
                new TestCase(LANGUAGE_TYPE_FILTER.SPANISH, PUBLISH_TYPE_FILTER.WORKING, false, true, 0, true)
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
    public void testPullRelated(final TestCase testCase) throws DotDataException, DotSecurityException {
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
                    parentContentType.id(), childContentType.variable(), RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            createAndSaveRelationshipField("myParents",
                    childContentType.id(), fullFieldVar, RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());


            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //Save children content
            final Contentlet childInEnglish = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            ContentletDataGen.publish(childInEnglish);

            final Contentlet childInSpanish = new ContentletDataGen(childContentType.id())
                    .languageId(spanishLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).nextPersisted();

            if (testCase.publishAll){
                ContentletDataGen.publish(childInSpanish);
            }

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

            if (testCase.publishAll){
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
            if (testCase.addCondition){
               condition = testCase.publishAll? "+live:false":"+working:true";
            }

            final List<Contentlet> results = ContentUtils
                    .pullRelated(fullFieldVar, contentletToPullFrom.getIdentifier(),
                            condition,
                            testCase.pullParents, -1, null, user, null,
                            languageParam, liveParam);

            //Validate results
            validateResults(testCase, defaultLanguage, spanishLanguage, results);

            //Validate results for cached results
            if (!testCase.addCondition){
                final List<Contentlet> cachedResults = ContentUtils
                        .pullRelated(fullFieldVar, contentletToPullFrom.getIdentifier(),null,
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
    private Field createAndSaveRelationshipField(final String relationshipName, final String parentTypeId,
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

            if (!testCase.selfRelated){
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
                    map(relationship, list(firstChild, secondChild)),
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
        relationship.setParentStructureInode( parentStructureInode );
        relationship.setParentRelationName( "parent" );
        //Set Child Info
        relationship.setChildStructureInode( childStructureInode );
        relationship.setChildRelationName( "child" );
        //Set general info
        relationship.setRelationTypeValue( "parent-child" + time);
        relationship.setCardinality(cardinality);

        //Save it
        FactoryLocator.getRelationshipFactory().save( relationship );

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
                    parentContentType.id(), childContentType.variable(), RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final String fullFieldVar = parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

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

            childContent.setInode("");
            childContent.setLanguageId(defaultLanguage.getId());
            childContent.setIndexPolicy(IndexPolicy.FORCE);

            childContent = contentletAPI.checkin(childContent, user, false);

            //Save and publish parent content
            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setPolicy(IndexPolicy.FORCE).next();

            parentContent = contentletAPI.checkin(parentContent,
                    map(relationship, list(childContent)),
                    null, user, false);

            parentContent = ContentletDataGen.publish(parentContent);

            //Keep parent inode of the live version to validate results
            parentInode = parentContent.getInode();

            //Create parent working version
            parentContent = contentletAPI
                    .checkout(parentContent.getInode(), user, false);

            parentContent.setInode("");
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
     *
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
     *
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
    public void testSortPullRelated(final SortTestCase testCase) throws DotDataException, DotSecurityException {
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
            childContentType = testCase.selfRelated? parentContentType: contentTypeAPI
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
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            Contentlet contentlet;
            Contentlet relatedContent1;
            Contentlet relatedContent2;
            Contentlet relatedContent3;

            contentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE).next();

            //Save related content
            relatedContent1 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE).setProperty("myCustomDate",
                            new Date(System.currentTimeMillis()))
                    .nextPersisted();

            relatedContent2 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE).setProperty("myCustomDate",
                            new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                    .nextPersisted();

            relatedContent3 = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId()).setPolicy(IndexPolicy.FORCE).setProperty("myCustomDate",
                            new Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))
                    .nextPersisted();


            final ContentletRelationships contentletRelationships = new ContentletRelationships(
                    contentlet);

            final ContentletRelationships.ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                    relationship, !testCase.pullParents);
            records.setRecords(list(relatedContent1, relatedContent2, relatedContent3));
            contentletRelationships.getRelationshipsRecords().add(records);

            contentlet = contentletAPI.checkin(contentlet,contentletRelationships,
                    null,null, user, false);


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

}
