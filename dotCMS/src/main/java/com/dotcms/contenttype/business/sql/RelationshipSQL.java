package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class RelationshipSQL {



	static RelationshipSQL instance = DbConnectionFactory.isH2() 
			? new RelationshipMySQL() : DbConnectionFactory.isMySql() 
					? new RelationshipMySQL() : DbConnectionFactory.isPostgres() 
							? new RelationshipMySQL()
								: DbConnectionFactory.isMsSql() 
									? new RelationshipMySQL() : new RelationshipMySQL();

	public static RelationshipSQL getInstance() {
		return instance;
	}

	public static final String DELETE_RELATIONSHIP="delete from relationship where parent_structure_inode = ? or child_structure_inode=?";
	
	
	
}
