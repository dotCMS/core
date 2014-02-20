package com.dotmarketing.portlets.cmsmaintenance.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Created by Jonathan Gamba
 * Date: 6/11/12
 * Time: 10:57 AM
 */
public class CleanAssetsThread extends Thread {
    public static class BasicProcessStatus {
        private int totalFiles=0;
        private int currentFiles=0;
        private int deleted=0;
        private boolean running=false;
        private String status="";
        
        public int getDeleted() {
            return deleted;
        }
        public void setDeleted(int deleted) {
            this.deleted = deleted;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public boolean isRunning() {
            return running;
        }
        public void setRunning(boolean running) {
            this.running = running;
        }
        public int getTotalFiles() {
            return totalFiles;
        }
        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }
        public int getCurrentFiles() {
            return currentFiles;
        }
        public void setCurrentFiles(int currentFiles) {
            this.currentFiles = currentFiles;
        }
        public Map buildStatusMap()  {
            try {
                return BeanUtils.describe(this);
            } catch (Exception e) {
                return new HashMap();
            }
        }
    }

    private static CleanAssetsThread instance;
    private static BasicProcessStatus processStatus;

    /**
     * Return the current instance of this thread, this class is create it as a singleton a we need to recover this thread to track the process it is running
     *
     * @param restartIfDied creates or not a new instance depending of the current thread is alive or not
     * @return
     */
    public static CleanAssetsThread getInstance ( Boolean restartIfDied, boolean processFiles, boolean processBinary ) {

        if ( instance == null ) {

            instance = new CleanAssetsThread(processFiles,processBinary);
            processStatus = new BasicProcessStatus();

        } else if ( !instance.isAlive() && restartIfDied ) {

            instance = new CleanAssetsThread(processFiles,processBinary);
            processStatus = new BasicProcessStatus();
        }

        return instance;
    }

    private boolean processFiles, processBinary;
    private CleanAssetsThread (boolean processFiles, boolean processBinary) {
        this.processFiles=processFiles;
        this.processBinary=processBinary;
    }

    @Override
    public void run () {
        if(!processStatus.isRunning()) {
            try {
                processStatus.setStatus("Starting");
                processStatus.setRunning(true);
                deleteAssetsWithNoInode();
            } catch ( DotDataException e ) {
                processStatus.setStatus( "Error: "+e.getMessage() );
                processStatus.setRunning(false);
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    /**
     * Delete all files who are no longer in the FileAsset Table (this is the 1.9 table) and the Contentlet Table where the structure type is a type of File Asset
     *
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     */
    @SuppressWarnings ("unchecked")
    void deleteAssetsWithNoInode () throws DotDataException {

        //Assest folder path
        String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
        File assetsRootFolder = new File( assetsPath );
        
        processStatus.setStatus("Counting");
        int total=0;
        char[] hexChr = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        for(char x : hexChr) {
            for(char y : hexChr) {
                File dir=new File(assetsPath+File.separator+x+File.separator+y);
                if(dir.isDirectory())
                    total += dir.list().length;
                processStatus.setTotalFiles(total);
            }
        }
        
        processStatus.setStatus("Cleaning");
        int deleted=0, current=0;
        User systemUser = APILocator.getUserAPI().getSystemUser();
        for(char x : hexChr) {
            for(char y : hexChr) {
                File dir=new File(assetsPath+File.separator+x+File.separator+y);
                if(dir.isDirectory()) {
                    for(File ff : dir.listFiles()) {
                        processStatus.setCurrentFiles(++current);
                        if(ff.isDirectory()) {
                            // binary file for a contentlet
                            if(processBinary) {
                                String inode=ff.getName();
                                try {
                                    Contentlet cont=APILocator.getContentletAPI().find(inode, systemUser,false);
                                    if(cont==null || !UtilMethods.isSet(cont.getIdentifier())) {
                                        Logger.info(this, "deleting orphan binary content "+ff.getAbsolutePath());
                                        if(FileUtils.deleteQuietly(ff))
                                            processStatus.setDeleted(++deleted);
                                    }
                                }
                                catch(Exception ex) {
                                    Logger.warn(this, ex.getMessage(), ex);
                                }
                            }
                        }
                        else {
                            // is a file_asset
                            if(processFiles) {
                                String inode=ff.getName();
                                // maybe a thumbnail
                                if(inode.indexOf('_')!=-1)
                                    inode=inode.substring(0,inode.indexOf('_'));
                                // remove extension if any
                                if(inode.indexOf('.')!=-1)
                                    inode=inode.substring(0,inode.indexOf('.'));
                                com.dotmarketing.portlets.files.model.File file = null;
                                try {
                                    file = APILocator.getFileAPI().find(inode, systemUser, false);
                                }
                                catch(Exception ex) {
                                    Logger.warn(this, ex.getMessage(), ex);
                                }
                                if(file==null || !UtilMethods.isSet(file.getIdentifier())) {
                                    Logger.info(this, "deleting orphan file_asset "+ff.getAbsolutePath());
                                    if(FileUtils.deleteQuietly(ff))
                                        processStatus.setDeleted(++deleted);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        processStatus.setRunning(false);
        processStatus.setStatus("Finished");
    }

    public BasicProcessStatus getProcessStatus () {
        return processStatus;
    }

}
