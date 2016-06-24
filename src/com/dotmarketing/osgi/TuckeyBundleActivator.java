package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.tuckey.web.filters.urlrewrite.Rule;
import com.dotmarketing.util.CollectionsUtils;
import com.dotmarketing.util.ConversionUtils;
import com.dotmarketing.util.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * Bundle for Tuckey activation.
 * @author jsanca
 */
public class TuckeyBundleActivator extends ServletBundleActivator {

    protected final List<TuckeyBean> tuckeys;

    private ConversionUtils conversionUtils = ConversionUtils.INSTANCE;

    private Converter<TuckeyBean, ServletBean> converter = new Converter<TuckeyBean, ServletBean>() {
        @Override
        public ServletBean convert(final TuckeyBean tuckeyBean) {

            return (ServletBean)tuckeyBean;
        }
    };

    public TuckeyBundleActivator() {
        super();
        this.tuckeys =
                CollectionsUtils.getNewList();
    }

    public TuckeyBundleActivator(final List<TuckeyBean> tuckeys) {
        super();
        this.tuckeys =
                tuckeys;
    }

    protected TuckeyBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.tuckeys =
                CollectionsUtils.getNewList();
    }

    protected TuckeyBundleActivator(final GenericBundleActivator bundleActivator,
                                    final List<TuckeyBean> tuckeys) {
        super(bundleActivator);
        this.tuckeys =
                tuckeys;
    }

    @Override
    public  void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<TuckeyBean>  tuckeys =
                this.getTuckeys(bundleContext);

        if (null != tuckeys) {

            for (TuckeyBean tuckeyBean : tuckeys) {

                this.addRules(tuckeyBean);
            }
        }
    }

    protected void addRules(final TuckeyBean tuckeyBean) throws Exception {

        for (Rule rule : tuckeyBean.getRules()) {

            if (null != rule) {

                this.getBundleActivator().addRewriteRule(rule);
            }
        }
    }

    @Override
    public  void stop(final BundleContext bundleContext) throws Exception {

        super.stop(bundleContext);
    }

    @Override
    protected List<ServletBean> getServlets(BundleContext bundleContext) {

        return this.conversionUtils.convert(this.getTuckeys(bundleContext),
                this.converter);
    }

    protected List<TuckeyBean> getTuckeys(final BundleContext bundleContext) {

        return this.tuckeys;
    }


} // E:O:F:PortletActionBundleActivator.
