package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class ContentTypeSql {

	static ContentTypeSql instance = DbConnectionFactory.isH2() ? new ContentTypeH2DB()
			: DbConnectionFactory.isMySql() ? new ContentTypeMySql() : DbConnectionFactory.isPostgres() ? new ContentTypePostgres()
					: DbConnectionFactory.isMsSql() ? new ContentTypeMSSQL() : new ContentTypeOracle();

	public static ContentTypeSql getInstance() {
		return instance;
	}

	private final String SELECT_ALL_STRUCTURE_FIELDS = "select  inode.inode as inode, owner, idate as idate, name, description, default_structure, page_detail, structuretype, system, fixed, velocity_var_name , url_map_pattern , host, folder, expire_date_var , publish_date_var , mod_date   from inode, structure  where inode.type='structure' and inode.inode = structure.inode  ";

	public final String SELECT_BY_INODE = SELECT_ALL_STRUCTURE_FIELDS + " and inode.inode = ?";
	public final String SELECT_BY_VAR = SELECT_ALL_STRUCTURE_FIELDS + " and structure.velocity_var_name = ?";
	public final String SELECT_ALL = SELECT_ALL_STRUCTURE_FIELDS + " order by %s  ";
	public final String SELECT_BY_TYPE = SELECT_ALL_STRUCTURE_FIELDS + " and structuretype= ? order by %s ";
	public final String SELECT_DEFAULT_TYPE = SELECT_ALL_STRUCTURE_FIELDS + " and default_structure= true ";

	public final String UPDATE_TYPE_INODE = "update inode set inode=?, idate=?, owner = ? where inode = ? and type='structure'";

	public final String INSERT_TYPE_INODE = "insert into inode (inode, idate, owner, type) values (?,?,?,'structure')";

	public final String INSERT_TYPE = "insert into structure "
			+ "(inode,name,description,default_structure,page_detail,structuretype,system,fixed,velocity_var_name,url_map_pattern,host,folder,expire_date_var,publish_date_var,mod_date) "
			+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public final String UPDATE_TYPE = "update structure set "
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
			+ "mod_date=? "
			+ "where inode=?";

	
	public final String SELECT_QUERY_CONDITION = SELECT_ALL_STRUCTURE_FIELDS 
			+ " and (inode.inode like ? or name like ? or velocity_var_name like ?) "  //search
			+ " %s" //if we have a condition
			+ " and structuretype>=? and structuretype<= ? order by %s";
	
	public final String SELECT_COUNT_CONDITION = "select count(*) as test from structure, inode where inode.type='structure' and inode.inode=structure.inode and "
			+ " (inode.inode like ? or name like ? or velocity_var_name like ?) "
			+ " %s" //if we have a condition
			+ " and structuretype>=? and structuretype<= ? ";
	
	
	public final String SELECT_COUNT_VAR="select count(*) as test from structure where velocity_var_name like ?";
	
	public final String UPDATE_ALL_DEFUALT = "update structure set default_structure=?";
	/**
	 * Fields in the db inode owner idate type inode name description
	 * default_structure page_detail structuretype system fixed
	 * velocity_var_name url_map_pattern host folder expire_date_var
	 * publish_date_var mod_date
	 */

}
