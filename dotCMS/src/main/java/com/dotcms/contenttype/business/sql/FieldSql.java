package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class FieldSql {


		static FieldSql instance = DbConnectionFactory.isH2() 
				? new FieldSqlMysql() : DbConnectionFactory.isMySql() 
						? new FieldSqlMysql() : DbConnectionFactory.isPostgres() 
								? new FieldSqlMysql()
									: DbConnectionFactory.isMsSql() 
										? new FieldSqlMysql() : new FieldSqlMysql();

		public static FieldSql getInstance() {
			return instance;
		}
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
	public String findByContentTypeAndRelationType = SELECT_ALL_FIELDS
			+ ", structure where inode.inode = field.inode and structure_inode =? and field.field_relation_type= ? order by sort_order";
	
	
	public String findByContentTypeAndFieldVar = SELECT_ALL_FIELDS + " where inode.inode = field.inode and structure_inode =? and field.velocity_var_name=?";
	
	
	public String deleteByContentType = "delete from field where structure_inode = ?";
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
	
	
	public String selectCountOfType= "select count(*) as test from field where structure_inode = ? and field_type like ? or field_type like ?";
	
	public String selectFieldVars= 	"select id, field_id, variable_name, variable_key, variable_value, user_id, last_mod_date from field_variable where field_id = ? order by variable_key";
	public String selectFieldVar= 	"select id, field_id, variable_name, variable_key, variable_value, user_id, last_mod_date from field_variable where id = ? ";
	public String selectFieldVarByKey= 	"select id, field_id, variable_name, variable_key, variable_value, user_id, last_mod_date from field_variable where variable_key = ? ";
	public String selectFieldIdVarByKey= 	"select id, field_id, variable_name, variable_key, variable_value, user_id, last_mod_date from field_variable where field_id = ? and variable_key = ?";

    public String deleteFieldVar= 	"delete from field_variable where id = ? or (field_id=? and variable_key=?)";
	public String deleteFieldVarsForField= "delete from field_variable where field_id = ?";
	public String insertFieldVar= 	"insert into field_variable( id, field_id, variable_name, variable_key, variable_value, user_id, last_mod_date) values (?,?,?,?,?,?,?)";

	public static final String moveSorOrderForward = "update field set sort_order = sort_order + 1 where structure_inode = ? and sort_order >= ? and sort_order < ?";
	public static final String moveSorOrderBackward = "update field set sort_order = sort_order - 1 where structure_inode = ? and sort_order > ? and sort_order <= ?";
}
