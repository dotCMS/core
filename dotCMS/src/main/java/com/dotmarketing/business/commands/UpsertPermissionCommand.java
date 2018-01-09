package com.dotmarketing.business.commands;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.commands.DatabaseCommand;
import com.dotmarketing.exception.DotDataException;

/**
 * Command to generate and execute a native SQL Upsert for the PermissionReference table
 * @author Andre Curione
 */
public abstract class UpsertPermissionCommand implements DatabaseCommand {

    protected static final String PERMISSION_REFERENCE = "permission_reference";
    protected static final String ASSET_ID = "asset_id";
    protected static final String REFERENCE_ID = "reference_id";
    protected static final String PERMISSION_TYPE = "permission_type";
    protected static final String ID = "id";

    protected DotConnect dc;
    protected String permissionId;
    protected Permissionable newReference;
    protected String type;

    /**
     * Factory method to instantiate the proper UpsertPermissionCommand based on the current Database type,
     * Also receives the parameters for the Upsert Permission_Reference query
     * @param dc DotConnect, if provided this DotConnect will be used, if null then a new DotConnect instance will be created
     * @param permissionId
     * @param newReference
     * @param type
     * @return concrete instance of UpsertPermissionCommand
     */
    public static UpsertPermissionCommand createUpsertPermissionCommand (DotConnect dc, String permissionId,
            Permissionable newReference, String type) throws DotDataException {

        UpsertPermissionCommand command = null;
        if (DbConnectionFactory.isH2()) {
            command = new H2UpsertPermissionCommand();
        }
        if (DbConnectionFactory.isPostgres()) {
            command = new PostgresUpsertPermissionCommand();
        }
        if (DbConnectionFactory.isMySql()) {
            command = new MySQLUpsertPermissionCommand();
        }
        if (DbConnectionFactory.isMsSql()) {
            command = new MSSQLUpsertPermissionCommand();
        }
        if (DbConnectionFactory.isOracle()) {
            command = new OracleUpsertPermissionCommand();
        }

        if (command == null) {
            throw new DotDataException("Datatabase not implemented in UpsertPermissionCommand");
        }

        command.dc = (dc != null) ? dc : new DotConnect();
        command.permissionId = permissionId;
        command.newReference = newReference;
        command.type = type;
        return command;
    }

}

final class H2UpsertPermissionCommand extends UpsertPermissionCommand {

    @Override
    public void execute() throws DotDataException {
        String query = SQLUtil.generateUpsertSQL(PERMISSION_REFERENCE, ASSET_ID,
                new String[]{ASSET_ID, REFERENCE_ID, PERMISSION_TYPE},
                new String[]{SQLUtil.PARAMETER, SQLUtil.PARAMETER, SQLUtil.PARAMETER});
        dc.executeUpdate(query, permissionId, newReference.getPermissionId(), type);
    }
}

final class PostgresUpsertPermissionCommand extends UpsertPermissionCommand {

    @Override
    public void execute() throws DotDataException {
        String query = SQLUtil.generateUpsertSQL(PERMISSION_REFERENCE, ASSET_ID,
                new String[]{ID, ASSET_ID, REFERENCE_ID, PERMISSION_TYPE},
                new String[]{"nextval('permission_reference_seq')", SQLUtil.PARAMETER, SQLUtil.PARAMETER, SQLUtil.PARAMETER});
        this.dc.executeUpdate(query, permissionId, newReference.getPermissionId(), type,
                permissionId, newReference.getPermissionId(), type);
    }
}

final class MySQLUpsertPermissionCommand extends UpsertPermissionCommand {

    @Override
    public void execute() throws DotDataException {
        String query = SQLUtil.generateUpsertSQL(PERMISSION_REFERENCE, ASSET_ID,
                new String[]{ASSET_ID, REFERENCE_ID, PERMISSION_TYPE},
                new String[]{SQLUtil.PARAMETER, SQLUtil.PARAMETER, SQLUtil.PARAMETER});
        this.dc.executeUpdate(query, permissionId, newReference.getPermissionId(), type,
                permissionId, newReference.getPermissionId(), type);
    }
}

final class MSSQLUpsertPermissionCommand extends UpsertPermissionCommand {

    @Override
    public void execute() throws DotDataException {
        String query = SQLUtil.generateUpsertSQL(PERMISSION_REFERENCE, ASSET_ID,
                new String[]{ASSET_ID, REFERENCE_ID, PERMISSION_TYPE},
                new String[]{SQLUtil.PARAMETER, SQLUtil.PARAMETER, SQLUtil.PARAMETER});
        dc.executeUpdate(query, permissionId,
                permissionId, newReference.getPermissionId(), type,
                permissionId, newReference.getPermissionId(), type);
    }
}

final class OracleUpsertPermissionCommand extends UpsertPermissionCommand {

    @Override
    public void execute() throws DotDataException {
        String query = SQLUtil.generateUpsertSQL(PERMISSION_REFERENCE, ASSET_ID,
                new String[]{ID, ASSET_ID, REFERENCE_ID, PERMISSION_TYPE},
                new String[]{"permission_reference_seq.NEXTVAL", SQLUtil.PARAMETER, SQLUtil.PARAMETER, SQLUtil.PARAMETER});
        try {
            //In Oracle the Upsert (Merge) is not thread safe. Attempt to insert first:
            dc.executeUpdate(query, false,
                    permissionId, newReference.getPermissionId(), type,
                    permissionId, newReference.getPermissionId(), type);
        } catch (DotDataException ex) {
            if (SQLUtil.isUniqueConstraintException(ex)) {
                //On Unique constraint exception, attempt to update:
                dc.executeUpdate(query, permissionId,
                        newReference.getPermissionId(), type,
                        permissionId, newReference.getPermissionId(), type);
            } else {
                throw ex;
            }
        }
    }
}