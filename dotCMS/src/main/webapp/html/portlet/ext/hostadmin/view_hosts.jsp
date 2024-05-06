<%@ include file="/html/portlet/ext/hostadmin/init.jsp" %>

<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotmarketing.util.Config" %>

<%
	PermissionAPI permAPI = APILocator.getPermissionAPI();
	HostAPI hostAPI = APILocator.getHostAPI();
	RoleAPI roleAPI = APILocator.getRoleAPI();
	Host systemHost = hostAPI.findSystemHost(user, false);
	boolean doesHavePermissionToAddHosts = permAPI.doesUserHavePermission(systemHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);

	boolean isCMSAdmin = roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());

	String showDeleted = (request.getParameter("showDeleted") != null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.HOST_SHOW_DELETED);
%>

<%@ include file="/html/portlet/ext/hostadmin/view_hosts_js_inc.jsp" %>

<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("filter"));
		  t.stop();
		}
		t.start();
	});
	function stopEvent(event){
		if(event.preventDefault){
			event.preventDefault();
			event.stopPropagation();
		}
	}

</script>

<div class="portlet-main">
	<!-- START Toolbar -->
	<div class="portlet-toolbar" style="margin: 16px">
		<div class="portlet-toolbar__actions-primary">
			<div class="inline-form">
				<input type="text" name="filter" id="filter" onkeydown="hostAdmin.filterHosts()" dojoType="dijit.form.TextBox" value="" />
		        <button dojoType="dijit.form.Button" onclick="hostAdmin.clearFilter()" class="dijitButtonFlat">
		            <%=LanguageUtil.get(pageContext, "Reset")%>
		        </button>
		        <div class="checkbox">
		        	<input type="checkbox" dojoType="dijit.form.CheckBox" name="showDeleted" id="showDeleted" onClick="hostAdmin.filterHosts();" <%=(showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : ""%> value="true" />
					<label for="showDeleted"><%=LanguageUtil.get(pageContext, "Show-Archived")%></label>
				</div>
			</div>
		</div>
		<div class="portlet-toolbar__info">

		</div>
    	<div class="portlet-toolbar__actions-secondary">
			<%
				if (doesHavePermissionToAddHosts) {
			%>
		        <button dojoType="dijit.form.Button" onClick="hostAdmin.openAddHostDialog()" iconClass="plusIcon">
		            <%=LanguageUtil.get(pageContext, "add-host")%>
		        </button>
			<%
				}
			%>
    	</div>
   </div>
   <!-- END Toolbar -->

	<table class="listingTable host-list">
		<thead id="hostsTableHeader"></thead>
		<tbody id="hostsTableBody"></tbody>
		<tbody id="noResultsSection">
			<tr class="alternate_1" id="rowNoResults">
				<td colspan="5">
					<div class="noResultsMessage">
						<%=LanguageUtil.get(pageContext, "No-Results-Found")%>
					</div>
				</td>
			</tr>
		</tbody>
	</table>

	<div id="hostContextMenues"></div>

	<div class="yui-gb buttonRow">
		<div class="yui-u first" style="text-align: left;" id="buttonPreviousResultsWrapper">
			<button dojoType="dijit.form.Button" id="buttonPreviousResults" onclick="hostAdmin.gotoPreviousPage()" iconClass="previousIcon">
				<%=LanguageUtil.get(pageContext, "Previous")%>
			</button>
		</div>
		<div class="yui-u" style="text-align: center;" id="resultsSummary">
			<%=LanguageUtil.get(pageContext, "Viewing-Results-From")%>
		</div>
		<div class="yui-u" style="text-align: right;" id="buttonNextResultsWrapper">
			<button dojoType="dijit.form.Button" id="buttonNextResults" onclick="hostAdmin.gotoNextPage()" iconClass="nextIcon">
				<%=LanguageUtil.get(pageContext, "Next")%>
			</button>
		</div>
	</div>

</div>

<!-- Add Host Dialog -->
<div id="addHostDialog" dojoType="dijit.Dialog" class="noDijitDialogTitleBar">
    <form id="addHostDialogForm" dojoType="dijit.form.Form">

    	<!-- Dialog step 1 -->
        <div id="addHostStep1">
            <h2><%=LanguageUtil.get(pageContext, "Create-a-new-Host")%></h2>
            <div id="addNewHostStep1Parameters">
	            <hr/>
	            <input type="radio" name="copyHost" dojoType="dijit.form.RadioButton" id="copyHostRadio" <%if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level){%>checked value="on"<% }else{ %>disabled="disabled"<% } %> />
	           <%if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level){%>
	           		<label for="copyHostRadio" id="copyHostTextId">
		                <%= LanguageUtil.get(pageContext, "Copy-an-existing-Host") %>
		            </label>
	           <%}else{ %>
		            <label for="copyHostRadio" id="copyHostTextId">
		                <%= LanguageUtil.get(pageContext, "Copy-an-existing-Host") %>
		            </label>

                	<span dojoType="dijit.Tooltip" connectId="copyHostTextId" id="copyHostTextToolTipId">
                		<%= LanguageUtil.get(pageContext, "Copy-an-existing-Host-Disabled") %>
                	</span>
                <%} %>
	            <br/>
	            <!-- <span style="text-align: center"><%= LanguageUtil.get(pageContext, "or") %></span> -->
				<br/>
				<input type="radio" name="copyHost" dojoType="dijit.form.RadioButton" id="startBlankHostRadio" <%if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level){%>value="off"<% }else{ %>checked value="on"<% } %>  />
	            <label for="startBlankHostRadio">
	                <%= LanguageUtil.get(pageContext, "Start-with-a-blank-Host") %>
	            </label>
            </div>
            <hr/>
            <div class="buttonRow-right">
			    <button dojoType="dijit.form.Button" onClick="hostAdmin.cancelCreateHost(); stopEvent; return false;" class="dijitButtonFlat">
			        <%= LanguageUtil.get(pageContext, "Cancel") %>
			    </button>
                <button dojoType="dijit.form.Button" onClick="hostAdmin.goToStep2(); stopEvent; return false; " iconClass="nextIcon">
                    <%= LanguageUtil.get(pageContext, "Next") %>
                </button>
            </div>
        </div>
        <!-- END Step 1 -->

    	<!-- Dialog step 2 -->
    	<div id="addHostStep2" style="display: none">
        	<h2><%= LanguageUtil.get(pageContext, "Select-a-Host-to-copy") %></h2>
         	<hr/>
         	<table style="width: 100%">
         	<tr>
         		<td style="width:40%; vertical-align: top;">
         			<div id="hostTemplateWrapper">
						<%= LanguageUtil.get(pageContext, "Host-Template") %>:
						<span id="hostToCopyWrapper"></span>
					</div>
					<input type="hidden" id="copyTagStorage" name="copyTagStorage" value="" />
					<br/>
					<div id="hostPreviewWrapper">
						<div id="hostThumbnailWrapper" style="min-height: 100px;">
							<img id="hostThumbnail" src="">
							<br/>
						</div>
						<%= LanguageUtil.get(pageContext, "View-site") %>: <a id="websitePreviewLink" target="_blank" href=""></a>
					</div>
         		</td>

         		<td style="width:10%">&nbsp;</td>

         		<td style="width:60%">
         			<b><%= LanguageUtil.get(pageContext, "What-to-copy") %></b><br/>
					<div class="yui-g" style="margin-bottom: 16px;">
					    <div class="yui-u first" style="text-align: left">
					        <input type="checkbox" id="copyAll" onchange="hostAdmin.copyAllChanged()" checked="true" dojoType="dijit.form.CheckBox">
							<label for="copyAll">
		    					<%= LanguageUtil.get(pageContext, "All") %>
							</label>
					    </div>
					    <div class="yui-u" style="text-align: left"></div>
					</div>
					<div id="otherCheckboxesWrapper">
						<div class="yui-g" style="margin-bottom: 16px;">
						    <div class="yui-u first" style="text-align: left">
						        <input type="checkbox" id="copyTemplatesAndContainers" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyTemplatesAndContainers">
			    					<%= LanguageUtil.get(pageContext, "Templates-and-Containers") %>
								</label>
						    </div>
						    <div class="yui-u" style="text-align: left">
						        <input type="checkbox" id="copyContentOnPages" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyContentOnPages">
			    					<%= LanguageUtil.get(pageContext, "Content-on-Pages") %>
								</label>
						    </div>
						</div>
						<div class="yui-g" style="margin-bottom: 16px;">
						    <div class="yui-u first" style="text-align: left">
						        <input type="checkbox" id="copyFolders" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyFolders">
			    					<%= LanguageUtil.get(pageContext, "Folders") %>
								</label>
						    </div>
						    <div class="yui-u" style="text-align: left">
						        <input type="checkbox" id="copyContentOnHost" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyContentOnHost">
			    					<%= LanguageUtil.get(pageContext, "Content-on-Host") %>
								</label>
						    </div>
						</div>
						<div class="yui-g" style="margin-bottom: 16px;">
							<% if (Config.getBooleanProperty("FEATURE_FLAG_ENABLE_CONTENT_TYPE_COPY", false)) { %>
							<div class="yui-u first" style="text-align: left">
								<input type="checkbox" id="copyContentTypes" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyContentTypes">
									<%= LanguageUtil.get(pageContext, "Content Types") %>
								</label>
							</div>
						    <% } else { %>
								<input style="display: none" type="checkbox" id="copyContentTypes" disabled="true" dojoType="dijit.form.CheckBox">
							<% } %>
							<div class="yui-u" style="text-align: left">
								<input type="checkbox" id="copyLinks" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyLinks">
									<%= LanguageUtil.get(pageContext, "Menu-Links") %>
								</label>
							</div>
						</div>
						<div class="yui-g" style="margin-bottom: 16px;">
						    <div class="yui-u first" style="text-align: left">
								<input type="checkbox" id="copyHostVariables" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
								<label for="copyHostVariables">
									<%= LanguageUtil.get(pageContext, "Host-Variables") %>
								</label>
						    </div>
						    <div class="yui-u" style="text-align: left"></div>
						</div>
					</div>
         		</td>
         	</tr>
         </table>

        <div class="buttonRow-right" style="clear: both;">
            <hr/>
		    <button dojoType="dijit.form.Button" onClick="hostAdmin.cancelCreateHost(); dojo.stopEvent; return false;" class="dijitButtonFlat">
		        <%= LanguageUtil.get(pageContext, "Cancel") %>
		    </button>
            <button dojoType="dijit.form.Button" onClick="hostAdmin.goToStep1(); dojo.stopEvent; return false; ">
                <%= LanguageUtil.get(pageContext, "Previous") %>
            </button>
            <button dojoType="dijit.form.Button" onClick="hostAdmin.gotoCreateHost(); dojo.stopEvent; return false; ">
                <%= LanguageUtil.get(pageContext, "Next") %>
            </button>
        </div>


		</div>
		<!-- End Step 2 -->

	</form>
</div>

<%@ include file="/html/portlet/ext/hostadmin/view_host_variables_inc.jsp" %>

