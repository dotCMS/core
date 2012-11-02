<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String identifier = request.getParameter("id");
	PublisherEndpointAPI peAPI = APILocator.getPublisherEndpointAPI();
	PublishingEndPoint currentEndpoint = peAPI.findEndpointById(identifier);
	if(currentEndpoint ==null){
		currentEndpoint = new PublishingEndPoint();
		currentEndpoint.setEnabled(true);
		currentEndpoint.setPort("80");
		currentEndpoint.setProtocol("http");
		currentEndpoint.setSending(false);
		
	}
	
	
	
%>

<script type="text/javascript">
	function saveEndpoint(){
  
		var form = dijit.byId("formSaveEndpoint");
		dijit.byId("serverName").required=true;
		if(dijit.byId("sendingServer").checked){
			dijit.byId("address").setAttribute('required',true);	
			dijit.byId("port").setAttribute('required',true);	
		}
		else{
			dijit.byId("address").setAttribute('required',false);	
			dijit.byId("port").setAttribute('required',false);	
			
		}
		if (form.validate()) {


			var xhrArgs = {
				url: "/DotAjaxDirector/com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction/cmd/addEndpoint",
				form: dojo.byId("formSaveEndpoint"),
				handleAs: "text",
				load: function(data){
					if(data.indexOf("FAILURE") > -1){
						
						alert(data);
					}
					else{
						backToEndpointsList();
					}
				},
				error: function(error){
					alert(error);
					
				}
			}

			var deferred = dojo.xhrPost(xhrArgs);				
		}

	}

	
	function enableSave(){
		var servernamevalue = dojo.byId("serverName").getValue();

		if(servernamevalue && servernamevalue.length > 0){
			dijit.byId("save").setAttribute('disabled',false);	
		}else{
			
			dijit.byId("save").setAttribute('disabled',true);	
		}

	}
	
	var sending=<%=currentEndpoint.isSending()%>;
	
	function toggleServerType(){
		sending=!sending;
		if(sending){
			dojo.style("addressRow", "display", "table-row");
			dojo.style("portRow", "display", "table-row");
			
		}
		else{
			
			dojo.style("addressRow", "display", "none");
			dojo.style("portRow", "display", "none");
		}
		
		
		
	}
	
	
	
	dojo.ready( function(){
		toggleServerType();
		
		
	});
	
	
	
	
</script>

<style>
	.myTable {margin:20px;padding:10px;}
	.myTable tr td{padding:5px;vertical-align: top;}
	#addressRow {display:none;}
	#portRow {display:none;}
	
</style>


<div dojoType="dijit.form.Form"  name="formSaveEndpoint"  id="formSaveEndpoint" onsubmit="return false;">
	<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getId())) %>">
	<table class="myTable shadowBox"  align="center">
		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>:
			</td>
			<td>
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
						  name="serverName" 
						  id="serverName" 
						  style="width:300px;"
						  value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getServerName())) %>"
						  promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_ServerName_Prompt_Message") %>" 
						  />
			</td>
		</tr>
		<tr>
			<td align="center" colspan=2>
				
				<input onClick="toggleServerType()" dojoType="dijit.form.RadioButton" type="radio" name="sending" value="sending" checked="<%=!currentEndpoint.isSending()%>" id="sendingServer" />
				<label for="sendingServer"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server") %></label>	
				
				&nbsp;
				&nbsp;
				
				
				<input onClick="toggleServerType()" dojoType="dijit.form.RadioButton" type="radio" name="sending" value="receive"  checked="<%=currentEndpoint.isSending()%>"  id="recivingServer" />
				<label for="recivingServer"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Receiving_Server") %></label>		
			</td>				
		</tr>	
		
		
		
		
		<tr id="addressRow">
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %>:
			</td>
			<td>
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
					   name="address" 
					   id="address"
					   style="width:300px" 
					   value="<%=UtilMethods.webifyString(currentEndpoint.getAddress()) %>"
					   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Address_Prompt_Message") %>" 
					   />
			</td>
		</tr>	
		
		
		<tr id="portRow">
			<td align="right"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:</td>
			<td nowrap="nowrap">
				
				<input type="text" dojoType="dijit.form.ValidationTextBox" 
					   name="port" id="port" style="width:50px" 
					   value="<%=UtilMethods.webifyString(currentEndpoint.getPort()) %>"
					   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Prompt_Message") %>" regExp="^[0-9]+$" invalidMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Invalid_Message") %>" />		
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %>:
				
	
				<select dojoType="dijit.form.Select" name="protocol" id="protocol" style="width:100px;">
					<option value="http" <%=("http".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>>http</option>
					<option value="https" <%=("https".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>>https</option>
				</select>		

			</td>		
		</tr>	
		
		
		<tr id="authKeyRow">
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key") %>:
			</td>
			<td>						          	
				<textarea dojoType="dijit.form.Textarea" name="authKey" id="authKey" style="width:450px;min-height:180px;max-height: 600px"><%=( currentEndpoint.getAuthKey() != null && currentEndpoint.getAuthKey().length() > 0) ? PublicEncryptionFactory.decryptString( currentEndpoint.getAuthKey().toString())  : "" %></textarea>
			</td>		
		</tr>	

		<tr>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
			</td>
			<td>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" <%=(currentEndpoint.isEnabled()) ? "checked=true" : "" %> />		
			</td>						
		</tr>	
	</table>
	<table align="center">
		<tr>
			<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
				<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon"  onclick="saveEndpoint()"><%= LanguageUtil.get(pageContext, "Save") %></button>
				&nbsp;
				<button dojoType="dijit.form.Button" onClick="backToEndpointsList(true)" id="closeSave" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
				
		    </td>
	    </tr>
   </table>	
</div>