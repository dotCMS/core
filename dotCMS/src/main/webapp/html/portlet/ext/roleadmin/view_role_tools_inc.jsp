<%--
    Markup shared between the Dojo Roles Admin portlet and the
    Angular Beta iframe wrapper for the Tools tab. Extracted verbatim from
    `view_roles.jsp` (New Layout dialog + Custom Portlet dialog + Tool
    Groups grid area). Keeps the Dojo JS in `view_roles_js_inc.jsp`
    (loadRoleLayouts / createNewLayout / saveLayout / ...) working
    unchanged — those functions look up the ids defined here by name.
--%>

<%-- New Layout Dialog --%>
<div id="newLayouDialog" title="<%= LanguageUtil.get(pageContext, "edit-tab") %>" draggable="true" dojoType="dijit.Dialog" style="display: none;width:625px;">
    <form id="newLayoutForm" dojoType="dijit.form.Form">
        <ul id="addLayoutErrorMessagesList"></ul>
        <table style="width:80%" class="toolTable">
            <tr>
                <td style="width: 20%;text-align:right"><label for="layoutName"><%=LanguageUtil.get(pageContext, "Tool-Group")%>:</label></td>
                <td><input id="layoutName" type="text" placeholder="<%=LanguageUtil.get(pageContext, "Tool-Group")%>"
                    maxlength="255" required="true"
                    invalidMessage="Required."
                    dojoType="dijit.form.ValidationTextBox" /></td>
            </tr>
            <tr>
                <td style="text-align:right;vertical-align: top;padding-top:20px;"><label for="layoutDescription"><%=LanguageUtil.get(pageContext, "Icon")%>:</label>
                <td>
                    <input type="hidden" dojoType="dijit.form.TextBox" id="layoutDescription" />
                    <dot-material-icon-picker size="13px"></dot-material-icon-picker>
                </td>
            </tr>
            <tr>
                <td style="text-align:right"><label for="layoutOrder"><%=LanguageUtil.get(pageContext, "order")%>:</label></td>
                <td><input id="layoutOrder" type="text" value="0"
                    dojoType="dijit.form.ValidationTextBox" /></td>
            </tr>
            <tr>
                <td style="text-align:right"><label for="portletList"><%=LanguageUtil.get(pageContext, "Tools")%>:</label></td>
                <td style="white-space: nowrap;">
                    <select id="portletList"></select>
                    <button dojoType="dijit.form.Button" onclick="addPortletToLayoutList()" type="button"><%=LanguageUtil.get(pageContext, "add")%></button>
                </td>
            </tr>
        </table>

        <div id="portletsListWrapper" class="view-roles__portlets-list" style="height:300px;width:95%;margin:0 auto"></div>

        <div class="inputCaption" style="text-align:right">* <%= LanguageUtil.get(pageContext, "drag-a-tool-to-order-it") %></div>

        <div class="buttonRow">
            <div style="float: left; padding-left: 20px;">
                <span id="deleteLayoutButtonWrapper">
                    <button dojoType="dijit.form.Button" type="button" onClick="deleteLayout()" style="disabled"
                        class="dijitButtonDanger" iconClass="deleteIcon">
                        <%=LanguageUtil.get(pageContext, "Delete")%>
                    </button>
                </span>
            </div>
            <div style="float: right; padding-right: 20px;">
                <button dojoType="dijit.form.Button" type="button" onClick="cancelEditLayout()" class="dijitButtonFlat">
                    <%=LanguageUtil.get(pageContext, "Cancel")%>
                </button>
                <button dojoType="dijit.form.Button" type="button" onClick="saveLayout()">
                    <%=LanguageUtil.get(pageContext, "Save")%>
                </button>
            </div>
            <div style="clear: both;"></div>
        </div>
    </form>
</div>
<%-- /New Layout Dialog --%>


<%-- Custom Portlet Dialog --%>
<div dojoType="dijit.Dialog" style="width:500px;" id="customPortletDialog" title="<%=LanguageUtil.get(pageContext, "custom.content.portlet.create")%>">
    <div dojoType="dijit.form.Form" style="width:500px;" id="customPortletForm" onsubmit="return false;">
        <table class="listingTable">
            <tr>
                <td style="white-space: nowrap;"><label for="customPortletName"><%=LanguageUtil.get(pageContext, "custom.content.portlet.portletName")%></label></td>
                <td><input dojoType="dijit.form.ValidationTextBox" type="text" required="true"
                    name="customPortletName" id="customPortletName" value="" onKeyUp="setPortletIdValue(this.getValue())"></td>
            </tr>
            <tr>
                <td style="white-space: nowrap;"><label for="customPortletId"><%=LanguageUtil.get(pageContext, "custom.content.portlet.portletId")%></label></td>
                <td><input dojoType="dijit.form.ValidationTextBox" type="text" required="true"
                    name="customPortletId" id="customPortletId" value="" onKeyUp="setPortletIdValue(this.getValue())" onBlur="cleanUpPortletId()"></td>
            </tr>
            <tr>
                <td style="white-space: nowrap;"><label for="customPortletBaseTypes"><%=LanguageUtil.get(pageContext, "custom.content.portlet.baseTypes")%></label></td>
                <td><input dojoType="dijit.form.TextBox" type="text"
                    name="customPortletBaseTypes" id="customPortletBaseTypes" value="">
                    <div class="hint-text"><%=LanguageUtil.get(pageContext, "custom.content.portlet.baseTypes.hint")%></div>
                </td>
            </tr>
            <tr>
                <td style="white-space: nowrap;"><label for="customPortletContentTypes">
                    <%=LanguageUtil.get(pageContext, "OR")%>&nbsp;
                    <%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.content-types-angular")%>
                </label></td>
                <td><input dojoType="dijit.form.TextBox" type="text"
                    name="customPortletContentTypes" id="customPortletContentTypes" value="">
                    <div class="hint-text"><%=LanguageUtil.get(pageContext, "custom.content.portlet.contentTypes.hint")%></div>
                </td>
            </tr>
            <tr>
                <td style="white-space: nowrap;"><label><%=LanguageUtil.get(pageContext, "custom.content.portlet.dataViewMode")%></label></td>
                <td>
                    <input type="radio" dojoType="dijit.form.RadioButton" name="dataViewMode" id="radioOne" value="list" checked />
                    <label for="radioOne"><%= LanguageUtil.get(pageContext, "custom.content.portlet.dataViewMode.list") %></label>&nbsp;
                    <input type="radio" dojoType="dijit.form.RadioButton" name="dataViewMode" id="radioTwo" value="card" />
                    <label for="radioTwo"><%= LanguageUtil.get(pageContext, "custom.content.portlet.dataViewMode.card") %></label>
                </td>
            </tr>
        </table>

        <div class="buttonRow">
            <button dojoType="dijit.form.Button" type="button" class="dijitButtonFlat"
                onClick="dijit.byId('customPortletDialog').hide();"><%=LanguageUtil.get(pageContext, "cancel")%></button>
            &nbsp;
            <button dojoType="dijit.form.Button" type="submit"
                onClick="createCustomContentType()"><%=LanguageUtil.get(pageContext, "ok")%></button>
        </div>
    </div>
</div>
<%-- /Custom Portlet Dialog --%>


<%-- Tools grid area (formerly the "CMS Tabs" tab inside the roles portlet). --%>
<div id="roleToolsWrapper">
    <div class="view-roles__heading">
        <h3 class="nameText" id="displayRoleName3"></h3>
        <div style="float:right">
            <button dojoType="dijit.form.Button" onclick="showCustomContentPortletDia()" type="button">
                <%=LanguageUtil.get(pageContext, "custom.content.portlet.create")%>
            </button>
            &nbsp;
            <button dojoType="dijit.form.Button" onclick="createNewLayout()" type="button">
                <%= LanguageUtil.get(pageContext, "create-custom-tab") %>
            </button>
        </div>
    </div>

    <div id="loadingRoleLayoutsWrapper"><img src="/html/images/icons/processing.gif"></div>

    <div id="roleLayoutsGridWrapper" style="overflow-y:auto;overflow-x:hidden;" class="view-roles__cms-tabs">
        <div id="roleLayoutsGrid"></div>
    </div>

    <div class="buttonRow">
        <button dojoType="dijit.form.Button" id="saveRoleLayoutsButton" onClick="saveRoleLayouts()" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
    </div>
</div>
