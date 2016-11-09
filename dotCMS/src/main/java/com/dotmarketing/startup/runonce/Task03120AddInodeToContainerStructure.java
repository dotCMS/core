package com.dotmarketing.startup.runonce;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UtilMethods;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * This task adds an extra column to the container_structures table: The "inoe"
 * column. This column is necessary to keep version standard
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 02-25-2015
 *
 */
public class Task03120AddInodeToContainerStructure implements StartupTask {

    private static final String SQL_ADD_INODE_COLUMN = "ALTER TABLE container_structures ADD container_inode varchar(36)";
    private static final String ORACLE_ADD_INODE_COLUMN = "ALTER TABLE container_structures ADD container_inode varchar2(36)";
    private static final String SQL_ADD_FK_INODE = "ALTER TABLE container_structures ADD CONSTRAINT FK_cs_inode FOREIGN KEY (container_inode) references inode(inode)";
    private static final String SQL_ADD_NOT_NULL = "ALTER TABLE container_structures MODIFY container_inode varchar(36) not null";
    private static final String POSTGRES_ADD_NOT_NULL = "ALTER TABLE container_structures ALTER COLUMN container_inode TYPE varchar(36), ALTER COLUMN container_inode SET NOT NULL";
    private static final String ORACLE_ADD_NOT_NULL = "ALTER TABLE container_structures MODIFY container_inode varchar2(36) not null";
    private static final String MSSQL_ADD_NOT_NULL = "ALTER TABLE container_structures ALTER COLUMN container_inode varchar(36) NOT NULL";
    private static final String SQL_INSERT_INTO_CONTAINER_STRUCTURE = "INSERT INTO container_structures(id, container_id, structure_id, code, container_inode) VALUES(?, ?, ?, ?, ?)";
    private static final String SQL_DELETE_FROM_CONTAINER = "DELETE FROM containers WHERE inode = ?";
    private static final String SQL_DELETE_FROM_CONTAINER_STRUCTURE = "DELETE FROM container_structures WHERE id = ?";
    private static final String SQL_UPDATE_CONTAINER_STRUCTURE_BY_IDENTIFIER = "UPDATE container_structures SET container_inode = ? WHERE container_id = ?";
    private static final String SQL_UPDATE_CONTAINER_STRUCTURE_BY_ID = "UPDATE container_structures SET container_inode = ? WHERE id = ?";
    private static final String SQL_GET_CONTAINER_VERSION = "SELECT identifier FROM container_version_info WHERE working_inode = ? AND live_inode = ?";
    private static final String SQL_GET_CONTAINER_STRUCTURE = "SELECT id, container_id, structure_id, code, container_inode FROM container_structures WHERE container_id = ?";
    private static final String SQL_GET_CONTAINER_ID = "select id, container_id, structure_id, container_inode from container_structures order by container_id, structure_id";

    private final String SQL_GET_NON_WORKING_LIVE_INODES = "SELECT containers.inode " +
            "FROM containers " +
            "WHERE containers.inode " +
            "NOT IN(SELECT containers.inode " +
                "FROM containers, container_version_info " +
                "WHERE containers.identifier = container_version_info.identifier " +
                "AND (containers.inode = container_version_info.working_inode " +
                    "OR containers.inode = container_version_info.live_inode))";

    private static final String SQL_GET_CONTAINER_IDENT_INODE = "SELECT containers.identifier, containers.inode " +
            "FROM containers, container_version_info " +
            "WHERE containers.identifier = container_version_info.identifier " +
            "AND (containers.inode = container_version_info.working_inode " +
                "OR containers.inode = container_version_info.live_inode)";

    /**
     * By Default tasks only execute once.  If you have a task that needs to execute more then once use this method.
     *
     * @return
     */
    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * The instructions to execute.
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotRuntimeException
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        DotConnect dc = new DotConnect();
        //Add Column inode to container_structures.
        dc.setSQL(SQL_ADD_INODE_COLUMN);
        if (DbConnectionFactory.isOracle()) {
            dc.setSQL(ORACLE_ADD_INODE_COLUMN);
        }
        dc.loadResults();

        //Gets list of inodes that are NOT live nor working to DELETE from container_structures.
        dc = new DotConnect();
        dc.setSQL(SQL_GET_NON_WORKING_LIVE_INODES);
        ArrayList<Map<String, String>> inodesToDelete = dc.loadResults();

        if (inodesToDelete != null && !inodesToDelete.isEmpty()) {
            for(Map<String, String> inodeToDelete : inodesToDelete){
                dc = new DotConnect();
                String inode = inodeToDelete.get("inode");

                dc.setSQL(SQL_DELETE_FROM_CONTAINER);
                dc.addParam(inode);
                dc.loadResults();
            }
        }

        //Delete all the rows in ContainerStructures before inserting inode.
        dc = new DotConnect();
        dc.setSQL(SQL_GET_CONTAINER_ID);
        ArrayList<Map<String, String>> identifiersContainerStructure = dc.loadResults();

        if (identifiersContainerStructure != null && !identifiersContainerStructure.isEmpty()) {
            //Use this variable to get the previous value of the identifier.
            String firstIdentifier = "";
            String firstStructure = "";

            for(Map<String, String> identifierContainerStructure : identifiersContainerStructure){
                String identifier = identifierContainerStructure.get("container_id");
                String structure = identifierContainerStructure.get("structure_id");

                //If the identifier and structure is the same that the previous one, we need to delete it to avoid duplicates.
                if(firstIdentifier.equals(identifier) && firstStructure.equals(structure)){
                    //Delete this row based on the id.
                    String id = identifierContainerStructure.get("id");

                    dc = new DotConnect();
                    dc.setSQL(SQL_DELETE_FROM_CONTAINER_STRUCTURE);
                    dc.addObject(id);
                    dc.loadResults();
                } else { //If it is not the same, we have a new identifier and we don't want to delete it.
                    firstIdentifier = identifier;
                    firstStructure = structure;
                }
            }
        }

        //Get list of identifier-inode to update container_structures.
        dc = new DotConnect();
        dc.setSQL(SQL_GET_CONTAINER_IDENT_INODE);
        ArrayList<Map<String, String>> containerIdentInodes = dc.loadResults();

        if (containerIdentInodes != null && !containerIdentInodes.isEmpty()) {
            for(Map<String, String> containerIdentInode : containerIdentInodes){
                String identifier = containerIdentInode.get("identifier");
                String inode = containerIdentInode.get("inode");

                dc = new DotConnect();
                dc.setSQL(SQL_GET_CONTAINER_VERSION);
                dc.addParam(inode);
                dc.addParam(inode);
                ArrayList<Map<String, String>> containerVersion = dc.loadResults();

                //If inode is both Working and Live, then update container_structures to add inode.
                if (containerVersion != null && !containerVersion.isEmpty()) {
                    dc = new DotConnect();
                    dc.setSQL(SQL_UPDATE_CONTAINER_STRUCTURE_BY_IDENTIFIER);
                    dc.addParam(inode);
                    dc.addParam(identifier);
                    dc.loadResult();

                } else {
                    //Else if inode is not same Working and Live, we need to duplicate row and add one inode for each.
                    dc = new DotConnect();
                    dc.setSQL(SQL_GET_CONTAINER_STRUCTURE);
                    dc.addParam(identifier);
                    ArrayList<Map<String, String>> containerStructures = dc.loadResults();

                    if (containerStructures != null && !containerStructures.isEmpty()) {
                        for (Map<String, String> containerStructure : containerStructures) {
                            String id = containerStructure.get("id");
                            String containerId = containerStructure.get("container_id");
                            String structureId = containerStructure.get("structure_id");
                            String code = containerStructure.get("code");
                            String containerInode = containerStructure.get("container_inode");

                            //If the row already has the inode means that the row is already updated.
                            //We need to duplicate the row with new ID and Inode.
                            if (UtilMethods.isSet(containerInode)) {
                                id = UUID.randomUUID().toString();

                                dc = new DotConnect();
                                dc.setSQL(SQL_INSERT_INTO_CONTAINER_STRUCTURE);
                                dc.addParam(id);
                                dc.addParam(containerId);
                                dc.addParam(structureId);
                                dc.addParam(code);
                                dc.addParam(inode);
                                dc.loadResult();

                            } else {
                                //If the row doesn't have the inode we just need to update the row to set the value.
                                dc = new DotConnect();
                                dc.setSQL(SQL_UPDATE_CONTAINER_STRUCTURE_BY_ID);
                                dc.addParam(inode);
                                dc.addParam(id);
                                dc.loadResult();
                            }
                        }
                    }
                }
            }
        }

        //Add FK between inode - container_structures.
        dc = new DotConnect();
        dc.setSQL(SQL_ADD_FK_INODE);
        dc.loadResults();

        //Add contraint not null to inode column.
        dc = new DotConnect();
        dc.setSQL(SQL_ADD_NOT_NULL);
        if (DbConnectionFactory.isPostgres()) {
            dc.setSQL(POSTGRES_ADD_NOT_NULL);
        }
        if (DbConnectionFactory.isOracle()) {
            dc.setSQL(ORACLE_ADD_NOT_NULL);
        }
        if (DbConnectionFactory.isMsSql()) {
            dc.setSQL(MSSQL_ADD_NOT_NULL);
        }
        dc.loadResults();
    }
}
