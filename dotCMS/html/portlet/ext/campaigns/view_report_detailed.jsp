<%@ include file="/html/portlet/ext/campaigns/init.jsp" %>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%
PermissionAPI perAPI = APILocator.getPermissionAPI();
com.dotmarketing.portlets.campaigns.model.Campaign c = (com.dotmarketing.portlets.campaigns.model.Campaign)  request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT);
int total = com.dotmarketing.factories.InodeFactory.countChildrenOfClass(c, com.dotmarketing.portlets.campaigns.model.Recipient.class);
int totalOpened = com.dotmarketing.portlets.campaigns.factories.RecipientFactory.getOpenedRecipientsByCampaign(c).size();
int totalErrors = com.dotmarketing.portlets.campaigns.factories.RecipientFactory.getRecipientsWithErrorsByCampaign(c).size();
int totalOpenedPercent =  (total ==0) ? 0 : ( totalOpened * 100 ) / total;
int totalUnopened = (total ==0) ? 0 : total - totalOpened - totalErrors;
int totalUnopenedPercent =  (total ==0) ? 0 : (totalUnopened * 100 ) / total;
int totalErrorsPercent =  (total == 0) ? 0 : (totalErrors * 100) / total;

int pageNumber = (request.getParameter("pageNumber") != null ? Integer.parseInt(request.getParameter("pageNumber")) : 0);
int pageSize = 10;

com.dotmarketing.portlets.communications.model.Communication comm = (com.dotmarketing.portlets.communications.model.Communication) com.dotmarketing.factories.InodeFactory.getChildOfClass(c, com.dotmarketing.portlets.communications.model.Communication.class);

String title ="Report for " +  c.getTitle() + " &nbsp;(" + request.getAttribute(com.dotmarketing.util.WebKeys.RECIPIENT_LIST_TITLE) + ")";
java.util.List recipients = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.RECIPIENT_LIST);
java.util.List clicks =  com.dotmarketing.portlets.campaigns.factories.ClickFactory.getClicksByParentOrderByCount(c);
boolean isCampaignManagerViewer = false;
String campaignManagerViewerRoleId = "";
try {
	Role campaignManagerViewerRole = APILocator.getRoleAPI().loadRoleByKey(com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_VIEWER"));
	campaignManagerViewerRoleId = campaignManagerViewerRole.getId();
}
catch (Exception e) {}
Role[] userRoles = (Role[])com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
for (int i = 0; i < userRoles.length; i++) 
	{
	Role userRole = (Role) userRoles[i];
	if (userRole.getId().equals(campaignManagerViewerRoleId)) 
	{
		isCampaignManagerViewer = true;
	}	
}

%>


<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.quartz.QuartzUtils"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="com.dotmarketing.quartz.CronScheduledTask"%><script language="Javascript">
	function copyCampaign() {
		var form = document.getElementById('fm');
		form.<portlet:namespace />cmd.value = 'resend';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
		submitForm(form);
	}
	function deleteCampaign() {
		var form = document.getElementById('fm');
		if(confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-campaign-(this-cannot-be-undone)")%>")){
			form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
			submitForm(form);
		}
	}
	function cancelEdit() {				
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:renderURL>';
	}
// START GRAPH FUNCTIONS

dojo.require("dojox.charting.Chart2D");
dojo.require("dojox.charting.themes.PlotKit.blue");
dojo.require("dojox.charting.widget.Legend");

// START LINE GRAPH
	dojo.addOnLoad(function(){

		var chart2 = new dojox.charting.Chart2D("simplepie");
			chart2.addPlot("default", {
				type: "Pie",
				fontColor: "grey",
				labels: false,
				radius: 100
			}
		);
		chart2.setTheme(dojox.charting.themes.PlotKit.blue);
		chart2.addSeries("Series 1", [
			<% if (totalOpened > 0) { %>
				{y: <%=totalOpened%>, text: "<%=LanguageUtil.get(pageContext, "Opened")%>"},
			<% } %>
			<% if (totalUnopened > 0) { %>
				{y: <%=totalUnopened%>, text: "<%=LanguageUtil.get(pageContext, "Unopened")%>"},
			<% } %>
			<% if (totalErrors > 0) { %>
				{y: <%=totalErrors%>, text: "<%=LanguageUtil.get(pageContext, "Bounces")%>"}
			<% } %>
		]);
		chart2.render();
		var legend2 = new dojox.charting.widget.Legend({chart: chart2, horizontal: true}, "legend2");
	});
</script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%=title%>" />
<html:form action='/ext/campaigns/edit_campaign' styleId="fm">
	<input type="hidden" name="inode" value="<%= String.valueOf(c.getInode()) %>">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="resend">
    <input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:renderURL>">


<% 
	boolean showTabs = true;
	if (c.getWasSent()) {
		showTabs = false;
	}
	else if (c.getIsRecurrent()) {
		if (com.dotmarketing.util.UtilMethods.isSet(c.getExpirationDate()) && c.getExpirationDate().before(new java.util.Date())) {
			showTabs = false;
		}
		else {
			CronScheduledTask scheduler = (CronScheduledTask)QuartzUtils.getScheduledTask(String.valueOf(c.getInode()), "Recurrent Campaign").get(0);
			String cronExpression = scheduler.getCronExpression();
			if (!com.dotmarketing.util.UtilMethods.isSet(cronExpression)) {
				showTabs = false;
			}
			else if (com.dotmarketing.util.UtilMethods.isSet(scheduler.getEndDate())) {
				Date endDate = scheduler.getEndDate();
				if (endDate.before(new Date())) {
					showTabs = false;
				}
			}
		}
	}
%>

<div class="buttonBoxRight">
	<% if (showTabs) { %>
		<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="tab" value="properties" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>" id="properties_link">
			<%=LanguageUtil.get(pageContext, "Campaign-Info")%>
		</a>
			
		<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="tab" value="permissions" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>" id="permissions_link">
			<%=LanguageUtil.get(pageContext, "permissions")%>
		</a>
		
		<% if (c.getIsRecurrent()) { %>
			<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="tab" value="recurrence" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>" id="recurrence_link">
		<% } else { %>
			<a href="#" id="recurrence_link">
		<% } %>
			<%=LanguageUtil.get(pageContext, "Recurrence")%>
		</a>
	<% } %>					
	<!-- <a href="#" id="reports_link">
		<%=LanguageUtil.get(pageContext, "Reports")%>
	</a>-->
</div>

<style>

	.dojoxLegendHorizontal{margin: 0 auto;border:none;}
	td.dojoxLegendIcon{padding:0 3px;border:none;}
	td.dojoxLegendText{padding:0 10px 0 0;border:none;}
</style>

<div class="buttonBoxLeft"><h3><%=LanguageUtil.get(pageContext, "Reports")%></h3></div>

<div class="shadowBoxLine" id="reports">
	<table class="listingTable">
		<tr>
			<td rowspan="7">
				<div id="simplepie" style="width:350px;height:230px;margin:0 auto;"></div>
				<div id="legend2"></div>
			</td>
			<th><%=LanguageUtil.get(pageContext, "Subject")%>:</th>
			<td colspan="2"> <%=com.dotmarketing.util.UtilMethods.webifyString(comm.getEmailSubject())%></td>
		</tr>
		<tr>
			<th><%=LanguageUtil.get(pageContext, "Sent-on")%>:</th>
			<td colspan="2"><%= com.dotmarketing.util.UtilMethods.dateToHTMLDate(c.getCompletedDate())%> @ <%= com.dotmarketing.util.UtilMethods.dateToHTMLTime(c.getCompletedDate())%></td>
		</tr>
		<tr>
			<th><%=LanguageUtil.get(pageContext, "From")%>:</th>
			<td colspan="2"><%=com.dotmarketing.util.UtilMethods.webifyString(comm.getFromName())%> (<%=comm.getFromName()%>)</td>
		</tr>

		<tr>
				<th width="20%"><%=LanguageUtil.get(pageContext, "Total-Recipients")%>:</th>
				<td width="60%"><%=total%></td>
				<td width="20%" style="text-align:center;white-space:nowrap;">
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_all" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="previewIcon">
						<%=LanguageUtil.get(pageContext, "view")%>
					</button>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_all" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>&child=true&csv=1&.csv'" iconClass="downloadIcon">
						<%=LanguageUtil.get(pageContext, "download")%>
					</button>
				</td>
			</tr>
	
			<tr>
				<th><%=LanguageUtil.get(pageContext, "Total-Opened")%>: </th>
				<td><%=totalOpened%> (<%=totalOpenedPercent%>%)</td>
				<td style="text-align:center;">
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_opened" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="previewIcon">
						<%=LanguageUtil.get(pageContext, "view")%>
					</button>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_opened" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>&child=true&csv=1&.csv'" iconClass="downloadIcon">
				 		<%=LanguageUtil.get(pageContext, "download")%>
					</button>
				 </td>
			</tr>
	
			<tr>
				<th><%=LanguageUtil.get(pageContext, "Total-Unopened")%>:  </th>
				<td><%=totalUnopened%> (<%=totalUnopenedPercent%>%)</td>
				<td style="text-align:center;">
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_unopened" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="previewIcon">
						<%=LanguageUtil.get(pageContext, "view")%>
					</button>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_unopened" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>&child=true&csv=1&.csv'" iconClass="downloadIcon">
						<%=LanguageUtil.get(pageContext, "download")%>
					</button>
				</td>
			</tr>
			
			<tr>
				<th><%=LanguageUtil.get(pageContext, "Total-Errors/Bounces")%>:  </th>
				<td><%=totalErrors%> (<%=totalErrorsPercent%>%)</td>
				<td style="text-align:center;">
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_errors" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="previewIcon">
						<%=LanguageUtil.get(pageContext, "view")%>
					</button>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_errors" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>&child=true&csv=1&.csv'" iconClass="downloadIcon">
						<%=LanguageUtil.get(pageContext, "download")%>
					</button>
				</td>
			</tr>
		
		<%if(clicks.size() > 0){%>
			<tr>
				<th nowrap># <%=LanguageUtil.get(pageContext, "clicks")%></td>
				<td><%=LanguageUtil.get(pageContext, "Link")%></td>
				<td nowrap><%=LanguageUtil.get(pageContext, "Report")%></td>
			</tr>
		<% } %>
		
		<%
		//get the queue's links
		Iterator j = clicks.iterator();
		while(j.hasNext()) {
			com.dotmarketing.portlets.campaigns.model.Click link = (com.dotmarketing.portlets.campaigns.model.Click) j.next();
			String reportHref=CTX_PATH +"?action=report&sub=viewLink&queueId=" + c.getInode() + "&clickId=" + link.getInode();%>

			<tr>
				<th><%=link.getClickCount()%></th>
				<td valign="top" style="word-Wrap:break-word; word-Break:break-all;">
					<% if (link.getLink().indexOf("http://") == -1 && link.getLink().indexOf("https://") == -1) { %>
						<a target="new" href="http://<%=com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getPortalURL()%><%=link.getLink()%>"><%=com.dotmarketing.util.UtilMethods.shortenString(link.getLink(),50)%></a>
					<% } else { %>
						<a target="new" href="<%=link.getLink()%>"><%=com.dotmarketing.util.UtilMethods.shortenString(link.getLink(),50)%></a>
					<% } %>
				</td>
				<td valign="top" nowrap>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_link" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /><portlet:param name="clickId" value="<%=String.valueOf(link.getInode())%>" /></portlet:actionURL>'" iconClass="previewIcon">
						<%=LanguageUtil.get(pageContext, "view")%>
					</button>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="view_link" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /><portlet:param name="clickId" value="<%=String.valueOf(link.getInode())%>" /></portlet:actionURL>&child=true&csv=1&.csv'" iconClass="downloadIcon">
						<%=LanguageUtil.get(pageContext, "download")%>
					</button>
				</td>
			</tr>
		<% } %>
	</table>
</div>

<%
	Iterator i = recipients.iterator();
	String cmd = request.getParameter("cmd");
	int size = total;
%>

<h3><%= request.getAttribute(com.dotmarketing.util.WebKeys.RECIPIENT_LIST_TITLE) %></h3>
<table class="listingTable">
    <tr>
    	<th width="30%" style="text-align:left;"><%=LanguageUtil.get(pageContext, "Name")%></th>
		<th style="text-align:left;"><%=LanguageUtil.get(pageContext, "Email")%></th>
		<th style="text-align:center;"><%=LanguageUtil.get(pageContext, "Sent")%></th>
		<th style="text-align:center;"><%=LanguageUtil.get(pageContext, "Opened")%></th>
		<th style="text-align:left;"><%=LanguageUtil.get(pageContext, "Last-Result")%></th>
	</tr>
	<% int k = 0; %>
	<% while(i.hasNext()){
		com.dotmarketing.portlets.campaigns.model.Recipient r = (com.dotmarketing.portlets.campaigns.model.Recipient) i.next();%>
		<% if((k >= pageNumber * pageSize) && (k < (pageNumber+1) * pageSize)){ %>
			<tr>
	        	<td width="20%"><%=r.getName()%> <%=r.getLastname()%></td>
	        	<td width="20%"><%-- if (!isCampaignManagerViewer) { --%>(<%=r.getEmail()%>)<%--} --%></td>
	        	<td width="10%"><%=com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getSent())%>&nbsp;<%=com.dotmarketing.util.UtilMethods.dateToHTMLTime(r.getSent())%></td>
	        	<td width="10%"><%=com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getOpened())%>&nbsp;<%=com.dotmarketing.util.UtilMethods.dateToHTMLTime(r.getOpened())%></td>
	        	<td width="40%"><%=com.dotmarketing.util.UtilMethods.webifyString(r.getLastMessage())%></td>
	        </tr>
			<% k++; %>
		<% } %>
	<% } %>
</table>


<div class="buttonRow">
	<div class="yui-gb">
		<div class="yui-u first" style="text-align:left;">
			<% i = recipients.iterator(); %>
			<% if (i.hasNext()) { %>
				<% cmd = request.getParameter("cmd"); %>
				<%  if(pageNumber != 0){ %>
					<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="<%=cmd%>" /><portlet:param name="pageNumber" value="<%=Integer.toString(pageNumber - 1)%>" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="previousIcon"><%=LanguageUtil.get(pageContext, "Previous")%></button>
				<% } %>
			<% } %>&nbsp;
		</div>
		
		<div class="yui-u">
			<button dojoType="dijit.form.Button" onClick="copyCampaign();" iconClass="repeatIcon"><%=LanguageUtil.get(pageContext, "Send-Again")%></button>
			<% if (perAPI.doesUserHavePermission(c,PermissionAPI.PERMISSION_WRITE,user)) { %>
				<button dojoType="dijit.form.Button" iconClass="deleteIcon" onClick="deleteCampaign();"><%=LanguageUtil.get(pageContext, "Delete-Campaign")%></button>
			<% } %>
		</div>

		<div class="yui-u first" style="text-align:right;">
			<%
				size = total;
				if(cmd.equals("view_opened")) {size = totalOpened;}
				else if((cmd.equals("view_unopened"))) {size = totalUnopened;}
				else if((cmd.equals("view_errors"))) {size = totalErrors;}
			%>
			<% if(((pageNumber + 1) * pageSize) < size){ %>
				<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_report" /><portlet:param name="cmd" value="<%=cmd%>" /><portlet:param name="pageNumber" value="<%=Integer.toString(pageNumber + 1)%>" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>'" iconClass="nextIcon"><%=LanguageUtil.get(pageContext, "Next")%></button>
			<%	} %>&nbsp;
		</div>
	</div>
</div>

</div>

</html:form>
</liferay:box>