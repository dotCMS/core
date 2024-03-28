<%@page import="com.dotcms.repackage.com.google.common.base.CaseFormat"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
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
<%@page import="com.dotcms.publisher.business.DotPublisherException"%>
<%@ page import="com.dotcms.publisher.business.PublishQueueElementTransformer" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="com.liferay.portal.model.User" %>


<%
	final int MAX_BUNDLE_ASSET_TO_SHOW = 20;

	if(null!=request.getParameter("delBundle")){
		String id = request.getParameter("delBundle");
		APILocator.getBundleAPI().deleteBundle(id);
		
		String selectedBundleKey = com.dotmarketing.util.WebKeys.SELECTED_BUNDLE + request.getSession().getAttribute("USER_ID");
		
		Bundle lastSelectedBundle = (com.dotcms.publisher.bundle.bean.Bundle) request.getSession().getAttribute( selectedBundleKey ); 
		if(lastSelectedBundle!=null && lastSelectedBundle.getId().equals(id)) {
		    request.getSession().removeAttribute( selectedBundleKey );
		}
	}

	if(null!=request.getParameter("delAsset")){
		String assetId = request.getParameter("delAsset");
		String bundleId = request.getParameter("bundleId");
		APILocator.getBundleAPI().deleteAssetFromBundleAndAuditStatus(assetId, bundleId);
	}

	PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();

	String userId = user.getUserId();

	List<Bundle> bundles = APILocator.getBundleAPI().getUnsendBundles(userId);

%>


<!-- <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles") %></span> -->
<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			
		</div>
		<div class="portlet-toolbar__info">
			
		</div>
    	<div class="portlet-toolbar__actions-secondary">
    		<button  dojoType="dijit.form.Button" onClick="showBundleUpload();" iconClass="uploadIcon">
				<%= LanguageUtil.get(pageContext, "publisher_upload") %>
			</button>
			<button dojoType="dijit.form.Button" onClick="loadUnpushedBundles();" class="dijitButtonFlat">
				<%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
			</button>
    	</div>
   </div>
   <!-- END Toolbar -->

	<%
			boolean hasBundles = false;
			for(Bundle bundle : bundles){
				hasBundles=true;
				if(null!=request.getParameter("delEp")){
					String id = request.getParameter("delEp");
					pepAPI.deleteEndPointById(id);
				}
				List<PublishingEndPoint> endpoints = pepAPI.findSendingEndPointsByEnvironment(bundle.getId());
				User bundleOwner = APILocator.getUserAPI().loadUserById(bundle.getOwner());

				PublisherAPI publisherAPI = PublisherAPI.getInstance();
				List<PublishQueueElement> assets = publisherAPI.getQueueElementsByBundleId(bundle.getId());%>
				<table id="un_publish_table_<%=bundle.getId()%>" class="listingTable" style="margin-bottom: 50px;">
					<thead>
					<tr>
						<th width="100%" onclick="goToEditBundle('<%=bundle.getId()%>')" style="cursor:pointer">
							<b><%=StringEscapeUtils.unescapeJava(bundle.getName())%></b> 
                            (<span> <%=bundle.getId() %> </span>)

                            <%if(bundle.bundleTgzExists()){%>
                                - <%=LanguageUtil.get(pageContext, "Already Generated") %> / Filter:
                                <%if(bundle.getOperation()==null || bundle.getOperation()==0){%>
                                    <%=(bundle.getFilterKey()!=null) ?bundle.getFilterKey().replace(".yml", "")  :""%>
                                 <%}else{ %>
                                    Unpublish
                                 <%}%>


                            <%} %>

						</th>
						<th align="right" nowrap="nowrap">
							<p>
								<strong>Created by: </strong>
								<%if(bundle.getOwner() != null && bundleOwner != null ){%>
								<%=bundleOwner.getFullName()%>
								<%}%>
							</p>
						</th>
						<th align="right" nowrap="nowrap">
							
							<!-- START Actions -->			
							<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
								<span></span>
								
								<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
								
									<div data-dojo-type="dijit/MenuItem" onClick="deleteSavedBundle('<%=bundle.getId()%>')">
										<%= LanguageUtil.get(pageContext, "Delete") %>
									</div>
                                    <%if(bundle.bundleTgzExists()){%>
                                        <div data-dojo-type="dijit/MenuItem" onClick="window.open('/api/bundle/_download/<%=bundle.getId()%>','_blank');">
                                            <%= LanguageUtil.get(pageContext, "Download") %>
                                        </div>
                                    <%} %>
                     
									<div data-dojo-type="dijit/MenuItem" disabled="<%= assets.isEmpty() %>" onClick="openDownloadBundleDialog('<%=bundle.getId()%>')">
                                        <%if(bundle.bundleTgzExists()){%>
                                            <%=LanguageUtil.get(pageContext, "Regenerate") %> / <%= LanguageUtil.get(pageContext, "Download") %>
                                        <%}else{ %>
										  <%=LanguageUtil.get(pageContext, "Generate") %> / <%= LanguageUtil.get(pageContext, "Download") %>
                                        <%} %>
									</div>
									<div data-dojo-type="dijit/MenuItem" disabled="<%= assets.isEmpty() %>" onClick="remotePublish('<%=bundle.getId()%>'); ">
										<%= LanguageUtil.get(pageContext, "Remote-Publish") %>
									</div>
									
								</div>
							</div>
							<!-- END Actions -->
						</th>
					</tr>
					</thead>
					<tbody></tbody>
				</table>

				<script>
					<%if (assets.size() > MAX_BUNDLE_ASSET_TO_SHOW) {%>
						addShowMoreRow('<%=bundle.getId()%>', '<%=assets.size()%>');
					<%}%>

					<%
						final PublishQueueElementTransformer publishQueueElementTransformer =
								new PublishQueueElementTransformer();

						final List<Map<String, Object>> assetsTransformed = publishQueueElementTransformer
								.transform(assets.stream()
									.limit(MAX_BUNDLE_ASSET_TO_SHOW)
									.collect(Collectors.toList())
								);

						for(Map<String, Object> asset : assetsTransformed){

							final String title = UtilMethods.isSet(asset.get(PublishQueueElementTransformer.TITLE_KEY)) ?
								StringEscapeUtils.escapeJavaScript(asset.get(PublishQueueElementTransformer.TITLE_KEY).toString()) : StringPool.BLANK;

							if (!title.equals( "" ) ) {%>

								addRow({
									title:'<%= title %>',
									inode:'<%=asset.get(PublishQueueElementTransformer.INODE_KEY)%>',
									type:'<%=asset.get(PublishQueueElementTransformer.TYPE_KEY)%>',
									content_type_name:'<%= asset.get(PublishQueueElementTransformer.CONTENT_TYPE_NAME_KEY) %>',
									language_code:'<%= asset.get(PublishQueueElementTransformer.LANGUAGE_CODE_KEY)  %>',
									country_code:'<%= asset.get(PublishQueueElementTransformer.COUNTRY_CODE_KEY) %>',
									asset:'<%= asset.get(PublishQueueElementTransformer.ASSET_KEY) %>'
								}, '<%=bundle.getId()%>');
							<%}
						}
					%>

					<%if (assetsTransformed.isEmpty()){%>
						addEmptyRow();
					<%}%>

				</script>
		<%}%>

		<%if(!hasBundles){ %>
			<table  class="listingTable">
				<tr>
					<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
				</tr>
			</table>
			<br>
		<%}%>


<script>
	function addRow(data, bundleId){
		let tbodyRef = document.getElementById('un_publish_table_' + bundleId).getElementsByTagName('tbody')[0];
		let newRow = tbodyRef.insertRow();
		let newCell = newRow.insertCell();
		newCell.colSpan = 2;

		var content = "<span class=\"deleteIcon\" style=\"margin-right:2px; cursor: pointer\" onclick=\"deleteAsset('" + data.asset + "','" + bundleId + "')\"/></span>&nbsp;";

		if (data.type === "contentlet" ) {
			content += "<a href=\"/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&p_p_mode=view&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/edit_contentlet&_content_cmd=edit&inode=" + data.inode + "&referer=<%=referer %>\">" +
					"<strong style=\"text-decoration: underline;\">" + data.title + "</strong>  : " + data.content_type_name +
					"</a>";
		} else if (data.type === "language" ) {
			content += "<a href=\"/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=<%=PortletID.LANGUAGES%>&p_p_action=1&p_p_state=maximized&p_p_mode=view&_<%=PortletID.LANGUAGES%>_struts_action=/ext/languages_manager/edit_language&_<%=PortletID.LANGUAGES%>_id=1&_<%=PortletID.LANGUAGES%> _cmd=edit&referer=<%=referer %>\">" +
					"<img src=\"/html/images/languages/" + data.language_code + "_" + data.country_code + ".gif\" border=\"0\" />&nbsp;" +
					"<strong style=\"text-decoration: underline;\">" + data.title + "</strong>  : " + (data.content_type_name === 'Page Asset' ? 'Page' : data.content_type_name) +
					"</a>";
		} else {
			content += "<strong>" + data.title + "</strong> : " + data.type;
		}
		
		newCell.innerHTML = content;
	}

	function addShowMoreRow(bundleId, nAssets){
		let tbodyRef = document.getElementById('un_publish_table_' + bundleId).getElementsByTagName('tbody')[0];
		let newRow = tbodyRef.insertRow();
		let newCell = newRow.insertCell();
		newCell.colSpan = 2;

		newCell.innerHTML = "<%=LanguageUtil.get(pageContext, "unpublished.bundles.item.show", MAX_BUNDLE_ASSET_TO_SHOW) %> " + nAssets + "&nbsp;" +
				"<a href=\"javascript:requestAssets('" + bundleId + "')\">" +
				"<strong id='view_all_" + bundleId + "' style=\"text-decoration: underline;\"><%=LanguageUtil.get(pageContext, "bundles.view.all") %></strong>" +
				"</a>";
	}

	function addShowLessRow(bundleId){
		let tbodyRef = document.getElementById('un_publish_table_' + bundleId).getElementsByTagName('tbody')[0];
		let newRow = tbodyRef.insertRow();
		let newCell = newRow.insertCell();
		newCell.colSpan = 2;

		newCell.innerHTML = "<%=LanguageUtil.get(pageContext, "bundles.item.all.show")%>&nbsp;" +
				"<a href=\"javascript:requestAssets('" + bundleId + "', '<%=MAX_BUNDLE_ASSET_TO_SHOW%>')\">" +
				"<strong id='view_less_" + bundleId + "' style=\"text-decoration: underline;\"><%=LanguageUtil.get(pageContext, "bundles.item.less.show")%></strong>" +
				"</a>";
	}

	function addEmptyRow(){
		let tbodyRef = document.getElementsByClassName('listingTable')[0].getElementsByTagName('tbody')[0];
		let newRow = tbodyRef.insertRow();
		let newCell = newRow.insertCell();

		newCell.innerHTML = "<div style=\"text-align: center\"><%= LanguageUtil.get(pageContext, "publisher_bundle_is_empty") %></div>";
	}

	function requestAssets(bundleId, numberToShow = -1){

		let viewAllNode = (numberToShow === -1) ? document.getElementById('view_all_' + bundleId) : document.getElementById('view_less_' + bundleId);

		if (viewAllNode) {
			viewAllNode.innerHTML = '<%= LanguageUtil.get(pageContext, "bundles.item.loading")  %>';
		}

		fetch('/api/bundle/' + bundleId + "/assets").then((response) => response.json()).then((data) => {
			let tbodyRef = document.getElementById('un_publish_table_' + bundleId).getElementsByTagName('tbody')[0];
			while (tbodyRef.firstChild) {
				tbodyRef.removeChild(tbodyRef.firstChild);
			}



			if (numberToShow !== -1) {
				addShowMoreRow(bundleId, data.length);
				data = data.slice(0, numberToShow);
			} else {
				addShowLessRow(bundleId);
			}

			data.forEach(asset => addRow(asset, bundleId));
		}).catch(function (err) {
			console.warn('Something went wrong.', err);
		});
	}
</script>