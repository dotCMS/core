package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public class CategoryFactoryImpl extends CategoryFactory {

	CategoryCache catCache;
	final CategorySQL categorySQL;

	public CategoryFactoryImpl () {
		catCache = CacheLocator.getCategoryCache();
		this.categorySQL = CategorySQL.getInstance();
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

		object = find(object.getInode());
        if(null == object) return;

		PermissionAPI perAPI = APILocator.getPermissionAPI();
		perAPI.removePermissions(object);

		new DotConnect()
				.setSQL(" DELETE FROM category WHERE inode = ? ")
				.addParam(object.getInode())
				.loadResults();

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

				final List<Map<String, Object>> result = new DotConnect()
						.setSQL(" SELECT * FROM category WHERE inode = ? ")
						.addParam(id)
						.loadObjectResults();

				cat = result.isEmpty() ? null : convertForCategory(result.get(0));

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
		if(cat == null){

			final List<Map<String, Object>> result = new DotConnect()
					.setSQL(" SELECT * FROM category WHERE lower(category_key) = ?")
					.addParam(key.toLowerCase())
					.loadObjectResults();

			cat = result.isEmpty() ? null : convertForCategory(result.get(0));

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
	protected Category findByVar(final String variable) throws DotDataException {
		DotPreconditions.checkArgument(UtilMethods.isSet(variable));

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category WHERE category_velocity_var_name = ?")
				.addParam(variable)
				.loadObjectResults();

		return result.isEmpty() ? null : convertForCategory(result.get(0));
	}

	@Override
	protected Category findByName(String name) throws DotDataException {

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category WHERE category_name = ?")
				.addParam(name)
				.loadObjectResults();

		return result.isEmpty() ? null : convertForCategory(result.get(0));

	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findAll() throws DotDataException {
		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category ")
				.loadObjectResults();

		List<Category> cats = convertForCategories(result);
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
	public void save(Category object) throws DotDataException {
		final String id = object.getInode();
		try {
			if (InodeUtils.isSet(id)) {
				Category cat = find(id);
				// WE NEED TO REMOVE ORIGINAL BEFORE SAVING BECAUSE THE KEY CACHE NEEDS TO BE CLEARED
				// DOTCMS-5717
				if (null != cat) {
					catCache.remove(cat);
					updateCategory(cat);
					cleanParentChildrenCaches(object);
				} else {
					insertCategory(object, id);
					cleanParentChildrenCaches(object);
					catCache.remove(object);
				}
			} else {
				final String inode = UUIDGenerator.generateUuid();
				insertCategory(object, inode);
				cleanParentChildrenCaches(object);
				catCache.remove(object);
			}
		} catch (DotCacheException ce){
			throw new DotDataException(ce.getMessage(), ce);
		}
	}

	/**
	 *
	 * @param object
	 * @param inode
	 * @throws DotDataException
	 * @throws DotCacheException
	 */
	private void insertCategory(final Category object, final String inode)
			throws DotDataException, DotCacheException {
		final Date date = new Date();
		new DotConnect()
				.setSQL("INSERT INTO inode (inode, idate, type) VALUES (?,?,'category')")
				.addParam(inode)
				.addParam(date)
				.loadResult();

		new DotConnect()
				.setSQL("INSERT INTO category(inode, category_name, category_key, sort_order, active, keywords, category_velocity_var_name, mod_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
				.addParam(inode)
				.addParam(object.getCategoryName())
				.addParam(object.getKey())
				.addParam(object.getSortOrder())
				.addParam(object.isActive())
				.addParam(object.getKeywords())
				.addParam(object.getCategoryVelocityVarName())
				.addParam(date)
				.loadResults();

		//The returned object must have an id assigned back into the original object sent
		object.setInode(inode);
	}

	/**
	 *
	 * @param object
	 * @throws DotDataException
	 */
	private void updateCategory(final Category object) throws DotDataException {
		new DotConnect()
				.setSQL("UPDATE category SET category_name=?, category_key=?, sort_order=?, active=?, keywords=?, category_velocity_var_name=?, mod_date=? WHERE inode=?")
				.addParam(object.getCategoryName())
				.addParam(object.getKey())
				.addParam(object.getSortOrder())
				.addParam(object.isActive())
				.addParam(object.getKeywords())
				.addParam(object.getCategoryVelocityVarName())
				.addParam(new Date())
				.addParam(object.getInode())
				.loadResults();
	}

	@Override
	protected void saveRemote(Category object) throws DotDataException {
		save(object);
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

	@SuppressWarnings("unchecked")
	@Override
		protected List<Category> getChildren(Categorizable parent) throws DotDataException {

		List<Category> children= catCache.getChildren(parent);
		if(children == null) {
			children = getChildren(parent, "sort_order");
			try {
				catCache.putChildren(parent, children);
			} catch (DotCacheException e) {
				throw new DotDataException(e.getMessage(), e);
			}
		}

		return children;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(final Categorizable parent, String orderBy)
	throws DotDataException {
		orderBy = SQLUtil.sanitizeSortBy(orderBy);

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL("select category.* from inode category_1_, category, tree where " +
						"category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
						"and category_1_.type = 'category' order by " + orderBy)
				.addParam(parent.getCategoryId())
				.loadObjectResults();

		return convertForCategories(result);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(final Categorizable parent, String orderBy,
			String relationType) throws DotDataException {
		orderBy = SQLUtil.sanitizeSortBy(orderBy);

		if(!UtilMethods.isSet(orderBy))
			orderBy = "tree_order";

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL("select category.* from inode category_1_, category, tree where " +
						"tree.relation_type = ? and category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
						"and category_1_.type = 'category' order by " + orderBy)
				.addParam(relationType)
				.addParam(parent.getCategoryId())
				.loadObjectResults();

		return convertForCategories(result);

	}

	@Override
	protected List<Category> getParents(final Categorizable child, final String relationType) throws DotDataException {

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL("select category.* from inode category_1_, category, tree " +
						"where tree.relation_type = ? and tree.child = ? and tree.parent = category.inode and category_1_.inode = category.inode " +
						"and category_1_.type = 'category' order by sort_order asc, category_name asc")
				.addParam(relationType)
				.addParam(child.getCategoryId())
				.loadObjectResults();

		return convertForCategories(result);
	}

    @SuppressWarnings ("unchecked")
    @Override
    protected List<Category> getParents (final Categorizable child ) throws DotDataException {

        List<String> parentIds = catCache.getParents( child );
        List<Category> parents;
        if ( parentIds == null ) {

			final List<Map<String, Object>> result = new DotConnect()
					.setSQL("select category.* from inode category_1_, category, tree " +
							"where tree.child = ? and tree.parent = category.inode and category_1_.inode = category.inode " +
							"and category_1_.type = 'category' order by sort_order asc, category_name asc")
					.addParam(child.getCategoryId())
					.loadObjectResults();

			parents = convertForCategories(result);
            try {
                catCache.putParents( child, parents );
            } catch ( DotCacheException e ) {
                throw new DotDataException( e.getMessage(), e );
            }
        } else {
            parents = new ArrayList<>();
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
	protected void removeChild(final Categorizable parent, final Category child, String relationType) throws DotDataException {
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
	protected void removeChildren(final Categorizable parent) throws DotDataException {

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
	protected void removeParent(final Categorizable child, final Category parent)
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
	protected void removeParents(final Categorizable child) throws DotDataException {
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
	protected void setChildren(final Categorizable parent, final List<Category> children)
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
	protected void setParents(final Categorizable child, final List<Category> parents)
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
	protected void deleteTopLevelCategories() throws DotDataException {
		final List<Category> topLevelCategories = findTopLevelCategories();
        for (final Category category:topLevelCategories){
        	delete(category);
		}
	}

	@Override
	protected List<Category> findTopLevelCategories() throws DotDataException {
		return findTopLevelCategoriesByFilter(null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findTopLevelCategoriesByFilter(String filter, String sort) throws DotDataException {

		try {

			filter = SQLUtil.sanitizeParameter(filter);
			sort = SQLUtil.sanitizeSortBy(sort);

			final DotConnect dc = new DotConnect();

			String selectQuery = "SELECT * FROM category LEFT JOIN tree ON category.inode = tree.child, inode "
					+ "WHERE tree.child IS NULL AND inode.inode = category.inode AND inode.type = 'category'";

			if ( UtilMethods.isSet(filter) ) {
				filter = filter.toLowerCase();
				selectQuery += " AND (LOWER(category.category_name) LIKE ? OR LOWER(category.category_key) LIKE ? "
						+ "OR LOWER(category.category_velocity_var_name) LIKE ? ) ";
			}
			if ( UtilMethods.isSet(sort) ) {
				String sortDirection = sort.startsWith("-") ? " DESC" : " ASC";
				sort = sort.startsWith("-") ? sort.substring(1, sort.length()) : sort;
				selectQuery += " ORDER BY category." + sort + sortDirection;
			} else {
				selectQuery += " ORDER BY category.sort_order, category.category_name";
			}

			//Set the sql query
			dc.setSQL(SQLUtil.addLimits(selectQuery, 0, -1));

			Logger.debug(this, "Executing the query: " + selectQuery +
					", filter: " + filter + ", sort" + sort);

			if ( UtilMethods.isSet(filter) ) {
				dc.addObject("%" + filter + "%");
				dc.addObject("%" + filter + "%");
				dc.addObject("%" + filter + "%");
			}

			//Execute and return the result of the query
			return convertForCategories(dc.loadObjectResults());
		} catch (Exception e) {
			throw new DotDataException("An error occurred when filtering the top level categories.", e);
		}
	}

	/**
	 * Convert the SQL categories results into a list of Category objects
	 *
	 * @param sqlResults sql query results
	 * @return a list of categories objects
	 */
	 List<Category> convertForCategories(final List<Map<String, Object>> sqlResults) {

		List<Category> categories = new ArrayList<>();

		if ( sqlResults != null ) {

			for ( Map<String, Object> row : sqlResults ) {
				Category category = convertForCategory(row);
				categories.add(category);
			}
		}

		return categories;
	}

	/**
	 * Converts the category information coming from the database into a {@link Category}
	 * object with all of its properties. If the information is not present, a
	 * <code>null</code> value will be returned.
	 *
	 * @param sqlResult - The data of a specific category from the database.
	 * @return The {@link Category} object.
	 */
	private Category convertForCategory(final Map<String, Object> sqlResult) {

		Category category = null;
		if ( sqlResult != null ) {
			category = new Category();

			Object sortOrder = sqlResult.get("sort_order");

			category.setInode((String) sqlResult.get("inode"));
			category.setCategoryName((String) sqlResult.get("category_name"));
			category.setKey((String) sqlResult.get("category_key"));
			if ( sortOrder != null ) {
				category.setSortOrder(Integer.valueOf(sortOrder.toString()));
			} else {
				category.setSortOrder((Integer) sortOrder);
			}
			category.setActive(DbConnectionFactory.isDBTrue(sqlResult.get("active").toString()));
			category.setKeywords((String) sqlResult.get("keywords"));
			category.setCategoryVelocityVarName((String) sqlResult.get("category_velocity_var_name"));
			category.setModDate((Date) sqlResult.get("mod_date"));
		}

		return category;
	}

	@Override
	@Deprecated
	//  Have to delete from cache
	protected void deleteChildren(String inode) {
		inode = SQLUtil.sanitizeParameter(inode);
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
				Logger.error(CategoryFactoryImpl.class,e1);
			}
			Logger.error(CategoryFactoryImpl.class, e);
		} finally {
			try {
				s.close();
				conn.close();
			} catch (SQLException e) {
				Logger.error(CategoryFactoryImpl.class,e);
			}
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findChildrenByFilter(String inode, String filter, String sort) throws DotDataException {

		try {

			inode = SQLUtil.sanitizeParameter(inode);
			filter = SQLUtil.sanitizeParameter(filter);
			sort = SQLUtil.sanitizeSortBy(sort);

			final DotConnect dc = new DotConnect();

			String selectQuery = "SELECT * FROM inode, category, tree WHERE category.inode = tree.child AND tree.parent = ? "
					+ "AND inode.inode = category.inode AND inode.type = 'category'";

			if ( UtilMethods.isSet(filter) ) {
				filter = filter.toLowerCase();
				selectQuery += " AND (LOWER(category.category_name) LIKE ? OR LOWER(category.category_key) LIKE ? "
						+ "OR LOWER(category.category_velocity_var_name) LIKE ? ) ";
			}
			if ( UtilMethods.isSet(sort) ) {
				String sortDirection = sort.startsWith("-") ? " DESC" : " ASC";
				sort = sort.startsWith("-") ? sort.substring(1, sort.length()) : sort;
				selectQuery += " ORDER BY category." + sort + sortDirection;
			} else {
				selectQuery += " ORDER BY category.sort_order, category.category_name";
			}

			//Set the sql query
			dc.setSQL(SQLUtil.addLimits(selectQuery, 0, -1));

			Logger.debug(this, "Select Query: " + selectQuery +
					", inode: " + inode + ", filter: " + filter + ", sort: " + sort);

			dc.addObject(inode);
			if ( UtilMethods.isSet(filter) ) {
				dc.addObject("%" + filter + "%");
				dc.addObject("%" + filter + "%");
				dc.addObject("%" + filter + "%");
			}

			//Execute and return the result of the query
			return convertForCategories(dc.loadObjectResults());
		} catch (Exception e) {
			throw new DotDataException("An error occurred when filtering child categories for inode '" + inode + "'.", e);
		}
	}

	@Override
	protected void clearCache() {
		catCache.clearCache();
	}
	public boolean  hasDependencies(final Category cat) throws DotDataException {

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

            putResultInCatCache( rs );
        } catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException sqlException) {
				Logger.debug( this, "Error trying to rollback connection", sqlException );
			}
            Logger.debug( this, "Error trying to execute statements", e );
		} finally {
            closeEverything( s, conn, rs );
        }
	}

    public void sortChildren(final String inode)  throws DotDataException {

		Statement statement = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			CategorySQL catSQL= CategorySQL.getInstance();
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			statement = conn.createStatement();
			String sql;

            if ( DbConnectionFactory.isOracle() ){
                //For Oracle we need to avoid ORA-01027 by creating the table before.
                sql = catSQL.createCategoryReorderTable();
                statement.execute( sql );
            }

			PreparedStatement createSortPreparedStatement = conn.prepareStatement( catSQL.getCreateSortChildren() );
            createSortPreparedStatement.setString( 1, inode );
            createSortPreparedStatement.executeUpdate();

			sql = catSQL.getUpdateSort();
			statement.executeUpdate( sql );

			sql = catSQL.getDropSort();
			statement.executeUpdate(sql);

			conn.commit();

            PreparedStatement getSortedPreparedStatement = conn.prepareStatement( catSQL.getSortedChildren() );
            getSortedPreparedStatement.setString( 1, inode );
            rs = getSortedPreparedStatement.executeQuery();

            putResultInCatCache( rs );

        } catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException sqlException) {
                Logger.debug( this, "Error trying to rollback connection", sqlException );
			}
            Logger.debug( this, "Error trying to execute statements", e );
		} finally {
            closeEverything( statement, conn, rs );
        }
	}

    private void putResultInCatCache( final ResultSet rs ) throws SQLException, DotDataException {
        while(rs.next()) {
			// calling find will put it into cache internally
			find(rs.getString("inode"));
        }
    }

    private void closeEverything( final Statement s, final Connection conn, final ResultSet rs ) {
        try {
            rs.close();
            s.close();
            conn.close();
        } catch (SQLException e) {
            Logger.debug( this, "Error trying to close statement, connection and result set", e );
        }
    }

    /**
     * Cleans the parent and child cache for a given category
     *
     * @param category
     * @throws DotDataException
     * @throws DotCacheException
     */
    private void cleanParentChildrenCaches ( final Category category ) throws DotDataException, DotCacheException {

		final List<String> parentIds = catCache.getParents( category );
        if ( parentIds != null ) {
            for ( String parentId : parentIds ) {
                catCache.removeChildren( parentId );
            }
        }
		final List<Category> children = catCache.getChildren( category );
		if ( children != null ) {
			for ( final Category child : children ) {
				catCache.removeParents( child.getCategoryId() );
			}
		}
    }

    protected String suggestVelocityVarName(final String categoryVelVarName) throws DotDataException {
        final DotConnect dc = new DotConnect();
		String var = VelocityUtil.convertToVelocityVariable(categoryVelVarName, false);
        for (int i = 1; i < 100000; i++) {
          dc.setSQL(this.categorySQL.getVelocityVarNameCount());
          dc.addParam(var);
          if (dc.getInt("test") == 0) {
            return var;
          }
            var = VelocityUtil.convertToVelocityVariable(categoryVelVarName, false) + String
                    .valueOf(i);
        }
        throw new DotDataException("Unable to suggest a variable name.  Got to:" + var);
    }

}
