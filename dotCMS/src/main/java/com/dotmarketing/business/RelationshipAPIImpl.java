package com.dotmarketing.business;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.RelationshipFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;

import com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformer;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Map;

// THIS IS A FAKE API SO PEOPLE CAN FIND AND USE THE RELATIONSHIPFACTORY
public class RelationshipAPIImpl implements RelationshipAPI {

    private final RelationshipFactory relationshipFactory;
    
    public RelationshipAPIImpl() {
        this.relationshipFactory = FactoryLocator.getRelationshipFactory();
    }

    @WrapInTransaction
    @Override
    public void deleteByContentType(ContentTypeIf type) throws DotDataException {
        this.relationshipFactory.deleteByContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byInode(String inode) {
        return this.relationshipFactory.byInode(inode);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byParent(ContentTypeIf parent) throws DotDataException {
        return this.relationshipFactory.byParent(parent);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byChild(ContentTypeIf child) throws DotDataException {
        return this.relationshipFactory.byChild(child);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byTypeValue(String typeValue) {
        return this.relationshipFactory.byTypeValue(typeValue);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type) throws DotDataException {
        return this.relationshipFactory.byContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf st, boolean hasParent) throws DotDataException{
        if(hasParent) {
            return this.relationshipFactory.byParent(st);
        }else{
            return this.relationshipFactory.byChild(st);
        }
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type, String orderBy){
        return this.relationshipFactory.byContentType(type, orderBy);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet)
            throws DotDataException {
        return this.relationshipFactory.dbRelatedContent(relationship, contentlet);
    }

    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, boolean hasParent)
            throws DotDataException {
        return this.relationshipFactory.dbRelatedContent(relationship, contentlet, hasParent);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws DotDataException {
        return this.relationshipFactory.relatedContentTrees(relationship, contentlet);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent) throws DotDataException {
        return this.relationshipFactory.relatedContentTrees(relationship, contentlet, hasParent);
    }

    @Override
    public boolean isParent(Relationship rel, ContentTypeIf st) {
        return this.relationshipFactory.isParent(rel, st);
    }

    @Override
    public boolean isChild(Relationship rel, ContentTypeIf st) {
        return this.relationshipFactory.isChild(rel, st);
    }

    @Override
    public boolean sameParentAndChild(Relationship rel) {
        return this.relationshipFactory.sameParentAndChild(rel);
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship) throws DotDataException {
        checkReadOnlyFields(relationship, relationship.getInode());
        this.relationshipFactory.save(relationship);
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship, String inode) throws DotDataException {
        checkReadOnlyFields(relationship, inode);
        this.relationshipFactory.save(relationship);
    }

    private void checkReadOnlyFields(final Relationship relationship, final String inode)
            throws DotDataException {
        if (UtilMethods.isSet(inode) && UtilMethods.isSet(relationship)) {

            //Check if the relationship already exists
            Relationship currentRelationship = this.relationshipFactory.byInode(inode);
            if (UtilMethods.isSet(currentRelationship) && UtilMethods.isSet(currentRelationship.getInode())) {

                //Check the parent has not been changed
                if (!relationship.getParentStructureInode().equals(currentRelationship.getParentStructureInode())) {
                    throw new DotValidationException("error.relationship.parent.structure.cannot.be.changed");
                }

                //Check the child has not been changed
                if (!relationship.getChildStructureInode().equals(currentRelationship.getChildStructureInode())) {
                    throw new DotValidationException("error.relationship.child.structure.cannot.be.changed");
                }

                //Check the typeValue has not been changed
                if (!relationship.getRelationTypeValue().equals(currentRelationship.getRelationTypeValue())) {
                    throw new DotValidationException("error.relationship.type.value.cannot.be.changed");
                }

            }
        }
    }

    @WrapInTransaction
    @Override
    public void create(Relationship relationship) throws DotDataException {
        this.relationshipFactory.save(relationship);
    }

    @WrapInTransaction
    @Override
    public void delete(String inode) throws DotDataException {
        this.relationshipFactory.delete(inode);
    }

    @WrapInTransaction
    @Override
    public void delete(Relationship relationship) throws DotDataException {
        this.relationshipFactory.delete(relationship);
    }

    @WrapInTransaction
    @Override
    public void deleteKeepTrees(Relationship relationship) throws DotDataException {
        this.relationshipFactory.deleteKeepTrees(relationship);
    }

    @CloseDBIfOpened
    @Override
    public int maxSortOrder(String parentInode, String relationType) {
        return this.relationshipFactory.maxSortOrder(parentInode, relationType);
    }

    @WrapInTransaction
    @Override
    public void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets) throws DotDataException {
        this.relationshipFactory.deleteByContent(contentlet, relationship, relatedContentlets);
    }

    @WrapInTransaction
    @Override
    public void addRelationship(String parent, String child, String relationType) throws DotDataException {
        this.relationshipFactory.addRelationship(parent, child, relationType);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> getOneSidedRelationships(final ContentType contentType,
            final int limit, final int offset) throws DotDataException {
        return this.relationshipFactory.getOneSidedRelationships(contentType, limit, offset);
    }

    @Override
    public ContentletRelationships getContentletRelationshipsFromMap(Contentlet contentlet, final Map<Relationship, List<Contentlet>> contentRelationships) {
        return new ContentletRelationshipsTransformer(contentlet, contentRelationships).findFirst();
    }
}
