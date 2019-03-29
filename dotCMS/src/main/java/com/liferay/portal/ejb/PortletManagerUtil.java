/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.ejb;

import java.util.Collection;

import com.dotcms.business.CloseDBIfOpened;
import com.liferay.portal.model.Portlet;

/**
 * <a href="PortletManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.78 $
 *
 */
public class PortletManagerUtil {

    public static Collection addPortlets(final java.lang.String[] xmls) throws com.liferay.portal.SystemException {
        try {
            final PortletManager portletManager = PortletManagerFactory.getManager();

            return portletManager.addPortlets(xmls).values();
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }

    public static com.liferay.portal.model.Portlet getPortletById(final java.lang.String companyId, final java.lang.String portletId)
            throws com.liferay.portal.SystemException {
        try {
            final PortletManager portletManager = PortletManagerFactory.getManager();

            return portletManager.getPortletById( portletId);
        } catch (final com.liferay.portal.SystemException se) {
            throw se;
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }

    public static com.liferay.portal.model.Portlet getPortletByStrutsPath(final java.lang.String companyId,
            final java.lang.String strutsPath) throws com.liferay.portal.SystemException {
        try {
            final PortletManager portletManager = PortletManagerFactory.getManager();

            return portletManager.getPortletByStrutsPath( strutsPath);
        } catch (final com.liferay.portal.SystemException se) {
            throw se;
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }

    @CloseDBIfOpened
    public static java.util.Collection<Portlet> getPortlets(final java.lang.String companyId) throws com.liferay.portal.SystemException {
        try {
            final PortletManager portletManager = PortletManagerFactory.getManager();

            return portletManager.getPortlets();
        } catch (final com.liferay.portal.SystemException se) {
            throw se;
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }

    public static com.liferay.portal.model.Portlet updatePortlet(final java.lang.String portletId, final java.lang.String groupId,
            final java.lang.String defaultPreferences, final boolean narrow, final java.lang.String roles, final boolean active)
            throws com.liferay.portal.PortalException, com.liferay.portal.SystemException {
        try {
            final PortletManager portletManager = PortletManagerFactory.getManager();

            return portletManager.updatePortlet(portletId, groupId, defaultPreferences, narrow, roles, active);
        } catch (final com.liferay.portal.PortalException pe) {
            throw pe;
        } catch (final com.liferay.portal.SystemException se) {
            throw se;
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }
}
