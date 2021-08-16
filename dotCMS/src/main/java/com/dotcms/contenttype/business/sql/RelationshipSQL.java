package com.dotcms.contenttype.business.sql;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class RelationshipSQL {



	static RelationshipSQL instance = DbConnectionFactory.isMySql()
					? new RelationshipMySQL() : DbConnectionFactory.isPostgres() 
							? new RelationshipMySQL()
								: DbConnectionFactory.isMsSql() 
									? new RelationshipMySQL() : new RelationshipMySQL();

	public static RelationshipSQL getInstance() {
		return instance;
	}

	public static final String DELETE_RELATIONSHIP_BY_PARENT_OR_CHILD_INODE="delete from relationship where parent_structure_inode = ? or child_structure_inode=?";

	public static final String SELECT_ALL_FIELDS = "select inode, parent_structure_inode, child_structure_inode, "
			+ "parent_relation_name, child_relation_name, relation_type_value, cardinality, "
			+ "parent_required, child_required, fixed, mod_date from relationship";

	public static final String FIND_BY_INODE = SELECT_ALL_FIELDS + " where inode = ?";

	public static final String FIND_BY_PARENT_INODE = SELECT_ALL_FIELDS + " where parent_structure_inode = ?";

	public static final String FIND_BY_CHILD_INODE = SELECT_ALL_FIELDS + " where child_structure_inode = ?";
	
	public static final String FIND_BY_PARENT_OR_CHILD_INODE = SELECT_ALL_FIELDS
			+ " where (parent_structure_inode = ? or child_structure_inode = ?)";

	public static final String FIND_BY_PARENT_CHILD_AND_RELATION_NAME =
			FIND_BY_PARENT_OR_CHILD_INODE
					+ " and (parent_relation_name=? or child_relation_name=?)";


	public static final String FIND_BY_TYPE_VALUE = SELECT_ALL_FIELDS + " where lower(relation_type_value) = ?";

	public static final String FIND_BY_TYPE_VALUE_LIKE = SELECT_ALL_FIELDS + " where lower(relation_type_value) like (?)";

	public static final String INSERT_INODE = "insert into inode (inode, idate, owner, type) values (?,?,?,'relationship')";

	public static final String INSERT_RELATIONSHIP = "insert into relationship (inode, parent_structure_inode, child_structure_inode, "
			+ "parent_relation_name, child_relation_name, relation_type_value, cardinality, "
			+ "parent_required, child_required, fixed, mod_date) values(?,?,?,?,?,?,?,?,?,?,?)";

	public static final String UPDATE_INODE = "update inode set inode = ?, idate = ?, owner = ? where inode = ? and type='relationship'";

	public static final String UPDATE_RELATIONSHIP = "update relationship set parent_structure_inode = ?,"
			+ " child_structure_inode = ?, parent_relation_name = ?, child_relation_name = ?,"
			+ " relation_type_value = ?, cardinality = ?, parent_required = ?, child_required = ?, "
			+ " fixed = ?, mod_date = ? where inode = ?";

	public static final String DELETE_RELATIONSHIP_BY_INODE = "delete from relationship where inode = ?";

	public static final String DELETE_INODE = "delete from inode where inode = ? and type='relationship'";

	public static final String SELECT_MAX_TREE_ORDER = "select max(tree_order) as tree_order from tree"
			+ " where parent = ? and relation_type = ?";

	public static final String SELECT_ONE_SIDE_RELATIONSHIP = "select * from relationship where ((child_structure_inode = ?"
			+ " and parent_relation_name is null) or (parent_structure_inode = ?"
			+ " and child_relation_name is null)) order by parent_relation_name";


	public static final String SELECT_ONE_SIDE_RELATIONSHIP_COUNT = "select count(*) as relationship_count from relationship where ((child_structure_inode = ?"
			+ " and parent_relation_name is null) or (parent_structure_inode = ?"
			+ " and child_relation_name is null))";
}
