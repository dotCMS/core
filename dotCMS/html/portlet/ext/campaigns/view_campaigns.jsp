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
	String direction = (request.getParameter("direction")!=null) ? request.getParameter("direction") : " desc";

%>
<script language="Javascript">
	function resetSearch() {
		form = document.getElementById('fm');
		form.query.value = '';
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" 
value="/ext/campaigns/view_campaigns" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name="orderby" 
value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "start_date")%>'/></portlet:renderURL>';
		submitForm(form);
	}
	function submitfm() {
		form = document.getElementById('fm');
		form.pageNumber.value = 1;
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" 
value="/ext/campaigns/view_campaigns" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name="orderby" 
value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "start_date")%>'/></portlet:renderURL>';
		submitForm(form);
	}
	function toggleRow(ID, length)	{
		if(document.layers)	{  //NN4+
			for(i = 0; i < length; i++) {
				var obj = document.layers[ID+i];
				if (obj.visibility == '' || obj.visibility == "hidden") {
					obj.visibility = "table-row";
					document.getElementById("link_recurrentLayer"+ID).className="chevronIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "bold";
				}
				else {
					obj.visibility = "hide";
					document.getElementById("link_recurrentLayer"+ID).className="chevronExpandIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "normal";
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

					document.getElementById("link_recurrentLayer"+ID).className="chevronIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "bold";
				}
				else {
					obj.style.display = "none";
					document.getElementById("link_recurrentLayer"+ID).className="chevronExpandIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "normal";
				}
			}
		}
		else if(document.all) {	// IE 4
			for(i = 0; i < length; i++) {
				var obj = document.all[ID+i];
				if (obj.style.display == '' || obj.style.display == "none") {
					obj.style.display = "inline";
					document.getElementById("link_recurrentLayer"+ID).className="chevronIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "bold";
				}
				else {
					obj.style.display = "none";
					document.getElementById("link_recurrentLayer"+ID).className = "chevronExpandIcon";
					document.getElementById("campaign_"+ID).style.fontWeight = "normal";
				}
			}
		}
	}
   var cbCampaignIdList = new Array();
   var i = 0;
   function checkUncheckAll() {
   		var checkAll = dijit.byId("checkAll").attr('checked');
   		
   		for (var i = 0; i < cbCampaignIdList.length; ++i) {
   			dijit.byId(cbCampaignIdList[i]).attr('checked', checkAll);
   		}
   }
   
   function getCheckboxSelectedCampaignsValues() {
   		var result = "";
   		
   		if ((cbCampaignIdList != null) || (0 < cbCampaignIdList.length)) {
   			for (var i = 0; i < cbCampaignIdList.length; ++i) {
   				if (dijit.byId(cbCampaignIdList[i]).attr('checked')) {
   					result = result + dijit.byId(cbCampaignIdList[i]).attr('value') + ",";
   				}
   			}
   		}
   		return result;
   }
   
	function deleteSelectedCampaigns() {
		if(confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-would-like-to-delete-these-campaigns")%>")){
		    var campaigns = getCheckboxSelectedCampaignsValues();
		    
		    var condition = "&campaigns=" + campaigns;
	
			form = document.getElementById('fm');
			form.pageNumber.value = 1;
			form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="cmd" 
			value="<%=com.liferay.portal.util.Constants.DELETE%>" /><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /><portlet:param name="direction" 
			value="<%=direction%>" /><portlet:param name="orderby" value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "start_date")%>'/></portlet:renderURL>'+ condition;
			submitForm(form);
		}
   }
   
   function newCampaign() {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>';		
	}
</script>

<form method="Post" id="fm" action="">
	
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%=LanguageUtil.get(pageContext, "Viewing-Email-Campaigns")%>' />

<input type="hidden" name="pageNumber" value="<%=pageNumber%>">

<div class="yui-g portlet-toolbar">
	<div class="yui-u first">
		<input type="text" dojoType="dijit.form.TextBox" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>" style="vertical-align:middle;" />
		<button dojoType="dijit.form.Button" onClick="submitfm();" iconClass="searchIcon" style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "Search")%></button>
		<button dojoType="dijit.form.Button" onClick="resetSearch();" iconClass="resetIcon" style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "Reset")%></button>
	</div>
	
<%
	boolean isCMSAdmin = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
	boolean isCampaignManagerAdmin = false;
	boolean isCampaignManagerEditor = false;
	
	List<com.dotmarketing.business.Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
	Iterator<com.dotmarketing.business.Role> rolesIt = roles.iterator();
	while (rolesIt.hasNext()) {
		com.dotmarketing.business.Role role = (com.dotmarketing.business.Role) rolesIt.next();
		if (role.getName().equals(com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_ADMIN"))) {
			isCampaignManagerAdmin = true;
			
			if (isCampaignManagerEditor)
				break;
		} else if (role.getName().equals(com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_EDITOR"))) {
			isCampaignManagerEditor = true;
			
			if (isCampaignManagerAdmin)
				break;
		}
	}
	
	if (isCMSAdmin || isCampaignManagerAdmin || isCampaignManagerEditor) {
%>
	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="newCampaign();return false;" iconClass="plusIcon"><%=LanguageUtil.get(pageContext, "Create-New-Campaign")%></button>
	</div>
<%
	}
%>
</div>

<table border="0" cellpadding="0" cellspacing="0" width="100%" class="listingTable" >	
	<tr>
		<th width="30" style="text-align:center;"><input type="checkbox" dojoType="dijit.form.CheckBox" name="checkAll" id="checkAll" onclick="checkUncheckAll()"></th>
		<th>
			<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
			<portlet:param name='struts_action' value='/ext/campaigns/view_campaigns' />
			<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
			<portlet:param name='orderby' value='title' />
			<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
			</portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Title")%></a>
		</th>
		<th width="150">
			<a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
			<portlet:param name='struts_action' value='/ext/campaigns/view_campaigns' />
			<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
			<portlet:param name='orderby' value='start_date' />
			<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
			</portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Start-Date")%></a>
		</th>
		<th width="150">
			<a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
			<portlet:param name='struts_action' value='/ext/campaigns/view_campaigns' />
			<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
			<portlet:param name='orderby' value='locked,completed_date' />
			<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
			</portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Status")%></a>
		</th>
	</tr>
<%
	Object[][] matches = (Object[][])request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_LIST);
	java.util.HashMap occurrencesRecurrentCampaign = (java.util.HashMap) request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_RECURRENT_OCURRENCES);

	if (matches == null) matches = new String[0][0];

	int	totalCampaigns = matches.length;
	for (int m = 0; m < matches.length; m++) {
		if ((matches[m] != null) && (matches[m][5] != null)) {
			boolean hasReadPermission = "true".equalsIgnoreCase(matches[m][5].toString())?true:false;
			if (!hasReadPermission)
				totalCampaigns--;
		}
	}

	for (int m = minIndex;m < maxIndex && m < matches.length; m++) {
	  String str_style1 = "";
		if ((m % 2) == 0) {
			str_style1 = "class='alternate_1'";
		}
		else{
			str_style1 = "class='alternate_2'";
		}
	
		if ((matches[m] != null) && (matches[m][3] != null) && (matches[m][5] != null)) {
			Object[] match = matches[m];
			boolean hasReadPermission = "true".equalsIgnoreCase(matches[m][5].toString())?true:false;
			 
			if (hasReadPermission) {
				boolean isRecurrent = "true".equalsIgnoreCase(matches[m][3].toString())?true:false;
				String[][] occurrencesCampaigns = (String[][]) occurrencesRecurrentCampaign.get(match[0].toString());
				boolean isAlert = "true".equalsIgnoreCase(matches[m][8].toString())?true:false;
				boolean isFinished = matches[m][4].toString().startsWith("done")||matches[m][4].toString().startsWith("expire")?true:false;
	 			boolean hasWritePermission = "true".equalsIgnoreCase(matches[m][6].toString())?true:false;%>
		<tr <%=str_style1%>>
			<td style="text-align:center;">
				<%	if (hasWritePermission) {%>
					<input type="checkbox" dojoType="dijit.form.CheckBox" name="<%= match[0].toString() %>" id="<%= match[0].toString() %>" value="<%= match[0].toString() %>">
					<script>
						cbCampaignIdList[i] = "<%= match[0].toString() %>";
						i++;
					</script>
				<% } %>
			</td>
			<td>
				<% 	if (isRecurrent) {%>
					<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<%if(isFinished) { %>
					<portlet:param name="struts_action" value="/ext/campaigns/view_report" />
					<%} else { %>
					<portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" />
					<%} %><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=match[0].toString()%>" /></portlet:actionURL>">
						<span class="resetIcon"></span>
					</a>
				<% } %>
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
					<%if(isFinished) { %>
					<portlet:param name="struts_action" value="/ext/campaigns/view_report" />
					<%} else { %>
					<portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" />
					<%} %>
					<portlet:param name="cmd" value="edit" />
					<portlet:param name="inode" value="<%=match[0].toString()%>" />
					</portlet:actionURL>">
						<span class="targetIcon"></span>
				</a>
				<%if (isRecurrent 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0]) 
					&& com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns[0][4])) {%>
						<a href="javascript:toggleRow('<%= match[0].toString() %>', <%= occurrencesCampaigns.length %>);"><span class="chevronExpandIcon" id="link_recurrentLayer<%=match[0].toString()%>"></span></a>
				<% } %>
			
				<a id="campaign_<%= match[0].toString() %>" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<%if(isFinished) { %>
					<portlet:param name="struts_action" value="/ext/campaigns/view_report" />
					<%} else { %>
					<portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" />
					<%} %>
				<portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=match[0].toString()%>" /></portlet:actionURL>">
				<%=match[1].toString()%></a>
			</td>
			<td><%=match[2].toString()%></td>
			<td><%=match[4].toString()%></td>
		</tr>
		<%if (isRecurrent) { // displaying the subsequent occurrences %>
			<%if (com.dotmarketing.util.UtilMethods.isSet(occurrencesCampaigns)) {
				for (int indexOccurrence = occurrencesCampaigns.length; indexOccurrence > 0; indexOccurrence--) {
					String[] occurrence = occurrencesCampaigns[occurrencesCampaigns.length - indexOccurrence];%>

					<tr <%=str_style1%> style='display: none;' id="<%= match[0].toString() %><%= occurrencesCampaigns.length - indexOccurrence %>">
						<td width="30"></td>
						<td>
							<%	if (hasWritePermission) {%>
								&nbsp;&nbsp;<input type="checkbox" dojoType="dijit.form.CheckBox" name="<%= occurrence[0].toString() %>" id="<%= occurrence[0].toString() %>" value="<%= occurrence[0].toString() %>">
								<script>
									cbCampaignIdList[i] = "<%= occurrence[0].toString() %>";
									i++;
								</script>
							<%}%>
							<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=occurrence[0].toString()%>" /></portlet:actionURL>">
							<%=occurrence[1].toString()%> (dispatch <%= indexOccurrence %> of <%= occurrencesCampaigns.length %>)</a>
						</td>
						<td align="center" width="150">
							<%=occurrence[2].toString()%>
						</td>
						<td align="left" width="150">
							<%=occurrence[4].toString()%>
						</td>
					</tr>
				<%}%>
			<%}%>
		<%}%>
	<%}%>
<%}%>
<%}%>

<!-- END Result List -->

<!-- START No Results -->
<%if(totalCampaigns == 0){%>
	<tr>
		<td colspan="5">
			<div class="noResultsMessage"><%=LanguageUtil.get(pageContext, "There-are-no-email-campaigns-to-view")%></div>
		</td>
	</tr>
<%}%>
<!-- END No Results -->

</table>

<%if ((totalCampaigns > 0) && (isCMSAdmin || isCampaignManagerAdmin || isCampaignManagerEditor)) {%>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button"  onClick="deleteSelectedCampaigns()" iconClass="deleteIcon">
			<%=LanguageUtil.get(pageContext, "Delete-Selected")%>
		</button>
	</div>
<%}%>


<!-- START Pagination -->
<div class="yui-gb" class="buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
			<span class="previousIcon"></span>
			<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
				<portlet:param name="direction" value="<%=direction%>" />
				<portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "start_date")%>' />
				</portlet:renderURL>">
				<%= LanguageUtil.get(pageContext, "Previous") %>
			</a>
		<% } %>
	</div>

	<div class="yui-u" style="text-align:right;">
		<% if (maxIndex < totalCampaigns) { %>
			<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query :  "") %>' />
				<portlet:param name="direction" value="<%=direction%>" />
				<portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "start_date")%>' />
				</portlet:renderURL>">
				<%= LanguageUtil.get(pageContext, "Next") %>
			</a>
			<span class="nextIcon"></span>
		<% } %>
	</div>
</div>
<!-- END Pagination -->

</liferay:box>
</form>
