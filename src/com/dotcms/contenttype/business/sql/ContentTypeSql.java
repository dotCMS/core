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

	public String findById = SELECT_ALL_STRUCTURE_FIELDS + " and inode.inode = ?";
	public String findByVar = SELECT_ALL_STRUCTURE_FIELDS + " and structure.velocity_var_name = ?";
	public String findAll = SELECT_ALL_STRUCTURE_FIELDS + " order by %s  ";
	public String findType = SELECT_ALL_STRUCTURE_FIELDS + " where structure_type= ? order by %s ";
	public String findDefaultContentType = SELECT_ALL_STRUCTURE_FIELDS + " where default_structure= true ";

	public String updateContentTypeInode = "update inode set inode=?, idate=?, owner = ? where inode = ? and type='structure'";

	public String insertContentTypeInode = "insert into inode (inode, idate, owner, type) values (?,?,?,'structure')";

	public String insertContentType = "insert into structure "
			+ "(inode,name,description,default_structure,page_detail,structuretype,system,fixed,velocity_var_name,url_map_pattern,host,folder,expire_date_var,publish_date_var,mod_date) "
			+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public String updateContentType = "update structure set "
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

	
	public String searchStructures = SELECT_ALL_STRUCTURE_FIELDS 
			+ " and (inode.inode like ? or name like ? or velocity_var_name like ?) "  //search
			+ " %s" //if we have a condition
			+ " and structuretype>=? and structuretype<= ? order by %s";
	
	public String countStructures = "select count(*) as test from structure, inode where inode.type='structure' and inode.inode=structure.inode and "
			+ " (inode.inode like ? or name like ? or velocity_var_name like ?) "
			+ " %s" //if we have a condition
			+ " and structuretype>=? and structuretype<= ? ";
	
	
	public String tryVelocityVar="select count(*) from structure where velocity_var_name like ?";
	
	public String updateAllDefaultToFalse = "update structure set default_structure=?";
	/**
	 * Fields in the db inode owner idate type inode name description
	 * default_structure page_detail structuretype system fixed
	 * velocity_var_name url_map_pattern host folder expire_date_var
	 * publish_date_var mod_date
	 */

}
