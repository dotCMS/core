package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Task05050FileAssetContentTypeReadOnlyFileName implements StartupTask {

    private static final String SQL_SELECT_FIELD_INODE_FROM_STRUCTURE
            = "select "
            + " f.inode as field_inode "
            + " from inode i, field f, structure s "
            + " where i.inode = f.inode "
            + " and s.structuretype = 4 " //all subtypes of File, that includes (Video, FileAsset, Document)
            + " and f.structure_inode = s.inode "
            + " and f.velocity_var_name = 'fileName' ";

    private static final String MySQL_UPDATE_FIELD = "update field set read_only = 0 where inode in (%s) ";
    private static final String MS_SQL_UPDATE_FIELD = "update field set read_only = 0 where inode in (%s) ";
    private static final String POSTGRE_SQL_UPDATE_FIELD = "update field set read_only = false where inode in (%s) ";
    private static final String ORACLE_SQL_UPDATE_FIELD = "update field set read_only = 0 where inode in (%s) ";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this, "Changing FileAsset CT FileName field for :" + DbConnectionFactory.getDBType());
        try{
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try{

                final DotConnect dotConnect = new DotConnect();
                dotConnect.setSQL(SQL_SELECT_FIELD_INODE_FROM_STRUCTURE);
                final List<Map<String,Object>> structureData = dotConnect.loadObjectResults();
                if(structureData.isEmpty()){
                    throw new IllegalStateException("Failed to locate the field 'fileName' on structure 'File Asset' ");
                }

                final String fieldInodes = inStatement(structureData);
                Logger.debug(getClass(),()->" field  Inode: "+fieldInodes );

                String updateStatement = null;

                if(DbConnectionFactory.isPostgres()){
                   updateStatement = POSTGRE_SQL_UPDATE_FIELD;
                }

                if(DbConnectionFactory.isOracle()){
                    updateStatement = ORACLE_SQL_UPDATE_FIELD;
                }

                if(DbConnectionFactory.isMsSql()){
                    updateStatement = MS_SQL_UPDATE_FIELD;
                }

                if(DbConnectionFactory.isMySql()){
                    updateStatement = MySQL_UPDATE_FIELD;
                }

                if(null == updateStatement){
                   throw new IllegalStateException("Unable to determine db type ");
                }

                dotConnect.setSQL(String.format(updateStatement, fieldInodes));
                dotConnect.loadResult();

            }finally{
                conn.setAutoCommit(false);
                conn.close();
            }

        }catch (Exception e){
            throw new DotDataException(e.getMessage(), e);
        }
    }


    private String inStatement(final List<Map<String,Object>> inodes){
        final StringBuilder stringBuilder =  new StringBuilder();
        final Iterator<Map<String,Object>> iterator =inodes.iterator();
        while (iterator.hasNext()){
            final Map<String,Object> entry = iterator.next();
            stringBuilder.append('\'').append(entry.get("field_inode").toString()).append('\'');
            if(iterator.hasNext()){
               stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

}
