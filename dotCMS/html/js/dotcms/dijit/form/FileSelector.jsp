<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div dojoAttachPoint="widgetRootNode" style="${style}">
	
    <div dojoAttachPoint="thumbnailWrapper" style="display: none;" class="thumbnailSliderWrapper">    
	   	<div dojoType="dijit.form.VerticalSlider" 
			maximum="100"
			minimum="350"
			showButtons="true"
			intermediateChanges="true"
			style="width:20px; height: 80px;"'
			discreteValues="6"
			dojoAttachPoint="thumbnailSizeSlider">
		 		
	  		<div dojoType="dijit.form.VerticalRule" count="6" style="height:5px;"></div>
		</div>
	     <!-- display -->
	    <img dojoAttachPoint="thumbnailImage" src="/html/images/shim.gif" border="1" class="imageFieldThumbnail" />
	</div>
		
	<div class="fileSelectorControls">
		<input name="${name}" type="hidden" value="${value}" dojoAttachPoint="valueTextField"/>
		<input type="text" dojoType="dijit.form.TextBox" name="${name}-filename" readonly="readonly" style="width: 250px;float: left;" dojoAttachPoint="labelTextField"/>
		<button dojoType="dijit.form.Button" dojoAttachEvent="onClick: _browseClicked" type="button" iconClass="browseIcon"><%= LanguageUtil.get(pageContext, "browse")%>...</button>
		<button dojoType="dijit.form.Button" style="display:none;" dojoAttachEvent="onClick: _removeClicked" 
			dojoAttachPoint="removeFileButton" iconClass="deleteIcon"><%= LanguageUtil.get(pageContext, "remove")%></button>
		<button dojoType="dijit.form.Button" style="display:none;" dojoAttachEvent="onClick: _infoClicked" 
			dojoAttachPoint="infoFileButton" iconClass="infoIcon"><%= LanguageUtil.get(pageContext, "info")%></button>
	</div>
	
	<div dojoAttachPoint="fileBrowser" dojoType="dotcms.dijit.FileBrowserDialog">
	</div>
	
	<div dojoType="dijit.Dialog" dojoAttachPoint="fileInfoDialog" style="width: 500px;"></div>

</div>


