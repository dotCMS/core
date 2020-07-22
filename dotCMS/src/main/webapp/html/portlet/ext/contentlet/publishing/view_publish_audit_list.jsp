<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.dotcms.publisher.business.PublishAuditUtil"%>
<%@page import="com.dotmarketing.beans.PermissionableProxy"%>
<%@page import="com.dotcms.publisher.business.PublishQueueElement"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
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
<%@page import="com.dotmarketing.business.DotStateException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%

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



    List<PublishAuditStatus> iresults =  null;
    int counter =  0;

    try{
   		iresults =  publishAuditAPI.getAllPublishAuditStatus(new Integer(limit),new Integer(offset));
   		counter =   publishAuditAPI.countAllPublishAuditStatus().intValue();
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

   function showDetail(bundleId) {
		var dialog = new dijit.Dialog({
			id: 'bundleDetail',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Audit_Detail_Desc")%>",
	        style: "width: 700px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/view_publish_audit_detail.jsp?bundle="+bundleId
	        }),
	        onHide: function() {
	        	var dialog=this;
	        	setTimeout(function() {
	        		dialog.destroyRecursive();
	        	},200);
	        },
	        onLoad: function() {

	        }
	    });
	    dialog.show();
	    dojo.style(dialog.domNode,'top','80px');
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
			<th  nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_dialog_bundle_name") %></strong></th>
			<th style="width:100%" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "Title") %></strong></th>
			<th style="width:100px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
			<th style="width:40px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:150px" nowrap="nowrap" align="center" ><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Updated") %></strong></th>
		</tr>
		<% for(PublishAuditStatus c : iresults) {
			String errorclass="";

			//Check bundle permissions
			PermissionAPI permAPI = APILocator.getPermissionAPI();
			Map<String, String> bundleAssets = c.getStatusPojo() == null ? new HashMap<String, String>() : c.getStatusPojo().getAssets();

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
					<%=c.getBundleId().split("-")[0]%>...
				</td>
				<%--BundleName--%>
				<td valign="top" nowrap="nowrap" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">
					<% Bundle bundle = APILocator.getBundleAPI().getBundleById(c.getBundleId()); %>
                    <%if ( bundle != null && bundle.getName() != null && (!bundle.getName().equals( bundle.getId() ))) { %>
                        <%=bundle.getName()%>
                    <%}%>
				</td>
				<%--BundleTitle--%>
				<%try{ %>
					<% if(bundleAssets.keySet().size()>0){ %>
						<td valign="top" style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')">

						    <%int count=0;for(String id : bundleAssets.keySet()) { %>
							<%if(count > 0){%><br /><%} %>
							<%if(count > 2){%>...<%=bundleAssets.keySet().size()-3%> <%=LanguageUtil.get(pageContext, "publisher_audit_more_assets") %><% break;} %>

                            <%String assetType = bundleAssets.get(id); %>
							<%String assetTitle = PublishAuditUtil.getInstance().getTitle(assetType, id); %>
                            <%if (assetTitle.equals( assetType )) {%>
                                <%=assetType %>
                            <%} else {
                            	if(assetType.equals("contentlet")) {
                            		Contentlet con = PublishAuditUtil.getInstance().findContentletByIdentifier(id);
                            		try {
                            			APILocator.getHTMLPageAssetAPI().fromContentlet(con);
                            			assetType = "htmlpage"; // content is an htmlpage
                            		} catch(DotStateException e) {
                            			// not an htmlpage 
                            		}
                            	}
                            %>
                            	
                                <strong><%= assetType%></strong> : <%=StringEscapeUtils.escapeHtml(assetTitle)%>
                            <%}%>

						    <%count++;} %>
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
		<%
			}
		}%>
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
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Updated") %></strong></th>
		</tr>
		<tr>
			<td colspan="4" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%} %>

