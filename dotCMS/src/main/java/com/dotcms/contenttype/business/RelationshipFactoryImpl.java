package com.dotcms.contenttype.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.contenttype.business.sql.RelationshipSQL;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

public class RelationshipFactoryImpl implements RelationshipFactory{

	static RelationshipSQL sql= RelationshipSQL.getInstance();
	
	@Override
	public void deleteByContentType(ContentTypeIf type) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.DELETE_RELATIONSHIP);
		dc.addParam(type.id());
		dc.addParam(type.id());
		dc.loadResults();

	}
	private static RelationshipCache cache = CacheLocator.getRelationshipCache();

    @Override
    public  Relationship byInode(String inode) {
		Relationship rel = null;
		try {
			rel = cache.getRelationshipByInode(inode);
			if(rel != null)
				return rel;
		} catch (DotCacheException e) {
			Logger.debug(this.getClass(), "Unable to access the cache to obtaion the relationship", e);
		}

		rel = (Relationship) InodeFactory.getInode(inode, Relationship.class);
		if(rel!= null && InodeUtils.isSet(rel.getInode()))
			cache.putRelationshipByInode(rel);
		return rel;
    }

    @Override
    @SuppressWarnings("unchecked")
	public  List<Relationship> byParent (ContentTypeIf parent) throws DotHibernateException {
        List<Relationship> list =new ArrayList<Relationship>();
        String query = "select {relationship.*} from relationship, inode relationship_1_ "
                + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
                + "relationship.parent_structure_inode = ?";

    	HibernateUtil dh = new HibernateUtil(Relationship.class);
		dh.setSQLQuery(query);
		dh.setParam(parent.id());
		list = dh.list();

        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
	public  List<Relationship> byChild (ContentTypeIf child) throws DotHibernateException {
        List<Relationship> list = new ArrayList<Relationship>();
        String query = "select {relationship.*} from relationship, inode relationship_1_ "
                + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
                + "relationship.child_structure_inode = ?";

    	HibernateUtil dh = new HibernateUtil(Relationship.class);
		dh.setSQLQuery(query);
		dh.setParam(child.id());
		list = dh.list();

        return list;
    }
    
    public  List<Relationship> dbAll() throws DotHibernateException {
        String orderBy = "inode";
        return dbAll(orderBy,"all");
    }

    @SuppressWarnings("unchecked")
    public  List<Relationship> dbAll(String orderBy, String contentTypeInode) throws DotHibernateException {
        List<Relationship> list = new ArrayList<Relationship>();
        String query;
        if("all".equals(contentTypeInode)){
           query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
                    + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
                    + "relationship.parent_structure_inode = parentstruct.inode and "
                    + "relationship.child_structure_inode = childstruct.inode order by " + orderBy;
        }else{
           query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
                    + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
                    + "relationship.parent_structure_inode = parentstruct.inode and "
                    + "relationship.child_structure_inode = childstruct.inode and (relationship.parent_structure_inode = '" +contentTypeInode
                    + "' or relationship.child_structure_inode = '"+contentTypeInode +"')  order by " + orderBy;
        }

		HibernateUtil dh = new HibernateUtil(Relationship.class);
		dh.setSQLQuery(query);
		list = dh.list();

        return list;
    }
    @Override
    public  Relationship byTypeValue(String typeValue) {
		Relationship rel = null;
		try {
			rel = cache.getRelationshipByName(typeValue);
			if(rel != null)
				return rel;
		} catch (DotCacheException e) {
			Logger.debug(this.getClass(), "Unable to access the cache to obtaion the relationship", e);
		}

		rel = (Relationship) InodeFactory.getInodeOfClassByCondition(Relationship.class, "relation_type_value = '"
                + typeValue + "'");
		if(rel!= null && InodeUtils.isSet(rel.getInode()))
			cache.putRelationshipByInode(rel);
		return rel;

    }
    @Override
    @SuppressWarnings("unchecked")
    public  List<Relationship> byContentType(ContentTypeIf type) throws DotDataException {
    	
        List<Relationship> list = null;
        
        try {
			list = cache.getRelationshipsByType(type);
		} catch (DotCacheException e1) {
			//Logger.debug(this.getClass(),e1.getMessage(),e1);
		}
        if(list ==null){
	        String query = "select {relationship.*} from relationship, inode relationship_1_ "
	                + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
				+ "(relationship.parent_structure_inode = ? or relationship.child_structure_inode = ?)";

				HibernateUtil dh = new HibernateUtil(Relationship.class);
				dh.setSQLQuery(query);
				dh.setParam(type.id());
				dh.setParam(type.id());
				list = dh.list();
	
					
				cache.putRelationshipsByType(type, list);

	

        }
        return list;
        
    }
    @Override
    @SuppressWarnings("unchecked")
	public  List<Relationship> byContentType(ContentTypeIf st, boolean hasParent)  {
        List<Relationship> list = new ArrayList<Relationship>();
        String query = "select {relationship.*} from relationship, inode relationship_1_ "
                + "where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and ";
		if (hasParent){
                query += "(relationship.parent_structure_inode = ?)";
		}
        else{
            query += "(relationship.child_structure_inode = ?)";
        }
		try{
			HibernateUtil dh = new HibernateUtil(Relationship.class);
			dh.setSQLQuery(query);
			dh.setParam(st.id());
			list = dh.list();
		}
		catch(Exception e){
		  throw new DotStateException(e);
		}

        return list;
    }
    @Override
    @SuppressWarnings("unchecked")
    public List<Relationship> byContentType(ContentTypeIf type, String orderBy) {
      orderBy = SQLUtil.sanitizeSortBy(orderBy);
      List<Relationship> list = new ArrayList<Relationship>();
      String query;
      if (type.id().equals("all")) {
        query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
            + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
            + "relationship.parent_structure_inode = parentstruct.inode and "
            + "relationship.child_structure_inode = childstruct.inode order by " + orderBy;
      } else {
        query = "select {relationship.*} from relationship, inode relationship_1_, structure parentstruct, "
            + "structure childstruct where relationship_1_.type='relationship' and relationship.inode = relationship_1_.inode and "
            + "relationship.parent_structure_inode = parentstruct.inode and "
            + "relationship.child_structure_inode = childstruct.inode and (relationship.parent_structure_inode = '"
            + type.id() + "' or relationship.child_structure_inode = '" + type.id() + "')  order by " + orderBy;
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
    
    
    
    
    
    
    
    @Override
    public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet) throws DotDataException {
        String stInode = contentlet.getStructure().getInode();
        List<Contentlet> matches = new ArrayList<Contentlet>();
        if (relationship.getParentStructureInode().equalsIgnoreCase(stInode)) {
            matches = dbRelatedContent(relationship, contentlet, true);
        } else if (relationship.getChildStructureInode().equalsIgnoreCase(stInode)) {
            matches = dbRelatedContent(relationship, contentlet, false);
        }
        return matches;
    }

    public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet,
            boolean hasParent) throws  DotDataException {
        return dbRelatedContent (relationship, contentlet, hasParent, false, "tree_order");
    }
    @Override
    public  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws  DotDataException {
        String stInode = contentlet.getStructure().getInode();
        List<Tree> matches = new ArrayList<Tree>();
        if (relationship.getParentStructureInode().equalsIgnoreCase(stInode)) {
            matches = relatedContentTrees(relationship, contentlet, true);
        } else if (relationship.getChildStructureInode().equalsIgnoreCase(stInode)) {
            matches = relatedContentTrees(relationship, contentlet, false);
        }
        return matches;
    }
    @Override
    @SuppressWarnings("deprecation")
	public  List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent) throws  DotDataException {
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
	public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet,
            boolean hasParent, boolean live, String orderBy) throws  DotDataException {
        List<Contentlet> matches = new ArrayList<Contentlet>();

        if(contentlet == null || !InodeUtils.isSet(contentlet.getInode()))
        	return matches;
        String iden = "";
        try{
        	iden = APILocator.getIdentifierAPI().find(contentlet).getInode();
        }catch(DotHibernateException dhe){
        	Logger.error(this.getClass(), "Unable to retrive Identifier", dhe);
        }

        if(!InodeUtils.isSet(iden))
        	return matches;

        if (hasParent) {
        	if (live)
        		matches = dbRelatedContentByParent(iden, relationship.getRelationTypeValue(),true, orderBy);
        	else
        		matches = dbRelatedContentByParent(iden, relationship.getRelationTypeValue(),false, orderBy);


        } else {
        	if (live)
        		matches = dbRelatedContentByChild(iden, relationship.getRelationTypeValue(),true, orderBy);
        	else
        		matches = dbRelatedContentByChild(iden, relationship.getRelationTypeValue(),false, orderBy);
        }
        return matches;
    }

    @Override
    public  boolean isParent(Relationship rel, ContentTypeIf st) {
        if (rel.getParentStructureInode().equalsIgnoreCase(st.id()) &&
                !(rel.getParentRelationName().equals(rel.getChildRelationName()) && rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode())))
            return true;
        return false;
    }
    @Override
    public  boolean isChild(Relationship rel, ContentTypeIf st) {
        if (rel.getChildStructureInode().equalsIgnoreCase(st.id()) &&
                !(rel.getParentRelationName().equals(rel.getChildRelationName()) && rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode())))
            return true;
        return false;
    }
    @Override
    public  boolean sameParentAndChild(Relationship rel) {
        if (rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode()) )
            return true;
        return false;
    }
    
    public static boolean isSameStructureRelationship(Relationship rel, Structure st) {
      if (rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode())  )
          return true;
      return false;
  }
    

    @Override
    public  void save(Relationship relationship) throws DotHibernateException {
    	HibernateUtil.saveOrUpdate(relationship);
    	CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
    	try{
    		CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
    		CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
    	}
    	catch(Exception e){
    		Logger.error(this.getClass(), e.getMessage(),e);
    	}

    	
    	
    	
    }
    
    @Override
	public  void save(Relationship relationship, String inode) throws DotHibernateException {
		Date now = new Date();
		relationship.setiDate(now);
		HibernateUtil.saveWithPrimaryKey(relationship, inode);
	}    

   @Override
    public void delete(String inode) throws DotHibernateException {
        Relationship relationship = byInode(inode);
        delete(relationship);
    }

    @Override
    public void delete(Relationship relationship) throws DotHibernateException {
        delete(relationship, false);
    }

    private void delete(Relationship relationship, Boolean keepTreeRecords) throws DotHibernateException {

        InodeFactory.deleteInode(relationship);

        if ( !keepTreeRecords ) {
            TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
        }

        CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
        try {
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
        } catch (DotCacheException e) {
            Logger.error(this.getClass(), e.getMessage(), e);

        }
    }

    public void deleteAndRecreate(Relationship outdatedRelationship, Relationship newRelationship) throws DotHibernateException {

        //Deletes the current relationship keeping intact the existing tree records
        delete(outdatedRelationship, true);

        //Saves the new relationship
        save(newRelationship, newRelationship.getInode());
    }


	public  List<Contentlet> dbRelatedContentByParent(String parentInode, String relationType, boolean live, String orderBy) throws DotDataException {

            HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

            String sql = "SELECT {contentlet.*} from contentlet contentlet, inode contentlet_1_, contentlet_version_info vi, tree tree1 "
            		+ "where tree1.parent = ? and tree1.relation_type = ?  "
                    + "and tree1.child = contentlet.identifier "
                    + "and contentlet.inode = contentlet_1_.inode and vi.identifier=contentlet.identifier "
                    + "and " + ((live)?"vi.live_inode":"vi.working_inode") + " = contentlet.inode ";

            if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
            	sql = sql + " order by contentlet." + orderBy;
            } else {
            	sql = sql + " order by tree1.tree_order";
            }


            Logger.debug(this.getClass(), "sql:  " + sql + "\n");
            Logger.debug(this.getClass(), "parentInode:  " + parentInode + "\n");
            Logger.debug(this.getClass(), "relationType:  " + relationType + "\n");
            dh.setSQLQuery(sql);
            dh.setParam(parentInode);
            dh.setParam(relationType);

            List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
            List<Contentlet> conResult = new ArrayList<Contentlet>();
            ESContentFactoryImpl conFac = new ESContentFactoryImpl();
            for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
            	try {
					conResult.add(conFac.convertFatContentletToContentlet(fatty));
				} catch (DotStateException | DotDataException | DotSecurityException e) {
					throw new DotDataException(e.getMessage(),e);
				}
			}
            return conResult;


    }

    /**
     * This method can be used to find the next in a sort order
     * @param parentInode The parent Relationship
     * @param relationType
     * @return the max in the sort order
     */
    @Override
  public int maxSortOrder(String parentInode, String relationType) {


        DotConnect db = new DotConnect();

        String sql = "SELECT max(tree_order) as tree_order from tree tree1 "
        		+ "where tree1.parent = ? and tree1.relation_type = ? ";

        db.setSQL(sql);
        db.addParam(parentInode);
        db.addParam(relationType);

       int x=  db.getInt("tree_order");

        return x;


    }



    @SuppressWarnings("unchecked")
	public  List<Contentlet> dbRelatedContentByChild(String childInode, String relationType, boolean live, String orderBy) throws DotDataException {

            HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

            String sql =    "SELECT {contentlet.*} "+
                            "from contentlet "+
                            "join inode contentlet_1_ "+
                            "on (contentlet.inode = contentlet_1_.inode) "+
                            "join contentlet_version_info vi "+
                            "on (vi."+(live?"live":"working")+"_inode = contentlet.inode) "+
                            "join tree "+
                            "on (tree.parent=contentlet.identifier) "+
                            "where "+
                            "  tree.child = ? "+
                            "  and tree.relation_type = ?";


           	if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
           		sql = sql + " order by contentlet." + orderBy;
           	} else {
           		sql = sql + " order by tree.tree_order";
           	}

            Logger.debug(this.getClass(), "sql:  " + sql + "\n");
            Logger.debug(this.getClass(), "childInode:  " + childInode + "\n");
            Logger.debug(this.getClass(), "relationType:  " + relationType + "\n");
            dh.setSQLQuery(sql);
            dh.setParam(childInode);
            dh.setParam(relationType);

            List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
            List<Contentlet> conResult = new ArrayList<Contentlet>();
            ESContentFactoryImpl conFac = new ESContentFactoryImpl();
            for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
            	try {
					conResult.add(conFac.convertFatContentletToContentlet(fatty));
				} catch (DotStateException | DotSecurityException e) {
					throw new DotDataException(e.getMessage(),e);
				}
			}
            return conResult;


    }

    public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet,
            boolean hasParent, boolean live) throws  DotDataException {
    	return dbRelatedContent(relationship, contentlet, hasParent, live,"");
    }

    /**
     * This method retrieves all the related contenlets and regardless if it has to retrieve parents, children or siblings
     * @param relationship
     * @param contentlet
     * @param orderBy
     * @return
     * @throws DotDataException 
     */
    public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, String orderBy, String sqlCondition, boolean liveContent) throws DotDataException {

    	return dbRelatedContent(relationship, contentlet, orderBy, sqlCondition, liveContent, 0);

    }

    /**
     * Removes the relationships from the list of related contentlets to the passed in contentlet
     * @param contentlet
     * @param relationship
     * @param relatedContentlets
     * @throws DotDataException
     */
    @Override
    public  void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets) throws DotDataException{
    	Tree t = new Tree();
		for (Contentlet con : relatedContentlets) {

			t= TreeFactory.getTree(contentlet.getIdentifier(), con.getIdentifier(), relationship.getRelationTypeValue());
			if(InodeUtils.isSet(t.getChild())  & InodeUtils.isSet(t.getParent())){
				TreeFactory.deleteTree(t);
			}else{
				t= TreeFactory.getTree(con.getIdentifier(),contentlet.getIdentifier(), relationship.getRelationTypeValue());
				if(InodeUtils.isSet(t.getChild()) & InodeUtils.isSet(t.getParent())){
					TreeFactory.deleteTree(t);
				}
			}
		}
    }

    /**
     * This method retrieves all the related contenlets and regardless if it has to retrieve parents, children or siblings
     * @param relationship
     * @param contentlet
     * @param orderBy
     * @param sqlCondition
     * @param liveContent
     * @param limit
     * @return
     * @throws DotDataException 
     */
    @SuppressWarnings("unchecked")
	public  List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, String orderBy, String sqlCondition, boolean liveContent, int limit) throws DotDataException {

        List<Contentlet> matches = new ArrayList<Contentlet>();

    	if(contentlet == null || !InodeUtils.isSet(contentlet.getInode())) {
    		return matches;
    	}

    	Identifier iden = APILocator.getIdentifierAPI().find(contentlet);
    	if(iden == null || !InodeUtils.isSet(iden.getInode()))
    		return matches;

        HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

        String sql = "SELECT {contentlet.*} from contentlet contentlet, inode contentlet_1_, tree relationshipTree, identifier iden, tree identifierTree "
        		+ "where (relationshipTree.child = ? or relationshipTree.parent = ?) and relationshipTree.relation_type = ? "
        		+ "and (iden.inode = relationshipTree.parent or iden.inode = relationshipTree.child) "
                + "and (iden.inode = identifierTree.parent and identifierTree.child = contentlet_1_.inode) "
                + "and contentlet.inode = contentlet_1_.inode and contentlet.inode <> ? ";
        if(liveContent)
        	sql += "and contentlet.live = " + DbConnectionFactory.getDBTrue();
        else
        	sql += "and contentlet.working = " + DbConnectionFactory.getDBTrue();

        if(UtilMethods.isSet(sqlCondition))
        	sql += "and " + sqlCondition;

       	if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
       		sql = sql + " order by contentlet." + orderBy;
       	} else {
       		sql = sql + " order by relationshipTree.tree_order";
       	}

        Logger.debug(this.getClass(), "sql:  " + sql + "\n");

        dh.setSQLQuery(sql);
        dh.setParam(iden.getInode());
        dh.setParam(iden.getInode());
        dh.setParam(relationship.getRelationTypeValue());
        dh.setParam(contentlet.getInode());

        if(limit > 0) {
        	dh.setMaxResults(limit);
        }

        List<com.dotmarketing.portlets.contentlet.business.Contentlet> l = dh.list();
        List<Contentlet> conResult = new ArrayList<Contentlet>();
        ESContentFactoryImpl conFac = new ESContentFactoryImpl();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : l) {
        	try {
				conResult.add(conFac.convertFatContentletToContentlet(fatty));
			} catch (DotStateException | DotSecurityException e) {
				throw new DotDataException(e.getMessage(),e);
			}
		}

        return new ArrayList<Contentlet> (new LinkedHashSet<Contentlet>(conResult));
    }

    
    @Override
    public void addRelationship(String parent,String child, String relationType)throws DotDataException {       
        Tree tree = TreeFactory.getTree(parent, child,relationType);
        if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
            tree.setParent(parent);
            tree.setChild(child);
            tree.setRelationType(relationType);
            TreeFactory.saveTree(tree);
        } else {
            tree.setRelationType(relationType);
            TreeFactory.saveTree(tree);
        }
    }
    

}
