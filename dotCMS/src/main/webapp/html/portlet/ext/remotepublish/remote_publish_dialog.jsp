<%@page import="com.dotcms.publisher.ajax.RemotePublishAjaxAction"%>
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
<%@ page import="com.dotmarketing.util.DateUtil" %>
<%
String inode=request.getParameter("inode");// DOTCMS-7085
GregorianCalendar cal = new GregorianCalendar();
Date dateValue = new Date();
cal.setTime(dateValue);

String currentDateStr = DateUtil.format(dateValue, "yyyy-MM-dd");

String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);

%>

<style type="text/css">
    .progressRow {
        padding-left: 33%;
    }
</style>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">

<div class="form-horizontal" dojoType="dijit.form.Form" id="publishForm">

        <%--DATE FILTERING BOX--%>
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
					<div class="radio"><input type="radio" dojoType="dijit.form.RadioButton" checked="true" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH %>" name="wfIWantTo" id="iwtPublish" ><label for="iwtPublish"><%= LanguageUtil.get(pageContext, "push") %></label></div>
					<div class="radio"><input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE %>" name="wfIWantTo" id="iwtExpire" ><label for="iwtExpire"><%= LanguageUtil.get(pageContext, "Remove") %></label></div>
					<div class="radio"><input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE %>" name="wfIWantTo" id="iwtPublishExpire" ><label for="iwtPublishExpire"><%= LanguageUtil.get(pageContext, "push") %> <%= LanguageUtil.get(pageContext, "Remove") %></label></div>
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
					<input type="checkbox" data-dojo-type="dijit/form/CheckBox"  name="forcePush" id="forcePush" value="true"><label for="forcePush"><%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %></label>
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
				id="wfExpireDateAux" name="wfExpireDateAux" value='<%=currentDateStr%>' style="width: 137px;">
			<input type="text" name="wfExpireTimeAux" id="wfExpireTimeAux" value='T<%=hour+":"+min%>:00'
			    data-dojo-type="dijit.form.TimeTextBox"
				style="width: 120px;" />
			</dd>
		</dl>

		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_environment") %>:
			</dt>
			<dd>
				<input data-dojo-type="dijit/form/FilteringSelect" required="false"  data-dojo-props="store:pushHandler.environmentStore, searchAttr:'name', displayedValue:'0'"
			    name="environmentSelect" id="environmentSelect" onChange="pushHandler.addSelectedToWhereToSend()" style="width:260px" />

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
			<button dojoType="dijit.form.Button" onClick="pushHandler.remotePublish()" type="button" id="remotePublishSaveButton">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_dialog_push")) %>
			</button>
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" onClick="window.lastSelectedEnvironments = pushHandler.inialStateEnvs; pushHandler.inialStateEnvs = new Array();dijit.byId('remotePublisherDia').hide()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
		</div>

		<input id="whereToSendRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>" />

</div>
