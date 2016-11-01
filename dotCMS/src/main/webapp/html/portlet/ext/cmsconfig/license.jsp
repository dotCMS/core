<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.struts.MultiMessageResources"%>
<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", "9"); %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Map"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="java.text.SimpleDateFormat"%>

<%
    String licenseTab = "/c/portal/layout?p_l_id=" + layoutId + "&p_p_id=9&tab=licenseTab";

    String error=null;
    String message=null;
    String newLicenseMessage="";
    String newLicenseURL="";

    if ( session.getAttribute( "applyForm" ) != null && session.getAttribute( "applyForm" ).equals( Boolean.TRUE ) ) {
        error = LicenseUtil.processForm( request );
    }

    String serverId = "";
    boolean badId=false;
    try {
        LicenseUtil.getLevel();
        serverId = LicenseUtil.getDisplayServerId();
    }
    catch(Exception ex) {
        badId=true;
    }


    boolean isCommunity =LicenseUtil.getLevel()==100;

    String expireString = "unknown";
    Date expires = null;
    try{
        expires = LicenseUtil.getValidUntil();
        SimpleDateFormat format =
                new SimpleDateFormat("MMMM d, yyyy");
        expireString=  format.format(expires);
    }
    catch(Exception e){

    }
    boolean expired = (expires !=null && expires.before(new Date()));
    boolean isPerpetual = LicenseUtil.isPerpetual();

    String requestCode=(String)request.getAttribute("requestCode");

    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    SimpleDateFormat dfOut = new SimpleDateFormat("MMM dd, yyyy");

%>



<script type="text/javascript">

	dojo.declare("dotcms.dijit.cmsconfig.LicenseAdmin", null, {

	    isCommunity		:"<%=isCommunity%>",
	    
	    requestTrial : function(){
	    	var data = {"licenseLevel":"400","licenseType":"trial"};
	   		
	   	    dojo.xhrPost({
	   	        url: "/api/license/requestCode/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(code) {
					dojo.byId("trialLicenseRequestCode").value=code;
					dojo.byId("trialLicenseForm").submit();
	   	        }
	   	    });
	    },
	
	    doCodeRequest : function () {
	    	
	    	if(dijit.byId("license_type").getValue()==undefined || dijit.byId("license_type").getValue()=="--"){
	    		//console.log("code request: " + dijit.byId("license_type").getValue());
	    		dojo.byId("licenseCode").value="";
	    		return;
	    	}
	    	if(dijit.byId("license_level").getValue() == "100"){
	    		//console.log("code request: " + dijit.byId("license_level").getValue());
	    		dojo.byId("licenseCode").value="";
	    		return;
	    	}
	    	
	    	var data = {"licenseLevel":dijit.byId("license_level").getValue(),"licenseType":dijit.byId("license_type").getValue()};
	   	    dojo.xhrPost({
	   	        url: "/api/license/requestCode/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(code) {

					dojo.byId("licenseCode").value=code;
					//dijit.byId("getLicenseCodeDia").show();
					
					
	   	        }
	
	   	    });
	   	
	    },

	    showCurrentCustomer : function(){
	    	//console.log("showing");
			dojo.removeClass("currentCustomer", "hidden");
			dojo.addClass("pasteTrial", "hidden");
			dojo.addClass("currentCustomerBtn", "btn-warning");
			
			this.hideCurrentEvent = dojo.connect(dojo.byId("currentCustomerBtn"), "onclick", function(evt){
			    licenseAdmin.hideCurrentCustomer();
			});
	    	
	    },
	    
	    
	    hideCurrentCustomer : function(){
	    	//console.log("hiding");
			dojo.addClass("currentCustomer", "hidden");
			dojo.removeClass("pasteTrial", "hidden");
			dojo.removeClass("currentCustomerBtn", "btn-warning");
			dojo.removeClass("generateCode", "hidden");
			
			this.showCurrentEvent=dojo.connect(dojo.byId("currentCustomerBtn"), "onclick", function(evt){
			    licenseAdmin.showCurrentCustomer();
			});
	    	
	    },
	    
	    
	    
	    resetLicense :function () {

	    	var data = {"licenseText":"reset"};
	   	    dojo.xhrPost({
	   	        url: "/api/license/resetLicense/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(message) {
	   	        	//licenseAdmin.refreshLayout();
	   	        },
	   	     	error: function(error){
	   	     		showDotCMSSystemMessage("ERROR:" + error,true);
	   	     		//licenseAdmin.refreshLayout();
	   	     	
	   	     	}
	   	    });
	   	 	setTimeout(licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-reset")) %>'),3000);
	   	 
	   	},
	   	
	   	
	   	refreshLayout : function(text){
	   		if(dijit.byId('uploadDiaWindow')){
	   			dijit.byId('uploadDiaWindow').hide();
	   		}
	   		if(dijit.byId('uploadgetLicenseCodeDiaDiaWindow')){
	   			dijit.byId('getLicenseCodeDia').hide();
	   		}
	   		
	   		
	   		
	   		loadLicenseTabMessage(text);
	   		//This code will update the license info in the top of the page
	   		dojo.xhrGet({ // 
	   	        url: "<%=licenseTab%>", 
	   	        handleAs: "text",

	   	        // The LOAD function will be called on a successful response.
	   	        load: function(response, ioArgs) {
	   	          var text = response.substring(response.indexOf('<div id="admin-site-tools-div">')+31);
	   	          text = text.substring(0, text.indexOf("</div>"));
	   	          dojo.byId("admin-site-tools-div").innerHTML = text; 
	   	          return text;
	   	        },

	   	        // The ERROR function will be called in an error case.
	   	        error: function(response, ioArgs) {
	   	        	showDotCMSSystemMessage("ERROR HTTP status code: " + ioArgs.xhr.status,true);
   	        		console.log("HTTP status code: ", ioArgs.xhr.status);
   	        		return response;
	   	          }
	   	        });
	   		
	   	},
	   	
	    	
	   	
	    doLicensePaste :function () {

	   		
	    	var data;

	   		if(dojo.byId("licenseCodePasteField").value==undefined || dojo.byId("licenseCodePasteField").value.length>0){
	   			 data = {"licenseText":dojo.byId("licenseCodePasteField").value};
	   		}
	   		else{
	   			console.log("test");
	   			 data = {"licenseText":dojo.byId("licenseCodePasteFieldTwo").value};
	   		}
	   	 		
	   		
	   		
	   	    dojo.xhrPost({
	   	        url: "/api/license/applyLicense/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(message) {
	   	        	
	   	        	if(! message ){
	   	        		licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-applied")) %>');
	   	        	}
	   	        	else{
	   	        		showDotCMSSystemMessage("ERROR: " + message,true);
	   	        		console.log("message:" + message);
	   	        	}
	   	        },
	   	     	error: function(error){
	   	     		
	   	     	
	   	     	}
	   	    });
	   	},

		 levelName : function(level) {
		    switch(level) {
		        case 100: return "Community";break;
		        case 200: return "Professional"; break;
		        case 300: return "Enterprise"; break;
		        case 400: return "Prime"; break;
		        default: return "-";
		    }
		},
        
        typeName : function (type) {
            switch(type) {
                case "dev": return "Development"; break;
                case "prod": return "Production"; break;
                case "trial": return "Trial"; break;
                default: return "-";
            }
        },

         currentServerId :'<%= serverId %>',

         load : function () {
        	if(dojo.byId("repotableBody") ==undefined){
        		return;
        	}
            dojo.empty("repotableBody");
            dojo.xhrGet({
                url: "/api/license/all/",
                handleAs: "json",
                load: function(data) {

                    dojo.forEach(data, function(lic) {
                        var row;

                        if(lic.serverid===licenseAdmin.currentServerId) {
                            row=dojo.create("tr",{"class":"current_server_row selected"},dojo.byId("repotableBody"),"first");
                        }
                        else {
                            row=dojo.create("tr",null,dojo.byId("repotableBody"));
                        }

                        var serial=lic.id;
                        var optd=dojo.create("td",{"nowrap":"true"},row);

                        if(lic.serverid==licenseAdmin.currentServerId  ) {
                        	dojo.addClass(dojo.byId("generateCode"), "hidden");
                            dojo.create("span",{"class":"unlockIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-free") )%>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.free()"},optd));
                        
                        } else if(!lic.available) {    
                        	dojo.addClass(dojo.byId("generateCode"), "hidden");
                            dojo.create("span",{"class":"unlockIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-free") )%>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.free('"+lic.id+"','"+lic.fullserverid+"')"},optd));	
                        } else if(lic.available) {

                            dojo.create("span",{"class":"downloadIcon",title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-pick")) %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.pick('"+serial+"')"},optd));

                            dojo.create("span",{"class":"deleteIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-del")) %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.del('"+serial+"')"},optd));
                        }

                        dojo.create("td", { innerHTML: (!lic.serverid || lic.serverid === "") ? "Available" : lic.serverid + (lic.available ? " (Available)" : "")}, row);
                        dojo.create("td", { innerHTML: lic.idDisplay}, row);
                        dojo.create("td", { innerHTML: !lic.available || lic.serverid ? lic.lastping : ""}, row);
                        dojo.create("td", { innerHTML: lic.perpetual ? "Perpetual" : lic.validUntil}, row);
                        dojo.create("td", { innerHTML: licenseAdmin.levelName(lic.level)}, row);
                        dojo.create("td", { innerHTML: licenseAdmin.typeName(lic.licenseType)}, row);
                    });
                    if(data.length==0){
                    	var row=dojo.create("tr",null,dojo.byId("repotableBody"));
                    	var optd=dojo.create("td",{"colspan":100,"align":"center"},row);
                    	optd.innerHTML="<a href='#' onclick=\"dijit.byId('uploadDiaWindow').show()\"><%= LanguageUtil.get(pageContext, "No-Results-Found") %></a>";
                    	licenseAdmin.hideCurrentCustomer();
                    }
                    else{
                    	licenseAdmin.showCurrentCustomer();
                    }
                    
                } 
            
            

            });
        	<%if(UtilMethods.isSet(request.getParameter("message"))){ %>
    			showDotCMSSystemMessage('<%= UtilMethods.javaScriptify(request.getParameter("message")) %>');
    		<%} %>
        },

        del:  function (serial) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-delete")) %>')) return;
            dojo.xhrDelete({
                url: "/api/license/delete/id/"+serial+"/",
                load: function() {
                	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-deleted")) %>');
                }
            });
        },

        pick:  function (serial) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-pick")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/pick/serial/"+serial+"/",
                load: function() {
                	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-applied")) %>');
                }
            });
        },

        free: function () {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-free")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/free/",
                load: function() {
                	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-freed") )%>');
                }
            });
            
        },

        free: function (serial, serverid) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-free-remote")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/free/serial/"+serial+"/serverid/"+serverid+"/",
                load: function() {
                	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-freed") )%>');
                }
            });
            
        },
        
        doPackUpload : function () {

        	if(!dojo.byId("uploadPackFile").value || dojo.byId("uploadPackFile").value.length<1){
        		return;
        	}

        	dojo.io.iframe.send({

	   	     	form: dojo.byId("uploadPackForm"),
	   	        load: function(message, ioArgs) {
	   	        	console.log(message);
	   	        	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "licenses-uploaded") )%>');
	   	        },
	   	     	error: function(error){
	   	     		//showDotCMSSystemMessage("ERROR:" + error,true);
	   	     	licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "licenses-uploaded") )%>');
	   	     	
	   	     	}
	   	    });
        	
            //dojo.byId('uploadPackForm').submit();
            return false;
        }
	});
	

		var licenseAdmin = new dotcms.dijit.cmsconfig.LicenseAdmin({});

	dojo.require("dojo.io.iframe");
	dojo.ready(licenseAdmin.load);
	
</script>




<style type="text/css">
    .content-wrapper{width:80%;margin: 30px auto;}
    .blankTable,  .blankTable th, .blankTable td {
		border: 0px solid #ffffff;
    }
    .blankTable {width: 100%;}
    .blankTable td {padding: 8px 10px;}
    .blankTable th {font-weight: bold;background: #fff;}
    .alert {
    	border-radius: 4px;
    	padding: 25px 20px 20px 20px;
    	border: 1px solid transparent;
    }
    .alert-info {
    	color: #31708f;
		background-color: #d9edf7;
		border-color: #bce8f1;
    }
    .alert-warning {
    	color: #8a6d3b;
		background-color: #fcf8e3;
		border-color: #faebcc;
    }
    .hidden{display: none;}
    hr {
    	margin: 40px 0;
    }
    .stepHeader {
    	color: #31708f;
    	background-color: #d9edf7;
    	border: 1px solid #bce8f1;
    	padding:5px 8px;
    	font-size: 16px;
    	font-weight: bold;
    }
    .stepHeader small {
    	font-weight: 300;
    	font-size: 12px;
    }
    .stepBody {
    	border: 1px solid #bce8f1;
    	padding: 15px 10px;
    	border-top: 0;
    	min-height: 230px;
    }
    .btn{
    	display: inline-blcok;
    	padding: 8px 12px;
    	font-size: 1.2em;
    	font-weight: 100;
    	border-radius: 4px;
    	text-decoration: none;
    	margin-right: 20px;
    	-webkit-transition: background 200ms ease-in 200ms; /* property duration timing-function delay */
	    -moz-transition: background 200ms ease-in 200ms;
	    -o-transition: background 200ms ease-in 200ms;
	    transition: background 200ms ease-in 200ms;
	    background: #f2f2f2;
    	color: #555555;
    }
    .btn:hover{
    	background: #DDD;
    }
    
    .btn-info {
    	background: #088AC8;
    	color: #ffffff;
    }
    .btn-info:hover{
    	background: #22A1D7;
    }
    .btn-warning {
    	background: #f0ad4e;
    	color: #ffffff;
    }
    .btn-warning:hover{
    	background: #ec971f;
    }
</style>



<form name="trialLicenseForm" id="trialLicenseForm" method="POST" target="trialRequestWindow" action="https://dotcms.com/licensing/request-a-license-3/">
	<input type="hidden" value="" name="trialLicenseRequestCode" id="trialLicenseRequestCode">
</form>	



<div class="portlet-wrapper">
	


	<table class="listingTable layoutTable" style="font-size:12px;margin-top:20px;">
	   <tr>
			<th><%= LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.EXT_LICENSE_MANAGER") %></th>
		</tr>
		<tr>
			<td>
				<div class="content-wrapper">
					
					<!-- CURRENT LICENSE INFO -->
					<table border="0" width="100%" style="margin:20px 0;border:1px solid silver;border-collapse: collapse;">
						 <tr>
					        <th colspan="2">
					        	<% if(!isCommunity){  %>  
									<div style="float:right;font-weight:normal;">
										    <button data-dojo-type="dijit.form.Button" onClick="licenseAdmin.resetLicense()" iconClass="resetIcon">
										        <%= LanguageUtil.get(pageContext, "license-bad-id-button") %>
										    </button>
									</div>
								<%} %>
					        	<%= LanguageUtil.get(pageContext, "license-current-info") %>
					        </th>
					    </tr>
						<tr>
							<td width="25%" nowrap="true">
								<%= LanguageUtil.get(pageContext, "license-level") %>
							</td>
							<td>
								<% if(isCommunity){  %> 
									<a href="/html/blank.jsp" 
											target="trialRequestWindow" 
											onclick="licenseAdmin.requestTrial()" 
											>
										<b><%= LicenseUtil.getLevelName()  %></b>
									</a>
									<div style="float:right;border-bottom:2px dotted silver;">
												<a href="/html/blank.jsp" 
											target="trialRequestWindow" 
											onclick="licenseAdmin.requestTrial()" 
											style="color:#555;">
									
									
										<%= LanguageUtil.get(pageContext, "request-trial-license") %>
									</a>
									</div></a>
								<%}else{ %>
									<b><%= LicenseUtil.getLevelName()  %></b>
								<%} %>
							</td>
						</tr>
						
						<tr>
							<td>
								Server ID: 
							</td>
							<td>
								<%= serverId %>
							</td>
						</tr>
						 <% if(!isCommunity){  %> 
								<tr>
									<td nowrap="true"><%= LanguageUtil.get(pageContext, "license-valid-until") %></td>
									<td>
					            			<% if(isPerpetual) { %>
					            				<%= LanguageUtil.get(pageContext, "request-license-perpetual") %>
					            			<%} else {%>
					            				<%if(expired && !isPerpetual){ %>
					            					<font color="red">
					            				<%} %>
					                			<%= expireString %>
					                			<%if(expired && !isPerpetual){ %>
					                				<%= LanguageUtil.get(pageContext, "request-license-expired") %></font>
					                			<%} %>
					           			 <%}%>
					           		</td>
								</tr>
								
								<tr>
									<td><%= LanguageUtil.get(pageContext, "licensed-to") %>:</td>
									<td><%=  UtilMethods.isSet(LicenseUtil.getClientName()) ? LicenseUtil.getClientName() : "No License Found" %></td>
								</tr>
								<tr>
									<td><%= LanguageUtil.get(pageContext, "license-type") %>:</td>
									<td><%= LicenseUtil.getLicenseType() %></td>
								</tr>
								<tr>
									<td><%= LanguageUtil.get(pageContext, "license-serial") %>:</td>
									<td><%= LicenseUtil.getDisplaySerial() %></td>
								</tr>
							<% } %>
					</table>

					<div style="text-align:center;margin:30px 0;">
						
						<a href="/html/blank.jsp" 
							target="trialRequestWindow" 
							onclick="licenseAdmin.requestTrial()" 
							id="trailBtn"
							class="btn btn-info">
							
							<%= LanguageUtil.get(pageContext, "request-trial-license") %>
							
						</a>
	
								

						<span id="currentCustomerToggle">
							<a href="#" id="currentCustomerBtn" class="btn" ><%= LanguageUtil.get(pageContext, "request-license-current-customers") %></a>
						</span>

	
					</div>	


					<div id="currentCustomer" <%if(isCommunity){ %>class="hidden"<%} %>>
						


						
					<!-- CURRENT CUSTOMERS Single License -->
							<div class="generateCode" id="generateCode">
								<hr>
								<div style="padding-bottom: 10px;">
									<h2><%= LanguageUtil.get(pageContext, "request-license-code") %></h2>
									<p><%= LanguageUtil.get(pageContext, "request-license-steps-explaination") %></p>
								</div>
								
								
								<div class="yui-gb">
									<div class="yui-u first">
										<div class="stepHeader"><%= LanguageUtil.get(pageContext, "global-step1") %> <small><%= LanguageUtil.get(pageContext, "request-license-step1-words") %></small></div>
										<div class="stepBody">
											<p>
												<%= LanguageUtil.get(pageContext, "request-license-generate-code") %>
											</p>
											<div style="height:102px;">
												<table border="0" class="blankTable">
													<tr>
														<th style="text-align: right;"><label for="license_type"><%= LanguageUtil.get(pageContext, "request-license-type") %>: </label></th>
														<td>
															<select onchange="licenseAdmin.doCodeRequest()" style="width:120px;"  data-dojo-id="license_type" id="license_type" name="license_type" data-dojo-type="dijit.form.Select">
											                    <option value="--"></option>
											                    <option value="prod"><%= LanguageUtil.get(pageContext, "request-license-prod") %></option>
											                    <option value="dev"><%= LanguageUtil.get(pageContext, "request-license-dev") %></option>
											                </select>
														</td>
													</tr>
													<tr>
														<th style="text-align: right;"><label for="license_level"><%= LanguageUtil.get(pageContext, "request-license-level") %>: </label></th>
														<td>
															<select onchange="licenseAdmin.doCodeRequest()" style="width:120px;" data-dojo-id="license_level" id="license_level" name="license_level" data-dojo-type="dijit.form.Select">
											                    <option value="100"></option>
											                    <option value="200"><%= LanguageUtil.get(pageContext, "request-license-standard") %></option>
											                    <option value="300"><%= LanguageUtil.get(pageContext, "request-license-professional") %></option>
											                    <option value="400"><%= LanguageUtil.get(pageContext, "request-license-prime") %></option>
											                </select>
														</td>
													</tr>
												</table>
											</div>
										  </div>
								      </div>
							        
							        <div class="yui-u">
							        	<div class="stepHeader"><%= LanguageUtil.get(pageContext, "global-step2") %> <small><%= LanguageUtil.get(pageContext, "request-license-step2-words") %></small></div>
							        	<div class="stepBody">
							        		<div id="getMeMyLicenseCode">
												<p><%= LanguageUtil.get(pageContext, "request-license-copy-code-to-portal") %></p>
												<div style="word-wrap: break-word;font-family: monospace;">
													<textarea id="licenseCode" style="width:100%;margin:auto;height:100px;font-family: monospace;border:1px solid silver;"></textarea>
												</div>
												<div style="text-align:center;margin-top: 15px;">
							                		<button type="button" onclick="window.open('http://my.dotcms.com')" data-dojo-id="openPortal" id="openPortal"
								                          data-dojo-type="dijit.form.Button" name="openPortal" iconClass="" value="openPortal">
									                  	<%= LanguageUtil.get(pageContext, "request-license-go-to-portal") %> 
									                </button>
									            </div>
											</div>
							        	</div>
							        </div>
							        <div class="yui-u">
							        	<div class="stepHeader"><%= LanguageUtil.get(pageContext, "global-step3") %> <small><%= LanguageUtil.get(pageContext, "request-license-step3-words") %></small></div>
							        	<div class="stepBody">
							        		<p><%= LanguageUtil.get(pageContext, "request-license-paste-code") %></p>
							        		<textarea id="licenseCodePasteField"  name="license_text" style="width:100%;margin:auto;height:100px;font-family: monospace;border:1px solid silver;"></textarea>
							        		<div style="padding:15px;text-align:center;">
											 	<button type="button" onclick="licenseAdmin.doLicensePaste()" data-dojo-id="uploadButton2" id="uploadButton2" data-dojo-type="dijit.form.Button" name="upload_button2" iconClass="keyIcon" value="upload">
											 		<%= LanguageUtil.get(pageContext, "save-license") %>
											 	</button>
											</div>
							        	</div>
							        </div>
								</div>
					      	</div>
				      	
				      		<hr>
				           
				<!-- LICENSE PACK -->
							<div id="licensePack">
								<h2><%= LanguageUtil.get(pageContext, "cluster-licenses") %></h2>	
								<div class="yui-ge">
									<div class="yui-u first">
										<p>
										<%= LanguageUtil.get(pageContext, "request-license-cluster-license-explaination") %>
										
										
										
										</p>
									</div>
									<div class="yui-u" style="text-align: right;">
										<button data-dojo-type="dijit.form.Button" onClick="dijit.byId('uploadDiaWindow').show()" iconClass="uploadIcon">
									        <%= LanguageUtil.get(pageContext, "Upload-license-pack-button") %>
									    </button>
							    	</div>
							   </div>
									
							    <table border="0" width="100%" style="margin:0;border:1px solid silver;border-collapse: collapse;">
							        <thead>
							        <tr>
							            <th>&nbsp;</th>
					                    <th><%= LanguageUtil.get(pageContext, "license-repo-serverid") %></th>
					                    <th><%= LanguageUtil.get(pageContext, "license-serial") %></th>
							            <th><%= LanguageUtil.get(pageContext, "license-repo-last-ping") %></th>
							            <th><%= LanguageUtil.get(pageContext, "license-repo-validuntil") %></th>
							            <th><%= LanguageUtil.get(pageContext, "license-repo-level") %></th>
							            <th><%= LanguageUtil.get(pageContext, "license-repo-type") %></th>
							        </tr>
							        </thead>
							        <tbody id="repotableBody">
										<tr>
											<td colspan="7"><%= LanguageUtil.get(pageContext, "license-repo-type") %></td>
										</tr>
							        </tbody>
							    </table>
							</div>
							
					</div><!-- /CURRENT CUSTOMER -->

					
		
				<!-- PASTE YOUR LICENSE KEY -->
					<div id="pasteTrial" <%if(!isCommunity){ %>class="hidden"<%} %>>
					<hr>
						<h2><%= LanguageUtil.get(pageContext, "I-already-have-a-license") %></h2>
						<div class="alert alert-warning">
							<div class="yui-gf">
								<div class="yui-u first" style="font-weight: bold;text-align: justify;">
									<%= LanguageUtil.get(pageContext, "paste-your-license") %>
								</div>
								<div class="yui-u">
									<textarea rows="5" cols="100"  id="licenseCodePasteFieldTwo"  name="license_text_two" ></textarea>
								</div>
							</div>
							<div style="padding:10px;text-align:center;">
							 	<button type="button" onclick="licenseAdmin.doLicensePaste()" data-dojo-id="uploadButton" id="uploadButton" data-dojo-type="dijit.form.Button" name="upload_button" iconClass="keyIcon" value="upload">
							 		<%= LanguageUtil.get(pageContext, "save-license") %>
							 	</button>
							</div>
						</div>
					</div>
						
				</div><!-- /CONTENT WRAPPER -->
			</td>
		</tr>
	</table>
	
	
</div>


<div dojoType="dijit.Dialog" id="uploadDiaWindow" title="<%= LanguageUtil.get(pageContext, "Upload-license-pack") %>">
    <div style="margin:auto;width:80%;margin:30px;">
   	
        <form method="POST" data-dojo-type="dijit.form.Form" action="/api/license/upload/" onSubmit="return false" encType="multipart/form-data" id="uploadPackForm">
			<div style="padding:10px;">
				<input type="file" name="file" id="uploadPackFile" accept="application/zip"/>
            </div>
			<div style="padding:10px;text-align: center;">
            	<button data-dojo-type="dijit.form.Button" name="btnSubmit" iconClass="uploadIcon" onClick="licenseAdmin.doPackUpload()"><%= LanguageUtil.get(pageContext, "Upload-license-pack-button") %></button>
        	</div>
        </form>
    </div>
</div>
	


