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


<div class="yui-g portlet-toolbar">
	<div class="yui-u first">

		
	</div>
	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="goToAddEndpoint();" iconClass="plusIcon">
			<%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %> 
		</button>		
	</div>
</div>


<div class="yui-g portlet-toolbar">
	<div class="yui-u first">
		<span class="sServerIcon"></span>
		<span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server_Short") %></span>
	</div>
	<div class="yui-u" style="text-align:right;">

	</div>
</div>

	
	
	

<table class="listingTable">
	<tr>
		<th style="width:40px"></th>		

		<th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>	
		<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "status") %></th>
		<th style="text-align: center"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_To") %></th>
	</tr>
	<%
		boolean hasRow = false;
		for(PublishingEndPoint endpoint : endpoints){
			if(endpoint.isSending()){
				continue;
			}
			hasRow=true;%>
		<tr <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%>>
			<td nowrap="nowrap">
				<a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
				<span class="deleteIcon"></span></a>&nbsp;
				<a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
				<span class="editIcon"></span></a>
			</td>
	
		
			<td style="cursor: pointer" width="50%" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				<%=endpoint.getServerName()%> 
			</td>
			<td align="center" nowrap="nowrap" style="cursor: pointer" width="40" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				<%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%>
			</td>
			<td style="cursor: pointer" align="center" nowrap="nowrap" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				
	
					<%=endpoint.getProtocol()%>://<%=endpoint.getAddress()%>:<%=endpoint.getPort()%>
	
			</td>
	
	
		</tr>
	<%}%>
	
	<%if(!hasRow){ %>
	
		<tr>
			<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>	
	<%}%>
</table>



<br> 
<br>







<div class="yui-g portlet-toolbar">
	<div class="yui-u first">
		<span class="rServerIcon"></span>
		<span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Receiving_Server_Short") %></span>
	</div>
	<div class="yui-u" style="text-align:right;">

	</div>
</div>



<table class="listingTable">
	<tr>
		<th style="width:40px"></th>		
		<th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>	
		<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "status") %></th>
		<th style="text-align: center"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_From") %></th>
	</tr>
	<%
		hasRow = false;
		for(PublishingEndPoint endpoint : endpoints){
			if(!endpoint.isSending()){
				continue;
			}
			hasRow=true;%>
		<tr <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%>>
			<td nowrap="nowrap">
				<a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
				<span class="deleteIcon"></span></a>&nbsp;
				<a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
				<span class="editIcon"></span></a>
			</td>
	
		
			<td style="cursor: pointer" width="50%" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				<%=endpoint.getServerName()%> 
			</td>
			<td align="center" nowrap="nowrap" style="cursor: pointer" width="40" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				<%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%>
			</td>
			<td style="cursor: pointer" align="center" nowrap="nowrap" onclick="goToEditEndpoint('<%=endpoint.getId()%>')">
				
	
					<%=endpoint.getAddress()%>
	
			</td>
	
		</tr>
	<%}%>
	
	<%if(!hasRow){ %>
	
		<tr>
			<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>	
	<%}%>
</table>


