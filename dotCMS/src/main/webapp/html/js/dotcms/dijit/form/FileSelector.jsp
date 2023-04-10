<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div dojoAttachPoint="widgetRootNode" style="${style}">
	
    <div dojoAttachPoint="thumbnailWrapper" class="thumbnailSliderWrapper">
	     <!-- display -->
	    <img dojoAttachPoint="thumbnailImage" src="/html/images/shim.gif" border="1" class="imageFieldThumbnail" />
	</div>
		
	<div class="fileSelectorControls">
		<input name="${name}" type="hidden" value="${value}" dojoAttachPoint="valueTextField"/>
		<input type="text" dojoType="dijit.form.TextBox" name="${name}-filename" readonly="readonly" dojoAttachPoint="labelTextField"/>
		<button dojoType="dijit.form.Button" type="button" dojoAttachEvent="onClick: _browseClicked" iconClass="browseIcon"><%= LanguageUtil.get(pageContext, "browse")%>...</button>
		<button dojoType="dijit.form.Button" type="button" style="display:none;" dojoAttachEvent="onClick: _infoClicked"
			dojoAttachPoint="infoFileButton" iconClass="infoIcon"><%= LanguageUtil.get(pageContext, "Info")%></button>
		<button dojoType="dijit.form.Button" type="button" style="display:none;" dojoAttachEvent="onClick: _removeClicked" class="dijitButtonFlat"
	dojoAttachPoint="removeFileButton" iconClass="deleteIcon"><%= LanguageUtil.get(pageContext, "remove")%></button>
	</div>
	
	<div dojoAttachPoint="fileBrowser" dojoType="dotcms.dijit.FileBrowserDialog"></div>
	
	<div dojoType="dijit.Dialog" dojoAttachPoint="fileInfoDialog" style="width: 500px;" class="noDijitDialogTitleBar"></div>

</div>


