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

let newTargetDefaultLanguage = '';
let transferAssetsToTheNewLanguage = true;

function makeDefaultLanguageProxy(langId){

    hidePopUp('context_menu_popup_' + langId);
    dijit.byId("languageTransferAssetsDialog").show();
    newTargetDefaultLanguage = langId;
}

function makeDefaultLanguage(){

      const transferAssets = transferAssetsToTheNewLanguage;
      const langId = newTargetDefaultLanguage;

      const data = {
          "fireTransferAssetsJob":transferAssets
      };

      const dataAsJson = dojo.toJson(data);

      var xhrArgs = {
          url : "/api/v2/languages/" + langId + "/_makedefault" ,
          postData: dataAsJson,
          handleAs: "json",
          headers : {
              'Accept' : 'application/json',
              'Content-Type' : 'application/json;charset=utf-8',
          },
          load : function(data){
              console.log(data);
              location.reload();
          },
          error : function(error) {
              console.error("Error attempting to make language [" + langId + "] the new default. ", error);

          }
      };

      dojo.xhrPut(xhrArgs);

}

function doTransferAssets(value){
    transferAssetsToTheNewLanguage = value;
}

function makeDefaultLanguageCancel(){
    dijit.byId("languageTransferAssetsDialog").hide();
}


//==========================================
// Language Popup
//==========================================
function showLanguagePopUp(languageId, hrefVariables, cmsAdminUser, origReferer, e) {
    var objId = String(languageId);
    var referer = encodeURIComponent(origReferer);

    if ($('context_menu_popup_' + objId) == null) {
        var divHTML = '<div id="context_menu_popup_' + objId + '" class="context-menu"></div>';
        new Insertion.Bottom('popups', divHTML);
    }

    var div = $('context_menu_popup_' + objId);
    var strHTML = '';

    // Edit variables
    strHTML += '<a class="context-menu__item" href="';
    strHTML += hrefVariables["editVariablesHref"];
    strHTML += '"><%= LanguageUtil.get(pageContext, "Edit-Variables") %></a>';

    // Edit language
    strHTML += '<a class="context-menu__item" href="';
    strHTML += hrefVariables["editLanguageHref"];
    strHTML += '"><%= LanguageUtil.get(pageContext, "Edit-Language") %></a>';

    const isAdminUser = <%= APILocator.getUserAPI().isCMSAdmin(user)%>;

    if (isAdminUser) {
       const makeDefaultLanguageId = hrefVariables["makeDefaultLanguageHref"];
       if(makeDefaultLanguageId){
         // Make Default language
         strHTML += '<a class="context-menu__item" href="#"';
         strHTML += ' onClick=javascript:makeDefaultLanguageProxy('+makeDefaultLanguageId+') ';
         strHTML += '><%= LanguageUtil.get(pageContext, "Make-Default") %></a>';
       }
    }

    // Push publish & add to bundle
    if (enterprise) {
        if (sendingEndpoints) {
            var remotePublishHref = 'javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_' + objId + '\');';
            
            strHTML += '<a class="context-menu__item" href="'; strHTML += remotePublishHref; strHTML += '">';
            remotePublishHref
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
            strHTML += '</a>';
        }

        var bundleHref = 'javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_' + objId + '\');';
        
        strHTML += '<a class="context-menu__item" href="'; strHTML += bundleHref; strHTML += '">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
        strHTML += '</a>';
    }

    // Close
    var closeHref = 'javascript:hidePopUp(\'context_menu_popup_' + objId + '\');';

    strHTML += '<div class="pop_divider"></div>';
    strHTML += '<a href="'; strHTML += closeHref; strHTML += '" class="context-menu__item">';
        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
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


<div id="languageTransferAssetsDialog" dojoType="dijit.Dialog" style="display:none;width:500px;vertical-align: middle; " draggable="true" title="<%= LanguageUtil.get(pageContext, "default-lang-switch-prompt") %>">
    <span class="ui-confirmdialog-message" style="text-align:center">
         <%=LanguageUtil.get(pageContext, "default-lang-switch-warning") %>
    </span>
    <br>
    <div style="text-align:center">
        <input type="checkbox" id="transfer-assets" dojoType="dijit.form.CheckBox" name="transfer-assets" checked="checked"  data-dojo-props="onChange: doTransferAssets" >
        <label for="transfer-assets"><%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "default-lang-switch-transfer-assets")) %></label>
    </div>
    <br>
    <div>
        <table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">
            <tr>
                <td style="width:50%;text-align: right">
                    <button id="cancelButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: makeDefaultLanguageCancel">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                    </button>
                </td>
                <td style="width:50%;text-align: left">
                    <button id="okButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: makeDefaultLanguage">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "default-lang-switch")) %>
                    </button>
                </td>
            </tr>
        </table>
    </div>
</div>
