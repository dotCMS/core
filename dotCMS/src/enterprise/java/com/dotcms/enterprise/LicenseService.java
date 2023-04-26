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

