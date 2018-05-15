<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<div dojoType="dijit.form.Form" id="importSchemeForm" jsId="importSchemeForm" encType="multipart/form-data" action="#" method="POST">
    <!-- START Listing Results -->
    <div class="form-horizontal">
        <dl><%= LanguageUtil.get(pageContext, "File-to-Import-JSON-File-Required") %></dl>
        <dl>
            <input type="file" name="schemejsonfile" id="schemejsonfile" />
        </dl>
    </div>

    <div class="buttonRow" style="margin-top: 20px;">
        <button dojoType="dijit.form.Button" onClick='schemeAdmin.importScheme()' iconClass="saveIcon" type="button">
            <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
        </button>
        <button dojoType="dijit.form.Button"
                onClick='schemeAdmin.hideImport()' class="dijitButtonFlat" type="button">
            <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
        </button>
    </div>

</div>

<div id="importWorkflowErrors" style="display: none;" dojoType="dijit.Dialog" class="content-edit__dialog-error" title="<%= LanguageUtil.get(pageContext, "error") %>">
    <div dojoType="dijit.layout.ContentPane" id="importWorkflowExceptionData" hasShadow="true"></div>
    <div class="content-edit__dialog-error-actions">
        <button dojoType="dijit.form.Button" class="dijitButtonFlat" onClick="dijit.byId('importWorkflowErrors').hide()" type="button"><%= LanguageUtil.get(pageContext, "close") %></button>
    </div>
</div>