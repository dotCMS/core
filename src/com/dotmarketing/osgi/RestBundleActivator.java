package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.util.CollectionsUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Bundle for Rest Activation,
 * Expects a list of a Rest Resources class
 * @author jsanca
 */
public class RestBundleActivator extends BaseBundleActivator {

    private final List<Class> resources;

    public RestBundleActivator() {
        super();
        this.resources =
                CollectionsUtils.getNewList();
    }

    public RestBundleActivator(final List<Class> resources) {
        super();
        this.resources =
                resources;
    }

    protected RestBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.resources =
                CollectionsUtils.getNewList();
    }

    protected RestBundleActivator(final GenericBundleActivator bundleActivator, final List<Class> resources) {
        super(bundleActivator);
        this.resources =
                resources;
    }

    @Override
    public  void start(final BundleContext bundleContext) throws Exception {

        final List<Class> resources =
                this.getResources(bundleContext);

        if (null != resources) {

            for (Class aClass : resources) {

                RestServiceUtil.addResource(aClass);
            }
        }
    }

    @Override
    public  void stop(final BundleContext bundleContext) throws Exception {

        final List<Class> resources =
                this.getResources(bundleContext);

        if (null != resources) {

            for (Class aClass : resources) {

                RestServiceUtil.addResource(aClass);
            }
        }
    }

    /**
     * Returns the List of resources class.
     * @param bundleContext {@link BundleContext}
     * return List
     */
    protected List<Class> getResources (final BundleContext bundleContext) {

        return this.resources;
    }


} // E:O:F:RestBundleActivator.
