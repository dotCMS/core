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
			<td>Cache Transport</td>
			<td align="right">{cache.cacheTransportClass}</td>
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
			<td><b><%= LanguageUtil.get(pageContext, "configuration_cluster_es_health") %></b></td>
			<td></td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td>
			<td align="right">{es.clusterName}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_timedout") %></td>
			<td align="right">{es.clusterName}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td>
			<td align="right">{es.numberOfNodes}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_data_nodes") %></td>
			<td align="right">{es.numberOfNodes}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_primary_shards") %></td>
			<td align="right">{es.activePrimaryShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_shards") %></td>
			<td align="right">{es.activeShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_relocating_shards") %></td>
			<td align="right">{es.relocatingShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_initializing_shards") %></td>
			<td align="right">{es.initializingShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_unassigned_shards") %></td>
			<td align="right">{es.unassignedShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_delayed_unassigned_shards") %></td>
			<td align="right">{es.delayedUnasignedShards}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_pending_tasks") %></td>
			<td align="right">{es.numberOfPendingTasks}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_in_flight_fetch") %></td>
			<td align="right">{es.numberOfInFlightFetch}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_task_max_waiting_in_queue_millis") %></td>
			<td align="right">{es.taskMaxWaitingInQueueMillis}</td>
		</tr>
		<tr>
			<td></td>
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_active_shards_percent_as_number") %></td>
			<td align="right">{es.activeShardsPercentAsNumber}</td>
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
			<td><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %></td>
			<td align="right">{assets.path}</td>

		</tr>
	</table>

	<a href="javascript:loadNetworkTab();" class="network-action__btn-refresh"><%= LanguageUtil.get(pageContext, "Refresh") %> <%= LanguageUtil.get(pageContext, "Status") %></a>
</div>
