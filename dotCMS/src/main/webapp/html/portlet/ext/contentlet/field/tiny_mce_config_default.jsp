<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>

<%@page import="com.liferay.portal.model.User"%>
<% User userb= com.liferay.portal.util.PortalUtil.getUser(request); %>

var tinyMCEProps = {
	theme: "modern",
	selector: "textarea",
	menubar: true,
	statusbar: true,
	resize: "both",
	plugins: [
		"advlist anchor autolink lists link image charmap print preview hr anchor pagebreak",
		"searchreplace wordcount visualblocks visualchars code fullscreen",
		"table template paste textcolor colorpicker textpattern validation dotimageclipboard compat3x"
	],
	toolbar1: "styleselect | bold italic | bullist numlist outdent indent | image dotimageclipboard | link unlink",
	paste_auto_cleanup_on_paste : true,
	paste_strip_class_attributes : "all",
	paste_as_text: true,
	paste_word_valid_elements: "b,strong,i,em,h1,h2,h3,h4,h5,h6,a",
	convert_urls : true,
	cleanup : true,
	browser_spellcheck:true,
	urlconverter_callback : cmsURLConverter,
	verify_css_classes : false,
	<%
		//Get the default CSS file if doesn't exist load the /css/base.css
		String cssPath = Config.getStringProperty("WYSIWYG_CSS", "/html/css/tiny_mce.css");
		if(InodeUtils.isSet(cssPath)){%>
	<%="content_css : \"" + cssPath + "\","%>
	<%}%>
	trim_span_elements : false,
	apply_source_formatting : true,
	valid_elements : "*[*]",
	relative_urls : true,
	document_base_url : "/",
	plugin_insertdate_dateFormat : "%Y-%m-%d",
	plugin_insertdate_timeFormat : "%H:%M:%S",
	paste_use_dialog : true,
	gecko_spellcheck : true,
	image_advtab: true,
	file_picker_callback: function(callback, value, meta) {
		cmsFileBrowser(callback, value, meta);
	},
	menubar: 'edit insert view table',
	link_class_list: [
		{title: 'None', value: ''},
		{title: 'External Link Disclaimer', value: 'link-disclaimer'}
	],
	style_formats_merge: false,
	style_formats: [
		{title: 'Headers', items: [
			{title: 'Header 2', format: 'h2'},
			{title: 'Header 3', format: 'h3'},
			{title: 'Header 4', format: 'h4'},
		]},
		{title: 'Inline', items: [
			{title: 'Bold', icon: 'bold', format: 'bold'},
			{title: 'Italic', icon: 'italic', format: 'italic'},
			{title: 'Superscript', icon: 'superscript', format: 'superscript'},
			{title: 'Subscript', icon: 'subscript', format: 'subscript'},
		]},
		{title: 'Blocks', items: [
			{title: 'Paragraph', format: 'p'},
			{title: 'Div', format: 'div'},
			{title: 'Pre', format: 'pre'},
			{title: 'Caption', block: 'p', classes: 'caption'}
		]},
		{title: 'Image Alignment', items: [
			{title: 'Image Left', selector: 'img', classes: 'float-left'},
			{title: 'Image Right', selector: 'img', classes: 'float-right'},
			{title: 'Image Centered', selector: 'img', classes: 'float-centered'}
		]},
		{title: 'Quote', items:[
			{title: 'Make Quote', block: 'blockquote', classes: 'quote-box'},
			{title: 'Mark Author', inline: 'span', selector: 'blockquote.quote-box', classes: 'author'}
		]}
	]
};
