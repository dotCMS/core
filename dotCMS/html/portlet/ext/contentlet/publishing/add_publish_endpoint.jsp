<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<script type="text/javascript">
	function addEndpoint(){
		var form = dojo.byId("formSaveEndpoint");
		dojo.connect(form, "onsubmit", function(event){
			dijit.byId("save").setAttribute('disabled',true);	  
			dijit.byId("closeSave").setAttribute("disabled", true);
			dojo.stopEvent(event);
			var xhrArgs = {
				url: "/DotAjaxDirector/com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction/cmd/addEndpoint",
				form: dojo.byId("formSaveEndpoint"),
				handleAs: "text",
				load: function(data){
					dojo.byId("response").style.color = 'green';
					dojo.byId("response").style.weight = 'bold';
					dojo.byId("response").innerHTML = "Endpoint saved successfully.";
					dijit.byId("save").setAttribute('disabled',true);
					dijit.byId("closeSave").setAttribute("disabled", false);
				},
				error: function(error){
					dojo.byId("response").style.color = 'red';
					dojo.byId("response").style.weight = 'bold';			    	
					dojo.byId("response").innerHTML = "Endpoint not saved successfully. "+error;
					dijit.byId("save").setAttribute('disabled',false);
					dijit.byId("closeSave").setAttribute("disabled", false);
				}
			}
			dojo.byId("response").style.color = '#FFCC00';
			dojo.byId("response").style.weight = 'bold';
			dojo.byId("response").innerHTML = "Endpoint is being to save..."
			var deferred = dojo.xhrPost(xhrArgs);				
		});
	}
	dojo.ready(addEndpoint);
	
	function enableSave(){
		var servernamevalue = dojo.byId("serverName").value;
		var addressvalue = dojo.byId("address").value;
		if(''!=servernamevalue && ''!=addressvalue)
			dijit.byId("save").setAttribute('disabled',false);	
		else
			dijit.byId("save").setAttribute('disabled',true);
	}
</script>

<style>
.myTable {margin:20px;}
.myTable tr td{padding:5px;vertical-align: top}
</style>


<form name="formSaveEndpoint" method="post" id="formSaveEndpoint">
	<table class="myTable shadowBox" style="padding:10px;" align="center">
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>:
			</td>
			<td>
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
						  name="serverName" id="serverName" 
						  style="width:440px;"
						  required="true" promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_ServerName_Prompt_Message") %>" onchange="enableSave()"/>
			</td>
			
		</tr>
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %>:
			</td>
			<td>
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
					   name="address" id="address" style="width:440px" 
					   required="true" promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Address_Prompt_Message") %>" onchange="enableSave()"/>
			</td>
		</tr>	
		<tr>
			<td align="right"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:</td>
			<td nowrap="nowrap">
				
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
					   name="port" id="port" style="width:50px" 
					   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Prompt_Message") %>" regExp="^[0-9]+$" invalidMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Invalid_Message") %>" />		
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %>:
				
	
				<select dojoType="dijit.form.Select" name="protocol" id="protocol" style="width:100px;height:28px;">
					<option value="http">http</option>
					<option value="https">https</option>
				</select>		

			</td>		
		</tr>	
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key") %>:
			</td>
			<td>						          	
				<textarea dojoType="dijit.form.Textarea" name="authKey" id="authKey" style="width:450px;min-height:180px;max-height: 600px"></textarea>
			</td>		
		</tr>	
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending") %>:
			</td>
			<td>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="sending" />		
			</td>				
		</tr>	
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
			</td>
			<td>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" />		
			</td>						
		</tr>	
	</table>
	<table align="center">
		<tr>
			<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
				<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon" disabled="true"><%= LanguageUtil.get(pageContext, "Save") %></button>
				&nbsp;
				<button dojoType="dijit.form.Button" onClick="backToEndpointsList(true)" id="closeSave" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
				
		    </td>
	    </tr>
   </table>	
</form>