<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="java.io.StringWriter"%>
<%@page import="com.dotcms.publisher.business.PublishQueueElement"%>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@page import="java.util.UUID"%>
<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PaginatedArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<script type="text/javascript">
    dojo.require("dijit.form.Button");
    dojo.require("dijit.Menu");
    dojo.require("dijit.MenuItem");

    dojo.require("dotcms.dojo.push.PushHandler");
    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

    <% Boolean enterprise = (LicenseUtil.getLevel() > 199); %>
    var enterprise = <%=enterprise%>;
    <%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();
    Boolean endPoints = UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty();
    %>
    var sendingEndpoints = <%=endPoints%>;

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

    // BEGIN https://github.com/dotCMS/dotCMS/issues/2671
    String limit = Config.getStringProperty("PUSH_PUBLISHING_PAGE_LIMIT");
    if(!UtilMethods.isSet(limit)){
    	limit="50";
    }
 	// END https://github.com/dotCMS/dotCMS/issues/2671

    String query = request.getParameter("query");
    if(!UtilMethods.isSet(query)){
    	query="*";
    	//nastyError=LanguageUtil.get(pageContext, "publisher_Query_required");
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


    		// if this is not a lucene query, lets query _all
    		if(query.indexOf(":") == -1){

    			StringWriter sw = new StringWriter();

    			String[] terms = query.split("\\s");
    			for(String x : terms){
    				if(UtilMethods.isSet(x))
    					sw.append("title:" + x + "* ");
    					sw.append("+_all:" + x + "* ");

    			}


    			query = sw.toString();
    		}




    		//contador de procesados y fallidos
    		//Add 'only not archived' condition
    		query+=" +deleted:false";






	    	if(addQueueElements){
	    		String bundeId = UUID.randomUUID().toString();

	    		List<String> identifiers = new ArrayList<String>();
	    		Long operationType = addOperationType.equals("add")? new Long(1): new Long(2);
	    		for(String item : addQueueElementsStr.split(",")){
	    			String[] value = item.split("\\$");

	    			List<PublishQueueElement> currentAssets = publisherAPI.getQueueElementsByAsset(value[0]);
	    			boolean trovato = false;
	    			for(PublishQueueElement currentAsset: currentAssets) {
	    				if(currentAsset.getOperation().intValue() == operationType.intValue())
	    					trovato = true;
	    			}

	    			if(!trovato) {
                        /*
                         Lets avoid to add multiple times the same identifier (Can happen if it is selected multiple languages of the same content)
                         as the Push Publishing API will push all the versions of a given identifier.
                         */
                        if ( !identifiers.contains( value[0] ) ) {
                            identifiers.add( value[0] );
                        }
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
	    				publisherAPI.addContentsToPublish(identifiers, bundeId, date, user);
	    			} else {
	    				publisherAPI.addContentsToUnpublish(identifiers, bundeId, date, user);
	    			}


	    		}catch(Exception b){
   					nastyError += "<br/>Unable to add selected contents";
   					errorCounter++;
   					processedCounter = 0;
   				}

    		}


    		iresults = conAPI.search(query,new Integer(limit),new Integer(offset),sortBy,user,false, PermissionAPI.PERMISSION_PUBLISH);
    		results = (PaginatedArrayList) conAPI.search(query, new Integer(limit),new Integer(offset),sortBy,user,false, PermissionAPI.PERMISSION_PUBLISH);
    		counter = ""+results.getTotalResults();
    	}
    }catch(Exception pe){
    	iresults = new ArrayList();
    	results = new PaginatedArrayList();
    	nastyError = pe.toString();
    }
  %>


<script type="text/javascript">

    function solrAddCheckUncheckAll() {
        var check = false;
        if (dijit.byId("add_all").checked) {
            check = true;
        }
        var nodes = dojo.query('.add_to_queue');
        dojo.forEach(nodes, function (node) {
            dijit.getEnclosingWidget(node).set("checked", check);
        });
    }

    function doLucenePagination(offset, limit) {
        var url = "layout=<%=layout%>&query=<%=UtilMethods.encodeURIComponent(query)%>&sort=<%=sortBy%>";
        url += "&offset=" + offset;
        url += "&limit=" + limit;
        refreshLuceneList(url);
    }

    var remotePublish = function () {

        var ids = "";
        var nodes = dojo.query('.add_to_queue');
        dojo.forEach(nodes, function (node) {
            if (dijit.getEnclosingWidget(node).checked) {
                ids += "," + dijit.getEnclosingWidget(node).value;
            }
        });

        pushHandler.showDialog( ids );
    };

</script>

<%if(UtilMethods.isSet(nastyError) && errorCounter == 0){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%} else { %>

    <%
        Boolean emptyResults = iresults == null || iresults.isEmpty();
        String disabled = emptyResults? "disabled=\"disabled\"" : "";
    %>

    <%if ( processedCounter > 0 || errorCounter > 0 ) { %>
	  	<dl>
			<dt>&nbsp;</dt><dd><span style='color:green;'><%= LanguageUtil.get(pageContext, "publisher_Processed_message") %> <%=processedCounter %></span>
			<span style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Error_Message") %> <%=errorCounter %></span></dd>
		</dl>
	<%}%>
    <%if ( UtilMethods.isSet( nastyError ) ) {%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
	<%}%>
	<table class="listingTable shadowBox">
		<tr>

			<th style="width:30px;text-align: center" align="center">
				<input dojoType="dijit.form.CheckBox" type="checkbox" <%=disabled%> name="add_all" value="all" id="add_all" onclick="solrAddCheckUncheckAll()" />
			</th>
			<th colspan="2">
                <% if (enterprise && endPoints) {%>
                    <button dojoType="dijit.form.Button" type="button" <%=disabled%> onclick="remotePublish()" iconClass="pushIcon"><%= LanguageUtil.get(pageContext, "Remote-Publish") %></button>
                <%}%>
			</th>

		</tr>
		<% for(Contentlet c : iresults) {%>
			<tr>

				<td style="width:30px;text-align: center" align="center">
					<input dojoType="dijit.form.CheckBox" type="checkbox" class="add_to_queue" name="add_to_queue" value="<%=c.getIdentifier()+"$"+c.getLanguageId() %>" id="add_to_queue_<%=c.getIdentifier()+"$"+c.getLanguageId()%>" />
				</td>
				<td width="100%" nowrap="nowrap">
					<a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getInode() %>&referer=<%=referer %>">
						<%=StringEscapeUtils.escapeHtml(c.getTitle())%>
					</a>
					<div style="float:right;color:silver">
						<a href="#" onclick="filterStructure('<%=c.getStructure().getVelocityVarName() %>')" style="color:silver"><%=c.getStructure().getName() %></a>
					</div>
				</td>
				<td nowrap="nowrap" style="width:200px"><%=UtilMethods.isSet(c.getModDate())?UtilMethods.dateToHTMLDate(c.getModDate(),"MM/dd/yyyy hh:mma"):""%></td>
			</tr>
		<%}%>
	</table>

	<table width="97%" style="margin:10px;" >
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
				<td width="33%" ><button dojoType="dijit.form.Button" onClick="doLucenePagination(<%=previous%>,<%=limit%>);return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
			<%}else{ %>
				<td  width="33%" >&nbsp;</td>
			<%} %>
				<td  width="34%"  colspan="2" align="center"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
			<%if(end < total){
				long next=(end < total?end:total);
			%>
				<td align="right" width="33%" ><button class="solr_right" dojoType="dijit.form.Button" onClick="doLucenePagination(<%=next%>,<%=limit%>);return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
			<%}else{ %>
				<td  width="33%" >&nbsp;</td>
			<%} %>
		</tr>
	</table>

    <form id="remotePublishForm">
        <input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
        <input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
        <input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
        <input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
        <input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
        <input name="iWantTo" id=iWantTo type="hidden" value="">
        <input name="whoToSend" id=whoToSend type="hidden" value="">
    </form>

<% }%>