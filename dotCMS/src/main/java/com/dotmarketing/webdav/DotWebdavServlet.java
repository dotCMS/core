package com.dotmarketing.webdav;

import com.bradmcevoy.http.MiltonServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * The WebDAV servlet, with dotCMS's own property permission check installed.
 *
 * <p>The library builds its handlers during {@code init}, and the only way to reach them afterwards
 * is through the manager it leaves behind. Nothing else here differs from the servlet this extends;
 * the resource factory and everything else still comes from the init parameters in {@code web.xml}.
 *
 * @see WebdavPropertyAuthoriser
 */
public class DotWebdavServlet extends MiltonServlet {

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        // Reaches every handler that checks property permissions, which is PROPPATCH and PROPFIND.
        httpManager.setPropertyPermissionService(new WebdavPropertyAuthoriser());
    }
}
