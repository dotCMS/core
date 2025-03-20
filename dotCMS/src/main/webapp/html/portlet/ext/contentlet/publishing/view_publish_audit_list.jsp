<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.dotmarketing.beans.PermissionableProxy"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.publisher.business.DotPublisherException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="com.dotcms.publisher.business.PublishQueueElementTransformer" %>
<%@ page import="com.dotcms.publisher.bundle.bean.Bundle" %>
<%@ page import="com.dotmarketing.exception.DotDataException" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="com.liferay.util.StringPool" %>
<%@ page import="com.dotcms.publishing.FilterDescriptor" %>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%
	final int MAX_ASSETS_TO_SHOW = 3;
    ContentletAPI conAPI = APILocator.getContentletAPI();
    PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();

    String nastyError = null;


   	int deletedCount=0;
    if(request.getParameter("deleteAudit") !=null){
    	String deleteAudit = 	request.getParameter("deleteAudit");
    	String[] deleteAuditArr = deleteAudit.split(",");
    	for(String bundleId : deleteAuditArr){
    		if(bundleId!=null && bundleId.length()>3){
    			publishAuditAPI.deletePublishAuditStatus(bundleId)	;
    			deletedCount++;
    		}

    	}



    }







    PublisherAPI pubAPI = PublisherAPI.getInstance();

    int offset = 0;
    try{offset = Integer.parseInt(request.getParameter("offset"));}catch(Exception e){}
    if(offset <0) offset=0;
    int limit = 50;
    try{limit = Integer.parseInt(request.getParameter("limit"));}catch(Exception e){}
    if(limit <0 || limit > 500) limit=50;

	final String auditFilterQuery = request.getParameter("q");

    List<PublishAuditStatus> iresults =  null;
    int counter =  0;

    try{
   		iresults =  publishAuditAPI.getPublishAuditStatus(
				   limit, offset, MAX_ASSETS_TO_SHOW, auditFilterQuery);
   		counter =   publishAuditAPI.countPublishAuditStatus(auditFilterQuery).intValue();
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }







	long begin=offset;
	long end = offset+limit;
	long total = counter;
	long previous=(begin-limit);
	if(previous < 0){previous=0;}





  %>


<script type="text/javascript">
   dojo.require("dijit.Tooltip");

   function showDetail(bundleId, event) {
	   if (!event || event.target.className !== 'view_link') {
		   var dialog = new dijit.Dialog({
			   id: 'bundleDetail',
			   title: "<%= LanguageUtil.get(pageContext, "publisher_Audit_Detail_Desc")%>",
			   style: "width: 700px; ",
			   content: new dojox.layout.ContentPane({
				   href: "/html/portlet/ext/contentlet/publishing/view_publish_audit_detail.jsp?bundle="
						   + bundleId
			   }),
			   onHide: function () {
				   var dialog = this;
				   setTimeout(function () {
					   dialog.destroyRecursive();
				   }, 200);
			   },
			   onLoad: function () {

			   }
		   });
		   dialog.show();
		   dojo.style(dialog.domNode, 'top', '80px');
	   }
	}

   function doAuditPagination(offset,limit) {
		var url="&offset="+offset;
		url+="&limit="+limit;
		refreshAuditList(url);
	}

	function checkAllAudits(){
		var chk = dijit.byId("chkBoxAllAudits").checked;

		 dojo.query(".chkBoxAudits input").forEach(function(box){

			 //dijit.byId(box.id).disabled = chk;
			 dijit.byId(box.id).setValue(chk);

		})

	}

	function deleteAuditsAPICall(url, data) {
		var xhrArgs = {
			url: url,
			postData: data,
			handleAs: 'json',
			headers : {
			'Accept' : 'application/json',
			'Content-Type' : 'application/json;charset=utf-8',
			},
			load: function(data) {},
			error: function(error){}
		};
		dojo.xhrDelete(xhrArgs);
		dijit.byId('deleteBundleActions').hide();
	}

	function deleteSelectedAudits() {
		var data =  {
			'identifiers': getSelectedAuditsIds()
		};
		var dataAsJson = dojo.toJson(data);
		deleteAuditsAPICall('/api/bundle/ids', dataAsJson);
	}

	function deleteAllAudits() {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.all.confirmation")) %>')) {
			deleteAuditsAPICall('/api/bundle/all');
		} else {
			dijit.byId('deleteBundleActions').hide();
		}
	}

	function deleteSuccessAudits() {
		deleteAuditsAPICall('/api/bundle/all/success');
	}

	function deleteFailAudits() {
		deleteAuditsAPICall('/api/bundle/all/fail');
	}

	function getSelectedAuditsIds() {
		var ids = [];
		dojo.query(".chkBoxAudits input").forEach(function(box) {
			var j= dijit.byId(box.id);
			if(j.checked){
				ids.push(j.getValue());
			}
		});
		return ids;
	}

   /**
    * Allow the user to send again failed bundles to que publisher queue job in order to try to republish them again
    */
   var retryBundles = function (bundleId) {

       var toRetry = "";
       if (bundleId == null || bundleId == undefined) {

           dojo.query(".chkBoxAudits input").forEach(function (box) {
               var j = dijit.byId(box.id);
               if (j.checked) {
                   toRetry += j.getValue() + ",";
               }
           });
       } else {
           toRetry = bundleId;
       }

       if (toRetry == "") {
           showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "publisher_retry.select.one")%>");
           return;
       }

       var forcePush = false;
       var deliveryStrategy = 1;
       
       var forcePushInput = dijit.byId("forcePush" + bundleId);
       if (forcePushInput) {
           forcePush = forcePushInput.getValue();
       }
       var deliveryStratInput = dijit.byId("deliveryStrategy" + bundleId);
       if (deliveryStratInput) {
    	   deliveryStrategy = deliveryStratInput.getValue();
       }
       
       var xhrArgs = {
           url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/retry",
           content: {
               'bundlesIds': toRetry,
               'forcePush': forcePush,
               'deliveryStrategy': deliveryStrategy
           },
           handleAs: "text",
           load: function (data) {

               var isError = false;
               if (data.indexOf("FAILURE") != -1) {
                   isError = true;
               }

               var message = data.replace(/FAILURE:/g, '');
               showDotCMSSystemMessage(message, isError);
           },
           error: function (error) {
               showDotCMSSystemMessage(error, true);
           }
       };
       dojo.xhrPost(xhrArgs);
       backToAuditList();
   };




		//dijit.byId("deleteAuditsBtn").disabled =<%=(iresults.size() ==0)%>;


	<%if(deletedCount > 0){%>
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "deleted") + " " + deletedCount  %>");
	<%} %>
</script>

<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%} %>



<%if(iresults.size() >0){ %>





	<table class="listingTable ">
		<tr>
			<th style="text-align:center;">
				<input dojoType="dijit.form.CheckBox"
					type="checkbox"
					name="chkBoxAllAudits"
					value="true"
					id="chkBoxAllAudits"
					onclick="checkAllAudits()"/>
			</th>


			<th  nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>
			<th  nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "Filter") %></strong></th>
			<th style="width:100%" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Contents") %></strong></th>
			<th style="width:100px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
			<th style="width:40px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:150px" nowrap="nowrap" align="center" ><strong><%= LanguageUtil.get(pageContext, "publisher_Last_Update") %></strong></th>
		</tr>
		<% for(PublishAuditStatus c : iresults) {
			String errorclass="";

			//Check bundle permissions
			PermissionAPI permAPI = APILocator.getPermissionAPI();
			Map<String, String> bundleAssets = c.getStatusPojo() == null || c.getStatusPojo().getAssets() == null ? new HashMap<String, String>() : c.getStatusPojo().getAssets();

			PermissionableProxy pp = new PermissionableProxy();
			for(String key : bundleAssets.keySet()) {

				String identifier = key;
				String assetType = bundleAssets.get(key);
				pp.setInode(identifier);
				pp.setIdentifier(identifier);
				pp.setType(assetType);


				break;
			}

			if(permAPI.doesUserHavePermission(pp, PermissionAPI.PERMISSION_PUBLISH, user) || bundleAssets.keySet().size()==0) {
				final String [] bundleIdParts = c.getBundleId().split("-");
				final StringBuilder shortBundleId = new StringBuilder(bundleIdParts[0]);
				for (int i = 1; i < bundleIdParts.length; i++) {
					if (shortBundleId.length() + bundleIdParts[i].length() >= 24) {
						break;
					}
					shortBundleId.append("-").append(bundleIdParts[i]);
				}

				String filterName = "";
				try {
					final Bundle bundle = APILocator.getBundleAPI().getBundleById(c.getBundleId());
					if ( UtilMethods.isSet(bundle) && UtilMethods.isSet(bundle.getFilterKey()) ) {
						final FilterDescriptor filterDescriptor =
								APILocator.getPublisherAPI().getFilterDescriptorByKey(bundle.getFilterKey());
						filterName = filterDescriptor != null ? filterDescriptor.getTitle() : "";
					}
				} catch (DotDataException e) {
					Logger.error(this, "Error getting bundle id: " + c.getBundleId(), e);
				}
		%>
			<tr <%=errorclass%>>
				<td style="width:30px;text-align:center;" valign="top">
					<input dojoType="dijit.form.CheckBox"
							type="checkbox"
							name="chkBoxAudits"
							class="chkBoxAudits"
							value="<%=c.getBundleId()%>"
							id="chkBox<%=c.getBundleId()%>"/>
				</td>

				<td valign="top" nowrap="nowrap" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">
					<%=shortBundleId.toString()%>
				</td>
				<%--BundleFilter--%>
				<td valign="top" nowrap="nowrap" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">
					<%= filterName %>
				</td>
				<%--BundleContents--%>
				<%try{ %>
					<% if(bundleAssets.keySet().size()>0){ %>
						<td valign="top" style="cursor: pointer" onclick="showDetail('<%=c.getBundleId()%>', event)">
							<table><tbody id="td_assets_<%=c.getBundleId()%>"></tbody></table>
						</td>
					<%}else{ %>

						<td valign="top" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">
							&nbsp;
						</td>
					<%} %>
				<%}catch(Exception e) {%>
					<td valign="top" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">
						-
					</td>

				<%} %>
				<%--BundleStatus--%>
			    <td valign="top" nowrap="nowrap" align="center"><%=LanguageUtil.get(pageContext, "publisher_status_" + c.getStatus().toString()) %></td>
				<%--BundleDateEntered--%>
			    <td valign="top" nowrap="nowrap"><%=UtilMethods.dateToHTMLDate(c.getCreateDate(),"MM/dd/yyyy hh:mma") %></td>
				<%--BundleDateUpdated--%>
			    <td valign="top" nowrap="nowrap" align="right"><%=DateUtil.prettyDateSince(c.getStatusUpdated()) %></td>
			</tr>
		<%}%>
	<%}%>
	</table>

<table width="97%" style="margin:10px;" >
	<tr>
		<%
		if(begin > 0){ %>
			<td width="33%" ><button dojoType="dijit.form.Button" onClick="refreshAuditList('offset=<%=previous%>&limit=<%=limit%>');return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
			<td  width="34%"  colspan="2" align="center"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
		<%if(end < total){
			long next=(end < total?end:total);
		%>
			<td align="right" width="33%" ><button style="float: right;" dojoType="dijit.form.Button" onClick="refreshAuditList('offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
	</tr>
</table>
<%
}else{
%>
	<table class="listingTable ">
		<tr>
			<th style="width:250px"><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>
			<th style="width:100px"><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Last_Update") %></strong></th>
		</tr>
		<tr>
			<td colspan="4" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%} %>

<script>
		<%
            final PublishQueueElementTransformer publishQueueElementTransformer =
                    new PublishQueueElementTransformer();

            for(PublishAuditStatus publishAuditStatus : iresults) {
                final Map<String,String> assets = publishAuditStatus.getStatusPojo().getAssets() == null ? new HashMap<String, String>() : publishAuditStatus.getStatusPojo().getAssets();

                final List<Map<String,Object>> assetsTransformed = assets.entrySet().stream()
                    .map(entry -> publishQueueElementTransformer.getMap(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

        %>
		<%if (publishAuditStatus.getTotalNumberOfAssets() > MAX_ASSETS_TO_SHOW) {%>
			addShowMoreMessage('<%=publishAuditStatus.getBundleId()%>', '<%=publishAuditStatus.getTotalNumberOfAssets()%>');
		<%}%>

		<%
			for (final Map<String,Object> asset : assetsTransformed) {
				final Object assetTypeAsObject = asset.get(PublishQueueElementTransformer.TYPE_KEY);
				final String assetType = UtilMethods.isSet(assetTypeAsObject) ?
					assetTypeAsObject.toString() : StringPool.BLANK;

				final Object assetTitleAsObject = asset.get(PublishQueueElementTransformer.TITLE_KEY);
				final String assetTitle = UtilMethods.isSet(assetTitleAsObject) ?
					assetTitleAsObject.toString() : StringPool.BLANK;
		%>
				addRow({
					title: '<%=StringEscapeUtils.escapeJavaScript(assetTitle)%>',
					type: '<%=assetType%>',
					bundleId: '<%=publishAuditStatus.getBundleId()%>',
					isHtml: <%=asset.get(PublishQueueElementTransformer.HTML_PAGE_KEY)%>
				});
			<%}%>


	<%}%>

	function addRow(rowData) {
		let tbody = document.getElementById("td_assets_" + rowData.bundleId);

		let newRow = tbody.insertRow();
		let newCell = newRow.insertCell();
		newCell.style.borderBottomWidth = "0px";

		let type = rowData.isHtml ? 'htmlpage' : rowData.type;
		let html = '<div>' +
				((rowData.title === rowData.type) ? type : '<strong>' + type + '</strong> : ' + rowData.title) + '</div>';
		newCell.innerHTML = html;
	}

	function addShowMoreMessage(bundleId, nAssets){
		let td = document.getElementById("td_assets_" + bundleId);
		let MAX_ASSETS_TO_SHOW = <%= MAX_ASSETS_TO_SHOW %>;
		let newRow = td.insertRow();
		let newCell = newRow.insertCell();
		var remaining = 0;
		if (nAssets > MAX_ASSETS_TO_SHOW) {
			remaining = nAssets - MAX_ASSETS_TO_SHOW;
		}
		newCell.innerHTML += remaining + ' <%=LanguageUtil.get(pageContext, "publisher_audit_more_assets") %>&nbsp;' +
				"<a href=\"javascript:requestAssets('" + bundleId + "', -1, " + nAssets + ")\">" +
				"<strong id='view_all_" + bundleId + "' class='view_link' style=\"text-decoration: underline;\"><%=LanguageUtil.get(pageContext, "bundles.view.all") %></strong>" +
				"</a>";
	}


	function addShowLessMessage(bundleId, numberToShow){
		let td = document.getElementById("td_assets_" + bundleId);
		let newRow = td.insertRow();
		let newCell = newRow.insertCell();
		newCell.innerHTML +=  '<%=LanguageUtil.get(pageContext, "bundles.item.all.show") %>&nbsp;' +
				"<a href=\"javascript:requestAssets('" + bundleId + "', <%= MAX_ASSETS_TO_SHOW %>, " + numberToShow + ")\">" +
				"<strong id='view_less_" + bundleId + "' class='view_link' style=\"text-decoration: underline;\"><%=LanguageUtil.get(pageContext, "bundles.item.less.show")%></strong>" +
				"</a>";
	}

	function requestAssets(bundleId, numberToShow = -1, totalAssets){
		let viewAllNode = (numberToShow === -1) ? document.getElementById('view_all_' + bundleId) : document.getElementById('view_less_' + bundleId);

		if (viewAllNode) {
			viewAllNode.innerHTML = '<%= LanguageUtil.get(pageContext, "bundles.item.loading")  %>';
		}

		fetch('/api/bundle/' + bundleId + "/assets?limit=" + numberToShow).then((response) => response.json()).then((data) => {
			let td = document.getElementById("td_assets_" + bundleId);
			while (td.firstChild) {
				td.removeChild(td.firstChild);
			}

			if (numberToShow !== -1) {
				addShowMoreMessage(bundleId, totalAssets);
				data = data.slice(0, numberToShow);
			} else {
				addShowLessMessage(bundleId, totalAssets);
			}

			data.forEach(asset => addRow({
				title: asset.<%=PublishQueueElementTransformer.TITLE_KEY%>,
				type: asset.<%=PublishQueueElementTransformer.TYPE_KEY%>,
				bundleId: bundleId,
				isHtml: asset.<%=PublishQueueElementTransformer.HTML_PAGE_KEY%>
			}));
		}).catch(function (err) {
			console.warn('Something went wrong.', err);
		});
	}
</script>