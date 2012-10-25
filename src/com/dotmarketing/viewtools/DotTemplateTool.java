package com.dotmarketing.viewtools;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class DotTemplateTool implements ViewTool {

    private static HttpServletRequest request;
    Context ctx;

    /**
     * @param initData the ViewContext that is automatically passed on view tool initialization, either in the request or the application
     * @return
     * @see ViewTool, ViewContext
     */
    public void init ( Object initData ) {
        ViewContext context = (ViewContext) initData;
        request = context.getRequest();
        ctx = context.getVelocityContext();
    }

    /**
     * Given a theme id we will parse it and return the Layout for the given template
     *
     * @param themeId
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    public static TemplateLayout themeLayout ( String themeId ) throws DotDataException, DotSecurityException {

        Identifier ident = APILocator.getIdentifierAPI().findFromInode( themeId );
        WebAsset template = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion( ident, APILocator.getUserAPI().getSystemUser(), false );
        if ( !template.getInode().equals( themeId ) ) {
            template = (WebAsset) InodeFactory.getInode( themeId, Template.class );
        }

        //Parse and return the layout for this template
        TemplateLayout parameters = DesignTemplateUtil.getDesignParameters( ((Template) template).getDrawedBody() );
        return parameters;
    }

}