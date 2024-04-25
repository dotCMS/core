package com.dotcms.rest.api.v1.site;

import static com.dotmarketing.util.Logger.debug;
import static com.dotmarketing.util.Logger.error;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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

	private final transient HostAPI hostAPI;
	private final transient HostVariableAPI hostVariableAPI;

	public static final String EXT_HOSTADMIN = "sites";

	public static final String HAS_PREVIOUS = "hasPrevious";
	public static final String HAS_NEXT = "hasNext";
	public static final String TOTAL_SITES = "total";
	public static final String RESULTS = "results";

	@VisibleForTesting
	public SiteHelper(HostAPI hostAPI, HostVariableAPI hostVariableAPI) {
		this.hostAPI = hostAPI;
		this.hostVariableAPI = hostVariableAPI;
	}

	/**
	 * Private constructor for the singleton holder.
	 */
	private SiteHelper () {
		this.hostAPI = APILocator.getHostAPI();
		this.hostVariableAPI = APILocator.getHostVariableAPI();
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
	 * Saves the given Site along with its variables.
	 *
	 * @param site              The Site to be saved.
	 * @param siteVariableForms The List of SimpleSiteVariableForm objects representing the
	 *                          variables for the Site.
	 * @param user              The User performing the save operation.
	 * @param respectAnonPerms  A boolean value indicating whether to respect anonymous user
	 *                          permissions.
	 * @return The SiteView object representing the saved Site along with its variables.
	 * @throws DotSecurityException  If there is a security exception during the save operation.
	 * @throws DotDataException      If there is a data exception during the save operation.
	 * @throws AlreadyExistException If a Site with the same hostname already exists.
	 * @throws LanguageException     If there is an exception related to the language during the
	 *                               save operation.
	 */
	public SiteView save(final Host site, final List<SimpleSiteVariableForm> siteVariableForms,
			final User user, final boolean respectAnonPerms)
			throws DotSecurityException, DotDataException, AlreadyExistException, LanguageException {

		if(null != hostAPI.findByName(site.getHostname(), user, respectAnonPerms)){
		   throw new AlreadyExistException(String.format("A site named `%s` already exists.",site.getHostname()));
		}

		// Saving the site
		var savedSite = hostAPI.save(site, user, respectAnonPerms);

		// Saving the variables
		List<HostVariable> variablesForHost = List.of();
		if (siteVariableForms != null) {
			final List<HostVariable> siteVariables = toHostVariable(
					savedSite.getIdentifier(), user, siteVariableForms
			);
			variablesForHost = saveVariables(siteVariables, savedSite, user, respectAnonPerms);
		}

		return toView(savedSite, variablesForHost, user);
	}

	/**
	 * Updates a site along with its variables.
	 *
	 * @param site              The host object representing the site to update.
	 * @param siteVariableForms The list of site variable forms to update.
	 * @param user              The user performing the update.
	 * @param respectAnonPerms  A boolean indicating whether to respect anonymous permissions.
	 * @return The updated site view.
	 * @throws DotSecurityException  If there is a security exception.
	 * @throws DotDataException      If there is a data exception.
	 * @throws DoesNotExistException If the site does not exist.
	 * @throws LanguageException     If there is a language exception.
	 */
	public SiteView update(final Host site, final List<SimpleSiteVariableForm> siteVariableForms,
			final User user, final boolean respectAnonPerms)
			throws DotSecurityException, DotDataException, DoesNotExistException, LanguageException {

		// Saving the site
		var savedSite = hostAPI.save(site, user, respectAnonPerms);

		// Saving the variables
		List<HostVariable> variablesForHost = null;
		if (siteVariableForms != null) {
			final List<HostVariable> siteVariables = toHostVariable(
					site.getIdentifier(), user, siteVariableForms
			);
			variablesForHost = saveVariables(siteVariables, savedSite, user, respectAnonPerms);
		}

		return toView(savedSite, variablesForHost, user);
	}

	/**
	 * Saves a list of variables for a given site and user.
	 *
	 * @param variables            The list of HostVariable objects to be saved.
	 * @param site                 The Host object representing the site.
	 * @param user                 The User object representing the user.
	 * @param respectFrontendRoles Flag to indicate whether or not to respect frontend roles.
	 * @return The updated list of saved HostVariable objects.
	 * @throws DotSecurityException If there is a security exception.
	 * @throws DotDataException     If there is a data exception.
	 * @throws LanguageException    If there is a language exception.
	 */
	public List<HostVariable> saveVariables(final List<HostVariable> variables,
			final Host site, final User user, final boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException, LanguageException {

		// First we need to validate the variables
		for (final HostVariable next : variables) {
			validateVariable(next, user);
		}

		return hostVariableAPI.save(variables,
				site.getIdentifier(),
				user,
				respectFrontendRoles
		);
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
		return hostDeleteResultOpt.orElse(null);
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

	/**
	 * Validates a HostVariable object.
	 *
	 * @param siteVariable The HostVariable object to validate.
	 * @param user         The User object associated with the validation.
	 * @throws LanguageException        If there is an error with the language utilized.
	 * @throws IllegalArgumentException If the siteVariable is empty or contains invalid
	 *                                  characters.
	 */
	public void validateVariable(final HostVariable siteVariable, final User user)
			throws LanguageException {

		if (!UtilMethods.isSet(siteVariable.getKey())) {
			throw new IllegalArgumentException(
					LanguageUtil.get(user, "message.hostvariables.key.required"));
		}

		if (RegEX.contains(siteVariable.getKey(), "[^A-Za-z0-9]")) {
			throw new IllegalArgumentException(
					LanguageUtil.get(user, "message.hostvariables.exist.error.regex"));
		}
	}

	/**
	 * Validates if a given site variable already exists in the list of existing variables. If a
	 * variable with the same key exists and has a different ID, it throws an
	 * IllegalArgumentException.
	 *
	 * @param siteVariable      the site variable to validate
	 * @param existingVariables the list of existing variables to check against
	 * @param user              the current user object
	 * @throws LanguageException        if there is an issue with retrieving the error message from
	 *                                  the LanguageUtil
	 * @throws IllegalArgumentException if a variable with the same key already exists and has a
	 *                                  different ID
	 */
	public void validateVariableAlreadyExist(final HostVariable siteVariable,
			final List<HostVariable> existingVariables, final User user) throws LanguageException {

		for (final HostVariable next : existingVariables) {

			if (next.getKey().equals(siteVariable.getKey()) &&
					!next.getId().equals(siteVariable.getId())) {
				throw new IllegalArgumentException(
						LanguageUtil.get(user, "message.hostvariables.exist.error.key")
				);
			}
		}
	}

	static SiteView toView(final Host host)
			throws DotStateException, DotDataException, DotSecurityException {
		return toView(host, null);
	}

	static SiteView toView(final Host host, final User user)
			throws DotStateException, DotDataException, DotSecurityException {
		return toView(host, null, user);
	}

	static SiteView toView(final Host host, final List<HostVariable> siteVariables,
			final User user) throws DotStateException, DotDataException, DotSecurityException {

		final SiteView.Builder builder = SiteView.Builder.builder();
		builder.withIdentifier(host.getIdentifier())
				.withInode(host.getInode())
				.withAliases(host.getAliases())
				.withSiteName(host.getHostname())
				.withTagStorage(host.getTagStorage())
				.withSiteThumbnail(null != host.getHostThumbnail() ? host.getHostThumbnail().getName(): StringPool.BLANK)
				.withRunDashboard(host.getBoolProperty(SiteResource.RUN_DASHBOARD))
				.withKeywords(host.getStringProperty(SiteResource.KEYWORDS))
				.withDescription(host.getStringProperty(SiteResource.DESCRIPTION))
				.withGoogleMap(host.getStringProperty(SiteResource.GOOGLE_MAP))
				.withGoogleAnalytics(host.getStringProperty(SiteResource.GOOGLE_ANALYTICS))
				.withAddThis(host.getStringProperty(SiteResource.ADD_THIS))
				.withProxyUrlForEditMode(host.getStringProperty(SiteResource.PROXY_EDIT_MODE_URL))
				.withEmbeddedDashboard(host.getStringProperty(SiteResource.EMBEDDED_DASHBOARD))
				.withLanguageId(host.getLanguageId())
				.withIsSystemHost(host.isSystemHost())
				.withIsDefault(host.isDefault())
				.withIsArchived(host.isArchived())
				.withIsLive(host.isLive())
				.withIsLocked(host.isLocked())
				.withIsWorking(host.isWorking())
				.withModDate(host.getModDate())
				.withModUser(host.getModUser());

		final List<HostVariable> variablesForHost;
		if (null != user && siteVariables == null) {
			variablesForHost = getVariablesForHost(host, user);
		} else {
			variablesForHost = siteVariables;
		}

		if (null != variablesForHost) {
			final List<SimpleSiteVarView> siteVariableViews = variablesForHost.stream()
					.map(variable -> {
						SimpleSiteVarView.Builder siteVarBuilder = SimpleSiteVarView.builder();
						return siteVarBuilder.name(variable.getName())
								.id(variable.getId())
								.key(variable.getKey())
								.value(variable.getValue())
								.build();
					}).collect(Collectors.toList());
			builder.withVariables(siteVariableViews);
		} else {
			builder.withVariables(List.of());
		}

		return builder.build();
	}

	/**
	 * Converts a list of SimpleSiteVariableForm objects to a list of HostVariable objects.
	 *
	 * @param siteId           the site ID
	 * @param user             the user object
	 * @param siteVariableForm the list of SimpleSiteVariableForm objects to be converted
	 * @return the list of converted HostVariable objects
	 */
	static List<HostVariable> toHostVariable(final String siteId, final User user,
			final List<SimpleSiteVariableForm> siteVariableForm) {

		return siteVariableForm.stream()
				.map(form -> toHostVariable(siteId, user, form))
				.collect(Collectors.toList());
	}

	/**
	 * Converts a SimpleSiteVariableForm object into a HostVariable object.
	 *
	 * @param siteId           The ID of the site.
	 * @param user             The user object.
	 * @param siteVariableForm The SimpleSiteVariableForm object to be converted.
	 * @return A HostVariable object.
	 */
	static HostVariable toHostVariable(final String siteId, final User user,
			final SimpleSiteVariableForm siteVariableForm) {

		var siteVariable = new HostVariable();

		siteVariable.setId(siteVariableForm.getId());
		siteVariable.setHostId(siteId);
		siteVariable.setName(siteVariableForm.getName());
		siteVariable.setKey(siteVariableForm.getKey());
		siteVariable.setValue(siteVariableForm.getValue());
		siteVariable.setLastModifierId(user.getUserId());
		siteVariable.setLastModDate(new Date());

		return siteVariable;
	}

	static List<HostVariable> getVariablesForHost(final Host host, final User user)
			throws DotDataException, DotSecurityException {
		if (null == user){
			return List.of();
		}
		return APILocator.getHostVariableAPI()
				.getVariablesForHost(host.getIdentifier(), user, false);
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
