<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.web.HostWebAPI"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.cache.VirtualLinksCache"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.URLUtils"%>
<%@page import="com.dotmarketing.util.URLUtils.ParsedURL"%>
<%@page import="com.dotmarketing.cache.WorkingCache"%>
<%@page import="com.liferay.util.ParamUtil"%>
<%@page import="com.dotmarketing.portlets.common.bean.CrumbTrailEntry"%>
<%@page import="java.util.ArrayList"%>

<%

	boolean inPopupIFrame = UtilMethods.isSet(ParamUtil.getString(request, "popup")) || UtilMethods.isSet(ParamUtil.getString(request, "in_frame"));

	if(!inPopupIFrame) {
		UserAPI userAPI = APILocator.getUserAPI();
		HostWebAPI hostApi = WebAPILocator.getHostWebAPI();
	
		Boolean dontDisplayAllHostsOption = (Boolean) request
				.getAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS);
		if (dontDisplayAllHostsOption == null) {
			dontDisplayAllHostsOption = false;
		}
	
		boolean showHostSelector = request.getAttribute("SHOW_HOST_SELECTOR") != null;
		String hostId = (String)request.getAttribute("_crumbHost");
	
		List<CrumbTrailEntry> crumbTrailEntries = (request
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS) != null) ? (List<CrumbTrailEntry>) request
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)
				: new ArrayList<CrumbTrailEntry>();
				
	
	    if(!UtilMethods.isSet(hostId)){
		   hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	    }
		Host currentHost = null;
		String hostName = null;
	
		try{
			currentHost = hostApi.find(hostId, user, false);
			hostName = currentHost.getTitle();
		}
		catch(Exception e){
			try{
				currentHost=hostApi.findDefaultHost(user, false);
				hostName = currentHost.getTitle();
			}
			catch(Exception ex){
				com.dotmarketing.util.Logger.error(this.getClass(), "user does not have a default host");
			}
		}
		
	
	
		String _browserCrumbUrl = null;
		boolean canManageHosts = false;
		String _hostManagerUrl = null;
	
		// if we have a host, get the url for the browser
		for (int i = 0; i < layouts.length; i++) {
			List<String> portletIDs = layouts[i].getPortletIds();
			for (String x : portletIDs) {
				if ("EXT_BROWSER".equals(x)) {
					_browserCrumbUrl = new PortletURLImpl(request, x, layouts[i].getId(), false).toString();
				}
				if ("EXT_HOSTADMIN".equals(x)) {
					canManageHosts = true;
					_hostManagerUrl = new PortletURLImpl(request, x, layouts[i].getId(), false).toString();
				}
			}
		}
	
	
	
	 if (showHostSelector) {
%>
<script type="text/javascript">
        dojo.require('dotcms.dojo.data.HostReadStore');
</script>

<!-- START Pop-up Host Select -->
<div id="hostSelectDialog" style="visibility: hidden; display: none;"
	title="<%= LanguageUtil.get(pageContext, "select-host") %>"
><%=LanguageUtil.get(pageContext, "select-host-nice-message")%>
<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>
<div style="text-align: center; padding: 15px;">
<div class="selectHostIcon"></div>
<select id="subNavHost" name=subNavHost" dojoType="dijit.form.FilteringSelect" 
	store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
	searchDelay="400" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>"
	onchange="updateCMSSelectedHosts()"
	>
</select>
</div>
<div style="text-align: right;">
<%
	if (canManageHosts) {
%>
<div
	style="float: left; font-size: 85%; padding-top: 7px; font-style: italic"
><a href="<%=_hostManagerUrl %>"><%=LanguageUtil.get(pageContext, "manage-hosts")%></a>
</div>
<%
	}
%>
<button dojoType="dijit.form.Button"
	onClick="dijit.popup.close(myDialog);" iconClass="cancelIcon"
><%=LanguageUtil.get(pageContext, "cancel")%></button>
</div>
</div>


<script type="text/javascript">
		var myDialog = new dijit.TooltipDialog({style:'display:none;'}, "hostSelectDialog");
		myDialog.startup();
	</script>
<!-- END Pop-up Host Select -->
<%
	}
%>

<%
	if (0 < crumbTrailEntries.size()) {
%>
<%
	boolean _amITheFirst = true;
%>
<div class="subNavCrumbTrail" id="subNavCrumbTrail">
<ul id="ulNav">
	<% if (!showHostSelector) { %>
	<% _amITheFirst = false; %>
		<li id="selectHostDiv" style=""
			<%if(UtilMethods.isSet(_browserCrumbUrl)){ %>
				onclick="window.location='<%=_browserCrumbUrl%>';" 
			<%} %>
		>
			<span class="hostStoppedIcon" style="float:left;margin-right:5px;"></span>
			<%=LanguageUtil.get(pageContext, "Global-Page")%>
		</li>
	<% } %>

	<% for (CrumbTrailEntry crumbTrailEntry : crumbTrailEntries) { %>
	<% if (UtilMethods.isSet(crumbTrailEntry.getLink())) { %>
		<li style="cursor: pointer" 
			<%if(_amITheFirst){%> id="selectHostDiv"<%} %>
		>
			<% if (_amITheFirst) { %> 
				<span class="publishIcon"></span> 
			<% } %> 
			<% _amITheFirst = false; %> 
			<a href="
				<%= crumbTrailEntry.getLink() %>"
			>
				<%=crumbTrailEntry.getTitle()%>
			</a>
		</li>
	<%
		} else {
	%>
	<li class="lastCrumb" id="lastCrumb"><span><%=crumbTrailEntry.getTitle()%></span></li>
	<%
		}
	%>
	<%
		}
	%>
</ul>
<%
	if (showHostSelector) {
%>
<div class="changeHost" onclick="dijit.popup.open({popup: myDialog, around: dojo.byId('changeHostId')})">
	<span id="changeHostId"><%=LanguageUtil.get(pageContext, "Change-Host")%></span>
	<span class="chevronExpandIcon"></span>
</div>
<%
	}
%>
<div class="clear"></div>

</div>
<%
	}
%>
<div class="clear"></div>



<script type="text/javascript">
		

	function showHostPreview() {
		window.location = '<%=_browserCrumbUrl%>';
	}
	function updateCMSSelectedHosts() {
		if( dijit.byId('subNavHost').attr('value')!=null && dijit.byId('subNavHost').attr('value')!=''){
			window.location.href = "/html/portlet/ext/common/sub_nav_refresh_host.jsp?referer=" + escape(window.location) + "&host_id=" + dijit.byId('subNavHost').attr('value');
		}
	}
	
</script>
<%
	}
%>

