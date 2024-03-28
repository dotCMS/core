package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

/**
 * Utility class which provides the different SQL queries that can be used by the {@link
 * com.dotcms.contenttype.business.ContentTypeFactory} to return information about Content Types in dotCMS. This class
 * serves as the main access point to add and reuse different SQL queries that return specific information required by
 * the Content Type API.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
public abstract class ContentTypeSql {

	private static ContentTypeSql instance;

	public static ContentTypeSql getInstance() {
		if (instance == null){
			instance = DbConnectionFactory.isMySql() ? new ContentTypeMySql() :
					DbConnectionFactory.isPostgres() ? new ContentTypePostgres()
							: DbConnectionFactory.isMsSql() ? new ContentTypeMSSQL() :
							new ContentTypeOracle();
		}
		return instance;
	}

	public static final String NON_MARKED_FOR_DELETION = " and marked_for_deletion = " + DbConnectionFactory.getDBFalse() + " ";

	public static String SELECT_ALL_STRUCTURE_FIELDS = "select  inode.inode as inode, owner, idate as idate, name, "
			+ "description, default_structure, page_detail, structuretype, system, fixed, velocity_var_name , "
			+ "url_map_pattern , host, folder, expire_date_var , publish_date_var , mod_date, icon, marked_for_deletion, sort_order, metadata "
			+ "from inode, structure  where inode.type='structure' and inode.inode = structure.inode ";

	public static String SELECT_ALL_STRUCTURE_FIELDS_EXCLUDE_MARKED_FOR_DELETE = SELECT_ALL_STRUCTURE_FIELDS + NON_MARKED_FOR_DELETION;

	public static String SELECT_ONLY_INODE_FIELD = "select inode.inode as inode from inode, structure  where inode.type='structure' and inode.inode = structure.inode  ";

	public static String SELECT_BY_INODE = SELECT_ALL_STRUCTURE_FIELDS + " and inode.inode = ?";
	public static String SELECT_BY_VAR = SELECT_ALL_STRUCTURE_FIELDS + " and lower(structure.velocity_var_name) like ?";
	public static String SELECT_BY_VAR_NAMES = SELECT_ALL_STRUCTURE_FIELDS + " AND LOWER(structure.velocity_var_name) IN (%s)";
	public static String SELECT_BY_VAR_NAMES_FILTERED = SELECT_BY_VAR_NAMES + " AND (LOWER(name) LIKE ? OR LOWER(structure.velocity_var_name) LIKE ?)";
	public static String SELECT_ALL = SELECT_ALL_STRUCTURE_FIELDS_EXCLUDE_MARKED_FOR_DELETE + " order by %s  ";
	public static String SELECT_BY_TYPE = SELECT_ALL_STRUCTURE_FIELDS_EXCLUDE_MARKED_FOR_DELETE + " and structuretype= ? order by %s ";
	public static String SELECT_DEFAULT_TYPE = SELECT_ALL_STRUCTURE_FIELDS_EXCLUDE_MARKED_FOR_DELETE + " and default_structure = " + DbConnectionFactory.getDBTrue();

	public static String UPDATE_TYPE_INODE = "update inode set owner = ? where inode = ? and type='structure'";

	public static String INSERT_TYPE_INODE = "insert into inode (inode, idate, owner, type) values (?,?,?,'structure')";

	public static String INSERT_TYPE = "insert into structure(inode,name,description,default_structure,page_detail,"
			+ "structuretype,system,fixed,velocity_var_name,url_map_pattern,host,folder,expire_date_var,publish_date_var,mod_date,icon,sort_order,marked_for_deletion,metadata) "
			+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public static String UPDATE_TYPE = "update structure set "
			+ "name=?, "
			+ "description=? ,"
			+ "default_structure = ?,"
			+ "page_detail=?,"
			+ "structuretype=?,"
			+ "system=?,"
			+ "fixed=?,"
			+ "velocity_var_name=?,"
			+ "url_map_pattern=?,"
			+ "host=?,folder=?,"
			+ "expire_date_var=?,"
			+ "publish_date_var=?,"
			+ "mod_date=?,"
			+ "icon=?,"
			+ "sort_order=?, "
			+ "marked_for_deletion=?, "
			+ "metadata=? "
			+ "where inode=?";

	public static String SELECT_QUERY_CONDITION = SELECT_ALL_STRUCTURE_FIELDS_EXCLUDE_MARKED_FOR_DELETE
			+ " and (inode.inode like ? or lower(name) like ? or velocity_var_name like ?) "  //search
			+ " %s" //if we have a condition
			+ " and (host like %s) "
			+ " and structuretype >= ? and structuretype <= ? "
			+ " order by %s";

	public static String SELECT_INODE_ONLY_QUERY_CONDITION = SELECT_ONLY_INODE_FIELD
			+ " and (inode.inode like ? or lower(name) like ? or velocity_var_name like ?) "  //search
			+ " %s" //if we have a condition
			+ " and (host like %s) "
			+ " and structuretype >= ? and structuretype <= ? "
			+  NON_MARKED_FOR_DELETION
			+ " order by %s";

	public static final String SELECT_COUNT_CONDITION = "select count(*) as test from structure, inode "
			+ "where inode.type='structure' and inode.inode=structure.inode and "
			+ " (inode.inode like ? or lower(name) like ? or velocity_var_name like ?) "
			+ " %s" //if we have a condition
			+ " and structuretype>=? and structuretype<= ?"
			+  NON_MARKED_FOR_DELETION;

	public static String SELECT_COUNT_VAR="select count(*) as test from structure where lower(velocity_var_name) like ?";

	public static String UPDATE_ALL_DEFAULT = "update structure set default_structure=?";

	public static String DELETE_INODE_BY_INODE = "delete from inode where inode = ? and type='structure'";

	public static String DELETE_TYPE_BY_INODE = "delete from structure where inode =?";
	
	public static String UPDATE_TYPE_MOD_DATE_BY_INODE = "update structure set mod_date = ? where inode = ?";

	public static String ORDER_BY = " ORDER BY %s";

	public static final String MARK_FOR_DELETION = "update structure set marked_for_deletion = "+DbConnectionFactory.getDBTrue()+" where inode = ?";

	public static String COUNT_CONTENT_TYPES_USING_NOT_SYSTEM_WORKFLOW = "select count(distinct structure_id) from workflow_scheme_x_structure " +
			"INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_scheme_x_structure.scheme_id " +
			"where name != 'System Workflow'";

}
