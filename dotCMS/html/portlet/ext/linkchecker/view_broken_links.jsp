<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@ include file="/html/common/init.jsp" %>

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
        if(pid.equals("EXT_11"))
            contentLayout=ll.getId();
}

int pageNumber=1;
if(request.getParameter("pageNumber")!=null) 
    pageNumber=Integer.parseInt(request.getParameter("pageNumber"));

%>
function movePage(x) {
	var cp=parseInt(dojo.byId('currentPage').textContent);
	dojo.byId('currentPage').textContent=cp+x;
	loadTable();
}

function disableButtons(x) {
	dijit.byId('refreshBtn').set('disabled',x);
	dijit.byId('runBtn').set('disabled',x);
	if(x=="") {
		var cp=parseInt(dojo.byId('currentPage').textContent);
		var tp=parseInt(dojo.byId('totalPages').textContent);
		if(cp>1)
			dijit.byId('prevBtn').set('disabled','');
		else
			dijit.byId('prevBtn').set('disabled','disabled');
		
		if(cp<tp)
			dijit.byId('nextBtn').set('disabled','');
		else
			dijit.byId('nextBtn').set('disabled','disabled');
	}
	else {
		dijit.byId('nextBtn').set('disabled',x);
	    dijit.byId('prevBtn').set('disabled',x);
	}
}

function runNow() {
	disableButtons('disabled');
	dojo.xhr('GET',{
        url:'/DotAjaxDirector/com.dotmarketing.portlets.linkchecker.ajax.LinkCheckerAjaxAction/cmd/runCheckNow',
        handleAs: 'json',
        load: function() {
        	disableButtons('');
        	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"BROKEN_LINKS_RUNNING_BG")%>");
        }
	});
}

function loadTable() {
	disableButtons('disabled'); //71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&   p_l_id=a8e430e3-8010-40cf-ade1-5978e61241a8
	var currentUser="<%=user.getUserId()%>";
	var lid="<%=contentLayout%>";
	var lidBL="<%=layout.getId()%>";
	var baseUrl="/c/portal/layout?p_l_id="+lid+"&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_EXT_11_cmd=edit";
	var referrer="/c/portal/layout?p_l_id="+lidBL+"&p_p_id=EXT_BROKEN_LINKS&p_p_action=0&pageNumber="+dojo.byId('currentPage').textContent;
	dojo.empty('table_body');
	var pageSize=10;
	var page=(parseInt(dojo.byId('currentPage').textContent)-1)*pageSize;
	dojo.xhr('GET',{
		url:'/DotAjaxDirector/com.dotmarketing.portlets.linkchecker.ajax.LinkCheckerAjaxAction/cmd/getBrokenLinks/offset/'+page+'/pageSize/'+pageSize,
		handleAs: 'json',
		load: function(data) {
			for(var i=0;i<data.list.length;i++) {
				var action=baseUrl+"&inode="+data.list[i].inode+'&referer=' + encodeURIComponent(referrer+"&r="+Math.floor((Math.random()*10000)+1));
				var conTitle=data.list[i].con_title;
				var field=data.list[i].field;
				var structure=data.list[i].structure;
				var moduser=data.list[i].user;
				var moddate=data.list[i].date;
				var link="<div><strong>"+data.list[i].url_title+"</strong></div> "+data.list[i].url;
				
				var row="<tr>"+ 
				          "<td><a href=\""+action+"\"><span class='editIcon'></span></a></td>"+
				          "<td>"+conTitle+"</td>"+
				          "<td>"+field+"</td>"+
				          "<td>"+structure+"</td>"+
				          "<td>"+moduser+"</td>"+
				          "<td>"+moddate+"</td>"+
				          "<td>"+link+"</td>"+
				         "</tr>";
				dojo.place(dojo.toDom(row),'table_body');
				dojo.byId('totalPages').textContent=Math.ceil(data.total/pageSize);
				disableButtons('');
			}
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
    loadTable();
});
</script>

<div class="portlet-wrapper">
    
    <div class="subNavCrumbTrail">
        <ul id="subNavCrumbUl">        
            <li>
                <%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_BROKEN_LINKS")%>
            </li>
            <li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_BROKEN_LINKS_VIEW")%></span></li>
        </ul>
        <div class="clear"></div>
    </div>
    
    <div id="brokenLinkMain">
        <div id="borderContainer" dojoType="dijit.layout.BorderContainer" style="width:100%;">
            <div dojoType="dijit.layout.ContentPane" region="top">
              <span id="tools">
                <button id="refreshBtn" type="button" dojoType="dijit.form.Button" onClick="loadTable()">
                   <span class="reindexIcon"></span>
                   <%=LanguageUtil.get(pageContext,"Refresh")%>
                </button>
                <button id="runBtn" type="button" dojoType="dijit.form.Button" onClick="runNow()">
                   <span class="linkCheckIcon"></span>
                   <%=LanguageUtil.get(pageContext,"BROKEN_LINKS_RUNNOW")%>
                </button>
              </span>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
                <table id="links_table" class="listingTable" border=1>
                <thead>
                    <tr>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_ACTION")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_TITLE")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_FIELD_NAME")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_STRUCTURE")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_USER")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_DATE")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_LINK")%></th>
                    </tr>
                </thead>
                <tbody id="table_body">
                </tbody>
                </table>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="bottom">
                <span id="tools">
                   <button id="prevBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(-1)">
	                   <span class="previousIcon"></span>
	               </button>
	               
	               <span id="currentPage"><%=pageNumber %></span> / <span id="totalPages"></span>
	               
	               <button id="nextBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(1)">
                       <span class="nextIcon"></span>
                   </button>
                </span>
            </div>
        </div>
    </div>
</div>
