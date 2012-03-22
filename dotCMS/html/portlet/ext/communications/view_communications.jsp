<%@ include file="/html/portlet/ext/communications/init.jsp" %>
<%
	String[] ids = java.util.TimeZone.getAvailableIDs(-5 * 60 * 60 * 1000);
	java.util.SimpleTimeZone et = new java.util.SimpleTimeZone(-5 * 60 * 60 * 1000, ids[0]);
	//set up rules for daylight savings time
	et.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 1 * 60 * 60 * 1000);
	et.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * 60 * 60 * 1000);

	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(et);
	
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	String query1 = (request.getParameter("query1")!=null) ? request.getParameter("query1") : "";
	String query2 = (request.getParameter("query2")!=null) ? request.getParameter("query2") : "";
	String query3 = (request.getParameter("query3")!=null) ? request.getParameter("query3") : "";
	
	String direction = (request.getParameter("direction")!=null) ? request.getParameter("direction") : " desc";
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",
			new String[] { "/ext/communications/view_communications" });
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(
			request, WindowState.MAXIMIZED.toString(), params);
%>
<script language="Javascript">
function resetSearch() {
	form = document.getElementById('fm');
	form.query1.value = '';
	form.query2.value = '';
	form.query3.value = '';
	form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" 
value="/ext/communications/view_communications" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name="orderby" 
value="<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter(\"orderby\")) ? request.getParameter(\"orderby\") : 
\"mod_date\")%>"/></portlet:renderURL>';
	submitForm(form);
}
function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" 
value="/ext/communications/view_communications" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name="orderby" 
value="<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter(\"orderby\")) ? request.getParameter(\"orderby\") : 
\"mod_date\")%>"/></portlet:renderURL>';
	submitForm(form);
}
function deleteCommunication(inode) {
		var form = document.getElementById('fm');
		if(confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-communication-(this-cannot-be-undone)")%>")){
				form.action ='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/edit_communication" /><portlet:param name="cmd" value='<%=Constants.DELETE%>' /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>&inode='+inode;
				submitForm(form);
		}
	}

   var cbCommInodeList = new Array();
   var i = 0;
   function getCheckboxSelectedCommsValues() {
   		var result = "";
   		
   		if ((cbCommInodeList != null) || (0 < cbCommInodeList.length)) {
   			for (var i = 0; i < cbCommInodeList.length; ++i) {
   				if (dijit.byId(cbCommInodeList[i]).attr('checked')) {
   					result = result + dijit.byId(cbCommInodeList[i]).attr('value') + ",";
   				}
   			}
   		}
   		
   		return result;
   }
   function deleteComms() {
   		var comms = getCheckboxSelectedCommsValues();
	   	if (comms != "") {
   			if (confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-all-the-selected-communications")%>")) {
	   			var condition = "&comms=" + comms + "&referer=<%= referer %>" + escape(condition);
				var form = document.getElementById('fm');
				form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/communications/edit_communication" /><portlet:param name="cmd" value="deleteComms" /></portlet:actionURL>' + condition
				form.submit ();
			}
		}
		else {
			alert("<%=LanguageUtil.get(pageContext, "You-have-to-select-at-least-one-communication")%>");
		}
   }
   
   function checkUncheckAll() {
   		var checkAll = dijit.byId("checkAll").attr('checked');
   		
   		for (var i = 0; i < cbCommInodeList.length; ++i) {
   			dijit.byId(cbCommInodeList[i]).attr('checked', checkAll);
   		}
   }
</script>
<form method="Post" id="fm" action="">
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%=LanguageUtil.get(pageContext, "Viewing-Communications")%>' />
	

<input type="hidden" name="pageNumber" value="<%=pageNumber%>">

<div class="yui-ge portlet-toolbar">
	<div class="yui-u first">
		<div style="vertical-align:middle;margin-right:15px;float:left;">
			<select name="query3" dojoType="dijit.form.ComboBox">
				<option value="" <%=(!com.dotmarketing.util.UtilMethods.isSet(request.getParameter("query3"))? " selected":"") %>><%=LanguageUtil.get(pageContext, "all")%></option>
				<option value="email" <%=((com.dotmarketing.util.UtilMethods.isSet(request.getParameter("query3")) && request.getParameter("query3").equals("email"))? " selected":"") %>><%=LanguageUtil.get(pageContext, "Email")%></option>
				<option value="external" <%=((com.dotmarketing.util.UtilMethods.isSet(request.getParameter("query3")) && request.getParameter("query3").equals("external"))? " selected":"") %>><%=LanguageUtil.get(pageContext, "External")%></option>
				<option value="alert" <%=((com.dotmarketing.util.UtilMethods.isSet(request.getParameter("query3")) && request.getParameter("query3").equals("alert"))? " selected":"") %>><%=LanguageUtil.get(pageContext, "Alert")%></option>
			</select>
		</div>
		
		<div style="vertical-align:middle;float:left;margin-right:15px;white-space:nowrap;">
			<span style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "Title")%>:</span>
			<input type="text" dojoType="dijit.form.TextBox" name="query1" value="<%= com.dotmarketing.util.UtilMethods.isSet(query1) ? query1 : "" %>">
		</div>
		
		<div style="float:left;margin-right:15px;white-space:nowrap;">
			<span style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "From")%>:</span>
			<input type="text" dojoType="dijit.form.TextBox" name="query2" value="<%= com.dotmarketing.util.UtilMethods.isSet(query2) ? query2 : "" %>">
		</div>
		
		<button dojoType="dijit.form.Button" onClick="submitfm()" iconClass="searchIcon">
			<%=LanguageUtil.get(pageContext, "Search")%>
		</button>

	</div>
	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/edit_communication" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>';return false;" iconClass="plusIcon">
			<%= LanguageUtil.get(pageContext, "Create-New-Communication") %>
		</button>
	</div>
</div>

			

<table class="listingTable">
	<tr>
		<th width="25" style="text-align:center;"><input type="checkbox" dojoType="dijit.form.CheckBox" name="checkAll" id="checkAll" onclick="checkUncheckAll()"></th>
		<th width="50" nowrap><%=LanguageUtil.get(pageContext, "Action")%></th>
		<th width="75" style="text-align:center;">
			<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/communications/view_communications' /><portlet:param name="query1" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query1)? query1 :  \"\") %>" /><portlet:param name="query2" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query2)? query2 :  \"\") %>" /><portlet:param name="query3" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query3)? query3 :  \"\") %>" /><portlet:param name='orderby' value='communication_type' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>">
				<%=LanguageUtil.get(pageContext, "Type")%>
			</a>
		</th>
		<th width="80%">
			<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/communications/view_communications' /><portlet:param name="query1" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query1)? query1 :  \"\") %>" /><portlet:param name="query2" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query2)? query2 :  \"\") %>" /><portlet:param name="query3" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query3)? query3 :  \"\") %>" /><portlet:param name='orderby' value='title' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>">
				<%=LanguageUtil.get(pageContext, "Title")%>
			</a>
		</th>
		<th nowrap style="text-align:center;">
			<a  href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name='struts_action' value='/ext/communications/view_communications' /><portlet:param name="query1" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query1)? query1 :  \"\") %>" /><portlet:param name="query2" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query2)? query2 :  \"\") %>" /><portlet:param name="query3" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query3)? query3 :  \"\") %>" /><portlet:param name='orderby' value='mod_date' /><portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' /></portlet:renderURL>">
				<%=LanguageUtil.get(pageContext, "Modified-Date")%>
			</a>
		</th>
		
	</tr>
	
	<% java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATIONS_LIST);%>
	<% for (int k=minIndex;k<maxIndex && k<list.size();k++) { %>
		<%String str_style= (k%2==0) ? "class='alternate_1'" : "class='alternate_2'";%>
		<%com.dotmarketing.portlets.communications.model.Communication c = (com.dotmarketing.portlets.communications.model.Communication) list.get(k);%>
			
			<tr <%=str_style %>>
			<td align="center">
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="<%= String.valueOf(c.getInode()) %>" id="<%= String.valueOf(c.getInode()) %>" value="<%= String.valueOf(c.getInode()) %>">
				<script>
					cbCommInodeList[i] = "<%= String.valueOf(c.getInode()) %>";
					i++;
				</script>
			</td>
			<td nowrap>
			    <a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/edit_communication" /><portlet:param name="cmd" value="edit" /><portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" /></portlet:actionURL>">
					<span class="editIcon"></span>
				</a>
				<a class="bg" href="javascript:deleteCommunication('<%=String.valueOf(c.getInode())%>')">
					<span class="deleteIcon"></span>
				</a>
			</td>
			<td align="center" nowrap><%=c.getCommunicationType()%></td>
			<td>
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
					<portlet:param name="struts_action" value="/ext/communications/edit_communication" />
					<portlet:param name="cmd" value="edit" />
					<portlet:param name="inode" value="<%=String.valueOf(c.getInode())%>" />
					</portlet:actionURL>">
					<%=c.getTitle()%> 
				</a>
			</td>
			<td align="center" nowrap>
				<%=modDateFormat.format(c.getModDate())%>
			</td>
		</tr>
	<%}%>
	<%if(list.size() ==0){%>
		<tr>
			<td colspan="5">
				<div class="noResultsMessage"><%=LanguageUtil.get(pageContext, "There-are-no-communications-to-view")%></div>
			</td>
		</tr>
	<%}%>
</table>

<div class="yui-gb buttonRow">
	<div class="yui-u first">
		<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/view_communications" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="query1" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query1)? query1 :  \"\") %>" /><portlet:param name="query2" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query2)? query2 :  \"\") %>" /><portlet:param name="query3" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query3)? query3 :  \"\") %>" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "mod_date")%>' /></portlet:renderURL>'" iconClass="previousIcon">
				<%=LanguageUtil.get(pageContext, "Previous")%>
			</button>
		<% } %> &nbsp;
	</div>
	<div class="yui-u" style="text-align:center;">
		<button dojoType="dijit.form.Button" onClick="deleteComms()">
	        <%=LanguageUtil.get(pageContext, "Delete-Selected")%>
	    </button>
	</div>
	<div class="yui-u" style="text-align:right;">
		<% if (maxIndex < list.size()) { %>
			<button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/view_communications" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="query1" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query1)? query1 :  \"\") %>" /><portlet:param name="query2" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query2)? query2 :  \"\") %>" /><portlet:param name="query3" value="<%= (com.dotmarketing.util.UtilMethods.isSet(query3)? query3 :  \"\") %>" /><portlet:param name="direction" value="<%=direction%>" /><portlet:param name='orderby' value='<%=(com.dotmarketing.util.UtilMethods.isSet(request.getParameter("orderby")) ? request.getParameter("orderby") : "mod_date")%>' /></portlet:renderURL>'" iconClass="nextIcon">
				<%=LanguageUtil.get(pageContext, "Next")%>
			</button>
		<% } %>
	</div>
</div>


</liferay:box>
</form>
