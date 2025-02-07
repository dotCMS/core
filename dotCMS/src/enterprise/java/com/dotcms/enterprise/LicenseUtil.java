/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.cluster.bean.Server;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.license.DotLicenseRepoEntry;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.enterprise.license.LicenseRepoDAO;
import com.dotcms.enterprise.license.LicenseType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;

/**
 * Provides utility methods to access to licensing information in dotCMS.
 * 
 * @author root
 * @version 1.x
 *
 */
public class LicenseUtil {


	public static final String IMPORTED_LICENSE_PACK_PREFIX = "imported_";
	public static final String LICENSE_NAME = "license";
	/**
	 * Verifies if dotCMS can run in the current application server.
	 * 
	 * @return Returns true if dotCMS is able to start up. Otherwise, returns
	 *         false.
	 */
	public static boolean isASAllowed() {
		return true;
	}

	/**
	 * Returns the last day this license will be valid.
	 * 
	 * @return The valid-until date.
	 */
	public static Date getValidUntil() {
	    return LicenseManager.getInstance().getValidUntil();
	}

	/**
	 * Checks if the current license is perpetual.
	 * 
	 * @return Returns true if the license is perpetual. Otherwise, returns
	 *         false.
	 */
	public static boolean isPerpetual() {
	    return LicenseManager.getInstance().isPerpetual();
	}

	/**
	 * Returns the name of the client that the current license belongs to.
	 * 
	 * @return The client's name.
	 */
	public static String getClientName() {
	    return LicenseManager.getInstance().getClientName();
	}

	/**
	 * Returns the official dotCMS license name based on the current license
	 * level.
	 * 
	 * @return The official license name.
	 */
	public static String getLevelName() {
	    return LicenseManager.getInstance().getLevelName(getLevel());
	}

	/**
	 * Returns the serial number associated to this license.
	 * 
	 * @return The serial number.
	 */
	public static String getSerial() {
	    return LicenseManager.getInstance().getSerial();
	}

	/**
	 * Returns a piece of the current server's serial number. The complete
	 * number is not displayed because of security reasons.
	 * 
	 * @return The safe version of the serial number.
	 */
    public static String getDisplaySerial () {
        return LicenseManager.getInstance().getDisplaySerial();
    }

	/**
	 * Returns a piece of the specified serial number. The complete number is
	 * not displayed because of security reasons.
	 * 
	 * @param serial
	 *            - The serial number.
	 * @return The safe version of the serial number.
	 */
    public static String getDisplaySerial ( String serial ) {
        return LicenseManager.getInstance().getDisplaySerial( serial );
    }

    /**
     * 
     * @return
     */
	public static String getDisplayServerId() {
	    return LicenseManager.getInstance().getDisplayServerId();
	}




	public static void deleteOldLicenses() {
		try {
			LicenseRepoDAO.deleteOldLicenses();
		}catch(Exception e){
			Logger.warn(LicenseUtil.class, "Error deleting old licenses", e);
		}
	}
	/**
	 * 
	 * @param serverId
	 * @return
	 */
    public static String getDisplayServerId ( String serverId ) {
        return LicenseManager.getInstance().getDisplayServerId( serverId );
    }
    
    /**
     *
     * @return
     */
    public static String getServerId ( ) {
        return APILocator.getServerAPI().readServerId() ;
    }

	/**
	 * Returns the license level set for the current dotCMS instance.
	 * 
	 * @return The license level.
	 */
	public static int getLevel(){
		return LicenseLevel.PLATFORM.level;
	}

	/**
	 * Processes the license form submitted though an HTTP Post.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return In case an something goes wrong during the process, an error
	 *         message will be returned. Otherwise, a null value indicates a
	 *         successful execution.
	 * @throws Exception
	 *             An error occurred when updating the server's data.
	 */
    public static String processForm ( HttpServletRequest request ) throws Exception {
        HttpSession session = request.getSession();
        String error = null;

        String iWantTo = getParameter( request, "iwantTo" );
        switch ( iWantTo ) {
            case "paste_license":
                try {
                    if ( UtilMethods.isSet( getParameter( request, "license_text" ) ) ) {

                        String text = getParameter( request, "license_text" );
                        try {
                            LicenseManager.getInstance().uploadLicense( text );
                        } catch ( Throwable aiobe ) {
                            error = "Invalid License";
                        }
                        if ( getLevel() == LicenseLevel.COMMUNITY.level ) {
                            error = "No License Found";
                        }
                    } else {
                        error = "invalid.license";
                    }
                } catch ( Exception e ) {
                    Logger.error( LicenseUtil.class, "IOException: " + e.getMessage(), e );
                    error = "invalid.license";
                }
                break;
            case "reset-license":
                String sid = APILocator.getServerAPI().readServerId();
                Server server = APILocator.getServerAPI().getServer( sid );
                APILocator.getServerAPI().updateServer( server );
                break;
        }

        //Clean up the session
        if ( session != null ) {
            session.removeAttribute( "applyForm" );
            session.removeAttribute( "iwantTo" );
            session.removeAttribute( "paste_license" );
            session.removeAttribute( "license_text" );
            session.removeAttribute( "request_code" );
            session.removeAttribute( "license_type" );
            session.removeAttribute( "license_level" );
        }

        return error;
    }

	/**
	 * Verifies if a requested value is found as a request parameter or into the
	 * session as an attribute.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param parameterName
	 *            - The parameter name to look for.
	 * @return The parameter's value.
	 */
    private static String getParameter ( HttpServletRequest request, String parameterName ) {

        HttpSession session = request.getSession();

        if ( session != null && UtilMethods.isSet( session.getAttribute( parameterName ) ) ) {
            return (String) session.getAttribute( parameterName );
        }

        if ( UtilMethods.isSet( request.getParameter( parameterName ) ) ) {
            return request.getParameter( parameterName );
        }

        return null;
    }

    /**
     * 
     * @throws Exception
     */
    public static void setUpLicenseRepo() throws Exception {


        LicenseManager.getInstance().takeLicenseFromRepoIfNeeded();


        if(LicenseManager.getInstance().getLevel()==LicenseLevel.COMMUNITY.level) {
            Logger.info(LicenseUtil.class, "Not joining cluster. A valid license is needed to joing the cluster");
        }
    }


    /**
	 * Returns the total number of licenses in the current instance.
	 * 
	 * @return The total number of licenses.
	 * @throws DotDataException
	 *             An error occurred when retrieving the data from the database.
	 */
    public static int getLicenseRepoTotal() throws DotDataException {
        return LicenseManager.getInstance().getLicenseRepoTotal();
    }

    /**
	 * Returns the number of site licenses that are available in the current
	 * instance.
	 * 
	 * @return The number of available licenses.
	 * @throws DotDataException
	 *             An error occurred when retrieving the data from the database.
	 */
    public static int getLicenseRepoAvailableCount() throws DotDataException {
        return LicenseManager.getInstance().getLicenseRepoAvailableCount();
    }

    /**
     * 
     * @throws DotDataException
     */
    public static void updateLicenseHeartbeat() throws DotDataException {
        LicenseManager.getInstance().updateLicenseHeartbeat();
    }

    /**
	 * Returns the site licenses that are available in the current dotCMS
	 * instance.
	 * 
	 * @return The available site licenses.
	 * @throws DotDataException
	 *             An error occurred when retrieving the licenses.
	 * @throws IOException
	 *             An error occurred when validating the license file.
	 */
    public static List<DotLicenseRepoEntry> getLicenseRepoList() throws DotDataException, IOException {
        return LicenseManager.getInstance().getLicenseRepoList();
    }

	/**
	 * Uploads dotCMS licenses via license pack to the current server.
	 * 
	 * @param in
	 *            - The zip file's data.
	 * @throws DotDataException
	 *             An error occurred when reading the zip's data.
	 * @throws IOException
	 *             The licenses could not be added to the server.
	 */
    public static void uploadLicenseRepoFile(InputStream in) throws DotDataException, IOException {
        //LicenseManager.getInstance().insertAvailableLicensesFromZipFile(in);
    }

    /**
	 * Removes the specified license from the current server.
	 * 
	 * @param id
	 *            - The license ID.
	 * @throws DotDataException
	 *             An error occurred when removing the data.
	 */
    public static void deleteLicense(String id) throws DotDataException {
        LicenseManager.getInstance().deleteLicense(id);
    }

    // dev license bottle neck
    protected static Semaphore devsem=new Semaphore(1, true);

    /**
     * 
     */
    public static void startLiveMode() {
        if(LicenseManager.getInstance().getLevel()>LicenseLevel.COMMUNITY.level && LicenseManager.getInstance().getLicenseType().equals("dev")) {
            try {
                devsem.acquire();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * 
     */
    public static void stopLiveMode() {
        if(LicenseManager.getInstance().getLevel()>LicenseLevel.COMMUNITY.level && LicenseManager.getInstance().getLicenseType().equals("dev")) {
            devsem.release();
        }
    }

    /**
	 * Returns the type of the current license.
	 * 
	 * @return The license type.
	 */
    public static String getLicenseType() {
    	
    	return LicenseType.fromString(LicenseManager.getInstance().getLicenseType()).type;
    	

    }

    /**
	 * Applies the specified license serial to the current dotCMS instance.
	 * 
	 * @param serial
	 *            - The serial number to apply.
	 * @throws Exception
	 *             An error occurred when applying the license.
	 */
    public static void pickLicense(String serial) throws Exception {
        LicenseManager.getInstance().forceLicenseFromRepo(serial);
    }

    /**
	 * Removes the current license from the server. The default license level is
	 * Community.
	 * 
	 * @throws DotDataException
	 *             An error occurred when setting up the default license.
	 */
    public static void freeLicenseOnRepo() throws DotDataException {
        LicenseManager.getInstance().freeLicenseOnRepo();
    }

    /**
	 * Removes the specified license serial number from a specific server
	 * instance.
	 * 
	 * @param licenseID
	 *            - The license serial number.
	 * @param server_id
	 *            - The ID of the server whose license will be removed.
	 * @throws DotDataException
	 *             An error occurred when resetting the server's license.
	 */
    public static void freeLicenseOnRepo(String licenseID, String server_id) throws DotDataException {
        LicenseManager.getInstance().freeLicenseOnRepo(licenseID, server_id);
    }

	/**
	 * Checks if the license is going to expire in 30 days or less and show a
	 * warning message.
	 * If the license already expired shows an error message.
	 * This only applies for users that have CMS Admin Role.
	 *
	 * @param user - user that is being logged in
	 * @throws DotDataException
	 * @throws LanguageException
	 */
	public static void licenseExpiresMessage(final User user) throws DotDataException, LanguageException {
		final long daysleftLicenseToExpire = DateUtil.diffDates(new Date(), getValidUntil()).get("diffDays"); //days left for license to expire
		if (APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole()) && //check if user have CMSAdmin Role
				(daysleftLicenseToExpire <= 30) && //check if license is gonna expire in 30 days or less
				!isPerpetual()) { //check if license is perpetual
			//Message if license is going to expire soon
			final SystemMessageBuilder message = new SystemMessageBuilder()
					.setMessage(LanguageUtil.format(
							user.getLocale(),
							"license-expires-soon-message",
							daysleftLicenseToExpire))
					.setSeverity(MessageSeverity.WARNING)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setLife(86400000);
			//if license already expired change message and severity
			if (daysleftLicenseToExpire < 0) {
				message.setMessage(LanguageUtil.get(
						user.getLocale(),
						"license-expired-message"));
				message.setSeverity(MessageSeverity.ERROR);
			}
			DotConcurrentFactory.getInstance().getSubmitter().delay(() -> {
						SystemMessageEventUtil.getInstance().pushMessage(message.create(), list(user.getUserId()));
						Logger.info(LicenseUtil.class,message.create().getMessage().toString());
					},
					3000, TimeUnit.MILLISECONDS);
		}
	}

	public static String getLicenseText() {

		Path path = Paths.get(Config.CONTEXT.getRealPath("/WEB-INF/LICENSE"));
		try (InputStream fis = Files.newInputStream(path)) {
			return IOUtils.toString(fis, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Logger.error(LicenseUtil.class, "Error reading LICENSE file", e);
			return "Please see the LICENSE file in the root directory of the dotCMS git hub repo: https://github.com/dotCMS/core/blob/main/LICENSE";
		}
	}

}
