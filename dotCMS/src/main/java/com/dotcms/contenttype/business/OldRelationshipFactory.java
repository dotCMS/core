package com.dotcms.contenttype.business;



import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

@Deprecated
public class OldRelationshipFactory {

  private static RelationshipCache cache = CacheLocator.getRelationshipCache();

  // ### READ ###
  public static Relationship getRelationshipByInode(String inode) {
    Relationship rel = null;
    try {
      rel = cache.getRelationshipByInode(inode);
      if (rel != null)
        return rel;
    } catch (DotCacheException e) {
      Logger.error(OldRelationshipFactory.class, "Unable to access the cache to obtaion the relationship", e);
    }

    rel = (Relationship) InodeFactory.getInode(inode, Relationship.class);
    if (rel != null && InodeUtils.isSet(rel.getInode()))
      cache.putRelationshipByInode(rel);
    return rel;
  }


  @SuppressWarnings("unchecked")
  public static List<Relationship> getRelationshipsByParent(Structure parent) {
    List<Relationship> list = new ArrayList<Relationship>();
    String query = "select {relationship.*} from relationship, inode relationship_1_ "
        + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
        + "relationship.parent_structure_inode = ?";
    try {
      HibernateUtil dh = new HibernateUtil(Relationship.class);
      dh.setSQLQuery(query);
      dh.setParam(parent.getInode());
      list = dh.list();
    } catch (DotHibernateException e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  public static List<Relationship> getRelationshipsByChild(Structure child) {
    List<Relationship> list = new ArrayList<Relationship>();
    String query = "select {relationship.*} from relationship, inode relationship_1_ "
        + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
        + "relationship.child_structure_inode = ?";

    try {
      HibernateUtil dh = new HibernateUtil(Relationship.class);
      dh.setSQLQuery(query);
      dh.setParam(child.getInode());
      list = dh.list();
    } catch (DotHibernateException e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }
    return list;
  }

  public static List<Relationship> getAllRelationships() {
    String orderBy = "inode";
    return getRelationships(orderBy, null);
  }

  @SuppressWarnings("unchecked")
  public static List<Relationship> getRelationships(String orderBy, String structureId) {
    List<Relationship> list = new ArrayList<Relationship>();
    String query;
    if (structureId.equals("all")) {
      query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
          + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
          + "relationship.parent_structure_inode = parentstruct.inode and "
          + "relationship.child_structure_inode = childstruct.inode order by " + orderBy;
    } else {
      query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
          + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
          + "relationship.parent_structure_inode = parentstruct.inode and "
          + "relationship.child_structure_inode = childstruct.inode and (relationship.parent_structure_inode = '"
          + structureId + "' or relationship.child_structure_inode = '" + structureId + "')  order by " + orderBy;
    }
    try {
      HibernateUtil dh = new HibernateUtil(Relationship.class);
      dh.setSQLQuery(query);
      list = dh.list();
    } catch (DotHibernateException e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }
    return list;
  }

  public static Relationship getRelationshipbyTypeValue(String typeValue) {
    Relationship rel = null;
    try {
      rel = cache.getRelationshipByName(typeValue);
      if (rel != null)
        return rel;
    } catch (DotCacheException e) {
      Logger.error(OldRelationshipFactory.class, "Unable to access the cache to obtaion the relationship", e);
    }

    rel = (Relationship) InodeFactory.getInodeOfClassByCondition(Relationship.class,
        "relation_type_value = '" + typeValue + "'");
    if (rel != null && InodeUtils.isSet(rel.getInode()))
      cache.putRelationshipByInode(rel);
    return rel;

  }

  @SuppressWarnings("unchecked")
  public static List<Relationship> getAllRelationshipsByStructure(Structure st) {

    List<Relationship> list = null;

    try {
      list = cache.getRelationshipsByStruct(st);
    } catch (DotCacheException e1) {
      // Logger.debug(OldRelationshipFactory.class,e1.getMessage(),e1);
    }
    if (list == null) {
      String query = "select {relationship.*} from relationship, inode relationship_1_ "
          + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
          + "(relationship.parent_structure_inode = ? or relationship.child_structure_inode = ?)";
      try {
        HibernateUtil dh = new HibernateUtil(Relationship.class);
        dh.setSQLQuery(query);
        dh.setParam(st.getInode());
        dh.setParam(st.getInode());
        list = dh.list();
        cache.putRelationshipsByStruct(st, list);

      } catch (DotHibernateException e) {
        Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
      } catch (DotCacheException e) {
        Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
      }
    }
    return list;

  }

  @SuppressWarnings("unchecked")
  public static List<Relationship> getAllRelationshipsByStructure(Structure st, boolean hasParent) {
    List<Relationship> list = new ArrayList<Relationship>();
    String query = "select {relationship.*} from relationship, inode relationship_1_ "
        + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and ";
    if (hasParent)
      query += "(relationship.parent_structure_inode = ?)";
    else
      query += "(relationship.child_structure_inode = ?)";
    try {
      HibernateUtil dh = new HibernateUtil(Relationship.class);
      dh.setSQLQuery(query);
      dh.setParam(st.getInode());
      list = dh.list();
    } catch (DotHibernateException e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }
    return list;
  }

  public static List<Contentlet> getAllRelationshipRecords(Relationship relationship, Contentlet contentlet)
      throws DotStateException, DotDataException {
    String stInode = contentlet.getStructure().getInode();
    List<Contentlet> matches = new ArrayList<Contentlet>();
    if (relationship.getParentStructureInode().equalsIgnoreCase(stInode)) {
      matches = getAllRelationshipRecords(relationship, contentlet, true);
    } else if (relationship.getChildStructureInode().equalsIgnoreCase(stInode)) {
      matches = getAllRelationshipRecords(relationship, contentlet, false);
    }
    return matches;
  }

  public static List<Contentlet> getAllRelationshipRecords(Relationship relationship, Contentlet contentlet,
      boolean hasParent) throws DotStateException, DotDataException {
    return getAllRelationshipRecords(relationship, contentlet, hasParent, false, "tree_order");
  }

  public static List<Tree> getAllRelationshipTrees(Relationship relationship, Contentlet contentlet)
      throws DotStateException, DotDataException {
    String stInode = contentlet.getStructure().getInode();
    List<Tree> matches = new ArrayList<Tree>();
    if (relationship.getParentStructureInode().equalsIgnoreCase(stInode)) {
      matches = getAllRelationshipTrees(relationship, contentlet, true);
    } else if (relationship.getChildStructureInode().equalsIgnoreCase(stInode)) {
      matches = getAllRelationshipTrees(relationship, contentlet, false);
    }
    return matches;
  }

  @SuppressWarnings("deprecation")
  public static List<Tree> getAllRelationshipTrees(Relationship relationship, Contentlet contentlet, boolean hasParent)
      throws DotStateException, DotDataException {
    List<Tree> matches = new ArrayList<Tree>();
    List<Tree> trees = new ArrayList<Tree>();
    Identifier iden = APILocator.getIdentifierAPI().find(contentlet);
    if (hasParent) {
      trees = TreeFactory.getTreesByParentAndRelationType(iden, relationship.getRelationTypeValue());
      for (Tree tree : trees) {
        matches.add(tree);
      }
    } else {
      trees = TreeFactory.getTreesByChildAndRelationType(iden, relationship.getRelationTypeValue());
      for (Tree tree : trees) {
        matches.add(tree);
      }
    }
    return matches;
  }

  @SuppressWarnings("deprecation")
  public static List<Contentlet> getAllRelationshipRecords(Relationship relationship, Contentlet contentlet,
      boolean hasParent, boolean live, String orderBy) throws DotStateException, DotDataException {
    List<Contentlet> matches = new ArrayList<Contentlet>();

    if (contentlet == null || !InodeUtils.isSet(contentlet.getInode()))
      return matches;
    String iden = "";
    try {
      iden = APILocator.getIdentifierAPI().find(contentlet).getInode();
    } catch (DotHibernateException dhe) {
      Logger.error(OldRelationshipFactory.class, "Unable to retrive Identifier", dhe);
    }

    if (!InodeUtils.isSet(iden))
      return matches;

    if (hasParent) {
      if (live)
        matches = getRelatedContentByParent(iden, relationship.getRelationTypeValue(), true, orderBy);
      else
        matches = getRelatedContentByParent(iden, relationship.getRelationTypeValue(), false, orderBy);


    } else {
      if (live)
        matches = getRelatedContentByChild(iden, relationship.getRelationTypeValue(), true, orderBy);
      else
        matches = getRelatedContentByChild(iden, relationship.getRelationTypeValue(), false, orderBy);
    }
    return matches;
  }


  public static boolean isParent(Relationship rel, Structure st) {
    if (rel.getParentStructureInode().equalsIgnoreCase(st.getInode())
        && !(rel.getParentRelationName().equals(rel.getChildRelationName())
            && rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode())))
      return true;
    return false;
  }

  public static boolean isChild(Relationship rel, Structure st) {
    if (rel.getChildStructureInode().equalsIgnoreCase(st.getInode())
        && !(rel.getParentRelationName().equals(rel.getChildRelationName())
            && rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode())))
      return true;
    return false;
  }

  public static boolean isSameStructureRelationship(Relationship rel, Structure st) {
    if (rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode()))
      return true;
    return false;
  }

  public static boolean isSameStructureRelationship(Relationship rel) {
    if (rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode()))
      return true;
    return false;
  }

  // ### CREATE AND UPDATE
  public static void saveRelationship(Relationship relationship) throws DotHibernateException {
    HibernateUtil.saveOrUpdate(relationship);
    CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
    try {
      CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
      CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
    } catch (Exception e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }



  }

  /**
   * ISSUE 2222: https://github.com/dotCMS/dotCMS/issues/2222
   * 
   * @author Graziano Aliberti Mar 4, 2013 - 4:54:16 PM
   */
  public static void saveRelationship(Relationship relationship, String inode) throws DotHibernateException {
    Date now = new Date();
    relationship.setiDate(now);
    HibernateUtil.saveWithPrimaryKey(relationship, inode);
  }

  // ### DELETE ###
  public static void deleteRelationship(String inode) throws DotHibernateException {
    Relationship relationship = getRelationshipByInode(inode);
    deleteRelationship(relationship);
  }

  public static void deleteRelationship(Relationship relationship) throws DotHibernateException {
    InodeFactory.deleteInode(relationship);
    CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
    try {
      CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
      CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
    } catch (DotCacheException e) {
      Logger.error(OldRelationshipFactory.class, e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Contentlet> getRelatedContentByParent(String parentInode, String relationType, boolean live,
      String orderBy) {
    try {

      HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

      String sql =
          "SELECT {contentlet.*} from contentlet contentlet, inode contentlet_1_, contentlet_version_info vi, tree tree1 "
              + "where tree1.parent = ? and tree1.relation_type = ?  " + "and tree1.child = contentlet.identifier "
              + "and contentlet.inode = contentlet_1_.inode and vi.identifier=contentlet.identifier " + "and "
              + ((live) ? "vi.live_inode" : "vi.working_inode") + " = contentlet.inode ";

      if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
        sql = sql + " order by contentlet." + orderBy;
      } else {
        sql = sql + " order by tree1.tree_order";
      }


      Logger.debug(OldRelationshipFactory.class, "sql:  " + sql + "\n");
      Logger.debug(OldRelationshipFactory.class, "parentInode:  " + parentInode + "\n");
      Logger.debug(OldRelationshipFactory.class, "relationType:  " + relationType + "\n");
      dh.setSQLQuery(sql);
      dh.setParam(parentInode);
      dh.setParam(relationType);

      List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
      List<Contentlet> conResult = new ArrayList<Contentlet>();
      ESContentFactoryImpl conFac = new ESContentFactoryImpl();
      for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
        conResult.add(conFac.convertFatContentletToContentlet(fatty));
      }
      return conResult;
    } catch (Exception e) {
      Logger.error(OldRelationshipFactory.class, "getChildrenClass failed:" + e, e);
      throw new DotRuntimeException(e.toString());
    }

  }

  /**
   * This method can be used to find the next in a sort order
   * 
   * @param parentInode The parent Relationship
   * @param relationType
   * @return the max in the sort order
   */
  public static int getMaxInSortOrder(String parentInode, String relationType) {
    try {

      DotConnect db = new DotConnect();

      String sql = "SELECT max(tree_order) as tree_order from tree tree1 "
          + "where tree1.parent = ? and tree1.relation_type = ? ";

      db.setSQL(sql);
      db.addParam(parentInode);
      db.addParam(relationType);

      int x = db.getInt("tree_order");

      return x;
    } catch (Exception e) {
      Logger.debug(InodeFactory.class, "getMaxInSortOrder failed:" + e, e);
    }
    return 0;

  }



  @SuppressWarnings("unchecked")
  public static List<Contentlet> getRelatedContentByChild(String childInode, String relationType, boolean live,
      String orderBy) {
    try {

      HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

      String sql = "SELECT {contentlet.*} " + "from contentlet " + "join inode contentlet_1_ "
          + "on (contentlet.inode = contentlet_1_.inode) " + "join contentlet_version_info vi " + "on (vi."
          + (live ? "live" : "working") + "_inode = contentlet.inode) " + "join tree "
          + "on (tree.parent=contentlet.identifier) " + "where " + "  tree.child = ? " + "  and tree.relation_type = ?";


      if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
        sql = sql + " order by contentlet." + orderBy;
      } else {
        sql = sql + " order by tree.tree_order";
      }

      Logger.debug(OldRelationshipFactory.class, "sql:  " + sql + "\n");
      Logger.debug(OldRelationshipFactory.class, "childInode:  " + childInode + "\n");
      Logger.debug(OldRelationshipFactory.class, "relationType:  " + relationType + "\n");
      dh.setSQLQuery(sql);
      dh.setParam(childInode);
      dh.setParam(relationType);

      List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
      List<Contentlet> conResult = new ArrayList<Contentlet>();
      ESContentFactoryImpl conFac = new ESContentFactoryImpl();
      for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
        conResult.add(conFac.convertFatContentletToContentlet(fatty));
      }
      return conResult;
    } catch (Exception e) {
      Logger.error(OldRelationshipFactory.class, "getChildrenClass failed:" + e, e);
      throw new DotRuntimeException(e.toString());
    }

  }

  public static List<Contentlet> getAllRelationshipRecords(Relationship relationship, Contentlet contentlet,
      boolean hasParent, boolean live) throws DotStateException, DotDataException {
    return getAllRelationshipRecords(relationship, contentlet, hasParent, live, "");
  }

  /**
   * This method retrieves all the related contenlets and regardless if it has to retrieve parents,
   * children or siblings
   * 
   * @param relationship
   * @param contentlet
   * @param orderBy
   * @return
   */
  public static List<Contentlet> getRelatedContentlets(Relationship relationship, Contentlet contentlet, String orderBy,
      String sqlCondition, boolean liveContent) {

    return getRelatedContentlets(relationship, contentlet, orderBy, sqlCondition, liveContent, 0);

  }

  /**
   * Removes the relationships from the list of related contentlets to the passed in contentlet
   * 
   * @param contentlet
   * @param relationship
   * @param relatedContentlets
   * @throws DotDataException
   */
  public static void deleteRelationships(Contentlet contentlet, Relationship relationship,
      List<Contentlet> relatedContentlets) throws DotDataException {
    Tree t = new Tree();
    for (Contentlet con : relatedContentlets) {

      t = TreeFactory.getTree(contentlet.getIdentifier(), con.getIdentifier(), relationship.getRelationTypeValue());
      if (InodeUtils.isSet(t.getChild()) & InodeUtils.isSet(t.getParent())) {
        TreeFactory.deleteTree(t);
      } else {
        t = TreeFactory.getTree(con.getIdentifier(), contentlet.getIdentifier(), relationship.getRelationTypeValue());
        if (InodeUtils.isSet(t.getChild()) & InodeUtils.isSet(t.getParent())) {
          TreeFactory.deleteTree(t);
        }
      }
    }
  }

  /**
   * This method retrieves all the related contenlets and regardless if it has to retrieve parents,
   * children or siblings
   * 
   * @param relationship
   * @param contentlet
   * @param orderBy
   * @param sqlCondition
   * @param liveContent
   * @param limit
   * @return
   */
  @SuppressWarnings("unchecked")
  public static List<Contentlet> getRelatedContentlets(Relationship relationship, Contentlet contentlet, String orderBy,
      String sqlCondition, boolean liveContent, int limit) {

    List<Contentlet> matches = new ArrayList<Contentlet>();

    if (contentlet == null || !InodeUtils.isSet(contentlet.getInode())) {
      return matches;
    }

    try {

      Identifier iden = APILocator.getIdentifierAPI().find(contentlet);
      if (iden == null || !InodeUtils.isSet(iden.getInode()))
        return matches;

      HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

      String sql =
          "SELECT {contentlet.*} from contentlet contentlet, inode contentlet_1_, tree relationshipTree, identifier iden, tree identifierTree, contentlet_version_info vi "
              + "where (relationshipTree.child = ? or relationshipTree.parent = ?) and relationshipTree.relation_type = ? "
              + "and (iden.id = relationshipTree.parent or iden.id = relationshipTree.child) "
              + "and (iden.id = identifierTree.parent and identifierTree.child = contentlet_1_.inode) "
              + "and contentlet.inode = contentlet_1_.inode and contentlet.inode <> ? ";
      if (liveContent)
        sql += "and contentlet.inode = vi.live_inode ";
      else
        sql += "and contentlet.inode = vi.working_inode ";

      if (UtilMethods.isSet(sqlCondition))
        sql += "and " + sqlCondition;

      if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
        sql = sql + " order by contentlet." + orderBy;
      } else {
        sql = sql + " order by relationshipTree.tree_order";
      }

      Logger.debug(OldRelationshipFactory.class, "sql:  " + sql + "\n");

      dh.setSQLQuery(sql);
      dh.setParam(iden.getInode());
      dh.setParam(iden.getInode());
      dh.setParam(relationship.getRelationTypeValue());
      dh.setParam(contentlet.getInode());

      if (limit > 0) {
        dh.setMaxResults(limit);
      }

      List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
      List<Contentlet> conResult = new ArrayList<Contentlet>();
      ESContentFactoryImpl conFac = new ESContentFactoryImpl();
      for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
        conResult.add(conFac.convertFatContentletToContentlet(fatty));
      }

      return new ArrayList<Contentlet>(new LinkedHashSet<Contentlet>(conResult));

    } catch (Exception e) {
      Logger.error(OldRelationshipFactory.class, "getChildrenClass failed:" + e, e);
      throw new DotRuntimeException(e.toString());
    }

  }


}
