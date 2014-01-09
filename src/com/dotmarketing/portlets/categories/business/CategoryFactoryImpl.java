package com.dotmarketing.portlets.categories.business;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.hibernate.ObjectNotFoundException;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 *
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public class CategoryFactoryImpl extends CategoryFactory {

	CategoryCache catCache;

	public CategoryFactoryImpl () {
		catCache = CacheLocator.getCategoryCache();
	}

	@Override
	protected void delete(Category object) throws DotDataException {

		List<Tree> trees = TreeFactory.getTreesByChild(object);
		for(Tree t : trees){
			HibernateUtil.delete(t);
		}
		trees = TreeFactory.getTreesByParent(object);
		for(Tree t : trees){
			HibernateUtil.delete(t);
		}

		object = (Category) HibernateUtil.load(Category.class, object.getInode());

		PermissionAPI perAPI = APILocator.getPermissionAPI();
		perAPI.removePermissions(object);

		HibernateUtil.delete(object);
		try {
			cleanParentChildrenCaches(object);
			catCache.remove(object);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected Category find(String id) throws DotDataException {
	    if(!UtilMethods.isSet(id)) return null;
	    
		Category cat = catCache.get(id);
		if(cat == null) {
			try {
				cat = (Category) HibernateUtil.load(Category.class, id);
			} catch (DotHibernateException e) {
				if(!(e.getCause() instanceof ObjectNotFoundException))
					throw e;
			}
			if(cat != null)
				try {
					catCache.put(cat);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
		return cat;
	}

	@Override
	protected Category findByKey(String key) throws DotDataException {
		if(key==null){
			throw new DotDataException("null key passed in");
		}
		Category cat = catCache.getByKey(key);
		if(cat ==null){
			HibernateUtil hu = new HibernateUtil(Category.class);
			hu.setQuery("from " + Category.class.getName() + " as cat where lower(cat.key) = ? and category0__1_.type='category'");
			hu.setParam(key.toLowerCase());
			cat = (Category) hu.load();
			if(cat != null)
				try {
					catCache.put(cat);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
		return cat;
	}

	@Override
	protected Category findByName(String name) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Category.class);
		hu.setQuery("from " + Category.class.getName() + " as cat where cat.categoryName = ? and category0__1_.type='category'");
		hu.setParam(name);
		return (Category) hu.load();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findAll() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Category.class);
		hu.setQuery("from " + Category.class.getCanonicalName());
		List<Category> cats = hu.list();
		for(Category cat : cats) {
			//Updating the cache since we are already loading all the categories
			if(catCache.get(cat.getInode()) == null)
				try {
					catCache.put(cat);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
		return cats;
	}

	@Override
	protected void save(Category object) throws DotDataException {
		String id = object.getInode();
		if(InodeUtils.isSet(id)) {
			try
			{
				Category cat = (Category) HibernateUtil.load(Category.class, id);
				// WE NEED TO REMOVE ORIGINAL BEFORE SAVING BECAUSE THE KEY CACHE NEEDS TO BE CLEARED
				// DOTCMS-5717
				catCache.remove(cat);
				BeanUtils.copyProperties(cat,object);
				HibernateUtil.saveOrUpdate(cat);
				cleanParentChildrenCaches(object);

			}catch(Exception ex){
				throw new DotDataException(ex.getMessage(),ex);
			}
		}else{
			HibernateUtil.save(object);
			try {
				cleanParentChildrenCaches(object);
				catCache.remove(object);
			} catch (DotCacheException e) {
				throw new DotDataException(e.getMessage(), e);
			}
		}
	}


	@Override
	protected void saveRemote(Category object) throws DotDataException {
		HibernateUtil.saveWithPrimaryKey(object, object.getInode());
		try {
			cleanParentChildrenCaches(object);
			catCache.remove(object);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void addChild(Categorizable parent, Category child, String relationType) throws DotDataException {
		if(!UtilMethods.isSet(relationType))
			relationType = "child";
		List<Category> childCategories = getChildren(parent);
		Tree tree = TreeFactory.getTree(parent.getCategoryId(), child.getInode());
		if(tree == null || !InodeUtils.isSet(tree.getChild())) {
			tree.setChild(child.getInode());
			tree.setParent(parent.getCategoryId());
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
		try {
			catCache.removeChild(parent, child);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void addParent(Categorizable child, Category parent)
	throws DotDataException {
		List<Category> parentCats = getParents(child);
		Tree tree = TreeFactory.getTree(parent.getInode(), child.getCategoryId());
		if(tree == null || !InodeUtils.isSet(tree.getChild())) {
			tree.setChild(child.getCategoryId());
			tree.setParent(parent.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
		try {
			catCache.removeParent(child, parent);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}
	
	private List<Category> readCatFromDotConnect(List<Map<String,Object>> list) {
	    List<Category> cats = new ArrayList<Category>();
	    for(Map<String,Object> m : list) {
            cats.add(readCatFromDotConnect(m));
        }
	    return cats;
	}
	
	private Category readCatFromDotConnect(Map<String,Object> m) {
	    Category cat = new Category();
        cat.setActive(DbConnectionFactory.getDBTrue().equals(m.get("active")));
        cat.setInode(m.get("inode").toString());
        cat.setCategoryName(m.get("category_name")!=null ? m.get("category_name").toString() : null);
        cat.setCategoryVelocityVarName(m.get("category_velocity_var_name")!=null?m.get("category_velocity_var_name").toString():null);
        cat.setKey(m.get("category_key")!=null?m.get("category_key").toString():null);
        cat.setKeywords(m.get("keywords")!=null?m.get("keywords").toString():null);
        cat.setSortOrder(m.get("sort_order")!=null ? m.get("sort_order").toString() : "0");
        return cat;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(Categorizable parent) throws DotDataException {

		List<String> childrenIds = catCache.getChildren(parent);
		List<Category> children = null;
		if(childrenIds == null) {
		    DotConnect dc = new DotConnect();
		    dc.setSQL("select inode,category_name,category_key,sort_order,active,keywords,category_velocity_var_name "+
		              " from category join tree on (category.inode = tree.child) where tree.parent = ? "+
		              " order by sort_order, category_name");
		    dc.addParam(parent.getCategoryId());
		    children = readCatFromDotConnect(dc.loadObjectResults());
			
			try {
				catCache.putChildren(parent, children);
			} catch (DotCacheException e) {
				throw new DotDataException(e.getMessage(), e);
			}
		} else {
			children = new ArrayList<Category>();
			for(String id : childrenIds) {
				Category cat = find(id);
				if(cat != null) {
					children.add(cat);
				}
			}
			Collections.sort(children,new CategoryComparator());
		}

		return children;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(Categorizable parent, String orderBy)
	throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Category.class);
		hu.setSQLQuery("select {category.*} from inode category_1_, category, tree where " +
				"category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
				"and category_1_.type = 'category' order by " + orderBy);
		hu.setParam(parent.getCategoryId());
		return (List<Category>) hu.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(Categorizable parent, String orderBy,
			String relationType) throws DotDataException {
		if(!UtilMethods.isSet(orderBy))
			orderBy = "tree_order";
		HibernateUtil hu = new HibernateUtil(Category.class);
		hu.setSQLQuery("select {category.*} from inode category_1_, category, tree where " +
				"tree.relation_type = ? and category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
				"and category_1_.type = 'category' order by " + orderBy);
		hu.setParam(relationType);
		hu.setParam(parent.getCategoryId());
		return (List<Category>) hu.list();
	}

	@Override
	protected List<Category> getParents(Categorizable child, String relationType) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Category.class);
		hu.setSQLQuery("select {category.*} from inode category_1_, category, tree " +
				"where tree.relation_type = ? and tree.child = ? and tree.parent = category.inode and category_1_.inode = category.inode " +
		"and category_1_.type = 'category' order by sort_order asc, category_name asc");
		hu.setParam(relationType);
		hu.setParam(child.getCategoryId());
		@SuppressWarnings("unchecked")
		List<Category> parents = (List<Category>) hu.list();

		return parents;
	}

    @SuppressWarnings ("unchecked")
    @Override
    protected List<Category> getParents ( Categorizable child ) throws DotDataException {

        List<String> parentIds = catCache.getParents( child );
        List<Category> parents;
        if ( parentIds == null ) {

            HibernateUtil hu = new HibernateUtil( Category.class );
            hu.setSQLQuery( "select {category.*} from inode category_1_, category, tree " +
                    "where tree.child = ? and tree.parent = category.inode and category_1_.inode = category.inode " +
                    "and category_1_.type = 'category' order by sort_order asc, category_name asc" );
            hu.setParam( child.getCategoryId() );
            parents = (List<Category>) hu.list();

            try {
                catCache.putParents( child, parents );
            } catch ( DotCacheException e ) {
                throw new DotDataException( e.getMessage(), e );
            }
        } else {
            parents = new ArrayList<Category>();
            for ( String id : parentIds ) {
                Category cat = find( id );
                if ( cat != null ) {
                    parents.add( cat );
                }
            }
        }

        return parents;
    }

	@Override
	protected void removeChild(Categorizable parent, Category child, String relationType) throws DotDataException {
		if(!UtilMethods.isSet(relationType)){
			relationType = "child";
		}
		Tree tree = TreeFactory.getTree(parent.getCategoryId(), child.getInode(), relationType);
		if(tree != null && InodeUtils.isSet(tree.getChild())) {
			TreeFactory.deleteTree(tree);
		}
		try {
			catCache.removeChild(parent, child);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}

	@Override
	protected void removeChildren(Categorizable parent) throws DotDataException {

		List<Tree> trees = TreeFactory.getTreesByParent(parent.getCategoryId());
		for(Tree tree : trees) {
			TreeFactory.deleteTree(tree);
		}
		try {
			catCache.removeChildren(parent);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}

	@Override
	protected void removeParent(Categorizable child, Category parent)
	throws DotDataException {

		Tree tree = TreeFactory.getTree(parent.getInode(), child.getCategoryId());
		if(tree != null && InodeUtils.isSet(tree.getChild())) {
			TreeFactory.deleteTree(tree);
		}
		try {
			catCache.removeParent(child, parent);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}

	@Override
	protected void removeParents(Categorizable child) throws DotDataException {
		List<Tree> trees = TreeFactory.getTreesByChild(child.getCategoryId());
		for(Tree tree : trees) {
			TreeFactory.deleteTree(tree);
		}
		try {
			catCache.removeParents(child);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}

	}

	@Override
	protected void setChildren(Categorizable parent, List<Category> children)
	throws DotDataException {

		List<Tree> trees = TreeFactory.getTreesByParent(parent.getCategoryId());
		for(Tree tree : trees) {
			TreeFactory.deleteTree(tree);
		}
		for (Category cat : children) {
			Tree tree = new Tree(parent.getCategoryId(), cat.getInode());
			TreeFactory.saveTree(tree);
		}
		try {
			catCache.removeChildren(parent);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void setParents(Categorizable child, List<Category> parents)
	throws DotDataException {
		List<Category> pars = getParents(child);
		for (Category category : pars) {
			Tree t = TreeFactory.getTree(category.getInode(), child.getCategoryId());
			TreeFactory.deleteTree(t);
		}
		for (Category cat : parents) {
			Tree tree = new Tree(cat.getInode(), child.getCategoryId());
			TreeFactory.saveTree(tree);
		}
		try {
			catCache.removeParents(child);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void deleteTopLevelCategories() {
		Statement s = null;
		Connection conn = null;
		try {
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			s = conn.createStatement();
			StringBuilder sql = new StringBuilder();
			sql.append("delete from category category left join tree tree on category.inode = tree.child, ");
			sql.append("inode category_1_ where tree.child is null and category_1_.inode = category.inode and category_1_.type = 'category' ");
			s.executeUpdate(sql.toString());
			conn.commit();
		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				s.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected List<Category> findTopLevelCategories() throws DotDataException {
		return findTopLevelCategoriesByFilter(null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findTopLevelCategoriesByFilter(String filter, String sort) throws DotDataException {
		HibernateUtil dh = new HibernateUtil(Category.class);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT {category.*} from category category left join tree tree on category.inode = tree.child, ");
		sql.append("inode category_1_ where tree.child is null and category_1_.inode = category.inode and category_1_.type = 'category' ");
		sql.append(getFilterAndSortSQL(filter, sort));
		dh.setSQLQuery(sql.toString());
		return (List<Category>) dh.list();
	}

	@Override
	@Deprecated
	//  Have to delete from cache
	protected void deleteChildren(String inode) {
		Statement s = null;
		Connection conn = null;
		try {
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			s = conn.createStatement();
			StringBuilder sql = new StringBuilder();
			sql.append("delete  from  category c where exists ( select 1 from category cat inner join inode category_1_ on (category_1_.inode = cat.inode) ");
			sql.append(" inner join tree on (cat.inode = tree.child) where ");
			sql.append(" tree.parent = '").append(inode).append("' and category_1_.type = 'category' and cat.inode = c.inode ) ");
			s.executeUpdate(sql.toString());
			conn.commit();
		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				s.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findChildrenByFilter(String inode, String filter, String sort) throws DotDataException {
		HibernateUtil dh = new HibernateUtil(Category.class);
		StringBuilder sql = new StringBuilder();
		sql.append("select {category.*} from inode category_1_, category, tree where ");
		sql.append("category.inode = tree.child and tree.parent = '").append(inode).append("' and category_1_.inode = category.inode ");
		sql.append("and category_1_.type = 'category'  ");
		sql.append(getFilterAndSortSQL(filter, sort));
		dh.setSQLQuery(sql.toString());
		return (List<Category>) dh.list();
	}

	private String getFilterAndSortSQL(String filter, String sort) {
		StringBuilder sb = new StringBuilder();

		if(UtilMethods.isSet(filter)) {
			filter = filter.toLowerCase();
			sb.append("and (lower(category.category_name) like '%").append(filter).append("%' or lower(category.category_key) like '%"+filter+"%'" +
				"or lower(category.category_velocity_var_name) like '%").append(filter).append("%' ) ");
		}

		if(UtilMethods.isSet(sort)) {
			String sortDirection = sort.startsWith("-")?"desc":"asc";
			sort = sort.startsWith("-")?sort.substring(1,sort.length()):sort;
			sb.append("order by category.").append(sort).append(" ").append(sortDirection);
		} else {
			sb.append("order by category.sort_order, category.category_name");
		}

		return sb.toString();
	}


	@Override
	protected void clearCache() {
		catCache.clearCache();
	}
	public boolean  hasDependencies(Category cat) throws DotDataException {

		String query;
		HibernateUtil dh = new HibernateUtil();
		HibernateUtil dh2 = new HibernateUtil();
		HibernateUtil dh3 = new HibernateUtil();
		query= "select  count(*) from Tree  Tree, Inode inode_ where Tree.parent = '"+cat.getInode()+"' and  inode_.type = 'category' and  Tree.child = inode_.inode" ;
		dh.setQuery(query);
		query= "select  count(*)   from Tree  Tree, Inode inode_ where Tree.child = '"+cat.getInode()+"' and Tree.parent = inode_.inode and inode_.type !='category'";
		dh2.setQuery(query);
		query= "select  count(*)   from Field field where field_values like '"+cat.getInode()+"'";
		dh3.setQuery(query);
		if((dh.getCount()==0) && (dh2.getCount()==0) &&(dh3.getCount()==0))
			return false;

		return true;
	}

	public void sortTopLevelCategories()  throws DotDataException {
		Statement s = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			CategorySQL catSQL= CategorySQL.getInstance();
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			s = conn.createStatement();
			s.executeUpdate(catSQL.getCreateSortTopLevel());
			s.executeUpdate(catSQL.getUpdateSort());
			s.executeUpdate(catSQL.getDropSort());
			conn.commit();

			rs = s.executeQuery(catSQL.getSortParents());

			while(rs.next()) {
				Category cat = null;
				try {
					cat = (Category) HibernateUtil.load(Category.class, rs.getString("inode"));
				} catch (DotHibernateException e) {
					if(!(e.getCause() instanceof ObjectNotFoundException))
						throw e;
				}
				if(cat != null)
					try {
						catCache.put(cat);
					} catch (DotCacheException e) {
						throw new DotDataException(e.getMessage(), e);
					}
			}
		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				s.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void sortChildren(String inode)  throws DotDataException {
		Statement s = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			CategorySQL catSQL= CategorySQL.getInstance();
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			s = conn.createStatement();
			String sql = "";
			sql = catSQL.getCreateSortChildren(inode);
			s.executeUpdate( sql );
			sql = catSQL.getUpdateSort();
			s.executeUpdate( sql );
			sql = catSQL.getDropSort();
			s.executeUpdate(sql);
			conn.commit();
			sql = catSQL.getSortedChildren(inode);
			rs = s.executeQuery(sql);

			while(rs.next()) {
				Category cat = null;
				try {
					cat = (Category) HibernateUtil.load(Category.class, rs.getString("inode"));
				} catch (DotHibernateException e) {
					if(!(e.getCause() instanceof ObjectNotFoundException))
						throw e;
				}
				if(cat != null)
					try {
						catCache.put(cat);
					} catch (DotCacheException e) {
						throw new DotDataException(e.getMessage(), e);
					}
			}

		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				s.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

    /**
     * Cleans the parent and child cache for a given category
     *
     * @param category
     * @throws DotDataException
     * @throws DotCacheException
     */
    private void cleanParentChildrenCaches ( Category category ) throws DotDataException, DotCacheException {

        List<String> parentIds = catCache.getParents( category );
        if ( parentIds != null ) {
            for ( String parentId : parentIds ) {
                catCache.removeChildren( parentId );
            }
        }
        List<String> childrenIds = catCache.getChildren( category );
        if ( childrenIds != null ) {
            for ( String childId : childrenIds ) {
                catCache.removeParents( childId );
            }
        }
    }

	private class CategoryComparator implements Comparator<Category>
	{
		public int compare(Category cat1, Category cat2) {
			int returnValue = 0;
			try
			{
				if(cat1.getSortOrder() > cat2.getSortOrder())
				{
					returnValue = 1;
				}
				else if(cat2.getSortOrder() > cat1.getSortOrder())
				{
					returnValue = -1;
				}
				else if(cat1.getSortOrder() == cat2.getSortOrder())
				{
					returnValue = cat1.getCategoryName().compareTo(cat2.getCategoryName());
				}
			}
			catch(Exception ex)
			{
				Logger.debug(CategoryFactoryImpl.class,ex.getMessage());
			}
			return returnValue;
		}
	}


}
