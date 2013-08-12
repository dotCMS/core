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
	String hostIdentifier = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

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


var browsingTimeMachine=false;


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
	dijit.byId('closeBtn').setDisabled(true);

	toggleDatePick();
	
	var today = new Date();
	var tomorrow = new Date();
	tomorrow.setDate(today.getDate()+1);
	
	dijit.byId('fdate').constraints.min = tomorrow;
});



// pass a function pointer
dojo.addOnUnload(function(){
	
	
	stopBrowing();
	if(browsingTimeMachine){
		
		return "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-LEAVE-THIS-PAGE")%>";
	}
});



var emptyData = { "identifier" : "id", "label" : "name", "items": [{ name: '',id: '' }] };
var emptyStore = new dojo.data.ItemFileReadStore({data:emptyData});





function timeChange() {
    var time=dijit.byId('timesel').get('value');
    var langid=dijit.byId('langsel').get('value');
	console.log("time:" + time);
	console.log("langid:" + langid);
	
	
	
	// in with time and lang set
    if(time && time.length>0 && langid && langid.length>0) {
    	console.log("with time and lang set");
    	dojo.xhr('GET',{
    		url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsing/date/'
    		       +time+'/hostIdentifier/<%=hostIdentifier%>/langid/'+langid+ "/r/" + Math.floor(Math.random()*11232132132131),
    		handle: function() {
    			dojo.empty('iframeWrapper');
                dojo.create("iframe", {
                    "src": '/',
                    "style": "border: 0; width: 100%; height: 90%;margin-top:10px"
                }, dojo.byId('iframeWrapper'));
                dijit.byId('closeBtn').setDisabled(false);
                browsingTimeMachine=true;
                showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
                
    		}
    	});
    }
	
 	// with time  set
    else if(time && time.length>0 && (langid == undefined || langid.length==0)) {
    	console.log("Showing List of LANGs");
    	var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableLangForTimeMachine/hostIdentifier/<%=hostIdentifier%>/date/"+time+ "/r/" + Math.floor(Math.random()*11232132132131);
        dijit.byId('langsel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
    }
	
	// init and changing the time
    else{
    	console.log("Showing List of TMs");


        dijit.byId('timesel').setValue("");

        var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostIdentifier/<%=hostIdentifier%>/r/" + Math.floor(Math.random()*11232132132131);
        dijit.byId('timesel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));

        dijit.byId('langsel').set('store',emptyStore);
    	
    	
    }
    
    
}

function stopBrowing() {
	dojo.xhr('GET',{
        url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/stopBrowsing',
        handle: function() {
            dojo.empty('iframeWrapper');
            dijit.byId('closeBtn').setDisabled(true);

            dijit.byId('timesel').required=false;
            dijit.byId('timesel').setValue(null);
            
            dijit.byId('langsel').setValue("");
            dijit.byId('langsel').required=false;
            
            dijit.byId('fdate').setValue(null);
            dijit.byId('fdate').required=false;
            
            dijit.byId('flang').setValue("");
            dijit.byId('flang').required=false;
            
            dojo.create("div", {
                "innerHTML": '<div ><span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %>',
                "style": "padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"
            }, dojo.byId('iframeWrapper'));
            browsingTimeMachine=false;
        }
    });
}
function showSettings() {
	var dialog = new dijit.Dialog({
		id: 'settingsDialog',
        title: "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>",
        style: "width: 600px;height: 600px;",
        content: new dojox.layout.ContentPane({
        	style: "height:550px; width: 580px;",
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

    dojo.style(dialog.domNode,'top','10px');
}

function toggleDatePick() {
	stopBrowing();
	if(dojo.byId("future").checked) {
		dojo.style('pastPicker','display','none');
	    dojo.style('futurePicker','display','');
	    var fdate=dijit.byId('fdate').getValue();
	    var flang=dijit.byId('flang').getValue();
	    if(fdate != null && flang!=null && flang.length > 0){
	    	futureChange();
	    }
	}
	else {
		dojo.style('futurePicker','display','none');
		dojo.style('pastPicker','display','');
	    var time=dijit.byId('timesel').get('value');
	    var langid=dijit.byId('langsel').get('value');
	    if(time != null && langid!=null){
	    	timeChange();
	    }
	}

}

function futureChange() {
	var fdate=dijit.byId('fdate').getValue();
	console.log("fdate:" + fdate);
	if(fdate){
		var day=fdate.getDate();
		var month=fdate.getMonth()+1;
		var year=fdate.getFullYear();
		var formated=year+"-"+month+"-"+day;
		
		var flang=dijit.byId('flang').getValue();
		console.log("flang:" + flang);
		if(flang && flang.length>0){
			dojo.xhr('GET',{
		        url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsingFutureDate/date/'
		               +formated+'/hostIdentifier/<%=hostIdentifier%>/langid/'+flang+'/rand/' + Math.random(),
		        handle: function() {
		            dojo.empty('iframeWrapper');
		            dojo.create("iframe", {
		                "src": '/',
		                "style": "border: 0; width: 100%; height: 90%;margin-top:10px"
		            }, dojo.byId('iframeWrapper'));
		            dijit.byId('closeBtn').setDisabled(false);
		
		            showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
		            browsingTimeMachine=true;
		        }
		    });
		}
	}
	
}



</script>

<div class="portlet-wrapper">

    <%@ include file="/html/portlet/ext/timemachine/sub_nav.jsp" %>

    <div id="timemachineMain">

        <div id="borderContainer" style="width:100%;">
            <div style="border:1px silver solid;padding:10px;">
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
			                      onChange="timeChange()" pageSize="20">
			                   </select>
		
			                   <select id="langsel" dojoType="dijit.form.FilteringSelect"
		                          labelAttr="pretty" searchDelay="400" searchAttr="pretty"
		                          onChange="timeChange()" pageSize="20">
		                       </select>
	                       </span>
							<span id="futurePicker">
						
	                          <input onchange="futureChange()" dojoType="dijit.form.DateTextBox" type="text" name="fdate" id="fdate" isDisabledDate="return showDate(x)" constraints= "{min:2012-01-13}"/>
	                          <select id="flang" dojoType="dijit.form.FilteringSelect" onChange="futureChange()">
	                          	<option value=""></option>
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
				<div class="clear"></div>
            </div>
            <div id="iframeWrapper"  >
		       <div style="padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"><span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %></div> 
		    </div>
        </div>
    </div>
</div>

