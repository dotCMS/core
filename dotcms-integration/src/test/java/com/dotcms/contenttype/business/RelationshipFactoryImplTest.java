package com.dotcms.contenttype.business;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.test.ContentTypeBaseTest;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Test;

public class RelationshipFactoryImplTest extends ContentTypeBaseTest{

    private final RelationshipFactory relationshipFactory = RelationshipFactoryImpl.instance();
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

    /**
     * <b>Method to test:</b> {@link RelationshipFactory#dbRelatedContentByParent(String, String, boolean, String)}<br></br>
     * <b>Given Scenario:</b> A new relationship between a parent and a couple of children is created. Children contain multilingual versions<br></br>
     * <b>ExpectedResult:</b> Children must be returned respecting the relationship order (tree_order) and the multilingual versions
     * must be returned respecting the language_id order
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testGetDBRelatedChildrenMultilingualContent() throws DotSecurityException, DotDataException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final Language defaultLanguage = languageAPI.getDefaultLanguage();
        
        //Creating multiple languages
        final Language danish = new Language(System.currentTimeMillis(), "da", "DK ", "Danish", "Denmark");
        languageAPI.saveLanguage(danish);

        final Language german = new Language(System.currentTimeMillis(), "de", "DE", "German", "Germany");
        languageAPI.saveLanguage(german);
        
        //Creating relationship and relating content
        final Relationship relationship = saveRelationship();
        
        Contentlet parentContentlet = new ContentletDataGen(parentContentType.id()).next();
        final Contentlet childInDefaultLanguage = new ContentletDataGen(childContentType.id()).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet childInDanish = new ContentletDataGen(childContentType.id()).languageId(danish.getId()).nextPersisted();

        final ContentletRelationships contentletRelationships = new ContentletRelationships(
                parentContentlet);
        final ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, true);
        records.setRecords( CollectionsUtils.list(childInDefaultLanguage, childInDanish));
        contentletRelationships.getRelationshipsRecords().add(records);

        parentContentlet = contentletAPI.checkin(parentContentlet, contentletRelationships, null, null, user, false);

        //Adding multilingual versions of the children
        Contentlet childInGerman = contentletAPI.checkout(childInDefaultLanguage.getInode(), user, false);
        Contentlet child2InGerman = contentletAPI.checkout(childInDanish.getInode(), user, false);

        childInGerman.setLanguageId(german.getId());
        child2InGerman.setLanguageId(german.getId());
        childInGerman = contentletAPI.checkin(childInGerman, user, false);
        child2InGerman = contentletAPI.checkin(child2InGerman, user, false);

        TestDataUtils.waitForEmptyQueue();

        final List<Contentlet> contentletList = relationshipFactory.dbRelatedContentByParent(parentContentlet.getIdentifier(),relationship.getRelationTypeValue(),false,null);

        //Children must be returned in order
        assertEquals(4, contentletList.size());
        assertEquals(childInDefaultLanguage.getInode(), contentletList.get(0).getInode());
        assertEquals(childInGerman.getInode(), contentletList.get(1).getInode());
        assertEquals(childInDanish.getInode(), contentletList.get(2).getInode());
        assertEquals(child2InGerman.getInode(), contentletList.get(3).getInode());
    }


    /**
     * <b>Method to test:</b> {@link RelationshipFactory#dbRelatedContentByChild(String, String, boolean, String)}<br></br>
     * <b>Given Scenario:</b> A new relationship between a child and a couple of parent is created. Parents contain multilingual versions<br></br>
     * <b>ExpectedResult:</b> Parents must be returned respecting the relationship order (tree_order) and the multilingual versions
     * must be returned respecting the language_id order
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testGetDBRelatedParentsMultilingualContent() throws DotSecurityException, DotDataException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final Language defaultLanguage = languageAPI.getDefaultLanguage();

        //Creating multiple languages
        final Language danish = new Language(System.currentTimeMillis(), "da", "DK ", "Danish", "Denmark");
        languageAPI.saveLanguage(danish);

        final Language german = new Language(System.currentTimeMillis(), "de", "DE", "German", "Germany");
        languageAPI.saveLanguage(german);

        //Creating relationship and relating content
        final Relationship relationship = saveRelationship(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        Contentlet childContentlet = new ContentletDataGen(childContentType.id()).next();
        final Contentlet parentInDefaultLanguage = new ContentletDataGen(parentContentType.id()).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet parentInDanish = new ContentletDataGen(parentContentType.id()).languageId(danish.getId()).nextPersisted();

        ContentletRelationships contentletRelationships = new ContentletRelationships(
                childContentlet);
        ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, false);
        records.setRecords( CollectionsUtils.list(parentInDefaultLanguage, parentInDanish));
        contentletRelationships.getRelationshipsRecords().add(records);

        childContentlet = contentletAPI.checkin(childContentlet, contentletRelationships, null, null, user, false);

        //Adding multilingual versions of the parents
        Contentlet parentInGerman = contentletAPI.checkout(parentInDefaultLanguage.getInode(), user, false);
        Contentlet parent2InGerman = contentletAPI.checkout(parentInDanish.getInode(), user, false);

        parentInGerman.setLanguageId(german.getId());
        parent2InGerman.setLanguageId(german.getId());
        parentInGerman = contentletAPI.checkin(parentInGerman, user, false);
        parent2InGerman = contentletAPI.checkin(parent2InGerman, user, false);
        final AtomicReference<List<Contentlet>> atomicReference = new AtomicReference<>();

        // Sometimes we get 3 not for, but for some reason it's not consistent
        // Is this a timing issue or something else?
        Contentlet finalChildContentlet = childContentlet;
        try {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            List<Contentlet> contentletList = relationshipFactory.dbRelatedContentByChild(
                    finalChildContentlet.getIdentifier(),
                    relationship.getRelationTypeValue(),
                    false,
                    null);

            atomicReference.set(contentletList); // Set the current list in the atomic reference
            Logger.info(RelationshipFactoryImplTest.class, "Relationships size: " + contentletList.size()); // Log the current size
            return contentletList.size(); // Return the current size for Awaitility to check
        }, is(4)); // Check that the size is exactly 4

           } catch (ConditionTimeoutException e) {
                // This block will execute if the condition is not met within the timeout period
                List<Contentlet> failedList = atomicReference.get();
                if (failedList != null) {
                    Logger.error(RelationshipFactoryImplTest.class,"Condition timeout reached. Details of contentletList:");
                    for (Contentlet contentlet : failedList) {
                        System.out.println(contentlet.toString()); // Log the details of each contentlet
                    }
                } else {
                    Logger.error(RelationshipFactoryImplTest.class,"Condition timeout reached, but contentletList was null.");
                }
                throw e;
            }
        List<Contentlet> contentletList = atomicReference.get();

        assertEquals(parentInDefaultLanguage.getInode(), contentletList.get(0).getInode());
        assertEquals(parentInGerman.getInode(), contentletList.get(1).getInode());
        assertEquals(parentInDanish.getInode(), contentletList.get(2).getInode());
        assertEquals(parent2InGerman.getInode(), contentletList.get(3).getInode());
    }
    
    private Relationship saveRelationship() throws DotSecurityException, DotDataException {
        return saveRelationship(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());
    }

    private Relationship saveRelationship(final int cardinality) throws DotSecurityException, DotDataException {

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
        relationship.setCardinality(cardinality);
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
