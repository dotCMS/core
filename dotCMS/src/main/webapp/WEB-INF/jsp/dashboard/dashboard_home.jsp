<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowTask"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.NoSuchUserException"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.business.Versionable"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.Map"%>



<%
User user = null;
try {
	user = PortalUtil.getUser(request);
} catch (NoSuchUserException nsue) {
	return;
}

DashboardAPI dAPI = APILocator.getDashboardAPI();
List<WorkflowTask> workflows = new java.util.ArrayList<WorkflowTask>();
List<Versionable> locked = new java.util.ArrayList<Versionable>();
RoleAPI rAPI = APILocator.getRoleAPI();
String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
HostAPI hAPI = APILocator.getHostAPI();
Host myHost =hAPI.find(hostId, user, false);
List<Map<String, Object>> referers = new java.util.ArrayList<Map<String, Object>>(); 

List hostList = dAPI.getHostList(user, false, null, 10, 0, "totalpageviews desc");
%>






<script language="Javascript">
//Layout Initialization
	function  resizeBrowser(){
		var viewport = dijit.getViewport();
		var viewport_height = viewport.h;
	
		var  e =  dojo.byId("borderContainer");
		dojo.style(e, "height", viewport_height -200+ "px");


	}
// need the timeout for back buttons

	dojo.addOnLoad(resizeBrowser);
	dojo.connect(window, "onresize", this, "resizeBrowser");
</script>

<div class="portlet-wrapper" >	
	<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:400px;" id="borderContainer" class="shadowBox headerBox">				
		

		
		<!-- START Right Column -->
		<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		
			<div class="contentWrapper" style="padding:5px;">
					<b>Dashboard</b>
			</div>
			<div style="padding:10px;padding-top:30px;float:left">Tasks:
				<table class="listingTable">
					<tr>
						<th>Tasks</th>
						<th>Assigned To</th>
						<th>Due</th>
					</tr>
					<%for(WorkflowTask wf : workflows){ %>
						<%if(wf.getDueDate() != null && wf.getDueDate().before(new Date())){ %>
							<tr style="background:pink">
						<%}else{ %>
							<tr>
						<%} %>
						    <td><%=wf.getTitle() %></td>
							<td><%=(wf.getAssignedTo().equals(user.getUserId())) ? "Me" : "role:" + rAPI.loadRoleById(wf.getBelongsTo()).getName() %></td>
							<td><%=UtilMethods.dateToHTMLDate(wf.getDueDate()) %></td>
						</tr>
					<%} %>
				</table>
			</div>
			<div style="padding:10px;padding-top:30px;float:left">
				Checked Out:
				<table class="listingTable">
					<tr>
						<th>Type</th>
						<th>Name</th>
						<th>Locked On</th>
					</tr>
					<%for(Versionable asset : locked){ %>
							<tr>
						    
							<td><%=asset.getClass().getSimpleName()%></td>
							<td><%=asset.getTitle() %></td>
							<td><%=UtilMethods.dateToHTMLDate(asset.getModDate()) %></td>
						</tr>
					<%} %>
				</table>
			</div>
			<div style="padding:10px;padding-top:30px;float:left">
				Today's Referers:
				<table class="listingTable">
					<tr>
						<th>Url</th>
						<th>Count</th>
					</tr>
					<%for(Map map : referers){ %>
							<tr>
						    
							<td><%=map.get("referer")%></td>
							<td><%=map.get("mycount") %></td>
						</tr>
					<%} %>
				</table>
			</div>
			<div style="padding:10px;padding-top:30px;float:left">
				
			</div>
		</div>
	
	</div>
</div>