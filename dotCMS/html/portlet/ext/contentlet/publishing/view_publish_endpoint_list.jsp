<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	PublisherEndpointAPI pepAPI = APILocator.getPublisherEndpointAPI();
	if(null!=request.getParameter("delEp")){
		String id = request.getParameter("delEp");
		pepAPI.deleteEndpointById(id);
	}
	List<PublishingEndPoint> endpoints = pepAPI.getAllEndpoints();
%>
<div style="float: left">
	<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Intro") %> 
</div>
<div style="float: right">
	<button dojoType="dijit.form.Button" onClick="goToAddEndpoint();" iconClass="plusIcon">
		<%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %> 
	</button>				
</div>			
<div>&nbsp;</div>
<div>&nbsp;</div>
<table class="listingTable shadowBox">
	<tr>
		<th style="width:40px"></th>		
		<th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>	
		<th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server") %></th>
		<th style="text-align: center"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %></th>
	</tr>
	<%
		if(endpoints.size()>0){
			for(PublishingEndPoint endpoint : endpoints){
	%>
	<tr>
		<td nowrap="nowrap">
			<a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
			<span class="deleteIcon"></span></a>&nbsp;
			<a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
			<span class="editIcon"></span></a>
		</td>
		<td style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
			<%=endpoint.getServerName()%> 
		</td>
		<td align="center" >
			<%=endpoint.isSending()  %>
			<%if(UtilMethods.isSet(endpoint.getAddress())){ %>
				<%=endpoint.getProtocol()%>://<%=endpoint.getAddress()%>:<%=endpoint.getPort()%>
			<%} %>
		</td>
		<td align="center"><%=(endpoint.isEnabled()?"<img src='/html/images/icons/status.png'":"")%></td>
	</tr>
	<%
			}
		}else{
	%>	
	<tr>
		<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
	</tr>	
	<%
		}
	%>
</table>