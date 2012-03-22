
<%@page import="com.dotmarketing.business.Layout"%>
<%@ page import="com.liferay.portal.NoSuchUserException" %>
<%@ page import="com.liferay.portal.auth.PrincipalException" %>
<%@ page import="com.liferay.portal.ejb.AddressManagerUtil" %>
<%@ page import="com.liferay.portal.ejb.CompanyLocalManagerUtil" %>
<%@ page import="com.liferay.portal.ejb.PortletManagerUtil" %>
<%@ page import="com.liferay.portal.ejb.PortletPreferencesManagerUtil" %>
<%@ page import="com.liferay.portal.ejb.UserLocalManagerUtil" %>
<%@ page import="com.liferay.portal.model.*" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="com.liferay.portal.util.CookieKeys" %>
<%@ page import="com.liferay.portal.util.ImageKey" %>
<%@ page import="com.liferay.portal.util.LuceneFields" %>
<%@ page import="com.liferay.portal.util.OmniadminUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.util.PortletKeys" %>
<%@ page import="com.liferay.portal.util.Recipient" %>
<%@ page import="com.liferay.portal.util.RecipientComparator" %>
<%@ page import="com.liferay.portal.util.ReleaseInfo" %>
<%@ page import="com.liferay.portal.util.Resolution" %>
<%@ page import="com.liferay.portal.util.ShutdownUtil" %>
<%@ page import="com.liferay.portal.util.WebAppPool" %>
<%@ page import="com.liferay.portlet.CachePortlet" %>
<%@ page import="com.liferay.portlet.LiferayWindowState" %>
<%@ page import="com.liferay.portlet.PortletURLImpl" %>
<%@ page import="com.liferay.portlet.RenderParametersPool" %>
<%@ page import="com.liferay.portlet.RenderRequestImpl" %>
<%@ page import="com.liferay.portlet.RenderResponseImpl" %>
<%@ page import="com.liferay.portlet.admin.ejb.AdminConfigManagerUtil" %>
<%@ page import="com.liferay.portlet.admin.model.EmailConfig" %>
<%@ page import="com.liferay.portlet.admin.model.JournalConfig" %>
<%@ page import="com.liferay.portlet.admin.model.ShoppingConfig" %>
<%@ page import="com.liferay.portlet.admin.model.UserConfig" %>
<%@ page import="com.liferay.util.BrowserSniffer" %>
<%@ page import="com.liferay.util.CollectionFactory" %>
<%@ page import="com.liferay.util.CookieUtil" %>
<%@ page import="com.liferay.util.CreditCard" %>
<%@ page import="com.liferay.util.FileUtil" %>
<%@ page import="com.liferay.util.Html" %>
<%@ page import="com.liferay.util.Http" %>
<%@ page import="com.liferay.util.JS" %>
<%@ page import="com.liferay.util.KeyValuePair" %>
<%@ page import="com.liferay.util.KeyValuePairComparator" %>
<%@ page import="com.liferay.util.MathUtil" %>
<%@ page import="com.liferay.util.ObjectValuePair" %>
<%@ page import="com.liferay.util.OrderedProperties" %>
<%@ page import="com.liferay.util.ParamUtil" %>
<%@ page import="com.liferay.util.PhoneNumber" %>
<%@ page import="com.liferay.util.PropertiesUtil" %>
<%@ page import="com.liferay.util.ServerDetector" %>
<%@ page import="com.liferay.util.SimpleCachePool" %>
<%@ page import="com.liferay.util.SortedProperties" %>
<%@ page import="com.liferay.util.State" %>
<%@ page import="com.liferay.util.StateUtil" %>
<%@ page import="com.liferay.util.StringComparator" %>
<%@ page import="com.liferay.util.StringPool" %>
<%@ page import="com.liferay.util.TextFormatter" %>
<%@ page import="com.liferay.util.Time" %>
<%@ page import="com.liferay.util.UnicodeFormatter" %>
<%@ page import="com.liferay.util.Validator" %>
<%@ page import="com.liferay.util.Xss" %>
<%@ page import="com.liferay.util.cal.CalendarUtil" %>
<%@ page import="com.liferay.util.cal.Recurrence" %>
<%@ page import="com.liferay.util.lang.BooleanWrapper" %>
<%@ page import="com.liferay.util.lang.IntegerWrapper" %>
<%@ page import="com.liferay.util.log4j.Levels" %>
<%@ page import="com.liferay.util.lucene.Hits" %>
<%@ page import="com.liferay.util.servlet.DynamicServletRequest" %>
<%@ page import="com.liferay.util.servlet.SessionParameters" %>
<%@ page import="com.liferay.util.servlet.StringServletResponse" %>
<%@ page import="com.liferay.util.servlet.UploadException" %>

<%@ page import="java.io.ByteArrayInputStream" %>
<%@ page import="java.io.StringReader" %>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Currency" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.GregorianCalendar" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Stack" %>
<%@ page import="java.util.TimeZone" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.TreeSet" %>

<%@ page import="javax.portlet.PortletConfig" %>
<%@ page import="javax.portlet.PortletContext" %>
<%@ page import="javax.portlet.PortletException" %>
<%@ page import="javax.portlet.PortletMode" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.UnavailableException" %>
<%@ page import="javax.portlet.ValidatorException" %>
<%@ page import="javax.portlet.WindowState" %>

<%
String CTX_PATH = (String)application.getAttribute(WebKeys.CTX_PATH);
String CAPTCHA_PATH = (String)application.getAttribute(WebKeys.CAPTCHA_PATH);
String IMAGE_PATH = (String)application.getAttribute(WebKeys.IMAGE_PATH);

String contextPath = PropsUtil.get(PropsUtil.PORTAL_CTX);
if (contextPath.equals("/")) {
	contextPath = "";
}

String COMMON_IMG = contextPath + "/html/skin/image/common";
%>



<%


Company company = PortalUtil.getCompany(request);

User user = null;
try {
	user = PortalUtil.getUser(request);
}
catch (NoSuchUserException nsue) {
}

boolean signedIn = false;

if (user == null) {
	user = company.getDefaultUser();

	if ((user.isDottedSkins() || user.isRoundedSkins()) && BrowserSniffer.is_ns_4(request)) {

		// Netscape 4.x users should never see dotted or rounded skins

		user = (User)user.clone();

		user.setDottedSkins(false);
		user.setRoundedSkins(false);
	}
}
else {
	signedIn = true;
}

Locale locale = (Locale)session.getAttribute(org.apache.struts.Globals.LOCALE_KEY);
if (locale == null) {

	// Locale should never be null except when the TCK tests invalidate the session

	locale = user.getLocale();
}

TimeZone timeZone = user.getTimeZone();
if (timeZone == null) {
	timeZone = company.getTimeZone();
}

Layout layout = (Layout)request.getAttribute(WebKeys.LAYOUT);
Layout[] layouts = (Layout[])request.getAttribute(WebKeys.LAYOUTS);

String layoutId = null;
if (layout != null) {
	layoutId = layout.getId();
}

//String portletGroupId = PortalUtil.getPortletGroupId(layoutId);

int RES_NARROW = 0;
int RES_TOTAL = 0;
int RES_WIDE = 0;

//String resolution = user.getResolution();

//if (resolution.equals(Resolution.S1024X768_KEY)) {
//	if ((layout != null) && (layout.getNumOfColumns() == 3)) {
//		RES_NARROW = Resolution.S800X600_NARROW;
//		RES_TOTAL = Resolution.S1024X768_TOTAL;
//		RES_WIDE = Resolution.S800X600_WIDE;
//	}
//	else if ((layout != null) && (layout.getNumOfColumns() == 1)) {
//		RES_TOTAL = Resolution.S1024X768_TOTAL;
//		RES_WIDE = RES_TOTAL;
//	}
//	else {
//		RES_NARROW = Resolution.S1024X768_NARROW;
//		RES_TOTAL = Resolution.S1024X768_TOTAL;
//		RES_WIDE = Resolution.S1024X768_WIDE;
//	}
//}
/** else {
	if ((layout != null) && (layout.getNumOfColumns() == 3)) {
		RES_NARROW = Resolution.S800X600_NARROW;
		RES_TOTAL = Resolution.S1024X768_TOTAL;
		RES_WIDE = Resolution.S800X600_WIDE;
	}
	else if ((layout != null) && (layout.getNumOfColumns() == 1)) {
		RES_TOTAL = Resolution.S800X600_TOTAL;
		RES_WIDE = RES_TOTAL;
	}
	else {
		RES_NARROW = Resolution.S800X600_NARROW;
		RES_TOTAL = Resolution.S800X600_TOTAL;
		RES_WIDE = Resolution.S800X600_WIDE;
	}
}*/

%>






<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.language.LanguageWrapper" %>
<%@ page import="com.liferay.portal.language.UnicodeLanguageUtil" %>
<%@ page import="com.liferay.portal.util.PropsUtil" %>
<%@ page import="com.liferay.portal.util.WebKeys" %>
<%@ page import="com.liferay.util.GetterUtil" %>
<%@ page import="com.liferay.util.StringUtil" %>
<%@ page import="com.liferay.util.servlet.SessionErrors" %>
<%@ page import="com.liferay.util.servlet.SessionMessages" %>

<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>

