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
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ContentletIntegrationTest {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static HostAPI hostAPI;
    private static Language defaultLanguage;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;
    private static Host defaultHost;

    private final static String CARDINALITY = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());


    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        fieldAPI    = APILocator.getContentTypeFieldAPI();
        hostAPI     = APILocator.getHostAPI();
        languageAPI = APILocator.getLanguageAPI();

        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentletAPI   = APILocator.getContentletAPI();
        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
        defaultHost     = hostAPI.findDefaultHost(user, false);
        defaultLanguage = languageAPI.getDefaultLanguage();
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
                    CollectionsUtils.map(relationship, CollectionsUtils.list(childContentlet)), user,
                    false);

            //No cached value
            List<Contentlet> result = parentContentlet.getRelated(field.variable());

            assertEquals(1, result.size());
            assertEquals(childContentlet.getIdentifier(), result.get(0).getIdentifier());

            //Cached value
            result = parentContentlet.getRelated(field.variable());

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

            final List<Contentlet> result = contentlet.getRelated(field.variable());

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

            contentlet.getRelated("AnyField");

        }finally{
            contentTypeAPI.delete(contentType);
        }
    }

    private ContentType getNewContentType() throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        return contentTypeAPI.save(ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Test ContentType " + time)
                .host(defaultHost.getIdentifier())
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
