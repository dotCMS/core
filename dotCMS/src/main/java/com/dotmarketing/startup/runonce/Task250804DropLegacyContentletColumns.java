package com.dotmarketing.startup.runonce;

import com.dotcms.util.content.json.PopulateContentletAsJSONUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Upgrade task that sets the system_folder identifier to SYSTEM_FOLDER and updates all folders to use the identifier as
 * the inode;
 */
public class Task250804DropLegacyContentletColumns implements StartupTask {



    String[] columnTypes = {"bool","float","integer","text_area","text","date"};


    @Override
    public boolean forceRun() {
        return true;

    }


    /**
     * Executes the upgrade task, creating the necessary tables and indexes for the Job Queue.
     *
     * @throws DotDataException    if a data access error occurs.
     * @throws DotRuntimeException if a runtime error occurs.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        DotConnect dc = new DotConnect();

        int count = dc.setSQL("select count(*) as test from contentlet where contentlet_as_json is null").getInt("test");
        if(count>0){
            Logger.warn(this.getClass(), "Your dotCMS content store needs to be updated to use JSONB - there are " +  count + " contentlets that need to be updated.  This could take awhile." );
            new PopulateContentletAsJSONUtil().populateEverything();
        }

        count = dc.setSQL("select count(*) as test from contentlet where contentlet_as_json is null").getInt("test");
        if(count>0){
            throw new DotDataException("Your dotCMS content store needs to be updated to use JSONB - there are " +  count + " contentlets that need to be updated to use JSONB" );
        }

        for(String columnType : columnTypes) {
            for(int i=0;i<26;i++) {
                Logger.info(this.getClass(), "Dropping contentlet column " + columnType + i);
                dc.setSQL("alter table contentlet drop column if exists " + columnType + i).loadResult();
            }

        }



    }


}
