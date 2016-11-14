package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;

public interface RelationshipFactory {

  void deleteByContentType(ContentTypeIf type) throws DotDataException;

  Relationship byInode(String inode);

  List<Relationship> byParent(ContentTypeIf parent) throws DotDataException;

  List<Relationship> byChild(ContentTypeIf child) throws DotDataException;

  Relationship byTypeValue(String typeValue);

  List<Relationship> byContentType(ContentTypeIf type) throws DotDataException;

  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet) throws DotDataException;

  void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets)
      throws DotDataException;

  List<Relationship> byContentType(ContentTypeIf st, boolean hasParent);

  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws DotDataException;

  void delete(String inode) throws DotDataException;

  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent)
      throws DotDataException;

  void save(Relationship relationship) throws DotDataException;

  void save(Relationship relationship, String inode) throws DotDataException;

  boolean sameParentAndChild(Relationship rel);

  boolean isChild(Relationship rel, ContentTypeIf st);

  int maxSortOrder(String parentInode, String relationType);

  boolean isParent(Relationship rel, ContentTypeIf st);

  void delete(Relationship relationship) throws DotDataException;

  void addRelationship(String parent, String child, String relationType) throws DotDataException;

  List<Relationship> byContentType(ContentTypeIf type, String orderBy);



}
