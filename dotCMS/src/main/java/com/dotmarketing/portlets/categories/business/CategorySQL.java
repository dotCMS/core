package com.dotmarketing.portlets.categories.business;

import com.dotmarketing.db.DbConnectionFactory;

abstract class CategorySQL {
	protected static final String MYSQL = "MySQL";
	protected static final String POSTGRESQL = "PostgreSQL";
	protected static final String ORACLE = "Oracle";
	protected static final String MSSQL = "Microsoft SQL Server";

	static protected CategorySQL getInstance() {
		String x = DbConnectionFactory.getDBType();
		if (MYSQL.equals(x)) {
			return new MySQLCategorySQL();
		} else if (POSTGRESQL.equals(x)) {
			return new PostgresCategorySQL();
		} else if (MSSQL.equals(x)) {
			return new MSSQLCategorySQL();
		} else if (ORACLE.equals(x))  {
			return new OracleCategorySQL();
		}else {
			return new H2CategorySQL();
		}
	}
	
	public abstract String getCreateSortTopLevel();
	public abstract String getCreateSortChildren();
	public abstract String getUpdateSort();
	public abstract String getDropSort();
	public abstract String createCategoryReorderTable();

	public String getSortParents() {
		return " SELECT category.inode " +
				" from category left join tree tree on category.inode = tree.child, " + 
				" inode category_1_ where tree.child is null and category_1_.inode = category.inode and category_1_.type = 'category' ";
	}
	
	public String getSortedChildren() {
		return "SELECT category.inode from inode category_1_, category, tree where " +
				"category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
				" and category_1_.type = 'category'";
	}
	
	public String getVelocityVarNameCount() {
	    return "select count(*) as test from category where category_velocity_var_name like ?";
	}

 }
