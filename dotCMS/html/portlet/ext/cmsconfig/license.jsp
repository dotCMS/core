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
	    	var data = {"licenseLevel":dijit.byId("license_level").getValue(),"licenseType":dijit.byId("license_type").getValue()};
	   	    dojo.xhrPost({
	   	        url: "/api/license/requestCode/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(code) {

					dojo.byId("licenseCode").value=code;
					dijit.byId("getLicenseCodeDia").show();
					
					
	   	        }
	
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
	   	 	setTimeout(licenseAdmin.refreshLayout(),3000);
	   	 
	   	},
	   	
	   	
	   	refreshLayout : function(){
	   		if(dijit.byId('uploadDiaWindow')){
	   			dijit.byId('uploadDiaWindow').hide();
	   		}
	   		if(dijit.byId('uploadgetLicenseCodeDiaDiaWindow')){
	   			dijit.byId('getLicenseCodeDia').hide();
	   		}
	   		dijit.byId("mainTabContainer").selectChild("licenseTab", true);
	   		
	   		
	   	},
	   	
	    	
	   	
	    doLicensePaste :function () {

	   		var data = {"licenseText":dojo.byId("licenseCodePasteField").value};
	   	    dojo.xhrPost({
	   	        url: "/api/license/applyLicense/",
	   	        handleAs: "text",
	   	        postData: data,
	   	        load: function(message) {
	   	        	
	   	        	if(! message ){
	   	        		licenseAdmin.refreshLayout();
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
                            row=dojo.create("tr",{"class":"current_server_row"},dojo.byId("repotableBody"),"first");
                        }
                        else {
                            row=dojo.create("tr",null,dojo.byId("repotableBody"));
                        }

                        var serial=lic.id;
                        var optd=dojo.create("td",{"nowrap":"true"},row);

                        if(lic.serverid==licenseAdmin.currentServerId  ) {
                            dojo.create("span",{"class":"unlockIcon", title:"<%= LanguageUtil.get(pageContext, "license-tip-free") %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.free()"},optd));
                        }
                        else if(lic.available) {

                            dojo.create("span",{"class":"downloadIcon",title:"<%= LanguageUtil.get(pageContext, "license-tip-pick") %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.pick('"+serial+"')"},optd));

                            dojo.create("span",{"class":"deleteIcon", title:"<%= LanguageUtil.get(pageContext, "license-tip-del") %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.del('"+serial+"')"},optd));
                        }
                        else if(lic.available) {
                        	
                        	
                        }

                        dojo.create("td",{ innerHTML: lic.id.substring(lic.id, 8)}, row);
                        dojo.create("td",{ innerHTML: (!lic.serverid || lic.serverid==="") ? "Available" :
                                lic.serverid+(lic.available ? " (Available)":"")}, row);
                        dojo.create("td",{ innerHTML: !lic.available || lic.serverid ? lic.lastping : ""}, row);
                        dojo.create("td",{ innerHTML: lic.perpetual ? "Perpetual" : lic.validUntil}, row);
                        dojo.create("td",{ innerHTML: licenseAdmin.levelName(lic.level)}, row);
                        dojo.create("td",{ innerHTML: licenseAdmin.typeName(lic.licenseType)}, row);
                    });
                    if(data.length==0){
                    	var row=dojo.create("tr",null,dojo.byId("repotableBody"));
                    	var optd=dojo.create("td",{"colspan":100,"align":"center"},row);
                    	optd.innerHTML="<a href='#' onclick=\"dijit.byId('uploadDiaWindow').show()\"><%= LanguageUtil.get(pageContext, "No-Results-Found") %></a>";
                    }
                    
                }
            });
        },

        del:  function (serial) {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-delete") %>')) return;
            dojo.xhrDelete({
                url: "/api/license/delete/id/"+serial+"/",
                load: this.load
            });
        },

        pick:  function (serial) {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-pick") %>')) return;
            dojo.xhrPost({
                url: "/api/license/pick/serial/"+serial+"/",
                load: function() {
                	licenseAdmin.refreshLayout();
                }
            });
        },

        free: function () {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-free") %>')) return;
            dojo.xhrPost({
                url: "/api/license/free/",
                load: function() {
                	
                }
            });
            licenseAdmin.refreshLayout();
        },
        
        
        doPackUpload : function () {

        	if(!dojo.byId("uploadPackFile").value || dojo.byId("uploadPackFile").value.length<1){
        		return;
        	}

        	dojo.io.iframe.send({

	   	     	form: dojo.byId("uploadPackForm"),
	   	        load: function(message, ioArgs) {
	   	        	console.log(message);
	   	        	licenseAdmin.refreshLayout();
	   	        },
	   	     	error: function(error){
	   	     		//showDotCMSSystemMessage("ERROR:" + error,true);
	   	     	licenseAdmin.refreshLayout();
	   	     	
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


<!-- 

//<%= LanguageUtil.get(pageContext, "license-trial-applied-successfully") %>
//<%= LanguageUtil.get(pageContext, "license-bad-id-button") %>
	


-->





<style type="text/css">
    tr.current_server_row td {
        background-color:#D8F6CE
    }
    .layoutTable td{
		padding:20px;
    }
    .layoutTable table td{
		padding:5px;
    }
</style>



<form name="trialLicenseForm" id="trialLicenseForm" method="POST" target="trialRequestWindow" action="https://www.dotcms.com/licensing/request-a-license-3/">
	<input type="hidden" value="" name="trialLicenseRequestCode" id="trialLicenseRequestCode">
</form>	



<div class="portlet-wrapper">
	<table class="listingTable shadowBox layoutTable" style="font-size:12px;margin-top:20px;">
	    <tr>
	        <th colspan=2>Current License Information</th>
	    </tr>
		<tr>
			<td colspan="2" align="center">
				<table border=0 width="80%" style="margin:20px;border-top:1px solid silver">
					<tr>
						<td width="25%" nowrap="true">
							<%= LanguageUtil.get(pageContext, "license-level") %>
						</td>
						<td>
							
							
							<% if(isCommunity){  %> 
								<a href="/html/blank.jsp" 
										target="trialRequestWindow" 
										onclick="licenseAdmin.requestTrial()" 
										style="color:#555;">
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
				            				Perpetual
				            			<%} else {%>
				            				<%if(expired && !isPerpetual){ %>
				            					<font color="red">
				            				<%} %>
				                			<%= expireString %>
				                			<%if(expired && !isPerpetual){ %>
				                				(expired)</font>
				                			<%} %>
				           			 <%}%>
				           		</td>
							</tr>
							
							<tr>
								<td><%= LanguageUtil.get(pageContext, "licensed-to") %></td>
								<td><%=  UtilMethods.isSet(LicenseUtil.getClientName()) ? LicenseUtil.getClientName() : "No License Found" %></td>
							</tr>
							<tr>
								<td><%= LanguageUtil.get(pageContext, "license-type") %></td>
								<td><%= LicenseUtil.getLicenseType() %></td>
							</tr>
							<tr>
								<td><%= LanguageUtil.get(pageContext, "license-serial") %></td>
								<td><%= LicenseUtil.getSerial() %></td>
							</tr>
						<% } %>
				</table>
				
				<% if(!isCommunity){  %> 
					<div style="padding-bottom:10px;">
						    <button data-dojo-type="dijit.form.Button" onClick="licenseAdmin.resetLicense()" iconClass="resetIcon">
						        <%= LanguageUtil.get(pageContext, "license-bad-id-button") %>
						    </button>
					</div>
				<%} %>
				
				
			</td>
		</tr>

		<tr>
	        <th colspan=2><%= LanguageUtil.get(pageContext, "request-license") %></th>

	    </tr>
		<tr>
			<td width="50%" style="padding:20px;" valign="top">
				<b><%= LanguageUtil.get(pageContext, "request-license-trial") %></b>
				<div class="callOutBox" >
				    <div>
				    	<a href="/html/blank.jsp" target="trialRequestWindow" onclick="licenseAdmin.requestTrial()" style="text-decoration: none;color:black">
				        	<%= LanguageUtil.get(pageContext, "license-trial-promo") %>
				        </a>
				    </div>
				    <div style="padding:10px;font-weight:bold">
					    <a href="/html/blank.jsp" target="trialRequestWindow" onclick="licenseAdmin.requestTrial()">
			             	<%= LanguageUtil.get(pageContext, "request-trial-license") %> 
						</a>
				    </div>
				</div>
			</td>
			<td width="50%" valign="top">
				<b><%= LanguageUtil.get(pageContext, "request-license-prod") %> / <%= LanguageUtil.get(pageContext, "request-license-dev") %></b>
				<div style="margin:20px;">
		            <div style="padding:10px">
		                <label for="license_type"><%= LanguageUtil.get(pageContext, "request-license-type") %></label>
		                <select style="width:150px;"  data-dojo-id="license_type" id="license_type" name="license_type" data-dojo-type="dijit.form.Select">
		                    <option value="prod"><%= LanguageUtil.get(pageContext, "request-license-prod") %></option>
		                    <option value="dev"><%= LanguageUtil.get(pageContext, "request-license-dev") %></option>
		                </select>
		            </div>
		            <div style="padding:10px">
		                <label for="license_level"><%= LanguageUtil.get(pageContext, "request-license-level") %></label>
		                <select style="width:150px;" data-dojo-id="license_level" id="license_level" name="license_level" data-dojo-type="dijit.form.Select">
		                    <option value="200"><%= LanguageUtil.get(pageContext, "request-license-standard") %></option>
		                    <option value="300"><%= LanguageUtil.get(pageContext, "request-license-professional") %></option>
		                    <option value="400"><%= LanguageUtil.get(pageContext, "request-license-prime") %></option>
		                </select>
					</div>
					<div style="margin:20px">
		                  <button type="button" onclick="licenseAdmin.doCodeRequest()" data-dojo-id="codereqButton" id="codereqButton"
		                          data-dojo-type="dijit.form.Button" name="codereqButton" iconClass="keyIcon" value="upload">
		                  	<%= LanguageUtil.get(pageContext, "request-license-code") %> 
		                  </button>
		              </div>
	          	</div>
			</td>
		</tr>
	    <tr>
	        <th colspan=2><%= LanguageUtil.get(pageContext, "save-license") %></th>
	    </tr>
		<tr>
			<td colspan="2" align="center">
				<div style="padding:10px;">
					<%= LanguageUtil.get(pageContext, "paste-your-license") %>:
				</div>
				<textarea rows="10" cols="60"  id="licenseCodePasteField"  name="license_text" ></textarea>
				<div style="padding:10px;">
				 	<button type="button" onclick="licenseAdmin.doLicensePaste()" data-dojo-id="uploadButton" id="uploadButton" data-dojo-type="dijit.form.Button" name="upload_button" iconClass="keyIcon" value="upload">
				 		<%= LanguageUtil.get(pageContext, "save-license") %>
				 	</button>
				</div>
			</td>
		</tr>

	    <tr>
	        <th colspan=2><%= LanguageUtil.get(pageContext, "cluster-licenses") %></th>
		</tr>
		<tr>
			<td colspan=2>
				<div style="float:right;padding-bottom:10px;">
					<button data-dojo-type="dijit.form.Button" onClick="dijit.byId('uploadDiaWindow').show()" iconClass="uploadIcon">
				        <%= LanguageUtil.get(pageContext, "Upload-license-pack-button") %>
				    </button>
			    </div>
	
			    <table id="repotable" class="listingTable" style="margin-bottom:40px">
			        <thead>
			        <tr>
			            <th>&nbsp;</th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-serial") %></th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-serverid") %></th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-last-ping") %></th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-validuntil") %></th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-level") %></th>
			            <th><%= LanguageUtil.get(pageContext, "license-repo-type") %></th>
			        </tr>
			        </thead>
			        <tbody id="repotableBody">
						<tr>
							<td colspan="40"><%= LanguageUtil.get(pageContext, "license-repo-type") %></td>
						</tr>
			        </tbody>
			    </table>
			</td>
		</tr>
	</table>	
</div>



	
		
	<div dojoType="dijit.Dialog" id="getLicenseCodeDia" title="<%= LanguageUtil.get(pageContext, "request-license") %>">
		<div class="callOutBox" id="getMeMyLicenseCode"  style="margin:20px;">
			<p><%= LanguageUtil.get(pageContext, "license-code-description") %></p>
			<div style="word-wrap: break-word;font-family: monospace;">
				<textarea id="licenseCode" style="width:360px;height:100px;font-family: monospace;border:0px solid;"></textarea>
				</strong>
			</div>
		</div>
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
	


