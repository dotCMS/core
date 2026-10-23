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

var dotCMSHasLicense = <%=licenseLevel > licenseStandard%>

var tinyMCEProps = {
    dotLanguageStrings: {
        edit_image: "<%=editImage%>",
        propertiesLabel: "<%=propertiesLabel%>",
        insertImageLabel: "<%=insertImageLabel%>"
    },
    selector: "textarea",
    menubar: 'false',
    statusbar: false,
    resize: true,
    // "theme" (was "modern") is gone in 8.x — the silver theme is the only one shipped and no
    // longer needs to be named. Dropped from this list: "print" (no such plugin/button in 8.x),
    // "paste"/"textcolor"/"colorpicker"/"textpattern" (merged into core, no plugin declaration
    // needed — their toolbar/behavior still works). "doteditimage" folded in directly rather
    // than pushed on after construction (see below) — no ordering constraint requires the split
    // in TinyMCE 8's plugin-registry model, unlike the sequential loading this worked around
    // originally; confirm in manual QA if anything relied on the old load order.
    //
    // Found in manual QA: this MUST be one plugin name per array element (or a single
    // space-separated string) — TinyMCE 8 tries to load each array element as one literal plugin
    // name verbatim, spaces included, instead of splitting on whitespace the way 4.x tolerated.
    // The original multi-name-per-element grouping (kept during the initial port to mirror the
    // 4.9.6 source's line breaks) 404'd on a plugin literally named
    // "emoticons validation dotimageclipboard dotCustomButtons doteditimage".
    plugins: [
        "advlist", "anchor", "autolink", "lists", "link", "image", "charmap", "preview",
        "searchreplace", "wordcount", "visualchars", "fullscreen",
        "emoticons", "validation", "dotimageclipboard", "dotCustomButtons", "doteditimage"
    ],
    block_formats: 'Paragraph=p;Header 1=h1;Header 2=h2;Header 3=h3;Header 4=h4;Header 5=h5;Pre=pre;Code=code;Remove Format=removeformat',
    toolbar1: "formatselect | bold italic underline strikethrough | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | dotAddImage dotimageclipboard  | link unlink anchor | hr charmap | fullscreen | validation",
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
    // "gecko_spellcheck" removed here — found in manual QA as a real deprecation warning: it was
    // a Firefox-specific option already gone in TinyMCE 8.0 (browser_spellcheck below, declared
    // twice in the original source, is the option that still exists).
    browser_spellcheck: true,
    image_advtab: true,
    image_caption: true,
    file_picker_callback: function(callback, value, meta) {
        cmsFileBrowser(callback, value, meta);
    },
    // dotCMS ships TinyMCE under the GPL license, never commercial.
    license_key: "gpl",
    // TinyMCE 6+ shows a premium-features promotion element by default; 4.9.6 had no such thing,
    // so this keeps the toolbar matching the pre-upgrade look.
    promotion: false
};
