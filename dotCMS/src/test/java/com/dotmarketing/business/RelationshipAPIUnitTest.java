package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;

public class RelationshipAPIUnitTest {

    @Test
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws Exception {

        ContentType parent = ContentTypeBuilder.builder(SimpleContentType.class).id("parent").name("Parent").variable("parent").build();
        
        ContentType child = ContentTypeBuilder.builder(SimpleContentType.class).id("child").name("Child").variable("child").build();
        
        ContentType cousins = ContentTypeBuilder.builder(SimpleContentType.class).id("cousins").name("Cousins").variable("Cousins").build();
        
        
        
        Relationship relationship = new Relationship();
        relationship.setParentRelationName("Parent " + parent.name());
        relationship.setParentStructureInode(parent.id());
        relationship.setChildRelationName("Child " + child.name());
        relationship.setChildStructureInode(child.id());
        relationship.setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());
        
        RelationshipAPIImpl  api = new RelationshipAPIImpl(null);
        
        String newName = api.suggestNewFieldName(parent, relationship);
        assertEquals(newName, "Parents");
         newName = api.suggestNewFieldName(child, relationship);
        assertEquals(newName, "Childs");
        
        relationship.setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());
        
        newName = api.suggestNewFieldName(parent, relationship);
        assertEquals(newName, "Parent");
         newName = api.suggestNewFieldName(child, relationship);
        assertEquals(newName, "Childs");
        
        relationship.setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());
        
        newName = api.suggestNewFieldName(parent, relationship);
        assertEquals(newName, "Parent");
         newName = api.suggestNewFieldName(child, relationship);
        assertEquals(newName, "Child");
        
        relationship.setParentStructureInode(child.id());
        relationship.setChildStructureInode(cousins.id());
        relationship.setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());
        
        newName = api.suggestNewFieldName(child, relationship);
        assertEquals(newName, "Child");
         newName = api.suggestNewFieldName(cousins, relationship);
        assertEquals(newName, "Cousins");
        
        
        
    }
    
}
