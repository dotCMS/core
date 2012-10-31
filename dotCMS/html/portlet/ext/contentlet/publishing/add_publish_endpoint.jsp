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
<div style="float: left; padding-left: 15px;">
	<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Add_Intro") %> 
</div>		
<div>&nbsp;</div>
<div>&nbsp;</div>
<div style="float: left; padding-left: 15px;" id="response"></div>
<form name="formSaveEndpoint" method="post" id="formSaveEndpoint">
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input type="text" dojoType="dijit.form.ValidationTextBox" 
					  name="serverName" id="serverName" 
					  style="width:440px;"
					  required="true" promptMessage="A friendly server name identifier" onchange="enableSave()"/>
		</div>
		
	</div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input type="text" dojoType="dijit.form.ValidationTextBox" 
				   name="address" id="address" style="width:440px" 
				   required="true" promptMessage="The DNS server name or the IP address" onchange="enableSave()"/>
		</div>
	</div>	
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:52px;float:left;">
			<input type="text" dojoType="dijit.form.ValidationTextBox" 
				   name="port" id="port" style="width:50px" 
				   promptMessage="The server port. Default 80" regExp="^[0-9]+$" invalidMessage="The server port must be a number." />		
		</div>
		<div style="padding-left:40px;padding-right:10px;width:60px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %>:
		</div>
		<div style="padding-left:5px;padding-right:10px;width:100px;height:50px;float:left;">
			<select dojoType="dijit.form.Select" name="protocol" id="protocol" style="width:100px;height:28px;">
				<option value="http">http</option>
				<option value="https">https</option>
			</select>		
		</div>		
	</div>		
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">						          	
			<textarea dojoType="dijit.form.Textarea" name="authKey" id="authKey" style="width:450px;min-height:180px;max-height: 600px"></textarea>
		</div>		
	</div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="sending" />		
		</div>				
	</div>	
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" />		
		</div>						
	</div>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" onClick="backToEndpointsList(true)" id="closeSave" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
		<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon" disabled="true"><%= LanguageUtil.get(pageContext, "Save") %></button>
    </div>	
</form>