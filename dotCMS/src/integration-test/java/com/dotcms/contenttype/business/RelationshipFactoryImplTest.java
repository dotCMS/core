package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.test.ContentTypeBaseTest;

import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
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
    public void testByTypeValueCachesRelationship()
            throws DotCacheException, DotDataException, DotSecurityException {

        final Relationship relationship = saveRelationship();
        try {
            final RelationshipCache cache = CacheLocator.getRelationshipCache();
            assertNull(cache.getRelationshipByName(relationship.getRelationTypeValue()));
            relationshipFactory.byTypeValue(relationship.getRelationTypeValue());
            assertEquals(relationship,
                    cache.getRelationshipByName(relationship.getRelationTypeValue()));
        } finally {
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            } catch (Exception e) {}
        }
    }

    @Test
    public void testdbAll() throws DotDataException, DotSecurityException {
        try {
            for(int i=0; i<3; i++) {
                saveRelationship();
            }

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
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            } catch (Exception e) {}
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
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            } catch (Exception e) {}
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
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            } catch (Exception e) {}
        }
    }

    @Test
    public void test_dbRelatedContentByChild() throws DotDataException, DotSecurityException {
        try {
            final Relationship relationship = saveRelationship();
            final Contentlet parentContentlet = new ContentletDataGen(parentContentType.id()).nextPersisted();
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id()).nextPersisted();
            relationshipFactory.addRelationship(parentContentlet.getIdentifier(),childContentlet.getIdentifier(),relationship.getRelationTypeValue());

            final Contentlet contentlet = relationshipFactory.dbRelatedContentByChild(childContentlet.getIdentifier(),relationship.getRelationTypeValue(),false,"inode").get(0);

            assertEquals(parentContentlet.getIdentifier(),contentlet.getIdentifier());
            assertEquals(parentContentlet.getInode(),contentlet.getInode());
            assertEquals(parentContentlet.getHost(),contentlet.getHost());
            assertEquals(parentContentlet.getTitle(),contentlet.getTitle());

            deleteRelationshipByInode(relationship.getInode());


        }finally {
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            }catch (Exception e) {}
        }
    }

    @Test
    public void test_dbRelatedContentByParent() throws DotDataException, DotSecurityException {
        try {
            final Relationship relationship = saveRelationship();
            final Contentlet parentContentlet = new ContentletDataGen(parentContentType.id()).nextPersisted();
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id()).nextPersisted();
            relationshipFactory.addRelationship(parentContentlet.getIdentifier(),childContentlet.getIdentifier(),relationship.getRelationTypeValue());

            final Contentlet contentlet = relationshipFactory.dbRelatedContentByParent(parentContentlet.getIdentifier(),relationship.getRelationTypeValue(),false,"inode").get(0);

            assertEquals(childContentlet.getIdentifier(),contentlet.getIdentifier());
            assertEquals(childContentlet.getInode(),contentlet.getInode());
            assertEquals(childContentlet.getHost(),contentlet.getHost());
            assertEquals(childContentlet.getTitle(),contentlet.getTitle());

            deleteRelationshipByInode(relationship.getInode());


        }finally {
            try {
                contentTypeAPI.delete(parentContentType);
                contentTypeAPI.delete(childContentType);
            } catch (Exception e) {
                // Quiet
            }
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
