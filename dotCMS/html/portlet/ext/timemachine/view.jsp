<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<style type="text/css">
#tools {
    text-align:center;
    width: 100%;
    margin: 0;
    display: block;
}
</style>

<script type="text/javascript">
dojo.require('dotcms.dojo.data.HostReadStore');
dojo.require('dojo.data.ItemFileReadStore');
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
});

function hostChange() {
    var hostid=dijit.byId('hostsel').attr('value');
    var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostid/"+
      hostid;
    dijit.byId('timesel').attr('value','');
    dijit.byId('timesel').attr('store',new dojo.data.ItemFileReadStore({url:myUrl}));
}
</script>
<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>

<div class="portlet-wrapper">
    
    <div class="subNavCrumbTrail">
        <ul id="subNavCrumbUl">        
            <li>
                <%=LanguageUtil.get(pageContext, "javax.portlet.title.TIMEMACHINE")%>
            </li>
            <li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "javax.portlet.title.TIMEMACHINE-VIEW")%></span></li>
        </ul>
        <div class="clear"></div>
    </div>
    
    <div id="timemachineMain">
        <div id="borderContainer" dojoType="dijit.layout.BorderContainer" style="width:100%;">
            <div dojoType="dijit.layout.ContentPane" region="top">
                   <span id="tools">
                   <button dojoType="dijit.form.Button">&lt;</button>
                   <button dojoType="dijit.form.Button">&gt;</button>
                   
                   <select id="timesel" dojoType="dijit.form.FilteringSelect" 
                      labelAttr="pretty" searchDelay="400" searchAttr="pretty" >
                   
                   </select>
                   
                   <select id="hostsel" dojoType="dijit.form.FilteringSelect" 
					    store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
					    searchDelay="400" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>"
					    onchange="hostChange()"></select>
                   </span>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
		        center
		    </div>
        </div>
    </div>
</div>

