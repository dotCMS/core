<%@ include file="/html/common/init.jsp" %>
<%@include file="/html/common/top_inc.jsp"%>

<script type="text/javascript">
function loadTable() {
	
}
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
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
            </div>
        </div>
    </div>
</div>

<%@include file="/html/common/bottom_inc.jsp"%>