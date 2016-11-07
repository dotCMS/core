package com.dotcms.rest.api.v1.site;

import static com.dotmarketing.util.Logger.error;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;

/**
 * Just a helper {@link SiteBrowserResource}
 *
 * @author jsanca
 */
public class SiteBrowserHelper implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HostAPI hostAPI;
    private final static HostNameComparator HOST_NAME_COMPARATOR =
            new HostNameComparator();

    public static final String EXT_HOSTADMIN = "EXT_HOSTADMIN";

    @VisibleForTesting
    public SiteBrowserHelper (HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    private SiteBrowserHelper () {
        this.hostAPI = APILocator.getHostAPI();
    }

    private static class SingletonHolder {
        private static final SiteBrowserHelper INSTANCE = new SiteBrowserHelper();
    }

    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SiteBrowserHelper getInstance() {

        return SiteBrowserHelper.SingletonHolder.INSTANCE;
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
            error(SiteBrowserHelper.class, e.getMessage(), e);
        }

        return checkArchived;
    } // checkArchived.

    public String getHostManagerUrl(final HttpServletRequest req, final List<Layout> userHasLayouts) {

        List<String> portletIds = null;

        for (Layout layout : userHasLayouts) {

            portletIds = layout.getPortletIds();
            for (String porletId : portletIds) {

                if (EXT_HOSTADMIN.equals(porletId)) {

                    return  new PortletURLImpl(req, porletId, layout.getId(), false).toString();
                }
            }
        }

        return null;
    } // getHostManagerUrl.

	/**
	 * Returns the list of sites that the given user has access to.
	 *
	 * @param showArchived
	 *            - Is set to {@code true}, archived sites will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param filter
	 *            - (Optional) If specified, returns the sites whose name starts
	 *            with the value of the {@code filter} variable.
	 * @return The list of sites that the given user has permissions to access.
	 * @throws DotDataException
	 *             An error occurred when retrieving the sites' data.
	 * @throws DotSecurityException
	 *             A system error occurred.
	 */
    public List<Host> getOrderedSites(final boolean showArchived, final User user, final String filter) throws DotDataException, DotSecurityException {
    	return getOrderedSites(showArchived, user, filter, Boolean.FALSE);
    }

	/**
	 * Returns the list of sites that the given user has access to.
	 *
	 * @param showArchived
	 *            - Is set to {@code true}, archived sites will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param filter
	 *            - (Optional) If specified, returns the sites whose name starts
	 *            with the value of the {@code filter} variable.
	 * @param respectFrontendRoles
	 *            -
	 * @return The list of sites that the given user has permissions to access.
	 * @throws DotDataException
	 *             An error occurred when retrieving the sites' data.
	 * @throws DotSecurityException
	 *             A system error occurred.
	 */
    public List<Host> getOrderedSites(final boolean showArchived, final User user, final String filter, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
    	final String sanitizedFilter = filter != null ? filter : StringUtils.EMPTY;
    	return this.hostAPI.findAll(user, respectFrontendRoles)
                .stream().sorted(HOST_NAME_COMPARATOR)
                .filter (site ->
                        !site.isSystemHost() && checkArchived(showArchived, site) &&
                                (site.getHostname().toLowerCase().startsWith(sanitizedFilter.toLowerCase())))
                .collect(Collectors.toList());
    }
    
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
        Optional<Host> siteOptional = this.hostAPI.findAll(user, Boolean.TRUE)
                .stream().filter(site -> !site.isSystemHost() && siteId.equals(site.getIdentifier()))
                .findFirst();

        return siteOptional.isPresent() ? siteOptional.get() : null;
    }

} // E:O:F:SiteBrowserHelper
