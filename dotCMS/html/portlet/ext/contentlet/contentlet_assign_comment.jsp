<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClass"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%
String actionId=request.getParameter("actionId");
String inode=request.getParameter("inode");// DOTCMS-7085
WorkflowAction action = APILocator.getWorkflowAPI().findAction(actionId, user);
Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());

List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action);
boolean hasPushPublishActionlet = false;
GregorianCalendar cal = new GregorianCalendar();
for(WorkflowActionClass actionlet : actionlets){
	if(actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){
		hasPushPublishActionlet = true;
	}
}
boolean mustShow = false;
if(action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) || hasPushPublishActionlet){
	mustShow = true;
}

String conPublishDateVar = request.getParameter("publishDate");
String conExpireDateVar = request.getParameter("expireDate");
String structureInode = request.getParameter("structureInode");
if(conPublishDateVar ==null && structureInode != null){
	Structure s = StructureFactory.getStructureByInode(structureInode);
	if(s!=null){
		conPublishDateVar = s.getPublishDateVar();
		conExpireDateVar = s.getExpireDateVar();
	}
}



%>

<script>
dojo.require("dojox.data.QueryReadStore");
dojo.require("dotcms.dojo.push.PushHandler");
var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');
var myRoleReadStore = new dojox.data.QueryReadStore({url: '/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfRoleStoreAjax?cmd=assignable&actionId=<%=actionId%>'});



function toggleExpire(){

	var x = dijit.byId("wfNeverExpire").getValue();
	dijit.byId("wfExpireDateAux").disabled = x;
	dijit.byId("wfExpireTimeAux").disabled = x;

}

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
		dijit.byId("wfNeverExpire").setValue(false);
		toggleExpire();
	}
	else{
		if(dijit.byId("wfNeverExpire")){
			dijit.byId("wfNeverExpire").setValue(true);
			toggleExpire();
		}
	}
	if(dijit.byId("<%=conExpireDateVar%>Time")){
		expireTime= dijit.byId("<%=conExpireDateVar%>Time").getValue();
		 dijit.byId("wfExpireTimeAux").setValue(expireTime);
	}


}

function validate() {
	var whereToSend = dojo.byId("whereToSend").value;
	if(whereToSend==null || whereToSend=="") {
		alert("<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>");
		return false;
	}

	contentAdmin.saveAssign();

}
</script>

<!--  DOTCMS-7085 -->
<input name="wfConId" id="wfConId" type="hidden" value="<%=inode%>">

<% if(mustShow){ %>
	<div id="wfDivWrapperForDojo">
		<div style="margin:auto;width:500px;">
			<div class="fieldWrapper">
				<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "Perform-Workflow") %>:</div>
				<div class="fieldValue"><%=action.getName() %></div>
				<div class="clear"></div>
			</div>
			<%if(action.isCommentable()){ %>
				<div class="fieldWrapper">
					<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "Comments") %>: </div>
					<div class="fieldValue"><textarea name="taskCommentsAux" id="taskCommentsAux" cols=40 rows=8 style="min-height:100px;" dojoType="dijit.form.Textarea"></textarea></div>
					<div class="clear"></div>
				</div>
			<%}else{ %>
				<div class="fieldWrapper">
					<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "Comments") %>: </div>
					<div class="fieldValue"><%= LanguageUtil.get(pageContext, "None") %></div>
					<div class="clear"></div>
				</div>
			<%} %>
				<div class="fieldWrapper">
					<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "Assignee") %>: </div>
					<div class="fieldValue">
						<%if(action.isAssignable()){ %>
							<select id="taskAssignmentAux" name="taskAssignmentAux" dojoType="dijit.form.FilteringSelect"
									store="myRoleReadStore" searchDelay="300" pageSize="30" labelAttr="name"
									invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>">
							</select>
						<%} else if (UtilMethods.isSet( role ) && UtilMethods.isSet( role.getId() )) { %>
							<%=APILocator.getRoleAPI().loadCMSAnonymousRole().getId().equals(role.getId())?LanguageUtil.get(pageContext, "current-user"):role.getName()%>
							<input type="text" dojoType="dijit.form.TextBox" style="display:none" name="taskAssignmentAux" id="taskAssignmentAux" value="<%=role.getId()%>">
						<%} %>
					</div>
					<div class="clear"></div>
				</div>
				<%if(hasPushPublishActionlet){
					String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
					String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
				%>

					<div class="fieldWrapper">
						<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "Publish") %>: </div>
						<div class="fieldValue">
							<input
								type="text"
								dojoType="dijit.form.DateTextBox"
								validate="return false;"
								invalidMessage=""
								id="wfPublishDateAux"
								name="wfPublishDateAux" value="now" style="width: 110px;">


							<input type="text" name="wfPublishTimeAux" id="wfPublishTimeAux" value="now"
							 	data-dojo-type="dijit.form.TimeTextBox"
								required="true" style="width: 100px;"/>

							<input type="checkbox" data-dojo-type="dijit/form/CheckBox"  name="forcePush" id="forcePush" value="true"><label for="forcePush"><%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %></label>

						</div>
						<div class="clear"></div>
					</div>
					<div class="fieldWrapper">
						<div class="fieldName" style="width: 120px"><%= LanguageUtil.get(pageContext, "publisher_Expire") %>: </div>
						<div class="fieldValue">
							<input
								type="text"
								dojoType="dijit.form.DateTextBox"
								validate="return false;"
								id="wfExpireDateAux"
								name="wfExpireDateAux"
								value=""
								style="width: 110px;">

							<input 	type="text"
									name="wfExpireTimeAux"
									id="wfExpireTimeAux"
									value=""
							    	data-dojo-type="dijit.form.TimeTextBox"
									style="width: 100px;"
									  />

							<input type="checkbox" onclick="toggleExpire()" dojoType="dijit.form.CheckBox" value="true" name="wfNeverExpire" checked="true" id="wfNeverExpire" > <label for="wfNeverExpire"><%= LanguageUtil.get(pageContext, "publisher_Never_Expire") %></label>
						</div>
						<div class="clear"></div>
					</div>
					<div class="fieldWrapper">
						<div class="fieldName" style="width: 120px">
							<%=LanguageUtil.get(pageContext,
									"publisher_dialog_choose_environment")%>:
						</div>
						<div class="fieldValue">
							<input data-dojo-type="dijit/form/FilteringSelect"
								data-dojo-props="store:pushHandler.environmentStore, searchAttr:'name'"
								name="environmentSelect" id="environmentSelect" />
							<button dojoType="dijit.form.Button"
								onClick='pushHandler.addSelectedToWhereToSend()' iconClass="addIcon">
								<%=LanguageUtil.get(pageContext, "add")%>
							</button>
							<div class="wfWhoCanUseDiv">
								<table class="listingTable" id="whereToSendTable">
								</table>
							</div>
							<input type="hidden" name="whereToSend" id="whereToSend" value="">
						</div>
						<div class="clear"></div>
					</div>
				<%}%>
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" iconClass="saveAssignIcon" onClick="validate()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
				</button>
				<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="dijit.byId('contentletWfDialog').hide()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
					</button>

			</div>
		</div>
	</div>
<script>
	dojo.parser.parse(dojo.byId("wfDivWrapperForDojo"));
	setDates();

</script>
<% } %>

