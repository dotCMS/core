package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import java.util.List;

import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;

public interface RelationshipAPI {

  void deleteByContentType(ContentTypeIf type) throws DotDataException;

  Relationship byInode(String inode);

  /**
   * Search a list of relationships that contain the string typeValue.
   * @param typeValue
   * @return
   */
  List<Relationship> dbAllByTypeValue(final String typeValue);

    List<Relationship> byParent(ContentTypeIf parent) throws DotDataException;

  List<Relationship> byChild(ContentTypeIf child) throws DotDataException;

  Relationship byTypeValue(String typeValue);

  List<Relationship> byContentType(ContentTypeIf type) throws DotDataException;

  List<Relationship> byContentType(ContentTypeIf type, String orderBy);

  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet) throws DotDataException;

  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet,
                                    boolean hasParent) throws  DotDataException;

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

  boolean isChild(Relationship rel, ContentTypeIf st);

  int maxSortOrder(String parentInode, String relationType);

  boolean isParent(Relationship rel, ContentTypeIf st);

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
}
