package com.dotcms.rendering.velocity.servlet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rendering.velocity.viewtools.RequestWrapper;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.visitor.business.VisitorAPI;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class VelocityPreviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;


    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();



    private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

    private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
        permissionAPI = permissionAPIRef;
    }

    private String CHARSET = "UTF-8";



    public static final String VELOCITY_CONTEXT = "velocityContext";


    @CloseDBIfOpened
    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        final String uri = URLDecoder.decode((req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE) != null)
                ? (String) req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE)
                : req.getRequestURI(), "UTF-8");



        RequestWrapper request = new RequestWrapper(req);
        request.setRequestUri(uri);


        Host host = hostWebAPI.getCurrentHostNoThrow(request);

        PageMode mode = PageMode.get(request);
        // Checking if host is active
        boolean hostlive;


        try {
            hostlive = APILocator.getVersionableAPI()
                .hasLiveVersion(host);
        } catch (Exception e1) {
            UtilMethods.closeDbSilently();
            throw new ServletException(e1);
        }
        if (!hostlive) {
            try {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        LanguageUtil.get("server-unavailable-error-message"));
            } catch (LanguageException e) {
                Logger.error(CMSFilter.class, e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return;
        }


        if (DbConnectionFactory.isMsSql() && LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;
        }

        if (DbConnectionFactory.isOracle() && LicenseUtil.getLevel() < LicenseLevel.PRIME.level) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;
        }
        if (!LicenseUtil.isASAllowed()) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;

        }

        try {


            if (uri == null) {
                response.sendError(500, "VelocityServlet called without running through the CMS Filter");
                Logger.error(this.getClass(),
                        "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                                + Constants.CMS_FILTER_URI_OVERRIDE);
                return;
            }



            // we will always need a visitor in admin mode
            if (mode.isAdmin) {
                visitorAPI.getVisitor(request, true);
            }


            switch (mode) {
                case PREVIEW_MODE:
                    Logger.debug(VelocityPreviewServlet.class, "VELOCITY SERVLET I'M ON PREVIEW MODE!!!");
                    doPreviewMode(request, response);
                    break;
                case EDIT_MODE:
                    Logger.debug(VelocityPreviewServlet.class, "VELOCITY SERVLET I'M ON EDIT MODE!!!");
                    doEditMode(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
            }



        } catch (ResourceNotFoundException rnfe) {
            Logger.error(this, "ResourceNotFoundException" + rnfe.toString(), rnfe);
            response.sendError(404);
            return;
        } catch (ParseErrorException pee) {
            Logger.error(this, "Template Parse Exception : " + pee.toString(), pee);
            response.sendError(500, "Template Parse Exception");
        } catch (MethodInvocationException mie) {
            Logger.error(this, "MethodInvocationException" + mie.toString(), mie);
            response.sendError(500, "MethodInvocationException Error on template");
        } catch (Exception e) {
            Logger.error(this, "Exception" + e.toString(), e);
            response.sendError(500, "Exception Error on template");

        } finally {
            DbConnectionFactory.closeSilently();
        }

    }

    public void init(ServletConfig config) throws ServletException {



    }

    public void doPreviewMode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = URLDecoder.decode(request.getRequestURI(), UtilMethods.getCharsetConfiguration());
        PageMode mode = PageMode.PREVIEW_MODE;

        Host host = hostWebAPI.getCurrentHost(request);


        // Getting the user to check the permissions
        User user = com.liferay.portal.util.PortalUtil.getUser(request);

        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI()
            .find(host, uri);
        request.setAttribute("idInode", id.getId());
        Logger.debug(VelocityPreviewServlet.class, "VELOCITY HTML INODE=" + id.getId());

        Template template = null;
        Template hostVariablesTemplate = null;

        // creates the context where to place the variables
        response.setContentType(CHARSET);
        Context context = VelocityUtil.getWebContext(request, response);

        IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, context);
        if ("contentlet".equals(htmlPage.getType())) {
            context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, true, host, context));
        }



        com.dotmarketing.portlets.templates.model.Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, true);



        VelocityUtil.makeBackendContext(context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, mode, host);



        PageContextBuilder builder = new PageContextBuilder(htmlPage, user, PageMode.PREVIEW_MODE);
        builder.addAll(context);



        hostVariablesTemplate = VelocityUtil.getEngine()
            .getTemplate(PageMode.PREVIEW_MODE.name() + File.separator + host.getIdentifier() + "." + VelocityType.SITE.fileExtension);

        if (cmsTemplate.isDrawed()) {// We have a designed template
            // Setting some theme variables
            Map<String, Object> dotThemeData = DotTemplateTool.theme(cmsTemplate.getTheme(), host.getIdentifier());
            context.put("dotTheme", dotThemeData);
            context.put("dotThemeLayout", DotTemplateTool.themeLayout(cmsTemplate.getInode()));
            // Our designed template
            template = VelocityUtil.getEngine()
                .getTemplate((String) dotThemeData.get("templatePath"));
        } else {
            template = VelocityUtil.getEngine()
                .getTemplate(PageMode.PREVIEW_MODE.name() + File.separator + cmsTemplate.getIdentifier() + "."
                        + VelocityType.TEMPLATE.fileExtension);
        }



        PrintWriter out = response.getWriter();
        request.setAttribute("velocityContext", context);
        try {

            if (builder.getWidgetPreExecute() != null) {
                VelocityUtil.getEngine()
                    .evaluate(context, out, "", builder.getWidgetPreExecute());
            }
            if (hostVariablesTemplate != null)
                hostVariablesTemplate.merge(context, out);
            template.merge(context, out);

        } catch (ParseErrorException e) {
            out.append(e.getMessage());
        }
    }


    protected void doEditMode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PageMode mode = PageMode.PREVIEW_MODE;
        String uri = request.getRequestURI();

        Host host = hostWebAPI.getCurrentHost(request);


        // Getting the user to check the permissions
        User user = com.liferay.portal.util.PortalUtil.getUser(request);


        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI()
            .find(host, uri);
        request.setAttribute("idInode", String.valueOf(id.getId()));
        Logger.debug(VelocityPreviewServlet.class, "VELOCITY HTML INODE=" + id.getId());

        Template template;
        Template hostVariablesTemplate = null;

        // creates the context where to place the variables
        response.setContentType(CHARSET);
        request.setAttribute("EDIT_MODE", Boolean.TRUE);
        Context context = VelocityUtil.getWebContext(request, response);

        IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, context);

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));


        com.dotmarketing.portlets.templates.model.Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, true);

        if (cmsTemplate == null) {// DOTCMS-4051
            cmsTemplate = new com.dotmarketing.portlets.templates.model.Template();
            Logger.debug(VelocityPreviewServlet.class, "HTMLPAGE TEMPLATE NOT FOUND");
        }

        Identifier templateIdentifier = APILocator.getIdentifierAPI()
            .find(cmsTemplate);

        Logger.debug(VelocityPreviewServlet.class, "VELOCITY TEMPLATE INODE=" + cmsTemplate.getInode());

        VelocityUtil.makeBackendContext(context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, mode, host);


        PageContextBuilder builder = new PageContextBuilder(htmlPage, user, PageMode.PREVIEW_MODE);
        builder.addAll(context);

        hostVariablesTemplate = VelocityUtil.getEngine()
            .getTemplate(PageMode.EDIT_MODE.name() + File.separator + host.getIdentifier() + "." + VelocityType.SITE.fileExtension);

        if (cmsTemplate.isDrawed()) {// We have a designed template
            // Setting some theme variables
            Map<String, Object> dotThemeData = DotTemplateTool.theme(cmsTemplate.getTheme(), host.getIdentifier());
            context.put("dotTheme", dotThemeData);
            context.put("dotThemeLayout", DotTemplateTool.themeLayout(cmsTemplate.getInode()));
            // Our designed template
            template = VelocityUtil.getEngine()
                .getTemplate((String) dotThemeData.get("templatePath"));
        } else {
            template = VelocityUtil.getEngine()
                .getTemplate(PageMode.EDIT_MODE.name() + File.separator + templateIdentifier.getId() + "."
                        + VelocityType.TEMPLATE.fileExtension);
        }

        PrintWriter out = response.getWriter();
        request.setAttribute("velocityContext", context);
        try {
            if (builder.getWidgetPreExecute() != null) {
                VelocityUtil.getEngine()
                    .evaluate(context, out, "", builder.getWidgetPreExecute());
            }
            if (hostVariablesTemplate != null)
                hostVariablesTemplate.merge(context, out);
            template.merge(context, out);

        } catch (ParseErrorException e) {
            out.append(e.getMessage());
        }
    }



}
