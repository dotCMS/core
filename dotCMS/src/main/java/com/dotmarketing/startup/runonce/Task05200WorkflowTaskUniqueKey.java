package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Task05200WorkflowTaskUniqueKey implements StartupTask {

   private final static String TABLE_NAME = "workflow_task";

   private final static String CONSTRAINT_NAME = "unique_workflow_task";

   private final static String ADD_CONSTRAINT_SQL = "ALTER TABLE workflow_task ADD CONSTRAINT unique_workflow_task UNIQUE (webasset,language_id)";

    @Override
    public boolean forceRun() {
        try {
           return new DotDatabaseMetaData().getConstraints(TABLE_NAME).stream().map(String::toLowerCase).noneMatch(s -> s.equals(CONSTRAINT_NAME));
        } catch (DotDataException e) {
           throw  new DotRuntimeException(e);
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this,
                String.format("Upgrading workflow_task table adding `%s` constraint", CONSTRAINT_NAME));
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final DotConnect dotConnect = new DotConnect();
        
        List<Map<String,Object>> results = dotConnect.setSQL("select webasset,language_id from workflow_task group by webasset,language_id having count(*)>1").loadObjectResults();
        
        for(final Map<String,Object> map : results) {
            final String webAsset = (String) map.get("webasset");
            final long lang = Try.of(()->(Long) map.get("language_id")).getOrElse(0l);
            final List<String> deleteMes = dotConnect
            .setSQL("select id from workflow_task where webasset=? and language_id=? order by mod_date desc")
            .addParam(webAsset)
            .addParam(lang)
            .setStartRow(1)
            .loadObjectResults()
            .stream()
            .map(r->(String)r.get("id")).collect(Collectors.toList());
            
            String subquery = "'" + String.join("','" , deleteMes) + "'";
            
            Logger.warn(this, "Found multiple workflow tasks for contentlet id:" + webAsset + " language: " + lang + ". Keeping the most recent workflow task");
            
            new DotConnect().setSQL("delete from workflow_comment where workflowtask_id in (" + subquery + ")" ).loadResult();

            new DotConnect().setSQL("delete from workflow_history where workflowtask_id  in (" + subquery + ")" ).loadResult();
        
            new DotConnect().setSQL("delete from workflowtask_files where workflowtask_id in (" + subquery + ")" ).loadResult();
        
            new DotConnect().setSQL("delete from workflow_task where id in (" + subquery + ")" ).loadResult();


        }
        
        
        try {
            dotConnect.executeStatement(ADD_CONSTRAINT_SQL);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
