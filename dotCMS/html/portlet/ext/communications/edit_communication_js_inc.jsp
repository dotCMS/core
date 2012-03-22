<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.util.Config"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>

<script language='javascript' type='text/javascript'>

dojo.require('dijit.form.Slider');
dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
dojo.require('dotcms.dijit.form.FileSelector');
dojo.require("dotcms.dijit.form.FileAjaxUploader");
dojo.require("dotcms.dijit.FileBrowserDialog");
	
//WYSIWYG special functions
	function cmsURLConverter (url, node, on_save) {
		var idx = url.indexOf('#');
		var servername = "http://<%= request.getServerName() %>";
		var start = url.substring(0, servername.length);
		var returl = "";
		if (idx >= 0 && start == servername) {
			returl = url.substring(idx, url.length);
		} else {
			returl = url;
		} 
		return returl;
	}
	
	var wysiwyg_field_name;
	var wysiwyg_url;
	var wysiwyg_type;
	var wysiwyg_win;
	
	function cmsFileBrowser(field_name, url, type, win) {
		wysiwyg_win = win;
		if(type=="image"){
			cmsFileBrowserImage.show();
		}
		else{
			cmsFileBrowserFile.show();
		}
		dojo.style(dojo.query('.clearlooks2')[0], { zIndex: '100' })
		dojo.style(dojo.byId('mceModalBlocker'), { zIndex: '90' })
	
	}
		
		
	
	//Links kind of fields
	function popupEditLink(inode, varName) {
	    editlinkwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /><portlet:param name="popup" value="1" /></portlet:actionURL>&inode=' + inode + '&child=true&page_width=650', "editlinkwin", 'width=700,height=400,scrollbars=yes,resizable=yes');
	}
	
	//Other functions
	function popUpMacroHelp(){
		openwin = window.open('http://www.dotcms.org/documentation/dotCMSMacros',"newin","menubar=1,width=1100,height=750,scrollbars=1,toolbar=1,status=0,resizable=1");
	}
	
	function addFileImageCallback(file) {
		var ident
		var ext=file.extension;
		var ident =file.identifier+'.'+ext;
		wysiwyg_win.document.forms[0].elements["src"].value = "/dotAsset/" + ident;
		wysiwyg_win.ImageDialog.showPreviewImage("/dotAsset/" + ident);
	}
	function addFileCallback(file) {
		var ident
		var ext=file.extension;
		var ident =file.identifier+'.'+ext;
		var fileExt = getFileExtension(file.name).toString();
		<% String extension = com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"); %>
		if(fileExt == '<%= extension %>'){
			wysiwyg_win.document.forms[0].elements["href"].value = file.pageURI;
		}else{
			wysiwyg_win.document.forms[0].elements["href"].value = /dotAsset/ + ident;
		}
	}
		
	function submitParent(param) {
		//WYSIWYG_CALLBACKS
		if (param == 'wysiwyg_image') {
			var imageName = document.getElementById("selectedwysiwyg_image").value;
			var imageFolder = document.getElementById("folderwysiwyg_image").value;
			var ident = document.getElementById("selectedIdentwysiwyg_image").value;
			wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = /dotAsset/ + ident;
		}
		if (param == 'wysiwyg_file') {
			var fileName = document.getElementById("selectedwysiwyg_file").value;
			var ident = document.getElementById("selectedIdentwysiwyg_file").value;
			var fileFolder = document.getElementById("folderwysiwyg_file").value;
			<% String ext = com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"); %>
			var fileExt = getFileExtension(fileName).toString();
			if(fileExt == '<%= ext %>'){
				wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = fileFolder + fileName;
			}else{
				wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = /dotAsset/ + ident;
			}
		}
	}

	//### TINYMCE ###
	function initTinyMCE (id) 
	{
		if(isEnableTinyMCE(id))
		{			
			(new tinymce.Editor(id, tinyMCEProps)).render();
			enableTinyMCEImage(id);
			/*
			var butt = document.getElementById("macroHelp" + id);
			butt.style.display="none";
			
			var button = document.getElementById('searchHighlightedGlossaryTinyMCE_' + id);
	        button.style.display = "";
	        */
		}
		else{
		/*
			var butt = document.getElementById("macroHelp" + id);
			butt.style.display="";
			
			var button = document.getElementById('searchHighlightedGlossaryTinyMCE_' + id);
	        button.style.display = "none";
	        */
		}
	}
	
	function popUpMacroHelp(){
		openwin = window.open('http://www.dotcms.org/documentation/dotCMSMacros',"newin","menubar=1,width=1100,height=750,scrollbars=1,toolbar=1,status=0,resizable=1");
	
	}
	
	
	
	function disableTinyMCE(id)
	{		
		var result = "";		
		disableTinyMCEImage(id);
		elementWysiwyg = document.getElementById("enabledWysiwyg");
		wysiwygValue = elementWysiwyg.value;

		if(wysiwygValue != "")
		{
			var wysiwygValueArray = wysiwygValue.split(",");
			
			for(i = 0;i < wysiwygValueArray.length;i++)
			{
				var number = wysiwygValueArray[i];
				if(number != id)
				{
					result += number + ",";
				}
			}
		}
		result += id;
		elementWysiwyg.value = result;
		tinymce.EditorManager.get(id).remove();
	}
	
	function enableTinyMCE(id)
	{
        if(!isEnableTinyMCE(id))
        {
		if(confirm("Switching to the WYSIWYG view can break\nJavaScript or Velocity coding in this content.\n\nAre you sure you would like to proceed?"))
		{		
			var result = "";
			enableTinyMCEImage(id);	
			elementWysiwyg = document.getElementById("enabledWysiwyg");
			wysiwygValue = elementWysiwyg.value;
			
			if(wysiwygValue != "")
			{
				var wysiwygValueArray = wysiwygValue.split(",");			
				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var number = wysiwygValueArray[i];
					if(number != id)
					{
						result += number + ",";
					}
				}
				result = result.substring(0,result.length - 1);
			}
			elementWysiwyg.value = result;
			try
			{
				(new tinymce.Editor(id, tinyMCEProps)).render();
			}
			catch(e)
			{alert(e.message);}
		} 
		}
	}
	
	function isEnableTinyMCE(id)
	{
		var result = true;
		elementWysiwyg = document.getElementById("enabledWysiwyg");
		wysiwygValue = elementWysiwyg.value;
		if(wysiwygValue != "")
		{
			var wysiwygValueArray = wysiwygValue.split(",");
			
			for(i = 0;i < wysiwygValueArray.length;i++)
			{
				var number = wysiwygValueArray[i];
				if(number == id)
				{
					result = false;
					break;
				}
			}
		}
		return result;
	}
	
	function enableTinyMCEImage(id)
	{
		var idHtml = id + "HB";
		var idWysiwyg = id + "WB";
		
		var wysiwygElement = document.getElementById(idWysiwyg);
		var htmlElement = document.getElementById(idHtml);
	}
	
	function disableTinyMCEImage(id)
	{
		idHtml = id + "HB";
		idWysiwyg = id + "WB";
		
		wysiwygElement = document.getElementById(idWysiwyg);
		htmlElement = document.getElementById(idHtml);	
	}
	</script>
	
<% if( !com.liferay.util.GetterUtil.get(com.liferay.util.SystemProperties.get(com.liferay.filters.compression.CompressionFilter.class.getName()), false) ) { %>
	<script language='javascript' type='text/javascript'>
	<% com.liferay.portal.model.User usera= com.liferay.portal.util.PortalUtil.getUser(request); %>
	tinyMCE_GZ.init({
		plugins : 'style,layer,table,save,advhr,advimage,advlink,emotions,iespell,insertdatetime,preview,media,searchreplace,print,contextmenu',
		themes : 'simple,advanced',
		languages : '<%= usera.getLanguageId().substring(0,2) %>',
		disk_cache : true,
		debug : false
	});
	</script>
<% } %>
	
<script language='javascript' type='text/javascript'>	
	
<jsp:include page="/html/portlet/ext/contentlet/field/tiny_mce_config.jsp"/>
	//### END INIT TINYMCE ###
	//### TINYMCE ###

	
	
</script>	