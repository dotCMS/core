package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DeterministicIdentifierAPI;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.HierarchedCategory;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;
import com.dotmarketing.portlets.categories.model.ShortCategory;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public class CategoryFactoryImpl extends CategoryFactory {

	public static final String INODE = "inode";
	public static final String CATEGORY_NAME = "category_name";
	public static final String CATEGORY_KEY = "category_key";

	CategoryCache catCache;
	final CategorySQL categorySQL;
	
	
	
	public CategoryFactoryImpl () {
		catCache = CacheLocator.getCategoryCache();
		this.categorySQL = CategorySQL.getInstance();
	}

	@Override
	protected void delete(Category object) throws DotDataException {

		List<Tree> trees = TreeFactory.getTreesByChild(object);
		for(final Tree tree : trees){
			TreeFactory.deleteTree(tree);
		}
		trees = TreeFactory.getTreesByParent(object);
		for(final Tree tree : trees){
			TreeFactory.deleteTree(tree);
		}

		object = find(object.getInode());
        if(null == object) return;

		final PermissionAPI perAPI = APILocator.getPermissionAPI();
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
	protected Category find(final String id) throws DotDataException {
	    if(!UtilMethods.isSet(id)) return null;
	    
	    Category category = catCache.get(id);
		if(category == null) {

				final List<Map<String, Object>> result = new DotConnect()
						.setSQL(" SELECT * FROM category WHERE inode = ? ")
						.addParam(id)
						.loadObjectResults();

				category = result.isEmpty() ? null : convertForCategory(result.get(0));

			if(category != null)
				try {
					catCache.put(category);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
		return category;
	}

	@Override
	protected Category findByKey(final String key) throws DotDataException {
		if(key==null){
			throw new DotDataException("null key passed in");
		}
		Category category = catCache.getByKey(key);
		if(category == null){

			final List<Map<String, Object>> result = new DotConnect()
					.setSQL(" SELECT * FROM category WHERE lower(category_key) = ?")
					.addParam(key.toLowerCase())
					.loadObjectResults();

			category = result.isEmpty() ? null : convertForCategory(result.get(0));

			if(category != null)
				try {
					catCache.put(category);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
		return category;
	}

	@Override
	protected Category findByVar(final String variable) throws DotDataException {
		DotPreconditions.checkArgument(UtilMethods.isSet(variable));

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category WHERE lower(category_velocity_var_name) = ?")
				.addParam(variable.toLowerCase())
				.loadObjectResults();

		return result.isEmpty() ? null : convertForCategory(result.get(0));
	}

	@Override
	protected Category findByName(final String name) throws DotDataException {
	    if(null==name) {
	        return null;
	    }
		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category WHERE category_name = lower(?)")
				.addParam(name.toLowerCase())
				.loadObjectResults();

		return result.isEmpty() ? null : convertForCategory(result.get(0));

	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findAll() throws DotDataException {
		final List<Map<String, Object>> result = new DotConnect()
				.setSQL(" SELECT * FROM category ")
				.loadObjectResults();

		List<Category> categories = convertForCategories(result);
		updateCache(categories);
		return categories;
	}

	@Override
	public void save(final Category object) throws DotDataException {
		this.save(object, null);
	}

	@Override
	public void save(final Category object, final Category parent) throws DotDataException {
		final String id = object.getInode();
		try {
			final DotConnect dotConnect = new DotConnect();
			if (InodeUtils.isSet(id)) {
				final Category category = find(id);
				// WE NEED TO REMOVE ORIGINAL BEFORE SAVING BECAUSE THE KEY CACHE NEEDS TO BE CLEARED
				// DOTCMS-5717
				if (null != category) {
					catCache.remove(category);
					updateCategory(object, dotConnect);
					cleanParentChildrenCaches(object);
				} else {
					insertCategory(object, id, dotConnect);
					cleanParentChildrenCaches(object);
					catCache.remove(object);
				}
			} else {
				final DeterministicIdentifierAPI api = APILocator.getDeterministicIdentifierAPI();
				final String inode = api.generateDeterministicIdBestEffort(object, parent);
				insertCategory(object, inode, dotConnect);
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
	private void insertCategory(final Category object, final String inode, final DotConnect dotConnect)
			throws DotDataException, DotCacheException {
		final Date date = new Date();

		final boolean inodeExists = dotConnect
				.setSQL("SELECT count(*) as test FROM inode WHERE inode=? AND type = 'category' ")
				.addParam(inode)
				.getInt("test")>0;

		if(!inodeExists) {
			dotConnect
					.setSQL("INSERT INTO inode (inode, idate, type) VALUES (?,?,'category')")
					.addParam(inode)
					.addParam(date)
					.loadResult();
		}

		dotConnect
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
	private void updateCategory(final Category object, final DotConnect dotConnect) throws DotDataException {
		dotConnect
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
	protected void saveRemote(final Category object) throws DotDataException {
		save(object);
		try {
			cleanParentChildrenCaches(object);
			catCache.remove(object);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void addChild(final Categorizable parent, final Category child, String relationType) throws DotDataException {
		if(!UtilMethods.isSet(relationType))
			relationType = "child";
		final Tree tree = TreeFactory.getTree(parent.getCategoryId(), child.getInode());
		if(tree != null && !InodeUtils.isSet(tree.getChild())) {
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
	protected void addParent(final Categorizable child,final Category parent)
	throws DotDataException {
		final Tree tree = TreeFactory.getTree(parent.getInode(), child.getCategoryId());
		if(tree != null && !InodeUtils.isSet(tree.getChild())) {
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
    protected List<Category> getAllChildren(final Categorizable parentCategory) throws DotDataException {
        if (parentCategory == null) {
            return List.of();
        }

        final Category allChildrenKey = new Category();
        allChildrenKey.setInode(parentCategory.getCategoryId() + ALL_CHILDREN_SUFFIX);

        List<Category> cachedChildren = catCache.getChildren(allChildrenKey);
        if (cachedChildren != null) {
            return cachedChildren;
        }

        synchronized (allChildrenKey.getCategoryId().intern()) {
            cachedChildren = catCache.getChildren(allChildrenKey);
            if (cachedChildren != null) {
                return cachedChildren;
            }

            final List<Category> categoryTree = new ArrayList<>();
            final LinkedList<Category> children = new LinkedList<>(getChildren(parentCategory));
            while (children.size() > 0) {
                Category child = children.poll();
                children.addAll(getChildren(child));
                categoryTree.add(child);
            }

            try {
                catCache.putChildren(allChildrenKey, categoryTree);
            } catch (DotCacheException e) {
                throw new DotDataException(e.getMessage(), e);
            }

            return categoryTree;
        }
    }


	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> getChildren(final Categorizable parent, String orderBy)
	throws DotDataException {
		orderBy = SQLUtil.sanitizeSortBy(orderBy);

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL("select category.* from category, tree where " +
						"category.inode = tree.child and tree.parent = ? order by " + orderBy)
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
				.setSQL("select category.* from category, tree where " +
						"tree.relation_type = ? and category.inode = tree.child and tree.parent = ? order by " + orderBy)
				.addParam(relationType)
				.addParam(parent.getCategoryId())
				.loadObjectResults();

		return convertForCategories(result);

	}

	@Override
	protected List<Category> getParents(final Categorizable child, final String relationType) throws DotDataException {

		final List<Map<String, Object>> result = new DotConnect()
				.setSQL("select category.* from category, tree " +
						"where tree.relation_type = ? and tree.child = ? and tree.parent = category.inode order by sort_order asc, category_name asc")
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
					.setSQL("select category.* from category, tree " +
							"where tree.child = ? and tree.parent = category.inode order by sort_order asc, category_name asc")
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

		final Tree tree = TreeFactory.getTree(parent.getInode(), child.getCategoryId());
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
		final List<Tree> trees = TreeFactory.getTreesByChild(child.getCategoryId());
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

		final List<Tree> trees = TreeFactory.getTreesByParent(parent.getCategoryId());
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
		final List<Category> oldParents = getParents(child);
		for (final Category category : oldParents) {
			final Tree tree = TreeFactory.getTree(category.getInode(), child.getCategoryId());
			TreeFactory.deleteTree(tree);
		}
		for (final Category cat : parents) {
			final Tree tree = new Tree(cat.getInode(), child.getCategoryId());
			TreeFactory.saveTree(tree);
		}
		try {
			catCache.removeParents(child);
		} catch (DotCacheException e) {
			throw new DotDataException(e.getMessage(), e);
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
				Category category = convertForCategory(row, Category.class);
				categories.add(category);
			}
		}

		return categories;
	}

	private List<HierarchedCategory> convertForHierarchedCategories(final List<Map<String, Object>> sqlResults) {
		List<HierarchedCategory> categories = new ArrayList<>();

		if ( sqlResults != null ) {

			for ( Map<String, Object> row : sqlResults ) {
				HierarchedCategory category = (HierarchedCategory) convertForCategory(row, HierarchedCategory.class);

				if (row == null) {
					continue;
				}

                try {
					if (row.get("path") != null ) {
						final String parentsASJsonArray = "[" + row.get("path") + "]";
						final List<ShortCategory> parentList = getShortCategories(parentsASJsonArray);
						category.setParentList(parentList.subList(0, parentList.size() - 1));
					}

					if (row.containsKey("childrencount")) {
						category.setChildrenCount(ConversionUtils.toInt(row.get("childrencount"), 0));
					}

					categories.add(category);
                } catch ( Exception e) {
					Logger.warn(CategoryFactoryImpl.class, e::getMessage);
                }
			}
		}

		return categories;
	}

	/**
	 * Create a set of Object {@link ShortCategory} from a JSON with the follow syntax:
	 *
	 * <code>
	 *    [
	 *       {
	 *           'categoryName': 'Category name value',
	 *           'key': 'Key value',
	 *           'inode': 'inode value'
	 *       }
	 *    ]
	 *
	 * </code>
	 *
	 * @param parentsASJsonArray
	 * @return
	 */
	private static List<ShortCategory> getShortCategories(String parentsASJsonArray) {
        try {
            return ((List<Map<String, String>>) JsonUtil.getObjectFromJson(parentsASJsonArray, List.class))
                    .stream()
                    .map(map -> new ShortCategory.Builder()
                            .setName(map.get("name"))
                            .setKey(map.get("key"))
                            .setInode(map.get(INODE))
                            .build()
                    )
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

	private Category convertForCategory(final Map<String, Object> sqlResult) {
		 return convertForCategory(sqlResult, Category.class);
	}

	/**
	 * Converts the category information coming from the database into a {@link Category}
	 * object with all of its properties. If the information is not present, a
	 * <code>null</code> value will be returned.
	 *
	 * @param sqlResult - The data of a specific category from the database.
	 * @return The {@link Category} object.
	 */
	private Category convertForCategory(final Map<String, Object> sqlResult, Class<? extends Category> clazz) {

		Category category = null;
		if ( sqlResult != null ) {
			category = ReflectionUtils.newInstance(clazz);

			Object sortOrder = sqlResult.get("sort_order");

			category.setInode((String) sqlResult.get(INODE));
			category.setCategoryName((String) sqlResult.get(CATEGORY_NAME));
			category.setKey((String) sqlResult.get(CATEGORY_KEY));
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


	@SuppressWarnings("unchecked")
	@Override
	protected List<Category> findChildrenByFilter(String inode, String filter, String sort) throws DotDataException {

		try {

			inode = SQLUtil.sanitizeParameter(inode);
			filter = SQLUtil.sanitizeParameter(filter);
			sort = SQLUtil.sanitizeSortBy(sort);

			final DotConnect dc = new DotConnect();

			String selectQuery = "SELECT * FROM category, tree WHERE category.inode = tree.child AND tree.parent = ? ";

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

	/**
	 * This method tells you if there are other categories associated to the one passed as param
	 * @param cat
	 * @return
	 * @throws DotDataException
	 */
	public boolean  hasDependencies(final Category cat) throws DotDataException {

		final DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL("select count(*) as count from Tree  Tree where Tree.parent = ? ").addParam(cat.getInode());
		int count1 = dotConnect.getInt("count");
		dotConnect.setSQL("select count(*) as count  from Tree  Tree where Tree.child = ? ").addParam(cat.getInode());
		int count2 = dotConnect.getInt("count");
		dotConnect.setSQL("select count(*) as count from Field field where field_values like ?").addParam(cat.getInode());
		int count3 = dotConnect.getInt("count");

		return (count1 != 0) || (count2 != 0) || (count3 != 0);
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
			if (null != conn) {
				try {
					conn.rollback();
				} catch (SQLException sqlException) {
					//Quiet
				}
			}
            Logger.error( this, "Error trying to execute statements", e );
		} finally {
			CloseUtils.closeQuietly(rs,s,conn);
        }
	}

	/**
	 *
	 * @param inode
	 * @throws DotDataException
	 */
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
			if (null != conn) {
				try {
					conn.rollback();
				} catch (SQLException sqlException) {
					//Quiet
				}
			}
            Logger.error( this, "Error trying to execute statements", e );
		} finally {
			CloseUtils.closeQuietly(statement, conn, rs);
        }
	}

    private void putResultInCatCache( final ResultSet rs ) throws SQLException, DotDataException {
        while(rs.next()) {
			// calling find will put it into cache internally
			find(rs.getString(INODE));
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
        catCache.clearCache();
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
            var = VelocityUtil.convertToVelocityVariable(categoryVelVarName, false) + i;
        }
        throw new DotDataException("Unable to suggest a variable name.  Got to:" + var);
    }

	/**
	 *  Default Implementation for {@link CategoryFactory#findAll(CategorySearchCriteria)}
	 * @param searchCriteria Search Criteria
	 *
	 * @return
	 * @throws DotDataException
	 */
	public List<HierarchedCategory>   findAll(final CategorySearchCriteria searchCriteria)
			throws DotDataException, DotSecurityException {

		final String filter = SQLUtil.sanitizeParameter(searchCriteria.filter);

		final String query = CategoryQueryBuilderResolver.getQueryBuilder(searchCriteria).build();

		final DotConnect dc = new DotConnect().setSQL(query);

		if (UtilMethods.isSet(searchCriteria.rootInode) ) {
			dc.addParam(searchCriteria.rootInode);
		}

		if (UtilMethods.isSet(filter) ) {
			dc.addParam("%" + filter.toLowerCase() + "%");
			dc.addParam("%" + filter.toLowerCase() + "%");
			dc.addParam("%" + filter.toLowerCase() + "%");
		} else if (CategoryQueryBuilderResolver.mustUseRecursiveTemplate(searchCriteria)){
			dc.addParam("%%");
			dc.addParam("%%");
			dc.addParam("%%");
		}

		final List<Map<String, Object>> results = UtilMethods.isSet(searchCriteria.rootInode) ?
				dc.loadObjectResults().stream()
						.filter(map -> !map.get(INODE).equals(searchCriteria.rootInode))
						.collect(Collectors.toList()) : dc.loadObjectResults();

		final List<HierarchedCategory> categories = convertForHierarchedCategories(results);
		updateCache(categories);
		return categories;
	}

	private void updateCache(List<? extends Category> categories) throws DotDataException {
		for(final Category category : categories) {
			if(catCache.get(category.getInode()) == null)
				try {
					catCache.put(category);
				} catch (DotCacheException e) {
					throw new DotDataException(e.getMessage(), e);
				}
		}
	}

	/**
	 * Default implementation of {@link CategoryFactory#findHierarchy(Collection)}
	 *
	 * @param keys list of keys to search
	 * @return
	 * @throws DotDataException
	 */
	@Override
	public  List<HierarchyShortCategory> findHierarchy(final Collection<String> keys) throws DotDataException {

		if (keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}

		final String queryTemplate = "WITH RECURSIVE CategoryHierarchy AS ( SELECT " +
					"c.inode," +
					"c.inode AS root_inode," +
					"c.category_name," +
					"c.category_name AS root_category_name," +
					"c.category_key," +
					"c.category_key AS root_category_key," +
					"1 AS level," +
					"json_build_object('inode', c.inode, 'name', c.category_name, 'key', c.category_key)::varchar AS path " +
				"FROM Category c " +
				"WHERE c.category_key IN (%s) " +
			"UNION ALL " +
			"SELECT " +
					"c.inode, " +
					"ch.root_inode AS root_inode, " +
					"c.category_name, " +
					"ch.root_category_name AS root_category_name, " +
					"c.category_key, " +
					"ch.root_category_key AS root_category_key, " +
					"ch.level + 1 AS level, " +
					"CONCAT(json_build_object('inode', c.inode, 'name', c.category_name, 'key', c.category_key)::varchar, ',', ch.path) AS path " +
				"FROM Category c JOIN tree t ON c.inode = t.parent JOIN CategoryHierarchy ch ON t.child = ch.inode " +
			"),"  +
			"MaxLevels AS (SELECT root_inode, MAX(level) AS max_level FROM CategoryHierarchy GROUP BY root_inode) " +
			"SELECT " +
				"ch.root_inode as inode, ch.root_category_name as category_name,  ch.root_category_key as category_key," +
				"CONCAT('[', path, ']')::jsonb as path " +
			"FROM CategoryHierarchy ch JOIN MaxLevels ml ON ch.root_inode = ml.root_inode AND ch.level = ml.max_level;";


		final DotConnect dc = new DotConnect()
				.setSQL(String.format(queryTemplate, DotConnect.createParametersPlaceholder(keys.size())));

		keys.forEach(dc::addParam);

		return dc.loadObjectResults().stream().map(row -> {
			final List<ShortCategory> parentList = getShortCategories(row.get("path").toString());

			return new HierarchyShortCategory.Builder()
					.setInode(row.get(INODE).toString())
					.setName(row.get(CATEGORY_NAME).toString())
					.setKey(row.get(CATEGORY_KEY).toString())
					.setParentList(parentList.subList(0, parentList.size() - 1))
					.build();
		}).collect(Collectors.toList());
	}



}
