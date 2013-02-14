<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.cache.StructureCache"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@page import="com.dotmarketing.portlets.structure.model.Field" %>
<%@page import="com.dotmarketing.util.InodeUtils" %>
<%@page import="com.dotmarketing.cache.FieldsCache" %>
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

Structure structure = StructureFactory.getDefaultStructure();
List<Structure> structures = (List<Structure>)request.getAttribute (com.dotmarketing.util.WebKeys.Structure.STRUCTURES);
if(structures == null) {
    structures=new ArrayList<Structure>();
	List<Structure> allStructures = StructureFactory.getStructures("structuretype,upper(name)", -1);
	for(Structure st : allStructures) {
		if(st.isArchived() == false) {
			for(Field field : FieldsCache.getFieldsByStructureInode(st.getInode())) {
				if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
					structures.add(st);
					break;
				}
			}
		}
	}
	request.setAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURES, structures);	
}

String structureSelected = null;
if(InodeUtils.isSet(request.getParameter("structInode"))){
    structureSelected=request.getParameter("structInode");
    structure = StructureCache.getStructureByInode(structureSelected);
}

if(!structures.contains(structure)){
    structureSelected = null;
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
	disableButtons(true); //71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&   p_l_id=a8e430e3-8010-40cf-ade1-5978e61241a8
	var currentUser="<%=user.getUserId()%>";
	var lid="<%=contentLayout%>";
	var lidBL="<%=layout.getId()%>";
	var baseUrl="/c/portal/layout?p_l_id="+lid+"&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_EXT_11_cmd=edit";
	var referrer="/c/portal/layout?p_l_id="+lidBL+"&p_p_id=EXT_BROKEN_LINKS&p_p_action=0&pageNumber="+dojo.byId('currentPage').textContent;
	dojo.empty('table_body');
	var pageSize=25;
	var page=(parseInt(dojo.byId('currentPage').textContent)-1)*pageSize;
	var id = dojo.byId('currentPage');
    if(typeof id.textContent == "undefined"){
            referrer="/c/portal/layout?p_l_id="+lidBL+"&p_p_id=EXT_BROKEN_LINKS&p_p_action=0&pageNumber="+dojo.byId('currentPage').innerText;
            page=(parseInt(dojo.byId('currentPage').innerText)-1)*pageSize;
    }
	dojo.xhr('GET',{
		url:'/DotAjaxDirector/com.dotmarketing.portlets.linkchecker.ajax.LinkCheckerAjaxAction/cmd/getBrokenLinks/offset/'+page+'/pageSize/'+pageSize+'/structInode/'+'<%=structureSelected%>',
		handleAs: 'json',
		load: function(data) {
			for(var i=0;i<data.list.length;i++) {
				var inode = data.list[i].inode;
				var action=baseUrl+"&inode="+inode+'&referer=' + encodeURIComponent(referrer+"&r="+Math.floor((Math.random()*10000)+1));
				var conTitle=data.list[i].con_title;
				var status = data.list[i].status;
				var field=data.list[i].field;
				var structure=data.list[i].structure;
				var moduser=data.list[i].user;
				var moddate=data.list[i].date;
				var link="<div><strong>"+data.list[i].url_title+"</strong></div> "+data.list[i].url;
				
				var statusRowHTML = "";
				if(status == "archived") {
					statusRowHTML = "<span class='archivedIcon'></span>";
				}
				else if(status == "live") {
					statusRowHTML = "<span class='liveIcon'></span>";					
				}
				else if(status == "working") {
					statusRowHTML = "<span class='workingIcon'></span>";
				}
				var row="<tr>"+ 
				          "<td><a href=\""+action+"\"><span class='editIcon'></span></a></td>"+
				          "<td>"+conTitle+"</td>"+
				          "<td style='text-align:center'>"+statusRowHTML+"</td>"+
				          "<td>"+field+"</td>"+
				          "<td>"+structure+"</td>"+
				          "<td>"+moduser+"</td>"+
				          "<td>"+moddate+"</td>"+
				          "<td>"+link+"</td>"+
				         "</tr>";
				dojo.place(dojo.toDom(row),'table_body');				
			}
			if(typeof id.textContent == "undefined"){
                dojo.byId('totalPages').innerText=Math.ceil(data.total/pageSize);
        	}
        	else
                dojo.byId('totalPages').textContent=Math.ceil(data.total/pageSize);
			disableButtons(false);
		},
		error: function(err) {
			console.log(err);
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
					<b><%=LanguageUtil.get(pageContext, "Structures")%></b> 
            		<select id="structuresList" name="structuresList" dojoType="dijit.form.FilteringSelect"
            			invalidMessage="<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
<%
						for (Structure st : structures) {
			                out.println("<option value=\""+st.getInode()+"\">"+st.getName()+"</option>");
						}
%>
              		</select>
                <button id="refreshBtn" type="button" dojoType="dijit.form.Button" onClick="loadTable()">
                   <span class="reindexIcon"></span>
                   <%=LanguageUtil.get(pageContext,"Refresh")%>
                </button>
                <button id="runBtn" type="button" dojoType="dijit.form.Button" onClick="runNow()">
                   <span class="linkCheckIcon"></span>
                   <%=LanguageUtil.get(pageContext,"BROKEN_LINKS_RUNNOW")%>
                </button>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
                <table id="links_table" class="listingTable" border=1>
                <thead>
                    <tr>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_ACTION")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_TITLE")%></th>
                        <th><%=LanguageUtil.get(pageContext, "BROKEN_LINKS_STATUS")%></th>
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
