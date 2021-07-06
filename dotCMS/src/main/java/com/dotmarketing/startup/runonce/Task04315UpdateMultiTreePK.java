package com.dotmarketing.startup.runonce;

import com.google.common.collect.ImmutableList;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.sql.Connection;
import java.util.List;

/**
 * This task updates the definition of the {@code workflow_action} table, creates the new
 * intermediate {@code workflow_action_step} table, and updates the definition of a table index:
 * <ul>
 * <li>A new column called {@code scheme_id} will be added to the {@code workflow_action} table.
 * This allows actions to be assigned to Workflow Schemes and not to steps directly.</li>
 * <li>The new {@code workflow_action_step} table allows an action to be associated to one or more
 * steps (an N-N relationship).</li>
 * <li>The index created for the {@code workflow_scheme_x_structure} column will be updated to NOT
 * be unique.</li>
 * </ul>
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Nov 1st, 2017
 */
public class Task04315UpdateMultiTreePK extends AbstractJDBCStartupTask{



    final static String tableName ="multi_tree";
    final static String indexName ="idx_multitree_index1";
    final static String columnNames = "parent1, parent2, child, relation_type";

    
    
    final static String updateRelationTypesSQL =   "update multi_tree set relation_type='" + MultiTree.LEGACY_RELATION_TYPE + "' where relation_type is null or relation_type=''";

    private static final String MSSQL_SET_NOT_NULL_RELATION_TYPE = "ALTER TABLE multi_tree ALTER COLUMN relation_type NVARCHAR(64) NOT NULL";
    private static final String MYSQL_SET_NOT_NULL_RELATION_TYPE = "ALTER TABLE multi_tree MODIFY relation_type VARCHAR(64) NOT NULL";
    private static final String ORACLE_SET_NOT_NULL_RELATION_TYPE = "ALTER TABLE multi_tree MODIFY relation_type VARCHAR2(64) NOT NULL";
    private static final String POSTGRES_SET_NOT_NULL_RELATION_TYPE = "ALTER TABLE multi_tree ALTER COLUMN relation_type SET NOT NULL";

    final static String alterRelationTypePK =   "ALTER TABLE " + tableName + " ADD CONSTRAINT " + indexName + " PRIMARY KEY (" + columnNames + ")";
    
    final static String addIndexToMultiTree =   "CREATE INDEX  idx_multi_tree_page_relation on " + tableName + " (parent1, relation_type) ";
    
    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        
        try {
            Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            
            // Drop PK
            getPrimaryKey(conn,ImmutableList.of(tableName), true );
            
            // Drop Indexes
            this.getIndexes(conn, ImmutableList.of(tableName), true );
            
            //Update relation_type null or empty values to LEGACY_RELATION_TYPE
            DotConnect db = new DotConnect();
            db.setSQL(updateRelationTypesSQL);
            db.loadResult(conn);

            //Change relation_type to Not null
            if (DbConnectionFactory.isMySql()){
                db.setSQL(MYSQL_SET_NOT_NULL_RELATION_TYPE);
            }
            if (DbConnectionFactory.isMsSql()) {
                db.setSQL(MSSQL_SET_NOT_NULL_RELATION_TYPE);
            }
            if (DbConnectionFactory.isOracle()) {
                db.setSQL(ORACLE_SET_NOT_NULL_RELATION_TYPE);
            }
            if (DbConnectionFactory.isPostgres()) {
                db.setSQL(POSTGRES_SET_NOT_NULL_RELATION_TYPE);
            }
            db.loadResult(conn);

            //Add PKs
            db.setSQL(alterRelationTypePK);
            db.loadResult(conn);
            
            //Add Indexes
            db.setSQL(addIndexToMultiTree);
            db.loadResult(conn);

        }
        catch(Exception e) {
            throw new DotDataException(e);
        }

    } // executeUpgrade.

    @Override
    public String getPostgresScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMySQLScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOracleScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMSSQLScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        // TODO Auto-generated method stub
        return null;
    }


}