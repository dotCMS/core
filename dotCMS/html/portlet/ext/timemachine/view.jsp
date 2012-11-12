<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.beans.Identifier"%>


<% if(LicenseUtil.getLevel()< 199){ %>
<%@ include file="/html/portlet/ext/timemachine/not_licensed.jsp" %>

<%return;} %>




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
dojo.require('dojo.data.ItemFileReadStore');
function resized() {
	var viewport = dijit.getViewport();
    var viewport_height = viewport.h;

    var  e =  dojo.byId("borderContainer");
    dojo.style(e, "height", viewport_height -175+ "px");

    //dijit.byId("borderContainer").resize();
}

dojo.ready(function(){
	dojo.connect(window,"onresize",resized);
	resized();
	dijit.byId('closeBtn').set('disabled','disabled');
	hostChange();
});

var emptyData = { "identifier" : "id", "label" : "name", "items": [{ name: '',id: '' }] };
var emptyStore = new dojo.data.ItemFileReadStore({data:emptyData});
var hostid;

function hostChange() {
    dijit.byId('timesel').required=true;
    dijit.byId('langsel').required=true;
    <%
    String hostInode = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
    Identifier hostIdentifier = APILocator.getIdentifierAPI().findFromInode(hostInode);
    %>
    hostid = "<%= hostIdentifier.getId() %>";
    dijit.byId('timesel').set('value','');
    if(hostid && hostid.length>0) {
	    var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostid/"+
	      hostid;
	    dijit.byId('timesel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
    }
    else {
    	dijit.byId('timesel').set('store',emptyStore);
    }
    dijit.byId('langsel').set('store',emptyStore);
}
function timeChange() {

    var time=dijit.byId('timesel').get('value');
    var langid=dijit.byId('langsel').get('value');

    if(hostid && hostid.length>0 && time && time.length>0) {
    	var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableLangForTimeMachine/hostid/"+
                   hostid+"/date/"+time;
        dijit.byId('langsel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
    }

    if(time && time.length>0 && langid && langid.length>0 && hostid && hostid.length>0) {
    	dojo.xhr('GET',{
    		url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsing/date/'
    		       +time+'/hostid/'+hostid+'/langid/'+langid,
    		handle: function() {
    			dojo.empty('iframeWrapper');
                dojo.create("iframe", {
                    "src": '/',
                    "style": "border: 0; width: 100%; height: 90%;margin-top:10px"
                }, dojo.byId('iframeWrapper'));
                dijit.byId('closeBtn').set('disabled','');
                dijit.byId('timesel').set('disabled','disabled');
                dijit.byId('langsel').set('disabled','disabled');
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
            dijit.byId('langsel').set('disabled','');
            dijit.byId('timesel').required=false;
            dijit.byId('langsel').required=false;
            dijit.byId('timesel').set('value','');
            dijit.byId('langsel').set('value','');
            
            dojo.create("div", {
                "innerHTML": '<div ><span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %>',
                "style": "padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"
            }, dojo.byId('iframeWrapper'));
          
            
            
            
        }
    });
}
function showSettings() {
	var dialog = new dijit.Dialog({
		id: 'settingsDialog',
        title: "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>",
        style: "width: 600px;",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/timemachine/settings.jsp"
        }),
        onHide: function() {
        	var dialog=this;
        	setTimeout(function() {
        		dialog.destroyRecursive();
        	},200);
        },
        onLoad: function() {

        }
    });

    dialog.show();

    dojo.style(dialog.domNode,'top','100px');
}
</script>

<div class="portlet-wrapper">

    <%@ include file="/html/portlet/ext/timemachine/sub_nav.jsp" %>

    <div id="timemachineMain">
        <div id="borderContainer" style="width:100%;">
            <div style="border:1px silver solid;padding:10px;position:relative;">
                   <span id="tools">

	                   <select id="timesel" dojoType="dijit.form.FilteringSelect"
	                      labelAttr="pretty" searchDelay="400" searchAttr="pretty"
	                      onChange="timeChange()">
	                   </select>

	                   <select id="langsel" dojoType="dijit.form.FilteringSelect"
                          labelAttr="pretty" searchDelay="400" searchAttr="pretty"
                          onChange="timeChange()">
                       </select>

	                   <button id="closeBtn" dojoType="dijit.form.Button" onClick="stopBrowing()">
	                      <%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE_SNAP")%>
	                   </button>

                   	<%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
	                   <span id="settings">
		                   <button id="settingsBtn" dojoType="dijit.form.Button" onClick="showSettings()">
		                      <%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>
		                   </button>
	                   </span>
	                <%} %>


                   </span>
            </div>
            <div id="iframeWrapper"  >
		       <div style="padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"><span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %></div> 
		    </div>
        </div>
    </div>
</div>

