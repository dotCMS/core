package com.dotmarketing.factories;

import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.Layout;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

/**
 *
 * @author  maria, david (2005)
 */
public class PreviewFactory {



	public static void setVelocityURLS(HttpServletRequest hreq) {

		HttpSession session = hreq.getSession();

		java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/director/direct"});
		String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params);
		session.setAttribute(WebKeys.DIRECTOR_URL, directorURL);

		String portletId=null;

		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/browser/view_browser"});
		portletId="EXT_BROWSER";
		String viewBrowserURL = null;
		try {
			 viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params, portletId);
		} catch(Exception e) {
			viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params);
		}
		session.setAttribute(WebKeys.VIEW_BROWSER_URL, viewBrowserURL);

		portletId="EXT_BROWSER";
		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/htmlpages/preview_htmlpage"});
		String previewPageURL = null;
		try {
			previewPageURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params,portletId);
		} catch (Exception e) {
			previewPageURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params);
		}

		session.setAttribute(WebKeys.PREVIEW_PAGE_URL, previewPageURL);

		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets_popup"});
		String viewContentsURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params);
		session.setAttribute(WebKeys.VIEW_CONTENTS_URL, viewContentsURL);

	}

	public static void setVelocityURLS(HttpServletRequest hreq, Layout layout) {
		hreq.setAttribute(WebKeys.LAYOUT, layout);
		HttpSession session = hreq.getSession();

		String portletId=null;
		java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/director/direct"});
		// director portlet
		portletId="EXT_BROWSER";
		String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params, portletId);
		session.setAttribute(WebKeys.DIRECTOR_URL, directorURL);

		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/browser/view_browser"});
		portletId="EXT_BROWSER";
		String viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params, portletId);
		//iewBrowserURL =(UtilMethods.isSet(hreq.getParameter("referer"))?hreq.getParameter("referer"):viewBrowserURL);
		session.setAttribute(WebKeys.VIEW_BROWSER_URL, viewBrowserURL);

		portletId="EXT_BROWSER";
		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/htmlpages/preview_htmlpage"});
		String previewPageURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params,portletId);
		session.setAttribute(WebKeys.PREVIEW_PAGE_URL, previewPageURL);

		params = new java.util.HashMap<String, String[]>();
		params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets_popup"});
		String viewContentsURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params,portletId);
		session.setAttribute(WebKeys.VIEW_CONTENTS_URL, viewContentsURL);

	}

}
