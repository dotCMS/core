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
				<label for=""><%=LanguageUtil.get(pageContext, "Step")%> <%=LanguageUtil.get(pageContext, "Id")%>:</label>
			</dt>
			<dd>
				<strong>
					<a onclick="this.parentNode.innerHTML='<%=step.getId()%>'; return false;" href="#"><%=APILocator.getShortyAPI().shortify(step.getId()) %></a>
					(<a  href="/api/v1/workflow/steps/<%=step.getId()%>" target="_blank" onclick="event.stopPropagation();">json</a>)
				</strong>

			</dd>
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
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Escalation-Action")%>:</label>
			</dt>
			<dd>
				<select dojoType="dijit.form.FilteringSelect" id="escalationAction" name="escalationAction" required="false"  disabled="true">
					<% for(WorkflowAction wa : actions) {%>
					<option value="<%=UtilMethods.webifyString(wa.getId())%>"
							<%= wa.getId().equals(step.getEscalationAction()) ? "selected='true'" : "" %>>
						<%=UtilMethods.webifyString(wa.getName()) %>
					</option>
					<% }%>
				</select>
			</dd>
		</dl>
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Escalation-Time")%>:</label>
			</dt>
			<dd>
				<input type="text" onchange="showExpirationTime()" dojoType="dijit.form.NumberTextBox"
					   name="escalationTime" constraints="{min:0,max:30758400,places:0}"  disabled="true"
					   id="escalationTime"
					   value="<%= step.isEnableEscalation() ? step.getEscalationTime() : 0 %>"
					   style="width:80px" />
			</dd>
		</dl>
		<dl>
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

<script>
    function showExpirationTime(){
        var ttl = dijit.byId("escalationTime").getValue();

        var m = 60 * 60 * 24 * 30;
        var w = 60*60*24*7;
        var d = 60*60*24;
        var h = 60*60;
        var mm = 60;
        var message = "";
        var x = 0;
        while(ttl>0){
            if(x>0){
                message+=", ";
            }

            if(ttl>=m){
                x = Math.floor(ttl / m);
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Month") %>"
                    : " <%= LanguageUtil.get(pageContext, "Months") %>";
                ttl = Math.floor(ttl % m);
            }
            else if(ttl >= w){
                x = Math.floor(ttl / w);
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Week") %>"
                    : " <%= LanguageUtil.get(pageContext, "Weeks") %>";
                ttl = Math.floor(ttl % w);
            }
            else if(ttl >= d){
                x = Math.floor(ttl / d);
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Day") %>"
                    : " <%= LanguageUtil.get(pageContext, "Days") %>";
                ttl = Math.floor(ttl % d);
            }
            else if(ttl >= h){
                x = Math.floor(ttl / h);
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Hour") %>"
                    : " <%= LanguageUtil.get(pageContext, "Hours") %>";
                ttl = Math.floor(ttl % h);
            }
            else if(ttl >= mm){
                x = Math.floor(ttl / mm);
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Minute") %>"
                    : " <%= LanguageUtil.get(pageContext, "Minutes") %>";
                ttl = Math.floor(ttl % mm);
            }
            else if(ttl > 0){
                x =ttl;
                message+= x;
                message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Second") %>"
                    : " <%= LanguageUtil.get(pageContext, "Seconds") %>";
                ttl=0;

            }
        }

        dojo.byId("showExpirationTime").innerHTML = message;
    }



    function edit_step_toggleEscalation() {

        if(dijit.byId("enableEscalation").checked) {
            dijit.byId("escalationAction").set('disabled', false);
            dijit.byId("escalationTime").set('disabled', false);
            dojo.style("showExpirationTime", "visibility","");
        }
        else {
            dijit.byId("escalationAction").set('disabled', true);
            dijit.byId("escalationTime").set('disabled', true);
            dojo.style("showExpirationTime", "visibility","hidden");
        }
    }

    function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    async function waitForRender() {
        i++;
        while(dijit.byId("enableEscalation") ==undefined){
            await sleep(200);
            if(i>10000){
                break;
            }
        }
        edit_step_toggleEscalation();
        showExpirationTime();
    }


    waitForRender();

</script>