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
<%@ page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException" %>
<%@ page import="com.dotmarketing.util.Logger" %>

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
		
		Bundle lastSelectedBundle = (com.dotcms.publisher.bundle.bean.Bundle) request.getSession().getAttribute( com.dotmarketing.util.WebKeys.SELECTED_BUNDLE ); 
		if(lastSelectedBundle!=null && lastSelectedBundle.getId().equals(id)) {
		    request.getSession().removeAttribute( com.dotmarketing.util.WebKeys.SELECTED_BUNDLE );
		}
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
		<span class="bundleIcon"></span>
		<span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles") %></span>
	</div>

	<div class="yui-u" style="text-align:right;">
		<button  dojoType="dijit.form.Button" onClick="showBundleUpload();" iconClass="uploadIcon">
						<%= LanguageUtil.get(pageContext, "publisher_upload") %>
					</button>
		<button dojoType="dijit.form.Button" onClick="loadUnpushedBundles();" iconClass="resetIcon">
			<%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
		</button>
	</div>
</div>
<div style="padding-top: 5px">

	<%
			boolean hasBundles = false;
			for(Bundle bundle : bundles){
				hasBundles=true;
				if(null!=request.getParameter("delEp")){
					String id = request.getParameter("delEp");
					pepAPI.deleteEndPointById(id);
				}
				List<PublishingEndPoint> endpoints = pepAPI.findSendingEndPointsByEnvironment(bundle.getId());

				PublisherAPI publisherAPI = PublisherAPI.getInstance();
				List<PublishQueueElement> assets = publisherAPI.getQueueElementsByBundleId(bundle.getId());%>
				<table  class="listingTable">
					<tr>
						<th width="100%" onclick="goToEditBundle('<%=bundle.getId()%>')" style="cursor:pointer">

							<b><%=bundle.getName()%></b>
						</th>
						<th align="right" nowrap="nowrap">
						
							<button dojoType="dijit.form.Button" onClick="deleteSavedBundle('<%=bundle.getId()%>')" iconClass="deleteIcon">
								<%= LanguageUtil.get(pageContext, "Delete") %>
							</button>
							
							
	                        <button dojoType="dijit.form.ComboButton" disabled="<%=assets.size()>0?"false":"true" %>"  iconClass="downloadIcon" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "download")) %>">
	                            <span><%=LanguageUtil.get(pageContext, "download") %></span>
		                        <div dojoType="dijit.Menu" id="menuItem-<%=bundle.getId()%>">
		                            <div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="downloadUnpushedBundle('<%=bundle.getId()%>','publish')"><%=LanguageUtil.get(pageContext, "download-for-Publish") %></div>
		                            <div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="downloadUnpushedBundle('<%=bundle.getId()%>','unpublish')"><%=LanguageUtil.get(pageContext, "download-for-UnPublish") %></div>
		                        </div>
	                        </button>
							
						

							<button dojoType="dijit.form.Button" disabled="<%=assets.size()>0?"false":"true" %>"   onClick="remotePublish('<%=bundle.getId()%>'); " iconClass="sServerIcon">
								<%= LanguageUtil.get(pageContext, "Remote-Publish") %>
							</button>
						</th>
					</tr>
					
					<tr>

						<td nowrap="nowrap" valign="top" colspan="2">
							
								<%boolean hasRow = false;
								for(PublishQueueElement asset : assets){
									hasRow=true;

                                    String identifier = asset.getAsset();
                                    String assetType = asset.getType();

                                    Contentlet contentlet = null;
                                    String structureName = "";
                                    String title = "";
                                    String inode = "";

                                    if ( assetType.equals( "contentlet" ) ) {

                                        //Searches and returns for a this Identifier a Contentlet using the default language
                                        try {
                                            contentlet = PublishAuditUtil.getInstance().findContentletByIdentifier( identifier );
                                        } catch ( DotContentletStateException e ) {
                                            Logger.warn( this.getClass(), "Unable to find contentlet with identifier: [" + identifier + "]", e );
                                        }
                                        if (contentlet != null) {
                                            title = contentlet.getTitle();
                                            inode = contentlet.getInode();
                                            structureName = contentlet.getStructure().getName();
                                        }
                                    } else {
                                        title = PublishAuditUtil.getInstance().getTitle(assetType, identifier);
                                        if (title.equals( assetType )) {
                                            title = "";
                                            Logger.warn( this.getClass(), "Unable to find Asset of type: [" + assetType + "] with identifier: [" + identifier + "]" );
                                        }
                                    }
                                %>
                                    <%if (contentlet != null || !title.equals( "" ) ) {%>
                                        <div style="padding:4px;margin:3px;border-bottom:1px solid #eeeeee">

                                            <span class="deleteIcon" style="margin-right:2px; cursor: pointer" onclick="deleteAsset('<%=asset.getAsset()%>', '<%=bundle.getId()%>')"></span>&nbsp;

                                            <%if ( assetType.equals( "contentlet" ) ) {%>
                                                <a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=inode %>&referer=<%=referer %>">
                                                    <strong style="text-decoration: underline;"><%=StringEscapeUtils.escapeHtml(title)%></strong>  : <%=structureName %>
                                                </a>
                                            <%} else {%>
                                                <strong><%=StringEscapeUtils.escapeHtml(title)%></strong> : <%= assetType%>
                                            <%}%>

                                        </div>
								    <%}
                                }%>
							
							<%if(!hasRow){ %>
								<div style="text-align: center"><%= LanguageUtil.get(pageContext, "publisher_bundle_is_empty") %></div>
							<%}%>

						</td>
					</tr>
				</table>
				<br>
			<%}%>


		<%if(!hasBundles){ %>
			<table  class="listingTable">
				<tr>
					<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
				</tr>
			</table>
			<br>
		<%}%>

		


</div>




