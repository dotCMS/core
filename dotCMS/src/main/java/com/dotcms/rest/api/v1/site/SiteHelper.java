package com.dotcms.rest.api.v1.site;

import static com.dotmarketing.util.Logger.debug;
import static com.dotmarketing.util.Logger.error;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

/**
 * Provides all the utility methods used by the {@link SiteResource}
 * class to provide the required data to the UI layer or any other type of
 * client.
 *
 * @author jsanca
 */
public class SiteHelper implements Serializable {

	private static final long serialVersionUID = 1L;

	private final HostAPI hostAPI;

	public static final String EXT_HOSTADMIN = "sites";

	public static final String HAS_PREVIOUS = "hasPrevious";
	public static final String HAS_NEXT = "hasNext";
	public static final String TOTAL_SITES = "total";
	public static final String RESULTS = "results";

	@VisibleForTesting
	public SiteHelper (HostAPI hostAPI) {
		this.hostAPI = hostAPI;
	}

	/**
	 * Private constructor for the singleton holder.
	 */
	private SiteHelper () {
		this.hostAPI = APILocator.getHostAPI();
	}

	private static class SingletonHolder {
		private static final SiteHelper INSTANCE = new SiteHelper();
	}

	/**
	 * Get the instance.
	 * @return JsonWebTokenFactory
	 */
	public static SiteHelper getInstance() {
		return SiteHelper.SingletonHolder.INSTANCE;
	} // getInstance.


	/**
	 * Check if a Site is archived or not, keeping the exception quietly
	 * @param showArchived {@link Boolean}
	 * @param host {@link Host}
	 * @return Boolean
	 */
	public boolean checkArchived (final boolean showArchived, final Host host) {
		boolean checkArchived = false;
		try {

			checkArchived = (showArchived || !host.isArchived());
		} catch (Exception e) {
			error(SiteHelper.class, e.getMessage(), e);
		}

		return checkArchived;
	} // checkArchived.

	/**
	 * Return a site by user and site id
	 *
	 * @param user User to filter the host to return
	 * @param siteId Id to filter the host to return
	 * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
	 * @throws DotSecurityException if one is thrown when the sites are search
	 * @throws DotDataException if one is thrown when the sites are search
	 */
	public Host getSite(User user, String siteId) throws DotSecurityException, DotDataException {
		Host site = this.hostAPI.find(siteId, user, Boolean.TRUE);
		return site;
	}

	/**
	 * Determines what site is to be marked as "selected" by a user. If the
	 * currently selected site in the HTTP Session is part of the sites that a
	 * user (actual or impersonated user) has access to, the Identifier of such
	 * a site is returned. If the site in the session is not in the list of
	 * sites, the Identifier of the first site in the list must be returned.
	 *
	 * @param siteList
	 *            - The list of sites (their metadata) that a user has access
	 *            to.
	 * @param siteInSession
	 *            - The Identifier of the site that is marked as selected in the
	 *            current user session.
	 * @param user
	 *            - The current user session.
	 * @return The Identifier of the site that will be marked as "selected".
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public String getSelectedSite(final List<Host> siteList, final String siteInSession, User user)  {
		String selectedSite = UtilMethods.isSet(siteInSession) ? siteInSession : StringUtils.EMPTY;
		boolean siteFound = false;
		if (siteList != null && !siteList.isEmpty()) {
			Host host = null;
			try {
				host = this.hostAPI.find(siteInSession, user, Boolean.FALSE);
			} catch (DotDataException | DotSecurityException e) {
				/** The user doesn't have permission to see this host **/
				debug(SiteHelper.class, "User doesn't have permission to see host ["+siteInSession+"}. error"+e.getMessage());
			}

			if (null != host && UtilMethods.isSet(host.getIdentifier())) {
				siteFound = true;
				if(!siteList.contains(host)){
					siteList.add(host);
				}
			}

			/**
			 * If the user doesn't have permission to see the host or
			 * the host doesn't exist then get the first one available
			 * for the user
			 */
			if (!siteFound) {
				selectedSite = siteList.get(0).getIdentifier();
			}
		}
		return selectedSite;
	}

	/**
	 * Return the number of sites
	 *
	 * @return
	 */
	public long getSitesCount(final User user){
		return this.hostAPI.count(user, Boolean.FALSE);
	}

	/**
	 * Return the current site
	 *
	 * @param req
	 * @return
	 */
	public Host getCurrentSite(final HttpServletRequest req, final User user) {
		try {
			final HttpSession session = req.getSession();
			String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

			if(null==hostId){
				return WebAPILocator.getHostWebAPI().getHost(req);
			}else{
				return hostAPI.find(hostId, user, false);
			}

		} catch (DotDataException|DotSecurityException e) {
			throw new DotRuntimeException(e);
		}
	}

	public void switchSite(final HttpServletRequest req, final String hostId) {
		final HttpSession session = req.getSession();

		session.removeAttribute(WebKeys.CMS_SELECTED_HOST_ID); // we do this in order to get a properly behaviour of the SwichSiteListener
		session.setAttribute(WebKeys.CMS_SELECTED_HOST_ID, hostId);
		session.removeAttribute(WebKeys.CONTENTLET_LAST_SEARCH);
	}

	public Host switchToDefaultHost(final HttpServletRequest req, final User user)
			throws DotSecurityException, DotDataException {

		final Host defaultHost = this.hostAPI.findDefaultHost(user, false);
		this.switchSite(req, defaultHost.getIdentifier());
		return defaultHost;
	}
}
