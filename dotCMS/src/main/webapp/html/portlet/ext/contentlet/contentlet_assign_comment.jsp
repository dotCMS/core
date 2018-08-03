<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.util.ActionletUtil"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%
final String actionId=request.getParameter("actionId");
final String inode=request.getParameter("inode");// DOTCMS-7085
final WorkflowAction action = APILocator.getWorkflowAPI().findAction(actionId, user);
final Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());

final GregorianCalendar cal = new GregorianCalendar();
final boolean hasPushPublishActionlet = ActionletUtil.hasPushPublishActionlet(action);
final boolean mustShow = ActionletUtil.requiresPopupAdditionalParams(action);

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

final boolean bulkActions = Boolean.parseBoolean(request.getParameter("bulkActions"));

%>

<script>

    <%if(hasPushPublishActionlet){%>
        dojo.require("dotcms.dojo.push.PushHandler");
        var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');
    <%}%>

    dojo.require("dojox.data.QueryReadStore");
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


function extractTime(dateValue) {
   return dateValue == null ? "" : dojo.date.locale.format(dateValue,{timePattern: "H-m", selector: "time"})
}

function extractDate(dateValue){
   return dateValue == null ? "" : dojo.date.locale.format(dateValue,{datePattern: "yyyy-MM-dd", selector: "date"});
}

function handleBulkActionsDialogSelection() {

    if(typeof fireAction  === "function") {

        var actionId = '<%=actionId%>';
        var comment = (dijit.byId("taskCommentsAux") ? dijit.byId("taskCommentsAux").getValue() : null);
        var assign = (dijit.byId("taskAssignmentAux") ? dijit.byId("taskAssignmentAux").getValue() : null);

        var assignComment = {
            comment: comment,
            assign: assign,
        };

        var data;
        <%if(hasPushPublishActionlet){%>

		    //for some reason hidden fields can only be retrieved using dojo and not dijit
			var whereToSend = (dojo.byId("whereToSend") ? dojo.byId("whereToSend").value : "");
			var publishDate = (dijit.byId("wfPublishDateAux") ? extractDate(dijit.byId("wfPublishDateAux").getValue()) : "");
			var publishTime = (dijit.byId("wfPublishTimeAux") ? extractTime(dijit.byId("wfPublishTimeAux").getValue()): "");
			var expireDate  = (dijit.byId("wfExpireDateAux") ? extractDate(dijit.byId("wfExpireDateAux").getValue()) : "");
			var expireTime  = (dijit.byId("wfExpireTimeAux") ? extractTime(dijit.byId("wfExpireTimeAux").getValue()) : "");
			var neverExpire = (dijit.byId("wfNeverExpire") ? dijit.byId("wfNeverExpire").getValue() : "");
			var forcePush   = (dijit.byId("forcePush") ? dijit.byId("forcePush").getValue(): null);

			var pushPublish = {
				whereToSend:whereToSend,
				publishDate:publishDate,
				publishTime:publishTime,
                expireDate:expireDate,
				expireTime:expireTime,
				neverExpire:neverExpire,
				forcePush:forcePush
			};

		    data = {
				assignComment:assignComment,
                pushPublish:pushPublish
			};

        <%} else {%>

			data = {
				assignComment:assignComment
			};

        <%}%>

        fireAction(actionId, data);
        dijit.byId('contentletWfDialog').hide();
    } else {
        console.error("Unable to find function `handleBulkActionsDialogSelection`")
    }
}

function validate() {
    <%if(hasPushPublishActionlet){%>
        var whereToSend = dojo.byId("whereToSend").value;
        if(whereToSend==null || whereToSend=="") {
            showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>");
            return false;
        }
    <%}%>

    <%if(bulkActions){%>

          // javascript handler to return selection to the bulk actions caller.
          handleBulkActionsDialogSelection();

    <%}else{%>
       contentAdmin.saveAssign();
    <%}%>
}

</script>

<!--  DOTCMS-7085 -->
<input name="wfConId" id="wfConId" type="hidden" value="<%=inode%>">

<% if(mustShow){ %>
	<div id="wfDivWrapperForDojo" class="form-horizontal content-search__assign-workflow-form">
		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "Perform-Workflow") %>:
			</dt>
			<dd><%=action.getName() %></dd>
		</dl>
		<%if(action.isCommentable()){ %>
		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "Comments") %>:
			</dt>
			<dd>
				<textarea name="taskCommentsAux" id="taskCommentsAux" cols=40 rows=8
						  style="min-height:100px;width:260px;"
						  dojoType="dijit.form.Textarea"></textarea>
			</dd>
		</dl>
		<%}%>
		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "Assignee") %>:
			</dt>
			<dd>
				<%if (action.isAssignable()) { %>
				<select id="taskAssignmentAux" name="taskAssignmentAux"
						dojoType="dijit.form.FilteringSelect"
						store="myRoleReadStore" searchDelay="300" pageSize="30" labelAttr="name"
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
		<%if(hasPushPublishActionlet){
			String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
			String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
		%>

		<dl>
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
						name="wfPublishDateAux" value="now" style="width: 137px;">

				<input type="text" name="wfPublishTimeAux" id="wfPublishTimeAux" value="now"
					   data-dojo-type="dijit.form.TimeTextBox"
					   required="true" style="width: 120px;"/>

				<div class="checkbox">
					<input type="checkbox" data-dojo-type="dijit/form/CheckBox" name="forcePush"
						   id="forcePush" value="true">
					<label for="forcePush">
						<%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %>
					</label>
				</div>
			</dd>
		</dl>

		<dl>
			<dt><%= LanguageUtil.get(pageContext, "publisher_Expire") %> :</dt>
			<dd>
				<input
						type="text"
						dojoType="dijit.form.DateTextBox"
						validate="return false;"
						id="wfExpireDateAux"
						name="wfExpireDateAux"
						value=""
						style="width: 137px;">

				<input type="text"
					   name="wfExpireTimeAux"
					   id="wfExpireTimeAux"
					   value=""
					   data-dojo-type="dijit.form.TimeTextBox"
					   style="width: 120px;"/>

				<div class="checkbox">
					<input type="checkbox" onclick="toggleExpire()" dojoType="dijit.form.CheckBox"
						   value="true" name="wfNeverExpire" checked="true" id="wfNeverExpire">
					<label for="wfNeverExpire">
						<%= LanguageUtil.get(pageContext, "publisher_Never_Expire") %>
					</label>
				</div>
			</dd>
		</dl>

		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_environment") %>:
			</dt>
			<dd>
				<input data-dojo-type="dijit/form/FilteringSelect" required="false"
					   data-dojo-props="store:pushHandler.environmentStore, searchAttr:'name', displayedValue:'0'"
					   name="environmentSelect" id="environmentSelect"
					   onChange="pushHandler.addSelectedToWhereToSend()" style="width:260px"/>

				<div class="who-can-use">
					<table class="who-can-use__list" id="whereToSendTable">
					</table>
				</div>
				<input type="hidden" name="whereToSend" id="whereToSend" value="">
			</dd>
		</dl>

		<%}%>
		<div class="buttonRow">
			<% if (hasPushPublishActionlet) { %>
			<button dojoType="dijit.form.Button" onClick="validate()"
					type="button">
				<%= UtilMethods.escapeSingleQuotes(
						LanguageUtil.get(pageContext, "publisher_dialog_push")) %>
			</button>
			<% } else { %>
			<button dojoType="dijit.form.Button" iconClass="saveAssignIcon" onClick="validate()"
					type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
			</button>
			<% } %>
			<button dojoType="dijit.form.Button" class="dijitButtonFlat"
					onClick="dijit.byId('contentletWfDialog').hide()" type="button"
					class="dijitButtonFlat">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
		</div>
	</div>
    <script>
        dojo.parser.parse(dojo.byId("wfDivWrapperForDojo"));

        <%if(hasPushPublishActionlet){%>

			dojo.addOnLoad(function () {

                setDates();

                try {
                    dojo.empty("whereToSendTable");
                    pushHandler.clear();
                } catch (e) {
                    console.error("Error cleaning up selected environments.", e)
                }

                try {

                    if (window.lastSelectedEnvironments && window.lastSelectedEnvironments.length > 0) {
						for (var count = 0; count < window.lastSelectedEnvironments.length; count++) {
							pushHandler.addToWhereToSend(window.lastSelectedEnvironments[count].id, window.lastSelectedEnvironments[count].name);
						}
						pushHandler.refreshWhereToSend();
					} else {
						var environmentSelectStore = dijit.byId("environmentSelect").store;
						environmentSelectStore.query().then(function(response) {

							let items = response.slice(0);

							/*
							 Auto select if there is only one environment.
							 The first value is an empty item, does not count, that's why === 2
							 */
							if (items && items.length === 2) {
								dijit.byId("environmentSelect").set("value", items[1].id);
							}

						});
					}
                } catch (e) {
                    console.error("Error setting pre-selected environment.", e)
                }
			});

        <%}%>

    </script>

<% } %>