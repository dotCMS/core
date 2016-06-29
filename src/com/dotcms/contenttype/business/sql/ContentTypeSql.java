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

	public String findById = "select  inode.inode as inode, owner, idate as idate, name, description, default_structure, page_detail, structuretype, system, fixed, velocity_var_name , url_map_pattern , host, folder, expire_date_var , publish_date_var , mod_date   from inode, structure  where inode.inode = ?  and inode.inode = structure.inode";

}
