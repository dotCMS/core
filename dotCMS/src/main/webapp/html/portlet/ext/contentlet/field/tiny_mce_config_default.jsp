<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.filters.CMSUrlUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.portal.model.User"%>

<% 

String cssPath = Config.getStringProperty("WYSIWYG_CSS", "/application/wysiwyg/wysiwyg.css");
int licenseLevel = LicenseUtil.getLevel();
int licenseStandard = LicenseLevel.STANDARD.level;
Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
if(!CMSUrlUtil.getInstance().amISomething(cssPath, host, WebAPILocator.getLanguageWebAPI().getLanguage(request).getId())){
  cssPath=null;
}
String editImage = LanguageUtil.get(pageContext, "edit");
String propertiesLabel = LanguageUtil.get(pageContext, "properties");
String insertImageLabel = LanguageUtil.get(pageContext, "insert-image");

%>

var dotCMS = dotCMS || {};
dotCMS.hasLicense = <%=licenseLevel > licenseStandard%>

var tinyMCEProps = {
    dotLanguageStrings: {
    <%if(editImage!=null){ %>
      edit_image: "<%=editImage%>",
      propertiesLabel: "<%=propertiesLabel%>",
      insertImageLabel: "<%=insertImageLabel%>"
    <%} %>
    },
    theme: "modern",
    selector: "textarea",
    menubar: 'false',
    statusbar: false,
    resize: "true",
    plugins: [
        "advlist anchor autolink lists link image charmap print  hr anchor ",
        "searchreplace wordcount visualchars fullscreen ",
        "emoticons  paste textcolor colorpicker textpattern validation dotimageclipboard dotCustomButtons"
    ],
    block_formats: 'Paragraph=p;Header 1=h1;Header 2=h2;Header 3=h3;Header 4=h4;Header 5=h5;Pre=pre;Code=code;Remove Format=removeformat',
    toolbar1: "formatselect | bold italic underline strikethrough | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | dotAddImage dotimageclipboard  | link unlink anchor | hr charmap | fullscreen | validation",
    paste_auto_cleanup_on_paste : true,
    paste_strip_class_attributes : "all",
    convert_urls : true,
    cleanup : true,
    browser_spellcheck:true,
    urlconverter_callback : cmsURLConverter,
    verify_css_classes : false,
    <%if(cssPath!=null){ %>
    content_css: "<%=cssPath %>",
    <%} %>
    trim_span_elements : false,
    apply_source_formatting : false,
    valid_elements : "*[*]",
    relative_urls : true,
    document_base_url : "/",
    plugin_insertdate_dateFormat : "%Y-%m-%d",
    plugin_insertdate_timeFormat : "%H:%M:%S",
    paste_use_dialog : true,
    gecko_spellcheck : true,
    browser_spellcheck: true,
    image_advtab: true,
    image_caption: true,
    file_picker_callback: function(callback, value, meta) {
        debugger;
        cmsFileBrowser(callback, value, meta);
    },
    init_instance_callback: function (editor) {
        debugger;
        editor.on('PostProcess', function (e) {
            debugger;
            e.content += 'My custom content!';
        });
    }
};

tinyMCEProps.plugins.push("doteditimage");
