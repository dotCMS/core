package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UtilMethods;

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

    private static final String MYSQL_ADD_INODE_COLUMN = "ALTER TABLE container_structures ADD container_inode varchar(36)";
    private static final String MYSQL_ADD_FK_INODE = "ALTER TABLE container_structures ADD CONSTRAINT FK_cs_inode FOREIGN KEY (container_inode) references inode(inode)";
    private static final String MYSQL_ADD_NOT_NULL = "ALTER TABLE container_structures MODIFY container_inode varchar(36) not null";
    private static final String MYSQL_INSERT_INTO_CONTAINER_STRUCTURE = "INSERT INTO container_structures(id, container_id, structure_id, code, container_inode) VALUES(?, ?, ?, ?, ?)";
    private static final String MYSQL_DELETE_FROM_CONTAINER = "DELETE FROM containers WHERE inode = ?";
    private static final String MYSQL_UPDATE_CONTAINER_STRUCTURE_BY_IDENTIFIER = "UPDATE container_structures SET container_inode = ? WHERE container_id = ?";
    private static final String MYSQL_UPDATE_CONTAINER_STRUCTURE_BY_ID = "UPDATE container_structures SET container_inode = ? WHERE id = ?";
    private static final String MYSQL_GET_CONTAINER_VERSION = "SELECT identifier FROM container_version_info WHERE working_inode = ? AND live_inode = ?";
    private static final String MYSQL_GET_CONTAINER_STRUCTURE = "SELECT id, container_id, structure_id, code, container_inode FROM container_structures WHERE container_id = ?";

    private final String MYSQL_GET_NON_WORKING_LIVE_INODES = "SELECT containers.inode " +
            "FROM containers " +
            "WHERE containers.inode " +
            "NOT IN(SELECT containers.inode " +
                "FROM containers, container_version_info " +
                "WHERE containers.identifier = container_version_info.identifier " +
                "AND (containers.inode = container_version_info.working_inode " +
                    "OR containers.inode = container_version_info.live_inode))";

    private static final String MYSQL_GET_CONTAINER_IDENT_INODE = "SELECT containers.identifier, containers.inode " +
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

        DotConnect dc = new DotConnect();
        //Add Column inode to container_structures.
        dc.setSQL(MYSQL_ADD_INODE_COLUMN);
        dc.loadResults();

        //Gets list of inodes that are NOT live nor working to DELETE from container_structures.
        dc = new DotConnect();
        dc.setSQL(MYSQL_GET_NON_WORKING_LIVE_INODES);
        ArrayList<Map<String, String>> inodesToDelete = dc.loadResults();

        if (inodesToDelete != null && !inodesToDelete.isEmpty()) {
            for(Map<String, String> inodeToDelete : inodesToDelete){
                dc = new DotConnect();
                String inode = inodeToDelete.get("inode");

                dc.setSQL(MYSQL_DELETE_FROM_CONTAINER);
                dc.addParam(inode);
                dc.loadResults();
            }
        }

        //Get list of identifier-inode to update container_structures.
        dc = new DotConnect();
        dc.setSQL(MYSQL_GET_CONTAINER_IDENT_INODE);
        ArrayList<Map<String, String>> containerIdentInodes = dc.loadResults();

        if (containerIdentInodes != null && !containerIdentInodes.isEmpty()) {
            for(Map<String, String> containerIdentInode : containerIdentInodes){
                String identifier = containerIdentInode.get("identifier");
                String inode = containerIdentInode.get("inode");

                dc = new DotConnect();
                dc.setSQL(MYSQL_GET_CONTAINER_VERSION);
                dc.addParam(inode);
                dc.addParam(inode);
                ArrayList<Map<String, String>> containerVersion = dc.loadResults();

                //If inode is both Working and Live, then update container_structures to add inode.
                if (containerVersion != null && !containerVersion.isEmpty()) {
                    dc = new DotConnect();
                    dc.setSQL(MYSQL_UPDATE_CONTAINER_STRUCTURE_BY_IDENTIFIER);
                    dc.addParam(inode);
                    dc.addParam(identifier);
                    dc.loadResult();

                } else {
                    //Else if inode is not same Working and Live, we need to duplicate row and add one inode for each.
                    dc = new DotConnect();
                    dc.setSQL(MYSQL_GET_CONTAINER_STRUCTURE);
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
                                dc.setSQL(MYSQL_INSERT_INTO_CONTAINER_STRUCTURE);
                                dc.addParam(id);
                                dc.addParam(containerId);
                                dc.addParam(structureId);
                                dc.addParam(code);
                                dc.addParam(inode);
                                dc.loadResult();

                            } else {
                                //If the row doesn't have the inode we just need to update the row to set the value.
                                dc = new DotConnect();
                                dc.setSQL(MYSQL_UPDATE_CONTAINER_STRUCTURE_BY_ID);
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
        dc.setSQL(MYSQL_ADD_FK_INODE);
        dc.loadResults();

        //Add contraint not null to inode column.
        dc = new DotConnect();
        dc.setSQL(MYSQL_ADD_NOT_NULL);
        dc.loadResults();
    }
}
