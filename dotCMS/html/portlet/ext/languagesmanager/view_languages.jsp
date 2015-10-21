<%@include file="/html/portlet/ext/browser/init.jsp"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%
java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LIST);
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/languages_manager/view_languages_manager"});
String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
%>

<style type="text/css" media="all">
    @import url(/html/portlet/ext/browser/browser.css);
</style>

<script type="text/javascript">
dojo.require("dotcms.dojo.push.PushHandler");

<% if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())) { %>
var cmsAdminUser = true;
<% } else { %>
var cmsAdminUser = false;
<% } %>

var enterprise = <%=LicenseUtil.getLevel() > 199%>;
<%PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();%>
var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

var pushHandler = new dotcms.dojo.push.PushHandler('Push Publish');
</script>

<liferay:box top="/html/common/box_top.jsp"
	bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title"
		value='<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Languages"))%>' />

	<div class="buttonRow" style="text-align: right;">
		<button dojoType="dijit.form.Button" onClick="editDefault"
			iconClass="editIcon"><%=LanguageUtil.get(pageContext, "Edit-Default-Language-Variables")%></button>
		<button dojoType="dijit.form.Button" onClick="addLanguage"
			iconClass="plusIcon"><%=LanguageUtil.get(pageContext, "Add-New-Language")%></button>
	</div>
	<table class="listingTable" id="listingLanguagesTable">
		<tr>
			<th colspan="2"><%=LanguageUtil.get(pageContext, "Languages")%></th>
		</tr>

	<%
	    int l = 0;
	    for (int k=0; k < (list.size()+1)/2;k++) {
	%>

		<tr>
		<%
		    for (int m=0; m < 2  ;m++) {
		%>
			<%
		    if(l < list.size()) {
		        final long longLanguageId = ((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getId();
			    final String strLanguage = ((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getLanguage();
			    final String strLangCode = ((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getLanguageCode();
			    final String strCountryCode = ((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getCountryCode();
			%>
			<td id="tdLanguage-<%=String.valueOf(longLanguageId)%>" class="tdLanguage" 
                data-href-edit-variables="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="referer" value="<%= referer %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, \"edit\")) %>" /></portlet:actionURL>"
                data-href-edit-language="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>">
                <a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="referer" value="<%= referer %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, \"edit\")) %>" /></portlet:actionURL>">
					<img
					src="/html/images/languages/<%= strLangCode %>_<%= strCountryCode %>.gif"
					border="0" />
			    </a> <%= strLanguage+"&nbsp;("+strCountryCode+")&nbsp;" %>
			</td>
		    <% } else { %>
		        <td></td>
		    <% } %>
		<%     l++;
	        }
		%>
		</tr>
	<%
	    }
	%>
	</table>
    <div id="popups"></div>
</liferay:box>
<form id="remotePublishForm">
	<input name="assetIdentifier" id="assetIdentifier" type="hidden"
		value=""> <input name="remotePublishDate"
		id="remotePublishDate" type="hidden" value=""> <input
		name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
	<input name="remotePublishExpireDate" id="remotePublishExpireDate"
		type="hidden" value=""> <input name="remotePublishExpireTime"
		id="remotePublishExpireTime" type="hidden" value=""> <input
		name="iWantTo" id=iWantTo type="hidden" value=""> <input
		name="whoToSend" id=whoToSend type="hidden" value=""> <input
		name="bundleName" id=bundleName type="hidden" value=""> <input
		name="bundleSelect" id=bundleSelect type="hidden" value=""> <input
		name="forcePush" id=forcePush type="hidden" value="">
</form>

<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>
<%@ include file="/html/portlet/ext/languagesmanager/view_languages_js_inc.jsp"%>
<script type="text/javascript">
dojo.addOnLoad(function() {
    $$(".tdLanguage").each(function(element) {
        // Remove default event
        $(element).observe("contextmenu", function(e) {
            Event.stop(e);
        });

        // Add right click event
        $(element).observe("mouseup", function(e) {
            if(isRightClick(e)){
                var hrefVariables = {};
                hrefVariables["editVariablesHref"] = element.getAttribute("data-href-edit-variables");
                hrefVariables["editLanguageHref"] = element.getAttribute("data-href-edit-language");

                showLanguagePopUp(element.id.replace("tdLanguage-", "").strip(), hrefVariables, cmsAdminUser, "<%=referer%>", e);
            }
        });
    });
});
</script>
