package com.dotmarketing.portlets.templates.design.servlets;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Servlet implementation class PreviewDesignTemplate
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *         <p/>
 *         May 7, 2012 - 12:22:31 PM
 */
public class PreviewDesignTemplateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        doPost( request, response );
    }

    protected void doPost ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        try {

            String bodyTemplate = request.getParameter( "bodyTemplateHTML" );
            if ( bodyTemplate == null ) return;

            request.setAttribute( "designedBody", bodyTemplate );

            String theme = request.getParameter( "theme" );
            String themeName = request.getParameter( "themeName" );
            String hostId = request.getParameter( "hostId" );
            /*
            String headerCheck = request.getParameter("headerCheck");
            String footerCheck = request.getParameter("footerCheck");
			boolean isHeader = UtilMethods.isSet(headerCheck) && headerCheck.equals("true");
			boolean isFooter = UtilMethods.isSet(footerCheck) && footerCheck.equals("true");
			*/

            UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
            User user = uWebAPI.getLoggedInUser( request );

            String themeHostId = APILocator.getFolderAPI().find( theme, user, false ).getHostId();
            String themePath;
            if ( themeHostId.equals( hostId ) ) {
                themePath = Template.THEMES_PATH + themeName + "/";
            } else {
                Host themeHost = APILocator.getHostAPI().find( themeHostId, user, false );
                themePath = "//" + themeHost.getHostname() + Template.THEMES_PATH + themeName + "/";
            }

            /*
            // get the imported files
            List<PreviewFileAsset> savedFiles = new ArrayList<PreviewFileAsset>();

            // get the preview body with the imported files (js and/or css)
            StringBuffer endBody = DesignTemplateUtil.getPreviewBody(bodyTemplate,savedFiles, themePath, isHeader, isFooter );

            // get the container's list
            List<Container> containers = PreviewTemplateUtil.getContainers(endBody);

            //replace the text
            for(Container c : containers){
                String identifier = c.getIdentifier();
                // if the container isn't into the header or into the footer than we insert the mock content...
                if(c.getMaxContentlets()>0)
                    endBody = new StringBuffer(endBody.toString().replace("#parseContainer('"+identifier+"')", PreviewTemplateUtil.getMockBodyContent()));
                else //...else the container's code
                    endBody = new StringBuffer(endBody.toString().replace("#parseContainer('"+identifier+"')", c.getCode()));
            }
            */

            StringBuilder sb = new StringBuilder();
            //Setting some theme variables
            sb.append( "#set ($dotTheme = $templatetool.themeByPath(\"" ).append( themePath ).append( "\",\"" ).append( hostId ).append( "\"))" );
            sb.append( "#set ($dotThemeLayout = $templatetool.themeLayout(\"" ).append( "null" ).append( "\"," ).append( true ).append( "))" );
            //Merging our template
            sb.append( "$velutil.mergeTemplate(\"$dotTheme.templatePath\")" );

            // parse
            StringWriter parsedBody = new StringWriter();
            org.apache.velocity.context.Context context = VelocityUtil.getWebContext( request, response );
            VelocityUtil.getEngine().evaluate( context, parsedBody, "", sb.toString() );

            PrintWriter out = response.getWriter();
            out.print( parsedBody );

        } catch ( DotDataException e ) {
            e.printStackTrace();
        } catch ( DotSecurityException e ) {
            e.printStackTrace();
        } catch ( DotRuntimeException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( PortalException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( SystemException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}