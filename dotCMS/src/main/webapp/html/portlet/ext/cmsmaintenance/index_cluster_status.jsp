
<%@ page import="com.dotcms.content.elasticsearch.business.ESIndexAPI" %>
<%@ page import="com.dotcms.content.index.domain.ClusterStats" %>
<%@ page import="com.dotcms.content.index.domain.NodeStats" %>

<%@ include file="/html/common/uservalidation.jsp"%>

<%
   final ESIndexAPI esIndexAPI = new ESIndexAPI();
   final ClusterStats clusterStats = esIndexAPI.getClusterStats();
%>
<strong>Cluster: <%=clusterStats.clusterName() %></strong>
<% for(NodeStats stats:clusterStats.nodeStats()) { %>
<table class="listingTable">
   <tr>
      <td> <strong>Node name</strong> </td>
      <td> <%= stats.name() %> <%=stats.master() ? "(master)" : ""  %> </td>
   </tr>
   <tr>
      <td> Site Name </td>
      <td> <%= stats.host() %> </td>
   </tr>
   <tr>
      <td> Address </td>
      <td> <%= stats.transportAddress() %> </td>
   </tr>
   <tr>
      <td> Store Size </td>
      <td> <%= stats.size() %> </td>
   </tr>
   <tr>
      <td> Document Count </td>
      <td> <%= stats.docCount() %> </td>
   </tr>
</table>
<% } %>
