<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@ include file="/html/common/init.jsp" %>

<script type="text/javascript">
<%
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[]{"/ext/contentlet/edit_contentlet"});
params.put("cmd", new String[] { Constants.EDIT });
String edit = PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);    

%>

function loadTable() {
	var currentUser="<%=user.getUserId()%>";
	var baseUrl="<%=edit%>";
	dojo.empty('table_body');
	dojo.xhr('GET',{
		url:'/DotAjaxDirector/com.dotmarketing.portlets.linkchecker.ajax.LinkCheckerAjaxAction/cmd/getBrokenLinks/offset/0/pageSize/50',
		handleAs: 'json',
		load: function(data) {
			for(var i=0;i<data.list.length;i++) {
				var action=baseUrl+"&inode="+data.list[i].inode+"&referer="+document.referrer;
				var conTitle=data.list[i].con_title;
				var field=data.list[i].field;
				var structure=data.list[i].structure;
				var moduser=data.list[i].user;
				var moddate=data.list[i].date;
				var link="<strong>"+data.list[i].url_title+"</strong> "+data.list[i].url;
				
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
              this is top
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
                <table border=1>
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
        </div>
    </div>
</div>
