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

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.portal.PortletFactory;
import com.liferay.portal.model.Portlet;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <a href="PortletManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.78 $
 *
 */
public class PortletManagerUtil {

    @WrapInTransaction
    public static Collection addPortlets(final InputStream[] xmls) throws com.liferay.portal.SystemException {
        try {
            final PortletFactory portletFactory = PortletManagerFactory.getManager();

            Collection<Portlet> portlets = new ArrayList<>();

            final Map<String,Portlet> foundPortlets = portletFactory.xmlToPortlets(xmls);
            for(Portlet portlet : foundPortlets.values()) {
              portlets.add(portletFactory.insertPortlet(portlet));
            }

            return portlets;
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }
    @CloseDBIfOpened
    public static com.liferay.portal.model.Portlet getPortletById(final java.lang.String companyId, final java.lang.String portletId)
            throws com.liferay.portal.SystemException {
        try {
            final PortletFactory portletFactory = PortletManagerFactory.getManager();

            return portletFactory.findById(portletId);
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }



    @CloseDBIfOpened
    public static java.util.Collection<Portlet> getPortlets(final java.lang.String companyId) throws com.liferay.portal.SystemException {
        try {
            final PortletFactory portletFactory = PortletManagerFactory.getManager();

            return portletFactory.getPortlets();
        } catch (final Exception e) {
            throw new com.liferay.portal.SystemException(e);
        }
    }


}
