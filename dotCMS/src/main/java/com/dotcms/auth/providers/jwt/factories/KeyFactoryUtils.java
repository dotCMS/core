package com.dotcms.auth.providers.jwt.factories;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;

/**
 * @author Jonathan Gamba 5/30/18
 */
public class KeyFactoryUtils {

    private static final String SECRET_FILE_NAME = "jwt_secret.dat";
    private final FileAssetAPI fileAssetAPI;
    private static KeyFactoryUtils instance;

    public static KeyFactoryUtils getInstance() {
        if (instance == null) {
            synchronized (KeyFactoryUtils.class) {
                if (instance == null) {
                    instance = new KeyFactoryUtils();
                }
            }
        }
        return instance;
    }

    @VisibleForTesting
    public static KeyFactoryUtils getInstance(final FileAssetAPI fileAssetAPI) {
        if (instance == null) {
            synchronized (KeyFactoryUtils.class) {
                if (instance == null) {
                    instance = new KeyFactoryUtils(fileAssetAPI);
                }
            }
        }
        return instance;
    }

    private KeyFactoryUtils() {
        this(APILocator.getFileAssetAPI());
    }

    private KeyFactoryUtils(final FileAssetAPI fileAssetAPI) {
        this.fileAssetAPI = fileAssetAPI;
    }

    /**
     * Writes a given secret string to a secret file inside the assets folder
     */
    public void writeSecretToDisk(final String secret) {

        final File secretFile = getSecretFile();

        if (secretFile.exists()) {
            try {
                //Clean up the existing file in order to override it
                secretFile.delete();
            } catch (Exception e) {
                Logger.error(this.getClass(),
                        String.format("Unable to delete existing JWT secret file [%s] [%s]",
                                secretFile.getAbsolutePath(), e.getMessage()), e);
            }
        }

        //Verify the folder where the file lives exist
        final File serverDir = getSecretFolder();
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }

        try {
            secretFile.createNewFile();
        } catch (IOException e) {
            Logger.error(this.getClass(),
                    String.format("Unable to create JWT secret file [%s] [%s]",
                            secretFile.getAbsolutePath(), e.getMessage()), e);
        }

        try {
            //Write the info into the file
            FileUtils.writeStringToFile(secretFile, secret);
        } catch (IOException e) {
            Logger.error(this.getClass(),
                    String.format("Unable to write JWT secret file [%s] [%s]",
                            secretFile.getAbsolutePath(), e.getMessage()), e);
        }
    }

    /**
     * Reads the secret found into the secret file in the assets folder
     */
    public String readSecretFromDisk() {

        if (existSecretFile()) {

            File secretFile = getSecretFile();
            try {
                return FileUtils.readFileToString(secretFile);
            } catch (IOException e) {
                Logger.error(this.getClass(),
                        String.format("Unable to read JWT secret file [%s] [%s]",
                                secretFile.getAbsolutePath(), e.getMessage()), e);
            }
        }

        return null;
    }

    /**
     * True if the secret file exist in the assets folder
     */
    public boolean existSecretFile() {
        return getSecretFile().exists();
    }

    /**
     * Returns the folder path where the secret file lives
     */
    private String getSecretFolderPath() {
        return this.fileAssetAPI.getRealAssetsRootPath()
                + java.io.File.separator
                + "server";
    }

    /**
     * Returns the folder file where the secret file lives
     */
    public File getSecretFolder() {
        return new File(getSecretFolderPath());
    }

    /**
     * Returns the secret file path
     */
    private String getSecretFilePath() {
        return getSecretFolderPath()
                + java.io.File.separator
                + SECRET_FILE_NAME;
    }

    /**
     * Returns the secret file
     */
    public File getSecretFile() {
        return new File(getSecretFilePath());
    }

}