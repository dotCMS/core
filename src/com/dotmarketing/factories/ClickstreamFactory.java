package com.dotmarketing.factories;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.beans.BrowserSniffer;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.listeners.ClickstreamListener;
import com.dotmarketing.loggers.DatabaseClickstreamLogger;
import com.dotmarketing.util.BotChecker;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * Provides an entry point for interacting with {@link Clickstream} objects.
 * Each {@link Clickstream} object represents a visit from a given user to a
 * site, and allow to generate site statistics regarding the most-visited pages,
 * site visits, etc. that can be seen in the Dashboard portlet.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ClickstreamFactory {

	public static final String CLICKSTREAM_SESSION_ATTR_KEY = "clickstream";

	/**
	 * Adds a new request to the stream of clicks. The HttpServletRequest is
	 * converted to a ClickstreamRequest object and added to the clickstream.
	 *
	 * @param request
	 *            - The servlet request to be added to the clickstream.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
	public static Clickstream addRequest(HttpServletRequest request, HttpServletResponse response,
			Host host) throws DotDataException {

		if(request.getAttribute("CLICKSTREAM_RECORDED")!=null){
			return (Clickstream) request.getSession().getAttribute("clickstream");
		}
		request.setAttribute("CLICKSTREAM_RECORDED", true);

		String pointer = (String) request.getAttribute("javax.servlet.forward.request_uri");
		if(pointer ==null)pointer=request.getRequestURI();

		HttpSession session = request.getSession();
		
		Clickstream clickstream = (Clickstream) request.getSession(true).getAttribute("clickstream");
		if (clickstream == null) {
			clickstream = new Clickstream();
			session.setAttribute("clickstream", clickstream);
		}

		String associatedIdentifier = request.getParameter("id");
		if (!UtilMethods.isSet(associatedIdentifier)) {
			associatedIdentifier = (String) request.getAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE);
		}
		if (!UtilMethods.isSet(associatedIdentifier)) {

			// Maybe is a problem with the URL, so we need to find it
			// in other place "request.getRequestURI()"
			String uri = "";
			try {
				uri = URLDecoder.decode(request.getRequestURI(),
						UtilMethods.getCharsetConfiguration());
			} catch (UnsupportedEncodingException e) {
				Logger.debug(ClickstreamFactory.class,
						"Could not retrieve URI from request.");
			}
			if (!UtilMethods.isSet(uri)) {
				uri = pointer;
			}

			associatedIdentifier = APILocator.getIdentifierAPI().find(host, uri).getInode();
		}

		if (UtilMethods.isSet(associatedIdentifier)) {
			clickstream.setLastPageId(associatedIdentifier);
			clickstream.setLastRequest(new Date());
		}

		if (clickstream.getHostname() == null) {
			clickstream.setHostname(request.getRemoteHost());
		}
		if (clickstream.getRemoteAddress() == null) {
			try {
				InetAddress address = HttpRequestDataUtil.getIpAddress(request);
				if (UtilMethods.isSet(address)) {
					clickstream.setRemoteAddress(address.getHostAddress());
				}
			} catch (UnknownHostException e) {
				Logger.debug(ClickstreamFactory.class, "Could not retrieve IP address from request.");
			}
		}
		// Setup initial referrer
		if (clickstream.getInitialReferrer() == null) {
			if (request.getHeader("Referer") != null) {
				clickstream.setInitialReferrer(request.getHeader("Referer"));
			} else {
				clickstream.setInitialReferrer("");
			}
		}
		// if this is the first request in the click stream
		if (clickstream.getClickstreamRequests().size() == Config.getIntProperty("MIN_CLICKSTREAM_REQUESTS_TO_SAVE", 2)) {
			if (request.getHeader("User-Agent") != null) {
				clickstream.setUserAgent(request.getHeader("User-Agent"));
			} else {
				clickstream.setUserAgent("");
			}
			BrowserSniffer bs = new BrowserSniffer(request.getHeader("User-Agent"));
			session.setAttribute("browserSniffer", bs);
			clickstream.setBrowserName(bs.getBrowserName());
			clickstream.setOperatingSystem(bs.getOS());
			clickstream.setBrowserVersion(bs.getBrowserVersion());
			clickstream.setMobileDevice(bs.isMobile());
			clickstream.setBot(BotChecker.isBot(request));
			clickstream.setFirstPageId(associatedIdentifier);
			clickstream.setHostId(host.getIdentifier());

		}

		// Set the cookie id to the long lived cookie
		if (!UtilMethods.isSet(clickstream.getCookieId())) {

			String _dotCMSID = "";
			if(!UtilMethods.isSet(UtilMethods.getCookieValue(request.getCookies(),
					com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE))) {
				CookieUtil.createCookie();

			}
			_dotCMSID = UtilMethods.getCookieValue(request.getCookies(),
					com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
			clickstream.setCookieId(_dotCMSID);
		}

		// set the user if we have it
		if (session.getAttribute(WebKeys.CMS_USER) != null && clickstream.getUserId() == null) {
			User user = (User) session.getAttribute(WebKeys.CMS_USER);
			setClickStreamUser(user.getUserId(), request);
		}

		ClickstreamRequest cr = ClickstreamRequestFactory.getClickstreamRequest(request, clickstream.getLastRequest());
		clickstream.setNumberOfRequests(clickstream.getNumberOfRequests() + 1);
		cr.setRequestOrder(clickstream.getNumberOfRequests());


		cr.setHostId(host.getIdentifier());
		cr.setAssociatedIdentifier(associatedIdentifier);


		// prevent dupe entries into the clickstream table - just retun if the user is on the same page
		if(clickstream.getClickstreamRequests() != null &&clickstream.getClickstreamRequests().size()>0){
			ClickstreamRequest last = clickstream.getClickstreamRequests().get(clickstream.getClickstreamRequests().size()-1);
			if(last != null && cr.getAssociatedIdentifier().equals( last.getAssociatedIdentifier())){
				return clickstream;
			}
		}

		clickstream.addClickstreamRequest(cr);
		return clickstream;



	}

	/**
	 * This method forces a clickstream save
	 *
	 * @param stream
	 */
	public static void flushClickStream(Clickstream stream) {
		if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
			
			int minToLog = Config.getIntProperty("MIN_CLICKSTREAM_REQUESTS_TO_SAVE", 2);
			
			DatabaseClickstreamLogger dblogger = new DatabaseClickstreamLogger();
			try {
				if (stream != null 
						&& stream.getClickstreamRequests() != null 
						&& stream.getClickstreamRequests().size() >= minToLog) {
					dblogger.log(stream);
				}
			} catch (Exception e) {
				Logger.error(ClickstreamListener.class, e.getMessage(), e);
			}
		}

	}

	public static void save(Clickstream clickstream) {
		try {
			HibernateUtil.saveOrUpdate(clickstream);
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamFactory.class, "Save Failed:" + e, e);
		}
	}

	public static Clickstream getClickstream(String clickstreamId) {
		HibernateUtil dh = new HibernateUtil(Clickstream.class);
		Clickstream clickStream = new Clickstream();
		try {
			dh.setQuery("from clickstream in class " + Clickstream.class.getName() + " where clickstream_id = ?");
			dh.setParam(Integer.parseInt(clickstreamId));
			clickStream = (Clickstream) dh.load();
		} catch (NumberFormatException e) {
			Logger.error(ClickstreamFactory.class, e.getMessage(), e);
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamFactory.class, "getClickstream failed:" + e, e);
		}
		return clickStream;

	}

	@SuppressWarnings("unchecked")
	public static List<Clickstream> getClickstreamsByCookieId(String cookieId) {
		HibernateUtil dh = new HibernateUtil(Clickstream.class);
		List<Clickstream> list = new ArrayList<Clickstream>();
		try {
			dh.setQuery("from clickstream in class " + Clickstream.class.getName() + " where cookie_id = ?");
			dh.setParam(cookieId);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamFactory.class, "getClickstreamsByCookieId failed:" + e, e);
		}
		return list;

	}

	/**
	 * This method sets the user for the current clickstream on login.
	 *
	 * @param userId
	 *            the
	 * @param request
	 */

	public static void setClickStreamUser(String userId, HttpServletRequest request) {
		if (!UtilMethods.isSet(userId))
			return;

		HttpSession session = ((HttpServletRequest) request).getSession();
		Clickstream clickstream = null;
		if (session.getAttribute("clickstream") == null) {
			clickstream = new Clickstream();
		} else {
			clickstream = (Clickstream) session.getAttribute("clickstream");
		}

		// overwrite if user is not already set
		if (!UtilMethods.isSet(clickstream.getUserId())) {
			clickstream.setUserId(userId);
		}
		session.setAttribute("clickstream", clickstream);
	}

	public static void add404Request(HttpServletRequest request, HttpServletResponse response,
			Host host) throws DotStateException, DotDataException {

		com.liferay.portal.model.User user = null;
		try {
			user = (com.liferay.portal.model.User) request.getSession()
					.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
		} catch (Exception nsue) {
			Logger.warn(ClickstreamFactory.class, "Exception trying to getUser: "
					+ nsue.getMessage(), nsue);
		}

		boolean clickstreamEnabled = false;
		if (user != null) {
			UserProxy userProxy = null;
			try {
				userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!userProxy.isNoclicktracking()) {
				clickstreamEnabled = true;
			}
		}else{
			clickstreamEnabled = true;

		}

		if(clickstreamEnabled){
			Clickstream404 clickstream404 = new Clickstream404();
			clickstream404.setRefererURI(request.getHeader("Referer"));
			String uri = (String)request.getAttribute("javax.servlet.forward.request_uri");
			String queryString = (String)request.getAttribute("javax.servlet.forward.query_string");
			if(request.getAttribute(WebKeys.CLICKSTREAM_URI_OVERRIDE) != null){
				uri = (String) request.getAttribute(WebKeys.CLICKSTREAM_URI_OVERRIDE);
			}
			clickstream404.setRequestURI(uri);
			clickstream404.setQueryString(queryString);

			clickstream404.setHostId(host.getIdentifier());
			Clickstream clickstream = (Clickstream) request.getSession().getAttribute("clickstream");
			if(clickstream==null){
				clickstream = addRequest(request, response, host);
			}
			if (user != null && clickstream.getUserId() == null) {
				clickstream404.setUserId(user.getUserId());
			}
			clickstream404.setTimestamp(clickstream.getLastRequest());
			clickstream.addClickstream404(clickstream404);
		}

	}

	public static void save404(Clickstream404 clickstream404) {
		try{
	    	HibernateUtil.saveOrUpdate(clickstream404);
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamFactory.class, "save: failed", e);
		}
	}

}