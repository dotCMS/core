package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RelationshipAPI {

  void deleteByContentType(ContentTypeIf type) throws DotDataException;

  Relationship byInode(String inode);

  /**
   * Search a list of relationships that contain the string typeValue.
   * @param typeValue
   * @return
   */
  List<Relationship> dbAllByTypeValue(final String typeValue);

  /**
   * Returns an {@link Optional} of {@link Relationship} for the given content type (parent or child)
   * and the given relationName (child relation name or parent relation name)
   * @param contentType
   * @param relationName
   * @return
   */
  Optional<Relationship> byParentChildRelationName(ContentType contentType,
          String relationName);

    List<Relationship> byParent(ContentTypeIf parent) throws DotDataException;

  List<Relationship> byChild(ContentTypeIf child) throws DotDataException;

  Relationship byTypeValue(String typeValue);

  List<Relationship> byContentType(ContentTypeIf type) throws DotDataException;

  List<Relationship> byContentType(ContentTypeIf type, String orderBy);

  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet)
          throws DotDataException;

  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet,
                                    boolean hasParent) throws DotDataException;

  void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets)
      throws DotDataException;

  List<Relationship> byContentType(ContentTypeIf st, boolean hasParent) throws DotDataException;

  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws DotDataException;

  void delete(String inode) throws DotDataException;

  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent)
      throws DotDataException;


  /**
   * Save or Update the relationship depending on whether the relationship inode exists
   * @param relationship
   * @throws DotDataException
   */
  void save(Relationship relationship) throws DotDataException;

  /**
   * Saves the relationship with the inode provided
   * @param relationship
   * @param inode
   * @throws DotDataException
   */
  void save(Relationship relationship, String inode) throws DotDataException;

  /**
   * Creates a new relationship, generating a new inode as primary key
   * @throws DotDataException
   */
  void create(Relationship relationship) throws DotDataException;

  boolean sameParentAndChild(Relationship rel);

    /**
     * @deprecated For relationship fields use {@link RelationshipAPI#isParentField(Relationship, com.dotcms.contenttype.model.field.Field)} instead
     * @param rel
     * @param st
     * @return
     */
    @Deprecated
  boolean isChild(Relationship rel, ContentTypeIf st);

  boolean isParentField(Relationship rel, com.dotcms.contenttype.model.field.Field field);

  int maxSortOrder(String parentInode, String relationType);

    /**
     * @deprecated For relationship fields use {@link RelationshipAPI#isChildField(Relationship, com.dotcms.contenttype.model.field.Field)} instead
     * @param rel
     * @param st
     * @return
     */
    @Deprecated
  boolean isParent(Relationship rel, ContentTypeIf st);

  boolean isChildField(Relationship rel, com.dotcms.contenttype.model.field.Field field);

  void delete(Relationship relationship) throws DotDataException;

  /**
   * Method to delete a relationship, but keep the TypeValue in Tree and multitree
   * @param relationship to be deleted
   * @throws DotDataException
   */
  void deleteKeepTrees(Relationship relationship) throws DotDataException;

  void addRelationship(String parent, String child, String relationType) throws DotDataException;

  List<Relationship> getOneSidedRelationships(final ContentType contentType, final int limit,
          final int offset) throws DotDataException;

  long getOneSidedRelationshipsCount(final ContentType contentType) throws DotDataException;

  ContentletRelationships getContentletRelationshipsFromMap(final Contentlet contentlet, final Map<Relationship,
          List<Contentlet>> contentRelationships);

  /**
   * Given a Relationship Field, returns the existing relationship
   * @param field
   * @param user
   * @return
   */
  Relationship getRelationshipFromField(Field field, final User user) throws DotDataException, DotSecurityException;

  /**
   * Given a Relationship Field, returns the existing relationship
   * @param field
   * @param user
   * @return
   */
  Relationship getRelationshipFromField(final com.dotcms.contenttype.model.field.Field field, final User user)
          throws DotDataException, DotSecurityException;

  /**
   * It converts an old Relationship to the new Relationship Field
   * @param oldRelationship
   * @throws DotDataException
   * @throws DotSecurityException
   */
  void convertRelationshipToRelationshipField(final Relationship oldRelationship) throws DotDataException, DotSecurityException;

    /**
     * Returns all relationships existing in DB
     * @return
     */
    List<Relationship> dbAll();
}
