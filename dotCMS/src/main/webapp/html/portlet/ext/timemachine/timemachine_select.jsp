<%@page import="com.dotmarketing.util.PageMode"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
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
    
    
    PageMode.setPageMode(request, PageMode.LIVE);
    
    
%>



    <%@ include file="/html/portlet/ext/timemachine/sub_nav.jsp" %>

    <% if(LicenseUtil.getLevel() <= LicenseLevel.STANDARD.level){ %>
    <%@ include file="/html/portlet/ext/timemachine/not_licensed.jsp" %>

    <%return;} %>





<script type="text/javascript">


var browsingTimeMachine=false;


dojo.require('dojo.data.ItemFileReadStore');
dojo.require('dijit.form.TimeTextBox');

/*
 * 
function resized() {
    var viewport = dijit.getViewport();
    var viewport_height = viewport.h;

    var  e =  dojo.byId("borderContainer");
    dojo.style(e, "height", viewport_height -175+ "px");
    var p = dojo.byId("iframeWrapper");
    dojo.style(p, "height", viewport_height -225+ "px");
    //dijit.byId("borderContainer").resize();
    
   
} */
const addZero = function(i) {
    if (i < 10) {
      i = "0" + i;
    }
    return i;
  }


const TODAY = new Date(new Date().setHours(0,0,0,0));
const NOW = new Date();

NOW.setMinutes (NOW.getMinutes() + 30);
NOW.setMinutes (0);







dojo.ready(function(){

    dijit.byId('closeBtn').setDisabled(true);

    require(["dijit/form/DateTextBox", "dojo/domReady!"], function(DateTextBox){

        new DateTextBox({name: "fdate", value: new Date(),
            constraints: {
                min: TODAY
            },
            onChange:futureChange,
            value: TODAY,
        }, "fdate").startup();
    });
    
    
    require(["dijit/form/TimeTextBox", "dojo/domReady!"], function(TimeTextBox){
        const minTime= addZero(TODAY.getHours()) + ":" + addZero(TODAY.getMinutes()) ;
       
        new TimeTextBox({name: "ftime", value: new Date(),
            constraints: {
                timePattern: 'HH:mm',
                clickableIncrement: 'T00:15:00',
                visibleIncrement: 'T00:15'
            },
            onChange:futureChange,
            value: NOW,
        }, "ftime").startup();
    });
    


    
    toggleDatePick();
});

function myConfirmation() {
    if(browsingTimeMachine){
        stopBrowing();
        return "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>";
    }
}

window.onbeforeunload = myConfirmation;

var emptyData = { "identifier" : "id", "label" : "name", "items": [{ name: '',id: '' }] };
var emptyStore = new dojo.data.ItemFileReadStore({data:emptyData});

function timeChange() {
    var time=dijit.byId('timesel').get('value');
    var langid=dijit.byId('langsel').get('value');
    
    // in with time and lang set
    if(time && time.length>0 && langid && langid.length>0) {
        dojo.xhr('GET',{
            url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsing/date/'
                   +time+'/hostIdentifier/<%=hostIdentifier%>/langid/'+langid+ "/r/" + Math.floor(Math.random()*11232132132131),
            handle: function() {
                dojo.empty('iframeWrapper');
                dojo.create("iframe", {
                    "src": '/',
                    "style": "border: 0; width: 100%; height: 100%;"
                }, dojo.byId('iframeWrapper'));
                dijit.byId('closeBtn').setDisabled(false);
                browsingTimeMachine=true;
                showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
                
            }
        });
    }
    
     // with time  set
    else if(time && time.length>0 && (langid == undefined || langid.length==0)) {
        var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableLangForTimeMachine/hostIdentifier/<%=hostIdentifier%>/date/"+time+ "/r/" + Math.floor(Math.random()*11232132132131);
        dijit.byId('langsel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
    }
    
    // init and changing the time
    else{
        dijit.byId('timesel').setValue("");

        var myUrl="/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/getAvailableTimeMachineForSite/hostIdentifier/<%=hostIdentifier%>/r/" + Math.floor(Math.random()*11232132132131);
        dijit.byId('timesel').set('store',new dojo.data.ItemFileReadStore({url:myUrl}));

        dijit.byId('langsel').set('store',emptyStore);
    }

}

function stopBrowing() {
    dojo.xhr('GET',{
        url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/stopBrowsing/r/'+ Math.floor(Math.random()*11232132132131),
        handle: function() {
            dojo.empty('iframeWrapper');
            dijit.byId('closeBtn').setDisabled(true);

            dijit.byId('timesel').required=false;
            dijit.byId('timesel').setValue(null);
            
            dijit.byId('langsel').setValue("");
            dijit.byId('langsel').required=false;
            

            
            dijit.byId('flang').setValue("");
            dijit.byId('flang').required=false;
            
            dojo.create("div", {
                "innerHTML": '<div ><span class="clockIcon"></span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME")) %>',
                "style": "padding:40px;text-align:center;white-space: nowrap;line-height: 20px;"
            }, dojo.byId('iframeWrapper'));
            
            browsingTimeMachine=false;
        }
    });
}
function showSettings() {
    var r = Math.floor(Math.random() * 1000000000);
    var dialog = new dijit.Dialog({
        id: 'settingsDialog',
        title: "<%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>",
        style: "width: 600px;height: auto;",
        content: new dojox.layout.ContentPane({
            style: "height:auto; width: 580px;",
            href: "/html/portlet/ext/timemachine/settings.jsp?random="+r
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

    if (dojo.byId("future").checked) {
        dojo.style('pastPicker', 'display', 'none');
        dojo.style('futurePicker', 'display', '');

    } else {
        dojo.style('futurePicker', 'display', 'none');
        dojo.style('pastPicker', 'display', '');

    }

}

function futureChange() {

    let pickedDate=dijit.byId('fdate').getValue();
    let pickedTime=dijit.byId('ftime').getValue();
    let pickedLanguage=dijit.byId('flang').getValue();
    

    
    if(pickedDate && pickedTime && pickedLanguage.length>0){
        
     pickedDate.setHours(pickedTime.getHours(), pickedTime.getMinutes(), 0,0);
     
     if(pickedDate < new Date()){
         dijit.byId('ftime').set("state", "Error");
         return;
     }

         dojo.xhr('GET',{
             url:'/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/startBrowsingFutureDate/date/'
                    +pickedDate.getTime()+'/hostIdentifier/<%=hostIdentifier%>/langid/'+pickedLanguage+'/rand/' + Math.random(),
             handle: function() {
                 dojo.empty('iframeWrapper');
                 dojo.create("iframe", {
                     "src": '/',
                     "style": "border: 0; width: 100%; height: 100%;"
                 }, dojo.byId('iframeWrapper'));
                 dijit.byId('closeBtn').setDisabled(false);
     
                 showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE-WHENDONE")%>");
                 browsingTimeMachine=true;
             }
         });
     }

}

</script>



<div class="portlet-main">
    <div id="timemachineMain">
        <!-- START Toolbar -->
        <div class="portlet-toolbar">
            <div class="portlet-toolbar__actions-primary">
                <div class="inline-form">
                     <input type="radio" dojoType="dijit.form.RadioButton" name="sn" id="past" onChange="toggleDatePick()" checked='true'/><label for="past"><%= LanguageUtil.get(pageContext, "Past") %></label>&nbsp; 
                     <input type="radio" dojoType="dijit.form.RadioButton" name="sn" id="future"/><label for="future"><%= LanguageUtil.get(pageContext, "Future") %></label>
    
                       <span id="pastPicker">           
                       <select id="timesel" dojoType="dijit.form.FilteringSelect"
                          labelAttr="pretty" searchDelay="400" searchAttr="pretty"
                          onFocus="timeChange()" onChange="timeChange()" pageSize="20">
                       </select>
        
                       <select id="langsel" dojoType="dijit.form.FilteringSelect"
                          labelAttr="pretty" searchDelay="400" searchAttr="pretty"
                          onChange="timeChange()" pageSize="20">
                       </select>
                   </span>
                    <span id="futurePicker">
                        <input id="fdate" />
                        <input id="ftime"  />
                            <select id="flang" dojoType="dijit.form.FilteringSelect" onChange="futureChange()">
                            <option value=""></option>
                            <% for(Language lang : APILocator.getLanguageAPI().getLanguages()) { %>
                                <option value="<%=lang.getId()%>"><%=lang.getLanguage() %>-<%=lang.getCountry() %></option>
                            <% } %>
                        </select>
                    </span>
                </div>
            </div>
            <div class="portlet-toolbar__info">
                
            </div>
            <div class="portlet-toolbar__actions-secondary">
                <script language="Javascript">
                    /**
                        focus on search box
                    **/
                    require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
                        dojo.require('dojox.timing');
                        t = new dojox.timing.Timer(500);
                        t.onTick = function(){
                          focusUtil.focus(dom.byId("timesel"));
                          t.stop();
                        };
                        t.start();
                    });
                </script>
                <!-- START Actions -->            
                <div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
                    <span></span>
                    
                    <div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
                    
                        <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                          <!-- <span id="settings" > -->
                            <div data-dojo-type="dijit/MenuItem" id="settingsBtn" onClick="showSettings()">
                                <%= LanguageUtil.get(pageContext, "TIMEMACHINE-SETTINGS")%>
                            </div>
                          <!-- </span> -->
                        <%} %>
                    
                        <div data-dojo-type="dijit/MenuItem" id="closeBtn" onClick="stopBrowing()">
                            <%= LanguageUtil.get(pageContext, "TIMEMACHINE-CLOSE_SNAP")%>
                        </div>
                
                    </div>
                </div>
                <!-- END Actions -->
                
                
            </div>
       </div>
       <!-- END Toolbar -->
               

           

        
        <div id="iframeWrapper" style="width: 100%; position: absolute; left: 0; top: 80px; right:0; bottom:0px;border-top: 1px solid #ECEDEE;">
           <div style="padding:40px;text-align:center;white-space: nowrap;line-height: 20px;">
                <span class="clockIcon"></span><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECT-HOST-TIME") %>
           </div> 
        </div>
        
    </div>
</div>


