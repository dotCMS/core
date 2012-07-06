
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest"%>
<%@page import="java.util.List"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder"%>
<%@page import="java.util.Map"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodeStats"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse"%>
<%@page import="org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>
<% 

String indexName=request.getParameter("indexName");

ESClient client=new ESClient();
NodesStatsResponse ns=client.getClient().admin().cluster().nodesStats(new NodesStatsRequest()).actionGet();
NodeStats[] nsArray=ns.getNodes();
%>
<strong>Cluster: <%=ns.getClusterName() %></strong>
<% for(int i=0;i<nsArray.length;i++) { %>
<table class="listingTable">
   <tr>
      <td> <strong>Node name</strong> </td>
      <td> <%= nsArray[i].node().name() %> <%=nsArray[i].node().isMasterNode() ? "(master)" : ""  %> </td>
   </tr>
   <tr>
      <td> Host Name </td>
      <td> <%= nsArray[i].getHostname() %> </td>
   </tr>
   <tr>
      <td> Address </td>
      <td> <%= nsArray[i].node().address() %> </td>
   </tr>
   <tr>
      <td> Store Size </td>
      <td> <%= nsArray[i].indices().store().getSize().toString() %> </td>
   </tr>
   <tr>
      <td> Document Count </td>
      <td> <%= nsArray[i].indices().docs().count() %> </td>
   </tr>
</table>
<% } %>
