package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class ContentTypeSql {

	
	static ContentTypeSql instance = DbConnectionFactory.isH2() ? new ContentTypeH2DB() : 
		DbConnectionFactory.isMySql() ? new ContentTypeMySql() :
			DbConnectionFactory.isPostgres() ? new ContentTypePostgres() :
				DbConnectionFactory.isMsSql() ? new ContentTypeMSSQL() :
					new ContentTypeOracle();
	
	public static ContentTypeSql getInstance() {
		return instance;
	}
	private final String SELECT_ALL_STRUCTURE_FIELDS = "select  inode.inode as inode, owner, idate as idate, name, description, default_structure, page_detail, structuretype, system, fixed, velocity_var_name , url_map_pattern , host, folder, expire_date_var , publish_date_var , mod_date   from inode, structure  where  inode.inode = structure.inode  ";

	public String findById =  SELECT_ALL_STRUCTURE_FIELDS + " and inode.inode = ?";
	public String findByVar = SELECT_ALL_STRUCTURE_FIELDS + " and structure.velocity_var_name = ?";
	public String findAll =   SELECT_ALL_STRUCTURE_FIELDS + " order by %s  ";
	public String finType =   SELECT_ALL_STRUCTURE_FIELDS + " where structure_type= ? order by %s ";
}
