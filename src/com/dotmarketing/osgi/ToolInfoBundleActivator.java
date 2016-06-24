package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.util.CollectionsUtils;
import org.apache.velocity.tools.view.ToolInfo;

import java.util.List;

/**
 * View Tool Info for Actions activation.
 * @author jsanca
 */
public class ToolInfoBundleActivator extends BaseBundleActivator {

    private final List<ToolInfo> tools;

    public ToolInfoBundleActivator() {

        super();
        this.tools =
                CollectionsUtils.getNewList();
    }

    public ToolInfoBundleActivator(final List<ToolInfo> tools) {

        super();
        this.tools = tools;
    }

    // Only for friends
    protected ToolInfoBundleActivator(final GenericBundleActivator bundleActivator) {

        super(bundleActivator);
        this.tools =
                CollectionsUtils.getNewList();
    }

    // Only for friends
    protected ToolInfoBundleActivator(final GenericBundleActivator bundleActivator, final List<ToolInfo> tools) {

        super(bundleActivator);
        this.tools = tools;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<ToolInfo> tools =
                this.getTools();

        if (null != tools) {

            for (ToolInfo toolInfo : tools) {

                if (null != toolInfo) {

                    this.registerViewToolService(bundleContext,
                            toolInfo);
                }
            }
        }
    }

    /**
     * Register a ViewTool service using a ToolInfo object
     *
     * @param context
     * @param info
     */
    @SuppressWarnings ("unchecked")
    protected void registerViewToolService ( final BundleContext context,
                                             final ToolInfo info ) {

        this.getBundleActivator().registerViewToolService
                (context, info);
    }

    protected List<ToolInfo> getTools () {

        return this.tools;
    }

} // E:O:F:PortletActionBundleActivator.
