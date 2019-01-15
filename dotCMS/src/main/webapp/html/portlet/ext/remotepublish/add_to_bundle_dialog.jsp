<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%
String inode=request.getParameter( "inode" );// DOTCMS-7085
%>

<style type="text/css">
    .progressRow {
        padding-left: 30%;
    }
</style>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">

<div dojoType="dijit.form.Form" id="addToBundleForm">

		 <%--DATE FILTERING BOX--%>
        <div class="fieldWrapper" id="filterTimeDiv_atb" style="display: none;">
            <div class="fieldName" style="width:120px">
                <%= LanguageUtil.get( pageContext, "publish.created.after" ) %>:
            </div>
            <div class="fieldValue">
                <input type="text" dojoType="dijit.form.DateTextBox" value="now" required="true"
                       id="wfFilterDateAux_atb" name="wfFilterDateAux_atb" style="width: 110px;">

                <input type="text" data-dojo-type="dijit.form.TimeTextBox" value="now" required="true"
                       id="wfFilterTimeAux_atb" name="wfFilterTimeAux_atb" style="width: 100px;"/>
            </div>
            <div class="clear"></div>
        </div>
        <%--DATE FILTERING BOX--%>

		<div class="fieldWrapper" id="bundleSelectDiv">
			<div class="fieldName" style="width:120px">
				<%= LanguageUtil.get(pageContext, "publisher_dialog_choose_bundle") %>:
			</div>
			<div class="fieldValue">
				<input data-dojo-type="dijit/form/ComboBox" autocomplete="false" data-dojo-props="store:pushHandler.bundleStore, searchAttr:'name', pageSize:30"
			    name="bundleSelect" id="bundleSelect" />
			</div>
			<div class="clear"></div>
		</div>

        <div class="progressRow" style="display: none;">
            <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveAddBundleProgress" id="saveAddBundleProgress"></div>
        </div>
		<div class="buttonRow-right">
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" onClick="dijit.byId('addToBundleDia').hide()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
			<button dojoType="dijit.form.Button" type="button" id="addToBundleSaveButton">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
			</button>
		</div>

		<input id="bundleNameRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "add_to_bundle_dialog_name_required")%>" />
		<input id="bundleRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "add_to_bundle_dialog_bundle_required")%>" />

</div>

<%@ include file="/html/portlet/ext/remotepublish/remote_publish_form_inc.jsp" %>
