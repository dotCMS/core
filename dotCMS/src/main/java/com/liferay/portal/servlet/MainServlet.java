/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.servlet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.com.httpbridge.webproxy.http.TaskController;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionServlet;
import com.dotcms.repackage.org.apache.struts.tiles.TilesUtilImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.startup.StartupTasksExecutor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.job.JobScheduler;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.PortletRequestProcessor;
import com.liferay.portal.struts.StrutsUtil;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.ShutdownUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.GetterUtil;
import com.liferay.util.Http;
import com.liferay.util.ParamUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.servlet.EncryptedServletRequest;
import com.liferay.util.servlet.UploadServletRequest;

import io.vavr.API;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.SuppressPropertiesBeanIntrospector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * <a href="MainServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Jorge Ferrer
 * @version $Revision: 1.41 $
 *
 */
public class MainServlet extends ActionServlet {

  @CloseDBIfOpened
  public void init(ServletConfig config) throws ServletException {
    synchronized (MainServlet.class) {
      super.init(config);
      Config.initializeConfig();
      com.dotmarketing.util.Config.setMyApp(config.getServletContext());
      // Need the plugin root dir before Hibernate comes up
      try {
        APILocator.getPluginAPI().setPluginJarDir(new File(config.getServletContext().getRealPath("/WEB-INF/lib")));
      } catch (IOException e1) {
        Logger.debug(InitServlet.class, "IOException: " + e1.getMessage(), e1);
      }

      // Checking for execute upgrades
      try {
        StartupTasksExecutor.getInstance().executeUpgrades(config.getServletContext().getRealPath("/"));
      } catch (DotRuntimeException e1) {
        throw new ServletException(e1);
      } catch (DotDataException e1) {
        throw new ServletException(e1);
      } finally {
        DbConnectionFactory.closeSilently();
      }

      HashSet<String> suppressProperties = new HashSet<>();
      suppressProperties.add("class");
      suppressProperties.add("multipartRequestHandler");
      suppressProperties.add("resultValueMap");
      PropertyUtils.addBeanIntrospector(new SuppressPropertiesBeanIntrospector(suppressProperties));
      PropertyUtils.clearDescriptors();
      Logger.info(this.getClass(), "SuppressPropertiesBeanIntrospector enabled for legacy Struts applications");

      // Context path
      ServletConfig sc = getServletConfig();
      ServletContext ctx = getServletContext();

      String ctxPath = GetterUtil.get(sc.getInitParameter("ctx_path"), "/");

      ctx.setAttribute(WebKeys.CTX_PATH, StringUtil.replace(ctxPath + "/c", "//", "/"));

      ctx.setAttribute(WebKeys.CAPTCHA_PATH, StringUtil.replace(ctxPath + "/captcha", "//", "/"));

      ctx.setAttribute(WebKeys.IMAGE_PATH, StringUtil.replace(ctxPath + "/image", "//", "/"));

      // Company id

      _companyId = ctx.getInitParameter("company_id");

      ctx.setAttribute(WebKeys.COMPANY_ID, _companyId);

      // Initialize portlets
      try {
        APILocator.getPortletAPI().findAllPortlets();
      } catch (SystemException e1) {
        throw new DotRuntimeException(e1);
      }

      // Check company

      try {
        CompanyLocalManagerUtil.checkCompany(_companyId);
      } catch (Exception e) {
        throw new DotRuntimeException(e);
      }

      // Check web settings

      try {
        String xml = Http.URLtoString(ctx.getResource("/WEB-INF/web.xml"));

        _checkWebSettings(xml);
      } catch (Exception e) {
        Logger.error(this, e.getMessage(), e);
      }

      // Scheduler

      // try {
      // Iterator itr =
      // PortletManagerUtil.getPortlets(_companyId).iterator();
      //
      // while (itr.hasNext()) {
      // Portlet portlet = (Portlet)itr.next();
      //
      // String className = portlet.getSchedulerClass();
      //
      // if (portlet.isActive() && className != null) {
      // Scheduler scheduler =
      // (Scheduler)InstancePool.get(className);
      //
      // scheduler.schedule();
      // }
      // }
      // }
      // catch (ObjectAlreadyExistsException oaee) {
      // }
      // catch (Exception e) {
      // Logger.error(this,e.getMessage(),e);
      // }

      // Message Resources

      MultiMessageResources messageResources = (MultiMessageResources) ctx.getAttribute(Globals.MESSAGES_KEY);

      messageResources.setServletContext(ctx);

      WebAppPool.put(_companyId, Globals.MESSAGES_KEY, messageResources);

      // Current users

      WebAppPool.put(_companyId, WebKeys.CURRENT_USERS, new TreeMap());

      // HttpBridge

      TaskController.bridgeUserServicePath = "/httpbridge/home";
      TaskController.bridgeHttpServicePath = "/httpbridge/http";
      TaskController.bridgeGotoTag = "(goto)";
      TaskController.bridgeThenTag = "(then)";
      TaskController.bridgePostTag = "(post)";

      // Process startup events

      try {
        EventsProcessor.process(PropsUtil.getArray(PropsUtil.GLOBAL_STARTUP_EVENTS), true);

        EventsProcessor.process(PropsUtil.getArray(PropsUtil.APPLICATION_STARTUP_EVENTS), new String[] {_companyId});
      } catch (Exception e) {
        Logger.error(this, e.getMessage(), e);
      }

      PortalInstances.init(_companyId);

      // Init other dotCMS services.
      DotInitializationService.getInstance().initialize();
    }
  }

  public void callParentService(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

    super.service(req, res);
  }

  public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

    if (!PortalInstances.matches()) {
      String html = ContentUtil.get("messages/en_US/init.html");

      res.getOutputStream().print(html);

      return;
    }

    if (ShutdownUtil.isShutdown()) {
      String html = ContentUtil.get("messages/en_US/shutdown.html");

      res.getOutputStream().print(html);

      return;
    }
    req.setAttribute("dotcache", "no");
    // Shared session

    HttpSession ses = req.getSession();

    /*
     * ses.setAttribute( com.liferay.portal.auth.CASAutoLogin.CAS_FILTER_USER, "liferay.com.1");
     */

    // CTX

    ServletContext ctx = getServletContext();
    ServletContext portalCtx = ctx.getContext(PropsUtil.get(PropsUtil.PORTAL_CTX));
    if (portalCtx == null) {
      portalCtx = ctx;
    }

    req.setAttribute(WebKeys.CTX, portalCtx);

    // CTX_PATH variable

    String ctxPath = (String) ctx.getAttribute(WebKeys.CTX_PATH);

    if (portalCtx.getAttribute(WebKeys.CTX_PATH) == null) {
      portalCtx.setAttribute(WebKeys.CTX_PATH, ctxPath);
    }

    if (ses.getAttribute(WebKeys.CTX_PATH) == null) {
      ses.setAttribute(WebKeys.CTX_PATH, ctxPath);
    }

    req.setAttribute(WebKeys.CTX_PATH, ctxPath);

    // CAPTCHA_PATH variable

    String captchaPath = (String) ctx.getAttribute(WebKeys.CAPTCHA_PATH);

    if (portalCtx.getAttribute(WebKeys.CAPTCHA_PATH) == null) {
      portalCtx.setAttribute(WebKeys.CAPTCHA_PATH, captchaPath);
    }

    if (ses.getAttribute(WebKeys.CAPTCHA_PATH) == null) {
      ses.setAttribute(WebKeys.CAPTCHA_PATH, captchaPath);
    }

    req.setAttribute(WebKeys.CAPTCHA_PATH, captchaPath);

    // IMAGE_PATH variable

    String imagePath = (String) ctx.getAttribute(WebKeys.IMAGE_PATH);

    if (portalCtx.getAttribute(WebKeys.IMAGE_PATH) == null) {
      portalCtx.setAttribute(WebKeys.IMAGE_PATH, imagePath);
    }

    if (ses.getAttribute(WebKeys.IMAGE_PATH) == null) {
      ses.setAttribute(WebKeys.IMAGE_PATH, imagePath);
    }

    req.setAttribute(WebKeys.IMAGE_PATH, imagePath);

    // WebKeys.COMPANY_ID variable

    String companyId = (String) ctx.getAttribute(WebKeys.COMPANY_ID);

    if (portalCtx.getAttribute(WebKeys.COMPANY_ID) == null) {
      portalCtx.setAttribute(WebKeys.COMPANY_ID, companyId);
    }

    if (ses.getAttribute(WebKeys.COMPANY_ID) == null) {
      ses.setAttribute(WebKeys.COMPANY_ID, companyId);
    }

    req.setAttribute(WebKeys.COMPANY_ID, companyId);

    // Portlet Request Processor

    PortletRequestProcessor portletReqProcessor = (PortletRequestProcessor) portalCtx.getAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR);

    if (portletReqProcessor == null) {
      portletReqProcessor = new PortletRequestProcessor(this, getModuleConfig(req));

      portalCtx.setAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR, portletReqProcessor);
    }

    // Tiles definitions factory

    if (portalCtx.getAttribute(TilesUtilImpl.DEFINITIONS_FACTORY) == null) {
      portalCtx.setAttribute(TilesUtilImpl.DEFINITIONS_FACTORY, ctx.getAttribute(TilesUtilImpl.DEFINITIONS_FACTORY));
    }

    // Set character encoding

    String strutsCharEncoding = PropsUtil.get(PropsUtil.STRUTS_CHAR_ENCODING);

    req.setCharacterEncoding(strutsCharEncoding);

    /*
     * if (!BrowserSniffer.is_wml(req)) { res.setContentType( Constants.TEXT_HTML + "; charset=" +
     * strutsCharEncoding); }
     */

    // Determine content type

    String contentType = req.getHeader("Content-Type");

    if ((contentType != null) && (contentType.startsWith("multipart/form-data"))) {

      req = new UploadServletRequest(req);
    } else if (ParamUtil.get(req, WebKeys.ENCRYPT, false)) {
      try {
        Company company = CompanyLocalManagerUtil.getCompany(companyId);

        req = new EncryptedServletRequest(req, company.getKeyObj());
      } catch (Exception e) {
      }
    }

    // Current URL

    String completeURL = Http.getCompleteURL(req);
    if (completeURL.indexOf("j_security_check") != -1) {
      completeURL = ctxPath;
    } else {
      completeURL = completeURL.substring(completeURL.indexOf("://") + 3, completeURL.length());

      completeURL = completeURL.substring(completeURL.indexOf("/"), completeURL.length());
    }

    req.setAttribute(WebKeys.CURRENT_URL, completeURL);

    // Chat server

    // Login

    String userId = PortalUtil.getUserId(req);

    if ((userId != null)) {
      PrincipalThreadLocal.setName(userId);
    }

    if (userId == null) {
      try {
        User user = UserManagerUtil.getDefaultUser(companyId);
        if (ses.getAttribute(Globals.LOCALE_KEY) == null)
          ses.setAttribute(Globals.LOCALE_KEY, user.getLocale());

      } catch (Exception e) {
        Logger.error(this, e.getMessage(), e);
      }
    }

    Optional<String> badProp = req.getParameterMap().keySet().stream().filter(k -> k.startsWith("class.")).findFirst();
    if (badProp.isPresent()) {
      SecurityLogger.logInfo(this.getClass(), "Possible exploit probe from " + req.getRemoteAddr()
          + ".  See: CVE-2014-0114 -  class parameter found in request: " + badProp.get());
      try {
        PropertyUtils.getNestedProperty(this, "class");
        Logger.error(this, "SECURITY ISSUE- `class` attribute NOT DISABLED for BeanUtil introspection, See: CVE-2014-0114 ");
      } catch (java.lang.NoSuchMethodException nse) {
        Logger.info(this, "`class` is disabled as a property for introspection in struts for security");
      } catch (Exception e) {
        Logger.warn(this, e.getMessage(), e);
      }
    }

    // Process pre service events

    try {
      EventsProcessor.process(PropsUtil.getArray(PropsUtil.SERVLET_SERVICE_EVENTS_PRE), req, res);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);

      req.setAttribute(PageContext.EXCEPTION, e);

      StrutsUtil.forward(PropsUtil.get(PropsUtil.SERVLET_SERVICE_EVENTS_PRE_ERROR_PAGE), portalCtx, req, res);
    }

    // Struts service

    callParentService(req, res);

    // Process post service events

    try {
      EventsProcessor.process(PropsUtil.getArray(PropsUtil.SERVLET_SERVICE_EVENTS_POST), req, res);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
    }

    // Clear the principal associated with this thread

    PrincipalThreadLocal.setName(null);
  }

  public void destroy() {

    // Destroy portlets

    try {
      Iterator itr = PortletManagerUtil.getPortlets(_companyId).iterator();

      while (itr.hasNext()) {
        Portlet portlet = (Portlet) itr.next();

        PortalUtil.destroyPortletInstance(portlet);
      }
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
    }

    // Scheduler

    JobScheduler.shutdown();

    // Parent

    super.destroy();
  }

  private void _checkWebSettings(String xml) throws DocumentException {
    SAXReader reader = new SAXReader();
    reader.setEntityResolver(null);

    Document doc = reader.read(new StringReader(xml));

    Element root = doc.getRootElement();

    int timeout = GetterUtil.getInteger(PropsUtil.get(PropsUtil.SESSION_TIMEOUT));

    Element sessionConfig = root.element("session-config");

    if (sessionConfig != null) {
      String sessionTimeout = sessionConfig.elementText("session-timeout");

      timeout = GetterUtil.get(sessionConfig.elementText("session-timeout"), timeout);
    }

    PropsUtil.set(PropsUtil.SESSION_TIMEOUT, Integer.toString(timeout));
  }

  private static final Log _log = LogFactory.getLog(MainServlet.class);

  private String _companyId;

}
