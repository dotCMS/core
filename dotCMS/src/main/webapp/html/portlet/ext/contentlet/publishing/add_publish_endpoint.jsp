<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher"%>
<%@page import="com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory"%>
<%@ page import="com.dotcms.publisher.pusher.PushPublisher" %>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.temporal.ChronoUnit"%>
<%@page import="java.time.Instant"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>

<%
	String identifier = request.getParameter("id");
	String environmentId = request.getParameter("environmentId");
	String isSender = request.getParameter("isSender");
	Boolean isServer = Boolean.parseBoolean(isSender);

	PublishingEndPointAPI peAPI = APILocator.getPublisherEndPointAPI();
	PublishingEndPoint currentEndpoint = peAPI.findEndPointById(identifier);

	Environment currentEnvironment = APILocator.getEnvironmentAPI().findEnvironmentById(environmentId);

	if(currentEndpoint ==null){
		PublishingEndPointFactory publishingEndPointFactory = new PublishingEndPointFactory();
		currentEndpoint = publishingEndPointFactory.getPublishingEndPoint(PushPublisher.PROTOCOL_HTTP);
		currentEndpoint.setEnabled(true);
		currentEndpoint.setPort("");
		currentEndpoint.setProtocol("");
		currentEndpoint.setSending(false);

		if(currentEnvironment!=null)
			currentEndpoint.setGroupId(currentEnvironment.getId());

	}

	List<String> groups = peAPI.findSendGroups();
%>

<script type="text/javascript">

	// if it is aws s3 selected, the password should be displayed.
	var currentProtocol = "<%= currentEndpoint.getProtocol() %>";

	require(["dojo/parser", "dijit/form/SimpleTextarea"]);
	function saveEndpoint(){

		var form = dijit.byId("formSaveEndpoint");

		dijit.byId("serverName").setAttribute('required',true);
		dijit.byId("address").setAttribute('required',true);

		if(dojo.byId("sending").value=='false'){
			dijit.byId("port").setAttribute('required',true);
		}
		else{
			dijit.byId("port").setAttribute('required',false);
		}

		if (form.validate()) {

			dijit.byId("save").setAttribute('disabled',true);

			var xhrArgs = {
				url: "/DotAjaxDirector/com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction/cmd/addEndpoint",
				form: dojo.byId("formSaveEndpoint"),
				handleAs: "text",
				load: function(data){
					if(data.indexOf("FAILURE") > -1){
						dijit.byId("save").setAttribute('disabled',false);

						alert(data);
					}
					else{
						<% if (UtilMethods.isSet(environmentId)) { %>
						backToEnvironmentList(true);
						<% } else {%>
						backToEndpointsList();
						<%}%>
					}
				},
				error: function(error){
					dijit.byId("save").setAttribute('disabled',false);

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

	function toggleServerType(sending){

		if(sending=='false'){
			dojo.style("addressFromSpan", "display", "none");
			dojo.style("addressToSpan", "display", "");

			dojo.style("sendGroupRow", "display", "flex");
		}
		else{
			dojo.style("addressFromSpan", "display", "");
			dojo.style("addressToSpan", "display", "none");
			dojo.style("protocolRow", "display", "none");
			dojo.style("portSpan", "display", "none");
			dojo.style("addressRow", "display", "");
			dojo.style("authPropertiesRow", "display", "");

		}
	}

	function setAddressRow(changedType) {

		if ((currentProtocol === "awss3" || currentProtocol === "static")
				&& isPlatformLicenseLevel()) {

			if (changedType) {
				dijit.byId("address").set("value", "static.dotcms.com");
				dijit.byId("port").set("value", "80");

				dojo.byId("addressRow").hide();
			}
		} else {
			if (changedType) {
				if(dijit.byId("address")){
					dijit.byId("address").set("value", "");
				}
				if(dojo.byId("addressRow")){
					dojo.byId("addressRow").show();
				}
			}
		}
	}

	function setAuthPropertiesRow(changedType) {

		if (currentProtocol === "awss3" && isPlatformLicenseLevel()) {

			dojo.style("authKeyHttpSpan", "display", "none");
			dojo.style("authKeyStaticSpan", "display", "");

			if (dijit.byId("authKey").value.trim().length == 0 || changedType) {

				dijit.byId("authKey").set("value",
						"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_TOKEN%>=myToken\n" +
						"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_SECRET%>=mySecret\n" +
						"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID%>=dotcms-bucket-{hostname}-{languageIso}\n" +
						"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_REGION%>=us-west-2"
				);
			}
		} else if(currentProtocol === "static" && isPlatformLicenseLevel()){
			dojo.style("authKeyHttpSpan", "display", "none");
			dojo.style("authKeyStaticSpan", "display", "");

			if (dijit.byId("authKey").value.trim().length == 0 || changedType) {

				dijit.byId("authKey").set("value",
						"<%=StaticPublisher.DOTCMS_STATIC_PUBLISH_TO%>=dotcms-static-{hostname}-{languageIso}"
				);
			}
		} else {

			dojo.style("authKeyHttpSpan", "display", "");
			dojo.style("authKeyStaticSpan", "display", "none");

			if (changedType || !isPlatformLicenseLevel()) {
				if(dijit.byId("authKey")){
					dijit.byId("authKey").set("value", "");
				}
			}
		}

		if (changedType) {
			dojo.byId("authPropertiesRow").show();
		}
	}


	function onChangeProtocolTypeSelectCheck() {
		currentProtocol = dijit.byId("protocol").value;

		if (dojo.getStyle("getTokenSpan", "display") === "none"){
			setAddressRow(true);
			setAuthPropertiesRow(true);
		}
	}

	function isPlatformLicenseLevel() {
		<% if(LicenseUtil.getLevel()>LicenseLevel.PRIME.level){ %>
		return true;
		<%} else { %>
		return false;
		<%} %>
	}

	height = 0;

	function showGetToken(show){

		if (show){
			height =  dojo.getComputedStyle(dojo.byId("main")).height;
			dojo.byId("authPropertiesRow").hide();
			dojo.byId("getTokenSpan").show();
			dojo.byId("showGetTokenSpan").hide();
		} else {
			dojo.byId("authPropertiesRow").show();
			dojo.byId("getTokenSpan").hide();
			dojo.byId("showGetTokenSpan").show();
			dojo.byId("main").setStyle({
				"height": height
			});
		}
	}

	function shouldEnabledGeToken(){
		address = document.getElementById('address').value;
		port = document.getElementById('port').value;

		if (address && port){
			dojo.byId("showGetTokenSpan").show();
		} else {
			dojo.byId("showGetTokenSpan").hide();
		}
	}

	function setToken(token){
		authKey = dojo.byId("authKey");
		authKey.value = token;

		showGetToken(false);
	}

	function showErrorGettingToken(e){
		alert(e);
	}

	function getToken(){

		if (dijit.byId('tokenForm').validate()) {

			var nowsers = new Date();
			var expires = dijit.byId('expiresDate').value;

			var timeDiff = expires.getTime() - nowsers.getTime();

			if(timeDiff<1000){
				alert("you cannot request a key in the past");
				return;
			}

			var data = {
				token: {
					expirationSeconds: Math.ceil(timeDiff / 1000 ),
					network: dijit.byId('network').value,
					claims: {"label" : 'Push Publish'},
					userId: '<%=PortalUtil.getUser(request).getUserId()%>'
				},
				remote: {
					host: dijit.byId('address').value,
					port: dijit.byId('port').value,
					protocol: currentProtocol
				},
				auth: {
					login: dijit.byId('login').value,
					password: window.btoa(dijit.byId('password').value)
				}
			};

			var xhrArgs = {
				url : "/api/v1/apitoken/_remote",
				handleAs: "json",
				postData : dojo.toJson(data),
				headers: {
					"Content-Type": "application/json"
				},
				load : function(data){
					setToken(data.entity.jwt);
				},
				error : function(error) {
					console.error("Error requesting a new APIKey", error);
					alert(error.message);

				}
			};

			dojo.xhrPut(xhrArgs);
		}
	}

	dojo.ready( function(){

		<% if( ! com.dotmarketing.util.UtilMethods.isSet( currentEndpoint.getProtocol() ) ) { %>
		dojo.byId("addressRow").hide();
		dojo.byId("authPropertiesRow").hide();
		<% } %>

		if ((currentProtocol === "awss3" || currentProtocol === "static")
				&& isPlatformLicenseLevel()) {

			dojo.byId("addressRow").hide();
		}

		setAddressRow(false);
		setAuthPropertiesRow(false);

		toggleServerType('<%=isSender%>');

		shouldEnabledGeToken();
	});

</script>


<div id="main" style="margin:auto;display: inline-block;">
	<div dojoType="dijit.form.Form"  name="formSaveEndpoint"  id="formSaveEndpoint" onsubmit="return false;">
		<input type="hidden" name="sending" id="sending" value="<%=isSender%>">
		<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getId())) %>">
		<div class="form-horizontal">
			<%if(currentEnvironment!=null) { %>
			<dl id="sendGroupRow">
				<dt>
					<%= LanguageUtil.get(pageContext, "publisher_Environment") %>:
				</dt>
				<dd>
					<%=currentEnvironment.getName() %>
					<input type="hidden" id="environmentId" name="environmentId" value="<%=currentEnvironment.getId() %>">
				</dd>
			</dl>
			<%} %>
			<dl>
				<dt>
					<label for="serverName"><%= (isServer) ? LanguageUtil.get(pageContext, "publisher_Server_Name") : LanguageUtil.get(pageContext, "publisher_Endpoint_Name") %>:</label>
				</dt>
				<dd>
					<input type="text" dojoType="dijit.form.ValidationTextBox"
						   name="serverName"
						   id="serverName"
						   style="width:300px;"
						   value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getServerName())) %>"
						   promptMessage="<%= (isServer) ? LanguageUtil.get(pageContext, "publisher_Server_Validation_Name_Prompt_Message") :
						    							   LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Name_Prompt_Message") %>"
					/>
				</dd>
			</dl>

			<dl id="protocolRow">
				<dt>
					<label form="protocol"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Type") %>:</label>
				</dt>
				<dd>
					<select dojoType="dijit.form.Select" name="protocol" id="protocol" onchange="onChangeProtocolTypeSelectCheck();">
						<% if( ! com.dotmarketing.util.UtilMethods.isSet( currentEndpoint.getProtocol() ) ) { %>
						<option disabled="disabled" selected="selected" value=""><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_placeholder") %></option>
						<%} %>
						<option value="http" <%=("http".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_http") %></option>
						<option value="https" <%=("https".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_https") %></option>
						<%if(LicenseUtil.getLevel() >= LicenseLevel.PLATFORM.level){ %>
						<option value="awss3" <%=("awss3".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_awss3") %></option>
						<option value="static" <%=("static".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_static") %></option>
						<%}else{ %>
						<option value="" disabled=true><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_awss3_requires_platform_license") %></option>
						<%} %>
					</select>
				</dd>
			</dl>

			<dl id="addressRow">
				<dt>
					<span id="addressToSpan">
						<label for="address"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_To") %>:</label>
					</span>
					<span id="addressFromSpan" style="display:none;">
						<label for="address"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_From") %>:</label>
					</span>
				</dt>
				<dd>
					<div class="inline-form">
						<input type="text" dojoType="dijit.form.ValidationTextBox"
							   name="address"
							   id="address"
							   style="width:300px"
							   value="<%=UtilMethods.webifyString(currentEndpoint.getAddress()) %>"
							   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Address_Prompt_Message") %>"
							   onKeyUp="shouldEnabledGeToken()"
						/>
						<span id="portSpan">
							<label for="port"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:</label>
							<input type="text" dojoType="dijit.form.ValidationTextBox"
								   name="port" id="port" style="width:60px"
								   value="<%=UtilMethods.webifyString(currentEndpoint.getPort()) %>"
								   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Prompt_Message") %>" regExp="^[0-9]+$" invalidMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Invalid_Message") %>"
								   onKeyUp="shouldEnabledGeToken()"/>
						</span>
					</div>
					<div id="addressHelpText" class="hint-text">e.g. 10.0.1.10 or server2.myhost.com</div>
				</dd>
			</dl>

			<dl id="authPropertiesRow">
				<dt>
					<span id="authKeyHttpSpan">
						<label for="authKey"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key_type_http") %>:</label>
					</span>
					<span id="authKeyStaticSpan" style="display:none;">
						<label for="authKey"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key_type_static") %>:</label>
					</span>
				</dt>
				<dd>
					<textarea dojoType="dijit.form.SimpleTextarea" name="authKey" id="authKey" style="width:400px;height:105px;"><%=currentEndpoint.hasAuthKey() ? PublicEncryptionFactory.decryptString( currentEndpoint.getAuthKey().toString())  : "" %></textarea>
				</dd>
			</dl>

			<dl id="getTokenSpan" style="display:none;">

				<dt></dt>
				<dd>
					<div dojoType="dijit.form.Form" id="tokenForm" onsubmit="return false;">
						<table class="listingTable" >

							<tr>
								<td>
									<h3>Request New Token</h3>
								</td>
							</tr>

							<tr>
								<td>
									<label for="login">Login</label>
								</td>
								<td>
									<input dojoType="dijit.form.TextBox" type="text"
										   name="login" id="login"/>
								</td>
							</tr>

							<tr>
								<td>
									<label for="password">Password</label>
								</td>
								<td>
									<input dojoType="dijit.form.TextBox" type="password"
										   name="password" id="password"/>
								</td>
							</tr>

							<tr>
								<td>
									<label for="expiresDate"><%=LanguageUtil.get(pageContext, "api.token.request.expires.date")%>:</label>
								</td>
								<td>
									<input dojoType="dijit.form.DateTextBox" type="text"
										   name="expiresDate" id="expiresDate"
										   value='<%=DateTimeFormatter.ofPattern("uuuu-MM-dd").format(LocalDate.now().plus(3, ChronoUnit.YEARS))%>'/>
								</td>
							</tr>

							<tr>
								<td><label for="netmask"><%=LanguageUtil.get(pageContext, "api.token.allowed.network")%>:
								</label></td>

								<td><input dojoType="dijit.form.TextBox" type="text"
										   name="network" id="network" value="0.0.0.0/0"></td>
							</tr>
						</table>

						<div class="buttonRow">
							<button dojoType="dijit.form.Button" type="button"
									class="dijitButtonFlat"
									onClick="showGetToken(false)"><%=LanguageUtil.get(pageContext, "cancel")%></button>
							&nbsp;
							<button id="ok_button" dojoType="dijit.form.Button" onClick="getToken();"><%=LanguageUtil.get(pageContext, "ok")%></button>
						</div>
					</div>
				</dd>
			</dl>

			<dl>
				<dt>
					<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
				</dt>
				<dd>
					<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" <%=(currentEndpoint.isEnabled()) ? "checked=true" : "" %> />
				</dd>
			</dl>
		</div>
		<div style="width: 95%;display: inline-block;">
			<span id= "showGetTokenSpan" style="float: left;display:none;">
				<button dojoType="dijit.form.Button" onClick="showGetToken(true)" id="getToken" class="dijitButtonFlat">Get Token</button>
			</span>
			<span style="float: right;">

				<button dojoType="dijit.form.Button" onClick="backToEndpointsList(true)" id="closeSave" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
				<button dojoType="dijit.form.Button" type="submit" id="save" onclick="saveEndpoint()"><%= LanguageUtil.get(pageContext, "Save") %></button>
			</span>
		</div>
	</div>
</div>
