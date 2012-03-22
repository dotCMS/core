<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>

<!-- JSP Imports --> 
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<script type='text/javascript' src='/dwr/interface/ImportContentletAjax.js'></script>

<!--  Initialization Code -->
<% 
	HashMap<String, List<String>> previewResults = (HashMap<String, List<String>>) request.getAttribute("previewResults");
%>

<script type='text/javascript'>
	var impSubmit=false;
	var importId=<%=(Long) request.getAttribute("importId")%>;
	<% 
	Long runAjaxImportCheck = (Long) request.getAttribute("importId");
	if(runAjaxImportCheck!=null && runAjaxImportCheck > 0){
		session.setAttribute("importId",(Long) request.getAttribute("importId"));
		%>
	
	<%}else{
		session.setAttribute("previewResults",previewResults);
	}%>
	
	function  importCheckCallback(response){
        if(response){
			var href =  '<portlet:actionURL>';
			href +=		'<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />';
			href +=		'<portlet:param name="cmd" value="publish" />';
			href +=		'<portlet:param name="subcmd" value="importContentletsResults" />';
			href +=	'</portlet:actionURL>';
			var form = document.getElementById("importForm");
			form.action = href;
			form.submit();
        }
        else{
        	setTimeout('importCheck(importId)', 5000);
        }
    }

	function importCancelCallback(response){
		importCheck(response);
	}
    
    function importCheck(id){
    	ImportContentletAjax.checkImportStatus(id,importCheckCallback);
    }

    function importCancel(id){
    	ImportContentletAjax.cancelImport(id,importCancelCallback);
    }
    
	function submitForm () {
		if(impSubmit){
			alert("This import job has already been submitted");
			return;
		}
		impSubmit=true;
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.proceed.importing")) %>')) {
			var href =  '<portlet:actionURL>';
				href +=		'<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />';
				href +=		'<portlet:param name="cmd" value="publish" />';			
				href +=	'</portlet:actionURL>';
			var form = document.getElementById("importForm");
			form.action = href;
			var dia = dijit.byId('dotImportContentDialog');
			dojo.style(dia.closeButtonNode, "visibility", "hidden"); 			
			dijit.byId("proceedButton").display = "none";
			dijit.byId("goBackButton").display = "none";
			dia.show();
			
			form.submit();
		}
	}
	
	function goBack () {
		var href =  '<portlet:actionURL>';
			href +=		'<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />';
			href +=	'</portlet:actionURL>';
		document.location = href;
	}
	
</script>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"import-contentlets-preview\") %>" />

<html:form action="/ext/contentlet/import_contentlets" styleId="importForm" onsubmit="return false;">
	<input type="hidden" name="cmd" value="publish" />
	<html:hidden property="structure" />
	<html:hidden property="language" />
<%
	ImportContentletsForm form = (ImportContentletsForm) request.getAttribute("ImportContentletsForm");
	for (String field: form.getFields()) {
%>
	<input type="hidden" name="fields" value="<%= field %>" />
<%
	}
%>

<div id="dotImportContentDialog" dojoType="dijit.Dialog" style="display:none" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Processing-Please-be-patient")) %>">
	<div dojoType="dijit.layout.ContentPane" style="width:300px;height:100px;text-align: center;vertical-align: middle;padding:20px;" class="box" hasShadow="true">
		<div style="width:300px"  indeterminate="true" id="indeterminateBar1"
			dojoType="dijit.ProgressBar"></div>
			<div style="padding:5px;text-align: center;">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "import-cannot-be-undone")) %>
			</div>
	</div>
</div>

<div class="shadowBox headerBox" style="width:600px;margin:auto;padding:10px 20px;margin-bottom:20px;">
	<h3 style="margin-bottom:20px;"><%= LanguageUtil.get(pageContext, "Preview-Analysis-Results") %></h3>
	
	<h4><%= LanguageUtil.get(pageContext, "Summary") %></h4>
	<ul class="withBullets" style="margin-bottom:20px;">
		<%
			List<String> messages = previewResults.get("messages");
			for (String message : messages) {
	    %>
	    	<li><%= message %></li>
	    <% } %>						
	</ul>


	<h4><%= LanguageUtil.get(pageContext, "Warnings") %>:</h4>
	<ul class="withBullets" style="margin-bottom:20px;">
		<%
			List<String> warnings = previewResults.get("warnings");
			for (String warning : warnings) {
	    %>
	    		<li><%= warning %></li>
	     <% } %>							
	
		<% if(warnings.size() == 0) { %>
	    	<li><%= LanguageUtil.get(pageContext, "No-warnings-found") %></li>
		<% } %>
	</ul>

	<h4><%= LanguageUtil.get(pageContext, "Errors") %>:</h4>
	<ul class="withBullets" style="margin-bottom:20px;">
		<%
			List<String> errors = previewResults.get("errors");
			for (String error : errors) {
	    %>
		    <li><%= error %></li>
	    <% } %>						
	
		<% if(errors.size() == 0) { %>
	    	<li><%= LanguageUtil.get(pageContext, "No-errors-were-found-in-the-file") %></li>
		<% } %>
	</ul>

	<div class="buttonRow">
	    <button dojoType="dijit.form.Button" id="goBackButton"  onclick="goBack()" iconClass="cancelIcon">
	        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
	    </button>
	    <button dojoType="dijit.form.Button" onClick="submitForm(document.fm)" id="proceedButton" onclick="submitForm()" iconClass="uploadIcon">
	        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import Content" )) %>
	    </button>
	</div>

</div>

</html:form>

</liferay:box>
<script type='text/javascript'>
	dojo.addOnLoad(function(){
		 if(importId!=null){
				var dia = dijit.byId('dotImportContentDialog');
				dojo.style(dia.closeButtonNode, "visibility", "hidden"); 
				dia.show();
				importCheck(importId);
				}
	});
</script>









