
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest"%>
<%@page import="java.util.List"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder"%>
<%@page import="java.util.Map"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>
<%@ page
        import="static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS" %>
<%@ page import="com.dotcms.content.elasticsearch.business.ESIndexAPI" %>
<%@ page import="com.dotcms.content.elasticsearch.business.ClusterStats" %>
<%@ page import="com.dotcms.content.elasticsearch.business.NodeStats" %>

<%@ include file="/html/common/uservalidation.jsp"%>

<%
   final ESIndexAPI esIndexAPI = new ESIndexAPI();
   final ClusterStats clusterStats = esIndexAPI.getClusterStats();
%>
<strong>Cluster: <%=clusterStats.getClusterName() %></strong>
<% for(NodeStats stats:clusterStats.getNodeStats()) { %>
<table class="listingTable">
   <tr>
      <td> <strong>Node name</strong> </td>
      <td> <%= stats.getName() %> <%=stats.isMaster() ? "(master)" : ""  %> </td>
   </tr>
   <tr>
      <td> Site Name </td>
      <td> <%= stats.getHost() %> </td>
   </tr>
   <tr>
      <td> Address </td>
      <td> <%= stats.getTransportAddress() %> </td>
   </tr>
   <tr>
      <td> Store Size </td>
      <td> <%= stats.getSize() %> </td>
   </tr>
   <tr>
      <td> Document Count </td>
      <td> <%= stats.getDocCount() %> </td>
   </tr>
</table>
<% } %>