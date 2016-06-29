package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class FieldSql {

	public final static FieldSql instance = DbConnectionFactory.isMySql() ? new FieldSqlMysql() : null;

	/*
	 * DbConnectionFactory.isH2() ? new ContentTypeH2DB() :
	 * DbConnectionFactory.isMySql() ? new ContentTypeMySql() :
	 * DbConnectionFactory.isPostgres() ? new ContentTypePostgres() :
	 * DbConnectionFactory.isMsSql() ? new ContentTypeMSSQL() : new
	 * ContentTypeOracle();
	 */


	private String SELECT_ALL_FIELDS_AND = "select structure_inode, field_name, field_type, "
			+ "field_relation_type, field_contentlet, required, indexed, " + "listed, velocity_var_name, "
			+ "sort_order, field_values, regex_check, hint, default_value, fixed, read_only, "
			+ "searchable, unique_, mod_date, inode.inode as inode, owner, idate "
			+ "from inode,  field where inode.inode = field.inode and ";

	public String findById = SELECT_ALL_FIELDS_AND + " inode.inode =?";
}
