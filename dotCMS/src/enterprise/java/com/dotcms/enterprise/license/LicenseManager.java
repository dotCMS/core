/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides access to licensing information for a dotCMS instance. This allows the system to perform
 * validations and limit or hide enterprise-level application features.
 * 
 * @author root
 * @since 1.x
 */
public final class LicenseManager {

    public final Lazy<DotLicense> myLicense = Lazy.of(()-> new DotLicense());



    /**
     * Protected class constructor.
     */

    private LicenseManager() {
        init();
    }

    @WrapInTransaction
    private void init() {

        insertDefaultLicense();
        licenseMessage();

    }


    public void takeLicenseFromRepoIfNeeded() throws Exception {
        takeLicenseFromRepoIfNeeded(true);
    }

    /**
     * 
     * @throws Exception
     */
    public void takeLicenseFromRepoIfNeeded(final boolean addToCluster) throws Exception {

        licenseMessage();

    }

    /**
     * Applies the specified license serial to the current dotCMS instance.
     * 
     * @param serial - The serial number to apply.
     * @throws Exception An error occurred when applying the license.
     */
    public void forceLicenseFromRepo(String serial) throws Exception {
        
            init();
            onLicenseApplied();


    }

    /**
     * Reads the license information from the dotCMS license file in order to check its validity and
     * expiration days.
     */
    private void insertDefaultLicense() {

        try {

            LicenseRepoDAO.insertDefaultLicense();
        }catch (final Throwable e) {
            Logger.error(this, "Error inserting default license", e);
            throw new DotRuntimeException("Error inserting default license", e);
        }

    }



    /**
     * Returns the name of the client that the current license belongs to.
     * 
     * @return The client's name.
     */
    public String getClientName() {
        return myLicense.get().clientName;
    }

    /**
     * Returns the serial number associated to this license.
     * 
     * @return The serial number.
     */
    public String getSerial() {
        return myLicense.get().serial;
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
        getInstance().init();
    }

    /**
     * Checks the validity of the current license.
     * 
     * @return Returns {@code true} if the license is still valid. If expired, returns
     *         {@code false}.
     */
    private boolean checkValidity() {
        return true;
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
        return true;
    }

    // /UTILITY METHODS



    /**
     * Verifies if dotCMS can run in the current application server.
     * 
     * @return Returns {@code true} if dotCMS is able to start up. Otherwise, returns {@code false}.
     */
    public boolean isASEnabled() {
        return true;
    }



    /**
     * Returns the last day this license will be valid.
     * 
     * @return The valid-until date.
     */
    public Date getValidUntil() {
        return myLicense.get().validUntil;
    }

    /**
     * Returns the current license level.
     * 
     * @return The license level.
     */
    public int getLevel() {
        return myLicense.get().level;
    }



    /**
     * Uploads a license file to the current instance.
     * 
     * @param data - The license data.
     * @throws IOException An error occurred when writing the license data.
     * @throws DotDataException 
     */
    public void uploadLicense(String data) throws IOException, DotDataException {
        

    }

    /**
     * Performs a cluster check every time a license is applied to a dotCMS instance.
     */
    private void onLicenseApplied() {
        ChainableCacheAdministratorImpl cacheAdm = (ChainableCacheAdministratorImpl) CacheLocator
                        .getCacheAdministrator().getImplementationObject();


           ClusterFactory.initialize();

    }







    /**
     * 
     * @return
     */
    public String getDisplayServerId() {


        return getDisplayServerId(APILocator.getServerAPI().readServerId()) ;

    }

    /**
     * 
     * @param serverId
     * @return
     */
    public String getDisplayServerId(String serverId) {
        if(serverId==null) return "unknown";
        if(serverId.indexOf("-")>0){
            return APILocator.getShortyAPI().shortify(serverId.split("-")[0]);
        }
        return APILocator.getShortyAPI().shortify(serverId);
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
     * Returns the type of the current license.
     * 
     * @return The license type.
     */
    public String getLicenseType() {
        return myLicense.get().licenseType;
    }

    /**
     * Checks if the current license is perpetual.
     * 
     * @return Returns {@code true} if the license is perpetual. Otherwise, returns {@code false}.
     */
    public boolean isPerpetual() {
        return myLicense.get().perpetual;
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
        //LicenseRepoDAO.freeLicense();
        //this. license = setupDefaultLicense();
        
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
        return true;

    }

    /**
     * Checks if the current dotCMS instance is running with a Platform License.
     * 
     * @return Returns {@code true} if the license level is Platform. Otherwise, returns
     *         {@code false}.
     */
    public boolean isPlatform() {
      return true;
    }

    
    private void licenseMessage() {

        if(LICENSE_INITED.compareAndSet(false, true)) {
            Logger.info(InitServlet.class,
                    "");
            Logger.info(InitServlet.class,
                    " * Copyright (c) " + ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getYear()
                            + " dotCMS LLC ");
            Logger.info(InitServlet.class,
                    " * This software, code and any modifications to the code are licensed under the terms of the dotCMS BSL License ");
            Logger.info(InitServlet.class,
                    " * which can be found here : https://www.github.com/dotCMS/core ");
            Logger.info(InitServlet.class,
                    "");
        }
        
    }



    private static class LicenceManagerHolder {
        static final LicenseManager INSTANCE = new LicenseManager();

    }

}
