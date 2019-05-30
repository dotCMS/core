
package com.liferay.portlet;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.Portlet;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.PortletSession;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotmarketing.business.Layout;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CollectionFactory;
import com.liferay.util.StringPool;
import com.liferay.util.lang.ClassUtil;

/**
 * This portlet wraps the implementing porlets e.g. (JspPortlet, StrutsPortlet)
 * and provides the Portet spec methods for those implementataions
 * @author will
 *
 */
public class ConcretePortletWrapper implements Portlet {

  public static void clearResponse(HttpSession ses, Layout layout, String portletId) {

    String sesResponseId = layout.getId() + StringPool.UNDERLINE + portletId;

    getResponses(ses).remove(sesResponseId);
  }

  public static void clearResponses(HttpSession ses) {
    getResponses(ses).clear();
  }

  public static Map getResponses(HttpSession ses) {
    Map responses = (Map) ses.getAttribute(WebKeys.CACHE_PORTLET_RESPONSES);

    if (responses == null) {
      responses = CollectionFactory.getHashMap();

      ses.setAttribute(WebKeys.CACHE_PORTLET_RESPONSES, responses);
    }

    return responses;
  }

  public static Map getResponses(PortletSession ses) {
    return getResponses(((PortletSessionImpl) ses).getSession());
  }

  public ConcretePortletWrapper(Portlet portlet, PortletContext portletCtx) {

    _portlet = portlet;
    _portletCtx = portletCtx;

    _strutsPortlet = ClassUtil.isSubclass(portlet.getClass(), StrutsPortlet.class);
  }

  public void init(PortletConfig config) throws PortletException {
    _portletConfig = (PortletConfigImpl) config;

    _portletId = _portletConfig.getPortletId();

    URLClassLoader classLoader = _getPortletClassLoader();

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader);
    }

    _portlet.init(config);

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader.getParent());
    }

    _destroyable = true;
  }

  public void processAction(ActionRequest req, ActionResponse res) throws IOException, PortletException {

    URLClassLoader classLoader = _getPortletClassLoader();

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader);
    }

    _portlet.processAction(req, res);

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader.getParent());
    }
  }

  public void render(RenderRequest req, RenderResponse res) throws IOException, PortletException {

    URLClassLoader classLoader = _getPortletClassLoader();

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader);
    }

    String userId = PortalUtil.getUserId(req);

    _portlet.render(req, res);

    if (classLoader != null) {
      Thread.currentThread().setContextClassLoader(classLoader.getParent());
    }
  }

  public void destroy() {
    if (_destroyable) {
      URLClassLoader classLoader = _getPortletClassLoader();

      if (classLoader != null) {
        Thread.currentThread().setContextClassLoader(classLoader);
      }

      _portlet.destroy();

      if (classLoader != null) {
        Thread.currentThread().setContextClassLoader(classLoader.getParent());
      }
    }

    _destroyable = false;
  }

  public Portlet getPortletInstance() {
    return _portlet;
  }

  public boolean isDestroyable() {
    return _destroyable;
  }

  public boolean isStrutsPortlet() {
    return _strutsPortlet;
  }

  private URLClassLoader _getPortletClassLoader() {
    return (URLClassLoader) _portletCtx.getAttribute(WebKeys.PORTLET_CLASS_LOADER);
  }

  private String _portletId;
  private Portlet _portlet;
  private PortletConfigImpl _portletConfig;
  private PortletContext _portletCtx;
  private boolean _destroyable;
  private boolean _strutsPortlet;

}
