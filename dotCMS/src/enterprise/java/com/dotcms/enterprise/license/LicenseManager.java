/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.ClusterUtils;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.bouncycastle.crypto.CryptoException;
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.AESEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.util.encoders.Base64;
import com.dotcms.enterprise.license.bouncycastle.util.encoders.Hex;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides access to licensing information for a dotCMS instance. This allows the system to perform
 * validations and limit or hide enterprise-level application features.
 * 
 * @author root
 * @since 1.x
 */
public final class LicenseManager {

    public DotLicense license = new DotLicense();
    final String serverId;


    /**
     * Protected class constructor.
     */

    private LicenseManager() {
        serverId = init();
    }

    @WrapInTransaction
    private String init() {
        String serverId;
        serverId = APILocator.getServerAPI().readServerId();
        this.license = readLicenseFile();
        licenseMessage();
        return serverId;
    }


    public void takeLicenseFromRepoIfNeeded() throws Exception {
        takeLicenseFromRepoIfNeeded(true);
    }

    /**
     * 
     * @throws Exception
     */
    public void takeLicenseFromRepoIfNeeded(final boolean addToCluster) throws Exception {
        final String currSerial = license.serial;
        final int currentLevel = license.level;
        if (license.level > LicenseLevel.COMMUNITY.level) {
            checkServerDuplicity();
            return;
        }

        final Optional<DotLicenseRepoEntry> newLicense = LicenseRepoDAO.requestLicense();
        if (newLicense.isPresent()) {
            writeLicenseFile(newLicense.get().license.getBytes());
            if (currSerial.equals(this.license.serial) || currentLevel < this.license.level) {
                // when manual pick of license fire the cluster check
                if (addToCluster) {
                    onLicenseApplied();
                }
            }
        }
        licenseMessage();
        checkServerDuplicity();
    }

    /**
     * Applies the specified license serial to the current dotCMS instance.
     * 
     * @param serial - The serial number to apply.
     * @throws Exception An error occurred when applying the license.
     */
    public void forceLicenseFromRepo(String serial) throws Exception {
        
        String currSerial = license.serial;
        int currentLevel=license.level;
        
        
        Optional<DotLicenseRepoEntry> newLicense = LicenseRepoDAO.forceLicenseFromRepo(serial);
        if (newLicense.isPresent()) {

            writeLicenseFile(newLicense.get().license.getBytes());

            if (currSerial.equals(this.license.serial) || currentLevel<this.license.level) {
                // when manual pick of license fire the cluster check
                onLicenseApplied();
            }
        }
    }

    /**
     * Reads the license information from the dotCMS license file in order to check its validity and
     * expiration days.
     */
    private DotLicense readLicenseFile() {
        final File licenseFile = new File(getLicensePath());
        try (final InputStream is = Files.newInputStream(licenseFile.toPath())) {
            final String licenseRaw = IOUtils.toString(is, StandardCharsets.UTF_8);
            final DotLicense dl = new LicenseTransformer(licenseRaw).dotLicense;
            try {
                LicenseRepoDAO.upsertLicenseToRepo( dl.serial, licenseRaw);
            } catch (final Exception e) {
                Logger.warnEveryAndDebug(this.getClass(), "Cannot upsert License to db", e,120000);
            }
            return dl;
        } catch (final Throwable e) {
            // Eat Me
            Logger.debug(System.class, String.format("No valid license was found: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
        }
        return setupDefaultLicense(false);
    }



    /**
     * Sets a default license for the current dotCMS instance in case no license is found. Any
     * existing license data will be removed by default.
     */
    public DotLicense setupDefaultLicense() {
        return setupDefaultLicense(true);
    }

    /**
     * Sets a default license for the current dotCMS instance in case no license is found.
     * 
     * @param cleanup - Set to {@code true} if any existing license data must be removed before
     *        setting up the license. Otherwise, set to {@code false}.
     */
    private DotLicense setupDefaultLicense(boolean cleanup) {
        Logger.info(LicenseUtil.class, "Setting up default license");

        if (cleanup) {
            File f = new File(getLicensePath());
            f.delete();
            
        }

        Logger.debug(LicenseUtil.class, "Was existing license cleaned up? " + cleanup
                        + " and stack trace is " + ExceptionUtils.getStackTrace(new Throwable()));

        new Thread(new Runnable() {
            public void run() {
                try {
                    LocalTransaction.wrap(() -> {
                        ClusterFactory.removeNodeFromCluster();

                    });
                } catch (Exception ex) {
                    Logger.error(ClusterFactory.class, "can't remove from cluster", ex);
                }

            }
        }).start();
        this.license=new DotLicense();
        return this.license;
    }

    /**
     * Returns the name of the client that the current license belongs to.
     * 
     * @return The client's name.
     */
    public String getClientName() {
        return license.clientName;
    }

    /**
     * Returns the serial number associated to this license.
     * 
     * @return The serial number.
     */
    public String getSerial() {
        return license.serial;
    }

    static final AtomicBoolean LICENSE_INITED = new AtomicBoolean(false);
    /**
     * Returns the official dotCMS license name based on the specified license level.
     * 
     * @param level - The license level number.
     * @return The official license name.
     */
    public String getLevelName(int level) {
        
        return LicenseLevel.fromInt(level).name;
    }

    /**
     * Returns a singleton instance of this class.
     * 
     * @return A unique instance of {@link LicenseManager}.
     */
    public static LicenseManager getInstance() {
        return LicenceManagerHolder.INSTANCE;
    }

    public static void reloadInstance() {
        getInstance().readLicenseFile();
    }

    /**
     * Checks the validity of the current license.
     * 
     * @return Returns {@code true} if the license is still valid. If expired, returns
     *         {@code false}.
     */
    private boolean checkValidity() {
        // license expired
        return license.perpetual || !new Date().after(license.validUntil);
    }

    /**
     * Checks if the license level of the current license belongs to one of the levels in the
     * specified array.
     * 
     * @param levels - The array with license levels.
     * @return Returns {@code true} if the current license is part of the array. Otherwise, returns
     *         {@code false}.
     */
    public boolean isAuthorized(int[] levels) {
        if (levels == null) {
            return false;
        }
        if (!checkValidity()) {
            // license expired
            return false;
        }
        for (int level : levels) {
            if (level <= license.level) {
                return true;
            }
        }
        return false;
    }

    // /UTILITY METHODS

    /**
     * Determines what application server can run dotCMS based on the current license. Some servers
     * require an enterprise license in order to run the application.
     * 
     * @param level - The current license level.
     * @return Returns {@code true} if the current AS can run dotCMS. Otherwise, returns
     *         {@code false}.
     */
    private boolean isASEnabled(int level) {
        if (ServerDetector.isTomcat()) {
            // Tomcat always works
            return true;
        }
        if (level >= LicenseLevel.PRIME.level) {
            // Prime, can run anything
            return true;
        }
        // Glassfish and JBoss only in professional
        return (ServerDetector.isGlassfish() || ServerDetector.isJBoss())
                && level >= LicenseLevel.PROFESSIONAL.level;
    }

    /**
     * Verifies if dotCMS can run in the current application server.
     * 
     * @return Returns {@code true} if dotCMS is able to start up. Otherwise, returns {@code false}.
     */
    public boolean isASEnabled() {
        return isASEnabled(license.level); 
    }



    /**
     * Returns the last day this license will be valid.
     * 
     * @return The valid-until date.
     */
    public Date getValidUntil() {
        return license.validUntil;
    }

    /**
     * Returns the current license level.
     * 
     * @return The license level.
     */
    public int getLevel() {
        return license.level;
    }

    /**
     * Writes information to the license data file.
     * 
     * @param data - The information to write.
     * @throws IOException An error occurred when writing the data.
     */
    private void writeLicenseFile(byte[] data) throws IOException {
        
        DotLicense newOne = new LicenseTransformer(data).dotLicense;
        if(newOne.expired){
            return;
        }
        //make sure we hava a license
        this.license = newOne;
        
        
        File licenseFile = new File(getLicensePath());
        if (!licenseFile.getParentFile().exists()) {
            licenseFile.getParentFile().mkdirs();
        }
        try(OutputStream os = Files.newOutputStream(licenseFile.toPath())){
            os.write(data);
        }

    }

    /**
     * Uploads a license file to the current instance.
     * 
     * @param data - The license data.
     * @throws IOException An error occurred when writing the license data.
     * @throws DotDataException 
     */
    public void uploadLicense(String data) throws IOException, DotDataException {
        
        writeLicenseFile(data.getBytes());
        DotLicense dl = new LicenseTransformer(data).dotLicense;
        LicenseRepoDAO.upsertLicenseToRepo(dl.serial,data);
        LicenseRepoDAO.setServerIdToCurrentLicense(this.license.serial);
        onLicenseApplied();
    }

    /**
     * Performs a cluster check every time a license is applied to a dotCMS instance.
     */
    private void onLicenseApplied() {
        ChainableCacheAdministratorImpl cacheAdm = (ChainableCacheAdministratorImpl) CacheLocator
                        .getCacheAdministrator().getImplementationObject();

        // check if clustering is enabled but didn't start when there wasn't any license
        if (license.level > LicenseLevel.COMMUNITY.level && ClusterUtils.isAutoScaleConfigured()
                        && !cacheAdm.getTransport().isInitialized()) {

           ClusterFactory.initialize();
        }
    }

    /**
     * Returns the location of the license data file in the file system.
     * 
     * @return The location of the license file.
     */
    private static String getLicensePath() {
        return ConfigUtils.getDynamicContentPath() 
                        + File.separator 
                        + "license"  
                        + File.separator 
                        + "license.dat";
    }

    /**
     * 
     * @param data
     * @param key
     * @param encrypt
     * @return
     * @throws CryptoException
     * @throws IllegalStateException
     */
    private byte[] processAES(byte[] data, byte[] key, boolean encrypt)
                    throws CryptoException, IllegalStateException {
        PaddedBufferedBlockCipher cypher =
                        new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        byte[] out = new byte[cypher.getOutputSize(data.length) + 32];
        cypher.init(encrypt, new KeyParameter(key));
        int count = cypher.processBytes(data, 0, data.length, out, 0);
        count += cypher.doFinal(out, count);
        byte[] result = new byte[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private String[] getLicData() {
        return LicenseTransformer.publicDatFile;
    }


    /**
     * 
     * @return
     */
    public String getDisplayServerId() {


        return getDisplayServerId(this.serverId) ;

    }

    /**
     * 
     * @param serverId
     * @return
     */
    public String getDisplayServerId(String serverId) {
        if(serverId==null) return "";
        return serverId.split("-")[0];
    }

    /**
     * Returns a piece of the current server's serial number. The complete number is not displayed
     * because of security reasons.
     * 
     * @return The safe version of the serial number.
     */
    public String getDisplaySerial() {
        return getDisplaySerial(getSerial());
    }

    /**
     * Returns a piece of the specified serial number. The complete number is not displayed because
     * of security reasons.
     * 
     * @param serial - The serial number.
     * @return The safe version of the serial number.
     */
    public String getDisplaySerial(String serial) {
        if(serial==null) return "";
        return serial.split("-")[0];
    }


    /**
     * 
     * @return
     * @throws IOException
     */
    private byte[] getRequestCodeAESKey() {
        return Hex.decode(getLicData()[3]);
    }

    /**
     *

     * @return
     * @throws Exception
     */
    public String createTrialLicenseRequestCode()  throws Exception {
    	
    	final int level = LicenseLevel.PLATFORM.level;
    	final int version = 400;
    	final LicenseType type = LicenseType.TRIAL;
        return new String(Base64.encode(processAES(
                        ("level=" + level + ",version=" + version + ",serverid=" + serverId
                                        + ",licensetype=" + type.type + ",serverid_display="
                                        + getDisplayServerId()).getBytes(),
                        getRequestCodeAESKey(), true)), StandardCharsets.UTF_8);
    }

    /**
     * Returns the type of the current license.
     * 
     * @return The license type.
     */
    public String getLicenseType() {
        return license.licenseType;
    }

    /**
     * Checks if the current license is perpetual.
     * 
     * @return Returns {@code true} if the license is perpetual. Otherwise, returns {@code false}.
     */
    public boolean isPerpetual() {
        return license.perpetual;
    }

    /**
     * Returns the total number of licenses in the current instance.
     * 
     * @return The total number of licenses.
     * @throws DotDataException An error occurred when retrieving the data from the database.
     */
    public int getLicenseRepoTotal() throws DotDataException {
        return LicenseRepoDAO.getLicenseRepoTotal();
    }

    /**
     * Returns the number of site licenses that are available in the current instance.
     * 
     * @return The number of available licenses.
     * @throws DotDataException An error occurred when retrieving the data from the database.
     */
    public int getLicenseRepoAvailableCount() throws DotDataException {
        return LicenseRepoDAO.getLicenseRepoAvailableCount();
    }

    /**
     * 
     * @throws DotDataException
     */
    public void updateLicenseHeartbeat() throws DotDataException {
        LicenseRepoDAO.updateLastPing();
    }

    /**
     * Uploads dotCMS licenses via license pack to the current server.
     * 
     * @param input - The zip file's data.
     * @throws IOException An error occurred when reading the zip's data.
     * @throws DotDataException The licenses could not be added to the server.
     */
    @WrapInTransaction
    public void insertAvailableLicensesFromZipFile(InputStream input)
                    throws IOException, DotDataException {
        LicenseRepoDAO.insertAvailableLicensesFromZipFile(input);
        try{
        	takeLicenseFromRepoIfNeeded();
        }
        catch(Exception e){
        	throw new DotStateException(e);
        }
    }

    /**
     * Returns the site licenses that are available in the current dotCMS instance.
     * 
     * @return The available site licenses.
     * @throws DotDataException An error occurred when retrieving the licenses.
     * @throws IOException An error occurred when validating the license file.
     */
    public List<DotLicenseRepoEntry> getLicenseRepoList() throws DotDataException, IOException {
        return LicenseRepoDAO.getLicenseRepoList();
    }

    /**
     * Removes the specified license from the current server.
     * 
     * @param id - The license ID.
     * @throws DotDataException An error occurred when removing the data.
     */
    public void deleteLicense(String id) throws DotDataException {
        LicenseRepoDAO.deleteLicense(id);
    }

    /**
     * Removes the current license from the server. The default license level is Community.
     * 
     * @throws DotDataException An error occurred when setting up the default license.
     */
    public void freeLicenseOnRepo() throws DotDataException {
        LicenseRepoDAO.freeLicense();
        this. license = setupDefaultLicense();
        
    }

    /**
     * Removes the specified license serial number from a specific server instance.
     * 
     * @param serial - The license serial number.
     * @throws DotDataException An error occurred when resetting the server's license.
     */
    public void freeLicenseOnRepo(String serial, String server_id) throws DotDataException {
        LicenseRepoDAO.freeLicense(serial);

    }

    /**
     * Checks if the current dotCMS instance is running with a Community License.
     * 
     * @return Returns {@code true} if the license level is Community. Otherwise, returns
     *         {@code false}.
     */
    public boolean isCommunity() {
      return !isEnterprise();

    }

    /**
     * Checks if the current dotCMS instance is running with an Enterprise License.
     * 
     * @return Returns {@code true} if the license level is Enterprise. Otherwise, returns
     *         {@code false}.
     */
    public boolean isEnterprise() {
        licenseMessage();
        return (this.license.level > LicenseLevel.COMMUNITY.level ) ;

    }

    /**
     * Checks if the current dotCMS instance is running with a Platform License.
     * 
     * @return Returns {@code true} if the license level is Platform. Otherwise, returns
     *         {@code false}.
     */
    public boolean isPlatform() {
      return (this.license.level == LicenseLevel.PLATFORM.level) ;
    }

    
    private void licenseMessage() {
        if (license.level ==  LicenseLevel.COMMUNITY.level || LICENSE_INITED.get()) {
            return;
        }
        
        if(LICENSE_INITED.compareAndSet(false, true)) {
            Logger.info(InitServlet.class,
                            "");
            Logger.info(InitServlet.class,
                            " * Copyright (c) 2023 dotCMS Inc. ");
            Logger.info(InitServlet.class,
                            " * This software, code and any modifications to the code are licensed under the terms of the dotCMS Enterprise License ");
            Logger.info(InitServlet.class,
                            " * which can be found here : https://www.github.com/dotCMS/core/LICENSE ");
            Logger.info(InitServlet.class,
                            "");
        }
        
        
    }
    
    
    
    /**
     * Check for server duplication and if so log an error
     */
    public boolean checkServerDuplicity() {
        if (license.level ==  LicenseLevel.COMMUNITY.level) {
            // This means there is no license set yet (community license), nothing to check
            return false;
        }

        
        final String serverId = APILocator.getServerAPI().readServerId();
        try {
            final boolean serverDuplicated = LicenseRepoDAO.isServerDuplicated(
                    serverId,
                    license.raw,
                    APILocator.getServerAPI().getServerStartTime());
            if (serverDuplicated) {
                Logger.error(
                        this,
                        String.format(
                                "DETECTED more than one server with same id %s or license, this can cause inconsistent behavior",
                                serverId));
            }

            return serverDuplicated;
        } catch (DotDataException e) {
            Logger.error(this, String.format("Could not detect if server %s is duplicated", serverId), e);
            return false;
        }
    }

    /**
     * Meant to be executed at start up time to update the server startup time.
     */
    public void updateServerStartTime() {
        if (license.level ==  LicenseLevel.COMMUNITY.level) {
            // This means there is no license set yet (community license), nothing to update
            return;
        }

        final String serverId = APILocator.getServerAPI().readServerId();
        final long startTime = APILocator.getServerAPI().getServerStartTime();
        Logger.info(this, String.format("Updating server %s start time to %s", serverId, startTime));

        try {
            LicenseRepoDAO.updateLicenseStartTime(serverId, license.raw, startTime);
        } catch (DotDataException e) {
            Logger.error(
                    LicenseManager.class,
                    String.format("Could not update startup time for server %s", serverId),
                    e);
        }
    }

    private static class LicenceManagerHolder {
        static final LicenseManager INSTANCE = new LicenseManager();

    }

}
