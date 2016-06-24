package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.directwebremoting.servlet.DwrServlet;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.service.http.NamespaceException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.util.CollectionsUtils;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.List;

/**
 * Dwr Publish Bundle Activator
 * @author jsanca
 */
public class DwrPublishBundleActivator extends PublishBundleActivator {

    private final List<DwrServletBean> dwrServlets;

    public DwrPublishBundleActivator() {
        this.dwrServlets =
                CollectionsUtils.getNewList();
    }

    public DwrPublishBundleActivator(final List<DwrServletBean> dwrServlets) {
        this.dwrServlets = dwrServlets;
    }

    protected DwrPublishBundleActivator(GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.dwrServlets =
                CollectionsUtils.getNewList();
    }

    protected DwrPublishBundleActivator(GenericBundleActivator bundleActivator, List<DwrServletBean> dwrServlets) {
        super(bundleActivator);
        this.dwrServlets = dwrServlets;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<DwrServletBean> servlets =
                this.getDwrServlets();

        if (null != servlets) {

            final ExtHttpService extHttpService =
                    this.getExtHttpService(bundleContext);

            if (null != extHttpService) {

                this.publishBundleServices(bundleContext);

                this.addDwrServlets(servlets, extHttpService);
            }
        }
    }

    private void addDwrServlets(final List<DwrServletBean> servlets,
                                final ExtHttpService extHttpService) throws ServletException, NamespaceException {

        for (DwrServletBean servletBean : servlets) {

            if (null != servletBean) {
                if (servletBean.getServlet() instanceof DwrServlet) {

                    extHttpService.registerServlet(servletBean.getAlias(),
                            servletBean.getServlet(),
                            servletBean.getInitParams(),
                            servletBean.getHttpContext()
                            );

                        CMSFilter.addExclude(servletBean.getExcludePath());
                } else {

                    if (Logger.isWarnEnabled(DwrPublishBundleActivator.class)) {

                        Logger.warn(DwrPublishBundleActivator.class,
                                "The servlet should be instance of DwrServlet, but it is : "
                                        + servletBean.getServlet().getClass().getName());
                    }
                }
            }
        }
    }

    public List<DwrServletBean> getDwrServlets() {
        return dwrServlets;
    }

} // E:O:F:DwrPublishBundleActivator.
