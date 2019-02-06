package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ESMappingAPITest {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static LanguageAPI languageAPI;
    private static Language language;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();

        contentTypeAPI = APILocator.getContentTypeAPI(user);
        contentletAPI = APILocator.getContentletAPI();
        fieldAPI = APILocator.getContentTypeFieldAPI();
        languageAPI = APILocator.getLanguageAPI();
        language = languageAPI.getDefaultLanguage();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @Test
    public void testLoadRelationshipFields_whenUsingLegacyRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        final Map<String, Object> esMap = new HashMap<>();

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());
            newsContentlet = dataGen.languageId(language.getId()).setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News").nextPersisted();

            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(commentsContentlet)),
                    null, user, false);

            esMappingAPI.loadRelationshipFields(newsContentlet, esMap);

            assertNotNull(esMap);
            assertEquals(commentsContentlet.getIdentifier(),
                    List.class.cast(esMap.get("News-Comments")).get(0));
            assertEquals(commentsContentlet.getIdentifier() + "_1",
                    List.class.cast(esMap.get("News-Comments" + ESMappingConstants.SUFFIX_ORDER)).get(0));

        } finally {
            if (newsContentlet != null && newsContentlet.getIdentifier() != null) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (commentsContentlet != null && commentsContentlet.getIdentifier() != null) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    @Test
    public void testLoadRelationshipFields_whenUsingSelfRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        final Map<String, Object> esMap = new HashMap<>();

        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet parentContentlet = null;
        Contentlet childContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(commentsContentType.id());
            childContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Child Comment for Test")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Child Comment for Test")
                    .next();

            //creates child contentlet
            parentContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Parent Comment for Test")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Parent Comment for Test")
                    .nextPersisted();

            final Relationship relationship = relationshipAPI.byTypeValue("Comments-Comments");

            childContentlet = contentletAPI.checkin(childContentlet,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(parentContentlet)),
                    null, user, false);

            esMappingAPI.loadRelationshipFields(parentContentlet, esMap);

            assertNotNull(esMap);
            assertEquals(childContentlet.getIdentifier(),
                    ((List)esMap.get("Comments-Comments" + ESMappingConstants.SUFFIX_CHILD)).get(0));
            assertEquals(childContentlet.getIdentifier() + "_1",
                    ((List)esMap.get("Comments-Comments" + ESMappingConstants.SUFFIX_ORDER)).get(0));

        } finally {
            if (parentContentlet != null && parentContentlet.getIdentifier() != null) {
                ContentletDataGen.remove(parentContentlet);
            }

            if (childContentlet != null && childContentlet.getIdentifier() != null) {
                ContentletDataGen.remove(childContentlet);
            }

        }
    }


    @Test
    public void testLoadRelationshipFields_whenUsingOneSideFieldRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        Map<String, Object> esMap = new HashMap<>();

        String cardinality = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");

            createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), cardinality);

            final Relationship relationship = relationshipAPI.byContentType(parentContentType)
                    .get(0);

            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContentlet = dataGen.languageId(language.getId()).next();

            //creates child contentlet
            dataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet1 = dataGen.languageId(language.getId()).nextPersisted();
            final Contentlet childContentlet2 = dataGen.languageId(language.getId()).nextPersisted();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(childContentlet1, childContentlet2)),
                    null, user, false);

            esMappingAPI.loadRelationshipFields(parentContentlet, esMap);

            assertNotNull(esMap);

            final List<String> expectedResults = CollectionsUtils
                    .list(childContentlet1.getIdentifier(), childContentlet2.getIdentifier());

            validateRelationshipIndex(esMap, relationship.getRelationTypeValue(), expectedResults);

            assertTrue(((List) esMap
                    .get(relationship.getRelationTypeValue() + ESMappingConstants.SUFFIX_ORDER))
                    .stream().allMatch(
                            child -> child.equals(childContentlet1.getIdentifier() + "_1") || child
                                    .equals(childContentlet2.getIdentifier() + "_2")));

        } finally {
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }

        }
    }

    @Test
    public void testLoadRelationshipFields_whenUsingTwoSidedFieldRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        Map<String, Object> esMap = new HashMap<>();

        String cardinality = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");

            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), cardinality);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField
                            .variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, cardinality);

            final Relationship relationship = relationshipAPI.byContentType(parentContentType)
                    .get(0);

            //creates child contentlet
            final ContentletDataGen childDataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet = childDataGen.languageId(language.getId()).nextPersisted();

            //creates parent contentlet
            final ContentletDataGen parentDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContentlet1 = contentletAPI
                    .checkin(parentDataGen.languageId(language.getId()).next(),
                            CollectionsUtils
                                    .map(relationship, CollectionsUtils.list(childContentlet)),
                            null, user, false);

            final Contentlet parentContentlet2 = contentletAPI
                    .checkin(parentDataGen.languageId(language.getId()).next(),
                            CollectionsUtils
                                    .map(relationship, CollectionsUtils.list(childContentlet)),
                            null, user, false);

            esMappingAPI.loadRelationshipFields(childContentlet, esMap);

            assertNotNull(esMap);

            final List<String> expectedResults = CollectionsUtils
                    .list(parentContentlet1.getIdentifier(), parentContentlet2.getIdentifier());

            validateRelationshipIndex(esMap, relationship.getRelationTypeValue(), expectedResults);

            validateRelationshipIndex(esMap, childContentType.variable() + StringPool.PERIOD
                    + childTypeRelationshipField.variable(), expectedResults);

            //parents cannot be ordered
            assertTrue(((List) esMap
                    .get(relationship.getRelationTypeValue() + ESMappingConstants.SUFFIX_ORDER))
                    .stream().allMatch(
                            parent -> parent.equals(parentContentlet1.getIdentifier() + "_1")
                                    || parent
                                    .equals(parentContentlet2.getIdentifier() + "_1")));


        } finally {
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }

        }
    }

    private void validateRelationshipIndex(final Map<String, Object> esMap, final String keyName,
            final List<String> identifiers) {

        final List results = List.class.cast(esMap.get(keyName));
        assertEquals(identifiers.size(), results.size());

        assertFalse(Collections.disjoint(results, identifiers));
    }

    private ContentType createAndSaveSimpleContentType(final String name)
            throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName,
            final String parentTypeId,
            final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return fieldAPI.save(field, user);
    }

}
