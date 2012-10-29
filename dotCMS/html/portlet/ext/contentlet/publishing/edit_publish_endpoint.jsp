<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublisherEndpointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	PublisherEndpointAPI pepAPI = APILocator.getPublisherEndpointAPI();
	List<PublishingEndPoint> endpoints = pepAPI.getAllEndpoints();
	String operation = request.getParameter("op");
	if(null==operation)
		operation = "add";
%>
<script type="text/javascript">
	
	function showAuthenticationMode(type){
		if(type=="path"){
			dojo.byId("tokenByPath").show();
			dojo.byId("tokenByString").hide();
		}else{
			dojo.byId("tokenByPath").hide();
			dojo.byId("tokenByString").show();
		}
	}

</script>
<div style="float: left; padding-left: 15px;">
	<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Add_Intro") %> 
</div>		
<div>&nbsp;</div>
<div>&nbsp;</div>
<div class="fieldWrapper" style="padding-top: 25px; clear: both;">	
	<div style="padding-left:30px;padding-right:10px;width:150px;float:left;">
		<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>
	</div>
	<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
		<textarea dojoType="dijit.form.Textarea" style="width:450px;min-height:100px;max-height: 600px"></textarea>		
	</div>
	
</div>
<div class="fieldWrapper" style="padding-top: 25px; clear: both;">	
	<div style="padding-left:30px;padding-right:10px;width:150px;float:left;">
		<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address") %>
	</div>
	<div style="padding-left:10px;padding-right:10px;width:475px;float:left;">
		<input type="text" dojoType="dijit.form.TextBox" style="width:440px" />		
	</div>
	<div style="padding-left:30px;padding-right:10px;width:30px;float:left;">
		<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>
	</div>
	<div style="padding-left:5px;padding-right:10px;width:52px;float:left;">
		<input type="text" dojoType="dijit.form.TextBox" style="width:50px" />		
	</div>
	<div style="padding-left:40px;padding-right:10px;width:50px;float:left;">
		<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Protocol") %>
	</div>
	<div style="padding-left:5px;padding-right:10px;width:100px;height:50px;float:left;">
		<select dojoType="dijit.form.Select" style="width:100px;height:28px;">
			<option value="http">http</option>
			<option value="https">https</option>
		</select>		
	</div>		
</div>
<div class="clear"></div>
<hr>
<div class="clear"></div>
<div class="fieldWrapper" style="padding-top: 25px; clear: both;">		
	<div style="padding-left:30px;padding-right:10px;width:150px;float:left;">
		<input type="radio" dojoType="dijit.form.RadioButton" name="authType" value="path" onclick="showAuthenticationMode(this.value);" checked="checked">&nbsp;<label>by path</label>&nbsp;<input type="radio" dojoType="dijit.form.RadioButton" name="authType" value="string" onclick="showAuthenticationMode(this.value);">&nbsp;<label>by string</label>
	</div>
</div>
<div class="fieldWrapper" style="padding-top: 25px; clear: both;">	
	<div style="padding-left:30px;padding-right:10px;width:150px;float:left;">
		<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key") %>
	</div>
	<div style="padding-left:30px;padding-right:10px;width:475px;float:left;">
		<input id="tokenByPath" name="uploadedfile" multiple="false"
					          type="file" data-dojo-type="dojox.form.Uploader"
					          label="Select File" id="restoreIndexUploader"
					          showProgress="true" />	
		<textarea id="tokenByString" dojoType="dijit.form.Textarea" style="width:450px;min-height:180px;max-height: 600px"></textarea>
		<script type="text/javascript">
			dojo.ready(function(){
				showAuthenticationMode("path");
			});
		</script>				          
	</div>
		
</div>