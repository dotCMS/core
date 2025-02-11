/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;

import com.dotcms.enterprise.ParentProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.dashboard.business.DashboardAPI;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits;
import com.dotmarketing.portlets.dashboard.model.DashboardWorkStream;
import com.dotmarketing.portlets.dashboard.model.HostWrapper;
import com.dotmarketing.portlets.dashboard.model.ViewType;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class DashboardAjax extends ParentProxy{

	private DashboardAPI dashboardAPI = APILocator.getDashboardAPI();
	private UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPIImpl();


	private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
	private static final SimpleDateFormat fullDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/dd/yyyy");
	private static final SimpleDateFormat hourMinuteFormat = new SimpleDateFormat("HH:mm:ss");
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
	private static final SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMM d");
	private static final NumberFormat numberFormat = NumberFormat.getInstance();
	private static final int perPage = 10;
	private static final int perPageHosts = 20;
	private static final int perPageWs = 20;

	public Map<String, Object> getHostStatistics(String hostId, String viewType, boolean showIgnored) throws DotRuntimeException, PortalException, SystemException {

		Map<String, Object> results = new HashMap<>();

		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());

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

		DashboardSummary summary = new DashboardSummary();
		try {

			if(allowExecution()){	
				summary = dashboardAPI.getDashboardSummary(hostId, fromDate, toDate);

				if(summary!=null){
					results.put("visits", numberFormat.format(summary.getVisits()));
					results.put("pageViews", numberFormat.format(summary.getPageViews()));
					results.put("newVisits", numberFormat.format(summary.getNewVisits()));
					results.put("uniqueVisits", numberFormat.format(summary.getUniqueVisits()));
					results.put("bounceRate", summary.getBounceRate());
					String timeOnSite = "00:00:00";
					if(summary.getAvgTimeOnSite()!=null){
						timeOnSite = hourMinuteFormat.format(summary.getAvgTimeOnSite());
					}
					results.put("timeOnSite",timeOnSite );
					results.put("directTraffic", summary.getDirectTraffic());
					results.put("referringSites", summary.getReferringSites());
					results.put("searchEngines", summary.getSearchEngines());
					results.put("viewType", viewType);
				}
			}


			List<Map<String, String>> contentMapList = new ArrayList<>();
			if(allowExecution()){	
				List<DashboardSummaryContent> contents = dashboardAPI.getTopContent(hostId, fromDate, toDate, 5, 0, " sum(summaryContent.hits) desc ");
				Map<String,String> contentMap = new HashMap<>();
				for(DashboardSummaryContent content : contents){
					contentMap = new HashMap<>();
					contentMap.put("inode", content.getInode());
					contentMap.put("title", content.getTitle());
					String url = "";
					try{
						Contentlet contentlet = conAPI.findContentletByIdentifier(content.getInode(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), 
								APILocator.getUserAPI().getSystemUser(), false);
						url = conAPI.getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), false);
					}catch(Exception e){
						Logger.error(this, "Could not find contentlet with identifier = " + content.getInode());
					}
					contentMap.put("uri", url);
					contentMap.put("hits", numberFormat.format(content.getHits()));
					contentMapList.add(contentMap);
				}
			}
			results.put("topContent", contentMapList);


			List<Map<String, String>> pageMapList = new ArrayList<>();
			if(allowExecution()){	
				List<DashboardSummaryPage> pages = dashboardAPI.getTopPages(hostId, fromDate, toDate, 5, 0, " sum(summaryPage.hits) desc ");
				Map<String,String> pageMap = new HashMap<>();
				for(DashboardSummaryPage page : pages){
					pageMap = new HashMap<>();
					pageMap.put("inode", page.getInode());
					pageMap.put("uri", page.getUri());
					pageMap.put("hits", numberFormat.format(page.getHits()));
					pageMapList.add(pageMap);
				}
			}
			results.put("topPages", pageMapList);


			List<Map<String, String>> refererMapList = new ArrayList<>();
			if(allowExecution()){	
				List<DashboardSummaryReferer> referers = dashboardAPI.getTopReferers(hostId, fromDate, toDate, 11, 0, " sum(summaryRef.hits) desc ");
				Map<String,String> refererMap = new HashMap<>();
				for(DashboardSummaryReferer referer : referers){
					refererMap = new HashMap<>();
					refererMap.put("uri", referer.getUri());
					refererMap.put("hits", numberFormat.format(referer.getHits()));
					refererMapList.add(refererMap);
				}
			}
			results.put("topReferers", refererMapList);


			List<Map<String, String>> summary404MapList = new ArrayList<>();
			if(allowExecution()){	
				List<DashboardSummary404> summary404s = dashboardAPI.get404s(user.getUserId(), hostId, showIgnored, fromDate, toDate, 5, 0, " summary404.uri desc,summary404.refererUri asc  ");
				Map<String,String> summary404Map = new HashMap<>();
				for(DashboardSummary404 summary404 : summary404s){
					summary404Map = new HashMap<>();
					summary404Map.put("referer", summary404.getRefererUri()==null?"":summary404.getRefererUri());
					summary404Map.put("uri", summary404.getUri());
					summary404Map.put("ignored", String.valueOf(summary404.isIgnored()));
					summary404Map.put("id", String.valueOf(summary404.getId()));
					summary404MapList.add(summary404Map);
				}
			}
			results.put("summary404s", summary404MapList);

			if(vt.equals(ViewType.DAY)){
				Calendar c1 = Calendar.getInstance(); 
				c1.setTime(now);
				c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH), 23, 0);
				c1.add(Calendar.DATE,-2);
				fromDate = c1.getTime();
			}
			List<DashboardSummaryVisits> summaryVisits = new ArrayList<>();
			if(allowExecution()){	
				summaryVisits = dashboardAPI.getDashboardSummaryVisits(hostId, vt, fromDate, toDate);
				for(DashboardSummaryVisits visit : summaryVisits){
					if(vt.equals(ViewType.DAY)){
						visit.setFormattedTime(timeFormat.format(visit.getVisitTime()));
					}else {
						visit.setFormattedTime(monthDayFormat.format(visit.getVisitTime()));
					}
				}
			}
			results.put("summaryVisits", summaryVisits);


		} catch (Exception e){
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
		return results;

	}

	public Map<String, Object> getSummary404s(String hostId, String viewType, boolean showIgnored, int limit, int offset, String orderBy ) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException{

		Date now = new Date();
		Date fromDate = null;
		Date toDate = null;
		int pageNumber = offset;
		offset = (offset - 1) * perPage;

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


		Map<String, Object> results = new HashMap<>();

		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		List<Map<String, String>> summary404MapList = new ArrayList<>();
		if(allowExecution()){
			List<DashboardSummary404> summary404s = dashboardAPI.get404s(user.getUserId(), hostId, showIgnored, fromDate, toDate, limit, offset, orderBy);
			Map<String,String> summary404Map = new HashMap<>();
			for(DashboardSummary404 summary404 : summary404s){
				summary404Map = new HashMap<>();
				summary404Map.put("referer", summary404.getRefererUri()==null?"":summary404.getRefererUri());
				summary404Map.put("uri", summary404.getUri());
				summary404Map.put("ignored", String.valueOf(summary404.isIgnored()));
				summary404Map.put("id", String.valueOf(summary404.getId()));
				summary404MapList.add(summary404Map);
			}
		}
		results.put("summary404s", summary404MapList);
		long count =  0;
		if(allowExecution()){
			count = dashboardAPI.get404Count(user.getUserId(), hostId, showIgnored, fromDate, toDate);
		}
		results.put("summaryCount", count);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);
		return results;

	}

	public Map<String, Object> getTopPages(String hostId, String viewType,int limit, int offset, String orderBy ) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException{

		Date now = new Date();
		Date fromDate = null;
		Date toDate = null;
		int pageNumber = offset;
		offset = (offset - 1) * perPage;

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


		Map<String, Object> results = new HashMap<>();
		List<Map<String, String>> pageMapList = new ArrayList<>();
		if(allowExecution()){
			List<DashboardSummaryPage> pages = dashboardAPI.getTopPages(hostId, fromDate, toDate, limit, offset, orderBy);
			Map<String,String> pageMap = new HashMap<>();
			for(DashboardSummaryPage page : pages){
				pageMap = new HashMap<>();
				pageMap.put("inode", page.getInode());
				pageMap.put("uri", page.getUri());
				pageMap.put("hits", numberFormat.format(page.getHits()));
				pageMapList.add(pageMap);
			}
		}
		results.put("topPages", pageMapList);
		long count  = 0;
		if(allowExecution()){
			count =  dashboardAPI.getTopPagesCount(hostId, fromDate, toDate);
		}
		results.put("summaryCount",count);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);

		return results;

	}

	public Map<String, Object> getTopContent(String hostId, String viewType,int limit, int offset, String orderBy ) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException, DotSecurityException{

		Date now = new Date();
		Date fromDate = null;
		Date toDate = null;
		int pageNumber = offset;
		offset = (offset - 1) * perPage;

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

		Map<String, Object> results = new HashMap<>();
		List<Map<String, String>> contentMapList = new ArrayList<>();
		if(allowExecution()){
			List<DashboardSummaryContent> contents = dashboardAPI.getTopContent(hostId, fromDate, toDate, limit, offset, orderBy);
			Map<String,String> contentMap = new HashMap<>();
			for(DashboardSummaryContent content : contents){
				contentMap = new HashMap<>();
				contentMap.put("inode", content.getInode());
				contentMap.put("title", content.getTitle());
				String url = "";
				try{
					Contentlet contentlet = conAPI.findContentletByIdentifier(content.getInode(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), 
							APILocator.getUserAPI().getSystemUser(), false);
					url = conAPI.getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), false);
				}catch(Exception e){
					Logger.error(this, "Could not find contentlet with identifier = " + content.getInode());
				}
				contentMap.put("uri", url);
				contentMap.put("hits", numberFormat.format(content.getHits()));
				contentMapList.add(contentMap);
			}
		}
		results.put("topContent", contentMapList);
		long count = 0;
		if(allowExecution()){
			count =dashboardAPI.getTopContentCount(hostId, fromDate, toDate);
		}
		results.put("summaryCount",count);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);

		return results;

	}

	public Map<String, Object> getTopReferers(String hostId, String viewType,int limit, int offset, String orderBy ) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException{

		Date now = new Date();
		Date fromDate = null;
		Date toDate = null;
		int pageNumber = offset;
		offset = (offset - 1) * perPage;

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

		Map<String, Object> results = new HashMap<>();
		List<Map<String, String>> refererMapList = new ArrayList<>();
		if(allowExecution()){
			List<DashboardSummaryReferer> referers = dashboardAPI.getTopReferers(hostId, fromDate, toDate, limit, offset, orderBy);
			Map<String,String> refererMap = new HashMap<>();
			for(DashboardSummaryReferer referer : referers){
				refererMap = new HashMap<>();
				refererMap.put("uri", referer.getUri());
				refererMap.put("hits", numberFormat.format(referer.getHits()));
				refererMapList.add(refererMap);
			}
		}
		results.put("topReferers", refererMapList);
		long count = 0;
		if(allowExecution()){
			count =dashboardAPI.getTopReferersCount(hostId, fromDate, toDate);
		}
		results.put("summaryCount", count);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);

		return results;

	}



	public void setIgnore(String summaryId, boolean ignored) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException{
		if(allowExecution()){	
			WebContext ctx = WebContextFactory.get();
			User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
			dashboardAPI.setIgnored(user, Long.parseLong(summaryId), ignored);
		}

	}


	public Map<String, Object> getWorkStreams(String hostId, String userId, String fromDateStr, String toDateStr, int limit, int offset, String orderBy) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException{
		Map<String, Object> results = new HashMap<>();
		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		int pageNumber = offset;
		offset = (offset - 1) * perPageWs;
		Date fromDate = null;
		try{
			if(UtilMethods.isSet(fromDateStr)){
				fromDate = monthYearFormat.parse(fromDateStr);
			}
		}catch(ParseException pe){

		}
		Date toDate = null;
		try{
			if(UtilMethods.isSet(toDateStr)){
				toDate = monthYearFormat.parse(toDateStr);
				Calendar c1 = Calendar.getInstance(); 
				c1.setTime(toDate);
				c1.add(Calendar.DATE,1);
				toDate = c1.getTime();
			}
		}catch(ParseException pe){

		}
		List<Map<String, String>> workStreamMapList = new ArrayList<>();
		if(allowExecution()){
			List<DashboardWorkStream> workStreams = dashboardAPI.getWorkStreamList(user, hostId, userId, fromDate, toDate, limit, offset, orderBy);
			Map<String,String> workStreamMap = new HashMap<>();
			Contentlet con = null;
			Structure structure = null;
			String structureInode = "";
			for(DashboardWorkStream ws : workStreams){
				workStreamMap = new HashMap<>();
				workStreamMap.put("title", UtilMethods.isSet(ws.getName())?ws.getName():"");
				workStreamMap.put("hostname", UtilMethods.isSet(ws.getHost().getHostname())?ws.getHost().getHostname():"");
				workStreamMap.put("username", UtilMethods.isSet(ws.getModUser())?ws.getModUser().getFirstName() + " " + ws.getModUser().getLastName():"");
				workStreamMap.put("action", UtilMethods.isSet(ws.getAction())?ws.getAction():"");
				workStreamMap.put("mod_date", UtilMethods.isSet(ws.getModDate())?fullDateFormat.format(ws.getModDate()):"" );
				if(ws.getAssetType()!=null && ws.getAssetType().equals("contentlet")){
					try{
						con = conAPI.find(ws.getInode(), APILocator.getUserAPI().getSystemUser(), false);
						structure = con.getStructure();
					}catch(Exception e){}
					if(structure != null){
						structureInode = structure.getInode();
					}
				}
				workStreamMap.put("assetType", UtilMethods.isSet(ws.getAssetType())?ws.getAssetType():"");
				workStreamMap.put("structureInode", structureInode);
				workStreamMap.put("inode", UtilMethods.isSet(ws.getInode())?ws.getInode():"");
				workStreamMapList.add(workStreamMap);
			}

			results.put("wsCount", dashboardAPI.getWorkStreamListCount(user, hostId, userId, fromDate, toDate));
		}else{
			results.put("wsCount",0);
		}
		results.put("workStreams", workStreamMapList);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);

		return results;
	}


	public Map<String, Object> getHosts(List<String> formData, int limit, int offset,String orderBy) throws DotRuntimeException, PortalException, SystemException, DotHibernateException, DotDataException, DotSecurityException{

		int pageNumber = offset;
		offset = (offset - 1) * perPageHosts;
		Map<String,Object> contentletFormData = new HashMap<>();	
		Map<String, Object> params = new HashMap<>();
		String categories = "";
		String hostId = "";
		Map<String, Object> results = new HashMap<>();

		// Storing form data into map.
		for (Iterator iterator = formData.iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();			

			if (!com.dotmarketing.util.UtilMethods.isSet(element))
				continue;

			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));
			Object elementValue = element.substring(element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR) + WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR.length());

			if(!UtilMethods.isSet(elementName))
				continue;			

			if(!UtilMethods.isSet(elementValue))
				elementValue="";

			if(elementName.equals("categories")){
				categories+=(String)elementValue+",";
			}

			if(elementName.equals("dahboardHostSelector")){
				hostId = (String) elementValue;
				if(UtilMethods.isSet(hostId)){
					params.put("hostId", hostId);
				}
			}

			contentletFormData.put(elementName, elementValue);			

		}		
		List<Field> fields = com.dotmarketing.cache.FieldsCache.getFieldsByStructureVariableName("Host");
		for(Field field: fields){			
			if(DbConnectionFactory.isMsSql() && field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())){				
				if( orderBy.contains("contentlet."+field.getFieldContentlet()) && orderBy.contains("desc")){					
					orderBy = "CAST(contentlet."+field.getFieldContentlet()+" AS NVARCHAR(4000)) desc";
				}else if( orderBy.contains("contentlet."+field.getFieldContentlet()) && orderBy.contains("asc")){
					orderBy = "CAST(contentlet."+field.getFieldContentlet()+" AS NVARCHAR(4000)) asc";
				}				
			}
			if(field.isSearchable()){
				String searchField = (String) contentletFormData.get(field.getFieldContentlet());
				if(UtilMethods.isSet(searchField)){
					Object value = searchField;
					if(field.getFieldType().equals(Field.FieldType.DATE.toString()) || 
							field.getFieldType().equals(Field.FieldType.TIME.toString()) || 
							field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){

						Date dateValue = new Date();
						if(value instanceof String && value != null) {
							try{
								dateValue = fullDateFormat2.parse((String) value);
							}catch(ParseException pe){

							}
						} else if(value != null) {
							dateValue = (Date)value;
						}
						if(field.getFieldType().equals(Field.FieldType.TIME.toString())){
							Calendar cal = Calendar.getInstance();
							cal.setTime(dateValue);	
							if(DbConnectionFactory.isPostgres()){
								params.put("EXTRACT (HOUR FROM contentlet."+field.getFieldContentlet()+")", cal.get(Calendar.HOUR));
								params.put("EXTRACT (MINUTE FROM contentlet."+field.getFieldContentlet()+")",  cal.get(Calendar.MINUTE));
							}else if(DbConnectionFactory.isMySql()){
								params.put("HOUR(contentlet."+field.getFieldContentlet()+")", cal.get(Calendar.HOUR));
								params.put("MINUTE(contentlet."+field.getFieldContentlet()+")",  cal.get(Calendar.MINUTE));
							}else if(DbConnectionFactory.isOracle()){
								params.put("EXTRACT (HOUR FROM contentlet."+field.getFieldContentlet()+")", cal.get(Calendar.HOUR));
								params.put("EXTRACT (MINUTE FROM contentlet."+field.getFieldContentlet()+")",  cal.get(Calendar.MINUTE));
							}else if(DbConnectionFactory.isMsSql()){
								params.put("DATEPART(hour,contentlet."+field.getFieldContentlet()+")", cal.get(Calendar.HOUR));
								params.put("DATEPART(minute,contentlet."+field.getFieldContentlet()+")",  cal.get(Calendar.MINUTE));	
							}
						}else{
							params.put("contentlet."+field.getFieldContentlet(), dateValue);
						}
					}else if(field.getFieldType().equals(Field.FieldType.TEXT.toString()) || 
							field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) ||
							field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()) ||
							field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString()) ) {           
						boolean isNumber = (field.getFieldContentlet().startsWith(Field.DataType.INTEGER.toString()) || 
								field.getFieldContentlet().startsWith(Field.DataType.FLOAT.toString()));
						if(!isNumber){
							params.put(" lower(contentlet." +field.getFieldContentlet()+ ") like ?", value);
						}else{
							params.put("contentlet."+field.getFieldContentlet(), value);
						}
					}else{
						params.put("contentlet."+field.getFieldContentlet(), value);
					}

				}

			}

		}    

		String[] categoriesArr = categories.split(",");
		if(UtilMethods.isSet(categoriesArr)){
			String cats = "";
			int count = 0;
			for(String cat: categoriesArr){
				if(UtilMethods.isSet(cat)){
					cats+="'"+cat+"'";
					count++;
					if(count<categoriesArr.length){
						cats+=", ";
					}
				}
			}
			if(UtilMethods.isSet(cats)){
				params.put("categories", cats);
			}
		}

		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		List<Map<String, Object>> hostMapList = new ArrayList<>();
		List<Object> hostMapFieldsList = new ArrayList<>();
		Map<String,Object> hostMap = new HashMap<>();
		if(allowExecution()){
			List<Host> hosts =  dashboardAPI.getHostList(user, false, params, limit, offset, orderBy);
			for(Host host : hosts){
				hostMap = new HashMap<>();
				hostMapFieldsList = new ArrayList<>();
				HostWrapper hostWrapper = (HostWrapper)host;
				String pageViews = numberFormat.format(hostWrapper.getPageViews()) + " (" + ((Long.valueOf(hostWrapper.getPageViewsDiff()).intValue()>0)?("+" + hostWrapper.getPageViewsDiff()):hostWrapper.getPageViewsDiff()) +"%)"; 
				String inode = host.getInode();
				String hostName = (String)hostWrapper.getContentletMap().get("hostName");
				String status = host.isLive()?"Live":"Stopped";
				hostMap.put("inode", inode);
				hostMap.put("identifier", host.getIdentifier());
				hostMap.put("pageViews", pageViews);
				hostMap.put("hostName", hostName);
				hostMap.put("status", status);
				for(com.dotmarketing.portlets.structure.model.Field field: fields){
					if(field.isListed() ){
						for(Object key : hostWrapper.getContentletMap().keySet()) {
							if(key.equals(field.getVelocityVarName())){
								Object value = hostWrapper.getContentletMap().get(key);
								if(UtilMethods.isSet(value) && field.getFieldType().equals(Field.FieldType.TIME.toString())){
									Date dateValue = (Date)value; 
									value = String.valueOf(timeFormat.format(dateValue));
								}
								if(value==null) value="";
								hostMapFieldsList.add(value);
								break;
							}
						}
					}
				}
				hostMap.put("fields", hostMapFieldsList);
				hostMapList.add(hostMap);
			}
		}
		results.put("hosts", hostMapList);
		long count = 0;
		if(allowExecution()){
			count = dashboardAPI.getHostListCount(user, false, params);
		}
		results.put("hostCount", count);
		results.put("pageNumber", pageNumber);
		results.put("orderBy", orderBy);
		return results;
	}


	public Map<String, Object> generateDashboardData(int monthFrom, int monthTo, int yearFrom, int yearTo){
		Map<String,Object> generatorMap = new HashMap<>();   
		boolean isError = false;
		if(allowExecution()){
			WebContext ctx = WebContextFactory.get();
			HttpSession session = ctx.getSession();
			if(session!=null){
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.MONTH, -1);
				Calendar cal2 = Calendar.getInstance();
				cal2.setTime(new Date());
				cal2.add(Calendar.MONTH, 1);
				monthFrom = monthFrom==0?cal.get(Calendar.MONTH)+1:monthFrom;
				monthTo = monthTo==0?cal2.get(Calendar.MONTH)+1:monthTo;
				yearFrom = yearFrom==0?cal.get(Calendar.YEAR):yearFrom;
				yearTo = yearTo==0?cal2.get(Calendar.YEAR):yearTo;
				if((yearTo>yearFrom) || (yearTo==yearFrom && monthTo>monthFrom)){
					try{
						DashboardDataGeneratorImpl generator = new DashboardDataGeneratorImpl(monthFrom, yearFrom, monthTo, yearTo);
						generator.start();
						session.setAttribute("dashboardDataGenerator", generator);
					}catch(Exception e){
						session.removeAttribute("dashboardDataGenerator");
						isError = true;
						generatorMap.put("error", e.getMessage());
					}

				}else{
					isError = true;
					generatorMap.put("error","Please provide a valid date interval");
				}
			}

		}else{
			isError = true;
			generatorMap.put("error", "Not a valid license");
		}

		generatorMap.put("isError",isError);
		return generatorMap;
	}

	public void stopGenerator(){
		if(allowExecution()){	
			WebContext ctx = WebContextFactory.get();
			HttpSession session = ctx.getSession();
			if(session!=null){
				DashboardDataGeneratorImpl generator = null;
				if(session.getAttribute("dashboardDataGenerator")!=null){
					generator=(DashboardDataGeneratorImpl)session.getAttribute("dashboardDataGenerator");
					generator.setFlag(false);
					session.removeAttribute("dashboardDataGenerator");
				}
			}
		}

	}

	public Map<String, Object> getDataGeneratorProgress(){

		Map<String,Object> generatorMap = new HashMap<>();
		if(allowExecution()){
			WebContext ctx = WebContextFactory.get();
			HttpSession session = ctx.getSession();
			if(session!=null){
				DashboardDataGeneratorImpl generator = null;
				if(session.getAttribute("dashboardDataGenerator")!=null){
					generator=(DashboardDataGeneratorImpl)session.getAttribute("dashboardDataGenerator");
					generatorMap.put("finished", generator.isFinished());
					generatorMap.put("rowCount", generator.getRowCount());
					generatorMap.put("progress", generator.getProgress());
					generatorMap.put("errors", generator.getErrors().size());
					if(generator.isFinished()){
						session.removeAttribute("dashboardDataGenerator");
					}
				}else{
					generatorMap.put("rowCount", -1);
					generatorMap.put("progress", -1);
				}

			}
		}else{
			generatorMap.put("rowCount", -1);
			generatorMap.put("progress", -1);
		}
		return generatorMap;

	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
	}

}
