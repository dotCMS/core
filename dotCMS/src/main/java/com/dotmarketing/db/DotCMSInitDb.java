package com.dotmarketing.db;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.startup.runonce.Task210304RemoveOldMetadataFiles;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ImportStarterUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.felix.framework.OSGIUtil;

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

        removeAnyOldMetadata();

        MaintenanceUtil.flushCache();
        ReindexThread.startThread();

        ContentletAPI conAPI = APILocator.getContentletAPI();
        Logger.info(DotCMSInitDb.class, "Building Initial Index");


        // Initializing felix
        OSGIUtil.getInstance().initializeFramework();

        // Reindexing the recently added content
        conAPI.refreshAllContent();
        long recordsToIndex = APILocator.getReindexQueueAPI().recordsInQueue();
        Logger.info(DotCMSInitDb.class, "Records left to index : " + recordsToIndex);

        int counter = 0;

        while (recordsToIndex > 0) {
            Thread.sleep(2000);
            recordsToIndex = APILocator.getReindexQueueAPI().recordsInQueue();
            Logger.info(DotCMSInitDb.class, "Records left to index : " + recordsToIndex);
            // ten minutes
            if(++counter>30000) {
                break;
            }
        }
		
	}

    /**
     * This needs to happen right after having imported the starter (which comes with old metadata files)
     * and  right before starting the reindex-thread.
     * Because the reindex thread regenerates now the metadata and internally the heuristic used to determine
     * if the metadata needs to be generated will skip it if it finds there's a file already in place.
     * This method waits for the execution to be completed. Because that's key.
     * The Reindex Thread must start once the old md has been removed.
     * @throws DotDataException
     */
	private static void removeAnyOldMetadata() throws DotDataException {
        final Task210304RemoveOldMetadataFiles task = new Task210304RemoveOldMetadataFiles();
        task.executeUpgrade();
        try {
            final Future<Tuple2<Integer, Integer>> future = task.getFuture();
            final Tuple2<Integer, Integer> tuple = future.get();
            Logger.info(DotCMSInitDb.class,
               String.format(" `%d` old metadata entries removed and `%d` old content files from the starter." , tuple._1 , + tuple._2)
            );
        }catch (ExecutionException | InterruptedException e){
            Logger.error(DotCMSInitDb.class,"");
        }
	}
}
