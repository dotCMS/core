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
<%
String inode=request.getParameter("inode");// DOTCMS-7085
GregorianCalendar cal = new GregorianCalendar();

%>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">

<div style="width:500px;" dojoType="dijit.form.Form" id="publishForm">

        <%--DATE FILTERING BOX--%>
        <div class="fieldWrapper" id="filterTimeDiv" style="display: none;">
            <div class="fieldName" style="width:120px">
                <%= LanguageUtil.get( pageContext, "publish.created.after" ) %>:
            </div>
            <div class="fieldValue">
                <input type="text" dojoType="dijit.form.DateTextBox" value="now" required="true"
                       id="wfFilterDateAux" name="wfFilterDateAux" style="width: 110px;">

                <input type="text" data-dojo-type="dijit.form.TimeTextBox" value="now" required="true"
                       id="wfFilterTimeAux" name="wfFilterTimeAux" style="width: 100px;"/>
            </div>
            <div class="clear"></div>
        </div>
        <%--DATE FILTERING BOX--%>

		<div class="fieldWrapper">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "I-want-to") %>:
			</div>
			<div class="fieldValue">
				<input type="radio" dojoType="dijit.form.RadioButton" checked="true" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH %>" name="wfIWantTo" id="iwtPublish" ><label for="iwtPublish"><%= LanguageUtil.get(pageContext, "publish") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE %>" name="wfIWantTo" id="iwtExpire" ><label for="iwtExpire"><%= LanguageUtil.get(pageContext, "delete") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE %>" name="wfIWantTo" id="iwtPublishExpire" ><label for="iwtPublishExpire"><%= LanguageUtil.get(pageContext, "publish") %> &amp; <%= LanguageUtil.get(pageContext, "delete") %></label>
			</div>
			<div class="clear"></div>
		</div>


		<%
			String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
			String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
		%>
		<div class="fieldWrapper" id="publishTimeDiv">

			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "Publish") %>:
			</div>
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

		<div class="fieldWrapper" id="expireTimeDiv" style="display:none">
			<div class="fieldName" style="width:120px"><%= LanguageUtil.get(pageContext, "publisher_Expire") %> :
			</div>
			<div class="fieldValue">
			<input
				type="text"
				dojoType="dijit.form.DateTextBox"
				validate="return false;"
				id="wfExpireDateAux" name="wfExpireDateAux" value="now" style="width: 110px;">


			<input type="text" name="wfExpireTimeAux" id="wfExpireTimeAux" value="now"
			    data-dojo-type="dijit.form.TimeTextBox"
				style="width: 100px;" />
			</div>
			<div class="clear"></div>
		</div>

		<div class="fieldWrapper">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_environment") %>:
			</div>
			<div class="fieldValue">
				<input data-dojo-type="dijit/form/FilteringSelect" required="false"  data-dojo-props="store:pushHandler.environmentStore, searchAttr:'name', displayedValue:'0'"
			    name="environmentSelect" id="environmentSelect" onChange="pushHandler.addSelectedToWhereToSend()" style="width:250px" />

				<div class="wfWhoCanUseDiv">
					<table class="listingTable" id="whereToSendTable">
					</table>
				</div>
				<input type="hidden" name="whereToSend" id="whereToSend" value="">
			</div>
			<div class="clear"></div>
		</div>

		<div class="buttonRow">
			<button dojoType="dijit.form.Button" iconClass="saveAssignIcon" onClick="pushHandler.remotePublish()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
			</button>
			<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="window.lastSelectedEnvironments = pushHandler.inialStateEnvs; pushHandler.inialStateEnvs = new Array();dijit.byId('remotePublisherDia').hide()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>

		</div>

		<input id="whereToSendRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>" />

</div>
