<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page
	import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page
	import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String stepId = request.getParameter("stepId");
	WorkflowStep step = wapi.findStep(stepId);
	WorkflowScheme scheme = wapi.findScheme(step.getSchemeId());

	List<WorkflowAction> actions = wapi.findActions(step, APILocator.getUserAPI().getSystemUser());

%>

<div dojoType="dijit.form.Form" id="addEditStepForm" jsId="addEditStepForm"
      encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax"
      method="POST">
	  
	  <div class="form-horizontal">
		<input type="hidden" name="cmd" value="reorder">
		<input type="hidden" name="stepId" id="stepId" value="<%=UtilMethods.webifyString(step.getId())%>">
		<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
		  <dl>
			  <dt>
				  <h3><%=LanguageUtil.get(pageContext, "Workflow-Step")%></h3>
			  </dt>
			  <dd></dd>
		  </dl>
		  <dl>
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Name")%>:</label>
			  </dt>
			  <dd>
				  <input type="text" name="stepName" id=""stepName""
					dojoType="dijit.form.ValidationTextBox"  required="true"
					value="<%=UtilMethods.webifyString(step.getName())%>"
					maxlength="255">
			  </dd>
		  </dl>
		  <dl>
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Order")%>:</label>
			  </dt>
			  <dd>
				<input type="text" name="stepOrder" style="width:50px;"
							id="stepOrder" dojoType="dijit.form.ValidationTextBox" regExp="\d+" 
							value="<%=step.getMyOrder()%>">
			 </dd>
		  </dl>
		  <dl>
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Resolve-Task")%>:</label>
			  </dt>
			  <dd>
				  <input type="checkbox" name="stepResolved"
					id="stepResolved" dojoType="dijit.form.CheckBox" value="true"
					<%=(step.isResolved()) ? "checked='true'" : ""%>>
			  </dd>
		  </dl>
		  <dl>
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Escalation-Enable")%>:</label>
			  </dt>
			  <dd>
				  <input type="checkbox" name="enableEscalation" onChange="edit_step_toggleEscalation()"
                    id="enableEscalation" dojoType="dijit.form.CheckBox"
                    <%=step.isEnableEscalation() ? "checked='true'" : "" %>/>
			  </dd>
		  </dl>
		  <dl class="escalation-row">
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Escalation-Action")%>:</label>
			  </dt>
			  <dd>
				<select dojoType="dijit.form.FilteringSelect" id="escalationAction" name="escalationAction" required="false">
					<% for(WorkflowAction wa : actions) {%>
						<option value="<%=UtilMethods.webifyString(wa.getId())%>"
							<%= wa.getId().equals(step.getEscalationAction()) ? "selected='true'" : "" %>>
							<%=UtilMethods.webifyString(wa.getName()) %>
						</option>
					<% }%>
				</select>
			  </dd>
		  </dl>
		  <dl class="escalation-row">
			  <dt>
				  <label for=""><%=LanguageUtil.get(pageContext, "Escalation-Time")%>:</label>
			  </dt>
			  <dd>
				  <input type="text" onchange="showExpirationTime()" dojoType="dijit.form.NumberTextBox"
                          name="escalationTime" constraints="{min:0,max:30758400,places:0}"
                          id="escalationTime"
                          value="<%= step.isEnableEscalation() ? step.getEscalationTime() : 0 %>"
                          style="width:80px" />
			  </dd>
		  </dl>
		  <dl class="escalation-row">
			  <dt></dt>
			  <dd>
				  <span id="showExpirationTime" style="float: left;">&nbsp;</span>
			  </dd>
		  </dl>
	  </div>

		<div class="buttonRow">
			<button dojoType="dijit.form.Button" onClick='stepAdmin.editStep()' iconClass="saveIcon"
				type="button" id="editStepBtn">
				<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
			</button> 

			<button dojoType="dijit.form.Button"
				onClick='stepAdmin.hideEdit()' iconClass="cancelIcon"
				type="button" class="dijitButtonFlat">
				<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
			</button>
		</div>

</div>
