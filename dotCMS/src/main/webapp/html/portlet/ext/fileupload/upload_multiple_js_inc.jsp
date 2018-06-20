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
 * @return {boolean}
 */
function uploadFiles(uploader, referer) {

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

    if (dijit.byId("wfActionId").value === "") {
        alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.alert.please.select.a.workflow.action")) %>');
        return false;
    }
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
    form.subcmd.value = "";
    form.cmd.value = "<%= Constants.ADD %>";
    dijit.byId('saveButton').setAttribute('disabled', true);
    if (dijit.byId('savePublishButton') != null) {
        dijit.byId('savePublishButton').setAttribute('disabled', true);
    }

    form = preventNameConflictsOnInputTypeFile(form);
    submitForm(form);

    return true;
}

var actionData = {
    identifier: 'id',
    label: 'name',
    items: [
        { id:'',  name:'' }
    ]};

var actionStore = new dojo.data.ItemFileReadStore({data:actionData});

function loadWorkflowActions(contentTypeId){
    var xhrArgs = {
        url: "/api/v1/workflow/initialactions/contenttype/" + contentTypeId,
        handleAs: "json",
        load: function(data) {
            var results = data.entity;
            fillAvailableWorkflowActions(results);
        },
        error : function(error) {
            showDotCMSSystemMessage(error, true);
        }
    };
    dojo.xhrGet(xhrArgs);
}

function fillAvailableWorkflowActions(actions){
    var items = new Array();
    for(var i=0; i < actions.length; i++){
        items.push({
            id: actions[i].action.id,
            name: actions[i].action.name+" ( "+actions[i].scheme.name+" )"
        });
    }
    actionData = {
        identifier: 'id',
        label: 'name',
        items: items
    };
    actionStore = new dojo.data.ItemFileReadStore({data:actionData});
    dijit.byId("wfActionId").attr('store', actionStore);
}
</script>