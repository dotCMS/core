<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI" %>
<%@page import="com.dotmarketing.business.CacheLocator"%>

<%
Structure defaultFileAssetStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
    String selectedLang=String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());
    if(session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED)!= null){
        selectedLang = (String) session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
    }
%>


<style type="text/css" media="all">
    .nameTD {
        max-width: 200px;
        overflow: hidden;
    }
    .nameTD .assetRef {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    .nameTD .assetName {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
</style>

<script type="text/javascript" src="/dwr/interface/HostAjax.js"></script>

<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>

<% // Include javascript method to upload multiple files %>
<%@ include file="/html/portlet/ext/fileupload/upload_multiple_js_inc.jsp" %>
<script language="JavaScript">

    dojo.require("dotcms.dojo.data.StructureReadStore");
    dojo.require("dotcms.dojo.push.PushHandler");
    //Global Variables
    var openFolders = new Array ();

    var selectedContent;
    var lastNameDivClicked;
    var lastShowOnMenuClicked;

    //Change name variables
    var changingShowOnMenuTo;
    var lastShowOnMenu;

    //Tree objects
    var inodes = new Array ();

    //ContentObject
    var contentInodes = new Array ();
    var contentDraggables = new Array ();

    //State variables
    var selectedFolder = "";
    var lastSelectedFolder = "";
    var activeHost = "";
    var lastActiveHost = "";
    var showArchived = false;
    var doubleClicked = false;
    var actionLoading = false;
    var markedForCopy = "";
    var markedForCut = "";

    //Images
    var selectedFolderImg = "folderSelectedIcon";
    var noSelectedFolderImg = "folderIcon";
    var worldImgOnImg = 'hostIcon'
    var worldImgOffImg = 'hostStoppedIcon'
    var worldAniIMG = 'hourglassIcon';
    var plusSignIMG = 'toggleOpenIcon';
    var lessSignIMG = 'toggleCloseIcon';
    var folderAniIMG = 'folderIcon';
    var shimIMG = 'shimIconSmall';
    var trashIMG = 'trah';

    var dragging = false;

    //Events Handlers

    var myHost = '<%= (myHost != null) ? myHost.getHostname() : ""%>';
    var myHostId = '<%= (myHost != null) ? myHost.getIdentifier() : "" %>';

    var selectedLang = '<%= selectedLang %>';




    //Dragging Events

    var AssetsDragObserver = Class.create();

    AssetsDragObserver.prototype = {
        initialize: function() {
        },

        onDrag: function(eventName, draggable, e) {
            var element = draggable.element;
            if (e.ctrlKey)
                element.style.cursor='url(/portal/images/icons/copy_asset.ico), default';
            else
                element.style.cursor='default';
        },

        onStart: function(eventName, draggable, e) {
            showDebugMessage('start dragging');
            var element = draggable.element;
            var handle = draggable.handle;
            dragging = true;
        },

        onEnd: function(eventName, draggable, e) {
            showDebugMessage('end dragging');
            var element = draggable.element;
            element.style.cursor='default';
            setTimeout('disablingDragging()', 500);
        }
    };

    function disablingDragging () {
        dragging = false;
    }

    Draggables.addObserver(new AssetsDragObserver());



    //----------------------------------
    //Tree Event Handlers

    function getRootFiles(e){
        var inode = null;
        try{
            inode = getInodeFromID(Event.element(e).id);
        }catch(e){}
        if(inode==null || inode===''){
            inode = activeHost;
        }
        treeFolderSelected(inode);
    }


    function treeHostRefMouseUp(e) {
        var inode = getInodeFromID(Event.element(e).id);
        if(isRightClick(e)) {
            showDebugMessage('treeHostRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "right-click-over-host")) %>: ' + inode);
            showHostPopUp(inodes[inode], cmsAdminUser, referer, e);
            return;
        } else {
            showDebugMessage('treeHostRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left-click-over-host")) %>: ' + inode);
            treeHostSelected(inode, e);
            return;
        }
    }

    function treeFolderRefMouseUp(e) {
        var inode = getInodeFromID(Event.element(e).id);
        if(isRightClick(e)) {
            showDebugMessage('treeFolderRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "right-click-over-folder")) %>: ' + inode);
            showFolderPopUp(inodes[inode], cmsAdminUser, referer, e);
            return;
        } else {
            showDebugMessage('treeFolderRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left-click-over-folder")) %>: ' + inode);
            treeFolderSelected(inode);
            return;
        }
    }

    function treeFolderSignRefMouseUp(e) {
        var inode = getInodeFromID(Event.element(e).id);
        if(isRightClick(e)) {
            showDebugMessage('treeFolderSignRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "right-click-over-folder-sign")) %>: ' + inode);
        } else {
            showDebugMessage('treeFolderSignRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left-click-over-folder-sign")) %>: ' + inode);
            treeFolderSignSelected(inode, e);
            return;
        }
    }

    function trashRefMouseUp (e) {
        if(isRightClick(e)) {
            showDebugMessage('trashRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "rigth-click-over-trash")) %>');
        } else {
            showDebugMessage('trashRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left-click-over-trash")) %>');
            trashSelected(e);
            return;
        }
    }

    function contentAreaRefMouseUp(e) {
        if(isRightClick(e)) {
            showDebugMessage('contentAreaRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "right-click-over-content-area")) %>');
        } else {
            showDebugMessage('contentAreaRefMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left-click-over-content-area")) %>');
            //executeChangeName();
        }
        return false;
    }

   function toggleArchive(archiveCheckboxValue){
       if (selectedFolder != null && isInodeSet(selectedFolder)) {
           //Emptying the assets rigth hand side listing
           cleanContentSide();
           showArchived = !!archiveCheckboxValue;

           //Showing the loading message
           Element.show('loadingContentListing');
           BrowserAjax.openFolderContent (selectedFolder, '', showArchived, selectedLang, selectFolderContentCallBack);
       }
   }

    function droppedOnTrash(draggableElem, droppableElem, e) {
        showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Dragged-to-trash")) %>: ' + droppableElem + ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "the-element")) %>: " + draggableElem + ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ctrl-pressed")) %>: " + e.ctrlKey);
        var inode = inodes[draggableElem];
        if (inode.type != 'folder' && !inode.deleted) {
            $(draggableElem + "-TR").style.display = "none";

            var response = false;

            if (inode.type == 'file_asset') {
                response = archiveFile(draggableElem, referer);
            }
            if (inode.type == 'links') {
                response = archiveLink(draggableElem, referer);
            }
            if (inode.type == 'htmlpage') {
                response = archiveHTMLPage(draggableElem, referer);
            }

            if (!response) {
                var folder = selectedFolder;
                selectedFolder = "";
                treeFolderSelected(folder);
            }
        } else {
            showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Asset-already-archived")) %>');
        }
    }



    function contentNameDIVClicked(e)
    {
        var inode = getInodeFromID(Event.element(e).id);
        showDebugMessage('contentDIVClicked: inode = ' + inode +
            ', selectedContent = ' + selectedContent);
        return false;
    }



    function contentTRMouseUp(e) {
        var inode = getInodeFromID(Event.element(e).id);
        showDebugMessage('contentTRMouseUp: inode = ' + inode);
        if(isRightClick(e)) {
            showDebugMessage('contentTRMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "right-click-over-tr")) %>: ' + inode + ', <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "type")) %>: ' + inodes[inode].type);
            if (inodes[inode].type == 'folder') {
                showFolderPopUp(inodes[inode], cmsAdminUser, referer, e);
            }
            if (inodes[inode].type == 'file_asset') {
                showFilePopUp(inodes[inode], cmsAdminUser, referer, e);
            }
            if (inodes[inode].type == 'links') {
                showLinkPopUp(inodes[inode], cmsAdminUser, referer, e);
            }
            if (inodes[inode].type == 'htmlpage') {
                showHTMLPagePopUp(inodes[inode], cmsAdminUser, referer, e);
            }
        } else {
            showDebugMessage('contentTRMouseUp: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "left click over tr")) %>: ' + inode + ', <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "type")) %>: ' + inodes[inode].type);
            if(inode != selectedContent) {
                if(selectedContent != null) {
                    var tr = selectedContent + "-TR";
                    if ($(tr) != null) {
                        removeClass(tr, 'contentRowSelected');
                    }
                }
                var tr = inode + "-TR";
                if (hasClass(tr, 'contentRowRoller'))
                    removeClass(tr, 'contentRowRoller');
                addClass(tr, 'contentRowSelected');
                selectedContent = inode;
            }

        }
        return false;
    }

    function contentTRDoubleClicked(e) {
    	console.log(e.target.dataset);
        doubleClicked = true;

        var inode = getInodeFromID(Event.element(e).id);

        showDebugMessage('contentTRDoubleClicked: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Double-click-over")) %>: ' + inode);
        if (inodes[inode].type == 'folder') {
            showDebugMessage(DWRUtil.toDescriptiveString(inodes[inode], 2));
            openFolder(inode);
        }
        if (inodes[inode].type == 'file_asset') {
            if(inodes[inode].isContent){
                editFileAsset(inode, inodes[inode].fileAssetType);
            }else{
                editFile(inode,referer);
            }
        }
        if (inodes[inode].type == 'links') {
            editLink(inode,referer);
        }
        if (inodes[inode].type == 'htmlpage') {

            previewHTMLPage(e.target.dataset.url || e.target.parentNode.dataset.url, inodes[inode]);
        }
        return;
    }

    function contentTRRightClicked(inode, e) {
        showDebugMessage('contentTRRightClicked: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Right-click-over")) %>: ' + inode);
    }

    function mouseOverContent(e) {
        var inode = getInodeFromID(Event.element(e).id);
        if(selectedContent == null || inode != selectedContent) {
            var tr = inode + "-TR";
            addClass(tr, 'contentRowRoller');
        }
    }

    function mouseOutContent(e) {
        var inode = getInodeFromID(Event.element(e).id);
        if(selectedContent == null || inode != selectedContent) {
            var tr = inode + "-TR";
            if (hasClass(tr, 'contentRowRoller'))
                removeClass(tr, 'contentRowRoller');
        }
    }

    function droppedOnFolder(draggable, droppable, e) {

        var draggableElem = getInodeFromID(draggable.id);
        var droppableElem = getInodeFromID(droppable.id);

        showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Dragged-to")) %>: ' + droppableElem + ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "the-element")) %>: " + draggableElem + ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ctrl-pressed")) %>: " + e.ctrlKey);

        if (droppableElem != selectedFolder) {
            if (!e.ctrlKey) {
                $(draggableElem + "-TR").style.display = "none";
            }

            var asset = inodes[draggableElem];
            if (asset.type == 'folder') {
                if (e.ctrlKey) {
                    copyFolder (draggableElem, droppableElem, referer);
                } else {
                    if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-move-the-folder")) %>'))
                        moveFolder (draggableElem, droppableElem, referer);
                    else {
                        var folder = selectedFolder;
                        selectedFolder = "";
                        treeFolderSelected(folder);
                    }
                }
            }
            if (asset.type == 'file_asset') {
                if (e.ctrlKey) {
                    copyFile (draggableElem, droppableElem, referer);
                } else {
                    if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-move-the-file")) %>'))
                        moveFile (draggableElem, droppableElem, referer);
                    else {
                        var folder = selectedFolder;
                        selectedFolder = "";
                        treeFolderSelected(folder);
                    }
                }
            }
            if (asset.type == 'links') {
                if (e.ctrlKey) {
                    copyLink (draggableElem, droppableElem, referer);
                } else {
                    if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-move-the-link")) %>'))
                        moveLink (draggableElem, droppableElem, referer);
                    else {
                        var folder = selectedFolder;
                        selectedFolder = "";
                        treeFolderSelected(folder);
                    }
                }
            }
            if (asset.type == 'htmlpage') {
                if (e.ctrlKey) {
                    copyHTMLPage (draggableElem, droppableElem, referer);
                } else {
                    if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-move-the-page")) %>'))
                        moveHTMLPage (draggableElem, droppableElem, referer);
                    else {
                        var folder = selectedFolder;
                        selectedFolder = "";
                        treeFolderSelected(folder);
                    }
                }
            }
        }
    }

    function droppedOnHost(draggable, droppable, e) {

        var draggableElem = getInodeFromID(draggable.id);
        var droppableElem = getInodeFromID(droppable.id);

        showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Dragged-to-host")) %>: ' + droppableElem + ':' + droppable +
            ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "the-element")) %>: " + draggableElem + ':' + draggable +
            ", <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ctrl-pressed")) %>: " + e.ctrlKey);

        if (!e.ctrlKey) {
            $(draggableElem + "-TR").style.display = "none";
        }

        var asset = inodes[draggableElem];
        if (asset.type == 'folder') {
            if (e.ctrlKey) {
                copyFolder (draggableElem, droppableElem, referer);
            } else {
                if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-move-the-folder")) %>')) {
                    moveFolder (draggableElem, droppableElem, referer);
                    return true;
                }
            }
        } else {
            showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "only-folder-can-be-moved-to-hosts")) %>');
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Only-folders-can-be-moved-over-host")) %>');
        }
        var folder = selectedFolder;
        selectedFolder = "";
        treeFolderSelected(folder);
        return false;
    }

    //Actions


    //---------------------------------------------------
    //Tree Actions

    function initializeTree (data) {
        //Emptying the assets rigth hand side listing
        DWRUtil.removeAllOptions('TreeUL');

        var hostId = "";
        var isAHostOpen = false;

        for (var i = 0; i < data.length; i++) {
            var host = data[i];

            var read = hasReadPermissions(host.permissions);
            var write = hasWritePermissions(host.permissions);
            var publish = hasPublishPermissions(host.permissions);

            if (!read) continue;

            hostId = host.identifier;
            var hostLI = document.createElement('li');
            hostLI['id']= host.identifier + '-TreeLI';
            if(host.open) {
                var image = worldImgOnImg;
                activeHost = host.identifier;
                isAHostOpen = true;
            } else {
                var image = worldImgOffImg;
            }

            Element.update(hostLI,
                '   <div style="font-weight: bold;padding: 10px 8px 10px 10px;font-size: 13px;background:#eee;">\n' +
                '       <a onmouseover="this.className=\'underline\'" onmouseout="this.className=\'\'" id="' + hostId + '-TreeREF">\n' +
                '		<i class="fa fa-globe" style="opacity:.6"></i>' +
                '           ' + host.hostname + '\n' +
                '       </a>\n' +
                '   </div>\n' +
                '   <ul id="' + host.identifier + '-TreeChildrenUL"></ul>\n');
            $('TreeUL').appendChild(hostLI);

            Event.observe(hostId + '-TreeREF', 'mouseup', treeHostRefMouseUp);

            Event.observe(hostId + '-TreeREF', 'click', getRootFiles);

            if (write) {
                Droppables.add(hostId+'-TreeREF',
                    {
                        hoverclass: 'boldunderline',
                        onDrop: droppedOnHost
                    });
            }

            //adding the children
            if(host.open) {
                var children = host.childrenFolders;
                for (var j = 0; j < children.length; j++) {
                    addChildFolder(hostId, children[j]);
                }
            }

            inodes[host.identifier] = host;
        }

        if(!isAHostOpen) {
            treeHostSelected(null, null);
        }

        Element.hide($('loadingContentListing'));

        if(selectedFolder===''){
            getRootFiles();
        }

        renderAddNewDropDownButton(activeHost, selectedFolder, referer);
    }

    function addChildFolder (parent, folder) {

        var folderName = folder.name;

        var read = hasReadPermissions(folder.permissions);
        var write = hasWritePermissions(folder.permissions);
        var publish = hasPublishPermissions(folder.permissions);

        if (!read) return;

        if (folder.selected) {
            var folderIMG = selectedFolderImg;
            var folderClass = 'folderSelected';
            selectedFolder = folder.inode;
        } else {
            var folderIMG = noSelectedFolderImg;
            var folderClass = '';
        }

        if(folder.open) {
            if(folder.childrenFolders.length == 0)
                var signIMG = shimIMG;
            else
                var signIMG = lessSignIMG;
        } else {
            var signIMG = plusSignIMG;
        }

        if(folder.open)
            openFolders[openFolders.length] = folder.inode;

        var folderLI = document.createElement('li');
        folderLI['id']= folder.inode + '-TreeLI';
        Element.update(folderLI,
            '<span id="' + folder.inode + '-TreeSignREF"><span class="' + signIMG + '" id="' + folder.inode + '-TreeSignIMG"></span></span>\n' +
            '<a class="' + folderClass + '" onmouseover="addClass(\'' + folder.inode + '-TreeREF\', \'underline\');" onmouseout="removeClass(\'' + folder.inode + '-TreeREF\', \'underline\'); return false;" id="' + folder.inode + '-TreeREF">\n' +
            '   <span class="' + folderIMG + '" id="' + folder.inode + '-TreeFolderIMG"></span>\n' +
            '   <span id="' + folder.inode + '-TreeFolderName" title="' + folderName + '">' + shortenString(folderName, 25) + '</span>' +
            '</a>\n' +
            '<ul id="' + folder.inode + '-TreeChildrenUL"></ul>\n');
        $(parent + '-TreeChildrenUL').appendChild(folderLI);

        //Attaching events
        if (write) {
            Droppables.add(folder.inode+'-TreeREF',
                {
                    hoverclass: 'boldunderline',
                    onDrop:droppedOnFolder
                });
        }

        Event.observe(folder.inode + '-TreeREF', 'mouseup', treeFolderRefMouseUp);

        Event.observe(folder.inode + '-TreeSignREF', 'mouseup', treeFolderSignRefMouseUp);



        if(folder.selected) {
            //Emptying the assets rigth hand side listing
            cleanContentSide();

            //Showing the loading message
            Element.show('loadingContentListing');

            selectedFolder = folder.inode;
            BrowserAjax.openFolderContent (folder.inode, '', showArchived, selectedLang, selectFolderContentCallBack);
        }

        if(folder.open) {
            var children = folder.childrenFolders;
            for (var j = 0; j < children.length; j++) {
                addChildFolder(folder.inode, children[j]);
            }
        }
        inodes[folder.inode] = folder;

    }

    function treeHostSelected(inode, e) {
        if(activeHost != inode) {
            if(isInodeSet(inode) && $(inode + '-TreeHostIMG') != null) {
                $(inode + '-TreeHostIMG').className = worldAniIMG;
            }
            lastActiveHost = activeHost;
            activeHost = inode;
            BrowserAjax.openHostTree(inode, function (data) { openHostTreeCallBack(inode, data) } );
        }
    }

    function openHostTreeCallBack (hostId, data) {
        if (hostId == activeHost) {
            cleanContentSide();
            if(isInodeSet(lastActiveHost) && $(lastActiveHost + '-TreeHostIMG') != null) {
                $(lastActiveHost + '-TreeHostIMG').className = worldImgOffImg;
                DWRUtil.removeAllOptions(lastActiveHost + '-TreeChildrenUL');
            }

            if($(hostId + '-TreeHostIMG'))
            {
                $(hostId + '-TreeHostIMG').className = worldImgOnImg;
                for (var j = 0; j < data.length; j++) {
                    addChildFolder(hostId, data[j]);
                }
            }
            else
            {
                Element.hide($('loadingContentListing'));
            }
        }
    }

    function trashSelected (e) {
        showDebugMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "trash-left-clicked")) %>');
        showArchived = !showArchived;
        if (showArchived)
            Element.update('trashLabel', '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Hide-Archived")) %>');
        else
            Element.update('trashLabel', '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Show-Archived")) %>');
        if (selectedFolder != null && isInodeSet(selectedFolder)) {
            //Emptying the assets rigth hand side listing
            cleanContentSide();

            //Showing the loading message
            Element.show('loadingContentListing');
            BrowserAjax.openFolderContent (selectedFolder, '', showArchived, selectedLang, selectFolderContentCallBack);
        }
    }

    //Left click over folder left hand side
    function treeFolderSelected(inode, lang) {

        if (selectedFolder == inode && selectedLang == lang)
            return;

        if(!lang)
            lang = selectedLang;

        //Changing icons and css classes
        if (isInodeSet(selectedFolder) && selectedFolder!=activeHost &&  $(selectedFolder + '-TreeREF') != null) {
            removeClass(selectedFolder + '-TreeREF', 'folderSelected');
            $(selectedFolder + '-TreeFolderIMG').className = noSelectedFolderImg;
        }

        selectedFolder = inode;
        if(inode && inode!=activeHost){
            $(inode + '-TreeFolderIMG').className = selectedFolderImg;
            $(inode + '-TreeREF').className = 'folderSelected';
        }

        //Emptying the assets rigth hand side listing
        cleanContentSide();

        //Showing the loading message
        Element.show('loadingContentListing');

        //Calling ajax
        BrowserAjax.openFolderContent (inode, '', showArchived, lang, selectFolderContentCallBack);

        //Opening folder at the left side
        if(!openFolders.contains(inode)) {
            treeFolderSignSelected(inode);
        }

    }

    function cleanContentSide () {
        for (var i = 0; i < contentInodes.length; i++) {
            var asset = contentInodes[i];

            var read = hasReadPermissions(asset.permissions);
            var write = hasWritePermissions(asset.permissions);
            var publish = hasPublishPermissions(asset.permissions);

            if($(asset.inode + '-DIV') != null)
            {
                Event.stopObserving(asset.inode + '-DIV', 'mouseup', contentNameDIVClicked);
                Event.stopObserving(asset.inode + '-TR', 'mouseup', contentTRMouseUp);
                Event.stopObserving(asset.inode + '-TR', 'dblclick', contentTRDoubleClicked);
                Event.stopObserving(asset.inode + '-TR', 'mouseout', mouseOutContent);
                Event.stopObserving(asset.inode + '-TR', 'mouseover', mouseOverContent);
            }

            if (write) {
                Droppables.remove(asset.inode + '-DIV');
            }

        }

        for (var i = 0; i < contentDraggables.length; i ++) {
            contentDraggables[i].destroy();
        }

        //Emptying the assets rigth hand side listing
        DWRUtil.removeAllRows('assetListBody');

        contentInodes = new Array();

        contentDraggables = new Array();
    }

    //AJAX callback to load the left hand side of the browser
    function selectFolderContentCallBack (content) {

        var subFoldersCount = 0;

        //Loading the contents table at the rigth hand side
        var table = $('assetListBody');
        for (var i = 0; i < content.length; i++) {

            var asset = content[i];
            inodes[asset.inode] = asset;

            contentInodes[contentInodes.length] = asset;

            var read = hasReadPermissions(asset.permissions);
            var write = hasWritePermissions(asset.permissions);
            var publish = hasPublishPermissions(asset.permissions);

            if (!read) continue;

            if (asset.type == 'folder') {
                subFoldersCount++;
                var order = '';
                if (asset.sortOrder > 0)
                    order = asset.sortOrder;
                else
                    order = "&nbsp;&nbsp;&nbsp;";

                var html = '<tr id="' + asset.inode + '-TR">\n' +
                    '    <td class="nameTD" id="' + asset.inode + '-NameTD">\n' +
                    '    <a class="assetRef" id="' + asset.inode + '-DIV" href="javascript:;">\n' +
                    '        <span class="folderIcon" id="' + asset.inode + '-ContentIcon"></span>\n' +
                    ' <span class="assetName" id="' + asset.inode + '-NameSPAN" title="' + asset.name + '">' + asset.name + '</span>\n' +
                    '    </a>\n' +
                    '    </td>\n' +
                    '    <td class="menuTD" id="' + asset.inode + '-MenuTD">\n' +
                    '        <span id="' + asset.inode + '-ShowOnMenuSPAN"';
                if (asset.showOnMenu) {
                    html = html + '>';
                }
                else {
                    html = html + ' style= "color: #C6C7C8;">';
                }
                html = html + order + '</span>\n' +
                    '   </td>\n' +
                    '   <td class="statusTD" id="' + asset.inode + '-StatusTD"></td>\n' +
                    '   <td class="descriptionTD" id="' + asset.inode + '-DescTD"></td>\n' +
                    '   <td class="modUserTD" id="' + asset.inode + '-ModUserTD"></td>\n' +
                    '   <td class="modDateTD" id="' + asset.inode + '-ModDateTD"></td>\n' +
                    '</tr>\n';

                new Insertion.Bottom(table, html);

                //Attaching events
                Event.observe(asset.inode + '-DIV', 'mouseup', contentNameDIVClicked);
                Event.observe(asset.inode + '-TR', 'mouseup', contentTRMouseUp);
                Event.observe(asset.inode + '-TR', 'dblclick', contentTRDoubleClicked);
                Event.observe(asset.inode + '-TR', 'mouseout', mouseOutContent);
                Event.observe(asset.inode + '-TR', 'mouseover', mouseOverContent);

                if (publish) {
                    /*
                        reverteffect: Needs to return the element to its place.
                        We can't use the default reverteffect function because the initial element doesn't have an initial
                        value of top_offset or left_offset.

                        More info: https://madrobby.github.io/scriptaculous/draggable/
                    */
                    var draggable = new Draggable(asset.inode + '-DIV', { ghosting:true, revert:true, zindex: 1000, reverteffect: function(element) { element.style.position = 'relative'; element.style.width = 'auto' } });
                    contentDraggables[contentDraggables.length] = draggable;
                }

                if (write) {
                    Droppables.add(asset.inode + '-DIV',
                        {
                            hoverclass: 'boldunderline',
                            onDrop: droppedOnFolder
                        });
                }

            } else {
                var name = asset.title;
                name = typeof name !== 'undefined'? name: asset.identifier;
                var assetIcon = '/icon?i=' + name.toLowerCase();
                if (asset.type == 'file_asset') {
                    name = asset.fileName;
                    assetIcon = asset.extension + 'Icon';
                }
                if (asset.type == 'htmlpage') {
                    name = asset.pageUrl;
                    assetIcon = 'pageIcon';
                }
                if (asset.type == 'links')
                    assetIcon = 'linkIcon';
                var order = '';
                if (asset.sortOrder > 0)
                    order = asset.sortOrder;
                else
                    order = "&nbsp;";

                var assetPrettyDate = getPrettyDate(asset.modDate);

                var assetFullName = name;

                //processing asset description and name to avoid long words that break the column width
                name = shortenLongWords(name, 30)
                var title = shortenString(asset.title, 30);
                var modUserName = shortenString(asset.modUserName, 20);
                //Show Language Icon for Contents (Pages, Files)
                var langIcon = asset.languageFlag;
                var langCode = asset.languageCode + "_" + asset.countryCode;
                if(asset.countryCode==""){
                	langCode = asset.languageCode;
                }
                var languageHTML = ((asset.type=='htmlpage' || asset.type=='file_asset') && asset.isContentlet && multipleLanguages)
                    ?"<img src=\"/html/images/languages/"+ langIcon +
                    ".gif\" width=\"16px\" height=\"11px\" style='margin-top:4px;float:left;' /><span id='"+asset.inode+"-LangSPAN'>&nbsp;("+ langCode +")</span>":"";
                if(asset.type == 'file_asset'){
                    var html =  '<tr id="' + asset.inode + '-TR">\n' +
                        '   <td class="nameTD" id="' + asset.inode + '-NameTD">' +
                        '<a class="assetRef" id="' + asset.inode + '-DIV" href="javascript:;">\n' +
                        '<span class="uknIcon ' + assetIcon + '" id="' + asset.inode + '-ContentIcon"></span>\n' +
                        '<span class="assetName" id="' + asset.inode + '-NameSPAN" title="' + assetFullName +'" >' + name + '</span>' +
                        '</a>' +
                        '   </td>\n' +
                        '   <td class="menuTD" id="' + asset.inode + '-MenuTD">\n' +
                        '       <span id="' + asset.inode + '-ShowOnMenuSPAN"';
                }else{
                    var html =  '<tr data-url="' +  asset.pageURI +'"id="' + asset.inode + '-TR">\n' +
                        '   <td class="nameTD" id="' + asset.inode + '-NameTD">' +
                        '<a class="assetRef" id="' + asset.inode + '-DIV" href="javascript:;" data-url="' +  asset.pageURI +'">\n' +
                        '<span style="pointer-events: none" class="uknIcon ' + assetIcon + '" id="' + asset.inode + '-ContentIcon"></span>\n' +
                        '<span style="pointer-events: none" class="assetName" id="' + asset.inode + '-NameSPAN" title="' + assetFullName +'" >' + name + '</span>' +
                        '</a>' +
                        '   </td>\n' +
                        '   <td class="menuTD" id="' + asset.inode + '-MenuTD">\n' +
                        '       <span id="' + asset.inode + '-ShowOnMenuSPAN"';
                }


                if (asset.showOnMenu || asset.showOnMenu == 'true') {
                    html = html + '>';
                }
                else {
                    html = html + ' style= "color: #C6C7C8;">';
                }
                html = html + order + '</span>\n' +
                    '   </td>\n' +
                    '   <td class="statusTD" id="' + asset.inode + '-StatusTD">\n' +
                    getStatusHTML (asset) +
                    '   </td>\n' +
                    '   <td style="padding:0px" class="descriptionTD" id="' + asset.inode + '-DescriptionTD">' +
                    '<table style="width:100%;">' +
                    '<tr style="height:15px">' +
                    '<td style="padding:0px; border:0px; width:80px;">'+ languageHTML +
                    '</td>' +
                    '<td style="text-align:left; border:0px">'+title+'</td>' +
                    ' </tr>' +
                    '</table>' +
                    '</td>\n' +
                    '   <td class="modUserTD" id="' + asset.inode + '-ModUserTD">' + modUserName + '</td>\n' +
                    '   <td class="modDateTD" id="' + asset.inode + '-ModDateTD">' + assetPrettyDate + '</td>\n' +
                    '</tr>\n';

                new Insertion.Bottom('assetListBody', html);

                //Attaching events
                Event.observe(asset.inode + '-DIV', 'mouseup', contentNameDIVClicked);
                Event.observe(asset.inode + '-TR', 'mouseup', contentTRMouseUp);
                Event.observe(asset.inode + '-TR', 'dblclick', contentTRDoubleClicked);
                Event.observe(asset.inode + '-TR', 'mouseout', mouseOutContent);
                Event.observe(asset.inode + '-TR', 'mouseover', mouseOverContent);

                if((!asset.live && write) || (asset.live && publish)) {
                    /*
                        reverteffect: Needs to return the element to its place.
                        We can't use the default reverteffect function because the initial element doesn't have an initial
                        value of top_offset or left_offset.

                        More info: https://madrobby.github.io/scriptaculous/draggable/
                    */
                    var draggable = new Draggable(asset.inode + '-DIV', { ghosting:true, revert:true, zindex: 1000, reverteffect: function(element) { element.style.position = 'relative'; element.style.width = 'auto' } });
                    contentDraggables[contentDraggables.length] = draggable;
                }
            }
        }

        Element.hide('loadingContentListing');

        if(subFoldersCount > 0) {
            var signIMG = lessSignIMG;
        } else {
            var signIMG = shimIMG;
        }
        try{
            $(selectedFolder + '-TreeSignIMG').className = signIMG;
        }catch(e){}
        renderAddNewDropDownButton(activeHost, selectedFolder, referer);
    }

    function getStatusHTML (asset) {
        var table = document.createElement( 'table' );
        table.setAttribute('class','browserTableStatus');
        table.innerHTML = asset.statusIcons;
        var spans = Array.prototype.slice.call(table.children);

        while (table.firstChild) {
          table.removeChild(table.firstChild);
        }

        var tds = new Array();
        for (var i = 0; i < spans.length; i++) {
           var td = document.createElement( 'td' );
           td.appendChild(spans[i]);
           tds.push(td);
        }

        var tr = document.createElement( 'tr' );
        for (var i = 0; i < tds.length; i++) {
           tr.appendChild(tds[i]);
        }

        table.appendChild(tr);
        return table.outerHTML;
    }

    function getPrettyDate (date) {

        var localOffset = date.getTimezoneOffset();
        <%
            TimeZone tz = APILocator.systemTimeZone();
            int offset = tz.getOffset((new Date()).getTime());
        %>
        var serverOffset = <%= offset / 1000 / 60 %>;

        date.setMinutes(date.getMinutes() + localOffset + serverOffset);

        var ampm = "AM";

        var month = date.getMonth() + 1;
        if (month < 10) month = "0" + month;

        var day = date.getDate();
        if (day < 10) day = "0" + day;

        var str = month + '/' + day + '/' + date.getFullYear() + ' ';
        var hour = date.getHours();
        if (hour > 12) {
            ampm = "PM";
            hour = (hour - 12);
        } else {
            hour = hour;
        }
        //hour
        if (hour < 10) hour = "0" + hour;
        str += hour
        //minutes / seconds
        minutes = date.getMinutes();
        if (minutes < 10) minutes = "0" + minutes;
        seconds = date.getSeconds();
        if (seconds < 10) seconds = "0" + seconds;
        str += ':'+minutes+':'+seconds+ ' ' + ampm;

        return str;
    }

    function treeFolderSignSelected(inode, e) {
        if(openFolders.contains(inode)) {
            var signImgId = inode + '-TreeSignIMG';

            $(signImgId).className = plusSignIMG;

            DWRUtil.removeAllOptions(inode + '-TreeChildrenUL');

            openFolders.remove(inode);
            BrowserAjax.closeFolderTree(inode, function (data) { } );
        } else {
            if(inode!=activeHost){
                var imgId = inode + '-TreeFolderIMG';
                $(imgId).className = folderAniIMG;
                BrowserAjax.openFolderTree (inode, function (data) { openTreeFolderCallBack(inode, data); } );
            }
        }
    }

    function openTreeFolderCallBack (parent, data) {
        var signImgId = parent + '-TreeSignIMG';
        var imgId = parent + '-TreeFolderIMG';

        for (var j = 0; j < data.length; j++) {
            addChildFolder(parent, data[j]);
        }

        if (selectedFolder == parent)
            $(imgId).className = selectedFolderImg;
        else
            $(imgId).className = noSelectedFolderImg;

        if(data.length == 0) {
            $(signImgId).className = shimIMG;
        } else {
            $(signImgId).className = lessSignIMG;
        }

        openFolders.add(parent);
    }


    // ----------------------------------------------
    //Content Section Actions

    function openFolder (inode, e) {
        var folder = inodes[inode];
        if (!openFolders.contains(folder.parent)) {
            var imgId = folder.parent + '-TreeFolderIMG';
            $(imgId).className = folderAniIMG;
            var parent = folder.parent;
            var params = {
                callback:function(data) { openTreeFolderCallBack(parent, data); },
                async:false
            };
            BrowserAjax.openFolderTree (folder.parent, params);
        }
        treeFolderSelected(inode);
    }

    function reloadContent () {
        //Emptying the assets rigth hand side listing
        cleanContentSide();

        //Showing the loading message
        Element.show('loadingContentListing');

        BrowserAjax.openFolderContent (selectedFolder, '', showArchived, selectedLang, selectFolderContentCallBack);
    }

    function changeContentSort (sortField) {
        if (selectedFolder != null && selectedFolder != '' && isInodeSet(selectedFolder)) {
            //Emptying the assets rigth hand side listing
            cleanContentSide();

            //Showing the loading message
            Element.show('loadingContentListing');

            BrowserAjax.openFolderContent (selectedFolder, sortField, showArchived, selectedLang, selectFolderContentCallBack);
        }
    }








    function markForCut (objId, parentId, referer) {
        if ($(markedForCopy + '-TR') != null) {
            if(Element.hasClassName(markedForCopy + '-TR', 'opaque'))
                removeClass(markedForCopy + '-TR', 'opaque');
        }

        if ($(markedForCut + '-TR') != null) {
            if(Element.hasClassName(markedForCut + '-TR', 'opaque'))
                removeClass(markedForCut + '-TR', 'opaque');
        }

        if (inodes[markedForCut] != null && inodes[markedForCut].type == 'folder' && $(markedForCut + '-TreeREF') != null) {
            removeClass(markedForCut + '-TreeREF', 'opaque');
        }

        if ($(objId + '-TR') != null)
            addClass(objId + '-TR', 'opaque');

        if (inodes[objId].type == 'folder' && $(objId + '-TreeREF') != null)
            addClass(objId + '-TreeREF', 'opaque');

        markedForCopy = "";
        markedForCut = objId;
        var name = inodes[objId].friendlyName;
        if (inodes[objId].type == 'folder')
            name = inodes[objId].name;
        showDotCMSSystemMessage(name +' '+ '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "marked-for-cut")) %>');
    }

    function markForCopy (objId, parentId, referer) {
        if ($(markedForCopy + '-TR') != null) {
            if(Element.hasClassName(markedForCopy + '-TR', 'opaque'))
                removeClass(markedForCopy + '-TR', 'opaque');
        }

        if ($(markedForCut + '-TR') != null) {
            if(Element.hasClassName(markedForCut + '-TR', 'opaque'))
                removeClass(markedForCut + '-TR', 'opaque');
        }

        if (inodes[markedForCut] != null && inodes[markedForCut].type == 'folder' && $(markedForCut + '-TreeREF') != null) {
            removeClass(markedForCut + '-TreeREF', 'opaque');
        }

        markedForCut = "";
        markedForCopy = objId;

        var name = inodes[objId].friendlyName;
        if (inodes[objId].type == 'folder')
            name = inodes[objId].name;

        showDotCMSSystemMessage(name + ' '+'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "marked-for-copy")) %>');
    }



    //---------------------------------------------------------------------------------------------------------
    //Asset Actions
    var inFrame=<%=(UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME)) && (Boolean)request.getSession().getAttribute(WebKeys.IN_FRAME))?true:false%>;
    //Host Actions
    function editHost(inode, referer) {
        editContentletEvent(inode, "HOST");
    }

    function setAsDefaultHost(objId,referer) {
        if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-set-this-host-as-the-default-host")) %>')) {
            HostAjax.makeDefault(objId, setAsDefaultHostCallback);
        }
    }

    function setAsDefaultHostCallback() {
        showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "set-as-default")) %>');
    }

    function deleteHost(objId, referer) {
        if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-the-selected-host-and-ALL-its-contents")) %>')) {
            var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_host" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + escape(referer);
            if(inFrame){
                window.location = loc;
            }else{
                top.location = loc;
            }
            actionLoading = true;
            return true;
        }
        return false;
    }

    function addHost(referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_host" /></portlet:actionURL>&referer=' + escape(referer);
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }


    //folder actions
    function addTopFolder(parentHostId, referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>&phostId=' + parentHostId + '&referer=' + escape(referer);
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function addFolder(parentId, referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>&pfolderId=' + parentId + '&referer=' + escape(referer);
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function editFolder(objId, referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>&inode=' + objId + '&referer=' + escape(referer);
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function deleteFolder(objId, referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-folder-this-action-cant-be-undone")) %>')) {
            actionLoading = true;
            cleanContentSide();
            Element.show('loadingContentListing');
            var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /><portlet:param name="cmd" value="<%=Constants.DELETE%>" /></portlet:actionURL>&inode=' + objId + '&referer=' + escape(referer);
            if(inFrame){
                window.location = loc;
            }else{
                top.location = loc;
            }
        }
    }

    function publishFolder (objId, referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/publish_folder" /></portlet:actionURL>&cmd=prepublish&inode=' + objId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function copyFolder (objId, parentId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + objId + '&parent=' + parentId + '&referer=' + referer;
        BrowserAjax.copyFolder(objId, parentId, copyFolderCallback);
        setTimeout('reloadContent()',500);
    }

    function copyFolderCallback (response) {
        if(response == '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder-copied")) %>'){
            BrowserAjax.getTree(null, initializeTree);
            showDotCMSSystemMessage(response);
        } else {
            reloadContent ();
            showDotCMSErrorMessage(response);
        }
    }

    function moveFolder (objId, parentId, referer) {
        BrowserAjax.moveFolder(objId, parentId, moveFolderCallback);
        setTimeout('reloadContent()',500);
    }

    function moveFolderCallback (response) {
        if(response == '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder-moved")) %>'){
            BrowserAjax.getTree(null, initializeTree);
            showDotCMSSystemMessage(response);
        } else {
            reloadContent ();
            showDotCMSErrorMessage(response);
        }
    }

    function pasteToFolder (objId, referer) {
        if (isInodeSet(markedForCut)) {
            var asset = inodes[markedForCut];
            if (asset.type == 'folder') {
                moveFolder (markedForCut, objId, referer);
            }
            if (asset.type == 'file_asset') {
                moveFile (markedForCut, objId, referer);
            }
            if (asset.type == 'links') {
                moveLink (markedForCut, objId, referer);
            }
            if (asset.type == 'htmlpage') {
                moveHTMLPage (markedForCut, objId, referer);
            }
        } else if (isInodeSet(markedForCopy)) {
            var asset = inodes[markedForCopy];
            if (asset.type == 'folder') {
                copyFolder (markedForCopy, objId, referer);
            }
            if (asset.type == 'file_asset') {
                copyFile (markedForCopy, objId, referer);
            }
            if (asset.type == 'links') {
                copyLink (markedForCopy, objId, referer);
            }
            if (asset.type == 'htmlpage') {
                copyHTMLPage (markedForCopy, objId, referer);
            }
        }
    }

    //HTML Page actions
    function addHTMLPage(parentId, referer) {
        //top.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=&referer=' + referer;
        refererVar=referer;
        BrowserAjax.getFolderMap(parentId,function(map) {
            showPageAssetPopUp(map);
        });
    }

    function previewHTMLPage(url, content) {
        // We can't new CustomEvent becuase it's not supported by IE11
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-page",
            data: {
                hostId: myHostId,
                url: url,
                languageId: content.languageId
            }
        });
        document.dispatchEvent(customEvent);
    }

    function editHTMLPage (objId, referer) {
        var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=edit&inode=' + objId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    var publishHTMLPageInode;
    var publishHTMLPageReferer;
    function publishHTMLPageExceptionHandler(msg) {
        if (msg == '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder-moved")) %>'){
            var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/publish_htmlpages" /><portlet:param name="cmd" value="prepublish" /></portlet:actionURL>&publishInode=' + publishHTMLPageInode + '&referer=' + publishHTMLPageReferer;
            if(inFrame){
                window.location = loc;
            }else{
                top.location = loc;
            }
        }else{
            alert(msg);
        }
    }

    function publishHTMLPage (objId, referer) {
        publishHTMLPageInode = objId;
        publishHTMLPageReferer = referer;
        dwr.engine.setErrorHandler(publishHTMLPageExceptionHandler);
        BrowserAjax.publishAsset(objId, publishHTMLPageCallback);
    }

    function publishHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish-failed-check-you-have-the-required-permission")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-published")) %>');
        }
    }

    /**
     * Displays the Push Publish modal window.
     *
     * objId - The Identifier or inode of the object to push.
     * referrer - The referrer page.
     * isArchived - If true, allow users to ONLY do a "Push" or "Remove", NOT a "Push & Remove".
     */
    function remotePublish (objId, referrer, isArchived) {
        if (isArchived != undefined && isArchived != null) {
            pushHandler.showDialog(objId, false, isArchived);
        } else {
            pushHandler.showDialog(objId);
        }
    }

    function addToBundle (objId) {

        pushHandler.showAddToBundleDialog(objId, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');

    }

    function unpublishHTMLPage (objId, referer) {
        BrowserAjax.unPublishAsset(objId, unpublishHTMLPageCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
    }

    function unpublishHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish-failed-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-unpublished")) %>');
        }
    }

    function archiveHTMLPage (objId, referer) {
        var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-asset")) %>';
        var inode = inodes[objId];
        if (inode.live)
            message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-unpublish-and-archive-this-asset")) %>';
        if (confirm(message)) {
            //self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer;
            BrowserAjax.archiveAsset(objId, archiveHTMLPageCallback);
            return true;
        }
        return false;
    }

    function archiveHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-archived")) %>');
        }
    }


    function unarchiveHTMLPage (objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unArchiveAsset(objId, unArchiveHTMLPageCallback);
    }

    function unArchiveHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-un-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-un-archived")) %>');
        }
    }


    function viewHTMLPageStatistics (objId, referer) {
        var userId = '<%=user.getUserId()%>';
        var loc='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpageviews/view_htmlpage_views" /></portlet:renderURL>&htmlpage=' + objId  + '&userId=' + userId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function unlockHTMLPage (objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unlockAsset(objId, unlockHTMLPageCallback);
    }

    function unlockHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-unlock-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-unlocked")) %>');
        }
    }

    function migratePage(objId, referer) {
        var loc ='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/director/direct" /><portlet:param name="cmd" value="migrate" /></portlet:actionURL>&htmlPage=' + objId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function copyHTMLPage (objId, parentId, referer) {
        BrowserAjax.copyHTMLPage(objId, parentId, function (response) {
            // copyHTMLPageCallback
            if (response.status == "success") {
                setTimeout("reloadContent()", 1000);
                showDotCMSSystemMessage(response.message);
                return;
            }

            // An error happened
            reloadContent();
            showDotCMSErrorMessage(response.message);
        });
    }

    function moveHTMLPage (objId, parentId, referer) {
        BrowserAjax.moveHTMLPage(objId, parentId, moveHTMLPageCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="move" /></portlet:actionURL>&inode=' + objId + '&parent=' + parentId + '&referer=' + referer;

    }

    function moveHTMLPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-move-another-page-with-the-same-name-already-exists-in-the-destination")) %>');
        } else {
            setTimeout('reloadContent()',1000);
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Page-moved")) %>');
        }
    }


    //File actions
    var refererVar;
    function addFile(parentId,referer,isMultiple) {
        refererVar = referer;
        var callbackArg1 = {isMultiple:isMultiple};
        var callMetaData = {
            callback:getFolderMapCallback,
            arg:callbackArg1
        };
        BrowserAjax.getFolderMap(parentId,callMetaData);
    }

    function getFolderMapCallback(data,arg){
        var folderMap = data;
        var callbackArg2 = {isMultiple:arg.isMultiple, folderMap:folderMap};
        var callMetaData = {
            callback:getStructureDetailsCallback,
            arg:callbackArg2
        };
        StructureAjax.getStructureDetails(data.defaultFileType, callMetaData);
    }

    function getStructureDetailsCallback(data, arg){
        showFileAssetPopUp(arg.folderMap, data, arg.isMultiple);
    }

    function showFileAssetPopUp(folderMap, fileAssetTypeMap, isMultiple){
        hidePopUp('context_menu_popup_'+folderMap.inode);
        var faDialog = dijit.byId("addFileAssetDialog");
        if (faDialog) {
            faDialog.destroyRecursive(true);
        }
        var fileAssetDialog = new dijit.Dialog({
            id   : "addFileAssetDialog",
            title: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "addfile.dialog")) %>",
        });
        var dialogHtml = getFileAssetDialogHtml(folderMap, fileAssetTypeMap);

        if(!fileAssetTypeMap.inode) {
            fileAssetTypeMap.inode = 0;
        }

        dialogHtml = dojo.string.substitute(dialogHtml, { stInode:fileAssetTypeMap.inode, folderInode:folderMap.inode, isMultiple:isMultiple});

        fileAssetDialog.attr("content", dialogHtml);
        fileAssetDialog.show();
    }

    function getFileAssetDialogHtml(folderMap, fileAssetTypeMap){

        return "<div>"+
            "<div style='margin:8px 5px;'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "select.the.type.of.fileasset.you.wish.to.upload")) %>:</div>" +
            "<span dojoType='dotcms.dojo.data.StructureReadStore' jsId='fileAssetStructureStore' dojoId='fileAssetStructureStoreDojo' structureType='<%=Structure.STRUCTURE_TYPE_FILEASSET %>'></span>"+
            "<select id='defaultFileType' name='defaultFileType' dojoType='dijit.form.FilteringSelect' style='width:200px;' store='fileAssetStructureStore' searchDelay='300' pageSize='15' autoComplete='false' ignoreCase='true' labelAttr='name' searchAttr='name'  value='${stInode}' invalidMessage='<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>'></select>"+
            "<button dojoType='dijit.form.Button' iconClass='addIcon' id='selectedFileAssetButton' onclick='getSelectedfileAsset(\"${folderInode}\",${isMultiple});'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "modes.Select")) %></button>" +
            "</div>";
    }

    function showPageAssetPopUp(folderMap){
        hidePopUp('context_menu_popup_'+folderMap.inode);
        var faDialog = dijit.byId("addPageAssetDialog");
        if (faDialog) {
            faDialog.destroyRecursive(true);
        }
        var pageAssetDialog = new dijit.Dialog({
            id   : "addPageAssetDialog",
            title: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "addpage.dialog")) %>",
            style: "height:160px; overflow: auto"
        });
        var dialogHtml = getHTMLPageAssetDialogHtml();

        <%
             String defaultPageSt = "0";
             Structure defaultHTMLPageST = CacheLocator.getContentTypeCache().getStructureByInode(APILocator.getHTMLPageAssetAPI().getHostDefaultPageType(myHost));
             if(APILocator.getPermissionAPI().doesUserHavePermission(defaultHTMLPageST, PermissionAPI.PERMISSION_READ, user, false)) {
               defaultPageSt = defaultHTMLPageST.getInode();
             }
        %>

        dialogHtml = dojo.string.substitute(dialogHtml, { stInode:'<%=defaultPageSt%>', folderInode:folderMap.inode });
        pageAssetDialog.attr("content", dialogHtml);
        pageAssetDialog.show();
    }

    function getHTMLPageAssetDialogHtml(){

        return "<div>"+
            "<div style='margin:8px 5px;'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "select.the.type.of.htmlpage.you.wish.to.upload")) %>:</div>" +
            "<span dojoType='dotcms.dojo.data.StructureReadStore' jsId='pageAssetStructureStore' dojoId='pageAssetStructureStoreDojo' structureType='<%=Structure.STRUCTURE_TYPE_HTMLPAGE %>'></span>"+
            "<div class='inline-form'>"+
            "<select id='defaultPageType' name='defaultPageType' dojoType='dijit.form.FilteringSelect' style='width:200px;' store='pageAssetStructureStore' searchDelay='300' pageSize='15' autoComplete='false' ignoreCase='true' labelAttr='name' searchAttr='name'  value='${stInode}' invalidMessage='<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>'></select>"+
            "<button dojoType='dijit.form.Button' iconClass='addIcon' id='selectedPageAssetButton' onclick='getSelectedpageAsset(\"${folderInode}\");'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "modes.Select")) %></button>" +
            "</div>"+
            "</div>";
    }

    function createContentlet(url, contentType) {
        url = url + "&lang=" + selectedLang;
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "create-contentlet",
            data: {
                url,
                contentType
            }
        });
        document.dispatchEvent(customEvent);
        var dialog = dijit.byId("addPageAssetDialog") || dijit.byId("addFileAssetDialog");

        if (dialog) {
            dialog.hide();
        }
    }

    function getSelectedpageAsset(folderInode) {
        var selected = dijit.byId('defaultPageType');

        if(!selected){
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-a-valid-htmlpage-asset-type")) %>');
        }

        var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="new" /></portlet:actionURL>&selectedStructure=' + selected +'&folder='+folderInode+'&referer=' + escape(refererVar);
        createContentlet(loc, selected.item.velocityVarName);
    }


    function getSelectedfileAsset(folderInode, isMultiple){
        var selected = dijit.byId('defaultFileType');

        if(!selected || (selected + '').trim().length == 0){
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-a-valid-file-asset-type")) %>');
            return;
        }

        if(!isMultiple){
            var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="new" /></portlet:actionURL>&selectedStructure=' + selected +'&folder='+folderInode+'&referer=' + escape(refererVar);
            createContentlet(loc, selected.item.velocityVarName);
        } else {
            addMultipleFile(folderInode, selected, escape(refererVar));
        }
    }


    function createFileAsset(stInode, folderInode){
        var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="new" /></portlet:actionURL>&selectedStructure=' + stInode +'&folder='+folderInode+'&referer=' + escape(refererVar);
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function addMultipleFile(parentId, selectedStructure, referer) {
        var url = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/upload_multiple" /></portlet:actionURL>&cmd=edit&<%=WebKeys.IN_FRAME%>=true&<%=WebKeys.HIDE_SUBNAV%>=true&parent=' + parentId + '&selectedStructure=' + selectedStructure +'&inode=\'\'&referer=' + referer;
        if(dijit.byId('addFileDialog')){
            var uploadDlg = dijit.byId('addFileDialog');
            uploadDlg.set('href',url);
            dojo.connect(uploadDlg, "onLoad", null, function(e) {
                loadWorkflowActions(selectedStructure);
            });
            uploadDlg.show();

            hidePopUp('context_menu_popup_'+parentId);
        }
    }

    function removeAddlStyleRef(){//DOTCMS-6856
        dijit.byId('addFileDialog').containerNode.getElementsByTagName("style")[0].remove(dijit.byId('addFileDialog').containerNode.getElementsByTagName("style")[0].childNodes[0]);
    }

    function editFile (objId, referer) {
        var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function editFileAsset (contInode, structureInode){
        editContentletEvent(contInode, inodes[contInode].contentType);
        hidePopUp('context_menu_popup_'+contInode);

    }

    function editHTMLPageAsset (contInode, structureInode) {
        editContentletEvent(contInode, inodes[contInode].contentType);
        hidePopUp('context_menu_popup_'+contInode);
    }

    function previewHTMLPageAsset(id,referer) {

    }

    function editContentletEvent(contInode, contentType) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-contentlet",
            data: {
                inode: contInode,
                contentType
            }
        });
        document.dispatchEvent(customEvent);
    }

    function publishFile (objId, referer) {
        BrowserAjax.publishAsset(objId, publishFileCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/publish_files" /></portlet:actionURL>&publishInode=' + objId + '&referer=' + referer;
    }

    function publishFileCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish-failed-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-published")) %>');
        }
    }

    function unpublishFile (objId, referer) {
        BrowserAjax.unPublishAsset(objId, unPublishFileCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
    }

    function unPublishFileCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish-failed-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-unpublished")) %>');
        }
    }

    function archiveFile (objId, referer) {
        var inode = inodes[objId];
        var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-asset")) %>';
        if (inode.live)
            message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-unpublish-and-archive-this-asset")) %>';
        if (confirm(message)) {
            BrowserAjax.archiveAsset(objId, archiveFileCallback);
            //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="delete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
            return true;
        }
        return false;
    }

    function archiveFileCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= LanguageUtil.get(pageContext, "File-archived") %>');
        }
    }

    function unarchiveFile (objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unArchiveAsset(objId, unarchiveFileCallback);
    }

    function unarchiveFileCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-un-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-un-archived")) %>');
        }
    }

    function viewFile(objId,fileExt) {
        window.open('/contentAsset/raw-data/' + objId + '/fileAsset/byInode/1' + "?random=<%=UtilMethods.getRandomNumber(100000)%>",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
    }

    function copyFile (objId, parentId, referer) {
        BrowserAjax.copyFile(objId, parentId, function(response) {
            // copyFileCallback
            if (response.status == "success") {
                setTimeout("reloadContent()", 1000);
                showDotCMSSystemMessage(response.message);
                return;
            }

            // An error happened
            reloadContent();
            showDotCMSErrorMessage(response.message);
        });
    }

    function moveFile (objId, parentId, referer) {
        BrowserAjax.moveFile(objId, parentId, moveFileCallback);

    }

    function moveFileCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-failed-to-move-another-file-with-the-same-name-already-exist-in-the-destination-folder")) %>');
        } else {
            setTimeout('reloadContent()',1000);
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-moved")) %>');
        }
    }

    function unlockFile(objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unlockAsset(objId, unlockFilePageCallback);
    }

    function unlockFilePageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-unlock-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-unlock")) %>');
        }
    }

    //### DELETE METHODS ###
    //File
    function deleteFile(objId, referer) {
        if(confirm("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-file-this-cannot-be-undone")) %>"))
        {
            BrowserAjax.deleteAsset(objId, deleteFilePageCallback);
        }
    }

    function deleteFilePageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-delete-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "File-deleted")) %>');
        }
    }

    //HTMLPage
    function deleteHTMLPage(objId, referer)
    {
        if(confirm("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-html-page-this-cannot-be-undone")) %>"))
        {
            BrowserAjax.validateRelatedContentType(objId, validateRelatedContentTypeCallback);
        }
    }

    function validateRelatedContentTypeCallback(response){
        if (response.message != ""){
            if(confirm(response.message)){
                BrowserAjax.deleteHTMLPageAsset(response.inode, deleteHTMLPageCallback);
            }
        }else{
            BrowserAjax.deleteHTMLPageAsset(response.inode, deleteHTMLPageCallback);
        }
    }

    function deleteHTMLPageCallback(response)
    {
        // deleteHTMLPageCallback
        if (response.status == "success") {
            setTimeout("reloadContent()", 1000);
            showDotCMSSystemMessage(response.message);
            return;
        }

        // An error happened
        reloadContent();
        showDotCMSErrorMessage(response.message);
    }

    function deleteHTMLPagePreCheck(objId, referer)
    {
        var dataFromBrowser = {"objId":objId,"referer":referer};

        var callMetaData = {
            callback:deleteHTMLPagePreCheckCallback,
            arg: dataFromBrowser
        };

        BrowserAjax.deleteHTMLPagePreCheck(objId, callMetaData);
    }

    function deleteHTMLPagePreCheckCallback (response, dataFromBrowser)
    {
        if (!response) {
            if(confirm("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.with.other.live.versions.delete")) %>"))
            {
                BrowserAjax.deleteHTMLPageAsset(dataFromBrowser.objId, deleteHTMLPageCallback);
            }
        } else {
            deleteHTMLPage(dataFromBrowser.objId, dataFromBrowser.referer);
        }
    }

    //Link
    function deleteLink(objId, referer) {
        if(confirm("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-link-this-cannot-be-undone")) %>"))
        {
            BrowserAjax.deleteAsset(objId, deleteLinkCallback);
        }
    }

    function deleteLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-delete-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-deleted")) %>');
        }
    }
    //### END DELETE METHODS ###


    //link action
    function addLink(parentId,referer) {
        var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=\'\'&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function editLink (objId, referer) {
        var loc='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        if(inFrame){
            window.location = loc;
        }else{
            top.location = loc;
        }
    }

    function publishLink (objId, referer) {
        BrowserAjax.publishAsset(objId, publishFileCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/publish_links" /></portlet:actionURL>&publishInode=' + objId + '&referer=' + referer;
    }

    function publishLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish-failed-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-published")) %>');
        }
    }

    function unpublishLink (objId, referer) {
        BrowserAjax.unPublishAsset(objId, unPublishFileCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
    }

    function unpublishLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish-failed-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-published")) %>');
        }
    }

    function archiveLink(objId, referer) {
        var inode = inodes[objId];
        var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-asset")) %>';
        if (inode.live)
            message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-unpublish-and-archive-this-asset")) %>';
        if (confirm(message)) {
            //top.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer;
            BrowserAjax.archiveAsset(objId, archiveLinkCallback);
            return true;
        }
        return false;
    }

    function archiveLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-archived")) %>');
        }
    }

    function unarchiveLink(objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unArchiveAsset(objId, unArchiveLinkCallback);
    }

    function unArchiveLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-un-archive-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-unarchived")) %>');
        }
    }

    function unlockLink(objId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
        BrowserAjax.unlockAsset(objId, unlockLinkPageCallback);
    }

    function unlockLinkPageCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-unlock-check-you-have-the-required-permissions")) %>');
        } else {
            reloadContent ();
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-unlocked")) %>');
        }
    }

    function copyLink (objId, parentId, referer) {
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + objId + '&parent=' + parentId + '&referer=' + referer;
        BrowserAjax.copyLink(objId, parentId, copyLinkCallback);
        setTimeout('reloadContent()',500);
    }

    function copyLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-copy-check-you-have-the-required-permissions")) %>');
        } else {
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-copied")) %>');
        }
    }

    function moveLink (objId, parentId, referer) {
        BrowserAjax.moveLink(objId, parentId, moveLinkCallback);
        //top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="move" /></portlet:actionURL>&inode=' + objId + '&parent=' + parentId + '&referer=' + referer;
        setTimeout('reloadContent()',500);
    }

    function moveLinkCallback (response) {
        if (!response) {
            reloadContent ();
            showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-move-another-link-with-the-same-name-already-exists-in-the-destination")) %>');
        } else {
            showDotCMSSystemMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Link-moved")) %>');
        }
    }



    //--------------------------------------------------------------

    //Utility funcs

    function getInodeFromID (id) {
        // To return inode of type UUID
        if((id.length > 36)
            && (id.charAt(8) == '-')
            && (id.charAt(13) == '-')
            && (id.charAt(18) == '-')
            && (id.charAt(23) == '-')
            && (id.charAt(36) == '-')){
            return  id.substring(0, 36);
        }else{// To return inode of type long
            return id.substring(0, id.indexOf('-'));
        }
    }

    Array.prototype.remove = function (element) {
        var result = false;
        var array = [];
        for (var i = 0; i < this.length; i++) {
            if (this[i] == element) {
                result = true;
            } else {
                array.push(this[i]);
            }
        }
        this.clear();
        for (var i = 0; i < array.length; i++) {
            this.push(array[i]);
        }
        array = null;
        return result;
    };

    Array.prototype.add = function (element) {
        this[this.length] = element;
    };

    Array.prototype.contains = function (value) {
        for (var i = 0; i < this.length; i++) {
            if (this[i] == value)
                return true;
        }
        return false;
    };

    function removeClass (id, className) {
        var actualClassName = "";
        if($(id))
            actualClassName = $(id).className;
        if (actualClassName.indexOf(className) > -1) {
            var newClassName = "";
            if (actualClassName.indexOf(className) > 0)
                newClassName += actualClassName.substring(0,actualClassName.indexOf(className));
            if (actualClassName.indexOf(className) + className.length < actualClassName.length)
                newClassName += actualClassName.substring(actualClassName.indexOf(className) + className.length, actualClassName.length);
            $(id).className = newClassName;
        }
    }

    function addClass (id, className) {
        if (!hasClass (id, className)) {
            var actualClassName = "";
            if($(id)){
                actualClassName = $(id).className;
                $(id).className = actualClassName + ' ' + className;
            }
        }
    }

    function hasClass (id, className) {
        var actualClassName = "";
        if($(id))
            actualClassName = $(id).className;
        if (actualClassName.indexOf(className) > -1)
            return true;
        return false;
    }

    document.oncontextmenu=nothing;

    if (document.all) {
        document.onmousedown=nothing;
    } else if (document.getElementById) {
        document.onmouseup=nothing;
    }

    function hasReadPermissions (permissions) {
        return permissions.contains("<%= PermissionAPI.PERMISSION_READ  %>");
    }

    function hasWritePermissions (permissions) {
        return permissions.contains("<%= PermissionAPI.PERMISSION_WRITE  %>");
    }

    function hasPublishPermissions (permissions) {
        return permissions.contains("<%= PermissionAPI.PERMISSION_PUBLISH  %>");
    }

    function hasAddChildrenPermissions (permissions) {
        return permissions.contains("<%= PermissionAPI.PERMISSION_CAN_ADD_CHILDREN %>");
    }

    function showMessage (msg) {
        if($('dotCMSMessages') != null)
            Element.hide('dotCMSMessages');
        Element.update('messageBox', msg);
        Element.hide('errorsTable');
        Element.show('messagesTable');
    }

    function showError (msg) {
        if($('dotCMSMessages') != null)
            Element.hide('dotCMSMessages');
        Element.update('errorBox', msg);
        Element.hide('messagesTable');
        Element.show('errorsTable');
    }

    //Debbugging
    var debugMessagesEnabled = false;
    function showDebugMessage (msg) {
        if (debugMessagesEnabled) {
            Element.show($('statusDiv'));
            var date = new Date();
            var dateStr = date.getHours() + ':' + date.getMinutes() + ':' + date.getSeconds();
            $('statusDiv').innerHTML = dateStr + ' - ' + msg + '<br>' + $('statusDiv').innerHTML;
        }
    }

    function angularWorkflowEventCallback () {
        BrowserAjax.getTree(null, initializeTree);
        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");
    }

    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');
</script>
