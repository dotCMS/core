<%@ include file="/html/portlet/ext/hostadmin/init.jsp" %>

<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>

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



<%@page import="com.dotcms.enterprise.LicenseUtil"%><link rel="stylesheet" type="text/css" href="/html/portlet/ext/hostadmin/view_hosts.css" />
<div class="yui-g portlet-toolbar">
    <div class="yui-u first">
        <input type="text" name="filter" id="filter" onkeydown="hostAdmin.filterHosts()" dojoType="dijit.form.TextBox" class="large" value="" />
        <button dojoType="dijit.form.Button" onclick="hostAdmin.clearFilter()" iconClass="resetIcon">
            <%=LanguageUtil.get(pageContext, "Reset")%>
        </button>
    </div>
    <div class="yui-u" style="text-align: right;">
		<input dojoType="dijit.form.CheckBox" type="checkbox" name="showDeleted" id="showDeleted" onClick="hostAdmin.filterHosts();" <%=(showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : ""%> value="true" />
		<label for="showDeleted" style="font-size:85%;"><%=LanguageUtil.get(pageContext, "Show-Archived")%></label>
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
<table class="listingTable">
	<thead id="hostsTableHeader">
	</thead>
	<tbody id="hostsTableBody">
	</tbody>
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
<div id="hostContextMenues">
</div>
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
<!-- Add Host Dialog -->
<div id="addHostDialog" dojoType="dijit.Dialog">
    <form id="addHostDialogForm" dojoType="dijit.form.Form">
    	<!-- Dialog step 1 -->
        <div id="addHostStep1">
            <h2><%=LanguageUtil.get(pageContext, "Create-a-new-Host")%></h2>
            <div id="addNewHostStep1Parameters">
	            <hr/><input type="radio" name="copyHost" dojoType="dijit.form.RadioButton" id="copyHostRadio" <%if(LicenseUtil.getLevel() > 199){%>checked value="on"<% }else{ %>disabled="disabled"<% } %> />
	           <%if(LicenseUtil.getLevel() >199){%>
	           		<label for="copyHostRadio" id="copyHostTextId">
		                <%= LanguageUtil.get(pageContext, "Copy-an-existing-Host") %> 
		            </label>
	           <%}else{ %>
		            <label for="copyHostRadio" id="copyHostTextId">
		                <span  style="color:silver"><%= LanguageUtil.get(pageContext, "Copy-an-existing-Host") %></span>
		                <span class="keyIcon"></span>
		            </label>
            		
                	<span dojoType="dijit.Tooltip" connectId="copyHostTextId" id="copyHostTextToolTipId">
                		<%= LanguageUtil.get(pageContext, "Copy-an-existing-Host-Disabled") %>
                	</span>
                <%} %>
	            <br/>
	            <span style="text-align: center"><%= LanguageUtil.get(pageContext, "or") %></span>
				<br/>
				<input type="radio" name="copyHost" dojoType="dijit.form.RadioButton" id="startBlankHostRadio" value="off" />
	            <label for="startBlankHostRadio">
	                <%= LanguageUtil.get(pageContext, "Start-with-a-blank-Host") %>
	            </label>
            </div>
            <hr/>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" onClick="hostAdmin.goToStep2(); dojo.stopEvent(event); return false; " iconClass="nextIcon">
                    <%= LanguageUtil.get(pageContext, "Next") %>
                </button>
                <button dojoType="dijit.form.Button" onClick="hostAdmin.cancelCreateHost(); dojo.stopEvent(event); return false; " iconClass="cancelIcon">
                    <%= LanguageUtil.get(pageContext, "Cancel") %>
                </button>
            </div>
        </div>
    	<!-- Dialog step 2 -->
		
        <div id="addHostStep2" style="display: none">
         <h2><%= LanguageUtil.get(pageContext, "Select-a-Host-to-copy") %></h2> 
         <hr/>	
         <div style="float: left"> 
			<div id="hostTemplateWrapper">
				<%= LanguageUtil.get(pageContext, "Host-Template") %>: 
				<span id="hostToCopyWrapper">
				</span>
			</div>
			<input type="hidden" id="copyTagStorage" name="copyTagStorage" value="" />
			<br/>
			<div id="hostPreviewWrapper">
				<div id="hostThumbnailWrapper">
					<img id="hostThumbnail" src="">
					<br/>
				</div>
				<%= LanguageUtil.get(pageContext, "View-site") %>: <a id="websitePreviewLink" target="_blank" href=""></a>
			</div>
		 </div>
		 
		 <div style="float: right">
			<b><%= LanguageUtil.get(pageContext, "What-to-copy") %></b><br/>
			<div class="yui-g buttonRow">
			    <div class="yui-u first" style="text-align: left">
			        <input type="checkbox" id="copyAll" onchange="hostAdmin.copyAllChanged()" checked="true" dojoType="dijit.form.CheckBox">
					<label for="copyAll">
    					<%= LanguageUtil.get(pageContext, "All") %>
					</label>
			    </div>
			    <div class="yui-u" style="text-align: left">
			    </div>
			</div>
			<div id="otherCheckboxesWrapper">			
				<div class="yui-g buttonRow">
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
				<div class="yui-g buttonRow">
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
				<div class="yui-g buttonRow">
				    <div class="yui-u first" style="text-align: left">
						&nbsp;&nbsp;&nbsp;&nbsp;
				        <input type="checkbox" id="copyFiles" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
						<label for="copyFiles">
	    					<%= LanguageUtil.get(pageContext, "Files") %>
						</label>
				    </div>
				    <div class="yui-u" style="text-align: left">
				        <input type="checkbox" id="copyVirtualLinks" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
						<label for="copyVirtualLinks">
	    					<%= LanguageUtil.get(pageContext, "Virtual-Links") %>
						</label>
				    </div>
				</div>	
				<div class="yui-g buttonRow">
				    <div class="yui-u first" style="text-align: left">
						&nbsp;&nbsp;&nbsp;&nbsp;
				        <input type="checkbox" id="copyPages" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
						<label for="copyPages">
	    					<%= LanguageUtil.get(pageContext, "Pages") %>
						</label>
				    </div>
				    <div class="yui-u" style="text-align: left">
				        <input type="checkbox" id="copyHostVariables" disabled="true" checked="checked" dojoType="dijit.form.CheckBox">
						<label for="copyHostVariables">
	    					<%= LanguageUtil.get(pageContext, "Host-Variables") %>
						</label>
				    </div>
				</div>	
			</div>						
		 </div>	
		 
            <div class="buttonRow" style="clear: both;">
                <hr/>
                <button dojoType="dijit.form.Button" onClick="hostAdmin.goToStep1(); dojo.stopEvent(event); return false; " iconClass="previousIcon">
                    <%= LanguageUtil.get(pageContext, "Previous") %>
                </button>
                <button dojoType="dijit.form.Button" onClick="hostAdmin.gotoCreateHost(); dojo.stopEvent(event); return false; " iconClass="nextIcon">
                    <%= LanguageUtil.get(pageContext, "Next") %>
                </button>
                <button dojoType="dijit.form.Button" onClick="hostAdmin.cancelCreateHost(); dojo.stopEvent(event); return false; " iconClass="cancelIcon">
                    <%= LanguageUtil.get(pageContext, "Cancel") %>
                </button>
            </div>
        </div>		
    </form>
</div>

<%@ include file="/html/portlet/ext/hostadmin/view_host_variables_inc.jsp" %>
