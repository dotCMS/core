package com.dotmarketing.db;

import java.io.File;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ImportStarterUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

public class DotCMSInitDb {

	@CloseDBIfOpened
	private static boolean isConfigured () {

		return new DotConnect()
                .setSQL("select count(*) as test from inode")
                .getInt("test")>0;

	}

    @CloseDBIfOpened
    public static void InitializeDb() {

        if (!isConfigured()) {

            Logger.info(DotCMSInitDb.class, "There are no inodes - initializing db with starter site");

    
            
            Try.run(() -> loadStarterSite()).getOrElseThrow(e->new DotRuntimeException(e));


        } else {
            Logger.info(DotCMSInitDb.class, "inodes exist, skipping initialization of db");
        }
    }

    
    @WrapInTransaction
    private static void loadStarterSiteData() throws Exception{
        String starter = Config.getStringProperty("STARTER_DATA_LOAD", null);
        File starterZip = null;
        
        if(UtilMethods.isSet(starter)){

            // First we try using the real path
            starterZip = new File(FileUtil.getRealPath(starter));

            // Then we try to see if there is an absolute path (or relative in case of integration tests)
            if (!starterZip.exists()) {
                starterZip = new File(starter);
            }
        }
        
        if(starterZip==null || (starterZip!=null && !starterZip.exists())){
            String starterSitePath = "/starter.zip";
            String zipPath = FileUtil.getRealPath(starterSitePath);
            starterZip = new File(zipPath); 
         }
        
        ImportStarterUtil ieu = new ImportStarterUtil(starterZip);

        ieu.doImport();

    }
    @CloseDBIfOpened
	private static void loadStarterSite() throws Exception{
		
	    loadStarterSiteData() ;
	    
		DbConnectionFactory.closeAndCommit();
		

        MaintenanceUtil.flushCache();
	}
}
