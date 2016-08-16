package com.dotcms.rest.api.v1.site;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Layout;
import com.liferay.portlet.PortletURLImpl;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;

import static com.dotmarketing.util.Logger.error;

/**
 * Just a helper {@link SiteBrowserResource}
 * @author jsanca
 */
public class SiteBrowserHelper implements Serializable {

    public static final String EXT_HOSTADMIN = "EXT_HOSTADMIN";

    /**
     * Check if a Host is archived or not, keeping the exception quietly
     * @param showArchived {@link Boolean}
     * @param host {@link Host}
     * @return Boolean
     */
    public static boolean checkArchived (final boolean showArchived, final Host host) {

        boolean checkArchived = false;
        try {

            checkArchived = (showArchived || !host.isArchived());
        } catch (Exception e) {
            error(SiteBrowserHelper.class, e.getMessage(), e);
        }

        return checkArchived;
    } // checkArchived.

    public static String getHostManagerUrl(final HttpServletRequest req, final List<Layout> userHasLayouts) {

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

} // E:O:F:SiteBrowserHelper
