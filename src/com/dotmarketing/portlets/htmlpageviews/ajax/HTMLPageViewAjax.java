package com.dotmarketing.portlets.htmlpageviews.ajax;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpageviews.factories.HTMLPageViewFactory;
import com.dotmarketing.portlets.htmlpageviews.factories.HTMLPageViewFactory.StatisticBetweenDates;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class HTMLPageViewAjax {

	private UserAPI userAPI = APILocator.getUserAPI();
	private IdentifierAPI idAPI = APILocator.getIdentifierAPI();

	public Map<String, Object> getPageStatistics(String htmlPageIdentifier, Date startDate, Date endDate) {

		Map<String, Object> results = new HashMap<String, Object>();

		try {
			User systemUser = userAPI.getSystemUser();

			Identifier id = idAPI.find(htmlPageIdentifier);

			String encodedURI = UtilMethods.encodeURIComponent(id.getURI()); // URIs
																				// are
			String hostId = id.getHostId();
			
			String viewBy = "DAY";
			List<StatisticBetweenDates> totalPageViewsByDay = null;
			List<StatisticBetweenDates> uniqueVisitorsByDay = null;
			List<StatisticBetweenDates> totalPageViewsByWeek = null;
			List<StatisticBetweenDates> uniqueVisitorsByWeek = null;
			List<StatisticBetweenDates> totalPageViewsByMonth = null;
			List<StatisticBetweenDates> uniqueVisitorsByMonth = null;

			long daysToLookup = (endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000);

			if (daysToLookup <= 35) {
				viewBy = "DAY";
				totalPageViewsByDay = HTMLPageViewFactory.getTotalHTMLPageViewsBetweenDatesGroupByDay(encodedURI, startDate, endDate, hostId);
				uniqueVisitorsByDay = HTMLPageViewFactory.getUniqueVisitorsBetweenDatesGroupByDay(encodedURI,startDate, endDate, hostId);
			} else if (daysToLookup > 150) {
				viewBy = "MONTH";
				totalPageViewsByMonth = HTMLPageViewFactory.getTotalHTMLPageViewsBetweenDatesGroupByMonth(encodedURI, startDate, endDate, hostId);
				uniqueVisitorsByMonth = HTMLPageViewFactory.getUniqueVisitorsBetweenDatesGroupByMonth(encodedURI,startDate, endDate, hostId);
			} else {
				viewBy = "WEEK";
				totalPageViewsByWeek = HTMLPageViewFactory.getTotalHTMLPageViewsBetweenDatesGroupByWeek(encodedURI, startDate, endDate, hostId);
				uniqueVisitorsByWeek = HTMLPageViewFactory.getUniqueVisitorsBetweenDatesGroupByWeek(encodedURI,startDate, endDate, hostId);				
			}
			
			
			int totalPageViews = HTMLPageViewFactory.getTotalHTMLPageViewsBetweenDates(encodedURI, startDate, endDate, hostId);
			int uniqueVisitors = HTMLPageViewFactory.getUniqueVisitorsBetweenDates(encodedURI, startDate, endDate, hostId);			
			List<Map<String, String>> totalPageViewsByLanguage = HTMLPageViewFactory.getTotalHTMLPageViewsByLanguageBetweenDates(encodedURI, startDate,
					endDate, hostId);
			int pageBounceRate = HTMLPageViewFactory.getPageBounceRate(encodedURI, startDate, endDate, hostId);
			long timeOnPage = HTMLPageViewFactory.getTimeOnPage(encodedURI, startDate, endDate, hostId);
			long pagesVisit = HTMLPageViewFactory.getPagesVisit(encodedURI, startDate, endDate, hostId);
			long pageExitRate = HTMLPageViewFactory.getPageExitRate(encodedURI, startDate, endDate, hostId);
			int searchEngineVisits = HTMLPageViewFactory.getSearchEngineVisits(encodedURI, startDate, endDate, hostId);
			int referringSitesVisits = HTMLPageViewFactory.getReferringSiteVisits(encodedURI, startDate, endDate, hostId);
			int directTrafficVisits = HTMLPageViewFactory.getDirectTrafficVisits(encodedURI, startDate, endDate, hostId);
			
			List<Map<String, String>> internalReferers = HTMLPageViewFactory.getTopInternalReferringPages(encodedURI, startDate, endDate, hostId);
			List<Map<String, String>> internalOutgoing = HTMLPageViewFactory.getTopInternalOutgoingPages(encodedURI, startDate, endDate, hostId);
			List<Map<String, String>> externalReferers = HTMLPageViewFactory.getTopExternalReferringPages(encodedURI, startDate, endDate, hostId);
			List<Map<String, String>> topUsers = HTMLPageViewFactory.getTopUsers(encodedURI, startDate, endDate, hostId);
			for (Map<String, String> user : topUsers) {
				if (UtilMethods.isSet(user.get("user_id"))) {
					try {
						User u = userAPI.loadUserById(user.get("user_id"), systemUser, false);
						user.put("user_full_name", u.getFullName());
						user.put("user_email", u.getEmailAddress());
					} catch (NoSuchUserException e) {
						user.put("user_full_name", "Unknown");
						user.put("user_email", "");
					}
				} else {
					user.put("user_full_name", "Unregistered Users");
					user.put("user_email", "");
				}
			}
			java.util.List<String> contentsInodesViews = HTMLPageViewFactory.getContentsInodesViewsBetweenDates(encodedURI, startDate, endDate, hostId);
			java.util.List<String> contentsInodesUniqueVisitors = HTMLPageViewFactory.getContentsInodesUniqueVisitorsBetweenDates(encodedURI, startDate,
					endDate, hostId);
			java.util.HashMap<String, Integer> countContentsInodesViews = _countNumEachLongFromList(contentsInodesViews);
			java.util.HashMap<String, Integer> countContentsInodesUniqueVisitors = _countNumEachLongFromList(contentsInodesUniqueVisitors);

			java.util.HashSet<String> contentsInodes = new java.util.HashSet<String>(contentsInodesViews);
			contentsInodes.addAll(contentsInodesUniqueVisitors);
			
			results.put("viewBy", viewBy);
			results.put("totalPageViews", totalPageViews);
			results.put("totalPageViewsByDay", totalPageViewsByDay);			
			results.put("uniqueVisitorsByDay", uniqueVisitorsByDay);
			results.put("totalPageViewsByWeek", totalPageViewsByWeek);			
			results.put("uniqueVisitorsByWeek", uniqueVisitorsByWeek);
			results.put("totalPageViewsByMonth", totalPageViewsByMonth);
			results.put("uniqueVisitors", uniqueVisitors);
			results.put("uniqueVisitorsByMonth", uniqueVisitorsByMonth);
			results.put("timeOnPage", timeOnPage);
			results.put("pageBounceRate", pageBounceRate);
			results.put("pagesVisit", pagesVisit);
			results.put("pageExitRate", pageExitRate);
			results.put("searchEngineVisits", searchEngineVisits);
			results.put("referringSitesVisits", referringSitesVisits);
			results.put("directTrafficVisits", directTrafficVisits);
			results.put("totalPageViewsByLanguage", totalPageViewsByLanguage);
			results.put("internalReferers", internalReferers);
			results.put("externalReferers", externalReferers);
			results.put("internalOutgoing", internalOutgoing);
			results.put("topUsers", topUsers);
			results.put("uri", id.getURI());

			results.put("contentsInodes", contentsInodes);
			results.put("countContentsInodesViews", countContentsInodesViews);
			results.put("countContentsInodesUniqueVisitors", countContentsInodesUniqueVisitors);

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage());
		}

		return results;

	}

	public List<User> getTopUsers(String htmlPageIdentifier, String startDateStr, String endDateStr) throws DotDataException {

		Date startDate = UtilMethods.htmlToDate(startDateStr);
		Date endDate = UtilMethods.htmlToDate(endDateStr);

		List<User> results = new ArrayList<User>();
		Identifier id = null;
		try {
			id = idAPI.find(htmlPageIdentifier);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.toString());
			return results;
		}

		List<Map<String, String>> users = (List<Map<String, String>>) HTMLPageViewFactory.getTopUsers(id.getURI(), startDate, endDate, id.getHostId());

		User sys = userAPI.getSystemUser();

		for (Map<String, String> m : users) {
			try {
				results.add(userAPI.loadUserById((String) m.get("user_id"), sys, false));
			} catch (Exception e) {
				Logger.error(this.getClass(), e.toString());
			}
		}
		return results;

	}

    public Map<String, Object> createMailingList(String pageIdentifier, Date startDate, Date endDate, String mailingListTitle, boolean allowPublicToSubscribe) throws Exception {

    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        
        //Saving mailing list
        MailingList ml = new MailingList();
        ml.setTitle(mailingListTitle);
        ml.setPublicList(allowPublicToSubscribe);
        ml.setUserId(user.getUserId());
        HibernateUtil.saveOrUpdate(ml);
        
        addToMailingList(pageIdentifier, startDate, endDate, ml.getInode());
        
        return ml.getMap();
        
    }
    
    public Map<String, Object> addToMailingList(String pageIdentifier, Date startDate, Date endDate, String mailingListInode) throws Exception {
        
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
    	MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
    	
        //Adding subscribers
        HTMLPage htmlPage = APILocator.getHTMLPageAPI().loadWorkingPageById(pageIdentifier, user, false);
        List<Map<String, String>> users = retrieveUsers(htmlPage, startDate, endDate, user);
        for (Map<String, String> userCounts : users) {
            if (userCounts.get("user_id") != null) {
                String userId = (String) userCounts.get("user_id");
                User webUser = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
                UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(webUser,APILocator.getUserAPI().getSystemUser(), false);
                MailingListFactory.addMailingSubscriber(ml, s, false);
            }
        }
        
        return ml.getMap();

    }

    public Map<String, Object> removeFromMailingList(String pageIdentifier, Date startDate, Date endDate, String mailingListInode) throws Exception {

    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
    	MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);

        //Removing subscribers
        HTMLPage htmlPage = APILocator.getHTMLPageAPI().loadWorkingPageById(pageIdentifier, user, false);
        List<Map<String, String>> users = retrieveUsers(htmlPage, startDate, endDate, user);
        for (Map<String, String> userCounts : users) {
            if (userCounts.get("user_id") != null) {
                String userId = (String) userCounts.get("user_id");
                User webUser = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
                UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(webUser,APILocator.getUserAPI().getSystemUser(), false);
                if (InodeUtils.isSet(s.getInode())) {
                	MailingListFactory.deleteUserFromMailingList(ml, s);
                }
            }
        }

        return ml.getMap();
        
    }
    
    private List<Map<String, String>> retrieveUsers(HTMLPage htmlPage, Date startDate, Date endDate, User user) throws DotDataException, DotSecurityException, PortalException, SystemException {

        Host host = APILocator.getHostAPI().findParentHost(htmlPage, user, false);
        String hostId = host.getIdentifier();
        String uri = APILocator.getIdentifierAPI().find(htmlPage).getURI();
        return HTMLPageViewFactory.getAllUsers(uri, startDate, endDate, hostId);

    }
    
	private java.util.HashMap<String, Integer> _countNumEachLongFromList(java.util.List<String> inodesList) {
		java.util.HashMap<String, Integer> result = new java.util.HashMap<String, Integer>();

		if (0 < inodesList.size()) {
			String lastInode = inodesList.get(0);
			int count = 1;
			String inode = "";

			for (int pos = 1; pos < inodesList.size(); ++pos) {
				inode = inodesList.get(pos);
				if (!lastInode.equalsIgnoreCase(inode)) {
					result.put(lastInode, count);
					lastInode = inode;
					count = 1;
				} else {
					++count;
				}
			}

			result.put(lastInode, count);
		}

		return result;
	}

}
