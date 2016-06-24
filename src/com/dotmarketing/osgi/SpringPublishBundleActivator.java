package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.service.http.NamespaceException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.util.CollectionsUtils;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import java.util.List;

/**
 * Spring Publish Bundle Activator
 * @author jsanca
 */
public class SpringPublishBundleActivator extends PublishBundleActivator {

    private final List<SpringServletBean> springServlets;

    public SpringPublishBundleActivator() {
        this.springServlets =
                CollectionsUtils.getNewList();
    }

    public SpringPublishBundleActivator(final List<SpringServletBean> springServlets) {
        this.springServlets = springServlets;
    }

    protected SpringPublishBundleActivator(GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.springServlets =
                CollectionsUtils.getNewList();
    }

    protected SpringPublishBundleActivator(GenericBundleActivator bundleActivator, List<SpringServletBean> springServlets) {
        super(bundleActivator);
        this.springServlets = springServlets;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<SpringServletBean> servlets =
                this.getSpringServlets();

        if (null != servlets) {

            final ExtHttpService extHttpService =
                    this.getExtHttpService(bundleContext);

            if (null != extHttpService) {

                this.publishBundleServices(bundleContext);

                this.addSpringServlets(servlets, extHttpService);
            }
        }
    }

    private void addSpringServlets(final List<SpringServletBean> servlets,
                                final ExtHttpService extHttpService) throws ServletException, NamespaceException {

        for (SpringServletBean servletBean : servlets) {

            if (null != servletBean) {

                final DispatcherServlet dispatcherServlet = new DispatcherServlet();
                dispatcherServlet.setContextConfigLocation( servletBean.getConfigLocation() );

                extHttpService.registerServlet(servletBean.getAlias(),
                        dispatcherServlet,
                        servletBean.getInitParams(),
                        servletBean.getHttpContext()
                        );

                CMSFilter.addExclude(servletBean.getExcludePath());
            }
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        final List<SpringServletBean> servlets =
                this.getSpringServlets();

        if (null != servlets) {

            for (SpringServletBean servletBean : servlets) {

                if (null != servletBean) {

                    CMSFilter.removeExclude(servletBean.getExcludePath());
                }
            }
        }


        super.stop(bundleContext);
    }

    public List<SpringServletBean> getSpringServlets() {
        return springServlets;
    }
} // E:O:F:SpringPublishBundleActivator.
