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
<%@page import="com.liferay.util.cal.CalendarUtil"%><%@page import="org.apache.lucene.queryParser.ParseException"%>
<%@page import="java.util.ArrayList"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
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
String submitURL = com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"EXT_LUCENE_TOOL");
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
	catch(ParseException pe){
		iresults = new ArrayList();
		nastyError = pe.toString();
	}
}

Layout layout = (Layout) request.getAttribute(WebKeys.LAYOUT);

String referer = new URLEncoder().encode("/c/portal/layout?p_l_id=" + layout.getId() + "&p_p_id=EXT_LUCENE_TOOL&");


%>
<script>
	dojo.require("dijit.form.NumberTextBox");
</script>
<div class="portlet-wrapper">
	<div>
		<form name="query" action="<%= submitURL %>" method="post">		
			<dl>	
				<dt><strong><%= LanguageUtil.get(pageContext, "Lucene-Query") %> :</strong></dt><dd>
				
				<textarea dojoType="dijit.form.Textarea" name="query" style="width:500px;min-height: 150px;" id="query" type="text"><%=UtilMethods.htmlifyString(query)%></textarea>
				
				</dd>
				<dt><strong><%= LanguageUtil.get(pageContext, "Offset") %> : </strong></dt><dd><input name="offset" id="offset" dojoType="dijit.form.NumberTextBox" type="text" value="<%=offset%>" size="10" /></dd>
				<dt><strong><%= LanguageUtil.get(pageContext, "Limit") %> : </strong></dt><dd><input name="limit" id="limit" dojoType="dijit.form.NumberTextBox" type="text" value="<%=limit%>" size="10" /></dd>
				<dt><strong><%= LanguageUtil.get(pageContext, "Sort") %> : </strong></dt><dd><input name="sort" id="sort" dojoType="dijit.form.TextBox" type="text" value="<%=sortBy%>" size="10" /></dd>
				<dt><strong><%= LanguageUtil.get(pageContext, "UserID") %> : </strong></dt><dd><input name="userid" id="userid" dojoType="dijit.form.TextBox" type="text" value="<%=UtilMethods.webifyString(userToPullID)%>" size="40" <% if(!userIsAdmin){ %> disabled="disabled" <% } %>/></dd>
				<dt><strong><%= LanguageUtil.get(pageContext, "Reindex-selected-contents") %> : </strong></dt><dd><input name="reindexResults" id="reindexResults" dojoType="dijit.form.CheckBox" type="checkbox"  value="true" <% if(!userIsAdmin){ %> disabled="disabled" <% } %>/></dd>
				
				<dt></dt><dd><button type="submit" id="submitButton" dojoType="dijit.form.Button" value="Submit"><%= LanguageUtil.get(pageContext, "Submit") %></button></dd>
			</dl>
		</form>
	</div>
	<hr>
	<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "Query-Error") %> : </dt>
			<dd><%=nastyError %></dd>
		</dl>
			
		
	<%}else if(iresults != null){%>
	
	<div>
		<div><strong><%= LanguageUtil.get(pageContext, "Query-took") %> : </strong><%= afterAPISearchPull-startAPISearchPull %> ms <em><%= LanguageUtil.get(pageContext, "This-includes-permissions-but-returns-only-the-index-objects") %></em></div>
		<div><strong><%= LanguageUtil.get(pageContext, "Content-Population-took") %> : </strong><%= afterAPIPull-startAPIPull %> ms <em><%= LanguageUtil.get(pageContext, "This-includes-permissions-and-returns-full-content-objects") %></em></div>
		<div><strong><%= LanguageUtil.get(pageContext, "Query-is") %> : </strong><%=query%></div>
		<div><strong><%= LanguageUtil.get(pageContext, "Translated-query-is") %> : </strong><%=translatedQuery%></div>
		<div><strong><%= LanguageUtil.get(pageContext, "The-offset-is") %> : </strong><%=offset%></div>
		<div><strong><%= LanguageUtil.get(pageContext, "The-limit-is") %> : </strong><%=limit%></div> 
		<div><strong><%= LanguageUtil.get(pageContext, "The-sort-is") %>: </strong><%=sortBy%></div> 
		
	</div>
	<div>&nbsp</div>
	
	
	
		<div id=results>
			<div><strong><%= LanguageUtil.get(pageContext, "The-total-results-are") %> : </strong><%=iresults == null ? "0" : iresults.size()%></div> 
	
			<% for (ContentletSearch r : iresults){%>
				<div id="result" style="padding-left:10px;"><strong><%= LanguageUtil.get(pageContext, "INODE") %> : </strong><%= r.getInode() %> <strong><%= LanguageUtil.get(pageContext, "IDENTIFIER") %> : </strong><%= r.getIdentifier() %></div>
			<% } %>
			<%if(iresults.size() >0){ %>
				<div>&nbsp</div>
				<div><strong><%= LanguageUtil.get(pageContext, "INODE") %></strong><hr /></div>
				
						
				<table>
				<% for(Contentlet c : cons) {%>
					<tr>
						<td><strong><%= counter %>.</td>
						<td width="100"><strong><%= LanguageUtil.get(pageContext, "Title") %>:</strong></td>
						
		
						
						
						<td><a href="/c/portal/layout?p_l_id=<%=layout.getId() %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getInode() %>&referer=<%=referer %>">
								<%=c.getTitle() %>
							</a>
						</td>
					</tr>
					<tr>
						<td>&nbsp;</td>
						<td><strong><%= LanguageUtil.get(pageContext, "Inode") %>:</strong></td>
						<td width="90%"><%=c.getInode() %></td>
					</tr>
					<tr>
						<td>&nbsp;</td>
						<td><strong><%= LanguageUtil.get(pageContext, "Identifier") %>:</strong></td>
						<td><%= c.getIdentifier() %></td>
					</tr>
					<tr >
						<td>&nbsp;</td>
						<td colspan=2>
							<div style="border-bottom:1px silver solid;padding:10px;"><%= UtilMethods.makeHtmlSafe(c.getMap().toString()) %></div>
						</td>
					</tr>
					<%	counter++;%>
				<%}%>
				</table>
			<% }else{ %>
				<div id="result"><%= LanguageUtil.get(pageContext, "No-Results") %></div>
			<%} %>
		</div>
	<%} %>
</div>

