package com.dotmarketing.startup.runalways;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class Task00020LoadClusterLicenses implements StartupTask {



    private static final String now= new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    private FileFilter licensePacks = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return (pathname.getName().startsWith("license") 
                            && pathname.getName().endsWith(".zip")
                            && pathname.isFile());
        }
    };
    
    @Override
    public boolean forceRun() {
        
       return licensePackFiles().length>0;

    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        
        for(File pack : licensePackFiles()){
            Logger.info(this.getClass(), "found license pack: " + pack);
            File oldPack = new File(pack.getParent() + File.separator + "imported_" + now  + "_" + pack.getName());
            pack.renameTo(oldPack);
            try(InputStream in = Files.newInputStream(oldPack.toPath())){
                LicenseUtil.uploadLicenseRepoFile(in);
                if(Config.getBooleanProperty("ARCHIVE_IMPORTED_LICENSE_PACKS", true)){
                    pack.renameTo(oldPack);
                }
            } catch (IOException e) {
                Logger.info(this.getClass(), "Unable to import licenses: " + e.getMessage());
            }
        
        }
        
        
    }
    
    private File[] licensePackFiles(){
        
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()).listFiles(licensePacks);
    }

    
    

    
    
    
    
}
