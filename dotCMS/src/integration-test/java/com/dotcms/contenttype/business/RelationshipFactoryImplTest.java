package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.test.ContentTypeBaseTest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.List;
import org.junit.Test;

public class RelationshipFactoryImplTest extends ContentTypeBaseTest{

    private final RelationshipFactory relationshipFactory = FactoryLocator.getRelationshipFactory();
    private ContentType parentContentType = null;
    private ContentType childContentType = null;
    private final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

    @Test
    public void testByTypeValue_RelationshipsShouldBeSameRegardlessCase_SameRelationship() {
        List<Relationship> relationshipList = relationshipFactory.dbAll();
        for (Relationship relationship : relationshipList) {
            Relationship relationshipWithUpperCase = relationshipFactory.byTypeValue(relationship.getRelationTypeValue().toUpperCase());
            assertEquals(relationship,relationshipWithUpperCase);

        }
    }

    @Test
    public void testdbAll() throws DotDataException, DotSecurityException {
        try {
            List<Relationship> relationshipList = relationshipFactory.dbAll();
            final int amountOriginalRelationships = relationshipList.size();
            assertTrue(amountOriginalRelationships > 0);

            saveRelationship();
            relationshipList = relationshipFactory.dbAll();
            assertTrue(relationshipList.size() > amountOriginalRelationships);

            deleteRelationshipByInode(relationshipList.get(0).getInode());
            relationshipList = relationshipFactory.dbAll();
            assertEquals(amountOriginalRelationships,relationshipList.size());
        }finally {
            contentTypeAPI.delete(parentContentType);
            contentTypeAPI.delete(childContentType);
        }


    }

    @Test
    public void testDeleteByContentType() throws DotDataException, DotSecurityException {
        try {
            saveRelationship();
            List<Relationship> relationshipList = relationshipFactory.byContentType(parentContentType.inode());
            assertEquals(1, relationshipList.size());

            relationshipFactory.deleteByContentType(parentContentType);
            relationshipList = relationshipFactory.byContentType(childContentType.inode());
            assertEquals(0,relationshipList.size());
        }finally {
            contentTypeAPI.delete(parentContentType);
            contentTypeAPI.delete(childContentType);
        }
    }

    @Test
    public void testByContentType() throws DotDataException, DotSecurityException {
        try {
            saveRelationship();
            List<Relationship> relationshipList = relationshipFactory.byContentType(parentContentType.inode());
            assertEquals(1, relationshipList.size());

            deleteRelationshipByInode(relationshipList.get(0).getInode());
            relationshipList = relationshipFactory.byContentType(childContentType.inode());
            assertEquals(0,relationshipList.size());
        }finally {
            contentTypeAPI.delete(parentContentType);
            contentTypeAPI.delete(childContentType);
        }
    }

    private Relationship saveRelationship() throws DotSecurityException, DotDataException {

        parentContentType = ContentTypeBuilder
                .builder(BaseContentType.CONTENT.immutableClass())
                .folder(Folder.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST).name("ParentContentType" + System.currentTimeMillis())
                .owner(APILocator.systemUser().toString())
                .variable("PCTVariable" + System.currentTimeMillis()).build();
        parentContentType = contentTypeAPI.save(parentContentType);

        childContentType = ContentTypeBuilder
                .builder(BaseContentType.CONTENT.immutableClass())
                .folder(Folder.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST).name("ChildContentType" + System.currentTimeMillis())
                .owner(APILocator.systemUser().toString())
                .variable("CCTVariable" + System.currentTimeMillis()).build();
        childContentType = contentTypeAPI.save(childContentType);


        final Relationship relationship = new Relationship();
        relationship.setParentStructureInode(parentContentType.id());
        relationship.setChildStructureInode(childContentType.id());
        relationship.setParentRelationName(parentContentType.name());
        relationship.setChildRelationName(childContentType.name());
        relationship.setCardinality(0);
        relationship.setParentRequired(false);
        relationship.setChildRequired(false);
        relationship.setRelationTypeValue(parentContentType.name() + "-" + childContentType.name());

        relationshipFactory.save(relationship);

        return relationship;
    }

    private void deleteRelationshipByInode(String relationshipInode) throws DotDataException {
        relationshipFactory.delete(relationshipInode);
    }
}
