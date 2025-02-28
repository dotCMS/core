/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.license.DotLicenseRepoEntry;
import com.dotmarketing.exception.DotDataException;

/**
 * Wrapper over licenseUtil to able to reference and mock
 * licenseUtil methods
 */
public class LicenseService {

	/**
	 * Check for whether the current application server is supported with the installed license or not
	 * @return true if the current application server is supported with the installed license.
	 */
	public boolean isASAllowed(){
		return LicenseUtil.isASAllowed();
	}

	public Date getValidUntil() {
	    return LicenseUtil.getValidUntil();
	}

	public boolean isPerpetual() {
	    return LicenseUtil.isPerpetual();
	}

	public String getClientName() {
	    return LicenseUtil.getClientName();
	}

	public String getLevelName() {
	    return LicenseUtil.getLevelName();
	}

	public String getSerial() {
	    return LicenseUtil.getSerial();
	}

    public String getDisplaySerial () {
        return LicenseUtil.getDisplaySerial();
    }

    public String getDisplaySerial ( String serial ) {
        return LicenseUtil.getDisplaySerial(serial);
    }

	public String getDisplayServerId() {
	    return LicenseUtil.getDisplayServerId();
	}

    public String getDisplayServerId ( String serverId ) {
    	return LicenseUtil.getDisplayServerId(serverId);
    }

	public int getLevel(){
		return LicenseUtil.getLevel();
	}

    /**
     * Processes the license form
     *
     * @param request
     * @return
     * @throws Exception
     */
    public String processForm ( HttpServletRequest request ) throws Exception {
    	return LicenseUtil.processForm(request);
    }

    public void setUpLicenseRepo() throws Exception {
        LicenseUtil.setUpLicenseRepo();
    }


    public int getLicenseRepoTotal() throws DotDataException {
        return LicenseUtil.getLicenseRepoTotal();
    }

    public int getLicenseRepoAvailableCount() throws DotDataException {
        return LicenseUtil.getLicenseRepoAvailableCount();
    }

    public void updateLicenseHeartbeat() throws DotDataException {
    	LicenseUtil.updateLicenseHeartbeat();
    }

    public List<DotLicenseRepoEntry> getLicenseRepoList() throws DotDataException, IOException {
        return LicenseUtil.getLicenseRepoList();
    }

    public void uploadLicenseRepoFile(InputStream in) throws DotDataException, IOException {
    	LicenseUtil.uploadLicenseRepoFile(in);
    }

    public void deleteLicense(String id) throws DotDataException {
    	LicenseUtil.deleteLicense(id);
    }

    public void startLiveMode() {
    	LicenseUtil.startLiveMode();
    }

    public void stopLiveMode() {
    	LicenseUtil.stopLiveMode();
    }

    public String getLicenseType() {
    	return LicenseUtil.getLicenseType();
    }

    public void pickLicense(String serial) throws Exception {
    	LicenseUtil.pickLicense(serial);
    }

    public void freeLicenseOnRepo() throws DotDataException {
    	LicenseUtil.freeLicenseOnRepo();
    }
    public void freeLicenseOnRepo(String licenseID, String server_id) throws DotDataException {
        LicenseUtil.freeLicenseOnRepo(licenseID, server_id);
    }
}
