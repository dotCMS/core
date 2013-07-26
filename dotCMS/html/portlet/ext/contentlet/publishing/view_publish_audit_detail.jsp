<%@page import="com.dotcms.publisher.business.PublishAuditUtil"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.publisher.business.EndpointDetail"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.publisher.business.PublishAuditHistory"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@ page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.dotcms.publisher.bundle.bean.Bundle"%>

<%
    String bundleId = request.getParameter("bundle");
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    PublishAuditHistory currentEndpointHistory = null;
    String assetTitle = null;
    String assetType=null;

    Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);

    PublishAuditStatus.Status status = null;
    int statusCode = 0;
    if ( null != bundleId ) {
        PublishAuditStatus publishAuditStatus = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
        String pojo_string = publishAuditStatus.getStatusPojo().getSerialized();
        currentEndpointHistory = PublishAuditHistory.getObjectFromString( pojo_string );
        status = publishAuditStatus.getStatus();
        statusCode = status.getCode();

        if ( currentEndpointHistory != null && currentEndpointHistory.getAssets() != null && currentEndpointHistory.getAssets().size() > 0 ) {
            for ( String id : currentEndpointHistory.getAssets().keySet() ) {
                assetType = currentEndpointHistory.getAssets().get( id );
                assetTitle = PublishAuditUtil.getInstance().getTitle( assetType, id );
                break;

            }

        }
    }
%>


<script type="text/javascript">
    function backToAuditList() {
        dijit.byId('bundleDetail').hide();
    }
</script>


<% if(null!=currentEndpointHistory){%>



<table class="listingTable shadowBox">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "title") %></th>
        <td>
            <%if (assetTitle != null && assetTitle.equals( assetType )) {%>
                <%=assetType %>
            <%} else {%>
                <b><span><%=assetTitle %></span></b> (<%=assetType %>)
            <%}%>
            <div style="float:right">
                <button dojoType="dijit.form.Button" onClick="window.location='/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadBundle/bid/<%=bundleId%>';" iconClass="downloadIcon"><%= LanguageUtil.get(pageContext, "download") %></button>

                <% if ( (statusCode != 0 && status != null) && (status.equals( PublishAuditStatus.Status.FAILED_TO_PUBLISH ) || status.equals( PublishAuditStatus.Status.SUCCESS )) ) { %>
                    <button id="retryButton" dojoType="dijit.form.Button" onClick="retryBundles('<%=bundleId%>')" iconClass="repeatIcon"><%= LanguageUtil.get(pageContext, "publisher_retry") %></button>
                <%}%>
            </div>


        </td>
    </tr>
    <tr>
        <th><%= LanguageUtil.get(pageContext, "status") %>:</th>
        <td> <%= LanguageUtil.get(pageContext, "publisher_status_" + PublishAuditStatus.getStatusByCode(statusCode))%></td>
    </tr>
    <tr>
        <th><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></th>
        <td> <%=bundleId %>
        </td>
    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Bundle_Start") %>: </b></th>
        <td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getBundleStart(),"MM/dd/yyyy hh:mma") %></td>

    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Bundle_End") %>: </b></th>
        <td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getBundleEnd(),"MM/dd/yyyy hh:mma") %></td>

    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Publish_Start") %>: </b></th>
        <td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getPublishStart(),"MM/dd/yyyy hh:mma") %></td>

    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Publish_End") %>: </b></th>
        <td style="background: white"><%=UtilMethods.dateToHTMLDate(currentEndpointHistory.getPublishEnd(),"MM/dd/yyyy hh:mma") %></td>

    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %>: </b></th>
        <td style="background: white"><%if ( bundle != null ) {%><%=bundle.isForcePush() %><%}%></td>

    </tr>
    <tr>
        <th><b><%= LanguageUtil.get(pageContext, "publisher_Audit_Asset_Number") %>: </b></th>
        <td style="background: white"><%=currentEndpointHistory.getAssets().size() %></td>

    </tr>
</table>

<div>&nbsp;</div>
<table class="listingTable shadowBox">
    <tr>
        <th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint") %></strong></th>
        <th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint_Status") %></strong></th>
        <th><strong><%= LanguageUtil.get(pageContext, "publisher_Audit_Endpoint_Status_Info") %></strong></th>
    </tr>

    <%
        if(currentEndpointHistory.getEndpointsMap().size()>0) {
            for(String groupkey : currentEndpointHistory.getEndpointsMap().keySet()) {
                Map<String, EndpointDetail> groupMap = currentEndpointHistory.getEndpointsMap().get(groupkey);

                Environment env = APILocator.getEnvironmentAPI().findEnvironmentById(groupkey);

                if(env!=null) {
	                %>
				    <tr>
				        <td nowrap="nowrap" valign="top" colspan="4" bgcolor="#F7F7F7"><strong><%= LanguageUtil.get(pageContext, "publisher_Environment") %></strong>: <%=env.getName()%>
					        <div style="float:right;color:silver">
								 <%= LanguageUtil.get(pageContext, "Push-To-All") %>: <%=env.getPushToAll()%>
						    </div>
						</td>
					</tr>
				    <%


	                for(String key : groupMap.keySet()) {
	                    EndpointDetail ed =  groupMap.get(key);
	                    String serverName = key;
	                    try{
	                        serverName = pepAPI.findEndPointById(key).getServerName().toString();
	                    }
	                    catch(Exception e){

	                    }
				    %>
				    <tr>
				        <td nowrap="nowrap" valign="top"><%=serverName%></td>
				        <td valign="top"><%= LanguageUtil.get(pageContext, "publisher_status_" + PublishAuditStatus.getStatusByCode(ed.getStatus()))%></td>
				        <td valign="top"><%=ed.getInfo()%></td>
				    </tr>
				    <%}

			    }%>
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