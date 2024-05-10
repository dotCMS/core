<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotmarketing.filters.CMSFilter" %>
<%@ page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.variant.VariantAPI" %>
<%

	String catCount = (String) request.getAttribute("counter");
	String isURLMap = (String) request.getParameter("isURLMap");
%>

<script language='javascript' type='text/javascript'>
    dojo.require("dojox.layout.ContentPane");
    //http://jira.dotmarketing.net/browse/DOTCMS-2273
    var workingContentletInode = "<%= contentlet.getInode() %>";
    var currentContentletInode = "";
    var isAutoSave = false;
    var isCheckin = true;
    var isContentAutoSaving = false;
    var isContentSaving = false;
    var doesUserCancelledEdit = false;

    // We define this variable when we load this page from an iframe in the ng edit page
    var ngEditContentletEvents;

    var tabsArray=new Array();

    dojo.require("dijit.Dialog");

    var assignToDialog = new dijit.Dialog({
        id:"assignToDialog"
    });


    //Tabs manipulation
    function displayProperties(id) {

        for(i =0;i< tabsArray.length ; i++){
            var ele = document.getElementById(tabsArray[i] + "_tab");

            if (ele != undefined) {
                if(tabsArray[i] == id){
                    ele.className = "alpha";
                    document.getElementById(tabsArray[i]).style.display = "";
                }
                else{
                    ele.className = "beta";
                    document.getElementById(tabsArray[i]).style.display = "none";
                }
            }
        }
    }







    var myForm = document.getElementById('fm');
    var copyAsset = false;

    var inode = '<%=contentlet.getInode()%>';
    var referer = '<%=java.net.URLEncoder.encode(referer,"UTF-8")%>';


    //Full delete contentlet action
    function submitfmDelete()
    {
        var href =  '<portlet:actionURL>';
        href +=		'	<portlet:param name="struts_action" value="<%= formAction %>" />';
        href +=		'	<portlet:param name="cmd" value="full_delete" />';
        href +=		'</portlet:actionURL>';

        form = document.getElementById("fm");
        form.action = href;
        form.submit();
    }

    //Versions management
    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.delete.content.version")) %>')){
            var xhrArgs = {
                url: '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId  + '&referer=' + referer,
                handle : function(dataOrError, ioArgs) {


                    refreshVersionCp();

                    showDotCMSSystemMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.full_delete")) %>");

                }
            };
            dojo.xhrGet(xhrArgs);



        }
    }
    function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.replace.version")) %>')){
            getVersionBack(objId)
        }
    }

    function getVersionBack(inode) {
        window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=getversionback&inode=' + inode + '&inode_version=' + inode  + '&referer=' + referer;
        setTimeout(() => {
            ngEditContentletEvents?.next({
                name: 'save',
                data: {
                    identifier: null,
                    inode: inode,
                    type: null
                }
            });
        }, 100);
    }

    function editVersion(objId) {
        window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=edit&inode=' + objId  + '&referer=' + referer;
    }

    function emmitCompareEvent(inode, identifier, language) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "compare-contentlet",
            data: { inode, identifier, language }
        });
        document.dispatchEvent(customEvent)
    }



    function openAssignTo()
    {
        if(dojo.isFF){
            assignToDialog._fillContent(dojo.byId('assignTaskInnerDiv'));
            assignToDialog.show();
        }
        else{
            var win=dijit.byId('assignTaskDiv');

            win.show();
        }

    }

    function cancelAssignTo() {
        if(dojo.isFF){
            assignToDialog.hide();
        }
        else{
            dijit.byId('assignTaskDiv').hide();
        }
    }




    //Structure change
    function structureSelected()
    {
        //migrateSelectedStructure();
        var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
        href += "<portlet:param name='struts_action' value='<%= formAction %>' />";
        href += "<portlet:param name='cmd' value='new' />";
        href += "<portlet:param name='referer' value='<%=referer%>' />";
        //This parameter is used to determine if the selection comes from Content Manager is EditContentletAction
        href += "<portlet:param name='selected' value='true' />";
        href += "<portlet:param name='inode' value='' />";
        href += "</portlet:actionURL>";
        document.forms[0].action = href;
        document.forms[0].submit();
    }


    //Review content changes control
    function reviewChange() {
        var obj = dijit.byId("reviewContent");
        enable = obj.checked;
        <%if (UtilMethods.isSet(structure.getReviewerRole())) {%>

        var reviewContentDate = document.getElementById("reviewContentDate");
        if(reviewContentDate == undefined){
            return;
        }
        var reviewIntervalNum = document.getElementById("reviewIntervalNumId");
        var reviewIntervalSel = document.getElementById("reviewIntervalSelectId");
        if (enable) {
            reviewContentDate.style.display='';
            reviewIntervalNum.disabled = false;
            reviewIntervalSel.disabled = false;
        } else {
            reviewContentDate.style.display='none';
            reviewIntervalNum.disabled = true;
            reviewIntervalSel.disabled = true;
        }

        <%}%>
    }

    //Copy contentlet action
    function copyContentlet()
    {
        var href =  '<portlet:actionURL>';
        href +=		'<portlet:param name="struts_action" value="<%= formAction %>" />';
        href +=		'<portlet:param name="cmd" value="copy" />';
        href +=		'</portlet:actionURL>';

        form = document.getElementById("fm");
        form.action = href;
        form.submit();
    }

    function editContentletURL (objId) {
        var loc = '';
        loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId;
        return loc;
    }



    function addTab(tabid){
        tabsArray.push(tabid);
    }



    function submitParent(param) {
        if (copyAsset) {
            disableButtons(myForm);
            var parent = document.getElementById("parent").value;
            self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="copy" /><portlet:param name="inode" value="<%=String.valueOf(contentlet.getInode())%>" /></portlet:actionURL>&parent=' + parent + '&referer=' + referer;
        }

        //WYSIWYG_CALLBACKS
        if (param == 'wysiwyg_image') {
            var imageName = document.getElementById("selectedwysiwyg_image").value;
            var imageFolder = document.getElementById("folderwysiwyg_image").value;
            var ident = document.getElementById("selectedIdentwysiwyg_image").value;
            wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = "/contentAsset/raw-data/" + ident + "/fileAsset";
            wysiwyg_win.ImageDialog.showPreviewImage("/contentAsset/raw-data/" + ident + "/fileAsset");
        }
        if (param == 'wysiwyg_file') {
            var fileName = document.getElementById("selectedwysiwyg_file").value;
            var ident = document.getElementById("selectedIdentwysiwyg_file").value;
            var fileFolder = document.getElementById("folderwysiwyg_file").value;
            <% String ext = CMSFilter.CMS_INDEX_PAGE; %>

            if(fileName.lastIndexOf('<%=ext%>') + '<%=ext%>'.length == filename.length){
                wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = fileFolder + fileName;
            }else{
                wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = "/contentAsset/raw-data/" + ident + "/fileAsset";
            }
        }
    }



    <% if(Config.getIntProperty("CONTENT_AUTOSAVE_INTERVAL",0) > 0){%>
    // http://jira.dotmarketing.net/browse/DOTCMS-2273
    var autoSaveInterval = <%= Config.getIntProperty("CONTENT_AUTOSAVE_INTERVAL",0) %>;
    setInterval("saveContent(true)",autoSaveInterval);
    <%}%>


    function getFormData(formId,nameValueSeparator){ // Returns form data as name value pairs with nameValueSeparator.

        var formData = new Array();

        //Taking the text from all the textareas
        var k = 0;
        dojo.query('textarea', dojo.byId(formId)).forEach(
            function (textareaObj) {
                var aceEditor;
                if(textareaObj.id == aceTextId[textareaObj.id]) {
                    aceEditor = textEditor[aceTextId[textareaObj.id]];
                } else{
                    aceEditor = aceEditors[textareaObj.id];
                }
                if ((textareaObj.id != "") && (aceEditor != null)) {
                    try {
                        if(aceEditor.getValue() != "")
                            document.getElementById(textareaObj.id).value=aceEditor.getValue();
                    } catch (e) {
                    }
                }
            }
        );

        var formElements = document.getElementById(formId).elements;

        var formDataIndex = 0; // To collect name/values from multi-select,text-areas.

        for(var formElementsIndex = 0; formElementsIndex < formElements.length; formElementsIndex++,formDataIndex++){


            // Collecting only checked radio and checkboxes
            if((formElements[formElementsIndex].type == "radio") && (formElements[formElementsIndex].checked == false)){
                continue;
            }

            if((formElements[formElementsIndex].type == "checkbox") && (formElements[formElementsIndex].checked == false)&& (formElements[formElementsIndex].id != "fieldNeverExpire")){
                continue;
            }

            // Collecting selected values from multi select
            if(formElements[formElementsIndex].type == "select-multiple") {
                for(var multiSelectIndex = 0; multiSelectIndex < formElements[formElementsIndex].length; multiSelectIndex++){

                    if(formElements[formElementsIndex].options[multiSelectIndex].selected == true){
                        formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].options[multiSelectIndex].value;
                        formDataIndex++;
                    }
                }
                continue;
            }

            // Getting values from text areas
            if(formElements[formElementsIndex].type == "textarea" && formElements[formElementsIndex].id != '') {
                if(tinyMCE.get(formElements[formElementsIndex].id) != null){
                    textAreaData = tinyMCE.get(formElements[formElementsIndex].id).getContent();
                    formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;
                    continue;
                }
                if(tinyMCE.get(formElements[formElementsIndex].id) == null){
                    textAreaData = document.getElementById(formElements[formElementsIndex].id).value;
                    formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;
                    continue;
                }
            }

            formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].value;
        }

        // Categories selected in the Category Dialog

        var catCount = <%=UtilMethods.isSet(catCount)?Integer.parseInt(catCount):0 %>;


        for(var i=1; i<catCount+1; i++) {

            if (typeof eval("addedStore"+i)!="undefined"){
                eval("addedStore"+i).fetch({onComplete:function(items) {
                    for (var i = 0; i < items.length; i++){
                        formData[formData.length] = "categories"+nameValueSeparator+items[i].id[0];
                    }
                }});
            }

        }

		var variantName = sessionStorage.getItem('<%=VariantAPI.VARIANT_KEY%>');

		if (variantName) {
			formData[formData.length] = '<%=VariantAPI.VARIANT_KEY%>' + nameValueSeparator + variantName;
		} else {
			formData[formData.length] = '<%=VariantAPI.VARIANT_KEY%>' + nameValueSeparator + 'DEFAULT';
		}

        return formData;

    }



    function publishContent(){
        persistContent(isAutoSave, true);

    }

    function saveContent(isAutoSave){

        persistContent(isAutoSave, false);
    }


    async function persistContent(isAutoSave, publish){

        window.onbeforeunload=true;
        var isAjaxFileUploading = false;
        var alertFileAssetSize = false;
        var size = 0;
        var maxSizeForAlert = <%= Config.getIntProperty("UPLOAD_FILE_ASSET_MAX_SIZE",30) %>
            dojo.query(".fajaxUpName").forEach(function(node, index, arr){
                FileAssetAjax.getFileUploadStatus(node.id,{callback: function(fileStats) {
                    if(fileStats!=null){
                        isAjaxFileUploading = true;
                    }
                }, async:false});

            });
        var maxSize = document.getElementById("maxSizeFileLimit");
        if(maxSize) {
            size = maxSize.value;

            if(size>maxSizeForAlert*1024*1024){
                alertFileAssetSize=true;
            }

            if(alertFileAssetSize){
                document.getElementById('maxSizeFileAlert').innerHTML='<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "alert-file-too-large-takes-lot-of-time"))%>'
            }else{
                document.getElementById('maxSizeFileAlert').innerHTML='';
            }
        }
        if(isAjaxFileUploading){
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-wait-until-all-files-are-uploaded")) %>');
            return false;
        }

        if(doesUserCancelledEdit){
            return false;
        }

        if(isContentAutoSaving){ // To avoid concurrent auto and normal saving.
            return;
        }
        window.scrollTo(0,0);	// To show lightbox effect(IE) and save content errors.
        dijit.byId('savingContentDialog').show();

        // Check if the relations have not been loaded.
        if(!allRelationsHaveLoad()) {
            await waitForRelation();
        }

        if(isAutoSave && isContentSaving){
            return;
        }
        var textAreaData = "";
        var fmData = new Array();

        fmData = getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>");

        if(isInodeSet(currentContentletInode)){
            isCheckin = false;
            isAutoSave=false;
        }

        if(isAutoSave){
            isContentAutoSaving = true;
        }else {
            isContentSaving = true;
        }

        const isURLMapContent = "<%=isURLMap%>" === "true";

        /**
         * If the content is a URLMap, we need to wait until the re-index process is done.
         * This is beacuse we may need to redirect the user to the new URLMap contentlet.
         * We need to wait until the re-index process is done to avoid a 404 error.
         * More info: https://github.com/dotCMS/core/issues/21818
         */
        const newSaveContentCallBack = isURLMapContent ? (data) => setTimeout(() => saveContentCallback(data), 1800) : saveContentCallback;

        ContentletAjax.saveContent(fmData, isAutoSave, isCheckin, publish, newSaveContentCallBack);
    }







    function createLockedWarningDiv(){

        //handle if the node is locked &&  editable and node is locked &! editable
        var lockedNode = dojo.byId("lockContentButton") ;
        var destroyAfterClick = true;
        var endColor = "#ffffff";
        if(!lockedNode){
            if(!dojo.byId("unlockContentButton")){
                lockedNode = dojo.byId("lockedTextInfoDiv");
                endColor = "#eeeeee";
                destroyAfterClick = false;
            }
        }


        if(lockedNode){
            dojo.query(".wrapperRight").forEach(function(node){
                var n = dojo.create("div", {className:"lockedWarningDiv"}, node, "first");
                dojo.connect(n, "onmousedown", null,function(){
                    dojo.query(".lockedWarningDiv").forEach(function(node){
                        if(destroyAfterClick){
                            dojo.destroy(node);
                        }
                    });
                    dojo.animateProperty({
                        node: lockedNode,
                        duration: 1000,
                        properties: {
                            backgroundColor: {
                                start: "#FA5858",
                                end: endColor
                            },

                        },
                        onEnd: function() {

                        }
                    }).play();
                });

            })
        }
        else{
            dojo.query(".lockedWarningDiv").forEach(function(node){
                dojo.destroy(node);
            });
        }
    }




    dojo.ready(
        function(){
            createLockedWarningDiv();
            resetHasChanged();
            scrollToTop()
        }
    );

    var _bodyKeyDown;
    var _bodyMouseDown;
    var _hasUserChanged  = false;

    dojo.require('dojox.fx.scroll');

    function emmitUserHasChange(val) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-contentlet-data-updated",
            payload: val
        });
        document.dispatchEvent(customEvent)
    }

    function scrollToTop() {
        try {
            dojox.fx.smoothScroll({
                node: dojo.query('#mainTabContainer_tablist')[0],
                win: dojo.query('.portlet-wrapper')[0]
            }).play();
        }
        catch (e) {
            console.error('Error smoothScroll()', e);
        }

    }

    function resetHasChanged(){
        _hasUserChanged = false;
        emmitUserHasChange(_hasUserChanged);
        _bodyKeyDown = dojo.connect(dojo.body(), "onkeydown", null,markHasChanged);

        dojo.query(".wrapperRight").forEach(function(node){
            _bodyMouseDown = dojo.connect(node, "onmousedown", null,markHasChanged);
            return;
        })
    }

    function markHasChanged($event){
        if ($event.key != 'Escape') {
            _hasUserChanged = true;
            emmitUserHasChange(_hasUserChanged);
            dojo.disconnect(_bodyKeyDown);
            dojo.disconnect(_bodyMouseDown);
        }
    }


    function saveContentCallback(data){
        isContentAutoSaving = false;
        dojo.byId("subcmd").value= "";

        if(data["contentletInode"] != null && isInodeSet(data["contentletInode"])){
            $('contentletInode').value = data["contentletInode"];
            currentContentletInode = data["contentletInode"];
            contentAdmin.contentletInode = data["contentletInode"];
            contentAdmin.contentletIdentifier = data["contentletIdentifier"];

            //After a save operation we need to update the underlying url associated to the language on the dropdown.
            //if we don't it'll continue to show the pre-popoulate lang dialog.
            let allLangContentlets = data["allLangContentlets"];
			if((typeof (storeData) != 'undefined') && allLangContentlets){
			// Pretty much every lang-contentlets instance is retrieved here.
			// So we match them with the lang displayed on the dropdown and update the underlying url.
                var arrayLength = allLangContentlets.length;
                for (let i = 0; i < arrayLength; i++) {
                    let contentlet = allLangContentlets[i];
                    let entry = storeData.items.filter(item => item.id[0] == contentlet.languageId);
                    if(entry != undefined && entry.length > 0){
                        let url = entry[0].value[0];
                        let newUrl = queryStringUrlReplacement(url, 'inode', contentlet.inode);
                            newUrl = queryStringUrlReplacement(url, 'identifier', contentlet.identifier);
                        entry[0].value[0] = newUrl;
                    }
                }
			}
        }

		//if we're looking at a File Asset and there's a resource link element on screen after save it  needs to be updated
        var resourceLink = dojo.byId('resourceLink');
		if(resourceLink){
            var xhrArgs = {
                url: "/api/v1/content/fileassets/"+currentContentletInode+"/resourcelink",
                handleAs: "json",
                preventCache: true,
                load: function (data) {
					if(data.entity && data.entity.resourceLink){
                       var resourceLinkData = data.entity.resourceLink;
                       resourceLink.text = shortenString(resourceLinkData.text, 100);
                       resourceLink.href = resourceLinkData.href;
					} else {
                        showDotCMSSystemMessage('Failed to update resource link', true);
					}
                },
                error: function (error) {
                    console.error(error.responseText);
                }
            }
            dojo.xhrGet(xhrArgs);
        }

        dijit.byId('savingContentDialog').hide();
        resetHasChanged();
        // Show DotContentletValidationExceptions.
        if(data["saveContentErrors"] && data["saveContentErrors"][0] != null ){
            var errorDisplayElement = dijit.byId('saveContentErrors');
            var exceptionData = data["saveContentErrors"];
            var errorList = "";
            for (var i = 0; i < exceptionData.length; i++) {
                var error = exceptionData[i];
                if (error) {
                    errorList = errorList + "<li>" + error + "</li>";
                }
            }

            var messages;
            if (errorList.length) {
                messages = "<ul>" + errorList + "</ul>";
            } else {
                messages = "<p><%= LanguageUtil.get(pageContext, "required-fields") %></p>"
            }

            dojo.byId('exceptionData').innerHTML = messages;
            dojo.byId("wfActionId").value=""; // hack to let the user choose save instead of wfAction
            errorDisplayElement.show();
            return;
        }

        var versionsTab = dijit.byId("versionsTab");

        if(versionsTab){
            versionsTab.attr("disabled", false);
        }

        var permissionsTab = dijit.byId("permissionsTab");

        if(permissionsTab){
            permissionsTab.attr("disabled", false);
        }


        refreshActionPanel(data["contentletInode"]);

        // If the contentlet is a urlContentMap, we need to reload the page
        data.shouldReloadPage = "<%=isURLMap%>" === "true";

        // if we have a referer and the contentlet comes back checked in
        var customEventDetail = {
            name: 'save-page',
            payload: data
        };


        if (data["contentletIdentifier"]) {
            if (ngEditContentletEvents) {
                ngEditContentletEvents.next({
                    name: 'save',
                    data: {
                        identifier: data.contentletIdentifier,
                        inode: data.contentletInode,
                        type: null
                    }
                });
            }

            if((data["referer"] != null && data["referer"] != '' && !data["contentletLocked"])) {
                if (data['isHtmlPage'] && workingContentletInode.length === 0 && !data["referer"].includes("relend")) {
                    var params = data['htmlPageReferer'].split('?')[1].split('&');
                    var languageQueryParam = params.find(function(queryParam) {
                        return queryParam.startsWith('com.dotmarketing.htmlpage.language');
                    });
                    var languageId = languageQueryParam.split('=')[1];

                    customEventDetail = {
                        name: 'close',
                        data: {
                            redirectUrl: data['htmlPageReferer'].split('?')[0],
                            languageId
                        }
                    };
                }
            }
        } else {
            customEventDetail = {
                name: 'close'
            };

            if (data['contentletBaseType'] === 'HTMLPAGE') {
                customEventDetail = {
                    name: 'deleted-page',
                    payload: data
                };
            }

            if (ngEditContentletEvents) {
                ngEditContentletEvents.next({
                    name: 'deleted-contenlet',
                    data: data
                });
            }
        }
        var customEvent = document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false, customEventDetail);
        document.dispatchEvent(customEvent);

    }

    function refreshPermissionsTab(){
        var y = Math.floor(Math.random() * 1123213213);

        var dojoDigit=dijit.byId("permissionsRoleSelector-rolesTree")
        if (dojoDigit) {
        	dojoDigit.destroyRecursive(false);
        }

        var myCp = dijit.byId("contentletPermissionCp");
        if (myCp) {
        	myCp.destroyRecursive(false);
        }
        var myDiv = dojo.byId("permissionsTabDiv");
        if(myDiv){
            dojo.empty(myDiv);
        }
        myCp = new dojox.layout.ContentPane({
            id : "contentletPermissionCp",
            style: "height:100%",
            href: "/html/portlet/ext/contentlet/edit_permissions_tab_inc_wrapper.jsp?contentletId=" +contentAdmin.contentletIdentifier + "&languageId=<%= contentlet.getLanguageId() %>" + "&r=" + y
        }).placeAt(myDiv);
    }



    function refreshVersionCp(){
        var x = dijit.byId("versions");
        var y =Math.floor(Math.random()*1123213213);

		const variantName = window.sessionStorage.getItem('variantName') || 'DEFAULT';

        var myCp = dijit.byId("contentletVersionsCp");
        if (myCp) {
            myCp.attr("href", "/html/portlet/ext/contentlet/contentlet_versions_inc.jsp?variantName=" +  variantName + "&contentletId=" +contentAdmin.contentletIdentifier + "&r=" + y);
            return;
        }
        var myDiv = dijit.byId("contentletVersionsDiv");
        if (myDiv) {
            dojo.empty(myDiv);
        }

		myCp = new dijit.layout.ContentPane({
            id : "contentletVersionsCp",
            style: "height:100%",
            href: "/html/portlet/ext/contentlet/contentlet_versions_inc.jsp?variantName=" +  variantName + "&contentletId=" +contentAdmin.contentletIdentifier + "&r=" + y
        }).placeAt("contentletVersionsDiv");
    }



    function refreshRulesCp(){

        var y =Math.floor(Math.random()*1123213213);

        var myCp = dijit.byId("contentletRulezDivCp");

        if (myCp) {
            return;

        }
        var myDiv = dijit.byId("contentletRulezDiv");

        if (myDiv) {
            dojo.empty(myDiv);
        }
        var hideRulePushOptions = false
        myCp = new dojox.layout.ContentPane({
            id : "contentletRulezDivCp",
            style: "height:100%",
            href:  "/api/portlet/rules/include?id=" +contentAdmin.contentletIdentifier + "&r=" + y+"&hideRulePushOptions="+hideRulePushOptions
        }).placeAt("contentletRulezDiv");


    }




    //*************************************
    //
    //
    //  ContentAdmin Obj
    //
    //
    //*************************************


    dojo.require("dotcms.dojo.push.PushHandler");

    dojo.declare("dotcms.dijit.contentlet.ContentAdmin", null, {
        contentletIdentifier : "",
        contentletInode : "",
        languageID : "",
        wfActionId:"",
        constructor : function(contentletIdentifier, contentletInode,languageId ) {
            this.contentletIdentifier = contentletIdentifier;
            this.contentletInode =contentletInode;
            this.languageId=languageId;

        },

        executeWfAction: function(wfId, popupable, showpush){
            this.wfActionId = wfId;
            if(popupable){
                var inode = (currentContentletInode != undefined && currentContentletInode.length > 0)
                    ? currentContentletInode
                    :workingContentletInode;

                var publishDate = '<%=structure.getPublishDateVar()%>';
                var expireDate =  '<%=structure.getExpireDateVar()%>';
                var structInode = '<%=structure.getInode()%>';

                let workflow = {
                    actionId:wfId,
                    inode:inode,
                    publishDate:publishDate,
                    expireDate:expireDate,
                    structInode:structInode
                };
                var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Workflow-Action")%>');
                pushHandler.showWorkflowEnabledDialog(workflow, saveAssignCallBack, false, true);

            } else{
                dojo.byId("wfActionId").value=wfId;
                saveContent(false);
            }

        }
    });

    function saveAssignCallBackAngular (actionId, formData) {
        // END: PUSH PUBLISHING ACTIONLET
        dojo.byId("wfActionAssign").value = formData.assign;
        dojo.byId("wfActionComments").value = formData.comments;
        dojo.byId("wfActionId").value = actionId;
        dojo.byId("wfPathToMove").value = formData.pathToMove;

        // BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("wfPublishDate").value = formData.publishDate;
        dojo.byId("wfPublishTime").value = formData.publishTime;
        dojo.byId("wfExpireDate").value = formData.expireDate;
        dojo.byId("wfExpireTime").value = formData.expireTime;
        dojo.byId("wfWhereToSend").value = formData.whereToSend;
        dojo.byId("wfiWantTo").value = formData.iWantTo;
        dojo.byId("wfFilterKey").value = formData.filterKey;
        dojo.byId("wfTimezoneId").value = formData.timezoneId;
        // END: PUSH PUBLISHING ACTIONLET

        saveContent(false);
    }


    function saveAssignCallBack(actionId, formData) {
        var pushPublish = formData.pushPublish;
        var assignComment = formData.assignComment;

        var comments = assignComment.comment;
        var assignRole = assignComment.assign;

        var whereToSend = pushPublish.whereToSend;
        var publishDate = pushPublish.publishDate;
        var publishTime = pushPublish.publishTime;
        var expireDate  = pushPublish.expireDate;
        var expireTime  = pushPublish.expireTime;
        var forcePush   = pushPublish.forcePush;
        var neverExpire = pushPublish.neverExpire;

        // END: PUSH PUBLISHING ACTIONLET
        dojo.byId("wfActionAssign").value = assignRole;
        dojo.byId("wfActionComments").value = comments;
        dojo.byId("wfActionId").value = actionId;

        // BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("wfPublishDate").value = publishDate;
        dojo.byId("wfPublishTime").value = publishTime;
        dojo.byId("wfExpireDate").value = expireDate;
        dojo.byId("wfExpireTime").value = expireTime;
        dojo.byId("wfNeverExpire").value = neverExpire;
        dojo.byId("wfWhereToSend").value = whereToSend;
        // END: PUSH PUBLISHING ACTIONLET

        saveContent(false);

    }

    var contentAdmin = new dotcms.dijit.contentlet.ContentAdmin('<%= contentlet.getIdentifier() %>','<%= contentlet.getInode() %>','<%= contentlet.getLanguageId() %>');

    function makeEditable(contentletInode){
        ContentletAjax.lockContent(contentletInode, checkoutContentletCallback);
        dojo.empty("contentletActionsHanger");
        dojo.byId("contentletActionsHanger").innerHTML="<div style='text-align:center;padding-top:20px;'><span class='dijitContentPaneLoading'></span></div>";

    }

    function checkoutContentletCallback(data){
        if(data["Error"]){
            showDotCMSSystemMessage(data["Error"], true);
            return;
        }


        refreshActionPanel(data["lockedIdent"]);

    }


    function stealLock(contentletInode){
        ContentletAjax.unlockContent(contentletInode, stealLockContentCallback);
    }

    function stealLockContentCallback(data){

        if(data["Error"]){
            showDotCMSSystemMessage(data["Error"], true);
            return;
        }


        refreshActionPanel(data["lockedIdent"]);

    }




    function unlockContent(contentletInode){

        window.onbeforeunload=true;


        ContentletAjax.unlockContent(contentletInode, unlockContentCallback);
        //dojo.empty("contentletActionsHanger");
        //dojo.byId("contentletActionsHanger").innerHTML="<div style='text-align:center;padding-top:20px;'><span class='dijitContentPaneLoading'></span></div>";

    }


    function unlockContentCallback(data){

        if(data["Error"]){
            showDotCMSSystemMessage(data["Error"], true);
            return;
        }

        refreshActionPanel(data["lockedIdent"]);


    }



    function refreshActionPanel(inode){
        var myCp = dijit.byId("contentActionsCp");
        if (myCp) {
            myCp.destroyRecursive(true);
        }

        dojo.empty("contentletActionsHanger");
        myCp = new dojox.layout.ContentPane({
            id 			: "contentActionsCp"

        }).placeAt("contentletActionsHanger");

        myCp.attr("href",  "/html/portlet/ext/contentlet/contentlet_actions_wrapper.jsp?contentletInode=" + inode);
    }
    function toggleLockedMessage(locked, who, when){
        if(dojo.byId("contentLockedInfo")){
            if (locked) {
                dojo.empty("contentLockedInfo");
                myTR  = dojo.byId("contentLockedInfo");
                dojo.create("th", {innerHTML:'<%= LanguageUtil.get(pageContext, "Locked") %>:'}, myTR);
                dojo.create("td", {innerHTML: who + ' : <span class="lockedAgo">(' + when + ')</span>'}, myTR);

                if(dojo.style("contentLockedInfo",'height') <30){
                    dojo.animateProperty({
                        node: dojo.byId("contentLockedInfo"),
                        duration: 300,
                        properties: {
                            height: {
                                start: 0,
                                end: 30
                            },

                        }
                    }).play();
                }
            }else {
                if(dojo.style("contentLockedInfo",'height') >29){
                    dojo.animateProperty({
                        node: dojo.byId("contentLockedInfo"),
                        duration: 300,
                        properties: {
                            height: {
                                start: 30,
                                end: 0
                            },

                        },
                        onEnd: function() {
                            dojo.empty("contentLockedInfo");
                        }
                    }).play();
                }
            }
        }
    }



</script>
