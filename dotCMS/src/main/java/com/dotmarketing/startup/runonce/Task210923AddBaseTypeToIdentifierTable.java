package com.dotmarketing.startup.runonce;

import static com.dotcms.util.ConversionUtils.toLong;
import static com.dotmarketing.util.ConfigUtils.getDeclaredDefaultLanguage;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;
import io.vavr.control.Try;

public class Task210923AddBaseTypeToIdentifierTable implements StartupTask {

    static final String CREATE_SCRIPT = "ALTER TABLE identifier ADD COLUMN basetype int;"
                    +"create index idx_identifier_asset_basetype on identifier (basetype);";
                    

    
    @Override
    public boolean forceRun() {
        
        return shouldRun();
    }
    
    
    

    public boolean shouldRun() {
        
        boolean result = Try.of(()->{
            List<Map<String,Object>> results =  new DotConnect()
                            .setSQL("select max(basetype) from identifier" )
            .loadObjectResults(DbConnectionFactory.getDataSource().getConnection());
            
            return !results.isEmpty();
        }).getOrElse(true);
        
        return result;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        if(!shouldRun()) {
            return;
        }
        

        final DotConnect dotConnect = new DotConnect();


        dotConnect.setSQL(CREATE_SCRIPT);
        dotConnect.loadResult();
        

        updateBaseContentTypeColumn(dotConnect);
    }





    private void updateBaseContentTypeColumn(final DotConnect dotConnect)
            throws DotDataException {

    }




}
