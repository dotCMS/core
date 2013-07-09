<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@ page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@ page import="com.dotcms.publisher.business.PublishQueueElement"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@ page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.publisher.business.PublishAuditUtil"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>

<style>

.myListingTable{width:99%;border:0px solid #d0d0d0;border-collapse:collapse;margin:0 auto;font-size:11px;}
.myListingTable tr{}
.myListingTable th, .myListingTable td{padding:1px 1px;border:0px solid #d0d0d0;border-top: none;}
.myListingTable th{font-weight:bold;background:#ececec;}
.myListingTable td a{text-decoration:none;}
.myListingTable td table tr{border:none;}
.myListingTable td button, .myListingTable table.dijitSelect, .myListingTable td .dijitTextBox{font-size:12px;}

</style>
<%

	if(null!=request.getParameter("delBundle")){
		String id = request.getParameter("delBundle");
		APILocator.getBundleAPI().deleteBundle(id);
	}

	if(null!=request.getParameter("delAsset")){
		String assetId = request.getParameter("delAsset");
		String bundleId = request.getParameter("bundleId");
		APILocator.getBundleAPI().deleteAssetFromBundle(assetId, bundleId);
	}

	PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();

	String userId = user.getUserId();

	List<Bundle> bundles = APILocator.getBundleAPI().getUnsendBundles(userId);

%>

<div class="yui-g portlet-toolbar">
  <div class="yui-u first">
    <span class="sServerIcon"></span>
    <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles") %></span>
  </div>

  <div class="yui-u" style="text-align:right;">
    <button dojoType="dijit.form.Button" onClick="loadUnpushedBundles();" iconClass="resetIcon">
      <%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
    </button>
  </div>
</div>
<div style="padding-top: 5px">
			<table  class="listingTable">
				<tr style="line-height:20px; padding-bottom: 15px">
					<th style="padding-left: 10px; font-size: 12px" >
					</th>
					<th nowrap="nowrap" style="padding-left: 10px; width: 280px">
						<%= LanguageUtil.get(pageContext, "publisher_dialog_bundle_name") %>
					</th>
					<th style="padding-left: 10px; font-size: 12px" >
						<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Assets") %>
					</th>
					<th align="right" style="padding-left: 10px; width: 12px">

					</th>

				</tr>
	<%
			boolean hasBundles = false;
			for(Bundle bundle : bundles){
				hasBundles=true;%>

				<tr style="line-height:20px; padding-bottom: 15px">
					<td nowrap="nowrap" style="padding-left: 10px; width: 53px">
						<a style="cursor: pointer" onclick="deleteBundle('<%=bundle.getId()%>')" >
						<span class="deleteIcon"></span></a>&nbsp;
						<a style="cursor: pointer" onclick="goToEditBundle('<%=bundle.getId()%>')" >
						<span class="editIcon"></span></a>
					</td>
					<td style="padding-left: 10px; font-size: 12px;" >
						<%=bundle.getName()%>
					</td>
					<td align="right">
						<table class="myListingTable" >
							<%


								if(null!=request.getParameter("delEp")){
									String id = request.getParameter("delEp");
									pepAPI.deleteEndPointById(id);
								}
								List<PublishingEndPoint> endpoints = pepAPI.findSendingEndPointsByEnvironment(bundle.getId());

								PublisherAPI publisherAPI = PublisherAPI.getInstance();
								List<PublishQueueElement> assets = publisherAPI.getQueueElementsByBundleId(bundle.getId());

								boolean hasRow = false;
								for(PublishQueueElement asset : assets){
									hasRow=true;%>
								<tr >
									<td nowrap="nowrap" width="20">
										<a style="cursor: pointer" onclick="deleteAsset('<%=asset.getAsset()%>', '<%=bundle.getId()%>')" >
										<span class="deleteIcon"></span></a>
									</td>


									<td  >
										<%
										String identifier = asset.getAsset();
										String assetType = asset.getType();
										String structureName = "";
										String title = "";
										String inode = "";

				                        if ( assetType.equals( "contentlet" ) ) {
				                            //Searches and returns for a this Identifier a Contentlet using the default language
				                            Contentlet contentlet = PublishAuditUtil.getInstance().findContentletByIdentifier( identifier );
				                            title = contentlet.getTitle();
				                            inode = contentlet.getInode();
				                            structureName = contentlet.getStructure().getName();
				                        %>
										    <a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=inode %>&referer=<%=referer %>">
										        <strong><%= assetType%></strong> :<%=StringEscapeUtils.escapeHtml(title)%>
				                            </a>
										<% } else {
				                            title = PublishAuditUtil.getInstance().getTitle(assetType, identifier);
				                        %>
											<strong><%= assetType%></strong> :<%=StringEscapeUtils.escapeHtml(title)%>
										<% } %>

										<div style="float:right;color:silver">
											<%=structureName %>
									    </div>


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
					<td style="padding-left: 10px; font-size: 12px" width="120px" >
						<button dojoType="dijit.form.Button" disabled="<%=assets.size()>0?"false":"true" %>"   onClick="remotePublish('<%=bundle.getId()%>'); " iconClass="plusIcon">
							<%= LanguageUtil.get(pageContext, "Remote-Publish") %>
						</button>
<%-- 						<button dojoType="dijit.form.Button" onClick="window.location='/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadSerializedBundle/bid/<%=bundle.getId()%>';" disabled="<%=assets.size()>0?"false":"true" %>"  iconClass="downloadIcon"> --%>
<%-- 							<%= LanguageUtil.get(pageContext, "download") %> --%>
<!-- 						</button> -->
					</td>

				</tr>


		<%}%>
			</table><br>

		<%if(!hasBundles){ %>
			<table style="width: 99%; border: 1px solid #D0D0D0">
				<tr>
					<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
				</tr>
			</table>
				<%}%>




</div>



