<%@ include file="/html/portlet/ext/campaigns/init.jsp" %>
<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = 20;
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : "";
	
	boolean isCampaignManagerAdmin = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_ADMIN"));
	boolean isCampaignManagerEditor = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_EDITOR"));
	
	String direction = (request.getParameter("direction")!=null) ? request.getParameter("direction") : " desc";
%>
<%@page import="com.dotmarketing.util.Config"%>
<script language="Javascript">
	function toggleRow(ID, length)	{
		if(document.layers)	{  //NN4+
			for(i = 0; i < length; i++) {
				var obj = document.layers[ID+i];
				if (obj.visibility == '' || obj.visibility == "hidden") {
					obj.visibility = "table-row";
					document.getElementById("link_recurrentLayer"+ID).src ="/html/skin/image/common/trees/minus.gif";
					document.getElementById(ID).style.fontWeight = 'bold';
				}
				else {
					obj.visibility = "hide";
					document.getElementById("link_recurrentLayer"+ID).src="/html/skin/image/common/trees/plus.gif";
					document.getElementById(ID).style.fontWeight = 'normal';
				}
			}
		}
		else if(document.getElementById) {	  //gecko(NN6) + IE 5+
			for(i = 0; i < length; i++) {
				var obj = document.getElementById(ID+i);
				if (obj.style.display == '' || obj.style.display == "none") {
					try {
						obj.style.display = "table-row";
					}
					catch (e) {
						obj.style.display = "inline";
					}
					document.getElementById("link_recurrentLayer"+ID).src ="/html/skin/image/common/trees/minus.gif";
					document.getElementById(ID).style.fontWeight = 'bold';
				}
				else {
					obj.style.display = "none";
					document.getElementById("link_recurrentLayer"+ID).src="/html/skin/image/common/trees/plus.gif";
					document.getElementById(ID).style.fontWeight = 'normal';
				}
			}
		}
		else if(document.all) {	// IE 4
			for(i = 0; i < length; i++) {
				var obj = document.all[ID+i];
				if (obj.style.display == '' || obj.style.display == "none") {
					obj.style.display = "inline";
					document.getElementById("link_recurrentLayer"+ID).src ="/html/skin/image/common/trees/minus.gif";
					document.getElementById(ID).style.fontWeight = 'bold';
				}
				else {
					obj.style.display = "none";
					document.getElementById("link_recurrentLayer"+ID).src="/html/skin/image/common/trees/plus.gif";
					document.getElementById(ID).style.fontWeight = 'normal';
				}
			}
		}
	}
</script>
<form method="Post" id="fm" action="">
<table border="0" cellpadding="2" cellspacing="0" width="100%" height="75" class="listingTable">
<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
	<tr>
		<td colspan="6">&nbsp;</td>
	</tr>
	<tr class="header">
		<td colspan="3" width="2%"></td>

		<td>
		<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' 
value='/ext/campaigns/view_campaigns' /><portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' /><portlet:param 
name='orderby' value='title' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>"> <%=LanguageUtil.get(pageContext, "Title")%></a>
		</td>
		
		<td align="center">
		<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' 
value='/ext/campaigns/view_campaigns' /><portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' /><portlet:param 
name='orderby' value='start_date' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Start-Date")%></a>
		</td>
		
		<td align="center">
		<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' 
value='/ext/campaigns/view_campaigns' /><portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' /><portlet:param 
name='orderby' value='locked,completed_date' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Status")%></a>
		</td>
		
	</tr>
<%
	Object[][] matches = (Object[][])request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_LIST);
	java.util.HashMap occurrencesRecurrentCampaign = (java.util.HashMap) request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_RECURRENT_OCURRENCES);
	if (matches == null) matches = new String[0][0];

	int campaignShowed = 0;
	for (int m = 0; campaignShowed < Config.getIntProperty("MAX_ITEMS_MINIMIZED_VIEW") && m < matches.length; m++) {
		Object[] match = matches[m];
		boolean hasReadPermission = "true".equalsIgnoreCase(matches[m][5].toString())?true:false;
        
		String str_style =  ((m%2)==0)  ? "class=\"alternate_1\"" : "class=\"alternate_2\"";

		
		if (hasReadPermission) {
			boolean isRecurrent = "true".equalsIgnoreCase(matches[m][3].toString())?true:false;
			String[][] occurrencesCampaigns = (String[][]) occurrencesRecurrentCampaign.get(match[0].toString());
			campaignShowed++;
			boolean isAlert = "true".equalsIgnoreCase(matches[m][8].toString())?true:false;
%>
		<tr <%=str_style %>>
			<td width="30" align="right">
<% 
			if (isRecurrent) {
%>
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=match[0].toString()%>" /></portlet:actionURL>">
					<img border="0" src="/portal/images/icons/recursive.gif" width="16" height="16">
				</a>
<%
			}
%>
			</td>
			<td width="30">
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=match[0].toString()%>" /></portlet:actionURL>">
<% 
				if (isAlert) {
%>
					<img class="bg" border="0" src="/portal/images/icons/campaign_alert_icon.gif" width="24" height="18">
<%
				}
				else {
%>
					<img class="bg" border="0" src="/portal/images/icons/campaign_icon.gif" width="24" height="18">
<%
				}
%>
				</a>
			</td>
			<td width="20">
<%
			if (isRecurrent 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0]) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0][4])) {
%>
				<a class="bg" href="javascript:toggleRow('<%= match[0].toString() %>', <%= occurrencesCampaigns.length %>);"><img border="0" id="link_recurrentLayer<%=match[0].toString()%>" src="/html/skin/image/common/trees/plus.gif"/></a>
<%
			}
%>
			</td>
			<td width="50%">
				<a id="<%= match[0].toString() %>" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=match[0].toString()%>" /></portlet:actionURL>">
				<%=match[1].toString()%></a>
			</td>
			<td align="center">
			     <%=match[2].toString()%>
			</td>
			<td align="left" width="17%" nowrap="nowrap">
<%--
			if (isRecurrent 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0]) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0][4])) {
%>
				<%= occurrencesCampaigns[0][4].toString() %>
<%
			} else {
--%>
				<%=match[4].toString()%>
<%--
			}
--%>
			</td>
		</tr>
<%
			if (isRecurrent) { // displaying the subsequent occurrences
				if (com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns)) {
					for (int indexOccurrence = occurrencesCampaigns.length; indexOccurrence > 0; indexOccurrence--) {
						String[] occurrence = occurrencesCampaigns[occurrencesCampaigns.length - indexOccurrence];
						//if (str_style.equalsIgnoreCase("class='alternate_2'")) {
						//	str_style = "class='alternate_1'";
						//}
						//else{
						//	str_style = "class='alternate_2'";
						//}
%>
		<tr <%=str_style%> style='display: none;' id="<%= match[0].toString() %><%= occurrencesCampaigns.length - indexOccurrence %>">
			<td width="30"></td>
			<td width="30"></td>
			<td width="20"></td>
			<td width="45%">
				<font class="gamma" size="2">
					&nbsp;&nbsp;<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=occurrence[0].toString()%>" /></portlet:actionURL>">
					<%=occurrence[1].toString()%> (dispatch <%= indexOccurrence %> of <%= occurrencesCampaigns.length %>)</a>
				</font>
			</td>
			<td align="center" >
				<font class="gamma" size="2"><%=occurrence[2].toString()%></font>
			</td>
			<td align="left" width="17%">
				<font class="gamma" size="2"><%=occurrence[4].toString()%></font>
			</td>
		</tr>
<%
					}
				}
			}
		}
	}
	if(matches.length ==0) {
%>
	<tr>
		<td colspan="6" align="center">
			<%=LanguageUtil.get(pageContext, "There-are-no-email-campaigns-to-view")%>
		</td>
	</tr>
<%
	}
%>
	<tr><td colspan="7">&nbsp;</td></tr>
</table>

<table align="right">
	<tr>
		<td nowrap>
			<a class="portletOption" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:actionURL>"><%=LanguageUtil.get(pageContext, "all1")%></a>
<%
	if (isCampaignManagerAdmin || isCampaignManagerEditor) { 
%>
			|&nbsp;<a class="portletOption" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>"><%=LanguageUtil.get(pageContext, "new1")%></a>&nbsp;&nbsp;
<%
	}
%>
		</td>
	</tr>
</table>
</form>
