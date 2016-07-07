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

	private final String SELECT_ALL_FIELDS = "select structure_inode, field_name, field_type, "
			+ "field_relation_type, field_contentlet, required, indexed, " + "listed, field.velocity_var_name as velocity_var_name, "
			+ "sort_order, field_values, regex_check, hint, default_value, field.fixed as fixed, field.read_only as read_only, "
			+ "field.searchable as searchable, unique_, field.mod_date as mod_date, inode.inode as inode, owner, idate from inode, field ";

	public String findById = SELECT_ALL_FIELDS + " where inode.inode = field.inode and inode.inode =?";
	public String findByContentType = SELECT_ALL_FIELDS + " where inode.inode = field.inode and structure_inode =? order by sort_order";
	public String findByContentTypeVar = SELECT_ALL_FIELDS
			+ ", structure where inode.inode = field.inode and field.structure_inode = structure.inode and structure.velocity_var_name= ? order by sort_order";

	public String deleteById = "delete from field where inode = ?";
	public String deleteInodeById = "delete from inode where inode = ? and type='field'";
	
	public String updateField = "update field set "
			+ "structure_inode=?, "
			+ "field_name=?, "
			+ "field_type=?, "
			+ "field_relation_type=?, "
			+ "field_contentlet=?, "
			+ "required=?, "
			+ "indexed=?, "
			+ "listed=?, "
			+ "velocity_var_name=?, "
			+ "sort_order=?, "
			+ "field_values=?, "
			+ "regex_check=?, "
			+ "hint=?, "
			+ "default_value=?, "
			+ "fixed=?, "
			+ "read_only=?, "
			+ "searchable=?, "
			+ "unique_=?, "
			+ "mod_date=? "
			+ "where inode =?";
	
	public String updateFieldInode = "update inode set inode=?, idate=?, owner = ? where inode = ? and type='field'";
	
	public String insertFieldInode = "insert into inode (inode, idate, owner, type) values (?,?,?,'field')";
	
	public String insertField = "insert into field ( "
			+ "inode,"
			+ "structure_inode , "
			+ "field_name , "
			+ "field_type , "
			+ "field_relation_type , "
			+ "field_contentlet , "
			+ "required , "
			+ "indexed , "
			+ "listed , "
			+ "velocity_var_name , "
			+ "sort_order , "
			+ "field_values , "
			+ "regex_check , "
			+ "hint , "
			+ "default_value , "
			+ "fixed , "
			+ "read_only , "	
			+ "searchable , "
			+ "unique_ , "
			+ "mod_date  )"
			+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	
	
	
	public String inodeCount = "select count(inode) as inode_count from field where inode = ?";
	
	
	
	public String selectFieldOfDbType = "select field_contentlet from field where structure_inode = ? and field_contentlet like ? order by field_contentlet";
}
