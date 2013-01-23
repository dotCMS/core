<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@page import="com.dotcms.publisher.business.EndpointDetail"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.publisher.business.PublishAuditHistory"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>

<%
	String bundleId = request.getParameter("bundle");
	PublisherEndpointAPI pepAPI = APILocator.getPublisherEndpointAPI();
	PublishAuditHistory currentEndpointHistory = null;
	int status = 0;
	if(null!=bundleId){
		PublishAuditStatus publishAuditStatus = PublishAuditAPI.getInstance().getPublishAuditStatus(bundleId);
		String pojo_string = (String)publishAuditStatus.getStatusPojo().getSerialized();
		currentEndpointHistory = PublishAuditHistory.getObjectFromString(pojo_string);
		status = publishAuditStatus.getStatus().getCode();
	}
%>


<script type="text/javascript">
	function backToAuditList() {
	   dijit.byId('bundleDetail').hide();
	}
</script>
	

<% if(null!=currentEndpointHistory){%>

	<div style="padding-left:10px;font-size: small;color: gray;">
		<div style="float:left">
			<h3><%= LanguageUtil.get(pageContext, "status") %>: <%= LanguageUtil.get(pageContext, "publisher_status_" + PublishAuditStatus.getStatusByCode(status))%></h3>
			<b>id</b>: <%=bundleId %>
		</div>
		<div style="float:right">
			<button dojoType="dijit.form.Button" onClick="window.location='/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadBundle/bid/<%=bundleId%>';" iconClass="downloadIcon"><%= LanguageUtil.get(pageContext, "download") %></button>
		</div>
		<div class="clear"></div>
	</div>
	
	<div style="width: 350px; margin:20px 15px 20px 2px; text-align: left;" class="callOutBox2">            
    	<table class="listingTable shadowBox">
	    	<tr>
	    		<td><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Bundle_Start") %>: </b></td>
	    		<td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getBundleStart(),"MM/dd/yyyy hh:mma") %></td>
	    	
	    	</tr>
	    	<tr>
	    		<td><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Bundle_End") %>: </b></td>
	    		<td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getBundleEnd(),"MM/dd/yyyy hh:mma") %></td>
	    	
	    	</tr>
	    	<tr>
	    		<td><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Publish_Start") %>: </b></td>
	    		<td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getPublishStart(),"MM/dd/yyyy hh:mma") %></td>
	    	
	    	</tr>
	    	<tr>
	    		<td><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Publish_End") %>: </b></td>
	    		<td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getPublishEnd(),"MM/dd/yyyy hh:mma") %></td>
	    	
	    	</tr>
	    	<tr>
	    		<td><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Asset_Number") %>: </b></td>
	    		<td style="background: white"><%=currentEndpointHistory.getAssets().size() %></td>
	    	
	    	</tr>
    	</table>
	</div>
	
	<table class="listingTable shadowBox">
		<tr>		
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint") %></strong></th>			
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint_Status") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint_Status_Info") %></strong></th>
		</tr>
	
		<%if(currentEndpointHistory.getEndpointsMap().size()>0) {
			for(String groupkey : currentEndpointHistory.getEndpointsMap().keySet()) {
				Map<String, EndpointDetail> groupMap = currentEndpointHistory.getEndpointsMap().get(groupkey);
				
				for(String key : groupMap.keySet()) {
					EndpointDetail ed =  groupMap.get(key);
					String serverName = key;
					try{
						serverName = pepAPI.findEndpointById(key).getServerName().toString();
					}
					catch(Exception e){
						
					}%>
					<tr>
						<td nowrap="nowrap" valign="top"><%=serverName%></td>
						<td valign="top"><%= LanguageUtil.get(pageContext, "publisher_status_" + PublishAuditStatus.getStatusByCode(ed.getStatus()))%></td>
						<td valign="top"><%=ed.getInfo()%></td>
					</tr>	
				<%}%>
			<%}%>
		<%}else{%>	
			<tr>
				<td colspan="5" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
			</tr>	
		<%}%>
	</table>
<%} else {%>
	<div style="float: left; color: red; weight: bold;">
		<%= LanguageUtil.get(pageContext, "publisher_Audit_Detail_Error") %> 
	</div>
<%}%>

<div class="buttonRow" style="margin-top: 15px;">
	<button dojoType="dijit.form.Button" onClick="backToAuditList()" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
</div>
