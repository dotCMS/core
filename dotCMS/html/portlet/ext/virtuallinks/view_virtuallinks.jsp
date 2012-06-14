<%@ include file="/html/portlet/ext/virtuallinks/init.jsp" %>


<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%List hosts = (List)request.getAttribute("host_list");%>

<%
 String hostId ="";
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	String orderby = request.getParameter("orderby");
	
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : "";
	
	
	if ((orderby==null) || (orderby.length()==0)) orderby = "sort_order";
	
	String parentName = request.getParameter("parentName");
	String parent = request.getParameter("parent");
	String referer = java.net.URLEncoder.encode(CTX_PATH + "/ext/virtuallinks/view_virtuallinks?orderby=" + orderby + "&parent=" + parent + "&parentName=" + parentName,"UTF-8");
	
%>
<script language="Javascript">

function submitfm() {
		form = document.getElementById('fm');
		form.pageNumber.value = 1;
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" 
		value="/ext/virtuallinks/view_virtuallinks" />
		<portlet:param name="orderby" 
		value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "title")%>'/></portlet:renderURL>';
		submitForm(form);
	}
	
function resetSearch() {

window.location="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
							<portlet:param name='struts_action' value='/ext/virtuallinks/view_virtuallinks' />

							<portlet:param name='orderby' value='<%= (orderby.equals("sort_order")? "title" :  "title") %>' />
							</portlet:renderURL>";


	}

</script>

<form method="post" id="fm" action="">
<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">

	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Virtual-Links")) %>'/>

		<div class="portlet-toolbar">
			<div style="float:left">
				<%= LanguageUtil.get(pageContext, "Search") %>: 
				
				
				
			
		  <input id="query" type="text" name="query"  value="<%=UtilMethods.webifyString(query) %>"
		    dojoType="dijit.form.TextBox"
		    trim="true"
			 />
								


          <input type="hidden" name="host_name" id="host_name" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>"> 









                <button dojoType="dijit.form.Button" iconClass="searchIcon" onClick="submitfm()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %></button>
                <button dojoType="dijit.form.Button" iconClass="resetIcon" onClick="resetSearch()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %></button>
           </div>

			<div style="float:right">
				<button dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/virtuallinks/edit_virtuallink" /></portlet:actionURL>';"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Virtual-Link") ) %></button>
			</div>	
			<div class="clear"></div>
		</div>
				<table class="listingTable">

					<tr>
						<th width="50%">
							<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
							<portlet:param name='struts_action' value='/ext/virtuallinks/view_virtuallinks' />
							<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
							<portlet:param name='orderby' value='<%= (orderby.equals("sort_order")? "title" :  "title") %>' />
							</portlet:renderURL>"><%= LanguageUtil.get(pageContext, "Title") %></a>
						</th>
						
						<th align="center" nowrap width="30%">
							<a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
							<portlet:param name='struts_action' value='/ext/virtuallinks/view_virtuallinks' />
							<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
							<portlet:param name='orderby' value='<%= (orderby.equals("sort_order")? "url" :  "url") %>' />
							</portlet:renderURL>"><%= LanguageUtil.get(pageContext, "URL") %></a>
						</th>
						<th align="center" nowrap width="20%"><%= LanguageUtil.get(pageContext, "Redirect-To") %>:</th>
					</tr>
				
					<% java.util.List lists = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.VIRTUAL_LINK_LIST_VIEW);%>
					<%if(lists ==null) lists = new ArrayList(); %>
					<% for (int k=minIndex;k<maxIndex && k<lists.size();k++) { %>
						<%com.dotmarketing.portlets.virtuallinks.model.VirtualLink vl = (com.dotmarketing.portlets.virtuallinks.model.VirtualLink) lists.get(k);%>
						<%  String str_style =  ((k%2)==0)  ? "class=\"alternate_1\"" : "class=\"alternate_2\""; %>
						<tr <%=str_style%> onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/virtuallinks/edit_virtuallink" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /><portlet:param name="inode" value="<%= vl.getInode() %>" /></portlet:actionURL>'">
							<td>
								<span class="vlinksIcon"></span>&nbsp;
								<%=vl.getTitle()%> 
							</td>
							<td><%=vl.getUrl()%></td>
							<td><a href="<%=com.dotmarketing.util.UtilMethods.encodeURIComponent(vl.getUri())%>"><%=vl.getUri()%></a></td>
						</tr>
					<%}%>
					<% if (lists.size() ==0) { %>
						<tr>
							<td colspan="3" align="center">
							<%= LanguageUtil.get(pageContext, "There-are-no-Virtual-Links-to-show") %>
							</td>
						</tr>
					<% } %>
				</table>

	<%if(minIndex != 0 || maxIndex < lists.size()){ %>
				<div style="float: left;">
				<% if (minIndex != 0) { %>
					
						<button dojoType="dijit.form.Button" iconClass="previousIcon"
						onclick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/virtuallinks/view_virtuallinks" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
				<portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "title")%>' />
				</portlet:renderURL>'"><%=LanguageUtil.get(pageContext, "Previous") %></button>
				<% } %>
				</div>
				
				<div style="float: right;">
				<% if (maxIndex < lists.size()) { %>
									
		
				<button dojoType="dijit.form.Button" iconClass="nextIcon" 
				onclick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
									<portlet:param name="struts_action" value="/ext/virtuallinks/view_virtuallinks" />
									<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" />
									<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
									<portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "title")%>' />
									</portlet:renderURL>'"
				><%=LanguageUtil.get(pageContext, "Next") %></button>
			

				<% } %>
				</div>
				<div style="clear: both;"></div>
	<%} %>

</liferay:box>

</form>