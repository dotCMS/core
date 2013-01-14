<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%
	String hostInode = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	Identifier hostIdentifier = APILocator.getIdentifierAPI().findFromInode(hostInode);
	//reset time machine
	session.removeAttribute("tm_date");
	session.removeAttribute("tm_lang");
	session.removeAttribute("tm_host");
%>

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
#tools table td.title {
 text-align: left;
}
#tools table td.input {
 text-align: left;
}
#tools table td {
 padding: 5px;
}
</style>

<script type="text/javascript">
dojo.require('dojo.data.ItemFileReadStore');
function resized() {
	var viewport = dijit.getViewport();
    var viewport_height = viewport.h;

    var  e =  dojo.byId("borderContainer");
    dojo.style(e, "height", viewport_height -175+ "px");
    var p = dojo.byId("iframeWrapper");
    dojo.style(p, "height", viewport_height -225+ "px");
    //dijit.byId("borderContainer").resize();
}

dojo.ready(function(){
	dojo.connect(window,"onresize",resized);
	resized();
	dijit.byId('closeBtn').set('disabled','disabled');
	hostChange();
	toggleDatePick();
});

var emptyData = { "identifier" : "id", "label" : "name", "items": [{ name: '',id: '' }] };
var emptyStore = new dojo.data.ItemFileReadStore({data:emptyData});
var hostid;

function hostChange() {
    dijit.byId('timesel').required=true;
    dijit.byId('langsel').required=true;
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
            if(dojo.byId("future").checked) {


            }
            else {

	            dijit.byId('timesel').required=false;
	            dijit.byId('langsel').required=false;
	            dijit.byId('timesel').set('value','');
	            dijit.byId('langsel').set('value','');

	            
            }
            
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

function toggleDatePick() {
	if(dojo.byId("future").checked) {
		dojo.style('pastPicker','display','none');
	    dojo.style('futurePicker','display','');
	}
	else {
		dojo.style('futurePicker','display','none');
		dojo.style('pastPicker','display','');
	}

}

function futureChange() {
	var fdate=dijit.byId('fdate').get('value');
	var day=fdate.getDate();
	var month=fdate.getMonth()+1;
	var year=fdate.getFullYear();
	var formated=year+"-"+month+"-"+day;
	
	var flang=dijit.byId('flang').get('value');
	
	dojo.xhr('GET',{
        url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsingFutureDate/date/'
               +formated+'/hostid/'+hostid+'/langid/'+flang,
        handle: function() {
            dojo.empty('iframeWrapper');
            dojo.create("iframe", {
                "src": '/',
                "style": "border: 0; width: 100%; height: 90%;margin-top:10px"
            }, dojo.byId('iframeWrapper'));
            dijit.byId('closeBtn').set('disabled','');

            showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
        }
    });
	
}



function buildAvailableDatesStore() {
	var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostid/<%= hostIdentifier.getId() %>" ;
	var x = {url:myUrl};		

	
	require(["dojo/request", "dojo/ready","dijit/dijit","dijit/Calendar","dojo/date"], function(request){
		  request(myUrl).then(function(data){
			  timeStore = data;
			  
				 
	             new Calendar({
	             value: new Date(),
	             isDisabledDate: function(d){
					var d = new Date(d); 
					d.setHours(0, 0, 0, 0);
					
					
					var sod = new Date(); 
					sod.setHours(0, 0, 0, 0);
					
					
					if( d.getTime() >= sod.getTime()){
						return false;	 
					}

					var eod =new Date(d); 
					eod.setHours(23, 59, 59, 999);

	                 
	                var returnMe = true;
					var gotItems = function(items, request){
						for(i=0;i< items.length;i++){
							console.log(items);
							if(Math.abs(id[0]) > sod.getTime()){
								returnMe= false;
								
								console.log( "Asdsad" );
							}
					
					}

	                	 
	                	 
	                	 
	                	 
					};
					timeStore.fetch({onComplete:  gotItems });
					console.log( "return:"+ returnMe );
	                 
	                 
	                 
	                 
	                 //console.log(eod.getTime());
                	return true;
	             }
	             }, "mycal");
			  

		  }, function(err){
		    // handle an error condition
		  }, function(evt){
		    // handle a progress event
		  });
		});
}

dojo.addOnLoad(function(){
	var today = new Date();
	var tomorrow = new Date();
	tomorrow.setDate(today.getDate()+1);
	
	dijit.byId('fdate').constraints.min = tomorrow;
});

</script>

<div class="portlet-wrapper">

    <%@ include file="/html/portlet/ext/timemachine/sub_nav.jsp" %>

    <div id="timemachineMain">

        <div id="borderContainer" style="width:100%;">
            <div style="border:1px silver solid;padding:10px;position:relative;height:70px;">

                   <table style="float:left">
                       <tr>
	                       <td class="title">
	                       		Snapshot:
	                         	<input type="radio" dojoType="dijit.form.RadioButton" name="sn" id="past" onChange="toggleDatePick()" checked='true'/><label for="past"><%= LanguageUtil.get(pageContext, "Past") %></label>&nbsp; 
	                         	<input type="radio" dojoType="dijit.form.RadioButton" name="sn" id="future" onChange="toggleDatePick()"/><label for="future"><%= LanguageUtil.get(pageContext, "Future") %></label>
	                         	&nbsp; &nbsp; 
	                       </td>
	                       <td class="input">
	                       <span id="pastPicker">
		                       
			                   <select id="timesel" dojoType="dijit.form.FilteringSelect"
			                      labelAttr="pretty" searchDelay="400" searchAttr="pretty"
			                      onChange="timeChange()">
			                   </select>
		
			                   <select id="langsel" dojoType="dijit.form.FilteringSelect"
		                          labelAttr="pretty" searchDelay="400" searchAttr="pretty"
		                          onChange="timeChange()">
		                       </select>
	                       </span>
							<span id="futurePicker">
						
	                          <input onchange="futureChange()" dojoType="dijit.form.DateTextBox" type="text" name="fdate" id="fdate" isDisabledDate="return showDate(x)" constraints= "{min:2012-01-13}"/>
	                          <select id="flang" dojoType="dijit.form.FilteringSelect" onChange="futureChange()">
	                            <% for(Language lang : APILocator.getLanguageAPI().getLanguages()) { %>
	                                <option value="<%=lang.getId()%>"><%=lang.getLanguage() %>-<%=lang.getCountry() %></option>
	                            <% } %>
	                          </select>
	                          </span>
	                          
	                       </td>
                       </tr>
                   </table>
                    <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                       <span id="settings" style="float:right">
                           <button id="settingsBtn" dojoType="dijit.form.Button" onClick="showSettings()">
                              <%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>
                           </button>
                       </span>
                    <%} %>
                    
                   <button style="float:right" id="closeBtn" dojoType="dijit.form.Button" onClick="stopBrowing()">
                      <%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE_SNAP")%>
                   </button>

            </div>
            <div id="iframeWrapper"  >
		       <div style="padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"><span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %></div> 
		    </div>
        </div>
    </div>
</div>

