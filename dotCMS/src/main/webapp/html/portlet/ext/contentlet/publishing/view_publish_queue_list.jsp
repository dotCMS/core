<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.dotcms.publisher.business.PublishAuditUtil"%>
<%@page import="com.dotcms.publisher.business.PublishQueueElement"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.publisher.business.DotPublisherException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="com.dotcms.publisher.bundle.business.BundleAPI"%>
<%@page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@page import="java.util.List"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotcms.publisher.business.PublishQueuePermissionFilter"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.beans.Identifier"%>
<%@ page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@ page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@ page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ page import="com.dotcms.publisher.business.PublishQueueElementTransformer" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%
	final int MAX_BUNDLE_ASSET_TO_SHOW = 20;
    ContentletAPI conAPI = APILocator.getContentletAPI();
    PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();



    PublisherAPI pubAPI = PublisherAPI.getInstance();
    String viewFilterStr = request.getParameter("viewFilter");
    Integer viewFilter = null;
    if(UtilMethods.isSet(viewFilterStr)){
    	viewFilter=Integer.valueOf(viewFilterStr);
    }


    String sortBy="entered_date desc";

    int offset = 0;
    try{offset = Integer.parseInt(request.getParameter("offset"));}catch(Exception e){}
    if(offset <0) offset=0;
    int limit = 10;
    try{limit = Integer.parseInt(request.getParameter("limit"));}catch(Exception e){}
    if(limit <0 || limit > 1000) limit=10;



    String nastyError = null;



    List<Map<String,Object>> iresults =  null;
    int counter =  0;

    boolean deleteQueueElements=false;
    boolean deleteBundleElements=false;
    String deleteQueueElementsStr = request.getParameter("delete");
    String deleteBundleElementsStr = request.getParameter("deleteBundle");
    if(UtilMethods.isSet(deleteQueueElementsStr)){
    	deleteQueueElements=true;
    }
    if(UtilMethods.isSet(deleteBundleElementsStr)){
    	deleteBundleElements=true;
    }
    String elementsToDelete=null;
    int malformedDeleteEntries = 0;

    try{
    	if(deleteQueueElements){
	    	for(String entry : deleteQueueElementsStr.split(",")){
	    		// Each entry is <assetId>$<bundleId>. Split on the LAST '$', not the first: an asset
	    		// identifier may legally contain '$' - an OSGI bundle's identifier is a jar file name
	    		// (PublisherAPIImpl:207) - whereas a bundle id never does. Splitting left-to-right
	    		// would mis-parse "my$plugin.jar$B" into asset="my", bundle="plugin.jar", delete zero
	    		// rows and reload the pane looking like success: the exact silent no-op #36861 fixes.
	    		final int separator = entry.lastIndexOf('$');
	    		final String assetToDelete = separator > 0 ? entry.substring(0, separator) : null;
	    		final String bundleOfAsset = separator > 0 ? entry.substring(separator + 1) : null;

	    		if(!UtilMethods.isSet(assetToDelete) || !UtilMethods.isSet(bundleOfAsset)){
	    			// The bundle is mandatory - deleting without it would clear the asset from every
	    			// bundle holding it. Tell the user rather than silently skipping.
	    			malformedDeleteEntries++;
	    			continue;
	    		}
	    		pubAPI.deleteElementFromPublishQueueTableAndAuditStatus(assetToDelete, bundleOfAsset);
	    	}
    	}

    	if(deleteBundleElements){
	    	for(String bundleId : deleteBundleElementsStr.split(",")){
	    		pubAPI.deleteElementsFromPublishQueueTableAndAuditStatus(bundleId);
	    	}
    	}


   		iresults =  pubAPI.getQueueBundleIds(limit, offset);
   		counter =  pubAPI.countQueueBundleIds();

    }catch(DotPublisherException e){
    	iresults = new ArrayList<Map<String,Object>>();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList<Map<String,Object>>();
    	nastyError = pe.toString();
    }

	long begin=offset;
	long end = offset+limit;
	long total = counter;
	long previous=(begin-limit);
	if(previous < 0){previous=0;}

 %>



<script type="text/javascript">

	function openContentEditor(inode, contentType) {
		var customEvent = document.createEvent("CustomEvent");
		customEvent.initCustomEvent("ng-event", false, false, {
			name: "edit-contentlet",
			data: {
				inode: inode,
				contentType: contentType
			}
		});
		document.dispatchEvent(customEvent);
	}

	dojo.require("dijit.Tooltip");



	function checkAllBundle(x){
		var chk = dijit.byId("bundle_to_delete_" + x).checked;
		 // ".b<bundleId> input" is correct and deliberate, despite the class being authored on the
		 // <input>: dijit.form.CheckBox moves the source node's classes onto its wrapper <div> and
		 // nests the real <input> inside it, so the descendant form is what actually matches.
		 // "input.b<bundleId>" matches nothing once the widgets are built - verified by test.
		 dojo.query(".b" + x  + " input").forEach(function(box){
			 dijit.byId(box.id).disabled = chk;
			 dijit.byId(box.id).setValue(chk);

		})

	}



   function doQueuePagination(offset,limit) {
		var url="layout=<%=layout%>";
		url+="&offset="+offset;
		url+="&limit="+limit;
		refreshQueueList(url);
	}

   /**
    * Returns the CheckBox widget for a node, or null when the node was never upgraded.
    *
    * dijit.getEnclosingWidget() walks UP the DOM, so for a plain <input> it returns the nearest
    * enclosing widget - the queueContent ContentPane. That has no .checked (undefined, falsy) and
    * no .disabled (undefined, so !undefined is true), which is why an un-upgraded checkbox used to
    * be skipped silently instead of throwing. See issue #36861.
    */
   function queueCheckBoxFor(node){
	   var widget = dijit.getEnclosingWidget(node);

	   return (widget && typeof widget.checked === "boolean") ? widget : null;
   }

   function deleteQueue(){
	   var url="layout=<%=layout%>&offset=<%=offset%>&limit=<%=limit%>";

		var ids="";
		var malformed=0;
		var nodes = dojo.query('.queue_to_delete');
		   dojo.forEach(nodes, function(node) {
			   var box = queueCheckBoxFor(node);
			   if(box && box.checked && !box.disabled){
				   // value is <asset>$<operation>$<bundleId>. Read the last two segments from the
				   // RIGHT and rejoin the rest: an asset identifier may legally contain '$' (an
				   // OSGI bundle's identifier is a jar file name), while the operation and bundle
				   // id never do. Taking parts[0] would truncate such an asset and send a delete
				   // that matches nothing - see issue #36861.
				   var parts = box.value.split("$");
				   if(parts.length < 3){
					   malformed++;

					   return;
				   }
				   var bundleId = parts[parts.length - 1];
				   var assetId = parts.slice(0, parts.length - 2).join("$");
				   ids+=","+assetId+"$"+bundleId;
			   }
		   });
		if(malformed > 0){
			// A checkbox whose value does not carry its bundle cannot be deleted safely - deleting
			// by asset alone would clear it from every bundle. Say so rather than dropping it.
			alert("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_delete_malformed_selection")) %>");

			return;
		}

		if(ids != ""){
			url+="&delete="+ids.substring(1);
		}

		deleteBundle(url);
   }

   function deleteBundle(url) {
	   var deletingQueueElements = url.indexOf("&delete=") > -1;
	   var ids="";
		var nodes = dojo.query('.bundle_to_delete');
		   dojo.forEach(nodes, function(node) {
			   var box = queueCheckBoxFor(node);
			   if(box && box.checked){
				   ids+=","+box.value;
			   }
		   });
		if(ids != ""){
			url+="&deleteBundle="+ids.substring(1);
		}

		if(ids == "" && !deletingQueueElements){
			// Nothing was collected. Reloading the pane here would look exactly like a successful
			// delete, which is how this failure stayed invisible for so long - tell the user.
			alert("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_delete_nothing_selected")) %>");

			return;
		}

		refreshQueueList(url);
   }
</script>

<%if(malformedDeleteEntries > 0){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_delete_malformed_selection") %></dt>
		</dl>
<%}%>

<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>

<%}else if(iresults.size() ==0){ %>
	<table class="listingTable">
		<tr>
			<th style="width:30px">&nbsp;</th>
			<th style="width:40px"><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></th>
			<th><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></th>
			<th><%= LanguageUtil.get(pageContext, "publisher_Status") %></th>
		</tr>
		<tr>
			<td colspan="14" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>

<%} else {
	// Resolve each bundle's queue elements ONCE and reuse them for both the permission check and
	// the render below. This loop used to call getQueueElementsByBundleId twice per bundle.
	final Map<String, List<PublishQueueElement>> elementsByBundle =
			new LinkedHashMap<String, List<PublishQueueElement>>();
	final Map<String, PublishQueueElement> firstElementByBundle =
			new LinkedHashMap<String, PublishQueueElement>();

	for(Map<String,Object> bundle : iresults) {
		final String currentBundleId = (String) bundle.get("bundle_id");
		final List<PublishQueueElement> elements = pubAPI.getQueueElementsByBundleId(currentBundleId);

		elementsByBundle.put(currentBundleId, elements);
		// A bundle's visibility is judged from its FIRST queue element only - preserving the
		// behaviour of the per-bundle loop this replaces, which broke after one element.
		firstElementByBundle.put(currentBundleId, elements.isEmpty() ? null : elements.get(0));
	}

	// One batched permission query instead of one per bundle (ADR-0020). A bundle missing from the
	// permitted set is simply not shown - previously a missing map entry NPE'd the whole render.
	final Set<String> permittedBundleIds =
			PublishQueuePermissionFilter.permittedBundleIds(firstElementByBundle, user);

	List<PublishQueueElement> bundleAssets = null;
	for(Map<String,Object> bundle : iresults) {

		if(permittedBundleIds.contains(bundle.get("bundle_id"))) {
		bundleAssets = elementsByBundle.get(bundle.get("bundle_id"));
		Bundle bundleObj = APILocator.getBundleAPI().getBundleById((String)bundle.get("bundle_id"));
		%>

	<table class="listingTable" style="margin-bottom:20px;">
		<tr>

			<th style="width:30px;text-align:center;">
				<input dojoType="dijit.form.CheckBox"
						type="checkbox"
						class="bundle_to_delete"
						name="bundle_to_delete"
						value="<%=bundle.get("bundle_id") %>"
						id="bundle_to_delete_<%=bundle.get("bundle_id") %>"
						onclick="checkAllBundle('<%=bundle.get("bundle_id") %>')"/>
			</th>

			<th style="width:100%">

				<%= LanguageUtil.get(pageContext, "publisher_PubUnpubDate") %>:
				<span style="color:<%=new Date().before((Date) bundle.get("publish_date")) ?"gray" : "red"%>;font-weight: normal;">
					<%=new SimpleDateFormat("MM/dd/yyyy hh:mma").format((Date) bundle.get("publish_date")) %>
				</span>


			<%--
				<%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %>:
				<span style="color:gray;font-weight: normal;">
					<%=UtilMethods.dateToHTMLDate((Date)((Map<String,Object>)bundleAssets.get(0)).get("entered_date"),"MM/dd/yyyy hh:mma") %>
				</span>
			--%>


				<div style="float:right;">
					<%= LanguageUtil.get(pageContext, "publisher_Identifier") %>: <span style="color:gray;font-weight: normal;"><%=bundle.get("bundle_id")%></span>
					<% if(bundleObj!=null) { %>
					&nbsp; <%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %>: <span style="color:gray;font-weight: normal;"><%=bundleObj.isForcePush()%></span>
					<% } %>
				</div>





			</th>


		</tr>
		<%
			final PublishQueueElementTransformer publishQueueElementTransformer = new PublishQueueElementTransformer();
			final List<Map<String, Object>> assets = publishQueueElementTransformer.transform(
					bundleAssets.stream().limit(MAX_BUNDLE_ASSET_TO_SHOW)
							.collect(Collectors.toList()));

		%>

		<%if (bundleAssets.size() > MAX_BUNDLE_ASSET_TO_SHOW){%>
		<tr>
			<td colspan="2">
				<%=LanguageUtil.get("unpublished.bundles.item.show", MAX_BUNDLE_ASSET_TO_SHOW)%> <%=bundleAssets.size()%>
			</td>
		</tr>
		<%}%>

		<%
			for(final Map<String, Object> asset : assets) {
				String errorclass="";
		%>


			<tr <%=errorclass%>>
				<td style="width:30px;text-align:center;">
					<input
							dojoType="dijit.form.CheckBox"
							type="checkbox"
							class="queue_to_delete b<%=bundle.get("bundle_id") %>"
							name="queue_to_delete"
							<%-- <asset>$<operation>$<bundleId>. deleteQueue() still reads the bare asset id as
							     split("$")[0]; the trailing bundle id is what lets the delete be scoped to the
							     bundle the row was clicked in, instead of clearing the asset from every bundle
							     that has it queued. See issue #36861. --%>
							value="<%=asset.get(PublishQueueElementTransformer.ASSET_KEY) %>$<%=asset.get(PublishQueueElementTransformer.OPERATION_KEY)  %>$<%=bundle.get("bundle_id") %>"
							<%-- The bundle id is part of the widget id because the same asset and operation
							     can be queued in more than one bundle on the same page. Without it the second
							     dijit.form.CheckBox collides in the widget registry, dojo/parser throws, and the
							     rest of the parse pass is abandoned - leaving every checkbox below it an
							     un-upgraded <input> that Delete silently skips. See issue #36861. --%>
							id="queue_to_delete_<%=asset.get(PublishQueueElementTransformer.ASSET_KEY) %>$<%=asset.get(PublishQueueElementTransformer.OPERATION_KEY)  %>$<%=bundle.get("bundle_id") %>" />
				</td>


				<td valign="top">
					<%=(asset.get(PublishQueueElementTransformer.OPERATION_KEY).equals("1")?"<span class='addIcon' style='opacity:.6'></span>":"<span class='closeIcon' style='opacity:.6'></span>")%>&nbsp;
					<%try{
						String identifier = UtilMethods.isSet(asset.get(PublishQueueElementTransformer.ASSET_KEY)) ?
                                asset.get(PublishQueueElementTransformer.ASSET_KEY).toString() : StringPool.BLANK;
						String assetType = UtilMethods.isSet(asset.get(PublishQueueElementTransformer.TYPE_KEY)) ?
                                asset.get(PublishQueueElementTransformer.TYPE_KEY).toString() : StringPool.BLANK;
						String structureName = "";
						String title = "";
						String inode = "";
						if ( assetType.equals( "contentlet" ) ) {
							final Object assetTitleObject = asset.get(PublishQueueElementTransformer.TITLE_KEY);
							title =  UtilMethods.isSet(assetTitleObject) ? assetTitleObject.toString() : StringPool.BLANK;

							final Object inodeObject = asset.get(PublishQueueElementTransformer.INODE_KEY);
							inode =  UtilMethods.isSet(inodeObject) ? inodeObject.toString() : StringPool.BLANK;

							final Object structureObject = asset.get(PublishQueueElementTransformer.CONTENT_TYPE_NAME_KEY);
							structureName =  UtilMethods.isSet(structureObject) ? structureObject.toString() : StringPool.BLANK;
							final Object contentTypeVariableObject = asset.get(PublishQueueElementTransformer.CONTENT_TYPE_VARIABLE_KEY);
							final String contentTypeVariable = UtilMethods.isSet(contentTypeVariableObject) ? contentTypeVariableObject.toString() : StringPool.BLANK;
                        %>
						    <a href="javascript:void(0)" onclick="openContentEditor('<%=inode%>', '<%=contentTypeVariable%>')">
						        <%=StringEscapeUtils.escapeHtml(title)%>
                            </a>
						<% } else {
                            title = PublishAuditUtil.getInstance().getTitle(assetType, identifier);
                        %>
							<%=StringEscapeUtils.escapeHtml(title)%>
						<% } %>

						<div style="float:right;color:silver">
							<%=structureName %>
					    </div>

					<%}catch(Exception e){nastyError=e.getMessage();%>
						<span style="color:red"><%= LanguageUtil.get(pageContext, "publisher_No_Title") %></span>
					<%} %>

    			</td>
			</tr>
		<%}%>
	</table>
<%
	}
}%>

<table width="100%">
	<tr>
		<%
		if(begin > 0){ %>
			<td width="33%" ><button dojoType="dijit.form.Button" onClick="refreshQueueList('offset=<%=previous%>&limit=<%=limit%>');return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
			<td  width="34%"  colspan="2" align="center"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
		<%if(end < total){
			long next=(end < total?end:total);
		%>
			<td align="right" width="33%" ><button style="float: right;" dojoType="dijit.form.Button" onClick="refreshQueueList('offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
	</tr>
</table>





<%} %>
