<%@page import="com.dotcms.publisher.ajax.RemotePublishAjaxAction"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@ page import="com.dotmarketing.util.DateUtil" %>
<%

final String actionId = request.getParameter("actionId");
WorkflowAction action = null;
Role role = null;
if(UtilMethods.isSet(actionId)) {
	action = APILocator.getWorkflowAPI().findAction(actionId, user);
	role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
}

String conPublishDateVar = request.getParameter("publishDate");
String conExpireDateVar = request.getParameter("expireDate");

final String structureInode = request.getParameter("structureInode");
if(UtilMethods.isSet(structureInode)){
    final Structure structure =  CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
    if(UtilMethods.isSet(structure)){
        conPublishDateVar = structure.getPublishDateVar();
        conExpireDateVar = structure.getExpireDateVar();
    }
}

final String inode = request.getParameter("inode");// DOTCMS-7085
final GregorianCalendar cal = new GregorianCalendar();
final Date dateValue = new Date();
cal.setTime(dateValue);

final String currentDateStr = DateUtil.format(dateValue, "yyyy-MM-dd");

final String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
final String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);

%>

<style type="text/css">
    .progressRow {
        padding-left: 33%;
    }
</style>

<script type="application/javascript" >

    function setDates(){
        // force dojo to parse
        var pubDay = new Date();
        var pubTime = new Date();
        var expireDate = new Date();
        var expireTime = new Date();

        if(dijit.byId("<%=conPublishDateVar%>Date")){
            pubDay = dijit.byId("<%=conPublishDateVar%>Date").getValue();
            dijit.byId("wfPublishDateAux").setValue(pubDay);
        }
        if(dijit.byId("<%=conPublishDateVar%>Time")){
            pubTime= dijit.byId("<%=conPublishDateVar%>Time").getValue();
            dijit.byId("wfPublishTimeAux").setValue(pubTime);
        }

        if(dijit.byId("<%=conExpireDateVar%>Date") && dijit.byId("<%=conExpireDateVar%>Date").getValue()){
            expireDate = dijit.byId("<%=conExpireDateVar%>Date").getValue();
            dijit.byId("wfExpireDateAux").setValue(expireDate);

        }

        if(dijit.byId("<%=conExpireDateVar%>Time")){
            expireTime= dijit.byId("<%=conExpireDateVar%>Time").getValue();
            dijit.byId("wfExpireTimeAux").setValue(expireTime);
        }
    }

</script>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">
<input name="actionId" id="actionId" type="hidden" value="<%=actionId%>">
<input name="hasCondition" id="hasCondition" type="hidden" value="<%=(null != action && UtilMethods.isSet(action.getCondition()))%>">

<div class="form-horizontal" dojoType="dijit.form.Form" id="publishForm">

        <%--  COMMENTABLE ACTION  --%>
		<%if(null != action && action.isCommentable()){ %>
		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "Comments") %>:
			</dt>
			<dd>
				<textarea name="taskCommentsAux" id="taskCommentsAux" cols=40 rows=8 style="min-height:100px;width:260px;" dojoType="dijit.form.Textarea"></textarea>
			</dd>
		</dl>
		<%}%>

		<%-- ASSIGNABLE ACTION --%>
		<%if ( null != action && action.isAssignable()) { %>

			<dl>
				<dt>
					<%= LanguageUtil.get(pageContext, "Assignee") %>:
				</dt>
				<dd>
					<%if (action.isAssignable()) { %>
					<select id="taskAssignmentAux" name="taskAssignmentAux"
							dojoType="dijit.form.FilteringSelect"
							searchDelay="300" pageSize="30" labelAttr="name"
							style="width:260px"
							invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>">
					</select>
					<%} else if (UtilMethods.isSet(role) && UtilMethods.isSet(role.getId())) { %>
					<%=APILocator.getRoleAPI().loadCMSAnonymousRole().getId().equals(role.getId())
							? LanguageUtil.get(pageContext, "current-user") : role.getName()%>
					<input type="text" dojoType="dijit.form.TextBox" style="display:none"
						   name="taskAssignmentAux" id="taskAssignmentAux" value="<%=role.getId()%>">
					<%} %>
				</dd>
			</dl>

		<%} %>

        <%-- DATE FILTERING BOX --%>
        <dl id="filterTimeDiv" style="display: none;">
            <dt>
                <%= LanguageUtil.get( pageContext, "publish.created.after" ) %>:
            </dt>
            <dd>
                <input type="text" dojoType="dijit.form.DateTextBox" value="now" required="true"
                       id="wfFilterDateAux" name="wfFilterDateAux" style="width: 110px;">

                <input type="text" data-dojo-type="dijit.form.TimeTextBox" value='T<%=hour+":"+min%>:00' required="true"
                       id="wfFilterTimeAux" name="wfFilterTimeAux" style="width: 100px;"/>
            </dd>
        </dl>
        <%--DATE FILTERING BOX--%>

		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "I-want-to") %>:
			</dt>
			<dd>
				<div class="inline-form">
					<div class="radio">
						<input type="radio" dojoType="dijit.form.RadioButton" checked="true" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH %>" name="wfIWantTo" id="iwtPublish" >
						<label for="iwtPublish"><%= LanguageUtil.get(pageContext, "push") %></label>
					</div>
					<div class="radio">
						<input type="radio" dojoType="dijit.form.RadioButton" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE %>" name="wfIWantTo" id="iwtExpire" >
						<label for="iwtExpire"><%= LanguageUtil.get(pageContext, "Remove") %></label>
					</div>
					<div class="radio">
						<input type="radio" dojoType="dijit.form.RadioButton" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE %>" name="wfIWantTo" id="iwtPublishExpire" >
						<label for="iwtPublishExpire"><%= LanguageUtil.get(pageContext, "push") %> <%= LanguageUtil.get(pageContext, "Remove") %></label>
					</div>
				</div>
			</dd>
		</dl>


		<dl id="publishTimeDiv">

			<dt>
				<%= LanguageUtil.get(pageContext, "Publish") %>:
			</dt>
			<dd>
				<input
					type="text"
					dojoType="dijit.form.DateTextBox"
					validate="return false;"
					invalidMessage=""
					id="wfPublishDateAux"
					name="wfPublishDateAux" value='<%=currentDateStr%>' style="width: 137px;">
				<input type="text" name="wfPublishTimeAux" id="wfPublishTimeAux" value='T<%=hour+":"+min%>:00'
				 	data-dojo-type="dijit.form.TimeTextBox"
					required="true" style="width: 120px;"/>
				<div class="checkbox">
					<input type="checkbox" data-dojo-type="dijit/form/CheckBox"  name="forcePush" id="forcePush" value="true">
					<label for="forcePush"><%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %></label>
				</div>
			</dd>
		</dl>

		<dl id="expireTimeDiv" style="display: none;">
			<dt><%= LanguageUtil.get(pageContext, "publisher_Expire") %> :</dt>
			<dd>
			<input
				   type="text"
				   dojoType="dijit.form.DateTextBox"
				   validate="return false;"
				   id="wfExpireDateAux"
				   name="wfExpireDateAux" value='<%=currentDateStr%>'
				   style="width: 137px;" />

			<input
				   type="text"
				   data-dojo-type="dijit.form.TimeTextBox"
				   id="wfExpireTimeAux"
				   name="wfExpireTimeAux" value='T<%=hour+":"+min%>:00'
				   style="width: 120px;" />

			</dd>
		</dl>

		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_environment") %>:
			</dt>
			<dd>
				<input data-dojo-type="dijit/form/FilteringSelect" required="false"
			    name="environmentSelect" id="environmentSelect"  style="width:260px" />

				<div class="who-can-use">
					<table class="who-can-use__list" id="whereToSendTable">
					</table>
				</div>
				<input type="hidden" name="whereToSend" id="whereToSend" value="">
			</dd>
		</dl>

        <div class="progressRow" style="display: none;">
            <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveRemotePublishProgress" id="saveRemotePublishProgress"></div>
        </div>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" type="button" id="remotePublishSaveButton">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_dialog_push")) %>
			</button>
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" type="button" id="remotePublishCancelButton" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
		</div>

		<input id="whereToSendRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>" />
		<input id="assignToRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "Assign-To-Required")%>" />

</div>
<script>
    dojo.addOnLoad(function () {
        setDates();
	});
</script>

<%@ include file="/html/portlet/ext/remotepublish/remote_publish_form_inc.jsp" %>

