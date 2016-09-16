package com.dotmarketing.servlets;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageRequestModeUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class SpeedyAssetServlet2 extends HttpServlet {

   private static final long serialVersionUID = 1L;



   protected void service(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      try {
         _serve(request, response);
      } catch (Throwable t) {
         throw new ServletException(t);
      } finally {
         UtilMethods.closeDbSilently();
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

      User user = WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(request);

      boolean live = (user != null) ? (EDIT_MODE || PREVIEW_MODE) ? false : true : true;



      StringTokenizer tokens = new StringTokenizer(request.getRequestURI(), "/");
      if (tokens.countTokens() < 2) {
         response.sendError(404);
         return;
      }
      tokens.nextToken();
      String id = tokens.nextToken();

      long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();


      Contentlet con = resolveContentlet(id, languageId, live, user);
      if (con == null) {
         response.sendError(404);
         return;
      }



      if (!live) {
         response.setHeader("Pragma", "no-cache");
         response.setHeader("Cache-Control", "no-cache");
         response.setDateHeader("Expires", 0);
      }



      request.getRequestDispatcher(
            "/contentAsset/raw-data/" + con.getInode() + "/" + resolveField(con) + "/byInode/true")
            .forward(request, response);



   }


   private Contentlet resolveContentlet(String id, long lang, boolean live, User user)
         throws DotDataException, DotSecurityException {

      long defualtLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
      ContentletAPI capi = APILocator.getContentletAPI();
      VersionableAPI vapi = APILocator.getVersionableAPI();
      Contentlet con = null;

      // if we have a shorty, use the index
      if (id.length() == 8) {
         StringBuilder query =
               new StringBuilder("+(identifier:").append(id).append("* inode:").append(id)
                     .append("*) ");


         if (live) {
            query.append("+live:true ");
         } else {
            query.append("+working:true ");
         }
         query.append("languageId:").append(lang).append("^10 ");
         query.append("languageId:").append(defualtLang);
         List<Contentlet> cons = capi.search(query.toString(), 1, 0, "score", user, true);
         con = (cons.size() > 0) ? cons.get(0) : con;
      } else {
         ContentletVersionInfo cvi = vapi.getContentletVersionInfo(id, lang);
         if (cvi == null && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)
               && lang != defualtLang) {
            cvi = vapi.getContentletVersionInfo(id, defualtLang);

         }
         if (cvi != null) {
            id = (live) ? cvi.getLiveInode() : cvi.getWorkingInode();
         }
         con = capi.find(id, user, true);

      }
      return con;
   }

   private String resolveField(Contentlet con) {
      String field = "fileAsset";
      if (con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_FILEASSET) {
         for (Field f : con.getStructure().getFields()) {
            if ("binary".equals(f.getFieldType())) {
               field = f.getVelocityVarName();
               break;
            }
         }
      }
      return field;
   }

}
