package com.dotcms.rendering.velocity.servlet;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.BlockPageCache.PageCacheParameters;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.viewtools.DotTemplateTool;
import com.dotmarketing.viewtools.RequestWrapper;
import com.dotmarketing.viewtools.content.ContentMap;

import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.view.context.ChainedContext;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

public abstract class VelocityPreviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private ContentletAPI conAPI = APILocator.getContentletAPI();

    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    private static PortletAPI portletAPI = APILocator.getPortletAPI();

    private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

    private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
        permissionAPI = permissionAPIRef;
    }

    private String CHARSET = "UTF-8";

    private final String VELOCITY_HTMLPAGE_EXTENSION = "dotpage";

    public static final String VELOCITY_CONTEXT = "velocityContext";
    private final String PREVIEW_MODE_VTL= "preview_mode.vtl";
    private final String PREVIEW_MODE_MENU_VTL= "preview_mode_menu.vtl";

    @CloseDBIfOpened
    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        final String uri = URLDecoder.decode((req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE) != null)
                ? (String) req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE)
                : req.getRequestURI(), "UTF-8");



        RequestWrapper request = new RequestWrapper(req);
        request.setRequestUri(uri);


        Host host = hostWebAPI.getCurrentHostNoThrow(request);


        // Checking if host is active
        boolean hostlive;
        boolean _adminMode = UtilMethods.isAdminMode(request, response);

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



            PageMode mode = PageMode.get(request);


            // we will always need a visitor in admin mode
            if (mode.isAdmin) {
                visitorAPI.getVisitor(request, true);
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

        // build the dirs
        String pathWorking = VelocityUtil.getVelocityRootPath() + File.separator + "working";
        String pathLive = VelocityUtil.getVelocityRootPath() + File.separator + "live";

        if (!new File(pathWorking).exists()) {
            new File(pathWorking).mkdirs();
        }

        if (!new File(pathLive).exists()) {
            new File(pathLive).mkdirs();
        }
    }



    @SuppressWarnings("unchecked")
    public void doPreviewMode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = URLDecoder.decode(request.getRequestURI(), UtilMethods.getCharsetConfiguration());


        Host host = hostWebAPI.getCurrentHost(request);

        StringBuilder preExecuteCode = new StringBuilder();
        Boolean widgetPreExecute = false;

        // Getting the user to check the permissions
        com.liferay.portal.model.User user = null;

        try {
            user = com.liferay.portal.util.PortalUtil.getUser(request);
        } catch (Exception nsue) {
            Logger.warn(this, "Exception trying getUser: " + nsue.getMessage(), nsue);
        }

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
        PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
        List<PublishingEndPoint> receivingEndpoints = pepAPI.getReceivingEndPoints();
        // to check user has permission to write on this page
        boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, user);
        boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, user);
        boolean hasRemotePublishPermOverHTMLPage =
                hasPublishPermOverHTMLPage && LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;
        boolean hasEndPoints = UtilMethods.isSet(receivingEndpoints) && !receivingEndpoints.isEmpty();

        context.put("EDIT_HTMLPAGE_PERMISSION", new Boolean(hasWritePermOverHTMLPage));
        context.put("PUBLISH_HTMLPAGE_PERMISSION", new Boolean(hasPublishPermOverHTMLPage));
        context.put("REMOTE_PUBLISH_HTMLPAGE_PERMISSION", new Boolean(hasRemotePublishPermOverHTMLPage));
        context.put("REMOTE_PUBLISH_END_POINTS", new Boolean(hasEndPoints));
        context.put("canAddForm", Boolean.valueOf(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false));
        context.put("canViewDiff", Boolean.valueOf(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false));

        context.put("HTMLPAGE_ASSET_STRUCTURE_TYPE", htmlPage.isContent() ? ((Contentlet) htmlPage).getStructureInode()
                : APILocator.getHTMLPageAssetAPI().DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        context.put("HTMLPAGE_IS_CONTENT", htmlPage.isContent());

        boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission(APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, true), PERMISSION_WRITE, user, true);
        context.put("EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate);

        com.dotmarketing.portlets.templates.model.Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, true);
        Identifier templateIdentifier = APILocator.getIdentifierAPI()
            .find(cmsTemplate);

        Logger.debug(VelocityPreviewServlet.class, "VELOCITY TEMPLATE INODE=" + cmsTemplate.getInode());

        VelocityUtil.makeBackendContext(context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, true, false, true, host);
        context.put("previewPage", "2");
        context.put("livePage", "0");



        // get the containers for the page and stick them in context
        List<Container> containers = APILocator.getTemplateAPI()
            .getContainersInTemplate(cmsTemplate, APILocator.getUserAPI()
                .getSystemUser(), false);
        for (Container c : containers) {

            context.put(String.valueOf("container" + c.getIdentifier()),
                    "/working/" + c.getIdentifier() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION"));

            context.put("EDIT_CONTAINER_PERMISSION" + c.getIdentifier(),
                    permissionAPI.doesUserHavePermission(c, PERMISSION_WRITE, user, true));

            boolean hasWritePermOverTheStructure = false;

            for (ContainerStructure cs : APILocator.getContainerAPI()
                .getContainerStructures(c)) {
                Structure st = CacheLocator.getContentTypeCache()
                    .getStructureByInode(cs.getStructureId());

                hasWritePermOverTheStructure |= permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, user, true);
            }


            context.put("ADD_CONTENT_PERMISSION" + c.getIdentifier(), new Boolean(hasWritePermOverTheStructure));

            Logger.debug(VelocityPreviewServlet.class, String.valueOf("container" + c.getIdentifier()) + "=/working/" + c.getIdentifier()
                    + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION"));

            String sort = (c.getSortContentletsBy() == null) ? "tree_order" : c.getSortContentletsBy();

            boolean staticContainer = !UtilMethods.isSet(c.getLuceneQuery());

            List<Contentlet> contentlets = null;

            // get contentlets only for main frame
            if (request.getParameter("mainFrame") != null) {
                if (staticContainer) {
                    Logger.debug(VelocityPreviewServlet.class, "Static Container!!!!");

                    Logger.debug(VelocityPreviewServlet.class, "html=" + htmlPage.getInode() + " container=" + c.getInode());

                    // The container doesn't have categories
                    Identifier idenHtmlPage = APILocator.getIdentifierAPI()
                        .find(htmlPage);
                    Identifier idenContainer = APILocator.getIdentifierAPI()
                        .find(c);
                    contentlets = conAPI.findPageContentlets(idenHtmlPage.getInode(), idenContainer.getInode(), sort, true, -1,
                            user, true);
                    Logger.debug(VelocityPreviewServlet.class, "Getting contentlets for language=" + (String) request.getSession()
                        .getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) + " contentlets =" + contentlets.size());

                }

                if (UtilMethods.isSet(contentlets) && contentlets.size() > 0) {
                    Set<String> contentletIdentList = new HashSet<String>();
                    List<Contentlet> contentletsFilter = new ArrayList<Contentlet>();
                    for (Contentlet cont : contentlets) {
                        if (!contentletIdentList.contains(cont.getIdentifier())) {
                            contentletIdentList.add(cont.getIdentifier());
                            contentletsFilter.add(cont);
                        }
                    }
                    contentlets = contentletsFilter;
                }
                List<String> contentletList = new ArrayList<String>();

                if (contentlets != null && contentlets.size() > 0) {
                    Iterator<Contentlet> iter = contentlets.iterator();
                    int count = 0;

                    while (iter.hasNext() && (count < c.getMaxContentlets())) {
                        count++;

                        Contentlet contentlet = (Contentlet) iter.next();
                        Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                            .find(contentlet);

                        boolean hasWritePermOverContentlet =
                                permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user, true);

                        context.put("EDIT_CONTENT_PERMISSION" + contentletIdentifier.getInode(),
                                new Boolean(hasWritePermOverContentlet));

                        contentletList.add(String.valueOf(contentletIdentifier.getInode()));
                        Logger.debug(this, "Adding contentlet=" + contentletIdentifier.getInode());
                        Structure contStructure = contentlet.getStructure();
                        if (contStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
                            Field field = contStructure.getFieldVar("widgetPreexecute");
                            if (field != null && UtilMethods.isSet(field.getValues())) {
                                preExecuteCode.append(field.getValues()
                                    .trim() + "\n");
                                widgetPreExecute = true;
                            }
                        }

                    }
                }

                // sets contentletlist with all the files to load per
                // container
                context.put("contentletList" + c.getIdentifier(), contentletList);
                context.put("totalSize" + c.getIdentifier(), new Integer(contentletList.size()));
            }
        }

        Logger.debug(VelocityPreviewServlet.class, "Before finding template: /working/" + templateIdentifier.getInode() + "."
                + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"));

        Logger.debug(VelocityPreviewServlet.class, "Velocity directory:" + VelocityUtil.getEngine()
            .getProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH));

        if (request.getParameter("leftMenu") != null) {
            /*
             * try to get the messages from the session
             */

            List<String> list = new ArrayList<String>();
            if (SessionMessages.contains(request, "message")) {
                list.add((String) SessionMessages.get(request, "message"));
                SessionMessages.clear(request);
            }
            if (SessionMessages.contains(request, "custommessage")) {
                list.add((String) SessionMessages.get(request, "custommessage"));
                SessionMessages.clear(request);
            }

            if (list.size() > 0) {
                ArrayList<String> mymessages = new ArrayList<String>();
                Iterator<String> it = list.iterator();

                while (it.hasNext()) {
                    try {
                        String message = (String) it.next();
                        Company comp = PublicCompanyFactory.getDefaultCompany();
                        mymessages.add(LanguageUtil.get(comp.getCompanyId(), user.getLocale(), message));
                    } catch (Exception e) {
                    }
                }
                context.put("vmessages", mymessages);
            }

            template = VelocityUtil.getEngine()
                .getTemplate(PREVIEW_MODE_MENU_VTL);
        } else if (request.getParameter("mainFrame") != null) {
            hostVariablesTemplate = VelocityUtil.getEngine()
                .getTemplate("/working/" + host.getIdentifier() + "." + Config.getStringProperty("VELOCITY_HOST_EXTENSION"));

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
                    .getTemplate("/working/" + templateIdentifier.getInode() + "."
                            + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"));
            }

        } else {
            template = VelocityUtil.getEngine()
                .getTemplate(PREVIEW_MODE_VTL);
        }

        PrintWriter out = response.getWriter();
        request.setAttribute("velocityContext", context);
        try {

            if (widgetPreExecute) {
                VelocityUtil.getEngine()
                    .evaluate(context, out, "", preExecuteCode.toString());
            }
            if (hostVariablesTemplate != null)
                hostVariablesTemplate.merge(context, out);
            template.merge(context, out);

        } catch (ParseErrorException e) {
            out.append(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    protected void doEditMode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getRequestURI();

        Host host = hostWebAPI.getCurrentHost(request);


        // Getting the user to check the permissions
        com.liferay.portal.model.User backendUser = null;
        try {
            backendUser = com.liferay.portal.util.PortalUtil.getUser(request);
        } catch (Exception nsue) {
            Logger.warn(this, "Exception trying getUser: " + nsue.getMessage(), nsue);
        }

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

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), backendUser, true, host, context));


        com.dotmarketing.portlets.templates.model.Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, true);
        // issue- 1775 If User doesn't have edit permission on HTML Pages
        /*
         * if(!hasWritePermOverHTMLPage){ doPreviewMode(request, response); return; }
         */
        if (cmsTemplate == null) {// DOTCMS-4051
            cmsTemplate = new com.dotmarketing.portlets.templates.model.Template();
            Logger.debug(VelocityPreviewServlet.class, "HTMLPAGE TEMPLATE NOT FOUND");
        }

        Identifier templateIdentifier = APILocator.getIdentifierAPI()
            .find(cmsTemplate);

        Logger.debug(VelocityPreviewServlet.class, "VELOCITY TEMPLATE INODE=" + cmsTemplate.getInode());

        VelocityUtil.makeBackendContext(context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, true, true, false, host);
        // added to show tabs
        context.put("previewPage", "1");
        // get the containers for the page and stick them in context

        if (request.getParameter("mainFrame") != null) {

        }
        Logger.debug(VelocityPreviewServlet.class, "Before finding template: /working/" + templateIdentifier.getId() + "."
                + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"));

        Logger.debug(VelocityPreviewServlet.class, "Velocity directory:" + VelocityUtil.getEngine()
            .getProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH));

        if (request.getParameter("leftMenu") != null) {
            /*
             * try to get the messages from the session
             */

            List<String> list = new ArrayList<String>();
            if (SessionMessages.contains(request, "message")) {
                list.add((String) SessionMessages.get(request, "message"));
                SessionMessages.clear(request);
            }
            if (SessionMessages.contains(request, "custommessage")) {
                list.add((String) SessionMessages.get(request, "custommessage"));
                SessionMessages.clear(request);
            }

            if (list.size() > 0) {
                ArrayList<String> mymessages = new ArrayList<String>();
                Iterator<String> it = list.iterator();

                while (it.hasNext()) {
                    try {
                        String message = (String) it.next();
                        Company comp = PublicCompanyFactory.getDefaultCompany();
                        mymessages.add(LanguageUtil.get(comp.getCompanyId(), backendUser.getLocale(), message));
                    } catch (Exception e) {
                    }
                }
                context.put("vmessages", mymessages);
            }

            template = VelocityUtil.getEngine()
                .getTemplate(PREVIEW_MODE_MENU_VTL);
        } else if (request.getParameter("mainFrame") != null) {
            hostVariablesTemplate = VelocityUtil.getEngine()
                .getTemplate("/working/" + host.getIdentifier() + "." + Config.getStringProperty("VELOCITY_HOST_EXTENSION"));

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
                    .getTemplate("/working/" + templateIdentifier.getId() + "."
                            + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"));
            }
        } else {
            // Return a resource not found right away if the page is not found,
            // not try to load the frames
            if (!InodeUtils.isSet(templateIdentifier.getInode()))
                throw new ResourceNotFoundException("");
            template = VelocityUtil.getEngine()
                .getTemplate(PREVIEW_MODE_VTL);
        }

        PrintWriter out = response.getWriter();
        request.setAttribute("velocityContext", context);
        try {
            if (context.containsKey("WIDGET_PRE_EXECUTE")) {
                VelocityUtil.getEngine()
                    .evaluate(context, out, "", context.get("WIDGET_PRE_EXECUTE")
                        .toString());
            }
            if (hostVariablesTemplate != null)
                hostVariablesTemplate.merge(context, out);
            template.merge(context, out);

        } catch (ParseErrorException e) {
            out.append(e.getMessage());
        }
    }



    /**
     * @author will this filter class strips all leading whitespace from the server response which
     *         is helpful for xml feeds and the like.
     */

    public class VelocityFilterWriter extends FilterWriter {

        private boolean firstNonWhiteSpace = false;

        public VelocityFilterWriter(Writer arg0) {
            super(arg0);

        }

        @Override
        public void write(char[] arg0) throws IOException {
            if (firstNonWhiteSpace) {
                super.write(arg0);
            } else {

                for (int i = 0; i < arg0.length; i++) {
                    if (arg0[i] > 32) {
                        firstNonWhiteSpace = true;
                    }
                    if (firstNonWhiteSpace) {
                        super.write(arg0[i]);
                    }

                }

            }

        }

        @Override
        public void write(String arg0) throws IOException {
            if (firstNonWhiteSpace) {
                super.write(arg0);
            } else {
                char[] stringChar = arg0.toCharArray();
                for (int i = 0; i < stringChar.length; i++) {

                    if (stringChar[i] > 32) {
                        firstNonWhiteSpace = true;
                        super.write(arg0.substring(i, stringChar.length));
                        break;
                    }

                }

            }

        }

    }

}
