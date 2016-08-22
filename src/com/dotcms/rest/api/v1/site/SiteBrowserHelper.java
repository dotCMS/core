package com.dotcms.rest.api.v1.site;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotmarketing.util.Logger.error;

/**
 * Just a helper {@link SiteBrowserResource}
 * @author jsanca
 */
public class SiteBrowserHelper implements Serializable {

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
     * Check if a Host is archived or not, keeping the exception quietly
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

    public List<Host> getOrderedHost(@PathParam("archived") boolean showArchived, User user, String filter) throws DotDataException, DotSecurityException {
        return this.hostAPI.findAll(user, Boolean.TRUE)
                .stream().sorted(HOST_NAME_COMPARATOR)
                .filter (host ->
                        !host.isSystemHost() && checkArchived(showArchived, host) &&
                                (host.getHostname().toLowerCase().startsWith(filter.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Host getHost(User user, String hostId) throws DotSecurityException, DotDataException {
        Optional<Host> hostOptional = this.hostAPI.findAll(user, Boolean.TRUE)
                .stream().filter(host -> !host.isSystemHost() && hostId.equals(host.getIdentifier()))
                .findFirst();

        return hostOptional.isPresent() ? hostOptional.get() : null;
    }

} // E:O:F:SiteBrowserHelper
