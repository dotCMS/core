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
#settings {
    float:right;
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
	dijit.byId('closeBtn').set('disabled','disabled');
});

function hostChange() {
    var hostid=dijit.byId('hostsel').get('value');
    dijit.byId('timesel').set('value','');
    if(hostid && hostid.length>0) {
	    var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostid/"+
	      hostid;
	    dijit.byId('timesel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
    }
    else {
    	dijit.byId('timesel').set('store',new dojo.data.ItemFileReadStore());
    }
}
function timeChange() {
    var time=dijit.byId('timesel').get('value');
    var hostid=dijit.byId('hostsel').get('value');
    if(time && time.length>0 && hostid && hostid.length>0) {
    	dojo.xhr('GET',{
    		url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsing/snap/'+time+'/hostid/'+hostid,
    		handle: function() {
    			dojo.empty('iframeWrapper');
                dojo.create("iframe", {
                    "src": '/',
                    "style": "border: 0; width: 100%; height: 100%"
                }, dojo.byId('iframeWrapper'));
                dijit.byId('closeBtn').set('disabled','');
                dijit.byId('timesel').set('disabled','disabled');
                dijit.byId('hostsel').set('disabled','disabled');
                showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
    		}
    	});
    }
}
function stopBrowing() {
	dojo.xhr('GET',{
        url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/stopBrowsing',
        handle: function() {
            dojo.empty('iframeWrapper');
            dijit.byId('closeBtn').set('disabled','disabled');
            dijit.byId('timesel').set('disabled','');
            dijit.byId('hostsel').set('disabled','');
            dijit.byId('timesel').set('value','');
        }
    });
}
function showSettings() {
	var dialog = new dijit.Dialog({
        title: "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>",
        style: "width: 400px;",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/timemachine/settings.jsp"
        })
    });
    
    dialog.show();
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
                   
	                   <select id="timesel" dojoType="dijit.form.FilteringSelect" 
	                      labelAttr="pretty" searchDelay="400" searchAttr="pretty" 
	                      onChange="timeChange()">
	                   
	                   </select>
	                   
	                   <select id="hostsel" dojoType="dijit.form.FilteringSelect" 
						    store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
						    searchDelay="400" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>"
						    onchange="hostChange()"></select>
						    
	                   <button id="closeBtn" dojoType="dijit.form.Button" onClick="stopBrowing()">
	                      <%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE_SNAP")%>
	                   </button>
                   
	                   <span id="settings">
		                   <button id="settingsBtn" dojoType="dijit.form.Button" onClick="showSettings()">
		                      <%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>
		                   </button>
	                   </span>
                   </span>
            </div>
            <div id="iframeWrapper" dojoType="dijit.layout.ContentPane" region="center">
		        <%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %>
		    </div>
        </div>
    </div>
</div>

