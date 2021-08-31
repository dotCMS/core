<%@ include file="/html/portlet/ext/folders/init.jsp" %>

<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.business.web.UserWebAPI" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
	String referer = (request.getParameter("referer") != null ) ? request.getParameter("referer") : "";
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
	User backUser = userAPI.getLoggedInUser(request);

	Folder parentFolder = (Folder) request.getAttribute(com.dotmarketing.util.WebKeys.MENU_MAIN_FOLDER);						
	boolean showSaveButton = false;
							
	List<Object> l = (List)request.getAttribute("htmlTreeList");

	String htmlTree = "";
	if(l.get(0) != null){
		htmlTree = (String)l.get(0);
		showSaveButton = (Boolean)request.getAttribute("showSaveButton");
	}
	else{
		showSaveButton = false;
	}

%>
<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>
<script language="Javascript">
function submitfm() {
	form = document.getElementById('fm');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/folders/order_menu" /></portlet:actionURL>';
	submitForm(form);
}
function savechanges(btn) {
    btn.setDisabled(true);
	form = document.getElementById('fm');
	form.cmd.value = "generatemenu";
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/folders/order_menu" /></portlet:actionURL>';
	document.getElementById('reorder_result').value = serialize();
    submitAjaxForm({
        form: form,
        success: function() {
            triggerCustomEvent("save-menu-order");
        },
        error: function() {
            triggerCustomEvent("error-saving-menu-order");
        }
    });
}
function moveMenuItemDown(menuItem,parentFolder){
 	document.getElementById('item').value=menuItem;
 	document.getElementById('folderParent').value=parentFolder;
 	document.getElementById('move').value="down";
	submitfm();
}

function moveMenuItemUp(menuItem,parentFolder){
 	document.getElementById('item').value=menuItem;
 	document.getElementById('folderParent').value=parentFolder;
 	document.getElementById('move').value="up";
	submitfm();
}
function goBack() 
{
	<% if (!referer.equals("")) { %>
		window.location.href = "<%=java.net.URLDecoder.decode(referer,"UTF-8")%>";
	<% } else { %>
		window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/folders/order_menu" /></portlet:actionURL>';
	<% } %>
}
</script>

<%
	String path = "/";
	String openLevelOne = request.getParameter("path");
	String pagePath = request.getParameter("pagePath");
	String openAll = request.getParameter("openAll");

	String openLevelTwo = "";
	String openLevelThree = "";
	
	//String openLevelOne = "/";
	int idx1 = pagePath.indexOf("/");
	int idx2 = 0;
	if (idx1 >= 0) {
		idx2 = pagePath.indexOf("/",idx1+1);
		if (idx2 >= 0) {
			openLevelOne = pagePath.substring(idx1+1,idx2);
		}
	}
	
	if ((idx1 = pagePath.indexOf("/",idx2+1)) != -1 ) {
		openLevelTwo = pagePath.substring(idx2+1, idx1);
	}
	
	idx2 = idx1;
	if ((idx1 = pagePath.indexOf("/",idx2+1)) != -1 ) {
		openLevelThree = pagePath.substring(idx2+1, idx1);
	}
%>
<!--
Don't delete this is for debugging purposes 
pagePath=<%=pagePath%>
<BR>openLevelOne=<%=openLevelOne%>
<BR>openLevelTwo=<%=openLevelTwo%>
<BR>openLevelThree=<%=openLevelThree%>
-->
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="Order Menu Items" />

	<form id="fm" method="post" onSubmit="emitOkEvent()">
		<input type="hidden" name="referer" value="<%=referer%>">
		<input type="hidden" name="cmd" value="reorder">
		<input type="hidden" name="item" id="item" value="">
		<input type="hidden" name="hostId" id="hostId" value="<%=request.getParameter("hostId")%>">
		<input type="hidden" name="folderParent" id="folderParent" value="">
		<input type="hidden" name="move" id="move" value="">
		<input type="hidden" name="path" id="path" value="<%=request.getParameter("path")%>">
		<input type="hidden" name="openAll" id="openAll" value="<%=request.getParameter("openAll")%>">
		<input type="hidden" name="pagePath" id="pagePath" value="<%=request.getParameter("pagePath")%>">	
		<input type="hidden" name="startLevel" value="<%=((Integer)request.getAttribute("startLevel")).intValue()%>">
		<input type="hidden" name="depth" value="<%=((Integer)request.getAttribute("depth")).intValue()%>">
		<input type="hidden" name="reorder_result" id="reorder_result">

        <style>
            .reorderMenuContent .reorderMenuContentWrapper {
                left: -50%;
                position: relative;
            }
            td li {
                font-size:12px;
                border-top:3px solid white;
                border-left:3px solid white;
                background:#eee;
                line-height:30px;
                padding-left:10px;
                font-weight: bold;
            }
            td li li {
                margin-left:20px;
                font-weight: normal;
            }
        </style>
        <table class="listingTable">
            <tr>
                <th><%= LanguageUtil.get(pageContext, "Reorder-Menu-Items") %></th>
            </tr>
            <tr>
                <td>
                    <div class="reorderMenuContent" style="position:absolute;left:50%;padding:10px;">
                        <div class="reorderMenuContentWrapper">
                            <%= htmlTree %>
                            <p align="center" style="margin-top: 2em;">
                                <%= LanguageUtil.get(pageContext, "Drag-and-drop-the-items-to-the-desired-position-and-then-save-your-changes") %>
                            </p>
    
                            <div class="buttonRow">
                                <% if(showSaveButton){%>
                                    <button dojoType="dijit.form.Button" onClick="savechanges(this)" iconClass="saveIcon">
                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-changes")) %>
                                    </button>       
                                <%} %>
                                <button dojoType="dijit.form.Button" onClick="triggerCustomEvent('cancel-save-menu-order')" iconClass="cancelIcon">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                                </button>
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        </table>
	</form>
</liferay:box>