package com.dotmarketing.db;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.startup.runonce.Task210321RemoveOldMetadataFiles;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.starter.ImportStarterUtil;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.PasswordGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.framework.OSGISystem;
import org.apache.felix.framework.OSGIUtil;

public class DotCMSInitDb {

    static final String INITIAL_ADMIN_PASSWORD = "INITIAL_ADMIN_PASSWORD";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";

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

            Try.run(DotCMSInitDb::loadStarterSite).getOrElseThrow(DotRuntimeException::new);

            Try.run(DotCMSInitDb::setUpInitialPassword).onFailure(DotRuntimeException::new);

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




        // Reindexing the recently added content
        conAPI.refreshAllContent();
        long recordsToIndex = APILocator.getReindexQueueAPI().recordsInQueue();
        Logger.info(DotCMSInitDb.class, "Records left to index : " + recordsToIndex);

        int counter = 0;

        while (recordsToIndex > 0) {
            Thread.sleep(2000);
            recordsToIndex = APILocator.getReindexQueueAPI().recordsInQueue();
            Logger.info(DotCMSInitDb.class, "Records left to index : " + recordsToIndex);
            if (!ReindexThread.isWorking())
                ReindexThread.startThread();
            // ten minutes
            if(++counter>300) {
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
        final Task210321RemoveOldMetadataFiles task = new Task210321RemoveOldMetadataFiles();
        task.executeUpgrade();
        try {
            final Future<Tuple2<Integer, Integer>> future = task.getFuture();
            final Tuple2<Integer, Integer> tuple = future.get();
            Logger.info(DotCMSInitDb.class,
               String.format(" `%d` old metadata entries removed and `%d` old content files from the starter." , tuple._1 , + tuple._2)
            );
        }catch (ExecutionException | InterruptedException e){
            Logger.error(DotCMSInitDb.class,"An error occurred removing old metadata ",e);
        }
	}

    /**
     * set the initial password to admin user and prints it out
     * @throws DotDataException
     * @throws DotSecurityException
     */
    static void setUpInitialPassword() throws DotDataException, DotSecurityException {
        if(ConfigUtils.isDevMode()){
            Logger.info(DotCMSInitDb.class,"While in DevMode dotCMS will not set an initial password to the admin user.");
            return;
        }
        final UserAPI userAPI = APILocator.getUserAPI();
        final User admin = userAPI
                .loadByUserByEmail(ADMIN_DEFAULT_MAIL, APILocator.systemUser(), false);
        if (null == admin) {
            throw new DotDataException(
                    "Unable to find admin user by the email: " + ADMIN_DEFAULT_MAIL);
        }
        Logger.info(DotCMSInitDb.class, "Setting up initial password.");

        final Tuple2<Boolean,String> initialPassword = loadInitialPassword();
        if(initialPassword._1){
            //Only generated passwords are meant to be printed as notice
            printNotice(initialPassword._2);
        }

        admin.setPassword(initialPassword._2);
        userAPI.save(admin, APILocator.systemUser(), false);

    }

    /**
     * The first component returned indicates if the passwords was loaded or generated
     * The Second component it the new password it self
     * @return
     */
    static Tuple2<Boolean,String> loadInitialPassword(){
        final String preSetPassword = Config.getStringProperty(INITIAL_ADMIN_PASSWORD, null);
        if(UtilMethods.isSet(preSetPassword)){
            return Tuple.of(false, preSetPassword);
        }
        final PasswordGenerator passwordGenerator = new PasswordGenerator.Builder().withDefaultValues().build();
        return Tuple.of(true, passwordGenerator.nextPassword());
    }

    /**
     * inform user the default password set to the admin user
     * @param initialPassword the notice message
     */
    static void printNotice(final String initialPassword){
        final String notice = String
                .format("NOTICE: %s password set to %s", ADMIN_DEFAULT_MAIL, initialPassword);
        final String warning = "WARNING: Once logged-in. Please change this password.";
        final int width = 100;
        Logger.info(DotCMSInitDb.class,StringUtils.rightPad("#", width, "#"));
        Logger.info(DotCMSInitDb.class,StringUtils.center(StringUtils.center(notice, width - 4), width, "##"));
        Logger.info(DotCMSInitDb.class,StringUtils.center(StringUtils.center(warning, width - 4), width, "##"));
        Logger.info(DotCMSInitDb.class,StringUtils.rightPad("#", width, "#"));
    }
}
