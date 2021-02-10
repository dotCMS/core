package com.dotmarketing.db;

import java.io.File;

import io.vavr.CheckedRunnable;
import org.apache.felix.framework.OSGIUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
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
        initializeWith(DotCMSInitDb::loadStarterSite);
    }

    /**
     * Initializes components for specific contexts (e.g. {@link com.liferay.portal.servlet.MainServlet}
     * and test init classes like IntegrationTestInitService).
     */
    @CloseDBIfOpened
    public static void initializeIfNeeded() {
        initializeWith(DotCMSInitDb::partialInitialize);
    }

    /**
     * Initializes with a provided block of code contained in a {@link CheckedRunnable}.
     *
     * @param initRunnable initialize logic
     */
    private static void initializeWith(final CheckedRunnable initRunnable) {
        if (!isConfigured()) {
            Logger.info(DotCMSInitDb.class, "There are no inodes - initializing db with starter site");
            Try.run(initRunnable).getOrElseThrow(DotRuntimeException::new);
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
	private static void loadStarterSite() throws Exception {
	    // loads starter
	    loadStarter();

	    // rest of the components to load/init
        partialInitialize();
	}

    /**
     * Loads starter file.
     *
     * @throws Exception
     */
    private static void loadStarter() throws Exception {
	    // load starter zip file
        loadStarterSiteData();
        // save
        DbConnectionFactory.closeAndCommit();
    }

    /**
     * Initializes Felix (OSGI) framework.
     */
    private static void initOsgi() {
        // Initializing felix
        OSGIUtil.getInstance().initializeFramework();
    }

    /**
     * Starts Reindex thread.
     *
     * @throws DotDataException
     * @throws InterruptedException
     */
    private static void startReindex() throws DotDataException, InterruptedException {
        MaintenanceUtil.flushCache();
        ReindexThread.startThread();

        ContentletAPI conAPI = APILocator.getContentletAPI();
        Logger.info(DotCMSInitDb.class, "Building Initial Index");

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
     * Partially initializes components for specific contexts (e.g. {@link com.liferay.portal.servlet.MainServlet}
     * and test init classes like IntegrationTestInitService).
     */
    private static void partialInitialize() throws InterruptedException, DotDataException {
        // Init OSGI
        initOsgi();

        // Reindex
        startReindex();
    }
}
