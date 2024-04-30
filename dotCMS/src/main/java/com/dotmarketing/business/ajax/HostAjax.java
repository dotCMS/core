package com.dotmarketing.business.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import org.quartz.SchedulerException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class handles the communication between the view and the back-end service that returns information to the user
 * regarding Sites in dotCMS. The information provided by this service is accessed via DWR.
 * <p>
 * For example, the <b>Sites</b> portlet uses this class to display the list of Sites to the users, which can be
 * filtered by specific search criteria.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 */
public class HostAjax {

	private final HostAPI hostAPI = APILocator.getHostAPI();
	private final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	public Map<String, Object> findHostsForDataStore(String filter, boolean showArchived, int offset, int count) throws PortalException, SystemException, DotDataException, DotSecurityException {
		return findHostsForDataStore(filter, showArchived, offset, count, Boolean.FALSE);
	}

	public Map<String, Object> findHostsForDataStore(String filter, boolean showArchived, int offset, int count,
													 boolean includeSystemHost) throws PortalException, SystemException, DotDataException, DotSecurityException {

		if (filter.endsWith("*")) {
			filter = filter.substring(0, filter.length() - 1);
		}
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = this.userWebAPI.getLoggedInUser(req);

		List<Host> sites = this.hostAPI.findAll(user, this.userWebAPI.isLoggedToFrontend(req));
		List<Map<String, Object>> siteResults = new ArrayList<>();
		Collections.sort(sites, new HostNameComparator());

		Boolean addedSystemSite = false;
		//Make sure that if we are not using a filter the System Host is on top
		if ( includeSystemHost && !UtilMethods.isSet(filter) ) {
			final Host systemSite = this.hostAPI.findSystemHost();
			if ( APILocator.getPermissionAPI().doesUserHavePermission(systemSite, PermissionAPI.PERMISSION_READ, user) ) {
				siteResults.add(systemSite.getMap());
				addedSystemSite = true;
			}
		}

		for (Host site : sites) {
			if ( !showArchived && site.isArchived() ) {
				continue;
			}

			if ( (!includeSystemHost && site.isSystemHost())
					|| (site.isSystemHost() && addedSystemSite) ) {
				continue;
			}

			if (site.getHostname().toLowerCase().startsWith(filter.toLowerCase())) {
				siteResults.add(site.getMap());
			}
		}

		Map<String, Object> hostMapToReturn = new HashMap<>();
		hostMapToReturn.put("total", siteResults.size());
		hostMapToReturn.put("list", siteResults);
		return hostMapToReturn;
	}

    /**
     * Returns the complete list of Sites that exist in a dotCMS instance based on specific case-insensitive filtering
     * criteria, and <b>excluding the System Host</b>. When filtering results, only listed text-type fields can be
	 * searched, which are basically the two columns displayed in the UI: {@code Site Key}, and {@code Aliases}.
     *
     * @param filter       Search term used to filter results.
     * @param showArchived If archived Sites must be returned, set to {@code true}. Otherwise, set to {@code false}.
     * @param offset       Offset value, for pagination purposes.
     * @param count        Count value, for pagination purposes.
     *
     * @return The full or filtered list of Sites in your instance.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
    public Map<String, Object> findHostsPaginated(final String filter, final boolean showArchived, int offset, int count) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = this.getLoggedInUser();
		final boolean respectFrontend = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
		final List<Host> sitesFromDb = this.hostAPI.findAllFromDB(user, false, respectFrontend);
		final List<Field> fields = FieldsCache.getFieldsByStructureVariableName(Host.HOST_VELOCITY_VAR_NAME);
        final List<Field> searchableFields = fields.stream().filter(field -> field.isListed() && field
                .getFieldType().startsWith("text")).collect(Collectors.toList());

        List<Map<String, Object>> siteList = new ArrayList<>(sitesFromDb.size());
		sitesFromDb.sort(new HostNameComparator());
		for (final Host site : sitesFromDb) {
			boolean addToResultList = false;
			if (showArchived || !site.isArchived()) {
				if(!UtilMethods.isSet(filter)) {
                    addToResultList = true;
                } else {
					for (final Field searchableField : searchableFields) {
						final String value = site.getStringProperty(searchableField.getVelocityVarName());
						if (UtilMethods.isSet(value) && value.toLowerCase().contains(filter.toLowerCase())) {
                            addToResultList = true;
                            break;
                        }
					}
				}
			}
			if (addToResultList) {
				boolean siteInSetup = false;
				try {
                    siteInSetup = QuartzUtils.isJobScheduled("setup-host-" + site.getIdentifier(), "setup-host-group");
                } catch (final SchedulerException e) {
                    Logger.warn(HostAjax.class, String.format("An error occurred when reviewing the creation status for " +
                            "Site '%s': %s", site.getIdentifier(), e.getMessage()), e);
                }
                final Map<String, Object> siteInfoMap = site.getMap();
				siteInfoMap.putAll(Map.of(
                        "userPermissions", this.permissionAPI.getPermissionIdsFromUser(site, user),
                        "hostInSetup", siteInSetup,
                        "archived", site.isArchived(),
                        "live", site.isLive()));
				siteList.add(siteInfoMap);
			}
		}

        final long totalResults = siteList.size();
		// Paginate results only if required
		if (totalResults > count) {
            if (totalResults > 0 && count > 0) {
                offset = offset >= siteList.size() ? siteList.size() - 1 : offset;
                count = offset + count > siteList.size() ? siteList.size() : offset + count;
                siteList = siteList.subList(offset, count);
            }
        }

        final List<Map<String, Object>> fieldMapList = fields.stream().map(Field::getMap).collect(Collectors.toList());
        final Structure siteContentType = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(Host.HOST_VELOCITY_VAR_NAME);
        return Map.of(
                "total", totalResults,
                "list", siteList,
                "structure", siteContentType.getMap(),
                "fields", fieldMapList);
	}

	public List<Map<String, Object>> findAllHostThumbnails() throws PortalException, SystemException, DotDataException, DotSecurityException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		ContentletAPI contentAPI = APILocator.getContentletAPI();

		boolean respectFrontend = !userWebAPI.isLoggedToBackend(req);
		List<Host> hosts = hostAPI.findAll(user, respectFrontend);
		Collections.sort(hosts, new HostNameComparator());

		List<Map<String, Object>> listOfHosts = new ArrayList<>(hosts.size());

		for(Host host : hosts) {
			if(host.isSystemHost())
				continue;
			Map<String, Object> thumbInfo = new HashMap<>();
			thumbInfo.put("hostId", host.getIdentifier());
			thumbInfo.put("hostInode", host.getInode());
			thumbInfo.put("hostName", host.getHostname());
			File hostThumbnail = contentAPI.getBinaryFile(host.getInode(), Host.HOST_THUMB_KEY, user);
			boolean hasThumbnail = hostThumbnail != null;
			thumbInfo.put("hasThumbnail", hasThumbnail);
			thumbInfo.put("tagStorage", host.getMap().get("tagStorage"));

			listOfHosts.add(thumbInfo);
		}

		return listOfHosts;

	}

    /**
     * Starts the specified Site. From a Contentlet standpoint, this means that the Site will be "published".
     *
     * @param id The ID of the Site that will be started.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
	public void publishHost(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
        updateSiteStatus(id, true);
	}

    /**
     * Stops the specified Site. From a Contentlet standpoint, this means that the Site will be "unpublished".
     *
     * @param id The ID of the Site that will be stopped.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
    public void unpublishHost(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		updateSiteStatus(id, false);
	}

    /**
     * Starts or stops the specified Site, based on the specified flag. From a Contentlet standpoint, this means that the
     * Site will be "published" or "unpublished".
     *
     * @param id        The ID of the Site that will be stopped.
     * @param startSite If the Site must be started, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
    private void updateSiteStatus(final String id, final boolean startSite)  throws DotDataException, DotSecurityException, PortalException, SystemException {
        final User user = this.getLoggedInUser();
        final boolean respectFrontendRoles = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
        final Host site = this.hostAPI.find(id, user, respectFrontendRoles);
        if (startSite) {
            this.hostAPI.publish(site, user, respectFrontendRoles);
        } else {
            this.hostAPI.unpublish(site, user, respectFrontendRoles);
        }
    }

    /**
     * Archives the specified Site. Notice that the "default" Site cannot be archived, which means that another Site
     * must be selected as default before doing this.
     *
     * @param id The ID of the Site that will be archived.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
    public void archiveHost(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = this.userWebAPI.getLoggedInUser(this.getHttpRequest());
		final boolean respectFrontendRoles = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
		final Host site = this.hostAPI.find(id, user, respectFrontendRoles);
		if (site.isDefault()) {
            throw new DotStateException(String.format("The default Site '%s' can't be archived. Set another site as " +
                    "'default' first.", site));
        }
		if (site.isLocked()) {
			APILocator.getContentletAPI().unlock(site, user, respectFrontendRoles);
		}
		this.hostAPI.archive(site, user, respectFrontendRoles);
	}

    /**
     * Archives the selected Site.
     *
     * @param id The ID of the Site that will be un-archived.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
	public void unarchiveHost(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = this.getLoggedInUser();
		boolean respectFrontendRoles = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
		final Host site = this.hostAPI.find(id, user, respectFrontendRoles);
		this.hostAPI.unarchive(site, user, respectFrontendRoles);
	}

    /**
     * Deletes the specified archived Site. Notice that the "default" Site cannot be deleted, which means that another
     * Site must be selected as default before doing this.
     * <p>
     * Additionally, it's very important to point out that this operation can take an important amount of time depending
     * on how many objects belong to the Site that will be deleted.
     * </p>
     *
     * @param id The ID of the Site that will be un-archived.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
    public void deleteHost(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = this.getLoggedInUser();
		boolean respectFrontendRoles = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
		final Host site = this.hostAPI.find(id, user, respectFrontendRoles);
		if (site.isDefault()) {
            throw new DotStateException(String.format("The default Site '%s' can't be deleted. Set another site as " +
                    "'default' first.", site));
        }
		this.hostAPI.delete(site, user, respectFrontendRoles, true);
	}

    /**
     * Marks the specified Site as "default".
     *
     * @param id The ID of the Site that will be set as default.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User requesting this information does not have the required permissions to do
     *                              so.
     * @throws PortalException      The User requesting this information does not have the required permissions to
     *                              access the dotCMS back-end.
     * @throws SystemException      An application error has occurred.
     */
	public void makeDefault(final String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = this.getLoggedInUser();
		final boolean respectFrontendRoles = !this.userWebAPI.isLoggedToBackend(this.getHttpRequest());
		final Host site = this.hostAPI.find(id, user, respectFrontendRoles);
        this.hostAPI.makeDefault(site, user, respectFrontendRoles);
	}

	public int getHostSetupProgress(String hostId) {
		return QuartzUtils.getTaskProgress("setup-host-" + hostId, "setup-host-group");
	}

	public Map<String, Object> fetchByIdentity(String id) throws DotDataException, DotSecurityException {
		Host host = hostAPI.find(id, userWebAPI.getSystemUser(), false);
		return host.getMap();
	}

    /**
     * Returns the {@link User} that is currently logged into the dotCMS back-end.
     *
     * @return The currently logged-in {@link User}.
     */
	protected User getLoggedInUser() {
        final WebContext ctx = WebContextFactory.get();
        final HttpServletRequest req = ctx.getHttpServletRequest();
        return this.userWebAPI.getLoggedInUser(req);
    }

    /**
     * Returns the current {@link HttpServletRequest} instance.
     *
     * @return The {@link HttpServletRequest} instance.
     */
    protected HttpServletRequest getHttpRequest() {
        final WebContext ctx = WebContextFactory.get();
        return ctx.getHttpServletRequest();
    }

}
