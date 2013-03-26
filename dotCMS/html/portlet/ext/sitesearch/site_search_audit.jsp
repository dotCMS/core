<%@page import="com.dotmarketing.sitesearch.model.SiteSearchAudit"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAuditAPI"%>
<% 
SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
SiteSearchAuditAPI audit = APILocator.getSiteSearchAuditAPI();
String jobId=request.getParameter("jobId");
int offset = request.getParameter("offset")!=null ? Integer.parseInt(request.getParameter("offset")) : 0;
int limit = request.getParameter("limit")!=null ? Integer.parseInt(request.getParameter("limit")) : 20;
List<ScheduledTask> tasks = ssapi.getTasks();
%>

<select id="auditJobSel" dojoType="dijit.form.FilterinSelect" 
        onChange="refreshAuditData(dijit.byId('auditJobSel').get('value'),0,<%=limit%>)">
   <% for(ScheduledTask tt : tasks) { %>
         <% String jId=tt.getProperties().get("JOB_ID"); jId=jId!=null ? jId : tt.getJobName(); %>
         <option value="<%= jId %>" <%= jobId!=null && jobId.equals(jId) ? "selected='true'" : "" %>>
            <%= tt.getJobName() %>
         </option>
   <% } %>
</select>

<% if(UtilMethods.isSet(jobId)) { %>
<%  List<SiteSearchAudit> recents = audit.findRecentAudits(jobId, offset, limit)%>
<table class="listingTable" style="width:99%">
     <th nowrap><%= LanguageUtil.get(pageContext, "Job") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "IndexAlias") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Hosts") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Language") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Include/Exclude") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Paths") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Fire-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Start-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "End-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Files-Count") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Pages-Count") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Urlmaps-Count") %></th>
  <% for(SiteSearchAudit a : recents) { %>
      <td><%= a.getJobName() %></td>
      <td><%= a.getIndexName() %></td>
      <td><%= a.getHostList() %></td>
      <td><%= a.getLangList() %></td>
      <td><%= a.isPathInclude() ? "inc" : "excl" %></td>
      <td><%= a.getPath() %></td>
      <td><%= a.getFireDate() %></td>
      <td><%= a.isIncremental() ? a.getStartDate().toString() : "&nbsp;" %>
      <td><%= a.isIncremental() ? a.getEndDate().toString() : "&nbsp;" %>
      <td><%= a.getFilesCount() %></td>
      <td><%= a.getPagesCount() %></td>
      <td><%= a.getUrlmapsCount() %></td>
  <% } %>
</table>
<% } %>