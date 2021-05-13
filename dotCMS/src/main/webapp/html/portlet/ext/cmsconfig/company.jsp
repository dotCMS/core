<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>

<% request.setAttribute("requiredPortletAccess", PortletID.CONFIGURATION.toString()); %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@page import="com.dotcms.rest.api.v1.system.ConfigurationHelper"%>
<%
final boolean hasAdminRole = user.isAdmin();
%>

<script type="text/javascript">

    dojo.require("dojox.widget.ColorPicker");
    dojo.require("dojo.parser");// scan page for widgets and instantiate them

    function bgColorStyler (val) {
        dojo.byId("bgColorBlock").style.background = val;
        dojo.byId("bgColor").value = val;
    }

    function PrimaryColorStyler (val) {
        dojo.byId("pColorBlock").style.background = val;
        dojo.byId("pColor").value = val;
    }

    function SecondaryColorStyler (val) {
        dojo.byId("sColorBlock").style.background = val;
        dojo.byId("sColor").value = val;
    }

    function imgSwap (val) {
        dojo.byId("imageBlock").style.backgroundImage = "url('" + val + "')";
        dojo.byId("bgURL").value = val;
    }

    dojo.addOnLoad(function () {
        bgColorStyler('<%= company.getSize() %>');
        PrimaryColorStyler('<%= company.getType()%>')
		SecondaryColorStyler('<%= company.getStreet()%>')
        imgSwap('<%= company.getHomeURL() %>');
    });

	function regenerateKeyProxy() {
		dijit.byId("regenerateKeyDialog").show();
	}

	function regenerateKeyProxyOk() {

		regenerateKey(function (value) {
			if(value){
				dojo.byId("key-digest").innerHTML = value;
				dijit.byId("regenerateKeyDialog").hide();
			}
		});
		return true;
	}

	function regenerateKeyProxyCancel() {
		dijit.byId("regenerateKeyDialog").hide();
		return true;
	}

	const overrideCheckbox = document.getElementById('override_logo');
	const overrideDropZone = document.getElementById('override__drop-zone');
	const logoDeleteButton = document.getElementById('logo--delete');
	const dotAssetDropZoneMain = document.getElementById('dot-asset-drop-zone-main');

	overrideCheckbox.addEventListener('change', toggleOverrideLogo)
	logoDeleteButton.addEventListener('click', logoDelete)

	function toggleOverrideLogo(e) {
    const isChecked = e.target.checked;
		overrideDropZone.style.display = isChecked ? 'block' : 'none'
}

function logoDelete(event, b) {
	event.target.parentElement.remove();
	dotAssetDropZoneMain.style.display = 'block'
}

</script>

<style type="text/css">
	dot-asset-drop-zone {
		width: 40rem;
	}
	dot-asset-drop-zone .dot-asset-drop-zone__indicators {
			position: static;
	}

	.listingTable__form-control {
		display: flex;
		justify-content: space-between;
		margin-bottom: 1rem;
	}

	.listingTable__form-control label {
		align-self: center;
    margin-left: auto;
    margin-right: 1.5rem;
	}

	.listingTable__form-control h3 {
		margin-left: auto;
		margin-right: 1.5rem;
		font-weight: normal;
	}

	.listingTable__form-control span.clusterid {
			display: block;
			width: 250px;
	}

	.listingTable__form {
		width: 33rem;
	}

	#bgButton .colorIcon {
		display: none;
	}
</style>

<table class="listingTable">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "basic-information") %></th>
    </tr>
    <tr>
        <td style="display: flex">
        	<div class="form-horizontal">
	            <dl>
	                <dt><%= LanguageUtil.get(pageContext, "portal-url") %></dt>
	                <dd><input dojoType="dijit.form.TextBox" id="companyPortalUrl" name="companyPortalUrl" size="25" type="text" value="<%= company.getPortalURL() %>" style="width: 250px"></dd>
							</dl>
				<dl>
	                <dt><%= LanguageUtil.get(pageContext, "mail-domain") %></dt>
	                <dd><input dojoType="dijit.form.TextBox" id="companyMX" name="companyMX" size="25" type="text" value="<%= company.getMx() %>" style="width: 250px"></dd>
				</dl>
				<dl>
	                <dt><%= LanguageUtil.get(pageContext, "email-address") %></dt>
	                <dd><input dojoType="dijit.form.TextBox" id="companyEmailAddress" name="companyEmailAddress" size="20" type="text" value="<%= company.getEmailAddress() %>" style="width: 250px"></dd>
				</dl>

				<dl>
					<dt><%= LanguageUtil.get(pageContext, "cluster-id") %></dt>
					<dd>
						 <span><%= ConfigurationHelper.getClusterId() %></span>
					</dd>
				</dl>

				<dl>
	                <dt><%= LanguageUtil.get(pageContext, "background.color") %></dt>
	                <dd style="position:relative;">
	                    <div id="bgColorBlock" style="position:absolute;left:75px;top: 7px;width:18px;height:18px;"></div>
	                    <div class="inline-form">
		                    <input id="bgColor" dojoType="dijit.form.TextBox" name="companySize" size="5" type="text" value="<%= company.getSize() %>" style="width: 250px">
		                    <button id="bgButton" dojoType="dijit.form.Button" type="button" iconClass="colorIcon">
								<%= LanguageUtil.get(pageContext, "color.picker") %>
		                        <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
		                            dijit.byId("bgColorPicker").show();
		                        </script>
		                    </button>
		                 </div>
	                </dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "primary.color") %></dt>
					<dd style="position:relative;">
						<div id="pColorBlock" style="position:absolute;left:75px;top: 7px;width:18px;height:18px;"></div>
						<div class="inline-form">
							<input id="pColor" dojoType="dijit.form.TextBox" name="companySize" size="5" type="text" value="<%= company.getType() %>" style="width: 250px">
							<button id="pButton" dojoType="dijit.form.Button" type="button" iconClass="colorIcon">
								<%= LanguageUtil.get(pageContext, "color.picker") %>
								<script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
		                            dijit.byId("pColorPicker").show();
		                        </script>
							</button>
						</div>
					</dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "secondary.color") %></dt>
					<dd style="position:relative;">
						<div id="sColorBlock" style="position:absolute;left:75px;top: 7px;width:18px;height:18px;"></div>
						<div class="inline-form">
							<input id="sColor" dojoType="dijit.form.TextBox" name="companySize" size="5" type="text" value="<%= company.getStreet() %>" style="width: 250px">
							<button id="sButton" dojoType="dijit.form.Button" type="button" iconClass="colorIcon">
								<%= LanguageUtil.get(pageContext, "color.picker") %>
								<script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
		                            dijit.byId("sColorPicker").show();
		                        </script>
							</button>
						</div>
					</dd>
				</dl>
				<dl>
	                <dt><%= LanguageUtil.get(pageContext, "background.image") %></dt>
	                <dd>
	                	<div class="inline-form">
		                    <input id="bgURL" dojoType="dijit.form.TextBox" name="companyHomeUrl" size="25" type="text" value="<%= company.getHomeURL() %>" style="width: 250px">
		                    <button id="buttonTwo" dojoType="dijit.form.Button" type="button" iconClass="bgIcon">
								<%= LanguageUtil.get(pageContext, "backgrounds") %>
		                        <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
		                            dijit.byId("bgPicker").show();
		                        </script>
		                    </button>
		                </div>
	                </dd>
	          </dl>
	          <dl>
	          		<dt>&nbsp;</dt>
	                <dd><div id="imageBlock" style="width:250px; height:170px; border:1px solid #b3b3b3;background-repeat:no-repeat; background-size:100% 100%;"></div></dd>
	          </dl>

				<div id="bgColorPicker" data-dojo-type="dijit.Dialog" title="Background Color Picker">
					<div id="bgPickerLive" dojoType="dojox.widget.ColorPicker"
						 webSafe="false"
						 liveUpdate="true"
						 value="<%= company.getSize() %>"
						 onChange="bgColorStyler(arguments[0])">
					</div>
				</div>

				<div id="pColorPicker" data-dojo-type="dijit.Dialog" title="Primary Color Picker">
					<div id="pPickerLive" dojoType="dojox.widget.ColorPicker"
						 webSafe="false"
						 liveUpdate="true"
						 value="<%= company.getType() %>"
						 onChange="PrimaryColorStyler(arguments[0])">
					</div>
				</div>

				<div id="sColorPicker" data-dojo-type="dijit.Dialog" title="Secondary Color Picker">
					<div id="sPickerLive" dojoType="dojox.widget.ColorPicker"
						 webSafe="false"
						 liveUpdate="true"
						 value="<%= company.getStreet() %>"
						 onChange="SecondaryColorStyler(arguments[0])">
					</div>
				</div>

				<div id="bgPicker" data-dojo-type="dijit.Dialog" title="Backgrounds">
					<table class="bgThumbnail">
						<tr>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-1.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-1-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-2.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-2-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-3.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-3-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-4.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-4-sm.jpg" width="75" height="47"></a></div></td>
						</tr>
						<tr>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-5.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-5-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-6.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-6-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-7.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-7-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-8.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-8-sm.jpg" width="75" height="47"></a></div></td>
						</tr>
						<tr>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-9.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-9-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-10.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-10-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-11.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-11-sm.jpg" width="75" height="47"></a></div></td>
							<td><div><a href="#" onclick="imgSwap(''); dijit.byId('bgPicker').hide();"> <img src="/html/images/backgrounds/bg-no-sm.jpg" width="75" height="47"></a></div></td>
						</tr>
					</table>
				</div>
			</div>
			  <div style="margin-left: 10rem;">
						<h3 style="font-weight: normal; margin-bottom: 1rem;">Login Screen Logo</h3>
            <div style="position: relative; max-width: 200px;">
							<button style="position: absolute; right: 0;" id="logo--delete">delete</button>
							<img style="max-width: 200px;" border="1" hspace="0" src="<%= IMAGE_PATH %>/company_logo?img_id=<%= company.getCompanyId() %>&key=<%= ImageKey.get(company.getCompanyId()) %>" vspace="0">
						</div>
						<dot-asset-drop-zone id="dot-asset-drop-zone-main" style="display: none;" drop-files-text="Drop Image" upload-file-text="Uploading Image..." display-indicator="true"></dot-asset-drop-zone>
						<p style="margin-top: 1rem; color: grey;">This is the logo used for the site lorem ipsum dolor sit amet
hello world.</p> <br>
							
							<div style="margin-top: 2rem;">
								<label for="override_logo">
								<input type="checkbox" name="override" id="override_logo">
								Overrider Navbar Logo
							</label> <br>
							<p style="margin-top: 1rem; color: grey;">If you want to override the main logo check this option and
upload an image lorem ipsum.</p>
								<div id="override__drop-zone" style="display: none;">
									<h3 style="font-weight: normal; margin-bottom: 1rem;">Navbar Logo</h3>
								<dot-asset-drop-zone id="dot-asset-drop-zone" drop-files-text="Drop Image" upload-file-text="Uploading Image..." display-indicator="true"></dot-asset-drop-zone>
								<p style="margin-top: 1rem; color: grey;">The image must be 300x300 px lorem ipsum dolor sit amet</p>
								</div><!-- /.override__drop-zone -->
							</div>
        </div>
        </td>
        <!-- <td valign="top">
            <img style="max-width: 300px;" border="1" hspace="0" src="<%= IMAGE_PATH %>/company_logo?img_id=<%= company.getCompanyId() %>&key=<%= ImageKey.get(company.getCompanyId()) %>" vspace="0"><br>
            <form action="/api/config/saveCompanyLogo" enctype="multipart/form-data" id="companyLogoForm" name="companyLogoForm" method="post">
            	<div style="margin-top:32px;">
                    <div style="margin-top:16px;"><%= LanguageUtil.get(pageContext, "File") %>  : <input type="file" id="logoFile" name="logoFile"></div>
                    <div style="margin-top:16px;">
                        <button  dojoType="dijit.form.Button" onClick="uploadCompanyLogo();" iconClass="saveIcon">
                            <%= LanguageUtil.get(pageContext, "upload-image") %>
                        </button>
                    </div>
            	</div>
            </form>
        </td> -->
    </tr>
</table>

<div class="buttonRow" style="margin-bottom: 60px;">
    <button dojoType="dijit.form.Button" onclick="saveCompanyBasicInfo();" type="button" id="basicSubmitButton" iconClass="saveIcon">
        <%= LanguageUtil.get(pageContext, "save") %>
    </button>
</div>



<table class="listingTable">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "locale") %></th>
    </tr>
    <tr>
        <td>
        	<div class="form-horizontal">
	            <dl>
	                <dt><%= LanguageUtil.get(pageContext, "language") %></dt>
	                <dd>
	                    <%
	                        User defuser=APILocator.getUserAPI().getDefaultUser();
	                    %>
	                    <select style="width: 250px" dojoType="dijit.form.FilteringSelect"  value="<%=defuser.getLocale().getLanguage()+ "_" + defuser.getLocale().getCountry()%>"
	                            id="companyLanguageId" name="companyLanguageId" style="width: 250px;">
	                        <%
	                            Locale[] locales = LanguageUtil.getAvailableLocales();
	                            for (int i = 0; i < locales.length; i++) {
	                        %>
	                        <option  value="<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>"><%= locales[i].getDisplayName(locale) %></option>
	                        <%}%>
	                    </select>
	                </dd>
				</dl>
				<dl>
	                <dt><%= LanguageUtil.get(pageContext, "time-zone") %></dt>
	                <dd>
	                    <span id="userTimezoneWrapper">
							<input type="hidden" name="currentCompanyTimeZoneId" id="currentCompanyTimeZoneId" value="<%=company.getTimeZone().getID() %>" />
	                        <select style="width: 250px" dojoType="dijit.form.FilteringSelect" value="<%=company.getTimeZone().getID() %>"
	                                id="companyTimeZoneId" name="companyTimeZoneId" style="width: 250px;">
	                            <% String[] ids = TimeZone.getAvailableIDs();
	                                Arrays.sort(ids);
	                                for(String id : ids) {
	                                    TimeZone tmz=TimeZone.getTimeZone(id);%>
	                                <option value="<%= id %>"><%= tmz.getDisplayName(locale) %> (<%= tmz.getID() %>) </option>
	                            <% }%>
	                        </select>
	                    </span>
	                </dd>
	            </dl>
           </div>
		</td>
	</tr>
</table>

<div class="buttonRow" style="margin-bottom: 60px;">
    <button dojoType="dijit.form.Button" onclick="saveCompanyLocaleInfo();" type="button" id="locateSubmitButton" iconClass="saveIcon">
        <%= LanguageUtil.get(pageContext, "save") %>
    </button>
</div>

<table class="listingTable">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "security") %></th>
    </tr>
    <tr>
        <td>
        	<div class="form-horizontal">
	            <dl>
	                <dt><%= LanguageUtil.get(pageContext, "authentication-type") %></dt>
	                <dd>
	                    <select dojoType="dijit.form.FilteringSelect"  value="<%= company.getAuthType()%>"  id="companyAuthType" name="companyAuthType" style="width: 250px">
	                        <option value="<%= Company.AUTH_TYPE_EA %>"><%= LanguageUtil.get(pageContext, "email-address") %></option>
	                        <option value="<%= Company.AUTH_TYPE_ID %>"><%= LanguageUtil.get(pageContext, "user-id") %></option>
	                    </select>
	                </dd>
	            </dl>
				<% if(userIsAdmin){  %>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "key-digest") %></dt>
					<dd>
						<div class="inline-form">
							<span id="key-digest"><%= company.getKeyDigest() %></span> &nbsp; &nbsp;
							<button id="regenKeyButton" dojoType="dijit.form.Button" type="button" iconClass="saveIcon" onclick="regenerateKeyProxy()" >
								<%= LanguageUtil.get(pageContext, "key-digest-regenerate") %>
							</button>
						</div>
					</dd>
				</dl>
				<% } %>
	        </div>
	    </td>
	</tr>
</table>

<div class="buttonRow" style="margin-bottom: 40px;">
    <button dojoType="dijit.form.Button" onclick="saveCompanyAuthTypeInfo();" type="button" id="securitySubmitButton" iconClass="saveIcon">
        <%= LanguageUtil.get(pageContext, "save") %>
    </button>
</div>

<div id="regenerateKeyDialog" dojoType="dijit.Dialog" style="display:none;width:500px;vertical-align: middle; " draggable="true" title="<%= LanguageUtil.get(pageContext, "key-digest-regenerate-prompt") %>" >

	<span class="ui-confirmdialog-message" >
		<%= LanguageUtil.get(pageContext, "key-digest-regenerate-warning") %>
	</span>

    <div>
        <table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">
          <tr>
			<td style="width:50%;text-align: right">
				<button id="cancelButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: regenerateKeyProxyCancel">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
				</button>
			</td>
			<td style="width:50%;text-align: left">
				<button id="okButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: regenerateKeyProxyOk">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "key-digest-regenerate")) %>
				</button>
			</td>
		  </tr>
        </table>
    </div>

</div>
