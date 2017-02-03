<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@ include file="/html/portlet/ext/containers/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/containers/view_containers"});
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);

	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);
	
	String showDeleted = (request.getParameter("showDeleted")!=null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_SHOW_DELETED);
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_QUERY);
	String orderby = (request.getParameter("orderby")!=null) ? request.getParameter("orderby") : "";

	//long structureId = 0;
	String structureId = "";
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_STRUCTURE_ID) != null)
			//structureId = Long.parseLong((String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_STRUCTURE_ID));
			structureId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_STRUCTURE_ID);
		if (request.getParameter("structure_id") != null)
			 structureId = request.getParameter("structure_id");
		    //structureId = Long.parseLong(request.getParameter("structure_id"));
	} catch (NumberFormatException e) {	}
	List structures = StructureFactory.getStructures();
	
	String popup = request.getParameter ("popup");
	String view = java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted = " + com.dotmarketing.db.DbConnectionFactory.getDBFalse() + ")","UTF-8");
%>



<%@page import="com.dotmarketing.util.UtilMethods"%>
<script language="Javascript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

function resetSearch() {
	form = document.getElementById('fm');
	form.showDeleted.value = '';
	form.resetQuery.value = "true";  
	form.structure_id.value="0";
	form.query.value = '';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/containers/view_containers_popup" /></portlet:renderURL>&popup=inode&child=true&page_width=650';
	submitForm(form);
}
function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/containers/view_containers_popup" /></portlet:renderURL>&popup=inode&child=true&page_width=650';
	submitForm(form);
}

</script>
<%  referer = java.net.URLEncoder.encode(referer,"UTF-8"); %>

<jsp:include page="/html/portlet/ext/folders/view_folders_js.jsp" />

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-containers-all") %>' />

	<table border="0" cellpadding="2" cellspacing="2" width="100%">
		<tr>
			<td align=right>
			<form id="fm" method="post">

			<input type="hidden" name="resetQuery" value="">
			<select name="structure_id">
				<OPTION value=""><%= LanguageUtil.get(pageContext, "Any-Structure") %></OPTION>
				<%
					Iterator structuresIt = structures.iterator();
					while (structuresIt.hasNext()) {
						Structure next = (Structure)structuresIt.next();
				%>
				<OPTION value="<%=next.getInode()%>" <%=structureId.equalsIgnoreCase(next.getInode())?"selected":""%>><%=next.getName()%></OPTION>
				<%	
					}
				%>
			</select>
			<input type="text" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
            <button dojoType="dijit.form.Button" onClick="submitfm()">
                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %>    
            </button>
			<button dojoType="dijit.form.Button"  onClick="resetSearch()">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "reset")) %>   
            </button>
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>">
			<input type="checkbox" name="showDeleted" onClick="javascript:submitfm();" 
			<%= (showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : "" %> value="true">
			<font class="gamma" size="2"><%= LanguageUtil.get(pageContext, "Show-Deleted") %></font>
			</form>
			</td>
		</tr>
		<tr>
			<td>
				<form id="fm_publish" method="post">
				<input type="hidden" name="referer" value="<%=referer%>">
				<input type="hidden" name="cmd" value="prepublish">
				<table border="0" cellpadding="2" cellspacing="2" width="100%">
					<tr class="beta">
						<Td width="50">
						<B><font class="beta" size="2">
						<%= LanguageUtil.get(pageContext, "Status") %>
						</font></B>
						</td>
						<Td>
						<B><font class="beta" size="2">
						<b><a class="beta" href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/containers/view_containers_popup' /><portlet:param name='pageNumber' value='<%= String.valueOf(pageNumber) %>' /></portlet:renderURL>&view='<%=view %>'&popup=inode&child=true&page_width=900">
						<%= LanguageUtil.get(pageContext, "Title") %></a>
						</font></B>
						</td>
						<Td>
						<B><font class="beta" size="2">
						<b><a class="beta" href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/containers/view_containers_popup' /><portlet:param name='pageNumber' value='<%= String.valueOf(pageNumber) %>' /><portlet:param name='orderby' value='mod_date' /></portlet:renderURL>&view='<%=view %>'&popup=inode&child=true&page_width=900">
						<%= LanguageUtil.get(pageContext, "Mod-Date") %></a>
						</font></B>
						</td>
					</tr>

					<% 
						java.util.List containers = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINERS_VIEW);
						int containersSize = ((Integer) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINERS_VIEW_COUNT)).intValue();
						String userId = user.getUserId();

						for (int k=0;k<containers.size();k++) { 
							com.dotmarketing.beans.PermissionAsset permAsset = (com.dotmarketing.beans.PermissionAsset) containers.get(k);							
							
							//gets permissions listings
							java.util.List permissions = permAsset.getPermissions();
							//gets template
							com.dotmarketing.portlets.containers.model.Container container = (com.dotmarketing.portlets.containers.model.Container) permAsset.getAsset();
							String escapedContainerTitle = container.getTitle();
							if( UtilMethods.isSet(escapedContainerTitle) ) {
							  escapedContainerTitle = escapedContainerTitle.replace("\"","\\\"");
							}

							String str_style = "";
							if ((k%2)==0) {
								str_style = "bgcolor=#EEEEEE";
							}

							//container properties and permissions
							//String inode = Long.toString(container.getInode());
							String inode = container.getInode();
							String live = (container.isLive())?"1":"0";
							String working = (container.isWorking())?"1":"0";
							String write = (permissions.contains(String.valueOf(PermissionAPI.PERMISSION_WRITE)))?"1":"0";

						%>
						<tr>
							<td <%=str_style%> nowrap="true">
								<%=UtilHTML.getStatusIcons(container) %>
			
							</td>
							<td <%=str_style%> > <!-- //jira-2143 <script type="text/javascript">
							var title<%=inode%>  = "<%= escapedContainerTitle %>";
							</script>-->
								<font class="gamma" size="2">
								<a class="bg" href="#" onclick='selectTreeLeaf("<%= popup %>","<%=inode%>","<%= escapedContainerTitle %>","",""); '><%=container.getTitle()%></a>
								</font>
							</td>
							<td <%=str_style%> >
								<font class="gamma" size="2">
								<%=modDateFormat.format(container.getModDate())%>
								</font>
							</td>
						</tr>
						
					<%}%>
						<tr>
							<td colspan="3" align=left>
							<% if (minIndex != 0) { %>
								<font class="gamma" size="2">
								<b><button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/containers/view_containers_popup' /><portlet:param name='pageNumber' value='<%= String.valueOf(pageNumber - 1) %>' /><portlet:param name='orderby' value='<%=orderby %>' /></portlet:actionURL>&view=<%=view %>&popup=inode&child=true&page_width=900'" iconClass="previousIcon">
								<%= LanguageUtil.get(pageContext, "Previous") %></button></b>
								</font>
							<% } %>
							</td>
							<td colspan="3" align=right>
							<% if (maxIndex < containersSize) { %>
								<font class="gamma" size="2">
								<b><button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/containers/view_containers_popup' /><portlet:param name='pageNumber' value='<%= String.valueOf(pageNumber + 1) %>' /><portlet:param name='orderby' value='<%=orderby %>' /></portlet:actionURL>&view=<%=view %>&popup=inode&child=true&page_width=900'" iconClass="nextIcon">
								<%= LanguageUtil.get(pageContext, "Next") %></button></b>
								</font>
							<% } %>
							</td>
						</tr>
					<% if (containersSize ==0) { %>
					<tr>
						<td colspan="6" align=center>
						<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "There-are-no-Container- to-show") %></font>
						</td>
					</tr>
					<% } %>
					<tr>
						<td colspan="6">
							<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Status-Legend") %>:</b></font>
							<table border=0 cellpadding=2>
								<tr>
									<td valign="middle"><span class="liveIcon"></span></td>
									<td valign="middle"><font class="bg" size="1"><%= LanguageUtil.get(pageContext, "live") %></font></td>
									<td valign="middle"><span class="archivedIcon"></span></td>
									<td valign="middle"><font class="bg" size="1"><%= LanguageUtil.get(pageContext, "archived") %></font></td>
									<td valign="middle"><span class="lockIcon"></span></td>
									<td valign="middle"><font class="bg" size="1"><%= LanguageUtil.get(pageContext, "locked") %></font></td>
								</tr>
							</table>
						</td>
					</tr>
				</table>
				</form>
			</td>
		</tr>
	</table>
</liferay:box>

