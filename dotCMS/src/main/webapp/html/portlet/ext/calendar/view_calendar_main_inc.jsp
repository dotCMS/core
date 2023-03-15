<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.calendar.business.EventAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>

<%
	GregorianCalendar today = new GregorianCalendar();

	int daysInMonth = today.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
	int currentDay = today.get(GregorianCalendar.DATE);
	int month = today.get(GregorianCalendar.MONTH);
	String monthName = new SimpleDateFormat("MMMM").format(today.getTime());
	int year = today.get(GregorianCalendar.YEAR);

	GregorianCalendar temp = new GregorianCalendar();
	temp.setTime(today.getTime());
	temp.set(GregorianCalendar.DATE, 1);
	Date firstDayOfMonth = temp.getTime();
	int firstDayOfWeek = temp.get(GregorianCalendar.DAY_OF_WEEK);
	temp.set(GregorianCalendar.DATE, daysInMonth);
	Date lastDayOfMonth = new Date(year, month, daysInMonth);
	
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/calendar/view_calendar"});
	
	String referer = PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	
	EventAPI evAPI = APILocator.getEventAPI();
	Structure eventStructure = evAPI.getEventStructure();
	
%>

<%@ include file="view_calendar_js_inc.jsp" %>

<%@page import="com.dotmarketing.business.PermissionAPI"%>

<script type='text/javascript' src='/dwr/interface/CalendarAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>

<%--jsp:include page="/html/portlet/ext/folders/view_folders_js.jsp" / --%>


<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="calendar-events">
	
	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" class="portlet-sidebar-wrapper" style="width:280px">
		<div id="sideMenuWrapper" class="portlet-sidebar">
			<div id="calendarNavigation">
				<%@ include file="view_calendar_navigation.jsp" %>
				<div class="clear">&nbsp;</div>
			</div>
		</div>
	</div>
	<!-- END Left Column -->
	
	<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">

		<div class="portlet-main">
			<!-- START Toolbar -->
			<div class="portlet-toolbar">
				<div class="portlet-toolbar__actions-primary">
					<div class="inline-form">
						<input type="text" dojoType="dijit.form.TextBox" style="width:250px;" name="keywordBox" id="keywordBox" />
						<button dojoType="dijit.form.Button" id="moreOptionsButton" onclick="addKeyword()">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View")) %>
						</button>
						<button dojoType="dijit.form.Button" id="addFilterButton" onclick="addKeyword()">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Filter")) %>
						</button>
					</div>
				</div>
				<div class="portlet-toolbar__info">

				</div>
		    	<div class="portlet-toolbar__actions-secondary">
					<div class="inline-form">
						<button dojoType="dijit.form.Button" onclick="changeCalendarView('list');" iconClass="calListIcon">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "List-View")) %>
						</button>

						<button dojoType="dijit.form.Button" onclick="changeCalendarView('weekly');" iconClass="calWeekIcon">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Weekly-View")) %>
						</button>

						<button dojoType="dijit.form.Button" onclick="changeCalendarView('monthly');" iconClass="calMonthIcon">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Monthly-View")) %>
						</button>
						<%
							PermissionAPI permissionAPI = APILocator.getPermissionAPI();
							if(permissionAPI.doesUserHavePermission(eventStructure,PermissionAPI.PERMISSION_WRITE,user,false)){
						%>
							<button dojoType="dijit.form.Button" id="addEventBtn" onclick="addEvent();" class="dijitButtonAction">
								<%= LanguageUtil.get(pageContext, "add-event") %>
							</button>
						<% } %>
					</div>
		    	</div>
		   </div>
		   <!-- END Toolbar -->
				
			<div id="calendarSection">
				<%@ include file="view_calendar_list_view.jsp" %>
				<%@ include file="view_calendar_monthly_view.jsp" %>
				<%@ include file="view_calendar_weekly_view.jsp" %>

				<div id="loadingView" style="display: none;">
					<img src="/html/images/icons/round-progress-bar.gif" /><br/>
					<%= LanguageUtil.get(pageContext, "Loading") %>...
				</div>
			</div>
		</div>
	</div>
	<!-- END Right Column -->

</div>

<!-- START FILTER POPUP -->
<div id="filtersBox" class="filtersBox shadowBox" style="display: none;">
	<div>
		<div id="closeFiltersButton" class="closeFilter" onclick="closeFiltersBox()"></div>
		<div class="clear"></div>
	</div>

	<div class="yui-g lineCenter">
		<div class="yui-u first">
			<div id="categoriesFilterBox"></div>
		</div>
		<div class="yui-u">
			<div id="tagsFilterBox"><b><%= LanguageUtil.get(pageContext, "Filter-by-tag") %>:</b>
		</div>
	</div>
</div>
<!-- END FILTER POPUP -->

<!-- START EVENT DETAIL POPUP -->
<div id="eventDetail" dojoType="dijit.Dialog" style="display: none; padding-top:20px\9;">
	<div class="eventDetail" style="width: 650px">
		<div id="eventDetailDate" style="padding:5px;padding-left:10px;"></div>
		
		<div id="showLocation" style="display:none;">
			<hr/>
			<div id="eventDetailLocation"><%= LanguageUtil.get(pageContext, "Location") %>:</div>
		</div>
		<div id="eventDetailRating"></div>
		<div id="eventDetailComments"></div>
		<div id="eventDetailTags"></div>
		<div id="eventDetailCategories"></div>
		<div id="eventDetailDescription" style="height:250px;margin:10px;padding:10px;overflow:auto;border:1px solid silver;"></div>
		<div align="center" style="padding:10px;">
			<div id="eventDetailActions"></div>
		</div>
	</div>
</div>
<!-- END EVENT DETAIL POPUP -->

<!-- END EVENT UNPUBLISH ERROR DIALOG -->
<div id="eventUnpublishErrors" style="display: none;" dojoType="dijit.Dialog">
	<div dojoType="dijit.layout.ContentPane" id="eventUnpublishExceptionData" hasShadow="true"></div>
	<div class="formRow" style="text-align:center">
		<button dojoType="dijit.form.Button"  onClick="dijit.byId('eventUnpublishErrors').hide()" type="button"><%= LanguageUtil.get(pageContext, "close") %></button>
	</div>
</div>
<!-- END EVENT UNPUBLISH ERROR DIALOG -->

<div id="recEventDetail" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Edit-event") %>" style="display: none; zIndex: '400000';">
  <div class="eventDetail">
     <div align="center">
        <div><%= LanguageUtil.get(pageContext, "Edit-event-message") %></div>
        <br></br>
		<div id="recEventDetailActions"></div>
	</div>
  </div>
</div>

<script type="text/javascript">
	dojo.addOnLoad(function () {
		initializeCalendar();
	});
</script>

<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("keywordBox"));
		  t.stop();
		}
		t.start();
	});
</script> 