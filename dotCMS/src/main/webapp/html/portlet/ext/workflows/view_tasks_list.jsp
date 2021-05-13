<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
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
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Optional" %>
<%request.setAttribute("requiredPortletAccess", "workflow"); %>
<%@ include file="/html/common/uservalidation.jsp"%>
<%

	Map<String, Object>  newMap = new HashMap<String, Object>();
	newMap.putAll(request.getParameterMap());
	WorkflowSearcher searcher = new WorkflowSearcher(newMap, user);

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

		final WorkflowStep step = new WorkflowStep();
		step.setId(currentStep);

		final List<WorkflowAction> actions = wapi.findActions(step, user);
		if (null != actions) {

			for (final WorkflowAction action : actions) {

				if (action.shouldShowOnListing()) {

					availableActions.add(action);
				}
			}
		}

		if (availableActions.size() == 0) {
			singleStep = false;
		}
	}





%>

<script>

	dojo.require("dotcms.dojo.push.PushHandler");
	require(["dojo/dom-attr"]);

    var actionData = {
        identifier: 'id',
        label: 'name',
        items: [
            { id:'',  name:'' }
            <%for(WorkflowAction action : availableActions){%>
            ,{ id:'<%=action.getId()%>',
                name:'<%=action.getName()%>',
                assignable:'<%=action.isAssignable()%>',
                commentable:'<%=action.isCommentable() ||  UtilMethods.isSet(action.getCondition())%>',
				pushPublish:'<%=action.hasPushPublishActionlet()%>'
            }

            <%}%>

        ]};


    var actionStore = new dojo.data.ItemFileReadStore({data:actionData});

</script>





<div style="margin:15px;">

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

	<table class="listingTable">

		<tr>
			<th id="checkAllCheckbox" style="width:20px;">
				<input type="checkbox" dojoType="dijit.form.CheckBox" <%if(!singleStep){ %>disabled="true"<%} %> id="checkAllCkBx" value="true" onClick="checkAll()" />

			</th>
			<th nowrap="nowrap" style="text-align:center;"><a href="javascript: doOrderBy('<%="title".equals(searcher.getOrderBy())?"title desc":"title"%>')"><%=LanguageUtil.get(pageContext, "Title")%></a></th>


			<th nowrap="nowrap" width="8%" style="text-align:center;"><a href="javascript: doOrderBy('<%="status".equals(searcher.getOrderBy())?"status desc":"status"%>')"><%=LanguageUtil.get(pageContext, "Status")%></a></th>
			<th nowrap="nowrap" width="10%" style="text-align:center;"><%=LanguageUtil.get(pageContext, "language")%></th>
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
		<%for(WorkflowTask task : tasks) { %>
		<%
			Role assignedRole = APILocator.getRoleAPI().loadRoleById(task.getAssignedTo());
			String assignedRoleName = "";
			if (UtilMethods.isSet( assignedRole ) && UtilMethods.isSet( assignedRole.getId() )) {
				assignedRoleName = assignedRole.getName();
			}
		%>
		<%Contentlet contentlet = new Contentlet();

			try{
				contentlet = APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(), false, task.getLanguageId(), APILocator.getUserAPI().getSystemUser(), true);
				//contentlet = APILocator.getContentletAPI().search("+identifier: "+task.getWebasset(), 0, -1, null, APILocator.getUserAPI().getSystemUser(), true).get(0);
			}
			catch(Exception e){
				Logger.debug(this.getClass(), e.getMessage());
			}
			if(contentlet == null || !UtilMethods.isSet(contentlet.getInode())){
				continue;
			}
		%>

		<%
			if (APILocator.getWorkflowAPI().hasValidLicense() || APILocator.getWorkflowAPI().isSystemStep(task.getStatus())) {
				WorkflowStep step = APILocator.getWorkflowAPI().findStep(task.getStatus()); %>
		<tr class="alternate_1">
			<td>

				<input <%if(!singleStep){ %>disabled="true"<%} %> type="checkbox" dojoType="dijit.form.CheckBox" id="<%=contentlet.getInode() %>" class="taskCheckBox" value="<%=task.getId() %>"  data-action-inode="<%=contentlet.getInode()%>" />


			</td>
			<td onClick="editTask(event, '<%=task.getId()%>', '<%=contentlet.getLanguageId()%>')">
				<%=contentlet.getTitle() %>
			</td>
			<td nowrap="true" align="center" width="1%" onClick="editTask('<%=task.getId()%>', '<%=contentlet.getLanguageId()%>')">
				<%if (contentlet.isLive()) {%>
				<span class="liveIcon"></span>
				<%} else if (contentlet.isArchived()) {%>
				<span class="archivedIcon"></span>
				<%} else if (contentlet.isWorking()) {%>
				<span class="workingIcon"></span>
				<%}%>
				<%if (contentlet.isLocked()) {
					Optional<String> lockedBy = APILocator.getVersionableAPI().getLockedBy(contentlet);
					if(lockedBy.isPresent()) {
					User u = APILocator.getUserAPI().loadUserById(lockedBy.get(), APILocator.getUserAPI().getSystemUser(), false); %>
					<span class="lockIcon"  title="<%=UtilMethods.javaScriptify(u.getFullName()) %>"></span>
					<%} else {
						Logger.error(this, "Can't find LockedBy for Contentlet. Identifier: "
								+ contentlet.getIdentifier() + ". Lang: " + contentlet.getLanguageId());
					}
				}%>

			</td>
			<td>
				<%
					final Language language = APILocator.getLanguageAPI()
							.getLanguage(contentlet.getLanguageId());

					final String langIcon = UtilMethods.isSet(language.getCountryCode())?
							language.getLanguageCode() + "_" + language.getCountryCode():language.getLanguageCode();
				%>
				<img src="/html/images/languages/<%=langIcon%>.gif" width="16px" height="11px" style="margin-top:4px;float:left;">
				<span>&nbsp;(<%=langIcon%>)</span>
			</td>


			<td align="center" onClick="editTask('<%=task.getId()%>', '<%=contentlet.getLanguageId()%>')" <%if(step.isResolved()) {%>style="text-decoration: line-through;"<%} %> >

				<%=step.getName() %>

			</td>

			<td nowrap="norap" align="center"><%=assignedRoleName %></td>
			<td align="center" nowrap="norap"><%=DateUtil.prettyDateSince(task.getModDate(), user.getLocale()) %></td>


		</tr>
		<%
				} // if
			} // for
		%>
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
				<%if(searcher.getTotalPages() > 1){
					for(int auxPage = searcher.getPage() - 4; auxPage < searcher.getPage(); auxPage++){
						if(auxPage >= 0){
							fakeSearcher.setPage(auxPage);
				%><a href="javascript:refreshTaskList('<%=fakeSearcher.getQueryString()%>')"><%=auxPage+1 %></a>&nbsp;<%
					}
				}

			%><%=searcher.getPage() + 1%>&nbsp;<%

				for(int auxPage = searcher.getPage() + 1; auxPage < searcher.getPage() + 4; auxPage++){
					if(auxPage < searcher.getTotalPages()){
						fakeSearcher.setPage(auxPage);
			%><a href="javascript:refreshTaskList('<%=fakeSearcher.getQueryString()%>')"><%=auxPage+1 %></a>&nbsp;<%
						}
					}
				} %>
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



	<form name="executeTasksFrm" id="executeTasksFrm" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=executeActions" method="post">
		<input name="wfActionAssign" id="wfActionAssign" type="hidden" value="">
		<input name="wfActionComments" id="wfActionComments" type="hidden" value="">
		<input name="wfActionId" id="wfActionId" type="hidden" value="">
		<input name="wfCons" id="wfCons" type="hidden" value="">
	</form>
	<%--
    <%=request.getQueryString() %>
    <br> &nbsp;<br>

    <%=searcher.getQueryString() %>
    --%>
</div>
