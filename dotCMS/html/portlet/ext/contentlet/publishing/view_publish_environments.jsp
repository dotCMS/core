<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@ page import="com.dotcms.publisher.environment.bean.Environment"%>
<%

	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
	if(null!=request.getParameter("delEnv")){
		String id = request.getParameter("delEnv");
		eAPI.deleteEnvironment(id);
	}

	List<Environment> environments = eAPI.findAllEnvironments();

	PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();

%>

<style>
.environmentsTable{width:99%;margin:0 auto;font-size:11px;}
.environmentsTable tr{}
.environmentsTable th, .listingTable td{padding:5px 8px;border:1px solid #d0d0d0;border-top: none;}
.environmentsTable th{font-weight:bold;background:#ececec;}
.environmentsTable td a{text-decoration:none;}
.environmentsTable td table tr{border:none;}
.environmentsTable td button, .listingTable table.dijitSelect, .listingTable td .dijitTextBox{font-size:12px;}
</style>


<div class="yui-g portlet-toolbar">
	<div class="yui-u first">
		<span class="sServerIcon"></span>
		<span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server_Short") %></span>
	</div>

	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="goToAddEnvironment();" iconClass="plusIcon">
			<%= LanguageUtil.get(pageContext, "publisher_Add_Environment") %>
		</button>
	</div>
</div>
<div style="padding-top: 5px">
	<%
			boolean hasEnvironments = false;
			for(Environment environment : environments){
				hasEnvironments=true;%>
			<table class="environmentsTable">
				<tr style="line-height:20px; padding-bottom: 15px">
					<td nowrap="nowrap" style="padding-left: 10px; width: 53px">
						<a style="cursor: pointer" onclick="deleteEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment") %>">
						<span class="deleteIcon"></span></a>&nbsp;
						<a style="cursor: pointer" onclick="goToEditEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Environment_Title") %>">
						<span class="editIcon"></span></a>
					</td>
					<td style="padding-left: 10px; font-size: 12px" >
						<%= LanguageUtil.get(pageContext, "publisher_Environment_Name") %> : <%=environment.getName()%>,
						<%= LanguageUtil.get(pageContext, "Push-To-All") %> : <%=environment.getPushToAll()%>
					</td>
					<td align="right">
						<button dojoType="dijit.form.Button" onClick="goToAddEndpoint('<%=environment.getId()%>', 'false');" iconClass="plusIcon">
							<%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %>
						</button>
					</td>

				</tr>

				<tr>
					<td colspan="4" style="padding-bottom: 0; padding-top: 10px;">
						<table class="listingTable" style="width:100%">
							<tr>
								<th style="width:40px"></th>

								<th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>
								<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "Status") %></th>
								<th style="text-align: center"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_To") %></th>
							</tr>
							<%


								if(null!=request.getParameter("delEp")){
									String id = request.getParameter("delEp");
									pepAPI.deleteEndPointById(id);
								}
								List<PublishingEndPoint> endpoints = pepAPI.findSendingEndPointByEnvironment(environment.getId());
								boolean hasRow = false;
								for(PublishingEndPoint endpoint : endpoints){
									if(endpoint.isSending()){
										continue;
									}
									hasRow=true;%>
								<tr <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%>>
									<td nowrap="nowrap">
										<a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>', true)" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
										<span class="deleteIcon"></span></a>&nbsp;
										<a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>', '<%=environment.getId()%>', 'false')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
										<span class="editIcon"></span></a>
									</td>


									<td width="50%" >
										<%=endpoint.getServerName()%>
									</td>
									<td align="center" nowrap="nowrap"  width="40" >
										<%=("https".equals(endpoint.getProtocol())) ? "<span class='encryptIcon'></span>": "" %>
										<%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%>

									</td>
									<td align="center" nowrap="nowrap" >
											<%=endpoint.getProtocol()%>://<%=endpoint.getAddress()%>:<%=endpoint.getPort()%>
									</td>


								</tr>
							<%}%>

							<%if(!hasRow){ %>

								<tr>
									<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Servers") %></td>
								</tr>
							<%}%>
						</table>
					</td>
				</tr>

				<%if(!hasEnvironments){ %>

				<tr>
					<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
				</tr>
				<%}%>

			</table><br>
		<%}%>





</div>




