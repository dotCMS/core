package com.dotmarketing.factories;

import com.dotcms.repackage.javax.portlet.WindowState;
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



	/**
	 * Stores in the session the portlet URLs that legacy back-end pages use to open the Site
	 * Browser, preview a page and pick content: {@link WebKeys#VIEW_BROWSER_URL},
	 * {@link WebKeys#PREVIEW_PAGE_URL} and {@link WebKeys#VIEW_CONTENTS_URL}.
	 *
	 * @param hreq the current back-end request; its session receives the URLs
	 */
	public static void setVelocityURLS(HttpServletRequest hreq) {

		HttpSession session = hreq.getSession();

		String portletId=null;

		java.util.Map<String, String[]> params = new java.util.HashMap<>();
		params.put("struts_action",new String[] {"/ext/browser/view_browser"});
		portletId="site-browser";
		String viewBrowserURL = null;
		try {
			 viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params, portletId);
		} catch(Exception e) {
			viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params);
		}
		session.setAttribute(WebKeys.VIEW_BROWSER_URL, viewBrowserURL);

		portletId="site-browser";
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

	/**
	 * Sets the layout on the request and stores in the session the Site Browser portlet URLs of
	 * that layout: {@link WebKeys#VIEW_BROWSER_URL}, {@link WebKeys#PREVIEW_PAGE_URL} and
	 * {@link WebKeys#VIEW_CONTENTS_URL}.
	 *
	 * @param hreq the current back-end request; its session receives the URLs
	 * @param layout the layout the URLs belong to
	 */
	public static void setVelocityURLS(HttpServletRequest hreq, Layout layout) {
		hreq.setAttribute(WebKeys.LAYOUT, layout);
		HttpSession session = hreq.getSession();

		String portletId=null;
		java.util.Map<String, String[]> params = new java.util.HashMap<>();
		params.put("struts_action",new String[] {"/ext/browser/view_browser"});
		portletId="site-browser";
		String viewBrowserURL = com.dotmarketing.util.PortletURLUtil.getActionURL(hreq,WindowState.MAXIMIZED.toString(),params, portletId);
		//iewBrowserURL =(UtilMethods.isSet(hreq.getParameter("referer"))?hreq.getParameter("referer"):viewBrowserURL);
		session.setAttribute(WebKeys.VIEW_BROWSER_URL, viewBrowserURL);

		portletId="site-browser";
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
