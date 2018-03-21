
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest"%>
<%@page import="java.util.List"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder"%>
<%@page import="java.util.Map"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodeStats"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>

<%@ include file="/html/common/uservalidation.jsp"%>

<%

String indexName=request.getParameter("indexName");

ESClient client=new ESClient();
NodesStatsResponse ns=client.getClient().admin().cluster().nodesStats(new NodesStatsRequest()).actionGet();
%>
<strong>Cluster: <%=ns.getClusterName() %></strong>
<% for(NodeStats stats:ns.getNodes()) { %>
<table class="listingTable">
   <tr>
      <td> <strong>Node name</strong> </td>
      <td> <%= stats.getNode().getName() %> <%=stats.getNode().isMasterNode() ? "(master)" : ""  %> </td>
   </tr>
   <tr>
      <td> Site Name </td>
      <td> <%= stats.getHostname() %> </td>
   </tr>
   <tr>
      <td> Address </td>
      <td> <%= stats.getNode().getAddress() %> </td>
   </tr>
   <tr>
      <td> Store Size </td>
      <td> <%= stats.getIndices().getStore().size().toString() %> </td>
   </tr>
   <tr>
      <td> Document Count </td>
      <td> <%= stats.getIndices().getDocs().getCount() %> </td>
   </tr>
</table>
<% } %>