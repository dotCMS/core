<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>


<script type="text/javascript">
function addLanguage(){
    window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /><portlet:param name="id" value="" /></portlet:actionURL>';
}
function editDefault(){
    window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /></portlet:actionURL>&cmd=edit';
}

function remotePublish(objId) {
    pushHandler.showDialog(objId);
}

function addToBundle (objId) {
    pushHandler.showAddToBundleDialog(objId, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
}

//==========================================
// Language Popup
//==========================================
function showLanguagePopUp(languageId, hrefVariables, cmsAdminUser, origReferer, e) {
    var objId = String(languageId);
    var referer = encodeURIComponent(origReferer);

    if($('context_menu_popup_'+objId) == null) {
        var divHTML = '<div id="context_menu_popup_' + objId + '" class="contextPopupMenuBox"></div>';
        new Insertion.Bottom ('popups', divHTML);
    }

    var div = $('context_menu_popup_'+objId);
    var strHTML = '';

    // Edit variables
    strHTML += '<a class="contextPopupMenu" href="';
    strHTML += hrefVariables["editVariablesHref"];
    strHTML += '"><span class="formNewIcon"></span><%= LanguageUtil.get(pageContext, "Edit-Variables") %></a>';

    // Edit language
    strHTML += '<a class="contextPopupMenu" href="';
    strHTML += hrefVariables["editLanguageHref"];
    strHTML += '"><span class="editIcon"></span><%= LanguageUtil.get(pageContext, "Edit-Language") %></a>';

    // Push publish & add to bundle
    if (enterprise) {
        if (sendingEndpoints) {
            var remotePublishHref = 'javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_' + objId + '\');';
            
            strHTML += '<a class="contextPopupMenu" href="'; strHTML += remotePublishHref; strHTML += '">';
            remotePublishHref
                strHTML += '<span class="sServerIcon"></span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
            strHTML += '</a>';
        }

        var bundleHref = 'javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_' + objId + '\');';
        
        strHTML += '<a class="contextPopupMenu" href="'; strHTML += bundleHref; strHTML += '">';
            strHTML += '<span class="bundleIcon"></span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
        strHTML += '</a>';
    }

    // Close
    var closeHref = 'javascript:hidePopUp(\'context_menu_popup_' + objId + '\');';
    
    strHTML += '<div class="pop_divider"></div>';
    strHTML += '<a href="'; strHTML += closeHref; strHTML += '" class="contextPopupMenu">';
        strHTML += '<span class="closeIcon"></span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
    strHTML += '</a>';

    Element.update(div, strHTML);

    showPopUp('context_menu_popup_'+objId, e);
}

//==========================================
// Popups Functions
//==========================================
var currentMenuId = "";
var currentChildMenuId = "";
document.oncontextmenu = nothing;

function showPopUp(id, e) {

    var mousePosX = Event.pointerX(e);
    var mousePosY = Event.pointerY(e);

    hidePopUp(currentMenuId);

    currentMenuId = id;

    var popup = $(id);

    var windowHeight = top.document.body.clientHeight;

    var popupHeight = Element.getHeight(popup);

    var noPx = document.childNodes ? 'px' : 0;
    var myReference = popup;
    if( myReference.style ) {
        myReference = popup.style;
    }

    myReference.left = ( mousePosX - 10 ) + noPx;

    if((mousePosY + popupHeight) >= windowHeight && windowHeight > popupHeight)
        myReference.top = ( mousePosY - popupHeight ) + noPx;
    else
        myReference.top = ( mousePosY ) + noPx;

    Element.show (id);
}

function hidePopUp(id) {
    if ($(id) != null) {
        if($(currentChildMenuId) != null && id == currentMenuId) {
            Element.hide (currentChildMenuId);
            currentChildMenuId = "";
        }
        Element.hide (id);
        currentMenuId = "";
    }
}
</script>