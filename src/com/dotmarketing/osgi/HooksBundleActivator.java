package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHook;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook;
import com.dotmarketing.util.CollectionsUtils;

import java.util.List;

/**
 * Bundle for Hooks activation.
 * @author jsanca
 */
public class HooksBundleActivator extends BaseBundleActivator {

    private final List<HooksBean> hooks;

    public HooksBundleActivator() {
        super();
        this.hooks =
                CollectionsUtils.getNewList();
    }

    public HooksBundleActivator(final List<HooksBean> hooks) {
        super();
        this.hooks =
                hooks;
    }

    protected HooksBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.hooks =
                CollectionsUtils.getNewList();
    }

    protected HooksBundleActivator(final GenericBundleActivator bundleActivator,
                                   final List<HooksBean> hooks) {
        super(bundleActivator);
        this.hooks =
                hooks;
    }

    /**
     * Adds a hook to the end of the chain
     *
     * @param preHook
     * @throws Exception
     */
    protected final void addPreHook ( final ContentletAPIPreHook preHook ) throws Exception {

        this.getBundleActivator().addPreHook(preHook);
    } // addPreHook

    /**
     * Adds a hook to the end of the chain
     *
     * @param postHook
     * @throws Exception
     */
    protected final void addPostHook ( final ContentletAPIPostHook postHook ) throws Exception {

        this.getBundleActivator().addPostHook(postHook);
    } // addPostHook

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<HooksBean> hooks =
                this.getHooks();

        if (null != hooks) {

            for (HooksBean hooksBean : hooks) {

                if (null != hooksBean) {

                    if (null != hooksBean.getApiPreHook()) {

                        this.addPreHook(hooksBean.getApiPreHook());
                    }

                    if (null != hooksBean.getApiPostHook()) {

                        this.addPostHook(hooksBean.getApiPostHook());
                    }
                }
            }
        }
    }

    protected List<HooksBean> getHooks() {
        return hooks;
    }
} // E:O:F:PublishBundleActivator.
