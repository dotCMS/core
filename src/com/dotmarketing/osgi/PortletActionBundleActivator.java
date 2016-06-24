package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.struts.action.Action;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.config.ForwardConfig;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.util.CollectionsUtils;
import com.liferay.portal.model.Portlet;

import java.util.Collection;
import java.util.List;

/**
 * Bundle for Actions activation.
 * @author jsanca
 */
public class PortletActionBundleActivator extends BaseBundleActivator {

    private final List<PortletActionBean> portletActions;

    public PortletActionBundleActivator() {

        super();
        this.portletActions = CollectionsUtils.getNewList();
    }

    public PortletActionBundleActivator(final List<PortletActionBean> portletActions) {

        super();
        this.portletActions = portletActions;
    }

    // Only for friends
    protected PortletActionBundleActivator(final GenericBundleActivator bundleActivator) {

        super(bundleActivator);
        this.portletActions = CollectionsUtils.getNewList();
    }

    // Only for friends
    protected PortletActionBundleActivator(final GenericBundleActivator bundleActivator, final List<PortletActionBean> portletActions) {

        super(bundleActivator);
        this.portletActions = portletActions;
    }

    /**
     * Get the list of Portlets
     * @return List
     */
    protected List<PortletActionBean> getPortletActions() {

        return this.portletActions; // you can override me
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<PortletActionBean> portletActions =
                this.getPortletActions ();

        if (null != portletActions) {

            for (PortletActionBean portletAction : portletActions) {

                if (null != portletAction) {

                    this.addPortletAction(portletAction, bundleContext);
                }
            }
        }
    }

    /**
     * Adds a new portlet action to the configuration
     * @param portletAction
     */
    protected void addPortletAction(final PortletActionBean portletAction,
                                    final BundleContext context) throws Exception {

        final ActionMapping actionMapping = new ActionMapping();

        //Configure the instance
        actionMapping.setPath( portletAction.getPath() );
        actionMapping.setType( portletAction.getType() );

        //Create and register the forwards for this mapping
        if (null != portletAction.getActionForwards()) {

            for (ActionForwardBean actionForward : portletAction.getActionForwards()) {

                if (null != actionForward) {

                    this.registerActionForward(context, actionMapping,
                            actionForward.getName(),
                            actionForward.getPath(),
                            actionForward.isRedirect());
                }
            }
        }

        //And finally register the ActionMapping
        this.registerActionMapping( actionMapping );

        //************************************************************
        //*******************REGISTER THE PORTLETS********************
        //************************************************************
        //Register our portlets
        registerPortlets( context, portletAction.getPortletXmls() );
    }

    /**
     * Method that will create and add an ActionForward to a ActionMapping, this call is mandatory for the creation of ActionForwards
     * because extra logic will be required for jsp forwards to work.
     *
     * @param actionMapping
     * @param name
     * @param path
     * @param redirect
     * @return
     * @throws Exception
     */
    protected ForwardConfig registerActionForward (BundleContext context, ActionMapping actionMapping, String name, String path, Boolean redirect ) throws Exception {

        return this.getBundleActivator().registerActionForward(context, actionMapping, name, path, redirect);
    }

    /**
     * Register a given ActionMapping
     *
     * @param actionMapping
     * @throws Exception
     */
    protected final void registerActionMapping ( final ActionMapping actionMapping ) throws Exception {

        this.getBundleActivator().registerActionMapping(actionMapping);
    } // registerActionMapping.

    /**
     * Register the portlets on the given configuration files
     *
     * @param xmls
     * @throws Exception
     */
    @SuppressWarnings ("unchecked")
    protected final Collection<Portlet> registerPortlets (final BundleContext context,
                                                    final String[] xmls ) throws Exception {

        return this.getBundleActivator().registerPortlets(context, xmls);
    } // registerPortlets
} // E:O:F:PortletActionBundleActivator.
