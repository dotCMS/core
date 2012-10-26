<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	PublisherEndpointAPI pepAPI = APILocator.getPublisherEndpointAPI();
	List<PublishingEndPoint> endpoints = pepAPI.getAllEndpoints();
%>
<div style="float: left">
	<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Intro") %> 
</div>
<div style="float: right">
	<button dojoType="dijit.form.Button" onClick="addEndpoint();" iconClass="plusIcon">
		<%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %> 
	</button>				
</div>			
<div>&nbsp;</div>
<div>&nbsp;</div>
<table class="listingTable shadowBox">
	<tr>
		<th style="width:40px"></th>		
		<th><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></strong></th>	
		<th><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %></strong></th>
		<th><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %></strong></th>
		<th><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %></strong></th>
		<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %></strong></th>
		<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending") %></strong></th>
	</tr>
	<%
		if(endpoints.size()>0){
			for(PublishingEndPoint endpoint : endpoints){
	%>
	<tr>
		<td><a style="cursor: pointer" onclick="javascript: alert('1')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>"><span class="deleteIcon"></span></a>&nbsp;<a style="cursor: pointer" onclick="javascript: alert('2')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>"><span class="editIcon"></span></a></td>
		<td><%=endpoint.getServerName()%></td>
		<td><%=endpoint.getAddress()%></td>
		<td><%=endpoint.getPort()%></td>
		<td><%=endpoint.getProtocol()%></td>
		<td><img class="center" src="/html/images/icons/<%=(endpoint.isEnabled()?"status.png":"status-offline.png")%>"/></td>
		<td><img class="center" src="/html/images/icons/<%=(endpoint.isSending()?"status.png":"status-offline.png")%>"/></td>
	</tr>
	<%
			}
		}else{
	%>	
	<tr>
		<td colspan="4" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
	</tr>	
	<%
		}
	%>
</table>