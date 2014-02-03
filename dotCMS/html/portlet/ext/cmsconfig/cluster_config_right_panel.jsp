<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<%
	String rewireable = request.getParameter("rewireable");
%>

<div class="actionPanelContent">

	<h2>Action Panel</h2>

	<hr>

	<table>
		<tr>
			<td style="width:15px;"><i class="fa fa-circle {cache.status}"></i></td>
			<td style="width:50%;padding: 0 5px;"><b>Cache</b></td>
			<td style="width:50%;">&nbsp;</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td>
			<td align="right">{cache.clusterName}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td>
			<td align="right">{cache.numberOfNodes}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_channel_open") %></td>
			<td align="right">{cache.open}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %></td>
			<td align="right" nowrap>{cache.address}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_received_bytes") %></td>
			<td align="right" nowrap>{cache.receivedBytes} bytes</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_cache_port") %></td>
			<td align="right" nowrap>{cache.port}</td>
		</tr>
	</table>

	<hr>

	<table>
		<tr>
			<td style="width:15px;"><i class="fa fa-1x fa-circle {es.status}"></i></td>
			<td style="width:100%;"><b>Index</b></td>
			<td style="width:15px;">&nbsp;</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td>
			<td align="right">{es.clusterName}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td>
			<td align="right">{es.numberOfNodes}</td>
		</tr>

		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_shards") %></td>
			<td align="right">{es.activeShards}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_es_port") %></td>
			<td align="right" nowrap>{es.port}</td>
		</tr>
	</table>

	<hr>

	<table>
		<tr>
			<td style="width:15px;"><i class="fa fa-circle {assets.status}"></i></td>
			<td style="width:100%;"><b>Assets</b></td>
			<td style="width:15px;">&nbsp;</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><%= LanguageUtil.get(pageContext, "Read") %>/<%= LanguageUtil.get(pageContext, "Write") %></td>
			<td align="right">{assets.canRead}/{assets.canWrite}</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td colspan="2">
				<div style="width: 100px"><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %>:  {assets.path}</div>
			</td>
		</tr>
	</table>

	<% if(rewireable.equalsIgnoreCase("true")) { %>

		<hr>

		<div style="text-align: center;">

			<button dojoType="dijit.form.Button" onClick="showClusterPropertiesDialog();"><div class="bigBtn"><%= LanguageUtil.get(pageContext, "configuration_cluster_wire_node") %></div></button>

		</div>



	<% } %>

	<hr>

	<div style="margin:15px;padding:0 0 20px 0;font-size: 88%;text-align:center;">
		<a href="#" onclick="refreshStatus()"><%= LanguageUtil.get(pageContext, "Refresh") %> <%= LanguageUtil.get(pageContext, "Status") %></a>
	</div>

</div>
