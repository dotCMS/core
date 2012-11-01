<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@page import="java.util.UUID"%>
<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PaginatedArrayList"%>
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
<script type="text/javascript">
   dojo.require("dijit.form.Button");
   dojo.require("dijit.Menu");
   dojo.require("dijit.MenuItem");
</script>  
<%

    ContentletAPI conAPI = APILocator.getContentletAPI();
    LanguageAPI languagesAPI = APILocator.getLanguageAPI();

    String nastyError = "";
    long processedCounter=0;
    long errorCounter=0;

    PublisherAPI publisherAPI = PublisherAPI.getInstance();
    PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();

    String sortBy = request.getParameter("sort");
    if(!UtilMethods.isSet(sortBy)){
    	sortBy="";
    }
    String offset = request.getParameter("offset");
    if(!UtilMethods.isSet(offset)){
    	offset="0";
    }
    String limit = request.getParameter("limit");
    if(!UtilMethods.isSet(limit)){
    	limit="50"; //TODO Load from properties
    }
    String query = request.getParameter("query");
    if(!UtilMethods.isSet(query)){
    	query="";
    	nastyError=LanguageUtil.get(pageContext, "publisher_Query_required");
    }



  	List<Contentlet> iresults =  null;
    PaginatedArrayList<ContentletSearch> results =  null;
    String counter =  "0";

    boolean addQueueElements=false;
    String addQueueElementsStr = request.getParameter("add");
    String publishDate = request.getParameter("publishDate");
    String publishTime = request.getParameter("publishTime");
    if(UtilMethods.isSet(addQueueElementsStr)){
    	addQueueElements=true;	
    	
        
        if(!UtilMethods.isSet(publishDate)){
        	publishDate="";
        	nastyError=LanguageUtil.get(pageContext, "publisher_Date_required");
        }
        
        if(!UtilMethods.isSet(publishTime)){
        	publishTime="";
        	nastyError=LanguageUtil.get(pageContext, "publisher_Date_required");
        }
    	
    }
    String addOperationType = request.getParameter("action");
    if(!UtilMethods.isSet(addOperationType)){
    	addOperationType="";
    }
    try{
    	if(UtilMethods.isSet(query)){
    		//contador de procesados y fallidos
    		//Add 'only not archived' condition
    		query+=" +deleted:false";
    		
	    	if(addQueueElements){
	    		String bundeId = UUID.randomUUID().toString();
		    		    		
	    		List<String> identifiers = new ArrayList<String>();
	    		Long operationType = addOperationType.equals("add")? new Long(1): new Long(2);
	    		for(String item : addQueueElementsStr.split(",")){
	    			String[] value = item.split("\\$");
	    			
	    			List<Map<String,Object>> currentAssets = publisherAPI.getQueueElementsByAsset(value[0]);
	    			boolean trovato = false;
	    			for(Map<String,Object> currentAsset: currentAssets) {
	    				if(((Long)currentAsset.get("operation")).equals(operationType))
	    					trovato = true;
	    			}
	    			
	    			if(!trovato) {
	   					identifiers.add(value[0]);
	   					processedCounter++;
	    			} else {
	    				errorCounter++;
	    				trovato = false;
	    			}
	    			
	    		}
	    		try{
	    			Date date = 
	    					new SimpleDateFormat("yyyy-MM-dd-H-m").parse(publishDate+"-"+publishTime);
	    			
	    			
	    			if(addOperationType.equals("add")){
	    				publisherAPI.addContentsToPublish(identifiers, bundeId, date);
	    			} else {
	    				publisherAPI.addContentsToUnpublish(identifiers, bundeId, date);
	    			}
	    			
	    			
	    		}catch(Exception b){
   					nastyError += "<br/>Unable to add selected contents";
   					errorCounter++;
   					processedCounter = 0;	
   				}
		    	
    		}
    		
    		
    		iresults = conAPI.search(query,new Integer(limit),new Integer(offset),sortBy,user,false);
    		results = (PaginatedArrayList) conAPI.searchIndex(query,new Integer(limit),new Integer(offset),sortBy,user,false);
    		counter = ""+results.getTotalResults();
    	}
    }catch(Exception pe){
    	iresults = new ArrayList();
    	results = new PaginatedArrayList();
    	nastyError = pe.toString();
    }
  %>
<script type="text/javascript">
 function solrAddCheckUncheckAll(){
	   var check=false;
	   if(dijit.byId("add_all").checked){
		   check=true;
	   } 
	   var nodes = dojo.query('.add_to_queue');
	   dojo.forEach(nodes, function(node) {
		    dijit.getEnclosingWidget(node).set("checked",check);
	   }); 
   }
   function doLucenePagination(offset,limit) {		
		var url="layout=<%=layout%>&query=<%=UtilMethods.encodeURIComponent(query)%>&sort=<%=sortBy%>";
		url+="&offset="+offset;
		url+="&limit="+limit;		
		refreshLuceneList(url);
	}
   
   function addToPublishQueueQueue(action){
	   var url="layout=<%=layout%>&query=<%=UtilMethods.encodeURIComponent(query)%>&sort=<%=sortBy%>&offset=0&limit=<%=limit%>";	

			var ids="";
			var nodes = dojo.query('.add_to_queue');
			   dojo.forEach(nodes, function(node) {
				   if(dijit.getEnclosingWidget(node).checked){
					   ids+=","+dijit.getEnclosingWidget(node).value; 
				   }
			   });
			if(ids != ""){   
				url+="&add="+ids.substring(1);
			}
		//}
		url+="&action="+action;
		
		if(dijit.byId('publishDate').get('value') != null) {
			var dateValue = 
					dojo.date.locale.format(dijit.byId('publishDate').get('value'),
					{datePattern: "yyyy-MM-dd", selector: "date"});
			
			url+="&publishDate="+dateValue;
		}
		
		if(dijit.byId('publishTime').get('value') != null) {
			var timeValue = 
					dojo.date.locale.format(dijit.byId('publishTime').get('value'),
					{timePattern: "H-m", selector: "time"});
			url+="&publishTime="+timeValue;
		}
		
		
		refreshLuceneList(url);	   
   }
</script>
<%if(UtilMethods.isSet(nastyError) && errorCounter == 0){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}else if(iresults.size() >0){ %>	
  	<%if( processedCounter > 0 || errorCounter > 0){ %>
	  	<dl>
			<dt>&nbsp;</dt><dd><span style='color:green;'><%= LanguageUtil.get(pageContext, "publisher_Processed_message") %> <%=processedCounter %></span>
			<span style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Error_Message") %> <%=errorCounter %></span></dd>
		</dl>	
	<%}%>		
	<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
	<%}%>						
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px">
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="add_all" value="all" id="add_all" onclick="solrAddCheckUncheckAll()" />
			</th>		
			<th colspan="2">

					<input 
					type="text" 
					dojoType="dijit.form.DateTextBox" 
					validate="return false;" 
					invalidMessage=""  
					id="publishDate"
					name="publishDate" value="now">
					&nbsp;
					<input type="text" name="publishDate" id="publishTime" value="now" style="width:100px;"
					  data-dojo-type="dijit.form.TimeTextBox"
					  onChange="dojo.byId('val').value=arguments[0].toString().replace(/.*1970\s(\S+).*/,'T$1')"
					  required="true" />
				&nbsp;
			
				<div id="addPublishQueueMenu" style="display: inline-block;"></div>
			</th>	
					
		</tr>
		<% for(Contentlet c : iresults) {%>
			<tr>
				<td style="width:30px"><input dojoType="dijit.form.CheckBox" type="checkbox" class="add_to_queue" name="add_to_queue" value="<%=c.getIdentifier()+"$"+c.getLanguageId() %>" id="add_to_queue_<%=c.getIdentifier()+"$"+c.getLanguageId()%>" /></td>
				<td><a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getInode() %>&referer=<%=referer %>"><%=c.getTitle()%></a></td>
				<td style="width:200px"><%=UtilMethods.isSet(c.getModDate())?UtilMethods.dateToHTMLDate(c.getModDate(),"MM/dd/yyyy hh:mma"):""%></a></td>
			</tr>
		<%}%>
	</table>
	<table >
		<tr>
			<%
			long begin=Long.parseLong(offset);
			long end = Long.parseLong(offset)+Long.parseLong(limit);
			long total = Long.parseLong(counter);
			if(begin > 0){ 
				long previous=(begin-Long.parseLong(limit));
				if(previous < 0){
					previous=0;					
				}
			%>
				<td style="width:130px"><button dojoType="dijit.form.Button" onClick="doLucenePagination(<%=previous%>,<%=limit%>);return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
			<%}else{ %>
				<td style="width:130px">&nbsp;</td>
			<%} %>
				<td class="solr_tcenter" colspan="2"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
			<%if(end < total){ 
				long next=(end < total?end:total);
			%>
				<td style="width:130px"><button class="solr_right" dojoType="dijit.form.Button" onClick="doLucenePagination(<%=next%>,<%=limit%>);return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
			<%}else{ %>
				<td style="width:130px">&nbsp;</td>
			<%} %>
		</tr>
	</table>
	
	<script type="text/javascript">
		dojo.ready(function() {
	       var menu = new dijit.Menu({
	           style: "display: none;"
	       });
	       var menuItem1 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_add_publish_queue" )) %>",
	                       iconClass: "plusIcon",
	                       onClick: function() {
	                    	   addToPublishQueueQueue('add');
	           }
	       });
	       menu.addChild(menuItem1);

	       var menuItem2 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_remove_publish_queue" )) %>",
	                       iconClass: "deleteIcon",
	                       onClick: function() {
	                    	   addToPublishQueueQueue('remove');
	           }
	       });
	       menu.addChild(menuItem2);
	       
	       var button = new dijit.form.ComboButton({
	            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_add_publish_queue" )) %>",
	                        iconClass: "plusIcon",
	                        dropDown: menu,
	                        onClick: function() {
	                        	addToPublishQueueQueue('add');
	            }
	        });

	      dojo.byId("addPublishQueueMenu").appendChild(button.domNode);
	   });
	</script>
<% }else{ %>
	<table class="listingTable shadowBox">
		<tr>
		<tr>
			<th style="width:30px">
				<input dojoType="dijit.form.CheckBox" disabled="disabled" type="checkbox" name="add_all" value="all" id="add_all" onclick="solrAddCheckUncheckAll()" />
			</th>		
			<th colspan="2">

					<input 
					type="text" 
					dojoType="dijit.form.DateTextBox" 
					validate="return false;" 
					invalidMessage=""  
					id="publishDate"
					name="publishDate" value="now" disabled="disabled">
					&nbsp;
					<input type="text" name="publishDate" id="publishTime" value="now"
					  data-dojo-type="dijit.form.TimeTextBox"
					  onChange="dojo.byId('val').value=arguments[0].toString().replace(/.*1970\s(\S+).*/,'T$1')"
					  required="false"  disabled="disabled" />
				&nbsp;
			
				<div id="addPublishQueueMenu" style="display: inline-block;"></div>
			</th>	
					
		</tr>
		</tr>
		<tr>
			<td colspan="33" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
	<script type="text/javascript">
	dojo.ready(function() {
	       var menu = new dijit.Menu({
	           style: "display: none;"
	       });
	       var menuItem1 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_add_publish_queue" )) %>",
	                       iconClass: "plusIcon",
	                       onClick: function() {
	                    	   addToPublishQueueQueue('add');
	           }
	       });
	       menu.addChild(menuItem1);

	       var menuItem2 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_remove_publish_queue" )) %>",
	                       iconClass: "deleteIcon",
	                       onClick: function() {
	                    	   addToPublishQueueQueue('remove');
	           }
	       });
	       menu.addChild(menuItem2);
	       
	       var button = new dijit.form.ComboButton({
	            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_add_publish_queue" )) %>",
	                        iconClass: "plusIcon",
	                        dropDown: menu,
	                        onClick: function() {
	                        	addToPublishQueueQueue('add');
	            },
	            disabled:true
	        });

	      dojo.byId("addPublishQueueMenu").appendChild(button.domNode);
	   });
	</script>
<%} %>