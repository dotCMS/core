package com.dotmarketing.portlets.dashboard.action;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.dashboard.business.DashboardAPI;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.ViewType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;

public class ViewDashboardAction extends DotPortletAction {

	private DashboardAPI dashboardAPI = APILocator.getDashboardAPI();
	
	private static final NumberFormat numberFormat = NumberFormat.getInstance();
	
	public ViewDashboardAction () throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		
	}

	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

		Logger.debug(this, "Running ViewDashboardAction!!!!");
		
		String cmd = req.getParameter(Constants.CMD);
		
		/*
		 * Viewing host report
		 */
		if ((cmd != null) && cmd.equals(Constants.VIEW_HOST_REPORT)) {
			    
			    return mapping.findForward("portlet.ext.dashboard.view_host_report");
		}
		
		/*
		 * Viewing Activity Streams
		 */
		if ((cmd != null) && cmd.equals(Constants.VIEW_ACTIVITY_STREAM)) {
			   
			   return mapping.findForward("portlet.ext.dashboard.view_workstream");
	    }
		
		/*
		 * Viewing Host Browser
		 */
		if ((cmd != null) && cmd.equals(Constants.VIEW_BROWSER)) {
				
			HttpServletRequest hreq = ((RenderRequestImpl)req).getHttpServletRequest();
			hreq.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, hreq.getParameter("hostId"));
			PreviewFactory.setVelocityURLS(hreq);
			return mapping.findForward("portlet.ext.browser.view_browser");

	    }
		
		
				  
		return mapping.findForward("portlet.ext.dashboard.view_dashboard");
	
		
	}
	
	
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		if ((cmd != null) && cmd.equalsIgnoreCase("export")) {

			try {
				String trending = req.getParameter("trending");
				String viewType =  req.getParameter("viewType");
				String hostId =  req.getParameter("hostId");
				User user = _getUser(req);
				ActionResponseImpl resImpl = (ActionResponseImpl) res;
				HttpServletResponse response = resImpl.getHttpServletResponse();			
				downloadToExcel(response, user, trending, viewType, hostId);
			} catch (Exception e) {
				Logger.warn(this, e.toString(), e);
				req.setAttribute(PageContext.EXCEPTION, e);
				req.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				req.setAttribute(com.liferay.portal.util.WebKeys.PORTLET_STRUTS_FORWARD,Constants.COMMON_ERROR);
			}
			_sendToReferral(req, res, referer);
		}

	}
	

	public void downloadToExcel(HttpServletResponse response, User user, String trending, String viewType, String hostId) throws DotSecurityException{

		String hostName = "";
		try {
			hostName = APILocator.getHostAPI().find(hostId, user, false).getHostname();
		} catch (DotDataException e) {			
			Logger.error(this,e.getMessage(),e);
		}
		Date now = new Date();
		Date fromDate = null;
		Date toDate = null;
		ViewType vt = ViewType.getViewType(viewType);

		if(vt.equals(ViewType.DAY)){
			Calendar c1 = Calendar.getInstance(); 
			c1.setTime(now);
			c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH), 0, 0);
			c1.add(Calendar.DATE,-2);
			fromDate = c1.getTime();

		}else if(vt.equals(ViewType.WEEK)){
			Calendar c1 = Calendar.getInstance(); 
			c1.setTime(now);
			c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH), 0, 0);
			c1.add(Calendar.DATE,-8);
			fromDate = c1.getTime();

		}else if(vt.equals(ViewType.MONTH)){
			Calendar c1 = Calendar.getInstance(); 
			c1.setTime(now);
			c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH), 0, 0);
			c1.add(Calendar.DATE,-31);
			fromDate = c1.getTime();

		}

		Calendar c2 = Calendar.getInstance(); 
		c2.setTime(now);
		c2.set(c2.get(Calendar.YEAR), c2.get(Calendar.MONTH), c2.get(Calendar.DAY_OF_MONTH), 0, 0);
		toDate = c2.getTime();

		List<Map<String, String>> trendingList = null;

		if(trending.equalsIgnoreCase("content")){

			trendingList = new ArrayList<Map<String, String>>();
			List<DashboardSummaryContent> contents = null;
			try{
				contents =  dashboardAPI.getTopContent(hostId, fromDate, toDate, 0, 0, " sum(summaryContent.hits) desc ");
				Map<String,String> contentMap = new HashMap<String,String>();
				for(DashboardSummaryContent content : contents){
					contentMap = new HashMap<String,String>();
					contentMap.put("inode", UtilMethods.isSet(content.getInode())?content.getInode():"");
					contentMap.put("title", UtilMethods.isSet(content.getTitle())?content.getTitle():"");
					Contentlet contentlet = null;
					try{
					   contentlet = APILocator.getContentletAPI().find(content.getInode(), APILocator.getUserAPI().getSystemUser(), false);
					}catch(Exception e){
						Logger.warn(this, e.toString(), e);
					}
					contentMap.put("uri", contentlet!=null?APILocator.getContentletAPI().getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), false):"");
					contentMap.put("hits", String.valueOf(content.getHits()));
					trendingList.add(contentMap);
				}
			}catch(Exception e){
				Logger.warn(this, e.toString(), e);
			}

		}else if(trending.equalsIgnoreCase("pages")){

			trendingList = new ArrayList<Map<String, String>>();
			List<DashboardSummaryPage> pages = null;
			try{
				pages = dashboardAPI.getTopPages(hostId, fromDate, toDate, 0, 0, " sum(summaryPage.hits) desc ");
				Map<String,String> pageMap = new HashMap<String,String>();
				for(DashboardSummaryPage page : pages){
					pageMap = new HashMap<String,String>();
					pageMap.put("inode", UtilMethods.isSet(page.getInode())?page.getInode():"");
					pageMap.put("uri", UtilMethods.isSet(page.getUri())?page.getUri():"");
					pageMap.put("hits",String.valueOf(page.getHits()));
					trendingList.add(pageMap);
				}
			}catch(Exception e){
				Logger.warn(this, e.toString(), e);
			}

		}else if(trending.equalsIgnoreCase("referers")){

			trendingList = new ArrayList<Map<String, String>>();
			List<DashboardSummaryReferer> referers = null;
			try{
				referers = dashboardAPI.getTopReferers(hostId, fromDate, toDate, 0, 0, " sum(summaryRef.hits) desc ");
				Map<String,String> refererMap = new HashMap<String,String>();
				for(DashboardSummaryReferer referer : referers){
					refererMap = new HashMap<String,String>();
					refererMap.put("uri", UtilMethods.isSet(referer.getUri())?referer.getUri():"");
					refererMap.put("hits",String.valueOf(referer.getHits()));
					trendingList.add(refererMap);
				}
			}catch(Exception e){
				Logger.warn(this, e.toString(), e);
			}

		}else if(trending.equalsIgnoreCase("404")){

			trendingList = new ArrayList<Map<String, String>>();	
			List<DashboardSummary404> summary404s = null;
			try{
				summary404s = dashboardAPI.get404s(user.getUserId(), hostId, true, fromDate, toDate, 0, 0, " summary404.uri desc, summary404.refererUri asc  ");
				Map<String,String> summary404Map = new HashMap<String,String>();
				for(DashboardSummary404 summary404 : summary404s){
					summary404Map = new HashMap<String,String>();
					summary404Map.put("referer", UtilMethods.isSet(summary404.getRefererUri())?summary404.getRefererUri():"");
					summary404Map.put("uri", UtilMethods.isSet(summary404.getUri())?summary404.getUri():"");
					summary404Map.put("ignored", String.valueOf(summary404.isIgnored()));
					summary404Map.put("id", String.valueOf(summary404.getId()));
					trendingList.add(summary404Map);
				}
			}catch(Exception e){
				Logger.warn(this, e.toString(), e);
			}
		}


		PrintWriter pr = null;
		try {
			response.setContentType("application/octet-stream; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\""+trending+"_"+viewType+"_" + UtilMethods.dateToHTMLDate(new java.util.Date(),"M_d_yyyy") +".csv\"");
			pr = response.getWriter();
				if(trending.equalsIgnoreCase("content")){
					pr.print("Host,Inode,Title,Hits");
					pr.print("\r\n");
					for(Map<String, String> map : trendingList){
						pr.print(hostName+","+map.get("inode").replaceAll("\r","")+"," + map.get("title").replaceAll("\r","")+","+map.get("hits").replaceAll("\r",""));
						pr.print("\r\n");
					}

				}else if(trending.equalsIgnoreCase("pages")){
					pr.print("Host,Inode,Uri,Hits");
					pr.print("\r\n");
					for(Map<String, String> map : trendingList){
						pr.print(hostName+","+map.get("inode").replaceAll("\r","")+"," + map.get("uri").replaceAll("\r","")+","+map.get("hits").replaceAll("\r",""));
						pr.print("\r\n");
					}

				}else if(trending.equalsIgnoreCase("referers")){
					pr.print("Host,Uri,Hits");
					pr.print("\r\n");
					for(Map<String, String> map : trendingList){
						pr.print(hostName+","+map.get("uri").replaceAll("\r","")+","+map.get("hits").replaceAll("\r",""));
						pr.print("\r\n");
					}


				}else if(trending.equalsIgnoreCase("404")){
					pr.print("Host,Referer,Uri,Ignored");
					pr.print("\r\n");
					for(Map<String, String> map : trendingList){
						pr.print(hostName+","+map.get("referer").replaceAll("\r","")+"," + map.get("uri").replaceAll("\r","")+","+map.get("ignored").replaceAll("\r",""));
						pr.print("\r\n");
					}
				}
			
			pr.flush();
			pr.close();
		}catch(Exception p){
			Logger.error(this,p.getMessage(),p);
		}
		
	}
		
}
