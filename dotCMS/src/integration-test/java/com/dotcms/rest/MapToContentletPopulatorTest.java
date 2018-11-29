package com.dotcms.rest;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

import java.util.Map.Entry;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class MapToContentletPopulatorTest {

    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        fieldAPI = APILocator.getContentTypeFieldAPI();
}

    @Test
    public void testPopulateLegacyRelationshipWithLuceneQueryAndIdentifier() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();
        final ContentType contentType = contentTypeAPI.find("Youtube");

        Contentlet contentlet = createContentlet(contentType);

        try {
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, contentType.id());
            properties.put("Products-Youtube", "+Youtube.url:new-youtube-content, " + contentlet.getIdentifier());

            contentlet = populator.populate(contentlet, properties);

            assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

            Map<Relationship, List<Contentlet>>  resultMap = (Map<Relationship, List<Contentlet>> ) contentlet
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(1, resultMap.size());

            Entry<Relationship, List<Contentlet>> result = resultMap.entrySet().iterator().next();

            //validates the relationship
            assertEquals("Products-Youtube", result.getKey().getRelationTypeValue());

            //validates the contentlet
            assertEquals(1, result.getValue().size());
            assertEquals(contentlet.getInode(), result.getValue().get(0).getInode());
        } finally {
            if(contentlet != null && contentlet.getInode() != null){
                ContentletDataGen.remove(contentlet);
            }
        }
    }

    @Test
    public void testPopulateOneSidedRelationshipWithIdentifier() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");
            Contentlet contentlet = createContentlet(parentContentType);

            Field field = createAndSaveRelationshipField("newRel", parentContentType.id(), childContentType.variable());
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, parentContentType.id());
            properties.put(field.variable(), contentlet.getIdentifier());

            contentlet = populator.populate(contentlet, properties);

            assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

            Map<Relationship, List<Contentlet>>  resultMap = (Map<Relationship, List<Contentlet>> ) contentlet
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(1, resultMap.size());

            Entry<Relationship, List<Contentlet>> result = resultMap.entrySet().iterator().next();

            //validates the relationship
            assertEquals(parentContentType.inode(), result.getKey().getParentStructureInode());

            //validates the contentlet
            assertEquals(1, result.getValue().size());
            assertEquals(contentlet.getInode(), result.getValue().get(0).getInode());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testPopulateTwoSidedRelationshipWithLuceneQuery() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");
            Contentlet contentlet = createContentlet(childContentType);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar);
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, childContentType.id());
            properties.put(childTypeRelationshipField.variable(), contentlet.getIdentifier());

            contentlet = populator.populate(contentlet, properties);

            assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

            Map<Relationship, List<Contentlet>>  resultMap = (Map<Relationship, List<Contentlet>> ) contentlet
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(1, resultMap.size());

            Entry<Relationship, List<Contentlet>> result = resultMap.entrySet().iterator().next();

            //validates the relationship
            assertEquals(childContentType.inode(), result.getKey().getChildStructureInode());

            //validates the contentlet
            assertEquals(1, result.getValue().size());
            assertEquals(contentlet.getInode(), result.getValue().get(0).getInode());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName, final String parentTypeId,
            final String childTypeVar)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values( String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    private Contentlet createContentlet(final ContentType contentType) {
        return new ContentletDataGen(contentType.id())
                .setProperty("widgetTitle", "New YouTube Content")
                .setProperty("url", "new-youtube-content").nextPersisted();
    }

}
