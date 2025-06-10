package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;

/**
 * Upgrade task that sets the system_folder identifier to SYSTEM_FOLDER and updates all folders to use the identifier as
 * the inode;
 */
public class Task250604UpdateFolderInodes implements StartupTask {


    String ALLOW_DEFER_CONSTRAINT_SQL = "ALTER TABLE folder ALTER CONSTRAINT folder_identifier_fk DEFERRABLE;";
    String DEFER_CONSTRAINT_SQL = "SET CONSTRAINTS folder_identifier_fk DEFERRED;";
    String UPDATE_SYSTEM_FOLDER_IDENTIFIER = "update identifier set id ='SYSTEM_FOLDER' where parent_path = '/System folder' or id='"+ FolderAPI.OLD_SYSTEM_FOLDER_ID + "';";
    String UPDATE_SYSTEM_FOLDER_FOLDER = "update folder set identifier ='SYSTEM_FOLDER' where inode = 'SYSTEM_FOLDER' or inode='"+ FolderAPI.OLD_SYSTEM_FOLDER_ID + "';";
    String UPDATE_FOLDER_IDENTIFIERS="update identifier "
            + "set id =subquery.inode "
            + "from (select inode, identifier from folder) as subquery "
            + "where  "
            + "subquery.identifier =identifier.id;";



    String UPDATE_ALL_FOLDERS = "update folder set identifier = inode;";
    String DENY_DEFER_CONSTRAINT_SQL = "ALTER TABLE folder ALTER CONSTRAINT folder_identifier_fk NOT DEFERRABLE;";


    String SHOULD_BE_EMPTY = "select * from folder where inode <> identifier limit 1";
    String SHOULD_NOT_BE_EMPTY =
            "select * from folder where inode ='" + Folder.SYSTEM_FOLDER + "' and identifier='" + Folder.SYSTEM_FOLDER
                    + "'";


    @Override
    public boolean forceRun() {

        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            DotConnect db = new DotConnect();
            return !db.setSQL(SHOULD_BE_EMPTY).loadObjectResults(conn).isEmpty() || db.setSQL(SHOULD_NOT_BE_EMPTY)
                    .loadObjectResults(conn).isEmpty();
        } catch (Exception e) {
            Logger.error(this, e);
            throw new DotRuntimeException(e);
        }
    }


    /**
     * Executes the upgrade task, creating the necessary tables and indexes for the Job Queue.
     *
     * @throws DotDataException    if a data access error occurs.
     * @throws DotRuntimeException if a runtime error occurs.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        doFolderUpgrades();


    }


    void doFolderUpgrades(){
        Logger.info(this,
                "Found non-matching folder inodes/identifiers, running Task250604UpdateFolderInodes upgrade task.");



        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            // allow deferred constraints
            conn.createStatement().execute(ALLOW_DEFER_CONSTRAINT_SQL);

            conn.setAutoCommit(false);

            // defer folder/identifier constraint
            conn.createStatement().execute(DEFER_CONSTRAINT_SQL);

            // update system folder identifier
            conn.createStatement().execute(UPDATE_SYSTEM_FOLDER_IDENTIFIER);

            // update system folder folder
            conn.createStatement().execute(UPDATE_SYSTEM_FOLDER_FOLDER);

            // update folder ids with the inodes
            conn.createStatement().execute(UPDATE_FOLDER_IDENTIFIERS);

            // set all folder inodes=identifer
            conn.createStatement().execute(UPDATE_ALL_FOLDERS);

            conn.commit();

            conn.setAutoCommit(true);

            // re-enable constraints
            conn.createStatement().execute(DENY_DEFER_CONSTRAINT_SQL);
        } catch (Exception e) {
            Logger.error(this, e);
            throw new DotRuntimeException(e);
        }


        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            conn.createStatement().execute(DENY_DEFER_CONSTRAINT_SQL);
        } catch (Exception e) {
            Logger.error(this, e);
            throw new DotRuntimeException(e);
        }

        // just in case
        CacheLocator.getFolderCache().clearCache();


    }

}
