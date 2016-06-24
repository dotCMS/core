package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;
import com.dotmarketing.util.CollectionsUtils;

import java.util.List;

/**
 * Bundle for Servet activation.
 * @author jsanca
 */
public class ServletBundleActivator extends BaseBundleActivator  {

    private ExtHttpService extHttpService;
    private final List<ServletBean>  servlets;

    public ServletBundleActivator() {
        super();
        this.servlets =
                CollectionsUtils.getNewList();
    }

    public ServletBundleActivator(final List<ServletBean>  servlets) {
        super();
        this.servlets =
                servlets;
    }

    protected ServletBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.servlets =
                CollectionsUtils.getNewList();
    }

    protected ServletBundleActivator(final GenericBundleActivator bundleActivator, final List<ServletBean>  servlets) {
        super(bundleActivator);
        this.servlets =
                servlets;
    }


    protected final ExtHttpService getExtHttpService (final BundleContext bundleContext) {

        final ServiceReference serviceReference =
                bundleContext.getServiceReference ( ExtHttpService.class.getName() );

        return (null != serviceReference)?
                (ExtHttpService)bundleContext.getService(serviceReference):
                null;
    }


    @Override
    public void start(BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<ServletBean> servlets =
                this.getServlets(bundleContext);

        if (null != servlets) {

            this.extHttpService =
                    this.getExtHttpService(bundleContext);

            for (ServletBean servletBean : servlets) {

                this.extHttpService.registerServlet
                        (servletBean.getAlias(),
                                servletBean.getServlet(),
                                servletBean.getInitParams(),
                                servletBean.getHttpContext());
            }
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        final List<ServletBean> servlets =
                this.getServlets(bundleContext);

        if (null != this.extHttpService
                && null !=  servlets) {

            for (ServletBean servletBean : servlets) {

                this.extHttpService.unregisterServlet(servletBean.getServlet());
            }
        }

        super.stop(bundleContext);
    }

    /**
     * Get the servlet list config
     * @param bundleContext {@link BundleContext}
     * @return List
     */
    protected List<ServletBean> getServlets (final BundleContext bundleContext) {

        return this.servlets;
    }

} // E:O:F:PublishBundleActivator.
