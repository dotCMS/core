<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>

<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", PortletID.CONFIGURATION.toString()); %>
<%@ include file="/html/common/uservalidation.jsp"%>
<%@page import="com.dotcms.rest.api.v1.system.ConfigurationHelper"%>

<%
   final boolean hasAdminRole = user.isAdmin();
   final String screenLogo = company.getCity();
   final boolean screenLogoIsSet = screenLogo.startsWith("/dA");
   final String navLogo = company.getState();

   final boolean navLogoIsSet = UtilMethods.isSet(navLogo) && navLogo.startsWith("/dA");
   
   final boolean enterprise = (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
%>
<script type="text/javascript">
	dojo.require("dojox.widget.ColorPicker");
	dojo.require("dojo.parser"); // scan page for widgets and instantiate them

    function getTopNavDropZone() {
        return document.getElementById("topNav__drop-zone");
    }

	function setNewColor(val) {
        const topNavDropZone = getTopNavDropZone();

        const logo = topNavDropZone ? topNavDropZone.querySelector(".logo") : null;

        if (logo) {
            logo.style.backgroundColor = val;
        }
	}

	function bgColorStyler(val) {
		setNewColor(val);
		dojo.byId("bgColorBlock").style.background = val;
		dojo.byId("bgColor").value = val;
	}

	function PrimaryColorStyler(val) {
		dojo.byId("pColorBlock").style.background = val;
		dojo.byId("pColor").value = val;
	}

	function SecondaryColorStyler(val) {
		dojo.byId("sColorBlock").style.background = val;
		dojo.byId("sColor").value = val;
	}

	function imgSwap(val) {
		dojo.byId("imageBlock").style.backgroundImage = "url('" + val + "')";
		dojo.byId("bgURL").value = val;
	}

	dojo.addOnLoad(function () {
		bgColorStyler("<%= company.getSize() %>");
		PrimaryColorStyler("<%= company.getType()%>");
		SecondaryColorStyler("<%= company.getStreet()%>");
		imgSwap("<%= company.getHomeURL() %>");
	});

	function regenerateKeyProxy() {
		dijit.byId("regenerateKeyDialog").show();
	}

	function regenerateKeyProxyOk() {
		regenerateKey(function (value) {
			if (value) {
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

    function validateEmail() {

       let senderAndEmail = prompt("An e-mail will be sent from the input address to the current logged-in user address.", dijit.byId("companyEmailAddress").getValue());

       const data = {
          "senderAndEmail": senderAndEmail
       };

       dojo.xhrPost({
          url: "/api/v1/configuration/_validateCompanyEmail",
          handleAs: "json",
          postData: dojo.toJson(data),
          headers: {
             'Accept': 'application/json',
             'Content-Type': 'application/json;charset=utf-8',
          },
          load: function (code) {
             showDotCMSSystemMessage(`A test e-mail is being sent to ${senderAndEmail} in the background. You'll receive a notification.`, true);
          },
          error: function(error){
             if(error.response.data){
                let data = JSON.parse(error.response.data);
                showDotCMSSystemMessage(data.message, true);
             } else {
                showDotCMSSystemMessage(`Unable to validate or send test email to ${senderAndEmail}. `, true);
             }
          }
       });
    }

	(function prepareEventListeners() {

      setTimeout(() => {
         const topNavDropZone = getTopNavDropZone();
         const navBarLogoCheckboxWidget = dijit.byId('topNav_logo');
         const navBarLogoCheckbox = dojo.byId('topNav_logo');

         if (navBarLogoCheckbox && navBarLogoCheckbox.checked) {
            topNavDropZone.style.display = "block"
         }

         if (navBarLogoCheckboxWidget) {
            navBarLogoCheckboxWidget.on('change', handleTopNavLogoDisplay)             
         }

      }, 0)

      document.addEventListener('click', logoDelete)
      document.addEventListener('uploadComplete', uploadComplete)
	})();

	function handleTopNavLogoDisplay(checked) {
        const topNavDropZone = getTopNavDropZone();
		topNavDropZone.style.display = checked ? "block" : "none";
	}

	function logoDelete(event) {
      if(!event.target.matches('.logo__delete')) return;

		const logoContainer = event.target.parentElement;
		logoContainer.style.display = "none";

		const logo = logoContainer.querySelector("img");
      logo.remove();

      event.target.closest('.form__group').querySelector('input[type="hidden"]').value = ""
		logoContainer.nextElementSibling.style.display = "block";
	}

   function assetIsMaxLength(asset) {
      return asset.match(/[^\\\/]+(?=\.[\w]+$)|[^\\\/]+$/)[0].length > 40;
   }

    function setLogoAndContainerNodes({ dropZone,  logoNode, details, dropZoneLabel }) {
        const topNavDropZone = getTopNavDropZone();
        const logo = document.createElement("img");
        // Once we received a response add the image URL to the src attribute
        logo.src = details.asset;
        logo.classList.add('logo__image');

        // Grab the previous sibling and append the loo
        dropZone.previousElementSibling
        .querySelector(".logo__container")
        .append(logo);

        // Do we have an error? Remove it as it was a successful upload   
        if(dropZoneLabel.parentElement.querySelector('.error')) {
        dropZoneLabel.parentElement.querySelector('.error').remove()  
        }

        // Reset our values
        logoNode.style.display = "flex";
        dropZone.style.display = "none";
        dropZone.parentElement.querySelector('input[data-hidden]').value = details.asset;
        topNavDropZone.querySelector(".logo__container").style.display = 'flex'
    }

	function uploadComplete(event) {
      if(!event.target.matches('dot-asset-drop-zone')) return

		// Grab the dropzone element
		const dropZone = event.target;

		// create a new logo element to append it later

      const logoNode = dropZone.parentElement.querySelector('.logo');
      const dropZoneLabel = logoNode.closest('.form__group').querySelector('.drop-zone__label');

		// details from the dotAssets upload event
		const [details] = event.detail;

      const logoAndContainerData = {
          dropZone, 
          logoNode, 
          details,
          dropZoneLabel
      }

      if(assetIsMaxLength(details.asset)) {
         createMaxLengthError(dropZoneLabel);
         return;
      }
      setLogoAndContainerNodes(logoAndContainerData)
	}

   function createMaxLengthError(elem) {
      const span = document.createElement('span');
      span.classList.add('error');
      span.textContent = "Make sure the filename of the image has less than 50 characters."
      elem.insertAdjacentElement('afterend', span)
   }
</script>
<style type="text/css">
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

   .logo__container {
      width: 200px; 
      height: 100px; 
      padding: 1rem; 
      border-radius: 2px;
      display: flex;
   }

	.logo {
		position: relative;
      width: 200px;
      height: 100px;
      border: 1px solid lightgray;
      display: flex;
      justify-content: center;
      align-content: center;
      align-items: center;
      border-radius: 2px;
	}

	.logo:hover .logo__delete {
		display: flex;
	}

	.logo__delete {
      cursor: pointer;
      position: absolute;
      right: -10px;
      display: none;
      background: white;
      border: 0;
      border-radius: 50%;
      width: 22px;
      height: 22px;
      justify-content: center;
      box-shadow: 0px 0px 0px 3px rgba(0, 0, 0, 0.1);
      top: -10px;
      align-items: center;
	}

   .logo__image {
      object-fit: scale-down;
      width: 100%;
   }

   dot-asset-drop-zone {
      box-sizing: border-box;
      width: 200px;
      display: block;
   }

   dot-asset-drop-zone .dot-asset-drop-zone__indicators.drop .dot-asset-drop-zone__icon {
      display: none !important;
   }

   dot-asset-drop-zone .dot-asset-drop-zone__indicators.drop dot-progress-bar {
      padding: 1rem;
   }

   dot-asset-drop-zone .dot-asset-drop-zone__indicators {
      padding: 11px 0;
      position: static;
   }

   dot-asset-drop-zone .dot-asset-drop-zone__indicators .dot-asset-drop-zone__icon {
      display: flex !important;
      flex-direction: column;
      align-items: center;
   }

   dot-asset-drop-zone .dot-asset-drop-zone__indicators .dot-asset-drop-zone__icon mwc-icon {
      --mdc-icon-size: 48px;
   }

    dot-asset-drop-zone .dot-asset-drop-zone__indicators .dot-asset-drop-zone__icon span {
       margin-top: 0;
    }

    .drop-zone__label {
       font-weight: normal;
       margin-bottom: 1rem;
    }
    .error {
       margin-bottom: 1rem;
       display: block;
       font-size: .9rem;
       color: red;
    }

    .hint {
       margin-top: .5rem; 
       color: grey;
    }

    .form__group:last-child {
       margin-top: 2rem;
    }
    .logo-upload__container {
       margin-left: 10rem;
    }
</style>
<table class="listingTable">
   <tr>
      <th><%= LanguageUtil.get(pageContext, "basic-information") %></th>
   </tr>
   <tr>
      <td>
         <div class="flex">
            <div class="form-horizontal">
               <dl>
                  <dt><%= LanguageUtil.get(pageContext, "portal-url") %></dt>
                  <dd><input dojoType="dijit.form.TextBox" id="companyPortalUrl" name="companyPortalUrl" size="25" type="text" value="<%= company.getPortalURL() %>" style="width: 250px"></dd>
               </dl>
               <dl>
                  <dt><%= LanguageUtil.get(pageContext, "email-address") %></dt>
                  <dd>
                     <div class="inline-form">
                        <input dojoType="dijit.form.TextBox" id="companyEmailAddress" name="companyEmailAddress" placeholder="dotCMS Website <website@dotcms.com>" size="20" type="text" value="<%= company.getEmailAddress() %>" style="width: 250px">
                        <button id="companyEmailButton" dojoType="dijit.form.Button" type="button" iconClass="saveIcon" onclick="validateEmail()" >
                           <%= LanguageUtil.get(pageContext, "email-address-validate") %>
                        </button>
                     </div>
                  </dd>
               </dl>
               <dl>
                  <dt><%= LanguageUtil.get(pageContext, "cluster-id") %></dt>
                  <dd>
                     <span><%= ConfigurationHelper.getClusterId() %></span>
                  </dd>
               </dl>
               <dl style="display: none;">
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
                  <dd>
                     <div id="imageBlock" style="width:250px; height:170px; border:1px solid #b3b3b3;background-repeat:no-repeat; background-size:100% 100%;"></div>
                  </dd>
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
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-1.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-1-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-2.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-2-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-3.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-3-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-4.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-4-sm.jpg" width="75" height="47"></a></div>
                        </td>
                     </tr>
                     <tr>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-5.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-5-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-6.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-6-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-7.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-7-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-8.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-8-sm.jpg" width="75" height="47"></a></div>
                        </td>
                     </tr>
                     <tr>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-9.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-9-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-10.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-10-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-11.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-11-sm.jpg" width="75" height="47"></a></div>
                        </td>
                        <td>
                           <div><a href="#" onclick="imgSwap(''); dijit.byId('bgPicker').hide();"> <img src="/html/images/backgrounds/bg-no-sm.jpg" width="75" height="47"></a></div>
                        </td>
                     </tr>
                  </table>
               </div>
            </div>
            <div class="logo-upload__container">
               <div class="form__group">
               <h3 class="drop-zone__label"><%= LanguageUtil.get(pageContext, "loginlogo.label") %></h3>
               <div class="logo">
                  <button class="logo__delete">&times;</button>
                  <div class="logo__container">
                     <% if(screenLogoIsSet) { %>
                        <img class="logo__image" border="1" hspace="0" src="<%= screenLogo %>" vspace="0" />
                     <% } else { %>
                        <img class="logo__image" border="1" hspace="0" src="<%= IMAGE_PATH %>/company_logo?img_id=<%= company.getCompanyId() %>&key=<%= ImageKey.get(company.getCompanyId()) %>" vspace="0" />
                     <% } %>
                  </div>
               </div>
               <dot-asset-drop-zone id="dot-asset-drop-zone-main" style="display: none;" drop-files-text="Drop Image" upload-file-text="Uploading Image..." display-indicator="true"></dot-asset-drop-zone>
               <input type="hidden" name="loginScreenLogoInput" id="loginScreenLogoInput" data-hidden="logo-input" value="<%= ( screenLogoIsSet ? screenLogo : "" ) %>">
               <p class="hint"><%= LanguageUtil.get(pageContext, "login-logo.hint") %></p>
               </div>
               <br />
               <% if ( enterprise ) { %>
               <div class="form__group">
                  <label for="topNav_logo">
                  <input dojoType="dijit.form.CheckBox" type="checkbox" name="topNav" id="topNav_logo" <%= ( navLogoIsSet ? "checked" : "" ) %> />
                     <%= LanguageUtil.get(pageContext, "navlogo-checkbox.label") %>
                  </label>
                  <br />
                  <p class="hint"><%= LanguageUtil.get(pageContext, "navlogo-checkbox.hint") %></p>
                  <div id="topNav__drop-zone" style="display: none;">
                     <h3 class="drop-zone__label"><%= LanguageUtil.get(pageContext, "navlogo.label") %></h3>
                     <div class="logo" <%= ( navLogoIsSet ? "style='display: flex;'" : "style='display: none;'" ) %>>
                        <button class="logo__delete">&times;</button>
                         <%
                           if(navLogoIsSet)  {
                        %>            
                          <div class="logo__container">
                              <img class="logo__image" src="<%= navLogo %>"/>
                           </div>
                        <%
                           } else {
                        %>
                           <div class="logo__container" style="display: none;"></div>
                        <%
                           }
                        %>
                     </div>
                     <dot-asset-drop-zone id="dot-asset-drop-zone-navbar" drop-files-text="Drop Image" upload-file-text="Uploading Image..." display-indicator="true" <%= ( navLogoIsSet ? "style='display: none;'" : "style='display: block;'" ) %>></dot-asset-drop-zone>
                     <input type="hidden" name="topNavLogoInput" id="topNavLogoInput" data-hidden="logo-input" value="<%= ( navLogoIsSet ? navLogo : "" ) %>">	
                     <p class="hint"><%= LanguageUtil.get(pageContext, "navlogo.hint") %></p>
                  </div>
               </div>
               <% } %>
            </div>
         </div>
         
      </td>
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
                     <input type="hidden" 
                     name="currentCompanyTimeZoneId" 
                     id="currentCompanyTimeZoneId" 
                     value="<%=company.getTimeZone().getID() %>" />
                     <select style="width: 250px" 
                        dojoType="dijit.form.FilteringSelect" 
                        value="<%=company.getTimeZone().getID() %>"
                        id="companyTimeZoneId" 
                        queryExpr='*${0}*' 
                        autoComplete="false" 
                        name="companyTimeZoneId" 
                        style="width: 250px;">
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