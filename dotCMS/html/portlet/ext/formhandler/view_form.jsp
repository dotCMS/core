<%@ page import="java.util.ArrayList" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@page import="com.dotmarketing.portlets.contentlet.util.HostUtils" %>
<%@ include file="/html/portlet/ext/formhandler/init.jsp" %>

<%
	java.util.Map params = new java.util.HashMap();
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(
			request, WindowState.MAXIMIZED.toString(), params);
	List structures = (List) request
			.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURES);
	
	int STRUCTURE_TYPE_FORM = Structure.STRUCTURE_TYPE_FORM;
%>


<%@page import="com.dotcms.enterprise.LicenseUtil"%><script language="javascript">
function addNewForm()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
	href = href + "<portlet:param name='structureType' value='<%=String.valueOf(STRUCTURE_TYPE_FORM)%>' />";
	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	href = href + "</portlet:actionURL>";
	document.location.href = href;
}


</script>

<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber") != null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
	}
    int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	//java.util.Map params = new java.util.HashMap();
	params.put("struts_action", new String[] { "/ext/formhandler/view_form" });
	params.put("pageNumber", new String[] { pageNumber + "" });


	java.text.DateFormat modDateFormat = java.text.DateFormat
			.getDateTimeInstance(java.text.DateFormat.SHORT,
					java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String query = (request.getParameter("query") != null) ? request
			.getParameter("query") : (String) session
			.getAttribute(com.dotmarketing.util.WebKeys.STRUCTURE_QUERY);
	String orderby = (request.getParameter("orderBy") != null) ? request
			.getParameter("orderBy")
			: "";
			
	
    List<Integer> structureTypes = new ArrayList<Integer>();
    structureTypes.add(STRUCTURE_TYPE_FORM);
       			

%>
<script language="Javascript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

function resetSearch() {
	form = document.getElementById('fm');
	form.resetQuery.value = "true";
	form.query.value = '';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/formhandler/view_form" /></portlet:renderURL>';
	submitForm(form);
}

function submitfm() {
	form = document.getElementById('fm');	
	form.pageNumber.value = 1;
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/formhandler/view_form" /></portlet:renderURL>';
	submitForm(form);
}

function downloadToExcel(structureInode){
	
	var fieldsValues = "";
	var categoriesValues = "";
	var showDeleted = false;
	
	
	var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
	href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
	href += "<portlet:param name='cmd' value='export' />";		
	href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";		
	href += "</portlet:actionURL>";
	href += "&expStructureInode="+structureInode+"&expFieldsValues="+fieldsValues+"&expCategoriesValues="+categoriesValues+"&showDeleted="+showDeleted;
		
	window.location.href=href;	

}
</script>



<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Forms")) %>' />


	
<% if(LicenseUtil.getLevel() > 199){ %>		
<form id="fm" method="post">
	<div class="yui-g portlet-toolbar">
		<div class="yui-u first">
			<hidden name="structureType" value="3">
			<input type="hidden" name="resetQuery" value=""> 
			
			<input type="text" dojoType="dijit.form.TextBox" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
			
			<button dojoType="dijit.form.Button" onClick="submitfm()" iconClass="searchIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
			</button>

			<button dojoType="dijit.form.Button" onClick="resetSearch()" iconClass="resetIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "reset")) %>
			</button>
			<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
		</div>
		<div class="yui-u" style="text-align:right;">
			<script type="text/javascript">
				dojo.require("dijit.form.CheckBox");
			</script>
			<%	String defaultHostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
    			String host = HostUtils.filterDefaultHostForSelect(defaultHostId, "PARENT:"+PermissionAPI.PERMISSION_CAN_ADD_CHILDREN+", STRUCTURES:"+ PermissionAPI.PERMISSION_PUBLISH, user); 
			 %>
			<button dojoType="dijit.form.Button" onCLick="addNewForm();return false;" iconClass="formNewIcon" <%=UtilMethods.isSet(host)?"":"disabled"%> >
	           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Form" )) %>
	        </button>
		</div>
	</div>            
</form>
	

<form action="" method="post" name="order">
	
<!-- START Listing Results -->
<table class="listingTable" >
	<tr>
		<th width="50"><%= LanguageUtil.get(pageContext, "Action") %></th>
		<th width="250">
			<a href="<portlet:actionURL>
			<portlet:param name='struts_action' value='/ext/formhandler/view_form' />
			<portlet:param name='orderBy' value='upper(name)' /><portlet:param name='direction' value='asc'/>
			</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Form-Name") %></a>
		</th>
		<th><%= LanguageUtil.get(pageContext, "Description") %></th>
		<th width="50" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Content") %></td>
        <th width="125" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Download-to-Excel") %></td>
	</tr>
	
	<%
		int structuresSize = ((Integer) request.getAttribute(com.dotmarketing.util.WebKeys.STRUCTURES_VIEW_COUNT)).intValue();
		if (structures.size() > 0) {
			for (int i = 0; i < structures.size(); i++) {
			Structure structure = (Structure) structures.get(i);
			String str_style = (i % 2 == 0 ? "class=\"alternate_1\""
			: "class=\"alternate_2\"");
	%>
		<tr <%=str_style%> >
			<td align="center">
				<a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
					 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
					 	<portlet:param name='inode' value='<%=structure.getInode()%>' />
					 	<portlet:param name='referer' value='<%=referer%>' />
					 	</portlet:actionURL>">
					    <span class="editIcon"></span>
				</a>
			</td>
			<td>
					<a  class="gamma" href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
				<portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' />
				<portlet:param name='structure_id' value='<%=structure.getInode()%>' />
				</portlet:actionURL>">
						  	<%=structure.getName()%>
					</a>
			</td>
			<td><%=!UtilMethods.isSet(structure.getDescription())?"":structure.getDescription() %></td>
			<td align="center" width="200">
				<a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
				<portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' />
				<portlet:param name='structure_id' value='<%=structure.getInode()%>' />
				</portlet:actionURL>">
				<%= LanguageUtil.get(pageContext, "view") %></a>
				</td>
			<td align="center"><a href="javascript:downloadToExcel('<%=structure.getInode()%>')"><img src='/icon?i=csv.xls' border='0' alt='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "export-results")) %>' alt='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "export-results")) %>' align='absbottom'></a></td>
		</tr>
	<% } %>
<% } %>
<!-- END Listing Results -->


<!-- Start No Results -->
<% if (structuresSize == 0) { %>
	<tr>
		<td colspan="5">
			<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Forms-to-display") %></div>
		</td>
	</tr>
<% } %>
<!-- End No Results -->

</table>

<!-- Start Pagination -->
<div class="yui-gb buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/formhandler/view_form" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="orderBy" value="<%= orderby %>" /></portlet:renderURL>';return false;" iconClass="previousIcon">
				<%= LanguageUtil.get(pageContext, "Previous") %> <%= perPage %> <%= LanguageUtil.get(pageContext, "Results") %>
			</button>
		<% } %>&nbsp;
	</div>
	<div class="yui-u" style="text-align:center;">
		<%= LanguageUtil.get(pageContext, "Viewing") %>
		<%= minIndex+1 %> - 
		<% if (maxIndex > (minIndex + structuresSize)) { %>
	    	<%= minIndex + structuresSize %>
		<%}else{ %>
			<%= maxIndex %>
		<% } %>
		
		<%= LanguageUtil.get(pageContext, "of1") %>
	    <% if (100 <= structuresSize) { %>
			<%= LanguageUtil.get(pageContext, "hundreds") %>
		<%}else{ %>
			<%= minIndex + structuresSize %>
		<%} %>
	</div>
	<div class="yui-u" style="text-align:right;">	
		<% if (maxIndex < (minIndex + structuresSize)) { %>
	        <button dojoType="dijit.form.Button" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/formhandler/view_form" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /></portlet:renderURL>';return false;" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %> <% if ((structuresSize - maxIndex) < perPage){ %> <%= ((minIndex + structuresSize)-maxIndex) %> <%}else{%> <%= perPage %><%}%> <%= LanguageUtil.get(pageContext, "Results") %>
	    	</button>
		<% } %>&nbsp;
	</div>
</div>
<!-- END Pagination -->

</form>
<% }else{ %>
	<%@ include file="/html/portlet/ext/formhandler/not_licensed.jsp" %>


<% } %>
</liferay:box>