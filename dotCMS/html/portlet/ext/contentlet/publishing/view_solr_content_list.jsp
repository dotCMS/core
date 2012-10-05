<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PaginatedArrayList"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.solr.business.DotSolrException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.solr.business.SolrAPI"%>
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
User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
ContentletAPI conAPI = APILocator.getContentletAPI();
if(user == null){
	response.setStatus(403);
	return;
}
String nastyError = "";
long processedCounter=0;
long errorCounter=0;

SolrAPI solrAPI = SolrAPI.getInstance();  
PluginAPI pluginAPI = APILocator.getPluginAPI();

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
	limit=pluginAPI.loadProperty("com.dotcms.solr","com.dotcms.solr.RESULTS_PER_PAGE");
}
String query = request.getParameter("query");
if(!UtilMethods.isSet(query)){
	query="";
	nastyError=LanguageUtil.get(pageContext, "SOLR_Query_required");
}

boolean userIsAdmin = false;
if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
	userIsAdmin=true;
}

String layout = request.getParameter("layout");
if(!UtilMethods.isSet(layout)) {
	layout = "";
}

String referer = new URLEncoder().encode("/c/portal/layout?p_l_id=" + layout + "&p_p_id=EXT_SOLR_TOOL&");
List<Contentlet> iresults =  null;
PaginatedArrayList<ContentletSearch> results =  null;
String counter =  "0";

boolean addQueueElements=false;
String addQueueElementsStr = request.getParameter("add");
if(UtilMethods.isSet(addQueueElementsStr)){
	addQueueElements=true;	
}
String addOperationType = request.getParameter("action");
if(!UtilMethods.isSet(addOperationType)){
	addOperationType="";
}
try{
	if(UtilMethods.isSet(query)){
		//contador de procesados y fallidos
		if(addQueueElements){
			if(addQueueElementsStr.equals("all")){
				iresults = conAPI.search(query,0,0,sortBy,user,false);
				for(Contentlet con : iresults){
					if(addOperationType.equals("add")){
						try{
							if(!con.isLive()){
								con = conAPI.findContentletByIdentifier(con.getIdentifier(),true,con.getLanguageId(),user, false);
							}
							solrAPI.addContentToSolr(con);
							processedCounter++;
						}catch(Exception b){
							nastyError += "<br/>"+con.getTitle()+": "+b.getMessage();
							errorCounter++;
						}
					}else if(addOperationType.equals("remove")){
						solrAPI.removeContentFromSolr(con);
						processedCounter++;
					}
				}
			}else{
				for(String item : addQueueElementsStr.split(",")){
					String[] value = item.split("_");
					if(addOperationType.equals("add")){
						try{
							Contentlet con = conAPI.findContentletByIdentifier(value[0],true,Long.parseLong(value[1]),user, false);
							solrAPI.addContentToSolr(con);
							processedCounter++;							
						}catch(Exception b){
							Contentlet con = conAPI.findContentletByIdentifier(value[0],false,Long.parseLong(value[1]),user, false);
							nastyError += "<br/>"+con.getTitle()+": "+b.getMessage();
							errorCounter++;
						}
					}else if(addOperationType.equals("remove")){
						solrAPI.removeContentFromSolr(value[0],Long.parseLong(value[1]));
						processedCounter++;
					}
				}
			}
		}
	
		iresults = conAPI.search(query,new Integer(limit),new Integer(offset),sortBy,user,false);
		results = (PaginatedArrayList) conAPI.searchIndex(query,new Integer(limit),new Integer(offset),sortBy,user,false);
		counter = ""+results.getTotalResults();
	}
}catch(DotSolrException e){
	iresults = new ArrayList();
	results = new PaginatedArrayList();
	nastyError = e.toString();
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
   
   function addToSolrQueue(action){
	   var url="layout=<%=layout%>&query=<%=UtilMethods.encodeURIComponent(query)%>&sort=<%=sortBy%>&offset=0&limit=<%=limit%>";	
		if(dijit.byId("add_all").checked){
			url+="&add=all";
		}else{
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
		}
		url+="&action="+action;
		refreshLuceneList(url);	   
   }
</script>
<%if(UtilMethods.isSet(nastyError) && errorCounter == 0){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "SOLR_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}else if(iresults.size() >0){ %>	
  <% if( processedCounter > 0 || errorCounter > 0){ %>
  	<dl>
		<dt>&nbsp;</dt><dd><span style='color:green;'><%= LanguageUtil.get(pageContext, "SOLR_Processed_message") %> <%=processedCounter %></span>
		<span style='color:red;'><%= LanguageUtil.get(pageContext, "SOLR_Error_Message") %> <%=errorCounter %></span></dd>
	</dl>	
	<% } %>								
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px"><input dojoType="dijit.form.CheckBox" type="checkbox" name="add_all" value="all" id="add_all" onclick="solrAddCheckUncheckAll()" /></th>		
			<th colspan="2"><div id="addSolrMenu"></div></th>			
		</tr>
		<% for(Contentlet c : iresults) {%>
			<tr>
				<td style="width:30px"><input dojoType="dijit.form.CheckBox" type="checkbox" class="add_to_queue" name="add_to_queue" value="<%=c.getIdentifier()+"_"+c.getLanguageId() %>" id="add_to_queue_<%=c.getIdentifier()%>" /></td>
				<td><a href="/c/portal/layout?p_l_id=<%=layout%>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getInode() %>&referer=<%=referer %>"><%=c.getTitle()%></a></td>
				<td style="width:200px"><%=UtilMethods.isSet(c.getModDate())?UtilMethods.dateToHTMLDate(c.getModDate(),"MM/dd/yyyy hh:mma"):""%></a></td>
			</tr>
		<%}%>
	</table>
	<table class="solr_listingTableNoBorder">
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
			<td style="width:130px"><button dojoType="dijit.form.Button" onClick="doLucenePagination(<%=previous%>,<%=limit%>);return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "SOLR_Previous") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
			<td class="solr_tcenter" colspan="2"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "SOLR_Of") %> <%=total%> </strong></td>
			<%if(end < total){ 
				long next=(end < total?end:total);
			%>
			<td style="width:130px"><button class="solr_right" dojoType="dijit.form.Button" onClick="doLucenePagination(<%=next%>,<%=limit%>);return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "SOLR_Next") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
		</tr>
	</table>
	<% if(UtilMethods.isSet(nastyError)){%>
	<dl>
		<dt style='color:red;'><%= LanguageUtil.get(pageContext, "SOLR_Query_Error") %> </dt>
		<dd><%=nastyError %></dd>
	</dl>
	<%} %>
	<script type="text/javascript">
	dojo.ready(function() {
	       var menu = new dijit.Menu({
	           style: "display: none;"
	       });
	       var menuItem1 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Reindex_In_Solr" )) %>",
	                       iconClass: "plusIcon",
	                       onClick: function() {
	                    	   addToSolrQueue('add');
	           }
	       });
	       menu.addChild(menuItem1);

	       var menuItem2 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Remove_From_Solr" )) %>",
	                       iconClass: "deleteIcon",
	                       onClick: function() {
	                    	   addToSolrQueue('remove');
	           }
	       });
	       menu.addChild(menuItem2);
	       
	       var button = new dijit.form.ComboButton({
	            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Reindex_In_Solr" )) %>",
	                        iconClass: "plusIcon",
	                        dropDown: menu,
	                        onClick: function() {
	                        	addToSolrQueue('add');
	            }
	        });

	      dojo.byId("addSolrMenu").appendChild(button.domNode);
	   });
	</script>
<% }else{ %>
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px">&nbsp;</th>		
			<th colspan="2"><div id="addSolrMenu"></div></th>			
		</tr>
		<tr>
			<td colspan="2" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "SOLR_No_Results") %></td>
		</tr>
	</table>
	<script type="text/javascript">
	dojo.ready(function() {
	       var menu = new dijit.Menu({
	           style: "display: none;"
	       });
	       var menuItem1 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Reindex_In_Solr" )) %>",
	                       iconClass: "plusIcon",
	                       onClick: function() {
	                    	   //addToSolrQueue('add');
	           }
	       });
	       menu.addChild(menuItem1);

	       var menuItem2 = new dijit.MenuItem({
	           label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Remove_From_Solr" )) %>",
	                       iconClass: "deleteIcon",
	                       onClick: function() {
	                    	   //addToSolrQueue('remove');
	           }
	       });
	       menu.addChild(menuItem2);
	       
	       var button = new dijit.form.ComboButton({
	            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "SOLR_Reindex_In_Solr" )) %>",
	                        iconClass: "plusIcon",
	                        dropDown: menu,
	                        onClick: function() {
	                        	//addToSolrQueue('add');
	            },
	            disabled:true
	        });

	      dojo.byId("addSolrMenu").appendChild(button.domNode);
	   });
	</script>
<%} %>