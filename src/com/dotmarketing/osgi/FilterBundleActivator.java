package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.util.CollectionsUtils;

import java.util.List;

/**
 * Bundle for Filter activation.
 * @author jsanca
 */
public class FilterBundleActivator extends BaseBundleActivator  {

    private final List<FilterBean>  filters;

    private ExtHttpService extHttpService;

    public FilterBundleActivator() {
        super();
        this.filters =
                CollectionsUtils.getNewList();
    }

    public FilterBundleActivator(final List<FilterBean>  filters) {
        super();
        this.filters =
                filters;

    }

    protected FilterBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.filters =
                CollectionsUtils.getNewList();
    }

    protected FilterBundleActivator(final GenericBundleActivator bundleActivator, final List<FilterBean>  filters) {
        super(bundleActivator);
        this.filters =
                filters;
    }


    protected final ExtHttpService getExtHttpService (final BundleContext bundleContext) {

        final ServiceReference serviceReference =
                bundleContext.getServiceReference ( ExtHttpService.class.getName() );

        return (null != serviceReference)?
                (ExtHttpService)bundleContext.getService(serviceReference):
                null;
    }


    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<FilterBean> filters =
                this.getFilters(bundleContext);

        if (null != this.filters) {

            this.extHttpService =
                    this.getExtHttpService(bundleContext);

            for (FilterBean filterBean : this.filters) {

                this.extHttpService.registerFilter
                        (filterBean.getFilter(),
                                filterBean.getPattern(),
                                filterBean.getInitParams(),
                                filterBean.getRanking(),
                                filterBean.getHttpContext());

                CMSFilter.addExclude(filterBean.getExcludePath());
            }
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {

        final List<FilterBean> filters =
                this.getFilters(bundleContext);

        if (null != filters) {

            for (FilterBean filterBean : filters) {

                // todo: ask Jonathan which one to used
                CMSFilter.removeExclude
                        (filterBean.getExcludePath());
            }
        }

        super.stop(bundleContext);
    }

    /**
     * Returns the filters configuration
     * @param bundleContext BundleContext
     * @return List
     */
    protected  List<FilterBean> getFilters (final BundleContext bundleContext) {

        return this.filters;
    }


} // E:O:F:PublishBundleActivator.
