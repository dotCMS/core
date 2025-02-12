<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page
        import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page
        import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="java.util.List" %>
<%@page import="com.dotmarketing.util.Config"%>


<%
    final User systemUser = APILocator.getUserAPI().getSystemUser();
    final WorkflowAPI wapi = APILocator.getWorkflowAPI();
    final String schemeId = request.getParameter("schemeId");
    final WorkflowScheme scheme = UtilMethods.isSet(pageContext.getAttribute("scheme")) ? WorkflowScheme.class.cast(pageContext.getAttribute("scheme")) : wapi.findScheme(schemeId);
    final List<WorkflowStep> steps = wapi.findSteps(scheme);

    Role role = null;
    final String roleId = request.getParameter("roleId");
    if(UtilMethods.isSet(roleId)){
        role = APILocator.getRoleAPI().loadRoleById(roleId);
    }

    ContentType contentType = null;
    final String contentTypeId = request.getParameter("contentTypeId");
    if(UtilMethods.isSet(contentTypeId)){
        contentType = APILocator.getContentTypeAPI(systemUser).find(contentTypeId);
    }

%>


<%
    int stepIndex = 0;
    boolean isFirst = true;
    for(WorkflowStep step : steps){

    final List<WorkflowAction> actions = (
            UtilMethods.isSet(role)
                    ? wapi.findActions(step, role, contentType)
                    : wapi.findActions(step, systemUser)
    );

%>

<script type="text/javascript">

    function addSeparator(schemeId, stepId) {
        const xhrArgs = {
            url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=save&stepId=" + stepId + "&schemeId=" + schemeId + "&actionId=SEPARATOR",
            handle: function (dataOrError, ioArgs) {
                if (dojo.isString(dataOrError)) {

                    if (dataOrError.indexOf("FAILURE") === 0) {
                        showDotCMSSystemMessage(dataOrError, true);
                    } else {
                        mainAdmin.refresh();
                    }
                } else {
                    this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");

                }
            }
        };

        dojo.xhrPost(xhrArgs);
        mainAdmin.refresh();
    }

</script>

<div class="list-wrapper wfStepInDrag" id="stepID<%=step.getId()%>" data-first="<%=isFirst%>">
    <div class="list-item"  onmouseout="colorMeNot()">
        <div class="wfStepTitle">
            <div class="showPointer wfStepTitleDivs handle" onClick="stepAdmin.showStepEdit('<%=step.getId()%>')">
                <span style="border-bottom:dotted 1px #fff;"><%=step.getName() %></span>
                <span style="font-weight:normal;display:inline-block;">
					<%=step.isResolved() ? "(" + LanguageUtil.get(pageContext, "resolved") + ")": "" %>
				</span>
            </div>
            <div class="clear"></div>
        </div>
        <div class="wfActionList" id="jsNode<%=step.getId()  %>"  data-wfstep-id="<%=step.getId()%>">
            <%for(WorkflowAction action : actions) {
                String subtype = action.getMetadata() != null ? String.valueOf(action.getMetadata().get("subtype")) : "";
                boolean isSeparator = "SEPARATOR".equals(subtype);
            %>
            <div class="wf-action-wrapper x<%=action.getId()%>" data-wfaction-id="<%=action.getId()%>" onmouseover="colorMe('x<%=action.getId()%>')" onmouseout="colorMeNot('x<%=action.getId()%>')" >
                <div class="handles"></div>
                <div class="wf-action <%= isSeparator ? "showDefaultCursor" : "showPointer" %>"">
                    <div class="pull-right showPointer" onclick="actionAdmin.deleteActionForStep(this, <%=stepIndex%>)"><span class="deleteIcon"></span></div>
                    <div class="pull-left" <% if(!isSeparator) { %>onClick="actionAdmin.viewAction('<%=scheme.getId()%>', '<%=action.getId() %>');" <% } %>>
                       <%=action.getName() %> <span style="color:#a6a6a6">&#8227; <%=(WorkflowAction.CURRENT_STEP.equals(action.getNextStep())) ?  WorkflowAction.CURRENT_STEP : wapi.findStep(action.getNextStep()).getName() %></span>
                   </div>
                </div>
            </div>
            <%} %>
        </div>

        <div class="btn-flat-wrapper">
            <%
                if (stepIndex > 0){
            %>
            <div class="btn-flat showPointer" onclick="stepAdmin.deleteStep('<%=step.getId()%>')">Delete</div>
            <%
                }
            %>
            <%
                String newContentEditorEnabled = Config.getStringProperty("CONTENT_EDITOR2_ENABLED");
                if (newContentEditorEnabled != null && newContentEditorEnabled.equalsIgnoreCase("true")) {
            %>
            <div class="btn-flat btn-primary showPointer" onclick="addSeparator('<%=scheme.getId()%>', '<%=step.getId()%>');">
                <i class="fa fa-plus" aria-hidden="true"></i> Divider
            </div>
            <% } %>
            <div class="btn-flat btn-primary showPointer" onclick="actionAdmin.addOrAssociatedAction('<%=scheme.getId()%>', '<%=step.getId()%>', 'step-action-<%=step.getId()%>');">
                <i class="fa fa-plus" aria-hidden="true"></i> Add
            </div>
        </div>
    </div>
</div>
<%
        isFirst = false;
        stepIndex++;
}
%>
<div class="list-wrapper showPointer ghostAddDiv" onclick="stepAdmin.schemeId='<%=schemeId%>';stepAdmin.showAddNewStep();" >
    <div class="list-item">
        <div class="wfStepTitle">
            <div class="wfStepTitleDivs">
                <span style="border-bottom:dotted 0px #fff;"><%=LanguageUtil.get(pageContext, "Add-Workflow-Step")%></span>
            </div>
            <div class="clear"></div>
        </div>
        <div class="wfActionList">

        </div>
    </div>
</div>


