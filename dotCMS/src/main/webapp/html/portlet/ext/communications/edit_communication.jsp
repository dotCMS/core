<%@page import="com.dotmarketing.business.DotStateException"%>
<%@ include file="/html/portlet/ext/communications/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%
	com.dotmarketing.portlets.communications.model.Communication c;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT)!=null) {
		c = (com.dotmarketing.portlets.communications.model.Communication) request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT);
	}
	else {
		c = (com.dotmarketing.portlets.communications.model.Communication) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"),com.dotmarketing.portlets.communications.model.Communication.class);
	
	}
	com.dotmarketing.portlets.communications.struts.CommunicationsForm form = (com.dotmarketing.portlets.communications.struts.CommunicationsForm )  request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT_FORM);
	com.dotmarketing.beans.Identifier id=null;
	
	try{
		id = com.dotmarketing.business.APILocator.getIdentifierAPI().find(c);
	}
	catch(DotStateException dse){	
	}
	
	boolean canChangePermissions = false;
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	if(request
	.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT_FORM_PERMISSION) != null){
		
		canChangePermissions = Boolean.parseBoolean(String.valueOf(request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT_FORM_PERMISSION)));
	} else{
		canChangePermissions = perAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_WRITE, user);
	}

	String htmlPage_tr_display = "none";
	boolean htmlPage_checked = true;
	String alternateEmailText_tr_display = "none";
	boolean alternateEmailText_checked = false;
	if (form.getCommunicationType().equals("email")) {
		if (UtilMethods.isSet(form.getHtmlPage())) {
	htmlPage_tr_display = "block";
	htmlPage_checked = true;
		}
		else if (UtilMethods.isSet(form.getTextMessage())) {
	alternateEmailText_tr_display = "block";
	alternateEmailText_checked = true;
	htmlPage_checked = false;
		}
	}
%>

<style type="text/css">
	.redBorder {
		border: solid 1px red;
	}
</style>

<style media="all" type="text/css">
	@import url(/html/portlet/ext/contentlet/field/edit_field.css);
</style>
<%if(Config.getBooleanProperty("ENABLE_GZIP",true)){ %>
<script type="text/javascript" src="/html/js/tinymce/js/tinymce/tiny_mce_gzip.js"></script>
<%}else { %>
<script type="text/javascript" src="/html/js/tinymce/js/tinymce/tinymce.min.js"></script>
<%}%>


<script type='text/javascript' src='/dwr/interface/CampaignsAjax.js'></script>

	<%@ include file="/html/portlet/ext/communications/edit_communication_js_inc.jsp" %>
	<input type="hidden" name="enabledWysiwyg" id="enabledWysiwyg" />

<script language="Javascript">

	dojo.require('dotcms.dijit.form.FileSelector');
	
	function submitfm(form) {
		var form = document.getElementById('fm');
		
		//setWysiwygValues(form);
		
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/communications/edit_communication" /></portlet:actionURL>';
		submitForm(form);
 	}
	
	function cancelEdit() {
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/communications/view_communications" /></portlet:renderURL>';
	}
	
	function deleteCommunication() {
		var form = document.getElementById('fm');
		if(confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-communication-(this-cannot-be-undone)")%>")){
			form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/communications/edit_communication" /></portlet:actionURL>';
			submitForm(form);
		}
	}
	
	function beLazy(){
		var ele = document.getElementById("emailSubject");
		if(ele.value.length ==0 ){
			ele.value = document.getElementById("title").value;
		}
	}

	
    
	function displayProperties(id) {
	
		if (id == "properties") {
			//swap beta and alpha tabs
			document.getElementById("column_properties").className = "alpha";
			document.getElementById("column_permissions").className = "beta";
			
			//display basic properties
			document.getElementById("properties").style.display = "";
			document.getElementById("permissions").style.display = "none";
		}
		else if (id == "permissions") {
			//swap beta and alpha tabs
			document.getElementById("column_properties").className = "beta";
			document.getElementById("column_permissions").className = "alpha";

			//display basic properties
			document.getElementById("properties").style.display = "none";
			document.getElementById("permissions").style.display = "";
		}
	}

	/*editor = new HTMLArea("textMessage");
	var editorCurrentMode;
	var editorGenerated;

	function setWysiwygValues(form) {
	    if (editorGenerated) {
	      form.textMessage.value = editor.getHTML();
	    }
    }
    
    function initDocument() {
	    editor = new HTMLArea("textMessage");
    }
	
	function setMode(mode) {
    	if (editor == null){
    		editor = new HTMLArea("textMessage");
    	}
  		if (!editorGenerated) 
  		{
    	    editor.registerPlugin(ContextMenu);
  			editor.generate();
	  		editorGenerated = true;
  		}
  		else 
  		{
		  	editor.setMode(mode);
		  	editor.updateToolbar();
		}

		var textimg = document.getElementById("textmode");
		var wysiwygimg = document.getElementById("wysiwyg");
		if (mode == "textmode") 
		{
			textimg.src = "/portal/images/btn_html_on.gif";
			wysiwygimg.src = "/portal/images/btn_wysiwyg_off.gif";
		}
		else 
		{
			textimg.src = "/portal/images/btn_html_off.gif";
			wysiwygimg.src = "/portal/images/btn_wysiwyg_on.gif";			
		}		
		editorCurrentMode = mode;			
	  }*/
	  
	function changeTypeContent(id) {
		if (id == 'HTMLPage') {
			document.getElementById('HTMLPage_tr').style.display = "";
			document.getElementById('alternateEmailText_tr').style.display = "none";
			document.getElementById('alternateEmailText_tr2').style.display = "none";
		} else if (id == 'alternateEmailText') {
			document.getElementById('alternateEmailText_tr').style.display = "";
			document.getElementById('alternateEmailText_tr2').style.display = "";
			document.getElementById('HTMLPage_tr').style.display = "none";
		}
	}
    
    function clearText() {
		var form = document.getElementById('fm');
    	form.textMessage.value = "";
    }

    //This function checks if the email address is in the list of 
    //bounce checked emails
    function checkEmail () {
    	<% if (Config.getBooleanProperty("ENABLE_POP_BOUNCES_THREAD")) { %>
    	CampaignsAjax.getValidBounceCheckedAccount(checkEmailCallback);
    	<% } %>
    }

    function checkEmailCallback(list) {
    	var input = Ext.get("fromEmail");
    	var currentValue = input.dom.value.toLowerCase();
    	var matched = false;
    	var listStr = "";
    	
    	if(list.length == 0) 
    		return;
    	
    	for (var i = 0; i < list.length; i++) {
    		var email = list[i].toLowerCase();
    		if(currentValue == email) {
    			matched = true;
    		}
    		listStr = listStr + "<br/>" + "<i>" + email + "</i>"; 
    	}
    	var message = '<%=LanguageUtil.get(pageContext, "This-email-address-is-not-being-checked-for-bounces")%>:' + listStr;
    	
    	if(!matched) {
    	
		    Ext.QuickTips.register({ target: 'fromEmailAlert', 
		    	title: 'Email not registered',
		    	text: message, 
		    	width: 280, animate: true });
    		input.addClass('x-form-invalid');
    		Ext.get('fromEmailAlert').show();
    	} else {
		    Ext.QuickTips.unregister(input);
    		input.removeClass('x-form-invalid');
    		Ext.get('fromEmailAlert').hide();
    	}
    }
    
	function hideEditButtonsRow() {
		
		dojo.style('editCommunicationButtonRow', { display: 'none' });
	}
	
	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editCommunicationButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}
    
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%=LanguageUtil.get(pageContext, "Edit-Communication")%>' />
	
<html:form action='/ext/communications/edit_communication' styleId="fm">

<div dojoAttachPoint="cmsFileBrowserImage" currentView="thumbnails" jsId="cmsFileBrowserImage" onFileSelected="addFileImageCallback" 
     mimeTypes="image"  dojoType="dotcms.dijit.FileBrowserDialog">
</div>
	
<div dojoAttachPoint="cmsFileBrowserFile" currentView="list" jsId="cmsFileBrowserFile" onFileSelected="addFileCallback"
	 dojoType="dotcms.dijit.FileBrowserDialog">
</div>		



<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
<input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/communications/view_communications" /></portlet:renderURL>">

<input type="hidden" name="submitParent" id="submitParent" value="">
<input type="hidden" name="wysiwyg_image" id="wysiwyg_image" value="">
<input type="hidden" name="selectedwysiwyg_image" id="selectedwysiwyg_image" value="">
<input type="hidden" name="selectedIdentwysiwyg_image" id="selectedIdentwysiwyg_image" value="">
<input type="hidden" name="folderwysiwyg_image" id="folderwysiwyg_image" value="">
<input type="hidden" name="wysiwyg_file" id="wysiwyg_file" value="">
<input type="hidden" name="selectedwysiwyg_file" id="selectedwysiwyg_file" value="">
<input type="hidden" name="selectedIdentwysiwyg_file" id="selectedIdentwysiwyg_file" value="">
<input type="hidden" name="folderwysiwyg_file" id="folderwysiwyg_file" value="">
<input type="hidden" name="communicationType" value="email" /> 

<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

    <div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>" onShow="showEditButtonsRow()">
        
		<input name="subcmd" type="hidden" value="" />
                                     
	    <% if(UtilMethods.isSet(form.getInode())) {%>
		
	         <dl>              
	            <dt><%=LanguageUtil.get(pageContext, "Modified-On")%>:</dt>
	            <dd><%= form.getWebModDate() %> </dd>
	            
	            <dt><%=LanguageUtil.get(pageContext, "Modified-By")%>:</dt>
	            <dd>
	                <%
	                    String modifybyUser ="";
	                    User displayUser = com.dotmarketing.business.APILocator.getUserAPI().loadUserById(form.getModifiedBy(),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(),false);
	                    if(com.dotmarketing.util.UtilMethods.isSet(displayUser.getFullName())){
	                        modifybyUser = displayUser.getFullName();
	                    }else {
	                        modifybyUser = form.getModifiedBy();
	                    }
	                
	                %>
	                <%= modifybyUser %>
	            </dd>
	            <dt>ID:</dt>
	            <dd>
	                <html:hidden styleClass="form-text" style="width:350" property="inode"/>
	                <%= form.getInode() %>
	            </dd>
	         </dl>		
	            
	
	    <%} else { %>
            <html:hidden styleClass="form-text" style="width:350" property="inode"/>
    <%} %>           
		<dl> 
			<dt><span class="required"></span> <%=LanguageUtil.get(pageContext, "Communication-Title")%>:</dt>
			<dd>
				<input type="text" dojoType="dijit.form.TextBox" style="width:370;" name="title" id="title" onchange="beLazy();" value="<%= UtilMethods.isSet(form.getTitle()) ? form.getTitle() : "" %>" />
			</dd>
		</dl>
	                                 

		<dl id="fromNameEmail_tr">
			<dt><span class="required"></span> <%=LanguageUtil.get(pageContext, "From-Name-/-Email")%>:</dt>
			<dd>
				<span id="fromEmailAlert" class="x-form-invalid-icon" style="position: static; float: right; padding-right: 15px;"></span>
				<input type="text" dojoType="dijit.form.TextBox" style="width:143;" name="fromName" id="fromName" value="<%= UtilMethods.isSet(form.getFromName()) ? form.getFromName() : "" %>" />&nbsp;
				<input type="text" dojoType="dijit.form.TextBox" style="width:190;" name="fromEmail" id="fromEmail" onblur="checkEmail()" value="<%= UtilMethods.isSet(form.getFromEmail()) ? form.getFromEmail() : "" %>" />
			</dd>
		</dl>		
	
	                 
		<dl id="emailSubject_tr">
			<dt><span class="required"></span> <%=LanguageUtil.get(pageContext, "Email-Subject")%>:</dt>
			<dd>
				<input type="text" dojoType="dijit.form.TextBox" style="width:370" name="emailSubject" id="emailSubject" value="<%= UtilMethods.isSet(form.getEmailSubject()) ? form.getEmailSubject() : "" %>" />
			</dd>
		</dl>

		<dl id="typeOfContent_tr" style="display:;">
			<dt>
				<%=LanguageUtil.get(pageContext, "Type-Of-Content")%>:
			</dt>
			<dd>
				<input type="radio"  dojoType="dijit.form.RadioButton"  name="typeContent" id="typeContentHTMLPage" value="HTMLPage" onclick="changeTypeContent('HTMLPage')" <%= htmlPage_checked ? "checked" : "" %> />&nbsp;<label for="typeContentHTMLPage"><%=LanguageUtil.get(pageContext, "HTML-Page")%></label>
				&nbsp;&nbsp;&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton"  name="typeContent" id="typeContentAlternate" value="alternateEmailText" onclick="changeTypeContent('alternateEmailText')" <%= alternateEmailText_checked ? "checked" : "" %> />&nbsp;<label for="typeContentAlternate"><%=LanguageUtil.get(pageContext, "Alternate-Email-Text")%></label>
			</dd>
		</dl>

		<dl id="HTMLPage_tr" style="display:<%=htmlPage_tr_display%>;">
			<dt><span class="required"></span> <%=LanguageUtil.get(pageContext, "HTML-Page")%>:</dt>
			<dd>
				<input type="text" name="htmlPage" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="details" 
					mimeTypes="application/dotpage"	value="<%= form.getHtmlPage() %>" />			
			</dd>
		</dl>

		<dl id="alternateEmailText_tr" style="display:<%= alternateEmailText_tr_display %>;">
		<dt><span class="required"></span><span id="alternateEmailLabel"><%=LanguageUtil.get(pageContext, "Alternate-Email-Text")%>:</span></dt>
			<dd>
				<div id="alternateEmailText_tr2" style="float: left; display:<%= alternateEmailText_tr_display %>;">
				    <table cellpadding="0" cellspacing="0">
				        <tr>
				            <td>
				                <textarea dojoType="dijit.form.Textarea" style="width:600px;height:250px;" name="textMessage" id="textMessage" ><%= UtilMethods.isSet(form.getTextMessage()) ? form.getTextMessage() : "" %></textarea>
				            </td>
				        </tr>
				        <tr>
				            <td>
				                <button dojoType="dijit.form.ToggleButton" iconClass="dijitCheckBoxIcon" onclick="if (this.checked) enableTinyMCE('textMessage'); else disableTinyMCE('textMessage');" checked type="button">
				                    WYSIWYG
				                </button>
				                <script>
									dojo.addOnLoad(function () {
										initTinyMCE("textMessage");
									});
				                </script>
				            </td>
				        </tr>
				    </table>
				</div>
			</dd>
		</dl>
	    <dl>
		    <dt>&nbsp;</dt>
		    <dd>
		       <fieldset style="width:600px;"> 
					<strong><%=LanguageUtil.get(pageContext, "Variables-used-to-replace-user-information")%></strong><hr/>
					<strong><%=LanguageUtil.get(pageContext, "Name")%>:</strong> &lt;varName&gt; <br/>
					<strong><%=LanguageUtil.get(pageContext, "Email")%>:</strong> &lt;varEmail&gt; <br/>
					<strong><%=LanguageUtil.get(pageContext, "Middle-Name")%>:</strong> &lt;varMiddleName&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Last-Name")%>:</strong> &lt;varLastName&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Address")%>1:</strong> &lt;varAddress1&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Address")%>2:</strong> &lt;varAddress2&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Phone")%></strong> &lt;varPhone&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "State")%>:</strong> &lt;varState&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "City")%>:</strong> &lt;varCity&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Country")%>:</strong> &lt;varCountry&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Zip-Code")%>:</strong> &lt;varZip&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "Subscription-Management")%>:</strong> &lt;subscriptionsLink&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "From-Name")%>:</strong> &lt;fromName&gt;<br/>
					<strong><%=LanguageUtil.get(pageContext, "From-Email")%>:</strong> &lt;fromEmail&gt;<br/>
		        </fieldset>
	     	</dd>
		</dl>
        
	</div>

			
	<%
		boolean canEditAsset = perAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
		if (canEditAsset) {
	%>			
		<div id="permissions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
			<%
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, c);
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, null);
			%>
			<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
		</div>
	<% } %>

</div>

<div class="clear"></div>

<div class="buttonRow" id="editCommunicationButtonRow">
<%
	if (canChangePermissions) {
%>
	<button dojoType="dijit.form.Button"  onClick="deleteCommunication()" iconClass="deleteIcon" type="button">
		<%=LanguageUtil.get(pageContext, "Delete")%>
	</button>
	<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="saveIcon" type="button">
		<%=LanguageUtil.get(pageContext, "save")%>
	</button>
	<button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon" type="button">
		<%=LanguageUtil.get(pageContext, "cancel")%>
	</button>
<%
	} else {
%>
	<button dojoType="dijit.form.Button" onClick="cancelEdit()" type="button">
		<%=LanguageUtil.get(pageContext, "Return-to-View")%>
	</button>
<%
	}
%>


</div>



</html:form>
			
</liferay:box>
<script>
<%String x = (htmlPage_checked) ? "HTMLPage" :"alternateEmailText" ;%>
changeTypeContent("<%=x%>");




</script>

