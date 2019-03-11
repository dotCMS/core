package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.business.RelationshipFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;
import org.junit.Test;
import org.mockito.Mockito;

public class RelationshipAPIUnitTest {

    @Test
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException() {

        final ContentType parent = ContentTypeBuilder.builder(SimpleContentType.class).id("parent")
                .name("Parent").variable("parent").build();

        final ContentType child = ContentTypeBuilder.builder(SimpleContentType.class).id("child")
                .name("Child").variable("child").build();

        final ContentType cousins = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("cousins").name("Cousins").variable("Cousins").build();

        final Relationship relationship = new Relationship();
        relationship.setParentRelationName("Parent " + parent.name());
        relationship.setParentStructureInode(parent.id());
        relationship.setChildRelationName("Child " + child.name());
        relationship.setChildStructureInode(child.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        final RelationshipFactory relationshipFactory = Mockito.mock(RelationshipFactory.class);
        Mockito.when(relationshipFactory.sameParentAndChild(relationship)).thenReturn(false);

        final RelationshipAPIImpl api = new RelationshipAPIImpl(relationshipFactory);

        String newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parents", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Childs", newName);

        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parent", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Childs", newName);

        relationship
                .setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parent", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Child", newName);

        relationship.setParentStructureInode(child.id());
        relationship.setChildStructureInode(cousins.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(child, relationship, false);
        assertEquals("Child", newName);
        newName = api.suggestNewFieldName(cousins, relationship, true);
        assertEquals("Cousins", newName);
    }

    @Test
    public void testSave_SelfRelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws Exception {

        final ContentType contentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("Testcontenttype").name("Testcontenttype").variable("Testcontenttype").build();

        final Relationship relationship = new Relationship();
        relationship.setParentRelationName("Parent " + contentType.name());
        relationship.setParentStructureInode(contentType.id());
        relationship.setChildRelationName("Child " + contentType.name());
        relationship.setChildStructureInode(contentType.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        final RelationshipFactory relationshipFactory = Mockito.mock(RelationshipFactory.class);
        Mockito.when(relationshipFactory.sameParentAndChild(relationship)).thenReturn(true);

        final RelationshipAPIImpl api = new RelationshipAPIImpl(relationshipFactory);

        String newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParents", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChildren", newName);

        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParent", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChildren", newName);

        relationship
                .setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParent", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChild", newName);
    }
    
}
