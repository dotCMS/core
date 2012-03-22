<%@ include file="/html/portlet/ext/scheduler/init.jsp" %>

<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>

<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>


<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">
	<div id="user_jobs" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "User-Jobs") %>" <%= ((request.getParameter("group") != null) && request.getParameter("group").equals("user_jobs")) ? "selected=\"true\"" : "" %> >		
		<div class="yui-g portlet-toolbar">
			<div class="yui-u" style="text-align:right;">	
				<button onClick="location.href='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>'" iconClass="plusIcon" dojoType="dijit.form.Button">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "new")) %>
				</button>
			</div>
		</div>
		
		<table class="listingTable">
			
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Action") %></th>
				<th><%= LanguageUtil.get(pageContext, "Name") %></th>
				<th><%= LanguageUtil.get(pageContext, "Description") %></th>
			</tr>

			<% java.util.List lists = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.SCHEDULER_LIST_VIEW);
			
				boolean itemShowed = false;
				String str_style = "";
				for (int k = minIndex; (k < maxIndex) && (k < lists.size()); k++) {
					
			     	if(k%2==0){
					  str_style="class=\"alternate_1\"";
					}
					else{
					  str_style="class=\"alternate_2\"";
					}
					
					ScheduledTask scheduler = (ScheduledTask) lists.get(k);
					if (scheduler.getJobGroup().equals("User Job")) {
					itemShowed = true;
			%>
				<tr <%= str_style %>>
					<td>
					
						<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" />
						<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
						<portlet:param name="group" value="<%= scheduler.getJobGroup() %>" />
						<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>">
							<span class="editIcon"></span>
						</a>
						
						<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" />
						<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
						<portlet:param name="group" value="<%= scheduler.getJobGroup() %>" />
						<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.DELETE %>" />
						<portlet:param name="referrer" value="<%= referrer %>" /></portlet:actionURL>">
							<span class="deleteIcon"></span>
						</a>
					</td>
					
					<td>
						<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" />
						<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
						<portlet:param name="group" value="<%= scheduler.getJobGroup() %>" />
						<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
						</portlet:actionURL>"><%= scheduler.getJobName() %>
						</a>
					</td>
					
					<td>
							<%= scheduler.getJobDescription() %>
					</td>
				</tr>
				<% }
			}%>
			<% if (!itemShowed) { %>
				<tr>
					<td colspan="3" align=center>
						<%= LanguageUtil.get(pageContext, "There-are-no-Schedulers-to-show") %>
					</td>
				</tr>
			<% } %>
		</table>
		<% if ((minIndex != 0) || (maxIndex < lists.size())) { %>
			<div style="float: left;" align="left">
				<% if (minIndex != 0) { %>
				<button dojoType="dijit.form.Button" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" />
				</portlet:renderURL>'" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %></button>
				<% } %>
			</div>
			
			<div style="float: right;"  align=right>
				<% if (maxIndex < lists.size()) { %>
	  			<button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" />
				</portlet:renderURL>'" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "Next") %></button>
				<% } %>
			</div>
			<div style="clear: both;"></div>
		<% } %>
	</div>
	<div id="recurrent_campaign" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Recurrent-Campaign-job") %>" <%= ((request.getParameter("group") != null) && request.getParameter("group").equals("user_jobs")) ? "selected=\"true\"" : "" %>>
		<!-- Recurrent Campaing -->
		<table class="listingTable">
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Name") %></th>
				<th><%= LanguageUtil.get(pageContext, "Description") %></th>
			</tr>

			<% 
				itemShowed = false;
				str_style = "";
				int r=0;
				for (int k = minIndex; (k < maxIndex) && (k < lists.size()); k++) {
					ScheduledTask scheduler = (ScheduledTask) lists.get(k);
					if (scheduler.getJobGroup().equals("Recurrent Campaign")) {
						
						if(r%2==0){
						  str_style="class=\"alternate_1\"";
                              }
						else{
						  str_style="class=\"alternate_2\"";
						}
						r++;
						
						itemShowed = true;
			%>
				<tr <%= str_style %>>
					<td>

						<a  href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" />
						<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
						<portlet:param name="group" value="<%= scheduler.getJobGroup() %>" />
						<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
						</portlet:actionURL>"><%= scheduler.getJobDescription() %>
						</a>
					</td>
					<td>
						<%= scheduler.getJobName() %>
					</td>
				</tr>
				<% }
			}%>
			<% if (!itemShowed) { %>
				<tr>
					<td colspan="2" align=center>
						<%= LanguageUtil.get(pageContext, "There-are-no-Schedulers-to-show") %>
					</td>
				</tr>
			<% } %>
		</table>
		<% if ((minIndex != 0) || (maxIndex < lists.size())) { %>
			<div style="float: left;" align="left">
				<% if (minIndex != 0) { %>
				<button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" />
				</portlet:renderURL>'" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %>
				</button>
				<% } %>
			</div>
			
			<div style="float: right;" align="right">
				<% if (maxIndex < lists.size()) { %>
	  				<button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" />
				</portlet:renderURL>'" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "Next") %></button>
				<% } %>
			</div>
			<div style="clear: both;"></div>
		<% } %>
	</div>
</div>