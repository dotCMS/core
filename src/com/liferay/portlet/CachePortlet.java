/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.Layout;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CollectionFactory;
import com.liferay.util.StringPool;
import com.liferay.util.Time;
import com.liferay.util.lang.ClassUtil;
import com.liferay.util.servlet.StringServletResponse;

/**
 * <a href="CachePortlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.16 $
 *
 */
public class CachePortlet implements Portlet {

	public static void clearResponse(
		HttpSession ses, Layout layout, String portletId) {

		String sesResponseId =
			layout.getId() + StringPool.UNDERLINE + portletId;

		getResponses(ses).remove(sesResponseId);
	}

	public static void clearResponses(HttpSession ses) {
		getResponses(ses).clear();
	}

	public static Map getResponses(HttpSession ses) {
		Map responses = (Map)ses.getAttribute(WebKeys.CACHE_PORTLET_RESPONSES);

		if (responses == null) {
			responses = CollectionFactory.getHashMap();

			ses.setAttribute(WebKeys.CACHE_PORTLET_RESPONSES, responses);
		}

		return responses;
	}

	public static Map getResponses(PortletSession ses) {
		return getResponses(((PortletSessionImpl)ses).getSession());
	}

	public CachePortlet(Portlet portlet, PortletContext portletCtx,
						Integer expCache) {

		_portlet = portlet;
		_portletCtx = portletCtx;
		_expCache = expCache;

		_strutsPortlet = ClassUtil.isSubclass(
			portlet.getClass(), StrutsPortlet.class);
	}

	public void init(PortletConfig config) throws PortletException {
		_portletConfig = (PortletConfigImpl)config;

		_portletId = _portletConfig.getPortletId();

		URLClassLoader classLoader = _getPortletClassLoader();

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}

		_portlet.init(config);

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(
				classLoader.getParent());
		}

		_destroyable = true;
	}

	public void processAction(ActionRequest req, ActionResponse res)
		throws IOException, PortletException {

		URLClassLoader classLoader = _getPortletClassLoader();

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}

		_portlet.processAction(req, res);

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(
				classLoader.getParent());
		}
	}

	public void render(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		URLClassLoader classLoader = _getPortletClassLoader();

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}

		String userId = "";
		try {
			userId = PortalUtil.getUser(req) == null?"":PortalUtil.getUser(req).getUserId();
		} catch (PortalException e) {
			throw new PortletException(e);
		} catch (SystemException e) {
			throw new PortletException(e);
		}

		if ((userId == null) || (_expCache == null) ||
			(_expCache.intValue() == 0)) {

			_portlet.render(req, res);
		}
		else {
			RenderResponseImpl resImpl = (RenderResponseImpl)res;

			StringServletResponse stringServletRes =
				(StringServletResponse)resImpl.getHttpServletResponse();

			PortletSession ses = req.getPortletSession();

			long now = System.currentTimeMillis();

			Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);

			Map sesResponses = getResponses(ses);

			String sesResponseId =
				layout.getId() + StringPool.UNDERLINE + _portletId;

			CachePortletResponse response =
				(CachePortletResponse)sesResponses.get(sesResponseId);

			if (response == null) {
				_portlet.render(req, res);

				response = new CachePortletResponse(
					resImpl.getTitle(),
					stringServletRes.getString(),
					now + Time.SECOND * _expCache.intValue());

				sesResponses.put(sesResponseId, response);
			}
			else if ((response.getTime() < now) &&
					 (_expCache.intValue() > 0)) {

				_portlet.render(req, res);

				response.setTitle(resImpl.getTitle());
				response.setContent(stringServletRes.getString());
				response.setTime(now + Time.SECOND * _expCache.intValue());
			}
			else {
				resImpl.setTitle(response.getTitle());
				stringServletRes.getWriter().print(response.getContent());
			}
		}

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(
				classLoader.getParent());
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
				Thread.currentThread().setContextClassLoader(
					classLoader.getParent());
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
		return (URLClassLoader)_portletCtx.getAttribute(
			WebKeys.PORTLET_CLASS_LOADER);
	}

	private String _portletId;
	private Portlet _portlet;
	private PortletConfigImpl _portletConfig;
	private PortletContext _portletCtx;
	private Integer _expCache;
	private boolean _destroyable;
	private boolean _strutsPortlet;

}