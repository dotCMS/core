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
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
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
	
	function deleteAudits(){

		var deleteMe="";
		 dojo.query(".chkBoxAudits input").forEach(function(box){
			var j= dijit.byId(box.id);
			if(j.checked){
				deleteMe+=j.getValue()+",";
			}
			 
		})
		var url="&deleteAudit="+deleteMe;		
		refreshAuditList(url);
	}
	
	


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
		
		
			<th style="width:100%" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>	
			<th style="width:100px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>	
			<th style="width:40px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:150px" nowrap="nowrap" align="center" ><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Updated") %></strong></th>
		</tr>
		<% for(PublishAuditStatus c : iresults) {
			String errorclass="";
		%>
			<tr <%=errorclass%>>
				<td style="width:30px;text-align:center;">
					<input dojoType="dijit.form.CheckBox" 
							type="checkbox" 
							name="chkBoxAudits" 
							class="chkBoxAudits"
							value="<%=c.getBundleId()%>" 
							id="chkBox<%=c.getBundleId()%>"/>
				</td>	
			
			
				<td nowrap="nowrap"><a style="cursor: pointer" onclick="javascript: showDetail('<%=c.getBundleId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Audit_Detail") %>"><%=c.getBundleId()%></a></td>
			    <td nowrap="nowrap" align="center"><%=c.getStatus().toString() %></td>
			    <td nowrap="nowrap"><%=UtilMethods.dateToHTMLDate(c.getCreateDate(),"MM/dd/yyyy hh:mma") %></td>
			    <td nowrap="nowrap" align="right"><%=DateUtil.prettyDateSince(c.getStatusUpdated()) %></td>
			</tr>
		<%}%>
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
			<td align="right" width="33%" ><button class="solr_right" dojoType="dijit.form.Button" onClick="refreshAuditList('offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
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

