<%@page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="com.liferay.portal.util.Constants" %>

<%@page import="com.dotmarketing.util.UtilMethods" %>

<script src="/dwr/interface/HostAjax.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>

<script language="JavaScript">
dojo.require("dotcms.dojo.data.StructureReadStore");
dojo.require("dotcms.dojo.push.PushHandler");

/**
 * Uploads multiple files
 *
 * @param uploader
 * @param referer
 * @param operation
 * @return {boolean}
 */
function uploadFiles(uploader, referer, operation) {

    /**
    * Registers and manage the onComplete event for uploaded files
     *
    * @param uploader
    * @param referer
     */
    var uploaderHandler = function (uploader, referer) {

        /*dojo.connect(uploader, "onProgress", function(dataArray){
         //...
         });*/
        dojo.connect(uploader, "onComplete", function(dataArray){
            window.location = referer;
        });
    };

    var form = document.getElementById("fm");
    var nameValueSeparator = "<%=com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR%>";
    var uploadFiles = uploader.getFileList();

    if (uploadFiles.length == 0) {
        alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.alert.please.upload")) %>');
        return false;
    }
    for (var temp = 0; temp < uploadFiles.length; temp++) {
        var fileName = uploadFiles[temp].name;
        if (temp == 0)
            document.getElementById("fileNames").value = fileName;
        else
            document.getElementById("fileNames").value = document.getElementById("fileNames").value + nameValueSeparator + fileName;
    }

    document.getElementById("tableDiv").style.display = "none";
    document.getElementById("messageDiv").style.display = "";

    form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/upload_multiple" /></portlet:actionURL>';
    form.<portlet:namespace />subcmd.value = operation;
    form.cmd.value = "<%= Constants.ADD %>";
    dijit.byId('saveButton').setAttribute('disabled', true);
    if (dijit.byId('savePublishButton') != null) {
        dijit.byId('savePublishButton').setAttribute('disabled', true);
    }

    submitForm(form);

    return true;
}

</script>