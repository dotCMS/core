<%@include file="/html/portlet/ext/browser/init.jsp"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%
java.util.List<Language> list = (java.util.List<Language>) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LIST);
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
	<table class="listingTable" style="width:90%;border:1px #eee solid;">
		<tr>
			<th><%=LanguageUtil.get(pageContext, "ISO")%></th>
            <th><%=LanguageUtil.get(pageContext, "Id")%></th>
            <th><%=LanguageUtil.get(pageContext, "Language")%></th>
            <th><%=LanguageUtil.get(pageContext, "Country")%></th>
            
		</tr>

	<%for (final Language lang : list){%>
            <%
                final long longLanguageId = lang.getId();
                final String strLanguage = lang.getLanguage();
                final String strLangCode = lang.getLanguageCode();
                final String strCountryCode = lang.getCountryCode();
                final String langIcon = LanguageUtil.getLiteralLocale(strLangCode, strCountryCode);
            %>
		<tr style="cursor: pointer;"
                 id="tdLanguage-<%=String.valueOf(longLanguageId)%>" class="tdLanguage" 
                data-href-edit-variables="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="referer" value="<%= referer %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, \"edit\")) %>" /></portlet:actionURL>"
                data-href-edit-language="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>"

              >
            
			<td onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                    <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
                    <portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
                    <portlet:param name="referer" value="<%= referer %>" />
                    <portlet:param name="<%= Constants.CMD %>" value="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, \"edit\")) %>" /></portlet:actionURL>';">
               <img
                    src="/html/images/languages/<%= langIcon %>.gif"
                    border="0" /> <%=lang %></td>   
                   <td> <%=lang.getId() %></td>
            <td><%=lang.getLanguage() %></td>
            <td><%=lang.getCountry() %></td>
                  
  


		</tr>
        <%  }%>
	</table>
	<!-- End Listing -->
	
	
    <div id="popups"></div>

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
