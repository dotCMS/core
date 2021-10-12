<%@include file="/html/portlet/ext/browser/init.jsp"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
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

var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;
<%PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();%>
var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

var pushHandler = new dotcms.dojo.push.PushHandler('Push Publish');
</script>

<liferay:box top="/html/common/box_top.jsp"
	bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title"
		value='<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Languages"))%>' />

<div class="portlet-main">
	<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			
		</div>
		<div class="portlet-toolbar__info">
			
		</div>
    	<div class="portlet-toolbar__actions-secondary">
    		<button dojoType="dijit.form.Button" onClick="editDefault" iconClass="editIcon">
    			<%=LanguageUtil.get(pageContext, "Edit-Default-Language-Variables")%>
    		</button>
			<button dojoType="dijit.form.Button" onClick="addLanguage" iconClass="plusIcon">
				<%=LanguageUtil.get(pageContext, "Add-New-Language")%>
			</button>
    	</div>
   </div>
   <!-- END Toolbar -->



	<!-- Listing -->
	<table class="listingTable" id="listingLanguagesTable">
		<tr>
			<th colspan="2"><%=LanguageUtil.get(pageContext, "Languages")%></th>
		</tr>

	<%
		final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
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
			    final String langIcon = LanguageUtil.getLiteralLocale(strLangCode, strCountryCode);
				final boolean isDefaultLang = (defaultLang.getId() == longLanguageId);
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
                    <portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>"
				data-href-make-default="<%= isDefaultLang ? "" : String.valueOf(longLanguageId)%>"
			>
                <a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="referer" value="<%= referer %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, \"edit\")) %>" /></portlet:actionURL>"
						<% if(isDefaultLang){ %>
				          title="<%=LanguageUtil.get(pageContext, "default-language")%>"
						<% } %>
				>
					<img src="/html/images/languages/<%= langIcon %>.gif" border="0" />
					<%= strLanguage %>&nbsp;<%= (UtilMethods.isSet(strCountryCode) ? ("(" + strCountryCode + ")&nbsp;") : StringPool.BLANK) %>
			    </a>

				<% if(isDefaultLang){ %>
				   <span class="liveIcon"></span>
				<% } %>
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
	<!-- End Listing -->
	
	
    <div id="popups"></div>
</liferay:box>

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
                hrefVariables["makeDefaultLanguageHref"] = element.getAttribute("data-href-make-default");
                showLanguagePopUp(element.id.replace("tdLanguage-", "").strip(), hrefVariables, cmsAdminUser, "<%=referer%>", e);
            }
        });
    });
});
</script>
