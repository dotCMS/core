<META HTTP-EQUIV="Refresh";URL=http://www.s.it/p.htm">
<%@page import="com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step"%>
<%@ include file="/html/common/init.jsp" %>
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


<script language="Javascript">

dojo.require("dijit.form.FilteringSelect");
dojo.require("dotcms.dojo.data.RoleReadStore");
dojo.require("dotcms.dojo.data.RoleReadStore");
dojo.require("dojox.layout.ContentPane");

function checkAll(){
	var x = dijit.byId("checkAllCkBx").checked;
	
	dojo.query(".taskCheckBox").forEach(function(node){
		dijit.byNode(node).setValue(x);
	})
}


function submitFrm() {
	
	var form = document.getElementById('sb');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/escalation/RunJobEsc" /></portlet:actionURL>';
	
	form.submit();
	
}

function editTask(id){
	var url = "<portlet:actionURL windowState="maximized"><portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" /><portlet:param name="cmd" value="view" /><portlet:param name="taskId" value="REPLACEME" /></portlet:actionURL>";
	url = url.replace("REPLACEME", id);
	window.location=url;
}

function refreshTaskList(urlParams){
	lastUrlParams = urlParams;
	var r = Math.floor(Math.random() * 1000000000);
	var url = "/html/plugins/com/dotcms.escalation.view/view_exp_tool.jsp?r=" + r + urlParams;
	
	
	
	
	var myCp = dijit.byId("hangTaskListHere");
	alert(url);
	
	if (myCp) {
		myCp.destroyRecursive(true);
	}
	myCp = new dojox.layout.ContentPane({
		id : "hangTaskListHere"
	}).placeAt("hangTaskListHere");


	myCp.attr("href", url);

}

</script>

<%
Role Manutentor = (Role)APILocator.getRoleAPI().loadRoleByKey("Manut_");
List<User> userL = (List<User>)APILocator.getRoleAPI().findUsersForRole(Manutentor); 
%>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<html:form action='/ext/escalation/RunJobEsc' styleId="sb">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "Filtered-Tasks") %>' />

<!-- START Button Row -->
	<div class="buttonBoxLeft"><h3><%= LanguageUtil.get(pageContext, "EXP_Manager") %></h3></div>

<!-- END Button Row -->

<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="height:500px;">

<!-- START Left Column -->	
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 350px;" class="lineRight">
		<div style="margin-top:48px;">
			<div  id="filterTasksFrm">
				<input type="hidden" name="orderBy" id="orderBy" value="mod_date desc">


				
				<dl>
					
					<dt>Assign to User:</dt>
					<dd>
					
						
						<select name="user" id="userId" dojoType="dijit.form.FilteringSelect" value="" >
							<option value=""></option>
							<%for(User u : userL) {%>
								<option value="<%=APILocator.getRoleAPI().getUserRole(u)%>" ><%=u.getFirstName()+" - "+Manutentor.getName()%></option>
							<%} %>
						</select>

				         
					</dd>
					
					<dt></dt>
					<dd>
					
						<input type="hidden" id="stepId" name="stepId"  />


				         
					</dd>
					
					
				</dl>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" onclick="submitFrm()"> 
					<%= LanguageUtil.get(pageContext, "EXP_button_launch") %> </button>
					<!-- <button dojoType="dijit.form.Button" name="resetButton"  iconClass="resetIcon" onclick="resetFilters()"></button>-->    
				</div>
				
			</div>
		</div>
	</div>
<!-- END Left Column -->

<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" title="<%= LanguageUtil.get(pageContext, "EXP_ML") %>" region="center" style="margin-top:37px;">
		<div id="hangTaskListHere">
		<hr/>
		<%
		WorkflowSearcher searcher = new WorkflowSearcher(request.getParameterMap(), APILocator.getUserAPI().getSystemUser());
		session.setAttribute(com.dotmarketing.util.WebKeys.WORKFLOW_SEARCHER, searcher);
		WorkflowSearcher fakeSearcher =(WorkflowSearcher) BeanUtils.cloneBean(searcher) ; 
		
		%>
		
			<div style="margin-left: 135px;margin-right: 115px">
			<table class="listingTable">
			<tr>
				
				<th><input type="checkbox" dojoType="dijit.form.CheckBox"  id="checkAllCkBx" value="true" onClick="checkAll()" /></th>
				<th nowrap="nowrap" width="22%" style="text-align:center;">User</th>
				
				<th nowrap="nowrap" width="18%" style="text-align:center;">Title</th>
				<th nowrap="nowrap" width="10%" style="text-align:center;">Status</th>
				<th nowrap="nowrap" width="10%" style="text-align:center;">Step</th>
				<th nowrap="nowrap" width="15%" style="text-align:center;">Last Updated</th>
			</tr>
			
			<%
			
			//for(User u : userL){ 
				
				
				List<WorkflowTask> tasksss = searcher.findAllTasks(searcher);
				//List<WorkflowTask> tasks = APILocator.getWorkflowAPI().searchAllTasks();
				
			for(WorkflowTask task : tasksss){ 
				
				Role r = APILocator.getRoleAPI().loadRoleById(task.getAssignedTo());
				Contentlet contentlet = new Contentlet();
				WorkflowStep step = APILocator.getWorkflowAPI().findStep(task.getStatus());
				try{
					contentlet 	= APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(),false,APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), true);
					
				} catch(Exception e){
					Logger.error(this.getClass(), e.getMessage());	
				}
					
				%>
				
				<tr class="alternate_1">
				
				<td><input type="checkbox" dojoType="dijit.form.CheckBox" name="task" id="<%=task.getWebasset() %>" class="taskCheckBox" value="<%=task.getId() %>"  /></td>
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
				


			
			<%	}
		//}
			%>
	
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
						<a href="javascript:refreshTaskList('<%=fakeSearcher.getQueryStringBis()%>')"><%=i+1 %></a>
					<%} %>
					&nbsp;
				<%} %>
			<%} %>
		</td>
		<td width="33%" align="right">
			<%if(searcher.hasNext()){ 
				fakeSearcher.setPage(searcher.getPage()+1);
			%>
			
			<button dojoType="dijit.form.Button" onClick="refreshTaskList('<%=fakeSearcher.getQueryStringBis()%>');" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %> 
			</button>

			<%} %>
		</td>
		</tr>
	
	</table>	
	</div>
	</div>
		
				
</div>
<!-- END Right Column -->
</html:form>
</liferay:box>

<script type="text/javascript">
resizeBrowser();
</script>