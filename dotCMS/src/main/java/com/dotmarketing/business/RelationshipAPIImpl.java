package com.dotmarketing.business;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.RelationshipFactoryImpl;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;

import java.util.List;

// THIS IS A FAKE API SO PEOPLE CAN FIND AND USE THE RELATIONSHIPFACTORY
public class RelationshipAPIImpl extends RelationshipFactoryImpl implements RelationshipAPI {

    @WrapInTransaction
    @Override
    public void deleteByContentType(ContentTypeIf type) throws DotDataException {
        super.deleteByContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byInode(String inode) {
        return super.byInode(inode);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byParent(ContentTypeIf parent) throws DotHibernateException {
        return super.byParent(parent);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byChild(ContentTypeIf child) throws DotHibernateException {
        return super.byChild(child);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> dbAll() throws DotHibernateException {
        return super.dbAll();
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> dbAll(String orderBy, String contentTypeInode) throws DotHibernateException {
        return super.dbAll(orderBy, contentTypeInode);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byTypeValue(String typeValue) {
        return super.byTypeValue(typeValue);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type) throws DotDataException {
        return super.byContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf st, boolean hasParent) {
        return super.byContentType(st, hasParent);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type, String orderBy) {
        return super.byContentType(type, orderBy);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet);
    }

    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, boolean hasParent) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet, hasParent);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws DotDataException {
        return super.relatedContentTrees(relationship, contentlet);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent) throws DotDataException {
        return super.relatedContentTrees(relationship, contentlet, hasParent);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, boolean hasParent, boolean live, String orderBy) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet, hasParent, live, orderBy);
    }

    @Override
    public boolean isParent(Relationship rel, ContentTypeIf st) {
        return super.isParent(rel, st);
    }

    @Override
    public boolean isChild(Relationship rel, ContentTypeIf st) {
        return super.isChild(rel, st);
    }

    @Override
    public boolean sameParentAndChild(Relationship rel) {
        return super.sameParentAndChild(rel);
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship) throws DotHibernateException {
        super.save(relationship);
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship, String inode) throws DotHibernateException {
        super.save(relationship, inode);
    }

    @WrapInTransaction
    @Override
    public void delete(String inode) throws DotHibernateException {
        super.delete(inode);
    }

    @WrapInTransaction
    @Override
    public void delete(Relationship relationship) throws DotHibernateException {
        super.delete(relationship);
    }

    @WrapInTransaction
    @Override
    public void deleteKeepTrees(Relationship relationship) throws DotHibernateException {
        super.deleteKeepTrees(relationship);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContentByParent(String parentInode, String relationType, boolean live, String orderBy) throws DotDataException {
        return super.dbRelatedContentByParent(parentInode, relationType, live, orderBy);
    }

    @CloseDBIfOpened
    @Override
    public int maxSortOrder(String parentInode, String relationType) {
        return super.maxSortOrder(parentInode, relationType);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContentByChild(String childInode, String relationType, boolean live, String orderBy) throws DotDataException {
        return super.dbRelatedContentByChild(childInode, relationType, live, orderBy);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, boolean hasParent, boolean live) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet, hasParent, live);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, String orderBy, String sqlCondition, boolean liveContent) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet, orderBy, sqlCondition, liveContent);
    }

    @WrapInTransaction
    @Override
    public void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets) throws DotDataException {
        super.deleteByContent(contentlet, relationship, relatedContentlets);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, String orderBy, String sqlCondition, boolean liveContent, int limit) throws DotDataException {
        return super.dbRelatedContent(relationship, contentlet, orderBy, sqlCondition, liveContent, limit);
    }

    @WrapInTransaction
    @Override
    public void addRelationship(String parent, String child, String relationType) throws DotDataException {
        super.addRelationship(parent, child, relationType);
    }
}
