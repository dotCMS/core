<%@ include file="/html/portlet/ext/scheduler/init.jsp" %>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>

<%
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );

	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>

<script language="javascript">

	function displayProperties(id) {
		if (id == "user_jobs") {
		
			//changing tabs
			document.getElementById('user').className="alpha";
			document.getElementById('recurrent').className="beta";
			
			//display basic properties
			document.getElementById("user_jobs").style.display = "";
			document.getElementById("recurrent_campaign").style.display = "none";
			document.getElementById("user_jobs_options").style.display = "";
			document.getElementById("recurrent_campaign_options").style.display = "none";
		} else if (id == "recurrent_campaign") {
			
			//changing tabs
			document.getElementById('user').className="beta";
			document.getElementById('recurrent').className="alpha";
			
			//display basic properties
			document.getElementById("user_jobs").style.display = "none";
			document.getElementById("recurrent_campaign").style.display = "";
			document.getElementById("user_jobs_options").style.display = "none";
			document.getElementById("recurrent_campaign_options").style.display = "";
		}
	}

</script>
<br />
<table border="0" width="100%" cellpadding="0" cellspacing="0">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0" class="portletMenu" width="100%">
				<tr>
				 <td width="100%">
					<a class="alpha" id="user" href="javascript:displayProperties('user_jobs')"><%= LanguageUtil.get(pageContext, "User-Jobs") %></a>
					<a class="beta" id="recurrent" href="javascript:displayProperties('recurrent_campaign')"><%= LanguageUtil.get(pageContext, "Recurrent-Campaign-job") %> </a>
				 </td>	
				</tr>
			</table>
		</td>
	</tr>
	
  <tr class="blue_Border">
	  <td><img src="/html/skin/image/common/spacer.gif" border="0" height="5" hspace="0" vspace="0" width="1"></td>
  </tr>
  
  <tr>
		 <td><img src="/html/skin/image/common/spacer.gif" border="0" height="4" hspace="0" vspace="0" width="1"></td>
   </tr>

</table>

<table border="0" cellpadding="0" cellspacing="0" width="100%" height="90" id="user_jobs" class="listingTable">
	
	<tr class="header"  height="18">
		<td align="center" width="50"><%= LanguageUtil.get(pageContext, "Action") %></td>
		<td><%= LanguageUtil.get(pageContext, "Name") %></td>
		<td><%= LanguageUtil.get(pageContext, "Description") %></td>
	</tr>
	
	<% java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.SCHEDULER_VIEW_PORTLET);
		boolean itemShowed = false;
		int k = 0;
		int numsItemShowed = 0;
		
		String str_style=""; 
		for (; (numsItemShowed < Config.getIntProperty("MAX_ITEMS_MINIMIZED_VIEW")) && (k < list.size()); ++k) {
		
			ScheduledTask scheduler = (ScheduledTask) list.get(k);
		 	if (scheduler.getJobGroup().equals("User Job")) {
		 		itemShowed = true;
		 		
				
				if(numsItemShowed%2==0){
				  str_style="class=\"alternate_1\"";
				}
				else{
				  str_style="class=\"alternate_2\"";
				}
				
				numsItemShowed++;
				

	%>
		<tr <%=str_style %> >
			<td align="center" width="50" class="icons">
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" />
				<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
				<portlet:param name="group" value="<%= scheduler.getJobGroup() %>" />
				<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
				</portlet:actionURL>">
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
<%		 	}
		}
%>
	<tr><td colspan=3>&nbsp;</td></tr>
	<% if (!itemShowed) { %>
		<tr>
			<td colspan="3" align=center>
				<%= LanguageUtil.get(pageContext, "There-are-no-Schedulers-to-show") %>
			</td>
		</tr>
	<% } %>
</table>


<!-- Recurrent Campaings -->
<table border="0" cellpadding="0" cellspacing="0" width="100%" height="90" id="recurrent_campaign" style="display:none" class="listingTable">
	
	<tr class="header" height="18">
		<td><%= LanguageUtil.get(pageContext, "Name") %></td>
		<td><%= LanguageUtil.get(pageContext, "Description") %></td>
	</tr>
	
	<%
		itemShowed = false;
		k = 0;
		numsItemShowed = 0;
		String str_style2="";
		for (; (numsItemShowed < 5) && (k < list.size()); k++) {
			ScheduledTask scheduler = (ScheduledTask) list.get(k);
		 	if (scheduler.getJobGroup().equals("Recurrent Campaign")) {
		 		itemShowed = true;
				
				if(numsItemShowed%2==0){
				  str_style2="class=\"alternate_1\"";
				}
				else{
				  str_style2="class=\"alternate_2\"";
				}
				
				numsItemShowed++;

				
	%>
		
		<tr <%=str_style2 %>>
			<td>
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
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
<%		 	}
		}
%>
	<tr><td colspan=2>&nbsp;</td></tr>
	<% if (!itemShowed) { %>
		<tr>
			<td colspan="2" align=center>
				<%= LanguageUtil.get(pageContext, "There-are-no-Schedulers-to-show") %>
			</td>
		</tr>
	<% } %>
</table>


<table align="right" id="user_jobs_options">
	<tr>
		<td nowrap>
   				<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" /><portlet:param name="group" value="user_jobs" /></portlet:renderURL>">
				   <%= LanguageUtil.get(pageContext, "all") %></a>
			| 
   				<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>">
           <%= LanguageUtil.get(pageContext, "new") %></a>&nbsp;&nbsp;
		</td>
	</tr>
</table>

<table align=right id="recurrent_campaign_options" style="display:none">
	<tr>
		<td nowrap>
   				<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" /><portlet:param name="group" value="recurrent_campaign" /></portlet:renderURL>">
				   <%= LanguageUtil.get(pageContext, "all") %></a>&nbsp;&nbsp;
		</td>
	</tr>
</table>