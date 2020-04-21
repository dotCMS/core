<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="${id}" class="fileAjaxUploader">
    <input name="${name}" id="${name}ValueField" dojoAttachPoint="fileNameField" type="hidden" value="${fileName}" />
    <div dojoAttachPoint="fileUploadForm" class="fileAjaxUploaderForm">
        <input
            name="${name}FileUpload"
            type="file"
            accept="${accept}"
            dojoAttachEvent="onchange:_doFileUpload"
            dojoAttachPoint="fileInputField"
            class="form-text"
            size="30"
        />
    </div>
    <div dojoAttachPoint="fileUploadStatus" class="fileAjaxUploaderStatus" style="display: none;">
        <div dojoType="dijit.ProgressBar" dojoAttachPoint="progressBar"></div>
        <div class="fileAjaxUploaderStatusMsg"><%= LanguageUtil.get(pageContext, "Uploading")%>...</div>
    </div>
    <div
        name="${name}FileName"
        dojoAttachPoint="fileNameDisplayField"
        class="fileAjaxUploaderFileName"
        style="display: none;"
    ></div>

    <div class="fileAjaxUploaderActions">
        <div dojoAttachPoint="fileUploadInfoButton" style="display: none;">
            <button dojoType="dijit.form.Button" dojoAttachEvent="onClick:_info" iconClass="infoIcon">
                <%= LanguageUtil.get(pageContext, "Info")%>
            </button>
        </div>
        <div dojoAttachPoint="fileUploadRemoveButton" style="display: none;">
            <button
                dojoType="dijit.form.Button"
                dojoAttachEvent="onClick:_remove"
                iconClass="deleteIcon"
                class="dijitButtonDanger"
            >
                <%= LanguageUtil.get(pageContext, "remove")%>
            </button>
        </div>
    </div>
    <input type="hidden" id="maxSizeFileLimit" value="" />
    <div
        id="${id}-Dialog"
        dojoType="dijit.Dialog"
        dojoAttachPoint="fileInfoDialog"
        style="width: 500px;"
        class="noDijitDialogTitleBar"
    ></div>
</div>
