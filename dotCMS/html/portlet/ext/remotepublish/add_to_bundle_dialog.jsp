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

Role myRole  = APILocator.getRoleAPI().getUserRole(user);

%>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">

<div style="width:430px;" dojoType="dijit.form.Form" id="addToBundleForm">

		<div class="fieldWrapper">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "Add-br-children") %>:
			</div>
			<div class="fieldValue">
				<input type="radio" dojoType="dijit.form.RadioButton" checked="checked" onChange="pushHandler.toggleNewExistingBundle()" value="false" name="newBundle" id="existingBundle" ><label for="existingBundle"><%= LanguageUtil.get(pageContext, "publisher_dialog_existing_bundle") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton"  onChange="pushHandler.toggleNewExistingBundle()" value="true" name="newBundle" id="newBundle" ><label for="newBundle"><%= LanguageUtil.get(pageContext, "publisher_dialog_new_bundle") %></label>&nbsp;
			</div>
			<div class="clear"></div>
		</div>

		<div class="fieldWrapper" id="bundleNameDiv" style="display: none">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "publisher_dialog_bundle_name") %>:
			</div>
			<div class="fieldValue">
				<input type="text" dojoType="dijit.form.ValidationTextBox"
							  name="bundleName"
							  id="bundleName"
							  style="width:200px;"
							  value=""
							  />
			</div>
			<div class="clear"></div>
		</div>

		<div class="fieldWrapper" id="bundleSelectDiv">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_bundle") %>:
			</div>
			<div class="fieldValue">
				<input data-dojo-type="dijit/form/FilteringSelect" data-dojo-props="store:pushHandler.bundleStore, searchAttr:'name'"
			    name="bundleSelect" id="bundleSelect" />
			</div>
			<div class="clear"></div>
		</div>



		<div class="buttonRow">
			<button dojoType="dijit.form.Button" iconClass="addIcon" onClick="pushHandler.addToBundle()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
			</button>
			<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="dijit.byId('addToBundleDia').hide()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>

		</div>

		<input id="existingBundleName" type="hidden" value="<%=LanguageUtil.get(pageContext, "add_to_bundle_dialog_existing_name")%>" />
		<input id="bundleNameRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "add_to_bundle_dialog_name_required")%>" />
		<input id="bundleRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "add_to_bundle_dialog_bundle_required")%>" />

</div>
