<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String identifier = request.getParameter("id");
	PublisherEndpointAPI peAPI = APILocator.getPublisherEndpointAPI();
	PublishingEndPoint currentEndpoint = peAPI.findEndpointById(identifier);
%>

<script type="text/javascript">

	function editEndpoint(){
		  var form = dojo.byId("formEditEndpoint");
		  dojo.connect(form, "onsubmit", function(event){
		  	  dijit.byId("update").setAttribute("disabled", true);
		      dijit.byId("closeUpdate").setAttribute("disabled", true);
		      dojo.stopEvent(event);
		      var xhrArgs = {
		    	url: "/DotAjaxDirector/com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction/cmd/editEndpoint",	  
		      	form: dojo.byId("formEditEndpoint"),
		      	handleAs: "text",
			    load: function(data){
			    	dojo.byId("response2").style.color = 'green';
			    	dojo.byId("response2").style.weight = 'bold';
			    	dojo.byId("response2").innerHTML = "Endpoint configuration updating successfully.";
			    	dijit.byId("update").setAttribute("disabled", true);
			    	dijit.byId("closeUpdate").setAttribute("disabled", false);
			    },
			    error: function(error){
			    	dojo.byId("response2").style.color = 'red';
			    	dojo.byId("response2").style.weight = 'bold';
			        dojo.byId("response2").innerHTML = "Endpoint configuration updating is not successful. Please contact the Administration. "+error;
			        dijit.byId("update").setAttribute("disabled", false);
			        dijit.byId("closeUpdate").setAttribute("disabled", false);
			    }
			  }
	    	  dojo.byId("response2").style.color = '#FFCC00';
	    	  dojo.byId("response2").style.weight = 'bold';
			  dojo.byId("response2").innerHTML = "Update the endpoint configuration. Please wait..."	
			  var deferred = dojo.xhrPost(xhrArgs);
		 });
	}
	dojo.ready(editEndpoint);

</script>
<div style="float: left; padding-left: 15px;">
	<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Add_Intro") %> 
</div>		
<div>&nbsp;</div>
<div>&nbsp;</div>
<div style="float: left; padding-left: 15px;" id="response2"></div>
<form name="formEditEndpoint" method="post" id="formEditEndpoint">
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<textarea dojoType="dijit.form.Textarea" name="serverName" style="width:450px;min-height:100px;max-height: 600px"><%if(null!=currentEndpoint)out.print(currentEndpoint.getServerName());%></textarea>
		</div>
		
	</div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input type="text" dojoType="dijit.form.TextBox" name="address" style="width:440px" value="<%if(null!=currentEndpoint)out.print(currentEndpoint.getAddress());%>" />		
		</div>
	</div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:52px;float:left;">
			<input type="text" dojoType="dijit.form.TextBox" name="port" style="width:50px" value="<%if(null!=currentEndpoint)out.print(currentEndpoint.getPort());%>"/>		
		</div>
		<div style="padding-left:40px;padding-right:10px;width:50px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %>:
		</div>
		<div style="padding-left:5px;padding-right:10px;width:100px;height:50px;float:left;">
			<select dojoType="dijit.form.Select" name="protocol" style="width:100px;height:28px;">
				<option value="http" <%if(null!=currentEndpoint && "http".equals(currentEndpoint.getProtocol())){%> selected="selected"<%}%>>http</option>
				<option value="https" <%if(null!=currentEndpoint && "https".equals(currentEndpoint.getProtocol())){%> selected="selected"<%}%>>https</option>
			</select>		
		</div>		
	</div>		
	<div class="clear"></div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">	
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">						          	
			<textarea dojoType="dijit.form.Textarea" name="authKey" style="width:450px;min-height:180px;max-height: 600px"><%if(null!=currentEndpoint)out.print(PublicEncryptionFactory.decryptString(currentEndpoint.getAuthKey().toString()));%></textarea>
		</div>		
	</div>
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="sending" <%if(null!=currentEndpoint && currentEndpoint.isSending()){%> checked="checked"<%}%>/>		
		</div>				
	</div>	
	<div class="fieldWrapper" style="padding-top: 15px; clear: both;">
		<div style="padding-left:30px;padding-right:10px;width:80px;float:left;">
			<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
		</div>
		<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" <%if(null!=currentEndpoint && currentEndpoint.isEnabled()){%> checked="checked"<%}%>/>		
		</div>						
	</div>
	<input type="hidden" name="identifier" value="<%=currentEndpoint.getId()%>" />
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" onClick="backToEndpointsList(false)" id="closeUpdate" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
		<button dojoType="dijit.form.Button" type="submit" id="update" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "update") %></button>
    </div>	
</form>