<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>

<!-- JSP Imports --> 
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<!--  Initialization Code -->
<% 
	HashMap<String, List<String>> processResults = (HashMap<String, List<String>>) request.getAttribute("importResults");
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"import-contentlets-results\") %>" />
<div style="padding:10px;">
	<div class="shadowBox headerBox" style="width:600px;margin:auto;padding:10px 20px;margin-bottom:20px;">
		<h3 style="margin-bottom:20px;"><%= LanguageUtil.get(pageContext, "import-contentlets-results") %></h3>
			
		<%if(UtilMethods.isSet(processResults)){%>
			<%
				List<String> resultMessages = processResults.get("results");
			%>
			<div style="padding:5px;">
				<span class="workflowIcon"></span>
				<%if(UtilMethods.isSet(resultMessages)){%>
					<b><%= LanguageUtil.get(pageContext, "Success") %></b>
				<%} else {%>
					<b><%= LanguageUtil.get(pageContext, "message.import.process.executing.in.background") %></b>
				<%}%>
			</div>
			<%if(UtilMethods.isSet(resultMessages)){%>
				<ul class="withBullets" style="margin-bottom:10px;">
					<%
						for (String message : resultMessages) {
					%>
							<li><%= message %></li>
					<%
						}
					%>
				</ul>
			<%}%>
			<div style="padding:30px;">
				<%= LanguageUtil.get(pageContext, "Import-in-Background") %>
			</div>
		<%}else{ %>
			<div style="padding:5px;">
				<span class="closeIcon"></span>
				<b><%= LanguageUtil.get(pageContext, "oops") %></b>
			</div>
			<div style="padding-left:30px;padding-right:30px;">
				<%= LanguageUtil.get(pageContext, "Import-Duplicate-Prevention") %>
			</div>
		<%} %>				
			
	</div>
</div>
<div style="text-align:center">
	<button dojoType="dijit.form.Button" id="importAgainButton"  onclick="window.location='<portlet:actionURL>
		<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />
		<portlet:param name="selectedStructure" value="<%= ((ImportContentletsForm)request.getAttribute(\"ImportContentletsForm\")).getStructure() %>" />
		</portlet:actionURL>';" iconClass="resetIcon">
	    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Do-Another-Import")) %>
	</button>
	&nbsp;&nbsp;&nbsp;&nbsp;
	<button dojoType="dijit.form.Button" id="goBackButton"  onclick="window.location='<portlet:actionURL>
		<portlet:param name="struts_action" value="/ext/contentlet/view_contentlets" />
		</portlet:actionURL>';" iconClass="nextIcon">
	    <%= LanguageUtil.get(pageContext, "Back-to-Content") %>
	</button>
</div>
</liferay:box>