package com.dotmarketing.startup.runalways;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.license.LicenseManager;
import io.vavr.control.Try;
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
import java.util.Optional;

import static com.dotcms.enterprise.LicenseUtil.IMPORTED_LICENSE_PACK_PREFIX;
import static com.dotcms.enterprise.LicenseUtil.LICENSE_NAME;

public class Task00002LoadClusterLicenses implements StartupTask {

    private static final String now= new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    private FileFilter licensePacks = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return (pathname.getName().startsWith(LICENSE_NAME)
                            && pathname.getName().endsWith(".zip")
                            && pathname.isFile());
        }
    };
    
    @Override
    public boolean forceRun() {
        
       return true;

    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        loadLicensePackFiles();
        getEnvLicenses();
    }

    public void loadLicensePackFiles() {
        File[] licensePackFiles=new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()).listFiles(licensePacks);



        for(File pack : licensePackFiles()){
            Logger.info(this.getClass(), "found license pack: " + pack);
            File oldPack = new File(pack.getParent() + File.separator + IMPORTED_LICENSE_PACK_PREFIX + now  + "_" + pack.getName());
            try(InputStream in = Files.newInputStream(pack.toPath())){
                LicenseUtil.uploadLicenseRepoFile(in);
                if(Config.getBooleanProperty("ARCHIVE_IMPORTED_LICENSE_PACKS", false)){
                    boolean status = pack.renameTo(oldPack);
                    if(status == false)
                        Logger.warn(this.getClass(), "Unable to rename license file - consider setting ARCHIVE_IMPORTED_LICENSE_PACKS to false if you do not want license file renamed after it is loaded.");
                }
            } catch (IOException | DotDataException e) {
                Logger.warn(this.getClass(), "Unable to import licenses: " + e.getMessage(), e);
            }        
        }





    }
    
    private File[] licensePackFiles(){
        
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()).listFiles(licensePacks);
    }


    private void getEnvLicenses()  {


        final String userHome = System.getProperty("user.home");

        // load single license file
        final File license = new File(userHome + "/.dotcms/license/license.dat");
        if(license.exists()) {
            Try.run(()->LicenseManager.getInstance().uploadLicense( Files.readString(license.toPath()) )).onFailure(e->Logger.error(this.getClass(), "Error uploading license", e));
        }

        //load license zip file
        File licenseZip = new File(userHome + "/.dotcms/license/license.zip");
        if(license.exists()) {
            Try.run(()-> LicenseUtil.uploadLicenseRepoFile(Files.newInputStream(licenseZip.toPath()))).onFailure(e->Logger.error(this.getClass(), "Error uploading license", e));
        }

        // load license from environment variable
        if(System.getenv("DOTCMS_LICENSE")!=null) {
            Try.run(()->    LicenseManager.getInstance().uploadLicense( System.getenv("DOTCMS_LICENSE") )).onFailure(e->Logger.error(this.getClass(), "Error uploading license", e));
        }


    }


    
}
