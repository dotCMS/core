<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<%
	String rewireable = request.getParameter("rewireable");
%>

<div class="network-action__content">
	<h3 class="network-action__title">{server.host} - {server.displayServerId}</h3>
	<table class="network-action__list">
		<tr>
			<td><i class="statusIcon {cache.status}"></i></td>
			<td><div id="myShowCacheButton"><b>Cache Transport</b></div></td>
			<td></td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td>
			<td align="right">{cache.clusterName}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td>
			<td align="right">{cache.numberOfNodes}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_channel_open") %></td>
			<td align="right">{cache.open}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %></td>
			<td align="right" nowrap>{cache.address}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_received_bytes") %></td>
			<td align="right" nowrap>{cache.receivedBytes}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_cache_port") %></td>
			<td align="right" nowrap>{cache.port}</td>
		</tr>
	</table>
	<table class="network-action__list">
		<tr>
			<td><i class="statusIcon {es.status}"></i></td>
			<td><b>Index</b></td>
			<td></td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td>
			<td align="right">{es.clusterName}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td>
			<td align="right">{es.numberOfNodes}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_shards") %></td>
			<td align="right">{es.activeShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_primary_shards") %></td>
			<td align="right">{es.activePrimaryShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_es_port") %></td>
			<td align="right" nowrap>{es.port}</td>
		</tr>
	</table>
	<table class="network-action__list">
		<tr>
			<td><i class="statusIcon {assets.status}"></i></td>
			<td><b>Assets</b></td>
			<td></td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "Read") %>/<%= LanguageUtil.get(pageContext, "Write") %></td>
			<td align="right">{assets.canRead}{assets.canWrite}</td>
		</tr>
		<tr>
			<td></td>
			<td colspan="2">
				<div style="width: 100px"><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %>:  {assets.path}</div>
			</td>
		</tr>
	</table>
	<table class="network-action__list">
		<tr>
			<td><span class="licenseIcon"></span></td>
			<td><b><%= LanguageUtil.get(pageContext, "configuration_cluster_license_repo") %></b></td>
			<td></td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "Total") %></td>
			<td align="right">{licenseRepo.total}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "Available") %></td>
			<td align="right">{licenseRepo.available}</td>
		</tr>
	</table>
	<a href="javascript:loadNetworkTab();" class="network-action__btn-refresh"><%= LanguageUtil.get(pageContext, "Refresh") %> <%= LanguageUtil.get(pageContext, "Status") %></a>
</div>
