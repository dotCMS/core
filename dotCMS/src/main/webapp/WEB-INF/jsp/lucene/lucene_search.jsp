<%@page import="com.dotmarketing.util.PortletID"%>
<%@page import="com.liferay.util.Xss"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.portlets.contentlet.util.ContentletUtil" %>
<html xmlns="http://www.w3.org/1999/xhtml">

<%
User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
if(user == null){
	response.setStatus(403);
	return;
}
String query = request.getParameter("query");
if(!UtilMethods.isSet(query)){
	query = "";
}
String submitURL = com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"query-tool");
String translatedQuery = "";
String sortBy = request.getParameter("sort");
String offset = request.getParameter("offset");
String limit = request.getParameter("limit");
boolean reindex = request.getParameter("reindexResults")!= null;
String userToPullID = request.getParameter("userId");
List<ContentletSearch> iresults =  null;
List<Contentlet> cons = null;
int counter = 1;
boolean userIsAdmin = false;
User userForPull = user;
long startAPISearchPull = 0;
long afterAPISearchPull = 0;
long startAPIPull = 0;
long afterAPIPull = 0;


String nastyError = null;

if(!UtilMethods.isSet(sortBy)){
	sortBy = "";
}
if(!UtilMethods.isSet(offset)){
	offset = "0";
}
if(!UtilMethods.isSet(limit)){
	limit = "20";
}

if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
	userIsAdmin = true;
	if(UtilMethods.isSet(userToPullID)){
		userForPull = APILocator.getUserAPI().loadUserById(userToPullID,APILocator.getUserAPI().getSystemUser(),true);
	}
}

if(!UtilMethods.isSet(userToPullID)){
	userToPullID = null;
}

if(query == null){
	query = "";
}else{

	startAPISearchPull = Calendar.getInstance().getTimeInMillis();


	try{

		afterAPISearchPull = Calendar.getInstance().getTimeInMillis();
		startAPIPull = Calendar.getInstance().getTimeInMillis();


		if (UtilMethods.isSet(query)) {
			iresults =  APILocator.getContentletAPI().searchIndex(query,new Integer(limit),new Integer(offset),sortBy,userForPull,true);
			cons = APILocator.getContentletAPI().search(query,new Integer(limit),new Integer(offset),sortBy,userForPull,true);
		}
		else {
			iresults =  new ArrayList();
			cons = new ArrayList();
		}
		afterAPIPull = Calendar.getInstance().getTimeInMillis();

		if(cons.size() > 0 && reindex){
			for(Contentlet con : cons) {
				con.setLowIndexPriority(true);
				//APILocator.getDistributedJournalAPI().addContentIndexEntryToDelete(con.getIdentifier());
				//APILocator.getDistributedJournalAPI().addContentIndexEntry(con);
			}
		}

	}
	catch(Exception pe){
		iresults = new ArrayList();
		nastyError = pe.toString();
	}
}

Layout layout = (Layout) request.getAttribute(WebKeys.LAYOUT);

String referer = new URLEncoder().encode("/c/portal/layout?p_l_id=" + layout.getId() + "&p_p_id="+PortletID.QUERY_TOOL+"&");

query = Xss.strip(query);
%>
<script>
	dojo.require("dijit.form.NumberTextBox");
</script>

<!-- START Split Screen -->
    <div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer">
	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" id="filterWrapper" splitter="false" region="leading" style="width: 400px;" class="portlet-sidebar-wrapper" >
	    <div class="portlet-sidebar">

			<div id="advancedSearch">
				<form name="query" action="<%= submitURL %>" method="post">
					<dl class="vertical">

						<dt><label><%= LanguageUtil.get(pageContext, "Lucene-Query") %> :</label></dt>
						<dd><textarea dojoType="dijit.form.Textarea" name="query" style="width:365px;min-height: 150px;" id="query" type="text"><%=UtilMethods.htmlifyString(query)%></textarea></dd>

						<dt><label><%= LanguageUtil.get(pageContext, "Offset") %> : </label></dt>
						<dd><input name="offset" id="offset" dojoType="dijit.form.NumberTextBox" type="text" value="<%=offset%>" size="10" /></dd>
				
						<dt><label><%= LanguageUtil.get(pageContext, "Limit") %> : </label></dt>
						<dd><input name="limit" id="limit" dojoType="dijit.form.NumberTextBox" type="text" value="<%=limit%>" size="10" /></dd>
				
						<dt><label><%= LanguageUtil.get(pageContext, "Sort") %> : </label></dt>
						<dd><input name="sort" id="sort" dojoType="dijit.form.TextBox" type="text" value="<%=sortBy%>" size="10" /></dd>
				
						<dt><label><%= LanguageUtil.get(pageContext, "UserID") %> : </label></dt>
						<dd><input name="userid" id="userid" dojoType="dijit.form.TextBox" type="text" value="<%=UtilMethods.webifyString(userToPullID)%>" size="40" <% if(!userIsAdmin){ %> disabled="disabled" <% } %>/></dd>
					</dl>
					<div class="inline-form">
						<input name="reindexResults" id="reindexResults" dojoType="dijit.form.CheckBox" type="checkbox"  value="true" <% if(!userIsAdmin){ %> disabled="disabled" <% } %>/>
						<label><%= LanguageUtil.get(pageContext, "Reindex-selected-contents") %></label>
					</div>
						<div class="buttonRow">
							<button type="submit" id="submitButton" dojoType="dijit.form.Button" value="Submit"><%= LanguageUtil.get(pageContext, "Submit") %></button>
					</div>

			        <script language="Javascript">
						/**
							focus on search box
						**/
						require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
							dojo.require('dojox.timing');
							t = new dojox.timing.Timer(500);
							t.onTick = function(){
							  focusUtil.focus(dom.byId("query"));
							  t.stop();
							}
							t.start();
						});
					</script>
				</form>
			</div>
		</div>
	</div>

	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="portlet-content-search" id="contentWrapper" style="overflow-y:auto; overflow-x:auto;">
		<div class="portlet-main" style="margin: 35px 20px;">

			<%if(UtilMethods.isSet(nastyError)){%>
				<dl>
					<dt style='color:red;'><%= LanguageUtil.get(pageContext, "Query-Error") %> : </dt>
					<dd><%=nastyError %></dd>
				</dl>

			<%}else if(iresults != null){%>

			<table class="listingTable">
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "The-total-results-are") %> :</strong> <%=iresults == null ? "0" : iresults.size()%></td>
					<td></td>
				</tr>
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "Query-took") %> :</strong> <<%= afterAPISearchPull-startAPISearchPull %> ms</td>
					<td><em><%= LanguageUtil.get(pageContext, "This-includes-permissions-but-returns-only-the-index-objects") %></em></td>
				</tr>
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "Content-Population-took") %> :</strong> <%= afterAPIPull-startAPIPull %> ms</td>
					<td><em><%= LanguageUtil.get(pageContext, "This-includes-permissions-and-returns-full-content-objects") %></em></td>
				</tr>
			</table>
				<!-- <tr>
					<td><%= LanguageUtil.get(pageContext, "Query-is") %> :</td>
					<td><%=UtilMethods.htmlifyString(query)%></td>
				</tr>
				<tr>
					<td><%= LanguageUtil.get(pageContext, "Translated-query-is") %> :</td>
					<td><%=translatedQuery%></td>
				</tr>
				<tr>
					<td><%= LanguageUtil.get(pageContext, "The-offset-is") %> :</td>
					<td><%=offset%></td>
				</tr>
				<tr>
					<td><%= LanguageUtil.get(pageContext, "The-limit-is") %> :</td>
					<td><%=limit%></td>
				</tr>
				<tr>
					<td><%= LanguageUtil.get(pageContext, "The-sort-is") %>:</td>
					<td><%=sortBy%> </td>
				</tr>
				 -->
			</table>
			<!--
			<table class="listingTable">
				<tr>
					<th><%= LanguageUtil.get(pageContext, "INODE") %></th>
					<th><%= LanguageUtil.get(pageContext, "IDENTIFIER") %></th>
					<th><%= LanguageUtil.get(pageContext, "score") %></th>
				</tr>
				<% for (ContentletSearch r : iresults){%>
					<tr>
						<td><%= r.getInode() %></td>
						<td><%= r.getIdentifier() %></td>
						<td><%= r.getScore() %></td>
					</tr>
				<% } %>
			</table>
			-->

			<div id="results" style="margin-top: 30px;">
				<table class="listingTable">
					<!--<tr>
						<th colspan="3"><%= LanguageUtil.get(pageContext, "INODE") %></th>
					</tr>-->
					<%if(iresults.size() >0){ %>

						<% for(Contentlet c : cons) {%>
							<tr>
								<th><strong><%= counter %>.</th>
								<th><strong><%= LanguageUtil.get(pageContext, "Title") %></strong></th>
								<th>
									<a href="/c/portal/layout?p_l_id=<%=layout.getId() %>&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&p_p_mode=view&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/edit_contentlet&_<%=PortletID.CONTENT%>_cmd=edit&inode=<%=c.getInode() %>&referer=<%=referer %>">
										<%=c.getTitle() %>
									</a>
								</th>
							</tr>
							<tr>
								<td></td>
								<td><strong><%= LanguageUtil.get(pageContext, "ContentType") %>:</strong></td>
								<td width="90%"><%=c.getStructure().getVelocityVarName() %></td>
							</tr>
							<tr>
								<td></td>
								<td><strong><%= LanguageUtil.get(pageContext, "Inode") %>:</strong></td>
								<td width="90%"><%=c.getInode() %></td>
							</tr>
							<tr>
								<td></td>
								<td><strong><%= LanguageUtil.get(pageContext, "Identifier") %>:</strong></td>
								<td><%= c.getIdentifier() %></td>
							</tr>
							<!-- <tr >
								<td></td>
								<td colspan="2">
									<div style="padding-bottom: 25px;"><%= UtilMethods.makeHtmlSafe(ContentletUtil.getContentPrintableMap(user, c).toString()) %></div>
								</td>
							</tr> -->

							<%	counter++;%>
						<%}%>
					<% }else{ %>
						<div id="result" style="text-align:center; padding: 40px;"><%= LanguageUtil.get(pageContext, "No-Results") %></div>
					<%} %>
				</table>
			</div>
		<%} %>
		</div>
	</div>
</div>

