<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@include file="/html/common/init.jsp"%>

<%if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){return;} %>


<%
String cronExp="";
List<Host> hosts=new ArrayList<Host>();
Boolean allhosts=false;
List<Language> langs=new ArrayList<Language>();
Boolean incremental = false;
ScheduledTask task=APILocator.getTimeMachineAPI().getQuartzJob();
if(task!=null) {
    allhosts=(Boolean) task.getProperties().get("allhosts");
    hosts=(List<Host>) task.getProperties().get("hosts");
    langs=(List<Language>) task.getProperties().get("langs");
    cronExp=(String) task.getProperties().get("CRON_EXPRESSION");
    incremental = task.getProperties().get("incremental")!=null && (Boolean) task.getProperties().get("incremental");
}
%>
<style type="text/css">
#settingform td {
    padding-bottom: 10px;
    padding-left: 10px;
}
</style>
<script type="text/javascript">
dojo.require("dotcms.dojo.data.HostReadStore");

function addHostUI() {
	if(dijit.byId('hostSelector').attr('value') == '') {
        return;
    }

    var hostId = dijit.byId('hostSelector').get('value');
    var hostName = dijit.byId('hostSelector').get('displayedValue');
    addHost(hostId,hostName);
}

function addHost(hostId, hostName) {
    var table = document.getElementById('hostTable');
    var rowCount = table.rows.length;
    var row  = document.getElementById(hostId);

    if(row!=null){
       alert('<%= LanguageUtil.get(pageContext, "host-already-selected") %>');
    }else{
        var nohosts = document.getElementById("nohosts");
        if(nohosts!=null)
            table.deleteRow(0);

        var newRow = table.insertRow(table.rows.length);
        if((table.rows.length%2)==0)
            newRow.className = "alternate_1";
        else
            newRow.className = "alternate_2";

        newRow.id = hostId;
        var cell0 = newRow.insertCell(0);
        var cell1 = newRow.insertCell(1);
        var anchor = document.createElement("a");
        anchor.href= 'javascript:deleteHost('+'"'+ hostId +'"'+');';
        anchor.innerHTML = '<span class="deleteIcon"></span>';
        cell0.appendChild(anchor);
        cell1.innerHTML = hostName;
        var input = document.createElement("input");
        input.type="hidden";
        input.name="snaphost";
        input.id="snaphost"+hostId;
        input.value=hostId;
        newRow.appendChild(input);

    }

}

function deleteHost(hostId) {
    var table = document.getElementById('hostTable');
    var row  = document.getElementById(hostId);
    if(row){
        try {
             var rowCount = table.rows.length;
             for(var i=0; i<rowCount; i++) {
                if(row.id==table.rows[i].id) {
                    table.deleteRow(i);
                    rowCount--;
                    i--;
                    if(rowCount <= 0) {
                        addEmptyMessage();
                        break;
                    }
                }
             }
         }catch(e) {}
    }
}

function addEmptyMessage() {
	var table = document.getElementById('hostTable');
	var rowCount = table.rows.length;
	var newRow = table.insertRow(rowCount);
    newRow.id="nohosts";
    var newdiv = document.createElement("div");
    if(dijit.byId('allhosts').get('value'))
    	newdiv.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "all-hosts-selected") %></div></td>';
    else
    	newdiv.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "no-hosts-selected") %></div></td>';

    newRow.appendChild(newdiv);
}

function indexAll(checked){
	dojo.empty('hostTable');
	addEmptyMessage();
    if(checked){
        dijit.byId('hostSelector').set('disabled','disabled');
        dijit.byId('addHostButton').set('disabled','disabled');
    }else{
    	dijit.byId('hostSelector').attr('disabled', false);
        dijit.byId('addHostButton').attr('disabled', false);
    }
}

function saveAndRun(dorun) {

	var allhosts=dijit.byId("allhosts").checked;
	if(dojo.query("[name='snaphost']").length==0 && !allhosts) {
		dijit.byId('hostSelector').focus();
		return;
	}

	if(dojo.query("#settingform input[name='lang']:checked").length==0) {
        showDotCMSErrorMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Choose-a-Language")) %>");
        return;
    }

	var actionURL='/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/saveJobConfig';
	if(dorun)
		actionURL+='/run/1';

	var form=dijit.byId('settingform');

	if(form.validate()) {
		dojo.xhrPost({
			url: actionURL,
            form : "settingform",
            preventCache:true,
            timeout : 30000,
            error: function(data) {
            	showDotCMSSystemMessage(data, true);
            	dijit.byId('settingsDialog').hide();
            },
            load : function(dataOrError, ioArgs) {
            	if(dataOrError.indexOf("FAILURE") == 0)
                    showDotCMSSystemMessage(dataOrError, true);
            	else {
                	 if(dorun)
                     	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "TIMEMACHINE-SAVED-RUN")%>", false);
                     else
                        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "TIMEMACHINE-SAVED")%>", false);
                     dijit.byId('settingsDialog').hide();
            	}
            }
        });
	}
}

function save() {
	saveAndRun(false);
}

function runNow() {
	saveAndRun(true);
}

function disableJob() {
	dojo.xhrGet({
        url: "/DotAjaxDirector/com.dotcms.timemachine.ajax.TimeMachineAjaxAction/cmd/disableJob",
        preventCache:true,
        timeout : 30000,
        error: function(data) {
            showDotCMSSystemMessage(data, true);
            dijit.byId('settingsDialog').hide();
        },
        load : function(dataOrError, ioArgs) {
            if(dataOrError.indexOf("FAILURE") == 0)
                showDotCMSSystemMessage(dataOrError, true);
            else {
                showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "TIMEMACHINE-DISABLED")%>", false);
                dijit.byId('settingsDialog').hide();
            }
        }
    });
}

dojo.ready(function() {
	<% if(!allhosts && hosts!=null) { %>
	     <% for(Host hh : hosts) { %>
	          addHost("<%=hh.getIdentifier()%>","<%=hh.getHostname()%>");
	     <% } %>
	<% } %>
});

</script>
<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>
<div style="height:550px; width:580px; overflow:auto;">
<form id="settingform" dojoType="dijit.form.Form" >
   <table>
   <tr>
   <td align="right" valign="top">
     <span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "TIMEMACHINE-SELECTHOST") %>:</strong></td>
   <td>
    <select id="hostSelector" name="hostSelector" dojoType="dijit.form.FilteringSelect"  store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname"  invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>" <%=allhosts?"disabled=true":"" %> required="false"></select>
    <button id="addHostButton" dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="addHostUI()" <%=allhosts?"disabled":"" %>><%= LanguageUtil.get(pageContext, "Add-Host") %></button>
    <table id="hostTable">
      <tr id= "nohosts">
        <td colspan="2">
            <div class="noResultsMessage"><%= allhosts?LanguageUtil.get(pageContext, "all-hosts-selected"):LanguageUtil.get(pageContext, "no-hosts-selected") %></div>
        </td>
      </tr>
    </table>
   </td>
   </tr>
   <tr>
   <td align="right" valign="top" nowrap="true">
      <strong><%= LanguageUtil.get(pageContext, "TIMEMACHINE-ALLHOSTS") %>: </strong>
   </td>
   <td>
      <div style="padding:5px;">
      <input name="allhosts" id="allhosts" dojoType="dijit.form.CheckBox" type="checkbox"
          value="true" <%=!allhosts?"":"checked='true'"%> onclick="indexAll(this.checked)" />
      </div>
   </td>
   </tr>
   <tr>
   <td align="right" valign="top" nowrap="true">
     <span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "TIMEMACHINE-LANGUAGE") %>:</strong>
   </td>
   <td>
     <% for(Language lang : APILocator.getLanguageAPI().getLanguages()) { %>
          <div class="langContainer" style="padding:5px;">
            <input type="checkbox" dojoType="dijit.form.CheckBox" id="op_<%=lang.getId()%>"
                   name="lang" value="<%=lang.getId()%>"
                   <%=(langs!=null && langs.contains(lang)) ? "checked='true'" : "" %>/>
            <label for="op_<%=lang.getId()%>">
              <%= lang.getLanguage() + " - " + lang.getCountry() %>
            </label>
          </div>
     <% } %>
   </td>
   



		<tr>
			<td align="right" valign="top" nowrap="true">
				&nbsp;
			</td>
			<td>
				<div style="padding:5px;">
					<input  type="checkbox" dojoType="dijit.form.CheckBox" id="incremental" name="incremental" value="true" <%=(incremental) ? "checked='true'": "" %>>
					<label for="incremental">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Incremental")) %></label>

				</div>

			</td>
		</tr>
   

   
   
   </tr>

   <tr class="showScheduler">
       <td align="right" valign="top">
           <span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "TIMEMACHINE-CRONEXP") %>: </strong>
       </td>
       <td>
           <input name="cronExp" id="cronExp" type="text" dojoType='dijit.form.ValidationTextBox'
                   required="true" style='width: 200px'" value="<%=cronExp %>" size="10" />
            <div style="width: 350px; margin:20px; text-align: left;" id="cronHelpDiv" class="callOutBox2">
               <h3><%= LanguageUtil.get(pageContext, "cron-examples") %></h3>
               <span style="font-size: 88%;">
               <p></p>
               <p><b><%= LanguageUtil.get(pageContext, "cron-once-an-hour") %>:</b> 0 0/60 * * * ?</p>
               <p><b><%= LanguageUtil.get(pageContext, "cron-twice-a-day") %>:</b> 0 0 10-11 ? * *</p>
               <p><b><%= LanguageUtil.get(pageContext, "cron-once-a-day-1am")%>:</b> 0 0 1 * * ?</p>
               </span>
           </div>

       </td>
   </tr>
   <tr>
     <td align="center" valign="top" nowrap="true" colspan="2">

         <div class="buttonRow" style="white-space: nowrap;">
             <span class="showScheduler" >
                 <button dojoType="dijit.form.Button"
                     id="saveButton" onClick="save()"
                     iconClass="calListIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "TIMEMACHINE-SAVE")) %>
                 </button>
             </span>

             <span class="showRunNow">
                 <button dojoType="dijit.form.Button"
                     id="runButton" onClick="runNow();"
                     iconClass="republishIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "TIMEMACHINE-RUN")) %>
                 </button>
             </span>
             
             <span class="showScheduler">
                 <button dojoType="dijit.form.Button"
                     id="disableButton" onClick="disableJob();"
                     iconClass="deleteIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "TIMEMACHINE-DISABLE")) %>
                 </button>
             </span>

         </div>
     </td>
   </tr>
   </table>

</form>
</div>
<%@include file="/html/common/bottom_inc.jsp"%>