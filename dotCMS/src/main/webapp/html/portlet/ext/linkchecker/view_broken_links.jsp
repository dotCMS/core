<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@page import="com.dotmarketing.portlets.structure.model.Field" %>
<%@page import="com.dotmarketing.util.InodeUtils" %>
<%@page import="com.dotmarketing.cache.FieldsCache" %>
<%@page import="com.dotmarketing.util.PortletID"%>
<%@ include file="/html/common/init.jsp" %>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>


<% if(LicenseUtil.getLevel() <= LicenseLevel.STANDARD.level){ %>
<%@ include file="/html/portlet/ext/linkchecker/not_licensed.jsp" %>

<%return;} %>

<style type="text/css">
#tools {
    text-align:center;
    width: 100%;
    margin: 0;
    display: block;
}
#links_table td {
    padding: 8px;
}
</style>

<script type="text/javascript">
<%
String contentLayout="";
List<Layout> list=APILocator.getLayoutAPI().loadLayoutsForUser(user);
for(Layout ll : list) {
    for(String pid : ll.getPortletIds())
        if(pid.equals("content"))
            contentLayout=ll.getId();
}

int pageNumber=1;
if(request.getParameter("pageNumber")!=null) 
    pageNumber=Integer.parseInt(request.getParameter("pageNumber"));

Structure structure = StructureFactory.getDefaultStructure();
String defaultStructureInode = structure.getInode();

String structureSelected = null;
if(InodeUtils.isSet(request.getParameter("structInode"))){
    structureSelected=request.getParameter("structInode");
    structure = CacheLocator.getContentTypeCache().getStructureByInode(structureSelected);
}

if(structureSelected == null){
    structure = (Structure)StructureFactory.getDefaultStructure();
    structureSelected = structure.getInode();
}

%>

function movePage(x) {
    var cp=parseInt(dojo.byId('currentPage').textContent);
    var id = dojo.byId('currentPage');
    if(typeof id.textContent == "undefined"){
            cp=parseInt(dojo.byId('currentPage').innerText);
            dojo.byId('currentPage').innerText=cp+x;
    }
    else
            dojo.byId('currentPage').textContent=cp+x;
    loadTable();
}

function disableButtons(x) {
    dijit.byId('refreshBtn').setDisabled(x);
    dijit.byId('runBtn').setDisabled(x);
    if(x==false) {
            var cp=parseInt(dojo.byId('currentPage').textContent);
            var tp=parseInt(dojo.byId('totalPages').textContent);
            var id = dojo.byId('currentPage');
            if(typeof id.textContent == "undefined"){
                    cp=parseInt(dojo.byId('currentPage').innerText);
                    tp=parseInt(dojo.byId('totalPages').innerText);
            }
            if(cp>1)
                    dijit.byId('prevBtn').setDisabled(false);
            else
                    dijit.byId('prevBtn').setDisabled(true);
            
            if(cp<tp)
                    dijit.byId('nextBtn').setDisabled(false);
            else
                    dijit.byId('nextBtn').setDisabled(true);
    }
    else {
            dijit.byId('nextBtn').setDisabled(x);
        dijit.byId('prevBtn').setDisabled(x);
    }
}

function runNow() {
	disableButtons(true);
	dojo.xhr('POST',{
        url:'/api/v1/link-checker/_run',
        handleAs: 'json',
        headers: {'Content-Type': 'application/json'},
        postData: '{}',
        load: function() {
        	disableButtons(false);
        	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"BROKEN_LINKS_RUNNING_BG")%>");
        },
        error: function(err) {
        	console.log(err);
        	disableButtons(false);
        	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"an-unexpected-error-occurred")%>", true);
        }
	});
}

function loadTable() {
	disableButtons(true); //71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&   p_l_id=a8e430e3-8010-40cf-ade1-5978e61241a8
	var currentUser="<%=user.getUserId()%>";
	var lid="<%=contentLayout%>";
	var lidBL="<%=layout.getId()%>";
	var baseUrl="/c/portal/layout?p_l_id="+lid+"&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&p_p_mode=view&_<%=PortletID.CONTENT%>_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_<%=PortletID.CONTENT%>_cmd=edit";
	var referrer="/c/portal/layout?p_l_id="+lidBL+"&p_p_id=<%=PortletID.LINK_CHECKER%>&p_p_action=0&pageNumber="+dojo.byId('currentPage').textContent;
	dojo.empty('table_body');
	var contentType=dijit.byId('structureSelect').get('value');
	if(!contentType) {
		// nothing to list until a content type is selected
		disableButtons(false);
		return;
	}
	var pageSize=25;
	var page=(parseInt(dojo.byId('currentPage').textContent)-1)*pageSize;
	var id = dojo.byId('currentPage');
    if(typeof id.textContent == "undefined"){
            referrer="/c/portal/layout?p_l_id="+lidBL+"&p_p_id=<%=PortletID.LINK_CHECKER%>&p_p_action=0&pageNumber="+dojo.byId('currentPage').innerText;
            page=(parseInt(dojo.byId('currentPage').innerText)-1)*pageSize;
    }
	dojo.xhr('GET',{
		url:'/api/v1/link-checker/broken-links?contentType='+encodeURIComponent(contentType)+'&offset='+page+'&limit='+pageSize,
		handleAs: 'json',
		preventCache: true,
		load: function(data) {
			var rows = data.entity.list;
			var total = data.entity.total;
			var statusIcons = { archived: 'archivedIcon', live: 'liveIcon', working: 'workingIcon' };
			var tableBody = document.getElementById('table_body');

			// cells are written as text
			rows.forEach(function(row) {
				var action=baseUrl+"&inode="+encodeURIComponent(row.inode)+'&referer=' + encodeURIComponent(referrer+"&r="+Math.floor((Math.random()*10000)+1));

				var tr = document.createElement('tr');
				tr.className = 'alternate_1';
				tr.addEventListener('click', function() {
					window.location = action;
				});

				var statusCell = document.createElement('td');
				statusCell.style.textAlign = 'center';
				if(statusIcons.hasOwnProperty(row.status)) {
					var statusIcon = document.createElement('span');
					statusIcon.className = statusIcons[row.status];
					statusCell.appendChild(statusIcon);
				}
				tr.appendChild(statusCell);

				var titleCell = document.createElement('td');
				var titleLink = document.createElement('a');
				titleLink.href = action;
				titleLink.textContent = row.contentTitle;
				titleCell.appendChild(titleLink);
				tr.appendChild(titleCell);

				var fieldCell = document.createElement('td');
				fieldCell.textContent = row.field;
				tr.appendChild(fieldCell);

				var linkCell = document.createElement('td');
				if(row.linkTitle !== "Untitled") {
					var linkTitleDiv = document.createElement('div');
					var linkTitleStrong = document.createElement('strong');
					linkTitleStrong.textContent = row.linkTitle;
					linkTitleDiv.appendChild(linkTitleStrong);
					linkCell.appendChild(linkTitleDiv);
					linkCell.appendChild(document.createTextNode(" " + row.url));
				}
				else {
					linkCell.appendChild(document.createTextNode(row.url));
				}
				tr.appendChild(linkCell);

				var userCell = document.createElement('td');
				userCell.textContent = row.modUserName;
				tr.appendChild(userCell);

				var dateCell = document.createElement('td');
				dateCell.setAttribute('nowrap', 'true');
				dateCell.textContent = row.modDate;
				tr.appendChild(dateCell);

				tableBody.appendChild(tr);
			});

			if( rows.length > 0 && pageSize >= total ) {
				require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
					domStyle.set(registry.byId("prevBtn").domNode, 'display', 'none');
					domStyle.set(registry.byId("nextBtn").domNode, 'display', 'none');
				});
			}

			if(typeof id.textContent == "undefined"){
                dojo.byId('totalPages').innerText=Math.ceil(total/pageSize);
				
        	}
        	else {
                dojo.byId('totalPages').textContent=Math.ceil(total/pageSize);
				disableButtons(false);
			}
		},
		error: function(err) {
			console.log(err);
			disableButtons(false);
			showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"an-unexpected-error-occurred")%>", true);
		}
	});
}

function resized() {
    var viewport = dijit.getViewport();
    var viewport_height = viewport.h;
    
    var  e =  dojo.byId("borderContainer");
    dojo.style(e, "height", viewport_height -150+ "px");
    
    dijit.byId("borderContainer").resize();
}

dojo.ready(function(){
    dojo.connect(window,"onresize",resized);
    resized();
    dijit.byId('structureSelect').set('value', '<%=defaultStructureInode%>');
    //dijit.byId('structureSelect').textbox.value = '<%=defaultStructureInode%>'
    loadTable();
});

</script>

<div class="portlet-main">
    
    <!--
    	<div class="subNavCrumbTrail">
	        <ul id="subNavCrumbUl">        
	            <li>
	                <%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.link-checker")%>
	            </li>
	            <li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.EXT_BROKEN_LINKS_VIEW")%></span></li>
	        </ul>
	        <div class="clear"></div>
	    </div>
	-->
    
    <div id="brokenLinkMain">
        <div id="borderContainer" dojoType="dijit.layout.BorderContainer" style="width:100%;background-image:none;">
            <div dojoType="dijit.layout.ContentPane" region="top">
            	<!-- START Toolbar -->
				<div class="portlet-toolbar">
					<div class="portlet-toolbar__actions-primary">
						<!-- <%=LanguageUtil.get(pageContext, "Structure")%>:-->
						<div id="structureSelect"></div>
						<button id="refreshBtn" type="button" dojoType="dijit.form.Button" onClick="loadTable()">
		                   <span class="reindexIcon"></span>
		                   <%=LanguageUtil.get(pageContext,"Refresh")%>
		                </button>
					</div>
					<div class="portlet-toolbar__info">
						
					</div>
			    	<div class="portlet-toolbar__actions-secondary">
			    		<button id="runBtn" type="button" dojoType="dijit.form.Button" onClick="runNow()">
		                   <%=LanguageUtil.get(pageContext,"BROKEN_LINKS_RUNNOW")%>
		                </button>
			    	</div>
			   </div>
			   <!-- END Toolbar -->
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
                <table id="links_table" class="listingTable">
                <thead>
                    <tr>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_STATUS")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_TITLE")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_FIELD_NAME")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_LINK")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_USER")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_DATE")%></th>
                    </tr>
                </thead>
                <tbody id="table_body">
                </tbody>
                </table>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="bottom">
                
            </div>
        </div>
    </div>
</div>
<div class="portlet-pagination">
	<span id="tools">
		<button id="prevBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(-1)" style="float: left">
			<%= LanguageUtil.get(pageContext, "Previous")%>
		</button>
		<span id="currentPage"><%=pageNumber %></span> / <span id="totalPages"></span>
		
		<button id="nextBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(1)" style="float: right">
			<%= LanguageUtil.get(pageContext, "Next")%>
		</button>
	</span>
</div>

   <script>
	require(["dijit/form/FilteringSelect", "dojo/store/JsonRest", "dojo/data/ObjectStore", "dojo/domReady!"],
	        function(FilteringSelect, JsonRest, ObjectStore) {   
	            var jsonStore = new JsonRest({
	                target: "/api/structure/@"
	            });
	            var structureStore = new ObjectStore({objectStore: jsonStore}); 
	            // create FilteringSelect widget, populating its options from the store
	            var select = new FilteringSelect({
	                name: "structureSelect",
	                store: structureStore,
	                searchAttr: "name",
	                pageSize: 20,
	                onChange: function(val){
	                    loadTable();
	                }
	            }, "structureSelect");
	            select.startup();
        });
    </script>

