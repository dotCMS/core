<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="${id}" class="fileAjaxUploader">
<div id="${name}" class="fajaxUpName"></div>
	<iframe dojoAttachPoint="fileUploadIframe" name="${id}_target_upload" dojoAttachEvent="onload:_fileUploadStart" src="" style="display: none"></iframe>
	<input name="${name}" dojoAttachPoint="fileNameField" type="hidden" value="${fileName}">
	<div dojoAttachPoint="fileUploadForm" style="float:left">
		<form enctype="multipart/form-data" method="post" action="/servlets/ajax_file_upload?fieldName=${name}" 
			dojoAttachPoint="form" target="${id}_target_upload">
			<input name="${name}FileUpload" type="file" dojoAttachEvent="onchange:_doFileUpload" dojoAttachPoint="fileInputField" class="form-text" size="30">
		</form>
	</div>
	<div name="${name}FileName" dojoAttachPoint="fileNameDisplayField" class="fileAjaxUploaderFileName" style="display: none;"></div>
	<div dojoAttachPoint="fileUploadStatus" class="fileAjaxUploaderStatus" style="display: none;">
		 <div dojoType="dijit.ProgressBar" style="width: 100px;" dojoAttachPoint="progressBar"></div><div class="fileAjaxUploaderStatusMsg"><%= LanguageUtil.get(pageContext, "Uploading")%>...</div>
	</div>
	<div dojoAttachPoint="fileUploadRemoveButton" style="display: none;">
		<button dojoType="dijit.form.Button" dojoAttachEvent="onClick:_remove">
			<%= LanguageUtil.get(pageContext, "remove")%>
		</button>
	</div>
	
</div>

