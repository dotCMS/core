package com.dotmarketing.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.PageRequestModeUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class SpeedyAssetServlet2 extends HttpServlet {

    private static final long serialVersionUID = 1L;

    final static String NOT_FOUND="fileAsset";

    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            _serve(request, response);
        } catch (Throwable t) {
            throw new ServletException(t);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }


    private void _serve(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {

        Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);


        HttpSession session = request.getSession(false);
        boolean ADMIN_MODE = PageRequestModeUtil.isAdminMode(session);
        boolean PREVIEW_MODE = PageRequestModeUtil.isPreviewMode(session);
        boolean EDIT_MODE = PageRequestModeUtil.isEditMode(session);


        // Checking if host is active
        if (!ADMIN_MODE && !APILocator.getVersionableAPI().hasLiveVersion(host)) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    LanguageUtil.get("server-unavailable-error-message"));
            return;
        }



        StringTokenizer tokens = new StringTokenizer(request.getRequestURI(), "/");
        if (tokens.countTokens() < 2) {
            response.sendError(404);
            return;
        }
        tokens.nextToken();
        String id = tokens.nextToken();
        String fieldName = tokens.hasMoreTokens() ? tokens.nextToken() : NOT_FOUND;


        Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(id);
        User user = WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(request);
        boolean live = (user != null) ? (EDIT_MODE || PREVIEW_MODE) ? false : true : true;
        if (!live) {
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        if (!shortOpt.isPresent()) {
            response.sendError(404);
            return;
        }
        ShortyId shorty = shortOpt.get();
        if (shorty.subType == ShortType.CONTENTLET) {


            if (shorty.type == ShortType.IDENTIFIER) {
                Contentlet con = APILocator.getContentletAPI().findContentletByIdentifier(
                        shorty.longId, false, -1, APILocator.getUserAPI().getSystemUser(), false);

                String field = resolveField(con, fieldName);


                request.getRequestDispatcher(
                        "/contentAsset/raw-data/" + shorty.longId + "/" + field)
                        .forward(request, response);
            } else {
                Contentlet con = APILocator.getContentletAPI().find(shorty.longId,
                        APILocator.getUserAPI().getSystemUser(), false);

                String field = resolveField(con, fieldName);

                request.getRequestDispatcher(
                        "/contentAsset/raw-data/" + shorty.longId + "/" + field + "/byInode/true")
                        .forward(request, response);
            }
        }

    }



    private String resolveField(Contentlet con, final String tryField) {
        if(!NOT_FOUND.equals(tryField)){
            Object obj = con.getMap().get(tryField);
            if (obj instanceof File) {
                return tryField;
            }
           return NOT_FOUND;
        }


        for (Field f : FieldsCache.getFieldsByStructureInode(con.getStructureInode())) {
            if ("binary".equals(f.getFieldType())) {
                return f.getVelocityVarName();
            }
        }

        return tryField;
    }

}
