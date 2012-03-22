<%@page import="com.dotmarketing.business.IdentifierCache" %>
<%@page import="com.dotmarketing.cache.LiveCache" %>
<%@page import="com.dotmarketing.cache.WorkingCache" %>
<%@page import="com.dotmarketing.business.APILocator" %>
<%@page import="com.dotmarketing.factories.ClickstreamFactory"%>
<%@page import="com.dotmarketing.beans.Clickstream"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.web.UserWebAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.factories.ClickstreamRequestFactory"%>
<%@page import="com.dotmarketing.beans.ClickstreamRequest"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>

<%
	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	User user = userWebAPI.getLoggedInUser(request);
	
	String layoutId = request.getParameter("layoutId");
	String userId = request.getParameter("userId");
	UserAPI userAPI = APILocator.getUserAPI();
	User viewUser = userAPI.loadUserById(userId, user, false);

	String clickstreamId = request.getParameter("clickstreamId");
	Clickstream clickstream = ClickstreamFactory.getClickstream(clickstreamId);
	List<ClickstreamRequest> clickstreamRequests = ClickstreamRequestFactory.getClickstreamRequestsByClickStream(clickstream);
	if(clickstreamRequests.size() > 0) {
    	Host h = APILocator.getHostAPI().find(clickstream.getHostId(), APILocator.getUserAPI().getSystemUser(), false); 
		ClickstreamRequest initialRequest = clickstreamRequests.get(0); 
%>


<div class="rigth">
	<button dojoType="dijit.form.Button" onClick="closeUserClickHistoryDetails()"><%= LanguageUtil.get(pageContext, "back") %></button>
</div>

<fieldset>
	<legend><%= LanguageUtil.get(pageContext, "user-click-detail") %></legend>
	<table class="listingTable">
	    <tr class="alternate_1">
	        <td nowrap>
	            <%= LanguageUtil.get(pageContext, "Server") %>
	        </td>
	        <td>
	            <%= initialRequest.getServerName() %>
	        </td>
	    </tr>
	    <tr class="alternate_2">
	        <td>
	            <%= LanguageUtil.get(pageContext, "Date") %>
	        </td>
	        <td>
	            <%= com.dotmarketing.util.UtilMethods.dateToHTMLDate(clickstream.getStart()) %><%= com.dotmarketing.util.UtilMethods.dateToHTMLTime(clickstream.getStart()) %>
	        </td>
	    </tr>
	    <tr class="alternate_1">
	        <td>
	            <%= LanguageUtil.get(pageContext, "Time-Spent") %>
	        </td>
	        <td>
	            <% 
	            	long diffMillis = clickstream.getLastRequest().getTime() - clickstream.getStart().getTime();
	            	diffMillis = diffMillis/1000;
	            	if(diffMillis > 60) {
	            		out.print((diffMillis/60) + "m " + diffMillis%60 + "s");
	            	} else {
	            		out.print((diffMillis + 1 ) +"s");
	            	} 
	            %>
	        </td>
	    </tr>
	    <tr class="alternate_2">
	        <td nowrap>
	            <%= LanguageUtil.get(pageContext, "Initial-Referer") %>
	        </td>
	        <td>
	            <%= clickstream.getInitialReferrer() %>
	        </td>
	    </tr>
	    <tr class="alternate_1">
	        <td>
	            <%= LanguageUtil.get(pageContext, "Web-Browser") %>
	        </td>
	        <td>
	            <%= clickstream.getUserAgent() %>
	        </td>
	    </tr>
	    <tr class="alternate_2">
	        <td>
	            <%= LanguageUtil.get(pageContext, "Remote-Address") %>
	        </td>
	        <td>
	            <%= clickstream.getRemoteAddress() %>
	        </td>
	    </tr>
	</table>
</fieldset>

<fieldset>
	<legend><%= LanguageUtil.get(pageContext, "user-clicks") %></legend>
	
	<table class="listingTable">
	    <tr>
	        <th>
	            <b><%= LanguageUtil.get(pageContext, "Click-Number") %></b>
	        </th>
	        <th>
	            <b><%= LanguageUtil.get(pageContext, "Page") %></b>
	        </th>
	        <th>
	            <b><%= LanguageUtil.get(pageContext, "Language") %></b>
	        </th>
	        <th>
	            <b><%= LanguageUtil.get(pageContext, "Time-Spent-on-Page") %></b>
	        </th>
	    </tr>
	<%
	    for(int i=0 ; i < clickstreamRequests.size(); i++) { 
	    	ClickstreamRequest clickstreamRequest = clickstreamRequests.get(i);
	    	String className = i % 2 == 0?"alternate_2":"alternate_1";
	%>
	    <tr class="<%= className %>">
	        <td>
	            <%=clickstreamRequest.getRequestOrder() %>
	        </td>
	        <td>
	        <%
	        	Identifier pageIdentifier; 
	        	if(h != null && (pageIdentifier = APILocator.getIdentifierAPI().find(h, clickstreamRequest.getRequestURI())) != null &&
	        		InodeUtils.isSet(pageIdentifier.getInode())) {
		        	String idInode = pageIdentifier.getInode();
					Map<String, String[]> params = new HashMap<String, String[]>();
			    	params.put("struts_action",new String[] {"/ext/htmlpageviews/view_htmlpage_views"});
			    	params.put("pageIdentifier",new String[] { idInode });
					String url = PortletURLUtil.getRenderURL(request, layoutId, WindowState.MAXIMIZED.toString(), params, "EXT_USER_ADMIN");
	        %>
		        <a href="<%= url %>"><%= clickstreamRequest.getRequestURI() %></a>
		    <% } else { %>
		        <%= clickstreamRequest.getRequestURI() %>
		    <% } %>
		    </td>
		    <td>
		        <%
		        	if(clickstreamRequest.getLanguageId() > 0){ 
		        		String langBrowser=APILocator.getLanguageAPI().getLanguage(clickstreamRequest.getLanguageId()).getLanguage(); 
		        %>
		        <%= LanguageUtil.get(pageContext,langBrowser) %><%} %>
		    </td>
		    <td>
		        <%
		        	if(i < clickstreamRequests.size() -1) {
			        	ClickstreamRequest csr2 = clickstreamRequests.get(i + 1); 
			        	diffMillis = (csr2.getTimestamp().getTime() - clickstreamRequest.getTimestamp().getTime()); 
				        diffMillis = diffMillis/1000;
				        if(diffMillis > 60) {
			    		    out.print((diffMillis/60) + "m " + diffMillis%60 + "s");
			        	} else {
			        		out.print((diffMillis + 1 ) +"s");
			        	} 
			        } else { 
		        %>
		        	exited
		        <%
		        	} 
		        %>
		    </td>
		</tr>
	<% } %>
	</table>
</fieldset>
<% } else { %>
No click history recorded
<% } %>