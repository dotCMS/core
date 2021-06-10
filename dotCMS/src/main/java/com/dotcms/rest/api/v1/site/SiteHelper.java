package com.dotcms.rest.api.v1.site;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.dotmarketing.util.Logger.debug;
import static com.dotmarketing.util.Logger.error;

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

	/**
	 * Retrieve all sites
	 * @param user
	 * @param respectFrontend
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Host> findAll(final User user, final boolean respectFrontend) throws DotSecurityException, DotDataException {

		return hostAPI.findAll(user, respectFrontend);
	}

	/**
	 * Unlock a site
	 * @param site
	 * @param user
	 * @param respectAnonPerms
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void unlock(final Host site, final User user, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

		APILocator.getContentletAPI().unlock(site, user, respectAnonPerms);
	}

	/**
	 * Archive a site
	 * @param site
	 * @param user
	 * @param respectAnonPerms
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void archive(final Host site, final User user, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

		hostAPI.archive(site, user, respectAnonPerms);
	}

	/**
	 * UnArchive a site
	 * @param site
	 * @param user
	 * @param respectAnonPerms
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void unarchive(final Host site, final User user, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

		hostAPI.unarchive(site, user, respectAnonPerms);
	}

	/**
	 * Save a new or existing site
	 * @param site
	 * @param user
	 * @param respectAnonPerms
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Host save(final Host site, final User user, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

		return APILocator.getHostAPI().save(site, user, respectAnonPerms);
	}

	/**
	 * Deletes a site
	 * @param site
	 * @param user
	 * @param respectAnonPerms
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Future<Boolean> delete(final Host site, final User user, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

		final Optional<Future<Boolean>> hostDeleteResultOpt = hostAPI.delete(site, user, respectAnonPerms, true);
		return hostDeleteResultOpt.isPresent()?hostDeleteResultOpt.get():null;
	}

	/**
	 * Make default a site
	 * @param site
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public boolean makeDefault(Host site, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

		this.hostAPI.makeDefault(site, user, respectFrontendRoles);
		return true;
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
	 * @param site {@link Host}
	 * @return Boolean
	 */
	public boolean checkArchived (final boolean showArchived, final Host site) {
		boolean checkArchived = false;
		try {

			checkArchived = (showArchived || !site.isArchived());
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
		final Host site = this.hostAPI.find(siteId, user, Boolean.TRUE);
		return site;
	}

	/**
	 * Return a site by user and site id, respect front roles = false
	 *
	 * @param user User to filter the host to return
	 * @param siteId Id to filter the host to return
	 * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
	 * @throws DotSecurityException if one is thrown when the sites are search
	 * @throws DotDataException if one is thrown when the sites are search
	 */
	public Host getSiteNoFrontEndRoles(final User user, final String siteId) throws DotSecurityException, DotDataException {

		final Host site = this.hostAPI.find(siteId, user, Boolean.FALSE);
		return site;
	}

	/**
	 * Return a site by user and site name
	 *
	 * @param user User to filter the host to return
	 * @param siteName name to filter the host to return
	 * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
	 * @throws DotSecurityException if one is thrown when the sites are search
	 * @throws DotDataException if one is thrown when the sites are search
	 */
	public Host getSiteByName(User user, String siteName) throws DotSecurityException, DotDataException {
		final Host site = this.hostAPI.findByName(siteName, user, Boolean.TRUE);
		return site;
	}

	/**
	 * Return a site by user and site name, respect front roles = false
	 *
	 * @param user User to filter the host to return
	 * @param siteName name to filter the host to return
	 * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
	 * @throws DotSecurityException if one is thrown when the sites are search
	 * @throws DotDataException if one is thrown when the sites are search
	 */
	public Host getSiteByNameNoFrontEndRoles(final User user, final String siteName) throws DotSecurityException, DotDataException {

		final Host site = this.hostAPI.findByName(siteName, user, Boolean.FALSE);
		return site;
	}

	/**
	 * Publish a site
	 * @param site {@link Host}
	 * @param user {@link User}
	 * @param respectAnonPerms {@link Boolean}
	 * @return Host
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host publish(final Host site, final User user, final boolean respectAnonPerms) throws DotContentletStateException, DotDataException, DotSecurityException {

		this.hostAPI.publish(site, user, respectAnonPerms);
		return site;
	}

	/**
	 * Unpublish a site
	 * @param site {@link Host}
	 * @param user {@link User}
	 * @param respectAnonPerms {@link Boolean}
	 * @return Host
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host unpublish(final Host site, final User user, final boolean respectAnonPerms) throws DotContentletStateException, DotDataException, DotSecurityException {

		this.hostAPI.unpublish(site, user, respectAnonPerms);
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
			String siteId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

			if(null==siteId){
				return WebAPILocator.getHostWebAPI().getHost(req);
			}else{
				return hostAPI.find(siteId, user, false);
			}

		} catch (DotDataException|DotSecurityException e) {
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Switch a site
	 * @param req
	 * @param siteId
	 */
	public void switchSite(final HttpServletRequest req, final String siteId) {

		// we do this in order to get a properly behaviour of the SwichSiteListener
		HostUtil.switchSite(req, siteId);
	}

	/**
	 * Switch to the default host
	 * @param req
	 * @param user
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Host switchToDefaultHost(final HttpServletRequest req, final User user)
			throws DotSecurityException, DotDataException {

		final Host defaultSite = this.hostAPI.findDefaultHost(user, false);
		this.switchSite(req, defaultSite.getIdentifier());
		return defaultSite;
	}
}
