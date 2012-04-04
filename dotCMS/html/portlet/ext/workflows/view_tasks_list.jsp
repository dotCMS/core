<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowTask"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowSearcher"%>
<%
	WorkflowSearcher searcher = new WorkflowSearcher(request.getParameterMap(), user);
	session.setAttribute(com.dotmarketing.util.WebKeys.WORKFLOW_SEARCHER, searcher);
	
	WorkflowSearcher fakeSearcher =(WorkflowSearcher) BeanUtils.cloneBean(searcher) ;
	WorkflowAPI wapi = APILocator.getWorkflowAPI();

	List<WorkflowTask> tasks = searcher.findTasks();
	


	java.util.Map params = new java.util.HashMap();
	params.put("struts_action", new String[] { "/ext/workflows/view_workflow_tasks" });

	String referer = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED
        	.toString(), params);
        	

	boolean singleStep = true;
	String currentStep =null;
	for(WorkflowTask task: tasks){
		if(currentStep==null && singleStep) {
			currentStep =task.getStatus();
		} 
		if(!task.getStatus().equals(currentStep)){ 
			singleStep = false;
			currentStep = null;
		} 
	}
	
	if(currentStep == null){
		currentStep = searcher.getStepId();
	}

	
	
   	List<WorkflowAction> availableActions= new ArrayList();
   	if(currentStep != null){
   		WorkflowStep step = new WorkflowStep();
   		step.setId(currentStep);
   		
   		List<WorkflowAction>  myActions = wapi.findActions(step, user);
   		for(WorkflowAction a : myActions){
   			if(!a.requiresCheckout()){
   				availableActions.add(a);
   			}
   			
   		}
   		if(availableActions.size() ==0){
   			singleStep=false;
   		}
   	}
   	

   	
   	

%>

<script>


	var actionData = { 
		identifier: 'id',
	 	label: 'name',
	  	items: [
		{ id:'',  name:'' }
		<%for(WorkflowAction action : availableActions){%>
			  ,{ id:'<%=action.getId()%>', 
				  name:'<%=action.getName()%>', 
				  assignable:'<%=action.isAssignable()%>',
				  commentable:'<%=action.isCommentable() ||  UtilMethods.isSet(action.getCondition())%>'
				  
			  }
		  
		<%}%>

	]};


	var actionStore = new dojo.data.ItemFileReadStore({data:actionData});


	
</script>





<div style="margin:15px;">
	<table class="listingTable">
	
	<tr>
		<th id="checkAllCheckbox" style="width:20px;">
			<input type="checkbox" dojoType="dijit.form.CheckBox" <%if(!singleStep){ %>disabled="true"<%} %> id="checkAllCkBx" value="true" onClick="checkAll()" />
			
		</th>
		<th nowrap="nowrap" style="text-align:center;"><a href="javascript: doOrderBy('<%="title".equals(searcher.getOrderBy())?"title desc":"title"%>')"><%=LanguageUtil.get(pageContext, "Title")%></a></th>

		<th nowrap="nowrap" width="8%" style="text-align:center;"><a href="javascript: doOrderBy('<%="status".equals(searcher.getOrderBy())?"status desc":"status"%>')"><%=LanguageUtil.get(pageContext, "Status")%></a></th>
		<th nowrap="nowrap" width="10%" style="text-align:center;"><a href="javascript: doOrderBy('<%="workflow_step.name".equals(searcher.getOrderBy())?"workflow_step.name desc":"workflow_step.name"%>')"><%=LanguageUtil.get(pageContext, "Workflow-Step")%></a></th>
		
		<th nowrap="nowrap" width="10%" style="text-align:center;"><a href="javascript: doOrderBy('<%="assigned_to".equals(searcher.getOrderBy())?"assigned_to desc":"assigned_to"%>')"><%=LanguageUtil.get(pageContext, "Assignee")%></a></th>
		<th nowrap="nowrap" width="15%" style="text-align:center;"><a href="javascript: doOrderBy('<%="mod_date".equals(searcher.getOrderBy())?"mod_date desc":"mod_date"%>')"><%=LanguageUtil.get(pageContext, "Last-Updated")%></a></th>
		
		
	</tr>
	<%if(tasks==null || tasks.size() ==0){ %>
		<tr>
			<td colspan="100">
				<div class="noResultsMessage"><%=LanguageUtil.get(pageContext, "No-Tasks-Found")%></div>
			
			</td>
		</tr>
	<%} %>
	<%for(WorkflowTask task : tasks){ %>
		<%Role r = APILocator.getRoleAPI().loadRoleById(task.getAssignedTo()); %>
		<%Contentlet contentlet = new Contentlet();

			try{
			 contentlet 	= APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(),false,APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true);
			}
			catch(Exception e){
				Logger.error(this.getClass(), e.getMessage());	
			}
			%>
		<%WorkflowStep step = APILocator.getWorkflowAPI().findStep(task.getStatus()); %>
		<tr class="alternate_1">
			<td>
				
					<input <%if(!singleStep){ %>disabled="true"<%} %> type="checkbox" dojoType="dijit.form.CheckBox" id="<%=task.getWebasset() %>" class="taskCheckBox" value="<%=task.getId() %>"  />

				
			</td>
			<td onClick="editTask('<%=task.getId()%>')">
				<%=task.getTitle() %>
				</td>
			<td nowrap="true" align="center" width="1%" onClick="editTask('<%=task.getId()%>')">
				<%if (contentlet.isLive()) {%>
		            <span class="liveIcon"></span>
		        <%} else if (contentlet.isArchived()) {%>
		        	<span class="archivedIcon"></span>
		        <%} else if (contentlet.isWorking()) {%>
		            <span class="workingIcon"></span>
		        <%}%>
		        <%if (contentlet.isLocked()) {
		        
		  		  	User u = APILocator.getUserAPI().loadUserById(APILocator.getVersionableAPI().getLockedBy(contentlet.getIdentifier()), APILocator.getUserAPI().getSystemUser(), false); %>
		        	<span class="lockIcon"  title="<%=UtilMethods.javaScriptify(u.getFullName()) %>"></span>
		   		<%} %>
				
				
				
			</td>

				
			<td align="center" onClick="editTask('<%=task.getId()%>')" <%if(step.isResolved()) {%>style="text-decoration: line-through;"<%} %> >

				 <%=step.getName() %>

			</td>

			<td nowrap="norap" align="center"><%=r.getName() %></td>
			<td align="center" nowrap="norap"><%=DateUtil.prettyDateSince(task.getModDate(), user.getLocale()) %></td>
			

		</tr>
	<%} %>
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
	
	
	<%if(tasks != null && tasks.size() >0 ){ %>
		<div class="buttonRow" style="text-align: left">
			
			<%if(availableActions.size() > 0){ %>
				<%=LanguageUtil.get(pageContext, "Workflows") %> : 
				<select name="performAction" id="performAction" store="actionStore" dojoType="dijit.form.FilteringSelect"></select>
	
				<button dojoType="dijit.form.Button" onClick="excuteWorkflowAction()">
					<%=LanguageUtil.get(pageContext, "Perform-Workflow") %>
				</button>
			<%} %>
		</div>
	<%} %>
	<form name="executeTasksFrm" id="executeTasksFrm" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=executeActions" method="post">
		<input name="wfActionAssign" id="wfActionAssign" type="hidden" value="">
		<input name="wfActionComments" id="wfActionComments" type="hidden"" value="">
		<input name="wfActionId" id="wfActionId" type="hidden" value="">
		<input name="wfCons" id="wfCons" type="hidden" value="">
	</form>
<%-- 
<%=request.getQueryString() %>
<br> &nbsp;<br>

<%=searcher.getQueryString() %>
--%>
</div>
