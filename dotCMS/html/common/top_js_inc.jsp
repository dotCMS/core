<%@page import="com.dotmarketing.cms.factories.PublicCompanyFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.portal.language.UnicodeLanguageUtil"%>
<%@page import="com.liferay.util.ParamUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="java.util.Locale"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.NoSuchUserException"%>
<%
	HttpSession sess = request.getSession(false);
	Locale locale = null;
	User user = null;
	try {
		user = PortalUtil.getUser(request);
	} catch (NoSuchUserException nsue) {
	}
	

	if(sess != null){
	 	locale = (Locale) sess.getAttribute(org.apache.struts.Globals.LOCALE_KEY);
	}
	if (locale == null && user != null) {
		// Locale should never be null except when the TCK tests invalidate the session
		locale = user.getLocale();
	}
	if(locale ==null){
		locale = PublicCompanyFactory.getDefaultCompany().getLocale();
		
	}
%>

	var n1Portlets = new Array();
	var n2Portlets = new Array();
	var wPortlets = new Array();

	var CTX_PATH = '<%= application.getAttribute(WebKeys.CTX_PATH)%>';

	<%
	boolean inFrame = ParamUtil.get(request, "in_frame", false);
	inFrame = (request.getAttribute("in_frame") != null) ? true : inFrame;
	%>

<%-- 
	<c:if test="<%= !inFrame %>">
		if (window != top) {
			top.location.href = location.href;
		}
	</c:if>
--%>


	function submitFormAlert() {
		alert("<%= UnicodeLanguageUtil.get(pageContext, "this-form-has-already-been-submitted") %>");
	}

	<%
	String[] calendarDays = CalendarUtil.getDays(locale, "EEEE");
	%>

	Calendar._DN = new Array(
		"<%= calendarDays[0] %>",
		"<%= calendarDays[1] %>",
		"<%= calendarDays[2] %>",
		"<%= calendarDays[3] %>",
		"<%= calendarDays[4] %>",
		"<%= calendarDays[5] %>",
		"<%= calendarDays[6] %>",
		"<%= calendarDays[0] %>"
	);

	<%
	calendarDays = CalendarUtil.getDays(locale, "EEE");
	%>

	Calendar._SDN = new Array(
		"<%= calendarDays[0] %>",
		"<%= calendarDays[1] %>",
		"<%= calendarDays[2] %>",
		"<%= calendarDays[3] %>",
		"<%= calendarDays[4] %>",
		"<%= calendarDays[5] %>",
		"<%= calendarDays[6] %>",
		"<%= calendarDays[0] %>"
	);

	<%
	String[] calendarMonths = CalendarUtil.getMonths(locale);
	%>

	Calendar._MN = new Array(
		"<%= calendarMonths[0] %>",
		"<%= calendarMonths[1] %>",
		"<%= calendarMonths[2] %>",
		"<%= calendarMonths[3] %>",
		"<%= calendarMonths[4] %>",
		"<%= calendarMonths[5] %>",
		"<%= calendarMonths[6] %>",
		"<%= calendarMonths[7] %>",
		"<%= calendarMonths[8] %>",
		"<%= calendarMonths[9] %>",
		"<%= calendarMonths[10] %>",
		"<%= calendarMonths[11] %>"
	);

	<%
	calendarMonths = CalendarUtil.getMonths(locale, "MMM");
	%>

	Calendar._SMN = new Array(
		"<%= calendarMonths[0] %>",
		"<%= calendarMonths[1] %>",
		"<%= calendarMonths[2] %>",
		"<%= calendarMonths[3] %>",
		"<%= calendarMonths[4] %>",
		"<%= calendarMonths[5] %>",
		"<%= calendarMonths[6] %>",
		"<%= calendarMonths[7] %>",
		"<%= calendarMonths[8] %>",
		"<%= calendarMonths[9] %>",
		"<%= calendarMonths[10] %>",
		"<%= calendarMonths[11] %>"
	);

	Calendar._TT = {};

	Calendar._TT["ABOUT"] = "<%= LanguageUtil.get(pageContext, "date-selection") %>";
	Calendar._TT["ABOUT"] = Calendar._TT["ABOUT"].replace("{0}", String.fromCharCode(0x2039));
	Calendar._TT["ABOUT"] = Calendar._TT["ABOUT"].replace("{1}", String.fromCharCode(0x203a));

	Calendar._TT["ABOUT_TIME"] = "";
	Calendar._TT["CLOSE"] = "<%= LanguageUtil.get(pageContext, "close") %>";
	Calendar._TT["DAY_FIRST"] = "Display %s First";
	Calendar._TT["DRAG_TO_MOVE"] = "";
	Calendar._TT["GO_TODAY"] = "<%= LanguageUtil.get(pageContext, "today") %>";
	Calendar._TT["INFO"] = "<%= LanguageUtil.get(pageContext, "help") %>";
	Calendar._TT["NEXT_MONTH"] = "<%= LanguageUtil.get(pageContext, "next-month") %>";
	Calendar._TT["NEXT_YEAR"] = "<%= LanguageUtil.get(pageContext, "next-year") %>";
	Calendar._TT["PART_TODAY"] = "";
	Calendar._TT["PREV_MONTH"] = "<%= LanguageUtil.get(pageContext, "previous-month") %>";
	Calendar._TT["PREV_YEAR"] = "<%= LanguageUtil.get(pageContext, "previous-year") %>";
	Calendar._TT["SEL_DATE"] = "<%= LanguageUtil.get(pageContext, "select-date") %>";
	Calendar._TT["SUN_FIRST"] = "";
	Calendar._TT["TIME_PART"] = "";
	Calendar._TT["TODAY"] = "<%= LanguageUtil.get(pageContext, "today") %>";
	Calendar._TT["WK"] = "";

	Calendar._TT["DEF_DATE_FORMAT"] = "%Y-%m-%d";
	Calendar._TT["TT_DATE_FORMAT"] = "%a, %b %e";

	Calendar._TT["WEEKEND"] = "0,6";









	function showAboutDotCMSMessage(){
       var myDialog = dijit.byId("dotBackEndDialog");
       myDialog.titleNode.innerHTML="<%= LanguageUtil.get(pageContext, "about") %> dotCMS";
       dijit.byId("dotBackEndDialogCP").setHref("/html/portal/about.jsp");
       myDialog.show();
	}


	function showDisclaimerMessage(){
	       var myDialog = dijit.byId("dotBackEndDialog");
	       myDialog.titleNode.innerHTML="<%= UnicodeLanguageUtil.get(pageContext, "disclaimer") %>";
	       dijit.byId("dotBackEndDialogCP").setHref("/html/portal/disclaimer.jsp");
	       myDialog.show();
		}
	