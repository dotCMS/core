<%@ include file="/html/common/init.jsp" %>
<%@include file="/html/common/top_inc.jsp"%>

<script type="text/javascript">
function loadTable() {
	dojo.empty('table_body');
	dojo.xhr('GET',{
		url:'/DotAjaxDirector/com.dotmarketing.portlets.linkchecker.ajax.LinkCheckerAjaxAction/cmd/getBrokenLinks/offset/0/pageSize/50',
		handleAs: 'json',
		load: function(data) {
			console.log(data);
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
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_ACTION")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_TITLE")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_FIELD_NAME")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_OWNER")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_LEU")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_NAME")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_HOST")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_LMD")%></th>
                        <th><%=LanguageUtil.get(pageContext,"BROKEN_LINKS_PORTLET_DETAIL_TABLE_LINK")%></th>                        
                    </tr>
                </thead>
                <tbody id="table_body">
                </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%@include file="/html/common/bottom_inc.jsp"%>