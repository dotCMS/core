<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

	String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX");
	if(!UtilMethods.isSet(regex)){
		regex=".*";
	}
	String[] files = FileUtil.listFiles(Config.CONTEXT.getRealPath(Config.getStringProperty("TAIL_LOG_LOG_FOLDER")));
	Pattern p = Pattern.compile(regex);  
	List<String> l = new ArrayList<String>();
	for(String x : files){
		if(p.matcher(x).matches()){  
			l.add(x);   
		}
	}
	// http://jira.dotmarketing.net/browse/DOTCMS-6271
	// put matched files set to an array with exact size and then sort them
	files = l.toArray(new String[l.size()]);
	Arrays.sort(files);
	


	
	
	try {
		user = com.liferay.portal.util.PortalUtil.getUser(request);
	} catch (Exception e) {
		response.sendError(403);
		return;
	}
	try {
		if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
			response.sendError(403);
			return;
		}
	} catch (Exception e2) {
		Logger.error(this.getClass(), e2.getMessage(), e2);
		response.sendError(403);
		return;
	}

	
%>

<%request.setAttribute("popup", "true"); %>
<%@ include file="/html/common/top_inc.jsp" %>




<div id="search" title="<%= LanguageUtil.get(pageContext, "LOG_activity") %>" >

<div style="width:90%;margin:auto;">


	<table class="listingTable" id="logsTable" align="center">
		<tr id="logsTableHeader">
			<th><input width="5%" type="checkbox" dojoType="dijit.form.CheckBox" id="checkAllCkBx" value="true" onClick="checkUncheck()" /></th>
			<th nowrap="nowrap" width="5%" style="text-align:center;">Status</th>
			<th nowrap="nowrap" width="32%" style="text-align:center;">Log Name</th>
			<th nowrap="nowrap" width="58%" style="text-align:center;">Log Description</th>
		</tr>
	</table>


</div>
<div>&nbsp;</div>
<div class="buttonRow">
    <button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" onclick="enableDisableLogs()"> <%= LanguageUtil.get(pageContext, "LOG_button") %> </button>
    </div>
</div>
