package com.dotcms.contenttype.business;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;

import java.util.List;
import java.util.Optional;

public interface RelationshipFactory {

    void deleteByContentType(final ContentTypeIf type) throws DotDataException;

    Relationship byInode(final String inode);

    List<Relationship> byParent(final ContentTypeIf parent) throws DotDataException;

    List<Relationship> byChild(final ContentTypeIf child) throws DotDataException;

    List<Relationship> dbAll();

    List<Relationship> dbAll(String orderBy);

    Relationship byTypeValue(final String typeValue);

    Optional<Relationship> byParentChildRelationName(ContentType contentType, String relationName);

    List<Relationship> dbAllByTypeValue(String typeValue);

    List<Relationship> byContentType(final String contentType);

    List<Relationship> byContentType(final ContentTypeIf contentType);

    List<Relationship> byContentType(final ContentTypeIf contentType, String orderBy);

    List<Relationship> byContentType(final String contentTypeInode, String orderBy);

    List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet)
            throws DotDataException;

    List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent) throws DotDataException;

    List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent, final boolean live, final String orderBy)
            throws DotDataException;

    List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent, final boolean live, final String orderBy, int limit, int offset)
            throws DotDataException;

    List<Tree> relatedContentTrees(final Relationship relationship, final Contentlet contentlet) throws  DotDataException;

    List<Tree> relatedContentTrees(final Relationship relationship, final Contentlet contentlet, final boolean hasParent) throws  DotDataException;

    boolean isChildField(Relationship relationship, Field field);

    /**
     * @deprecated For relationship fields use {@link RelationshipFactory#isChildField(Relationship, Field)} instead
     * @param relationship
     * @param contentTypeIf
     * @return
     */
    @Deprecated
    boolean isParent(final Relationship relationship, final ContentTypeIf contentTypeIf);

    /**
     * @deprecated For relationship fields use {@link RelationshipFactory#isParentField(Relationship, Field)} instead
     * @param relationship
     * @param contentTypeIf
     * @return
     */
    @Deprecated
    boolean isChild(final Relationship relationship, final ContentTypeIf contentTypeIf);

    boolean isParentField(Relationship relationship, Field field);

    boolean sameParentAndChild(final Relationship rel);

    void save(final Relationship relationship) throws DotDataException;

    void delete(final String inode) throws DotDataException;

    void delete(final Relationship relationship) throws DotDataException;

    void deleteKeepTrees(final Relationship relationship) throws DotDataException;

    List<Contentlet> dbRelatedContentByParent(final String parentInode, final String relationType, final boolean live,
            final String orderBy) throws DotDataException;

    List<Contentlet> dbRelatedContentByChild(final String childInode, final String relationType, final boolean live,
            final String orderBy) throws DotDataException;

    int maxSortOrder(final String parentInode, final String relationType);

    void deleteByContent(final Contentlet contentlet, final Relationship relationship,
            final List<Contentlet> relatedContentlets) throws DotDataException;

    void addRelationship(final String parent, final String child, final String relationType)throws DotDataException;

    List<Relationship> getOneSidedRelationships(final ContentTypeIf contentType, final int limit, final int offset) throws DotDataException;

    long getOneSidedRelationshipsCount(final ContentType contentType) throws DotDataException;

    Relationship dbByTypeValue(String typeValue);
}
