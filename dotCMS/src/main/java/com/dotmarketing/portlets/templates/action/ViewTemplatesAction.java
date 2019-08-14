package com.dotmarketing.portlets.templates.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import java.util.List;
import javax.servlet.jsp.PageContext;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author Maria Ahues
 * @version $Revision: 1.3 $
 */
public class ViewTemplatesAction extends DotPortletAction {

    public ActionForward render (
            ActionMapping mapping, ActionForm form, PortletConfig config,
            RenderRequest req, RenderResponse res )
            throws Exception {

        Logger.debug( this, "Running ViewTemplatesAction!!!!" );

        try {
            //get the user
            User user = _getUser( req );

            //Checking if the user can add templates to a host
            PermissionAPI perAPI = APILocator.getPermissionAPI();
            List<Host> hosts = APILocator.getHostAPI().findAll( user, false );
            hosts.remove( APILocator.getHostAPI().findSystemHost( user, false ) );
            hosts = perAPI.filterCollection( hosts, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, false, user );

            if ( hosts.size() == 0 ) {
                req.setAttribute( WebKeys.TEMPLATE_CAN_ADD, false );
            } else {
                req.setAttribute( WebKeys.TEMPLATE_CAN_ADD, true );
            }

            req.setAttribute( WebKeys.TEMPLATE_CAN_DESIGN, perAPI.doesUserHavePermissions( WebAPILocator.getHostWebAPI().getCurrentHost( req ), "TEMPLATE_LAYOUTS:" + PermissionAPI.PERMISSION_EDIT, user ) );

            _viewWebAssets( req, user, Template.class, "template", WebKeys.TEMPLATES_VIEW_COUNT, WebKeys.TEMPLATES_VIEW, WebKeys.TEMPLATE_QUERY, WebKeys.TEMPLATE_SHOW_DELETED, WebKeys.TEMPLATE_HOST_CHANGED );

            return mapping.findForward( "portlet.ext.templates.view_templates" );
        } catch ( Exception e ) {
            req.setAttribute( PageContext.EXCEPTION, e );
            return mapping.findForward( Constants.COMMON_ERROR );
        }
    }

}