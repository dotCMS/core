<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.sitesearch.model.SiteSearchAudit"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAuditAPI"%>
<%@ include file="/html/common/init.jsp"%>
<%
SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
SiteSearchAuditAPI audit = APILocator.getSiteSearchAuditAPI();
String jobId=request.getParameter("jobId");
int offset = request.getParameter("offset")!=null ? Integer.parseInt(request.getParameter("offset")) : 0;
int limit = request.getParameter("limit")!=null ? Integer.parseInt(request.getParameter("limit")) : 15;
List<ScheduledTask> tasks = ssapi.getTasks();
%>

<select id="auditJobSel" dojoType="dijit.form.FilteringSelect" >
   <% for(ScheduledTask tt : tasks) { %>
         <% String jId=(String)tt.getProperties().get("JOB_ID"); jId=jId!=null ? jId : tt.getJobName(); %>
         <option value="<%= jId %>" <%= jobId!=null && jobId.equals(jId) ? "selected='true'" : "" %>>
            <%= tt.getJobName() %>
         </option>
   <% } %>
</select>

<button dojoType="dijit.form.Button" iconClass="searchIcon" onClick="refreshAuditData(dijit.byId('auditJobSel').get('value'),0,<%=limit%>)">
<%= LanguageUtil.get(pageContext, "Load") %>
</button>

<% if(UtilMethods.isSet(jobId)) { %>

<%  List<SiteSearchAudit> recents = audit.findRecentAudits(jobId, offset, limit); %>

<% if(offset>0) { %>
<button dojoType="dijit.form.Button" iconClass="previousIcon" onClick="refreshAuditData('<%=jobId %>',<%=offset-limit %>,<%=limit%>)">
<%= LanguageUtil.get(pageContext, "Newer") %>
</button>
<% } %>

<% if(recents.size()>=limit) { %>
<button dojoType="dijit.form.Button" iconClass="nextIcon" onClick="refreshAuditData('<%=jobId %>',<%=offset+limit %>,<%=limit%>)">
<%= LanguageUtil.get(pageContext, "Older") %>
</button>
<% } %>

<table class="listingTable" style="width:99%">
    <tr>
     <th nowrap><%= LanguageUtil.get(pageContext, "Job") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Index") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Hosts") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Language") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Include/Exclude") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Paths") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Incremental") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Fire-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Start-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "End-Date") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Files-Count") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Pages-Count") %></th>
     <th nowrap><%= LanguageUtil.get(pageContext, "Urlmaps-Count") %></th>
    </tr>
  <% for(SiteSearchAudit a : recents) { %>
      <%
          StringBuilder hostList=new StringBuilder();
          for(String hid : a.getHostList().split(",")) {
        	  if(UtilMethods.isSet(hid))
              hostList.append(APILocator.getHostAPI().find(hid, APILocator.getUserAPI().getSystemUser(), false).getHostname())
                      .append("  ");
          }
          StringBuilder langList=new StringBuilder();
          for(String lid : a.getLangList().split(",")) {
              Language lang=APILocator.getLanguageAPI().getLanguage(lid);
              langList.append(lang.getLanguageCode()).append("_").append(lang.getCountryCode()).append(" ");
          }
      %>
     <tr>
      <td><%= a.getJobName() %></td>
      <td><%= a.getIndexName() %></td>
      <td><%= hostList.toString() %></td>
      <td><%= langList.toString() %></td>
      <td><span class="<%= a.isPathInclude() ? "plus" : "minus" %>Icon">&nbsp</span></td>
      <td><%= UtilMethods.isSet(a.getPath()) ? a.getPath() : "*" %></td>
      <td><span class="<%= a.isIncremental() ? "resolve" : "delete" %>Icon">&nbsp</span></td>
      <td><%= a.getFireDate() %></td>
      <td><%= (a.isIncremental() && a.getStartDate()!=null) ? a.getStartDate().toString() : "&nbsp;" %>
      <td><%= a.isIncremental() ? a.getEndDate().toString() : "&nbsp;" %>
      <td><%= a.getFilesCount() %></td>
      <td><%= a.getPagesCount() %></td>
      <td><%= a.getUrlmapsCount() %></td>
     </tr>
  <% } %>
</table>
<% } %>