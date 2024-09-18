<%@page import="com.dotmarketing.image.focalpoint.FocalPoint"%>
<%@page import="java.util.Optional"%>
<%@page import="com.dotmarketing.image.focalpoint.FocalPointAPIImpl"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.ResourceLink"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAsset"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI"%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.portlets.structure.business.FieldAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.model.FieldVariable"%>
<%@page import="java.io.IOException"%>

<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="com.dotmarketing.util.PortletID"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>
<%@page import="com.dotmarketing.util.json.JSONObject"%>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="com.dotcms.contenttype.model.type.BaseContentType" %>
<%@ page import="com.dotmarketing.portlets.browser.BrowserUtil" %>
<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotcms.contenttype.transform.field.LegacyFieldTransformer" %>
<%@ page import="static com.dotmarketing.portlets.contentlet.business.ContentletAPI.dnsRegEx" %>
<%@ page import="io.vavr.control.Try" %>
<%@ page import="com.dotcms.contenttype.model.field.HostFolderField" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotcms.contenttype.model.field.JSONField" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="com.fasterxml.jackson.datatype.jdk8.Jdk8Module" %>
<%@ page import="com.dotmarketing.util.ConfigUtils" %>


<%
    long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    final Structure structure = Structure.class.cast(request.getAttribute("structure"));
    final Contentlet contentlet = Contentlet.class.cast(request.getAttribute("contentlet"));
    long contentLanguage = contentlet.getLanguageId() > 0 ? contentlet.getLanguageId() : APILocator.getLanguageAPI().getDefaultLanguage().getId();
    final Field field = Field.class.cast(request.getAttribute("field"));
    final com.dotcms.contenttype.model.field.Field newField = LegacyFieldTransformer.from(field);

    Object value = (Object) request.getAttribute("value");
    ObjectMapper mapper = new ObjectMapper(); // Create an ObjectMapper instance
    mapper.registerModule(new Jdk8Module());
    String hint = UtilMethods.isSet(field.getHint()) ? field.getHint() : null;
    boolean isReadOnly = field.isReadOnly();
    String defaultValue = field.getDefaultValue() != null ? field
            .getDefaultValue().trim() : "";
    String fieldValues = field.getValues() == null ? "" : field
            .getValues().trim();
    Object inodeObj =(Object) request.getAttribute("inode");
    String inode = inodeObj != null ? inodeObj.toString() : "";

    String counter = (String) request.getAttribute("counter");

    boolean fullScreenField = Try.of(()->(boolean)request.getAttribute("DOT_FULL_SCREEN_FIELD")).getOrElse(false);
    String fullScreenClass=fullScreenField ? "edit-content-full-screen": "";
    String fullScreenHeight=fullScreenField ? "height: 100%;": "";
%>

<style type="text/css" media="all">
    .spinner-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 12.5rem;
        min-width: 12.5rem;
    }
    .loader-spinner {
        border-radius: 50%;
        width: 40px;
        height: 40px;
        display: inline-block;
        vertical-align: middle;
        font-size: 10px;
        position: relative;
        text-indent: -9999em;
        border: 4px solid rgba(107, 77, 226, 0.2);
        border-left-color: #6b4de2;
        transform: translateZ(0);
        animation: load8 1.1s infinite linear;
        overflow: hidden;
    }
    @-webkit-keyframes load8 {
        0% {
            -webkit-transform: rotate(0deg);
            transform: rotate(0deg);
        }
        100% {
            -webkit-transform: rotate(360deg);
            transform: rotate(360deg);
        }
    }
    @keyframes load8 {
        0% {
            -webkit-transform: rotate(0deg);
            transform: rotate(0deg);
        }
        100% {
            -webkit-transform: rotate(360deg);
            transform: rotate(360deg);
        }
    }
</style>

<div class="fieldWrapper" >
    <div class="fieldName" id="<%=field.getVelocityVarName()%>_tag">
        <% if (hint != null) {%>
        <a href="#" id="tip-<%=field.getVelocityVarName()%>"><span class="hintIcon"></span></a>
        <span dojoType="dijit.Tooltip" connectId="tip-<%=field.getVelocityVarName()%>" position="above" style="width:100px;">
				<span class="contentHint"><%=hint%></span>
			</span>
        <%}%>

        <% if(field.isRequired()) {%>
        <label for="<%=field.getVelocityVarName()%>_field" class="required">
		<%} else {%>
			<label for="<%=field.getVelocityVarName()%>_field">
		<% } %>
		<%
            if(!field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())&&
                    !field.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString()) &&
                    !field.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString()) &&
                    !field.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString()) &&
                    !field.getFieldType().equals(Field.FieldType.HIDDEN.toString()) &&
                    ! "constant".equals(field.getFieldType())

                    ) {
        %>
     		<%=field.getFieldName()%></label>
		<% } %>
    </div>

    <div class="fieldValue field__<%=field.getFieldType()%> <%= fullScreenClass%>" id="<%=field.getVelocityVarName()%>_field">
        <%
            //TEXT kind of field rendering
            if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
                String textValue = UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
                if(textValue != null){
                    textValue = textValue.replaceAll("&", "&amp;");
                    textValue = textValue.replaceAll("<", "&lt;");
                    textValue = textValue.replaceAll(">", "&gt;");
                }


                boolean isNumber = field.getFieldContentlet().startsWith(Field.DataType.INTEGER.toString());
                boolean isFloat = field.getFieldContentlet().startsWith(Field.DataType.FLOAT.toString());
                boolean isHostNameField = field.getVelocityVarName().equals("hostName");


                String regex = (isNumber) ? "[0-9]*" : (isFloat) ? "[+-]?([0-9]*[.])?[0-9]+" : (isHostNameField) ? dnsRegEx : "";

                if (isHostNameField && textValue != "") {
                    isReadOnly = true;
                }

                if (isHostNameField && textValue != "") {
        %>
            <%---  Renders wrapper to style INPUT and Edit Button for Site name --%>
            <div style="display: flex;">
        <%
                }
        %>
        <%---  Renders the field it self --%>
        <input type="text" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>"
                <%=(isFloat || isNumber || isHostNameField) ? "dojoType='dijit.form.ValidationTextBox' data-dojo-props=\"regExp:'"+regex+"', invalidMessage:'Invalid data.'\" " : "dojoType='dijit.form.TextBox'" %>
                <%=(isFloat || isNumber) ? " style='width:120px;' " : "" %>
               value="<%= UtilMethods.htmlifyString(textValue) %>" <%= isReadOnly?"readonly=\"readonly\"":"" %> />

        <%
            if (isHostNameField && textValue != "") {
        %>
            <%---  Renders EDIT button and dialog for "HostName" field --%>
            <button dojoType="dijit.form.Button" style="margin-left: 8px;" class="dijitButton" onClick="confirmEditSite('siteKeyChangeDialog_<%=field.getInode()%>')">
                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>
            </button>

            <div id="siteKeyChangeDialog_<%=field.getInode()%>" dojoType="dijit.Dialog" style="width:380px;vertical-align: middle; " draggable="true" title="<%= LanguageUtil.get(pageContext, "Change-site-key") %>">
                <span class="ui-confirmdialog-message" style="text-align:center">
                     <%=LanguageUtil.get(pageContext, "Change-site-key-confirm-message") %>
                </span>
                <div style="display: flex; justify-content: center; margin-top: 16px;">
                    <button dojoType="dijit.form.Button" class="dijitButton" onClick="javascript:dijit.byId('siteKeyChangeDialog_<%=field.getInode()%>').hide();">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                    </button>
                    <button style="margin-left: 8px;" dojoType="dijit.form.Button" class="dijitButton" onClick="enableSiteKeyUpdate('siteKeyChangeDialog_<%=field.getInode()%>', <%=field.getVelocityVarName()%>)">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ok")) %>
                    </button>
                </div>
            </div>
        </div>
        <%
            }


        }
        //END of TEXT field

        // STORY BLOCK
        else if (field.getFieldType().equals(Field.FieldType.STORY_BLOCK_FIELD.toString())) {
            String textValue = UtilMethods.isSet(value) ? value.toString(): (UtilMethods.isSet(defaultValue) ? defaultValue : "");
            String safeTextValue = "`" + StringEscapeUtils.escapeJavaScript(textValue.replaceAll("`", "&#96;").replaceAll("\\$", "&#36;")) + "`";

            String contentletIdentifier = contentlet.getIdentifier();
            String jsonField = "{}";
            String contentletObj = "{}";
            Boolean showVideoThumbnail = Config.getBooleanProperty("SHOW_VIDEO_THUMBNAIL", true);

            // If it can be parsed as a JSON, then it means that the value is already a Block Editor's value
            if (value != null) {
                try {
                     Map<String, Object> map = mapper.readValue((String) value, Map.class);
                } catch (IOException e) {
                    // If it can't be parsed as a JSON, then it means that the value is a string
                    value = safeTextValue;
                }
            }

            // Get Field and Contentlet as JSON
            try{
                jsonField = mapper.writeValueAsString(field); // Field
                contentletObj = mapper.writeValueAsString(contentlet); // Contentlet
            } catch(Exception e){
                Logger.error(this.getClass(), e.getMessage());
            }

            List<FieldVariable> acceptTypes=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, false);
            String fieldVariablesContent = mapper.writeValueAsString(acceptTypes); // Field Variables
            %>
            <script src="/html/showdown.min.js"></script>
            <div  id="block-editor-<%=field.getVelocityVarName()%>-container">
                <input type="hidden" name="<%=field.getFieldContentlet()%>" id="editor-input-value-<%=field.getVelocityVarName()%>"/>
            </div>

            <script>

                // Create a new scope so that variables defined here can have the same name without being overwritten.
                (
                    function autoexecute() {
                        const blockEditorContainer = document.querySelector('#block-editor-<%=field.getVelocityVarName()%>-container');
                        const field = document.querySelector('#editor-input-value-<%=field.getVelocityVarName()%>');
                        const blockEditor = document.createElement('dotcms-block-editor');
                        const proseMirror = blockEditor.querySelector('.ProseMirror');
                        blockEditor.id = "block-editor-<%=field.getVelocityVarName()%>";

                        const editorValue = <%=value%> || null;
                        let content;

                        /**
                         * If the value is a string, we need to convert it to HTML
                         * using showdown.
                         * If the value is an object, it means that the value is already Block Editor's
                         */
                        if (typeof editorValue === 'string') {
                            const text = editorValue.replace(/&#96;/g, '`').replace(/&#36;/g, '$');
                            const converter = new showdown.Converter({ tables: true });
                            content = converter.makeHtml(text || '');
                        } else {
                            content = editorValue;
                        }

                        // Set current value in the hidden field
                        field.value = content || '';

                        const contentlet =  (<%=contentletObj%>);
                        const fieldData = {
                            ...(<%=jsonField%>),
                            fieldVariables: JSON.parse('<%=fieldVariablesContent%>')
                        }

                        /**
                         * We need to listen to the "valueChange" event BEFORE setting the value
                         * to the editor.
                         */
                        blockEditor.addEventListener('valueChange', ({ detail }) => {
                            field.value = !detail ? null : JSON.stringify(detail);;
                        });

                        //
                        blockEditor.contentlet = contentlet;
                        blockEditor.field = fieldData;

                        // No variable inputs
                        blockEditor.value = content || '';
                        blockEditor.contentletIdentifier = '<%=contentletIdentifier%>';
                        blockEditor.showVideoThumbnail = <%=showVideoThumbnail%>;
                        blockEditor.isFullscreen = <%=fullScreenField%>;
                        blockEditor.languageId = '<%=contentLanguage%>';
                        blockEditorContainer.appendChild(blockEditor);
                    }
                )();

            </script>
        <% }


        //TEXTAREA kind of field rendering
        else if (field.getFieldType().equals(
                Field.FieldType.TEXT_AREA.toString())
                || newField instanceof JSONField)
        {
            String textValue = UtilMethods.isSet(value) ? (String) value : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
            String keyValue = com.dotmarketing.util.WebKeys.VELOCITY;
            FieldAPI fieldAPI = APILocator.getFieldAPI();
            if(fieldAPI.isElementConstant(field)){
                textValue = field.getValues();
                isReadOnly = true;
            }
            if(textValue != null){
                textValue = textValue.replaceAll("&", "&amp;");
                textValue = textValue.replaceAll("<", "&lt;");
                textValue = textValue.replaceAll(">", "&gt;");
            }
            List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, true);
            for(FieldVariable fv : fieldVariables){
                if(fv.getKey().equals( com.dotmarketing.util.WebKeys.TEXT_EDITOR)){
                    keyValue = fv.getValue();
                    break;
                }
            }
            boolean isWidget = false;
            ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
            int structureType = 0;
            if(UtilMethods.isSet(contentletForm)){
                structureType = contentletForm.getStructure().getStructureType();
            }else{
                structureType = contentlet.getStructure().getStructureType();
            }
            if(structureType == 2){
                isWidget = true;
            }
            boolean toggleOn = isWidget;
            String[] wysiwygsDisabled = new String[0];
            if(contentletForm != null && UtilMethods.isSet(contentletForm.getDisabledWysiwyg())){
                wysiwygsDisabled = contentletForm.getDisabledWysiwyg().split(",");
            }
            for (String fieldVelocityVarName: wysiwygsDisabled) {
                String varName = fieldVelocityVarName;
                if (fieldVelocityVarName.replaceAll(com.dotmarketing.util.Constants.TOGGLE_EDITOR_SEPARATOR,"").trim().equals(field.getVelocityVarName())) {
                    toggleOn = true;
                }
            }
        %>
        <script type="text/javascript">
            dojo.addOnLoad(function () {
                <%if(toggleOn){ %>
                aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>', '<%=fullScreenField%>');
                <%} %>
            });
        </script>
        <div id="aceTextArea_<%=field.getVelocityVarName()%>" class="classAce" style="width:100%; <%=fullScreenHeight%>"></div>
        <textarea <%= isReadOnly?"readonly=\"readonly\" style=\"background-color:#eeeeee;\"":"" %> style="width:100%; <%=fullScreenHeight%>" dojoType="dijit.form.SimpleTextarea"  <%=isWidget?"style=\"overflow:auto;min-height:362px;max-height: 400px\"":"style=\"overflow:auto;min-height:100px;max-height: 600px\""%>
                                                                                                   name="<%=field.getFieldContentlet()%>"
                                                                                                   id="<%=field.getVelocityVarName()%>" class="editTextAreaField" onchange="emmitFieldDataChange(true)"><%= UtilMethods.htmlifyString(textValue) %></textarea>
        <%
            if (!isReadOnly) {
        %>
        <div class="editor-toolbar">
            <div class="toggleEditorField checkbox">
                <%if(toggleOn){ %>
                <input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="true" checked="true"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>', '<%=fullScreenField%>');" />
                <%}else{ %>
                <input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="false"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>' , '<%=fullScreenField%>');" />
                <%} %>
                <label for="toggleEditor_<%=field.getVelocityVarName()%>"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
            </div>
            <div class="langVariablesField inline-form">

                <input type="text"
                       dojoType="dijit.form.TextBox"
                       id="glossary_term_<%= field.getVelocityVarName() %>"
                       name="glossary_term_<%= field.getVelocityVarName() %>"
                       style="margin-right: 0"
                       placeholder=" <%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Language-Variables")) %>"
                       onkeyup="lookupGlossaryTerm('<%= field.getVelocityVarName() %>','<%= contentLanguage %>');" />
                <div class="glossaryTermPopup" style="display:none;" id="glossary_term_popup_<%= field.getVelocityVarName() %>">
                    <div id="glossary_term_table_<%= field.getVelocityVarName() %>"></div>
                </div>
                <script type="text/javascript">
                    dojo.connect(dojo.byId('glossary_term_<%= field.getVelocityVarName() %>'), 'blur', '<%= field.getVelocityVarName() %>', clearGlossaryTermsDelayed);
                </script>
            </div>
        </div>
        <%
                }
            }
            //END of TEXTAREA Field

            //Host or Folder kind of field rendering //http://jira.dotmarketing.net/browse/DOTCMS-3232
            if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                String host = (String)(request.getAttribute("host") != null?request.getAttribute("host"):"");
                String folder = (String)(request.getAttribute("folder") != null?request.getAttribute("folder"):"");
                String selectorValue = UtilMethods.isSet(folder) && !folder.equals(FolderAPI.SYSTEM_FOLDER)?folder:host;
        %>


        <div id="HostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" onChange="updateHostFolderValues('<%=field.getVelocityVarName()%>');emmitFieldDataChange(true); setDotAssetHost();"
             value="<%= selectorValue %>"></div>
        <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>"
               value="<%= selectorValue %>"/>
        <input type="hidden" name="hostId" id="hostId" value="<%=host%>"/>
        <input type="hidden" name="folderInode" id="folderInode" value="<%=folder%>"/>
        <%
        }
        //END of Host or Folder field
        // http://jira.dotmarketing.net/browse/DOTCMS-2274

        //WYSIWYG kind of field rendering
        else if (field.getFieldType().equals(
                Field.FieldType.WYSIWYG.toString())) {
            String textValue = UtilMethods.isSet(value) ? (String) value : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
            textValue = textValue.replaceAll("&", "&amp;");
            ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
            String[] wysiwygsDisabled = new String[0];
            if(contentletForm != null && UtilMethods.isSet(contentletForm.getDisabledWysiwyg())){
                wysiwygsDisabled = contentletForm.getDisabledWysiwyg().split(",");
            }
            boolean wysiwygDisabled = false;
            boolean wysiwygPlain = false;
            for (String fieldVelocityVarName: wysiwygsDisabled) {
                String varName = fieldVelocityVarName;
                if (fieldVelocityVarName.replaceAll(com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR,"").trim().equals(field.getVelocityVarName())) {
                    wysiwygDisabled = true;
                    if(varName.contains(com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR)){
                        wysiwygPlain = true;
                    }
                    break;
                }
            }
        %>


        <div class="wysiwyg-wrapper <%= fullScreenClass%>">
            <div id="<%=field.getVelocityVarName()%>aceEditor" class="classAce aceTall" style="display: none"></div>

            <%
                final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                        contentlet, LegacyFieldTransformer.from(field),
                        user);
                defaultPathFolderPathIds.add(0, "root");

                boolean dragAndDrop = true;

                List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, true);
                    for(FieldVariable fv : fieldVariables){
                        if (fv.getKey().equals("dragAndDrop")) {
                            dragAndDrop = !"false".equalsIgnoreCase(fv.getValue());
                        }
                    }
            %>
                <div class="wysiwyg-container" data-select-folder="<%=String.join(", ", defaultPathFolderPathIds)%>" style="<%= fullScreenHeight%>" >
            <% if (dragAndDrop) {  %>
                  <dot-asset-drop-zone id="dot-asset-drop-zone-<%=field.getVelocityVarName()%>" class="wysiwyg__dot-asset-drop-zone"></dot-asset-drop-zone>
            <% }  %>
                  <textarea <%= isReadOnly?"readonly=\"readonly\"":"" %>
                      class="editWYSIWYGField aceText aceTall"
                      name="<%=field.getFieldContentlet()%>"
                      id="<%=field.getVelocityVarName()%>"
                      style="<%= fullScreenHeight%>">

                      <%=UtilMethods.htmlifyString(textValue)%>

                  </textarea>
                </div>
            <div class="wysiwyg-tools">
              <select  autocomplete="false" dojoType="dijit.form.Select" id="<%=field.getVelocityVarName()%>_toggler" onChange="enableDisableWysiwygCodeOrPlain('<%=field.getVelocityVarName()%>', '<%=fullScreenField%>');emmitFieldDataChange(true)">
                  <option value="WYSIWYG">WYSIWYG</option>
                  <option value="CODE" <%= !wysiwygPlain&&wysiwygDisabled?"selected='true'":"" %>>CODE</option>
                  <option value="PLAIN" <%= wysiwygPlain?"selected='true'":"" %>>PLAIN</option>
              </select>

              <div class="langVariablesField inline-form">

                  <input type="text" dojoType="dijit.form.TextBox"
                          id="glossary_term_<%= field.getVelocityVarName() %>"
                          name="glossary_term_<%= field.getVelocityVarName() %>"
                          style="margin: 0"
                          placeholder=" <%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Language-Variables")) %>"
                          onkeyup="lookupGlossaryTerm('<%= field.getVelocityVarName() %>','<%= contentLanguage %>');" />

                  <div style="display:none" class="glossaryTermPopup" id="glossary_term_popup_<%= field.getVelocityVarName() %>">
                      <div id="glossary_term_table_<%= field.getVelocityVarName() %>"></div>
                  </div>
                  <script type="text/javascript">
                      dojo.connect(dojo.byId('glossary_term_<%= field.getVelocityVarName() %>'), 'blur', '<%= field.getVelocityVarName() %>', clearGlossaryTermsDelayed);
                  </script>
              </div>
          </div>

            <!-- AChecker errors -->
            <div id="acheck<%=field.getVelocityVarName()%>"></div>

        </div>
        <style>
            .editWYSIWYGField.aceText.aceTall {
                height: 400px;
            }
        </style>
        <script type="text/javascript">
            dojo.addOnLoad(function () {
                <% if (!wysiwygDisabled) { %>
                    enableWYSIWYG('<%=field.getVelocityVarName()%>', false, '<%=fullScreenField%>');
                <% } else if (wysiwygPlain) { %>
                    toPlainView('<%=field.getVelocityVarName()%>');
                <% } else {%>
                    toCodeArea('<%=field.getVelocityVarName()%>');
                <% }%>
            });
        </script>

        <%}//END of WYSIWYG

        //DATE/DATETIME/TIME kind of field rendering
        else if (field.getFieldType().equals(
                Field.FieldType.DATE.toString())
                || field.getFieldType().equals(
                Field.FieldType.TIME.toString())
                || field.getFieldType().equals(
                Field.FieldType.DATE_TIME.toString())) {

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
            Date dateValue = null;
            if(value != null && value instanceof String) {
                dateValue = df.parse((String) value);
            } else if(value != null && value instanceof Date) {
                dateValue = (Date)value;
            }

            int dayOfMonth=0;
            int month=0;
            int year=0;
            GregorianCalendar cal=null;

            if(dateValue!=null) {
                cal = new GregorianCalendar();
                cal.setTime((Date) dateValue);
                cal.setTimeZone(APILocator.systemTimeZone());
                dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
                month = cal.get(GregorianCalendar.MONTH) + 1;
                year = cal.get(GregorianCalendar.YEAR) ;
            }%>


        <input type="hidden" id="<%=field.getVelocityVarName()%>"
               name="<%=field.getFieldContentlet()%>"
               value="<%= dateValue!=null ? df.format(dateValue) : "" %>" />

        <%if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>

        <%if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>
            <div class="inline-form" style="align-items: flex-start;">
         <% }%>

            <input type="text"
                   value="<%= dateValue!=null ? df2.format(dateValue) : "" %>"
                   onChange="updateDate('<%=field.getVelocityVarName()%>');emmitFieldDataChange(true)"
                   dojoType="dijit.form.DateTextBox"
                   name="<%=field.getFieldContentlet()%>Date"
                   id="<%=field.getVelocityVarName()%>Date">

            <% }

                if (field.getFieldType().equals(Field.FieldType.TIME.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {

                    String hour=null;
                    String min=null;

                    if(cal!=null) {
                        hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
                        min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
                    }
            %>
            <div>
            <input type="text" id="<%=field.getVelocityVarName()%>Time"
                   name="<%=field.getFieldContentlet()%>Time"
                   value='<%=cal!=null ? "T"+hour+":"+min+":00" : ""%>'
                   onChange="updateDate('<%=field.getVelocityVarName()%>');emmitFieldDataChange(true)"
                   dojoType="dijit.form.TimeTextBox"
                    <%=field.isReadOnly()?"disabled=\"disabled\"":""%>/>
                    <div style="font-size: 85%;padding:5px 0px 0px 5px;color:#888">
                        <%=APILocator.systemTimeZone().getDisplayName()%>
                    </div>
            </div>


        <%if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>
            </div>
        <% }%>
        <% }

            if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
                if(contentletForm != null){
                    String expireDateVar = contentletForm.getStructure().getExpireDateVar();
                    if (field.getVelocityVarName().equals(expireDateVar)) {
                        if (UtilMethods.isSet( value )) {%>
        <div class="checkbox">
            <input type="checkbox" onclick="toggleExpire('<%=field.getVelocityVarName()%>')" dojoType="dijit.form.CheckBox"  name="fieldNeverExpire" id="fieldNeverExpire">
            <label for="fieldNeverExpire"><%= LanguageUtil.get(pageContext, "never") %></label>
        </div>
        <%} else {%>
        <div class="checkbox">
            <input type="checkbox" onclick="toggleExpire('<%=field.getVelocityVarName()%>')" dojoType="dijit.form.CheckBox"  checked ="true" name="fieldNeverExpire"  id="fieldNeverExpire" >
            <label for="fieldNeverExpire"><%= LanguageUtil.get(pageContext, "never") %></label>
        </div>
        <%}%>
        <script type="text/javascript">
            function toggleExpire(velocityVarName) {
            	emmitFieldDataChange(true);
                var never = dijit.byId("fieldNeverExpire").getValue();
                var dateField = dijit.byId(velocityVarName + "Date");
                var timeField = dijit.byId(velocityVarName + "Time");

                if (never) {
                    dateField.set("value", null);
                    timeField.set("value", null);
                    document.getElementById("fm").elements["fieldNeverExpire"].value = "true";
                } else {
                    document.getElementById("fm").elements["fieldNeverExpire"].value = "false";
                }

                dateField.set("disabled", never);
                timeField.set("disabled", never);
            }

            dojo.addOnLoad(function() {
                toggleExpire('<%=field.getVelocityVarName()%>');
            });

        </script>
        <%}
        }
        }
        } //END DATIME/DATE/TIME Field

        //IMAGE kind of field rendering
        else if (field.getFieldType().equals(
                Field.FieldType.IMAGE.toString())) {
            final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                    contentlet, LegacyFieldTransformer.from(field),
                    user);
            defaultPathFolderPathIds.add(0, "root");
        %>
        <input type="text" name="<%=field.getFieldContentlet()%>" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" contentLanguage="<%=contentLanguage%>"
               value="<%= UtilMethods.isSet(value)?value:"" %>" mimeTypes="image" onlyFiles="true" showThumbnail="true" id="<%=field.getVelocityVarName()%>" selectFolder="<%=String.join(", ", defaultPathFolderPathIds)%>" onChange="emmitFieldDataChange(true)"/>

        <%
            //END IMAGE Field

            //FILE kind of field rendering
        } else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
                final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                        contentlet, LegacyFieldTransformer.from(field),
                        user);
                defaultPathFolderPathIds.add(0, "root");
        %>
        <input type="text" name="<%=field.getFieldContentlet()%>" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="details" contentLanguage="<%=contentLanguage%>"
               value="<%= value %>" onlyFiles="true" showThumbnail="false" id="<%=field.getVelocityVarName()%>" selectFolder="<%=String.join(", ", defaultPathFolderPathIds)%>" onChange="emmitFieldDataChange(true)"/>

        <%
            //END FILE Field




            //BINARY kind of field rendering  http://jira.dotmarketing.net/browse/DOTCMS-1073
        } else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())) {
            String fileName = "";
            String sib= request.getParameter("sibbling");
            String binInode=inode;
            if(!UtilMethods.isSet(inode) && UtilMethods.isSet(sib)) {
                binInode=sib;
            }

            ResourceLink resourceLink = new ResourceLinkBuilder().build(request, user, contentlet, field.getVelocityVarName());

        %>



        <%
            if (ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_NEW_BINARY_FIELD")) {
        %>

            <%
                String accept="";
                String maxFileLength="0";
                String helperText="";
                String jsonField = "{}";
                String mimeType="";

                try {
                    java.io.File fileValue = (java.io.File)value;
                    mimeType = com.dotcms.util.MimeTypeUtils.getMimeType(fileValue);
                } catch(Exception e){
                    Logger.error(this.getClass(), e.getMessage());
                }

                try{
                    jsonField = mapper.writeValueAsString(field); // Field
                } catch(Exception e){
                    Logger.error(this.getClass(), e.getMessage());
                }

                List<FieldVariable> acceptTypes=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, false);
                String fieldVariablesContent = mapper.writeValueAsString(acceptTypes); // Field Variables

                for(FieldVariable fv : acceptTypes){
                    if("accept".equalsIgnoreCase(fv.getKey())){
                        accept = fv.getValue();
                    }
                    if("maxFileLength".equalsIgnoreCase(fv.getKey())){
                        maxFileLength=fv.getValue();
                    }

                    if("helperText".equalsIgnoreCase(fv.getKey())){
                        helperText=fv.getValue();
                    }
                }

                %>


            <div id="confirmReplaceNameDialog-<%=field.getVelocityVarName()%>" dojoType="dijit.Dialog" >
                <div dojoType="dijit.layout.ContentPane" style="text-align:center;height:auto;" class="box" hasShadow="true" id="confirmReplaceNameDialog-<%=field.getVelocityVarName()%>CP">
                    <p style="margin:0;max-width:600px;word-wrap: break-word">
                        <%= LanguageUtil.get(pageContext, "Do-you-want-to-replace-the-existing-asset-name") %>
                        "<span id="confirmReplaceNameDialog-<%=field.getVelocityVarName()%>-oldValue"> </span>"
                        <%= LanguageUtil.get(pageContext, "with") %>
                        "<span id="confirmReplaceNameDialog-<%=field.getVelocityVarName()%>-newValue"></span>""
                        <br>&nbsp;<br>
                    </p>
                    <div class="buttonRow">
                        <button dojoType="dijit.form.Button" onClick="confirmReplaceName()" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "yes") %></button>

                        <button dojoType="dijit.form.Button" onClick="closeConfirmReplaceName()" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "no") %></button>
                    </div>
                </div>
            </div>

            <div id="container-binary-field-<%=field.getVelocityVarName()%>">
                <div class="spinner-container">
                    <div class="loader-spinner"></div>
                </div>
            </div>
            <input name="<%=field.getFieldContentlet()%>" id="binary-field-input-<%=field.getFieldContentlet()%>ValueField" type="hidden" />

            <script>
                function confirmReplaceName(){
                    let titleField = dijit.byId("title");
                    let fileNameField = dijit.byId("fileName");

                    titleField?.setValue(newFileName);
                    fileNameField?.setValue(newFileName);
                    dijit.byId("confirmReplaceNameDialog-<%=field.getVelocityVarName()%>").hide();
                }

                function closeConfirmReplaceName(){
                    dijit.byId("confirmReplaceNameDialog-<%=field.getVelocityVarName()%>").hide();
                }
            </script>
            <script>
                // Create a new scope so that variables defined here can have the same name without being overwritten.
                (function autoexecute() {
                    const binaryFieldContainer = document.getElementById("container-binary-field-<%=field.getVelocityVarName()%>");

                    /**
                     * Note: This is a temporary solution.
                     * This is a workaround to get the contentlet from the API
                     * because there is no way to get the same contentlet the AP retreive from the dwr call.
                     */
                    fetch('/api/v1/content/<%=inode%>', {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    })
                    .then(response => response.json())
                    .then(({ entity: contentlet }) => {
                        const field = document.querySelector('#binary-field-input-<%=field.getFieldContentlet()%>ValueField');
                        const variable = "<%=field.getVelocityVarName()%>";

                        let fieldData;
                        try {
                            fieldData = {
                                ...(<%=jsonField%>),
                                hint: '<%=hint%>',
                                variable,
                                fieldVariables: <%=fieldVariablesContent%>
                            }
                        } catch (error) {
                            fieldData = {
                                 ...(<%=jsonField%>),
                                 hint: '<%=hint%>',
                                 variable
                            }
                            console.warn('Error parsing the field variables', error);
                        }
                        // Setting the value of the field
                        field.value = "<%=value%>"

                        // Creating the binary field dynamically
                        // Help us to set inputs before the ngInit is executed.
                        const binaryField = document.createElement('dotcms-binary-field');
                        binaryField.id = "binary-field-<%=field.getVelocityVarName()%>";
                        binaryField.setAttribute("fieldName", "<%=field.getVelocityVarName()%>");

                        binaryField.field = fieldData;
                        binaryField.contentlet = contentlet;
                        binaryField.imageEditor = true;

                        const contentBaseType = <%= contentlet.getContentType().baseType().getType() %>;

                        binaryField.addEventListener('valueUpdated', ({ detail }) => {
                            const { value, fileName } = detail;
                            field.value = value;
                            if(contentBaseType === 4){ // FileAsset
                                let titleField = dijit.byId("title");
                                let fileNameField = dijit.byId("fileName");
                                window.newFileName = fileName; //To use in confirmReplaceName function

                                if(!fileNameField.value){
                                    titleField?.setValue(fileName);
                                }

                                if(fileNameField.value && fileName && fileNameField.value !== fileName) {
                                    document.getElementById("confirmReplaceNameDialog-<%=field.getVelocityVarName()%>-oldValue").innerHTML = fileNameField.value;
                                    document.getElementById("confirmReplaceNameDialog-<%=field.getVelocityVarName()%>-newValue").innerHTML = fileName;
                                    dijit.byId("confirmReplaceNameDialog-<%=field.getVelocityVarName()%>").show();
                                }

                                if(!fileNameField.value){
                                    fileNameField?.setValue(fileName);
                                }
                            }
                        });

                        binaryFieldContainer.innerHTML = '';
                        binaryFieldContainer.appendChild(binaryField);

                        document.addEventListener(`binaryField-open-image-editor-${variable}`,({ detail }) => {
                            const { inode, variable = '', tempId } = detail;

                            const imageEditor = new dotcms.dijit.image.ImageEditor({
                                inode,
                                tempId,
                                variable,
                                fieldName: variable,
                                binaryFieldId:  variable,
                                focalPoint: "0.0,0.0",
                            });

                            imageEditor.execute();
                        });
                    })
                    .catch(() => {
                        binaryFieldContainer.innerHTMl = '<div class="callOutBox">Error loading the binary field</div>';
                    })
                })();
            </script>
        <%}else{%>

        <!--  display -->
        <% if(UtilMethods.isSet(value)){
            String mimeType="application/octet-stream";
            fileName="unknown";



            try{
                java.io.File fileValue = (java.io.File)value;
                fileName = fileValue.getName();
                mimeType = com.dotcms.util.MimeTypeUtils.getMimeType(fileValue);
            }

            catch(Exception e){
                Logger.error(this.getClass(), "-----------------------------------");
                Logger.error(this.getClass(), e.getMessage());
                if(e.getCause() !=null){
                    Logger.error(this.getClass(), e.getCause().getMessage(), e);
                }
                Logger.error(this.getClass(), "crappy value=" + value);
                Logger.error(this.getClass(), "-----------------------------------");
                fileName = value.toString();
            }
            %>




        <%if(mimeType.startsWith("video/")){%>
            <div id="thumbnailParent<%=field.getVelocityVarName()%>">
                <video controls src="/dA/<%=inode%>/<%=field.getVelocityVarName()%>/<%=fileName%>" class="thumbnailDiv" style="max-width:75%;max-height:300px;">
            </div>
        <%}else if(fileName!=null && fileName.toLowerCase().endsWith("pdf")){%>
            <div id="thumbnailParent<%=field.getVelocityVarName()%>">
                <a href="/dA/<%=inode%>/<%=field.getVelocityVarName()%>/<%=fileName%>" target="downloadMe">
                    <img src="/dA/<%=inode%>/<%=field.getVelocityVarName()%>/<%=fileName%>/1000h" class="thumbnailDiv" style="max-width:75%;max-height:300px;">
                </a>
            </div>
        <%}%>




        <%if(UtilMethods.isImage(fileName)){%>
        <%int showDim=300; %>
        <%int imageEditors=0; %>
        <!--  If you are not enterprise -->
        <%if(LicenseUtil.getLevel() <= LicenseLevel.STANDARD.level ){ %>
           <div id="thumbnailParent<%=field.getVelocityVarName()%>">
               <%
                   String src = String.format("/contentAsset/image/%s/%s/?filter=Thumbnail&thumbnail_w=%d&thumbnail_h=%d&language_id=%s&r=%d", contentlet.getIdentifier(), field.getVelocityVarName(), showDim, showDim, contentlet.getLanguageId(), System.currentTimeMillis());

               %>
               <img src="<%=src%>"
                    class="thumbnailDiv thumbnailDiv<%=field.getVelocityVarName()%>"
                    onmouseover="dojo.attr(this, 'className', 'thumbnailDiv thumbnailDivHover');"
                    onmouseout="dojo.attr(this, 'className', 'thumbnailDiv');"
                    onclick="dijit.byId('fileDia<%=field.getVelocityVarName()%>').show()">
           </div>

           <div dojoType="dijit.Dialog" id="fileDia<%=field.getVelocityVarName()%>" title="<%=LanguageUtil.get(pageContext,"Image") %>"  style="width:90%;height:80%;display:none;"">
           <div style="text-align:center;margin:auto;overflow:auto;width:700px;height:400px;">
               <img src="/contentAsset/image/<%=binInode %>/<%=field.getVelocityVarName() %>/" />
           </div>
           <div class="callOutBox">
               <%=LanguageUtil.get(pageContext,"dotCMS-Enterprise-comes-with-an-advanced-Image-Editor-tool") %>
           </div>
       </div>

    <% }else{ %>

        <%
         final Optional<FocalPoint> focalPoint =new FocalPointAPIImpl().readFocalPoint(binInode, field.getVelocityVarName());
         final String fpStr = focalPoint.isPresent() ? focalPoint.get().x + "," + focalPoint.get().y :"0.0,0.0";
        %>


       <div id="thumbnailParent<%=field.getVelocityVarName()%>">
           <div dojoType="dotcms.dijit.image.ImageEditor"
                editImageText="<%= LanguageUtil.get(pageContext, "Edit-Image") %>"
                inode="<%= binInode%>"
                fieldName="<%=field.getVelocityVarName()%>"
                binaryFieldId="<%=field.getFieldContentlet()%>"
                fieldContentletId="<%=field.getFieldContentlet()%>"
                focalPoint="<%=fpStr %>"
                saveAsFileName="<%=fileName %>"
                class="thumbnailDiv<%=field.getVelocityVarName()%>"
           >
           </div>
       </div>
    <%} %>



    <%}else{%>

            <% if(UtilMethods.isSet(resourceLink) && !resourceLink.isDownloadRestricted()){ %>

                <div id="<%=field.getVelocityVarName()%>ThumbnailSliderWrapper">
                    <a class="bg" href="/contentAsset/raw-data/<%=binInode%>/<%=field.getVelocityVarName()%>?byInode=true&force_download=true" download
                       id="<%=field.getVelocityVarName()%>BinaryFile"><%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "download"))%></a>
                    <br/>
                </div>

            <% } %>

    <%}

    }%>


    <%
       String maxFileLength="-1";
       String accept="*/*";
       List<FieldVariable> acceptTypes=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, false);
       for(FieldVariable fv : acceptTypes){
           if("accept".equalsIgnoreCase(fv.getKey())){
               accept = fv.getValue();

           }
           if("maxFileLength".equalsIgnoreCase(fv.getKey())){
             maxFileLength=fv.getValue();

           }
       }

    %>


    <%-- File uploader --%>

    <div
            assetName="<%= contentlet.isFileAsset() ? resourceLink.getAssetName() : "" %>"
            resourceLink="<%= contentlet.isFileAsset() ? resourceLink.getResourceLinkAsString() : "" %>"
            resourceLinkUri="<%= contentlet.isFileAsset() ? resourceLink.getResourceLinkUriAsString() : "" %>"
            resourceLinkLabel="<%= contentlet.isFileAsset() ? LanguageUtil.get(pageContext, "Resource-Link") : "" %>"
            versionPath="<%= !resourceLink.isDownloadRestricted() ? resourceLink.getVersionPath() : "" %>"
            versionPathLabel="<%= LanguageUtil.get(pageContext, "VersionPath") %>"
            idPath="<%= !resourceLink.isDownloadRestricted() ? resourceLink.getIdPath() : "" %>"
            idPathLabel="<%= LanguageUtil.get(pageContext, "IdPath") %>:"

            id="<%=field.getVelocityVarName()%>"
            name="<%=field.getFieldContentlet()%>"
            fileNameVisible="false"
            <%= UtilMethods.isSet(fileName)?"fileName=\"" + fileName.replaceAll("\"", "\\\"") +"\"":"" %>
            fieldName="<%=field.getVelocityVarName()%>"
            dowloadRestricted="<%= UtilMethods.isSet(resourceLink) ? resourceLink.isDownloadRestricted() : false %>"
            inode="<%= binInode%>"
            lang="<%=contentlet.getLanguageId() %>"
            identifier="<%=contentlet.getIdentifier()%>"
            inodeShorty="<%=APILocator.getShortyAPI().shortify(contentlet.getInode())%>"
            idShorty="<%=APILocator.getShortyAPI().shortify(contentlet.getIdentifier())%>"
            onRemove="removeThumbnail('<%=field.getVelocityVarName()%>', '<%= binInode %>')"
            dojoType="dotcms.dijit.form.FileAjaxUploader"
            maxFileLength="<%= maxFileLength%>"
            licenseLevel="<%=LicenseUtil.getLevel() %>"
            accept="<%=accept %>" >
    </div>
    <script type="text/javascript">
        function saveBinaryFileOnContent<%=field.getVelocityVarName()%>(fileName, dijitReference){
            saveBinaryFileOnContent('<%=field.getInode()%>','<%=field.getVelocityVarName()%>','<%=field.getFieldContentlet()%>', dijitReference.fileNameField.value);
        }

        function doReplaceFileAssetName(newAssetName) {
            let titleField = dijit.byId("title");
            let fileNameField = dijit.byId("fileName");
            titleField.setValue(newAssetName);
            fileNameField.setValue(newAssetName);
            dijit.byId('fileAsset-Dialog').hide();
        }

        function doNotReplaceFileAssetName() {
            dijit.byId('fileAsset-Dialog').hide();
        }

    </script>


    <%

        String bnFlag = Config.getStringProperty("FEATURE_FLAG_NEW_BINARY_FIELD");
        Boolean newBinaryOn = bnFlag != null && bnFlag.equalsIgnoreCase("true");

        Boolean isBinaryField = field.getFieldType().equals(Field.FieldType.BINARY.toString());
        Boolean shouldShowEditFileOnBn = isBinaryField && !newBinaryOn; // If the new binary field is on, we don't show the edit field

        if(UtilMethods.isSet(value) && UtilMethods.isSet(resourceLink) && shouldShowEditFileOnBn){

          boolean canUserWriteToContentlet = APILocator.getPermissionAPI().doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE, user);

    %>

        <%if(canUserWriteToContentlet){%>
            <% if (resourceLink.isEditableAsText()) { %>
                <%
                    if (InodeUtils.isSet(binInode) && canUserWriteToContentlet) {

                %>
                    <%@ include file="/html/portlet/ext/contentlet/field/edit_file_asset_text_inc.jsp"%>
                <%  } %>
            <% } %>

        <% } %>
      <% } %>
    <%} %>
    <!--  END display -->
    <!-- javascript -->
    <script type="text/javascript">

        function serveFile(doStuff,conInode,velVarNm){
            var link

            if (doStuff != ''){
                link = '/contentAsset/' + doStuff + '/' + conInode + '/' + velVarNm + "?byInode=true";
            } else {
                link = '/contentAsset/raw-data/' + conInode + '/' + velVarNm + "?byInode=true";
            }
            window.location.href = link
        }

        function change<%=field.getFieldContentlet()%>ThumbnailSize(newValue) {
            <%=field.getFieldContentlet()%>ThumbSize = newValue;
            $('<%=field.getFieldContentlet()%>Thumbnail').src =
                "/contentAsset/image-thumbnail/<%=inode+"/"+field.getVelocityVarName()%>?w=" + newValue + "&rand=" + Math.random();
            dojo.cookie('<%=field.getStructureInode()%>-<%=field.getFieldContentlet()%>ThumbSize', new String(newValue));
        }



    </script>
    <!--end of javascript -->

    <div class="clear"></div>

    <%
        //END BINARY Field

        //TAG kind of field rendering

    } else if (field.getFieldType().equals(Field.FieldType.TAG.toString())) {
        String textValue = UtilMethods.isSet(value) ? (String) value : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
        String hiddenTextValue = textValue.replaceAll(":persona","");
    %>
    <!-- display -->
    <div class="tagsWrapper" id="<%=field.getVelocityVarName()%>Wrapper">
      <input type="hidden" name="<%=field.getVelocityVarName()%>" id="<%=field.getVelocityVarName()%>Content" value="<%=hiddenTextValue%>" />
      <input type="text" name="name" value="" dojoType="dijit.form.TextBox" id="<%=field.getVelocityVarName()%>"   onChange="emmitFieldDataChange(true)" />
      <div class="tagsOptions" id="<%=field.getVelocityVarName()%>SuggestedTagsDiv" style="display:none;"></div>
    </div>

    <script>
        dojo.addOnLoad(function() {

            <%
              Optional<com.dotcms.contenttype.model.field.Field> hostFolderField = Optional.empty();
              final ContentType contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser()).find(structure.getVelocityVarName())).getOrNull();
              if(null != contentType){
                  hostFolderField = contentType.fields(HostFolderField.class).stream().findFirst();
              }
            %>

            let tagField = dojo.byId("<%=field.getVelocityVarName()%>");
            dojo.connect(tagField, "onkeyup", function(e){

                let selectedHost = "<%= contentType != null ? contentType.host() : Host.SYSTEM_HOST%>";
                let hostOrFolderField = dojo.byId("<%=hostFolderField
                        .map(com.dotcms.contenttype.model.field.Field::variable).orElse(null)%>");
                suggestTagsForContent(e, null, hostOrFolderField, selectedHost);
            });
            dojo.connect(tagField, "onblur", closeSuggetionBox);
            var textValue = "<%=textValue%>";
            if (textValue != "") {
                fillExistingTags("<%=field.getVelocityVarName()%>", textValue);
            }
        })
    </script>
    <!-- end display -->
    <%
        //RADIO kind of field rendering

    } else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {

        String radio = field.getFieldContentlet();
        String[] pairs = fieldValues.contains("\r\n") ? fieldValues.split("\r\n") : fieldValues.split("\n");
        for (int j = 0; j < pairs.length; j++) {
            String pair = pairs[j];
            String[] tokens = pair.split("\\|");
            String name = (tokens.length > 0 ? tokens[0] : "");
            Object pairValue = (tokens.length > 1 ? tokens[1] : name);
            if (value instanceof Boolean)
                pairValue = Parameter
                        .getBooleanFromString((String) pairValue);
            else if (value instanceof Long)
                pairValue = Parameter.getLong((String) pairValue);
            else if (value instanceof Double)
                pairValue = Parameter.getDouble((String) pairValue);

            String checked = "";
            if ((UtilMethods.isSet(value) && pairValue.toString().equals(value.toString()))|| (!UtilMethods.isSet(value) && UtilMethods.isSet(defaultValue) && defaultValue.toString().equals(pairValue.toString()))) {
                checked = "checked";
            }
    %>
    <div class="radio">
        <input type="radio" dojoType="dijit.form.RadioButton"   onChange="emmitFieldDataChange(true)" name="<%=radio%>" id="<%=field.getVelocityVarName() + j %>" value="<%=pairValue%>"<%=field.isReadOnly()?" disabled=\"disabled\" ":"" %><%=checked%>>
        <label for="<%=field.getVelocityVarName() + j %>"><%=name%></label>
    </div>
    <%
        }
    %> <%
    //SELECT kind of field rendering
} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
%>
    <select dojoType="dijit.form.FilteringSelect"   onChange="emmitFieldDataChange(true)" autocomplete="true" id="<%=field.getVelocityVarName()%>Select" name="<%=field.getFieldContentlet()%>" <%=field.isReadOnly()?"readonly=\"\"":""%>>
        <%
            String[] pairs = fieldValues.contains("\r\n") ? fieldValues.split("\r\n") : fieldValues.split("\n");
            for (int j = 0; j < pairs.length; j++)
            {
                String pair = pairs[j];
                String[] tokens = pair.split("\\|");
                String name = (tokens.length > 0 ? tokens[0] : "");
                String pairvalue = (tokens.length > 1 ? tokens[1].trim() : name.trim());
                String selected = "";
                String compareValue = (UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : ""));
                if (compareValue != null && (compareValue.equals(pairvalue)))
                {
                    selected = "SELECTED";
                }
                //Added to support boolean values with: true/false - 1/0  - t/f
                else if((compareValue.equalsIgnoreCase("true") && (pairvalue.equalsIgnoreCase("true") || pairvalue.equalsIgnoreCase("1") || pairvalue.equalsIgnoreCase("t"))) ||
                        (compareValue.equalsIgnoreCase("false") && (pairvalue.equalsIgnoreCase("false") || pairvalue.equalsIgnoreCase("0") || pairvalue.equalsIgnoreCase("f"))))
                {
                    selected = "SELECTED";
                }
        %>
        <option value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(pairvalue)%>" <%=selected%>><%=name%></option>
        <%
            }
        %>
    </select>
    <%
        //END of select kind of field

        //MULTISELECT kind of field rendering

    } else if (field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())) {

        String[] pairs = fieldValues.contains("\r\n") ? fieldValues.split("\r\n") : fieldValues.split("\n");
    %>
    <select multiple="multiple" size="scrollable"
            name="<%=field.getFieldContentlet()%>MultiSelect"
            id="<%=field.getVelocityVarName()%>MultiSelect"
            onchange="update<%=field.getVelocityVarName()%>MultiSelect();emmitFieldDataChange(true)"
            style="width: 200px;""<%=field.isReadOnly()?"readonly=\"readonly\"":""%>">
    <%
        String compareValue = (UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : ""));
        String[] compareValueTokens = compareValue.split(",");
        for (int j = 0; j < pairs.length; j++) {
            String pair = pairs[j];
            String[] tokens = pair.split("\\|");
            String name = (tokens.length > 0 ? tokens[0] : "");
            String pairvalue = (tokens.length > 1 ? tokens[1] : name);
            String selected = "";
            String separator = (j<pairs.length-1)?",":"";
            if(UtilMethods.isSet(pairvalue)){
                for(int k=0; k<compareValueTokens.length; k++){
                    if(UtilMethods.isSet(compareValueTokens[k]) && pairvalue.equals(compareValueTokens[k])) {
                        selected = "SELECTED";
                        break;
                    }
                }
            }
            %>
            <option value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(pairvalue)%>" <%=selected%>><%=name%></option>
            <%
        }
    %>
    </select>
    <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>MultiSelectHF" value="<%= value %>"/>
        <script type="text/javascript">
        function update<%=field.getVelocityVarName()%>MultiSelect() {
            var valuesList = "";
            var multiselect = $('<%=field.getVelocityVarName()%>MultiSelect');
            for(var i = 0; i < multiselect.options.length; i++) {
                if(multiselect.options[i].selected) {
                    if (valuesList != ""){
                        valuesList += ","
                    }
                    valuesList += multiselect.options[i].value;
                }
            }
            $('<%=field.getVelocityVarName()%>MultiSelectHF').value = valuesList;
        }

        update<%=field.getVelocityVarName()%>MultiSelect();
    </script>

    <%
        //CHECKBOX Field rendering
    } else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {

        String fieldName = field.getFieldContentlet();
        String[] pairs = fieldValues.contains("\r\n") ? fieldValues.split("\r\n") : fieldValues.split("\n");
        for (int j = 0; j < pairs.length; j++) {
            String pair = pairs[j];
            String[] tokens = pair.split("\\|");
            String name = (tokens.length > 0 ? tokens[0] : "");
            String pairValue = (tokens.length > 1 ? tokens[1] : name);
            String checked = "";
            if (UtilMethods.isSet(value)) {
                if(value instanceof Number){
                    value = String.valueOf(value);
                }

                if(UtilMethods.isSet(pairValue)) {
                    // Find and checked values saved
                    if (Arrays.asList(((String)value).split(",")).contains(pairValue)) {
                        checked = "CHECKED";
                    }
                }
            } else {
                if (UtilMethods.isSet(defaultValue)) {
                    // Find and checked default values
                    if (Arrays.asList(defaultValue.split("|")).contains(pairValue)) {
                        checked = "CHECKED";
                    }
                }
            }
    %>
    <div class="checkbox">
        <input type="checkbox" dojoType="dijit.form.CheckBox" name="<%=fieldName%>Checkbox" id="<%=fieldName + j%>Checkbox"
               value="<%=pairValue%>" <%=checked%> <%=field.isReadOnly()?"disabled":""%>
               onchange="update<%=field.getVelocityVarName()%>Checkbox();">&nbsp
        <label for="<%=fieldName + j%>Checkbox"><%=name%></label>
    </div>
    <%
        }
    %>
    <input type="hidden" name="<%=fieldName%>" id="<%=field.getVelocityVarName()%>Checkbox"
           value="<%=value%>">

    <script type="text/javascript">
        function update<%=field.getVelocityVarName()%>Checkbox() {
        	emmitFieldDataChange(true);
            var valuesList = [];
            var checkedInputs = dojo.query("input:checkbox[name^='<%=fieldName%>Checkbox']:checked");
            checkedInputs.forEach(function(checkedInput) {
                valuesList.push(checkedInput.value);
            });
            $("<%=field.getVelocityVarName()%>Checkbox").value = valuesList.join(",");
        }

        update<%=field.getVelocityVarName()%>Checkbox();
    </script>

    <%
        //CATEGORY Field rendering
    } else if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {

        CategoryAPI catAPI = APILocator.getCategoryAPI();
        String[] selectedCategories = null;
        List<Category> categoriesList = null;
        String catInode = field.getInode();

        if (UtilMethods.isSet(value)) {
            categoriesList =  (List<Category>) value;
            selectedCategories =  new String[categoriesList.size()];
            int i = 0;
            for(Category cat: categoriesList){
                if(cat != null){
                    selectedCategories[i] = String.valueOf(cat.getInode());
                }
                i++;
            }
        } else {
            selectedCategories = field.getDefaultValue().split("\\|");
            int i = 0;
            for(String selectedCat: selectedCategories){
                Category selectedCategory = catAPI.findByName(selectedCat, user, false);
                selectedCategories[i] = String.valueOf(selectedCategory.getInode());
            }
            i++;
        }

        try {
            Category category = catAPI.find(field.getValues(), user, false);

            if(category != null && catAPI.canUseCategory(category, user, false)) {
                if(UtilMethods.isSet(counter)) {
                    request.setAttribute("counter", counter.toString());
                }
    %>
    <jsp:include page="/html/portlet/ext/categories/view_categories_dialog.jsp" />
    <a  id="link<%=counter%>"  href="javascript: showCatDialog<%=counter%>();"><%= LanguageUtil.get(pageContext, "select-categories") %></a>
    <div id="previewCats<%=counter%>" class="catPreview">
    </div>
    <script type="text/javascript">
        function init<%=counter%>() {
            baseCat<%=counter%> = currentInodeOrIdentifier<%=counter%>;
            dojo.byId("a_null<%=counter%>").id = "a_" + baseCat<%=counter%>;
            dojo.connect(dijit.byId('categoriesDialog<%=counter%>'), "hide", function(evt) {
                dojo.byId("catFilter<%=counter%>").blur();
                dojo.window.scrollIntoView('link<%=counter%>');
            });
        }

        function showCatDialog<%=counter%>() {
            dijit.byId('categoriesDialog<%=counter%>').show();
            doSearch<%=counter%>();
            fixAddedGrid<%=counter%>();
            emmitFieldDataChange(true);
        }

        dojo.addOnLoad(function() {
            currentInodeOrIdentifier<%=counter%> = '<%= category.getInode() %>';
            init<%=counter%>();
            initDialog<%=counter%>();
            showCatDialog<%=counter%>();
            <% if(UtilMethods.isSet(categoriesList)) {
                   for (Category cat: categoriesList) {
                       boolean add = catAPI.isParent(cat, category, user);

                       if(add) {
                       %>
            addSelectedCat<%=counter%>("<%= cat.getInode() %>", "<%= cat.getCategoryName() %>");
            <%}
         }
       }
  %>
            dijit.byId('categoriesDialog<%=counter%>').hide();
        });



    </script>
    <%
    } else {
    %>
    <br/>
    <%
        }

    } catch (DotSecurityException e) {
    %>
    <br/>
    <%
            Logger.debug(this, "User don't have permissions to edit this category");
        }
    %>

    <%

    }

//http://jira.dotmarketing.net/browse/DOTCMS-2869
//CUSTOM_FIELD kind of field rendering for DOTCMS-2869
    else if (field.getFieldType().equals(
            Field.FieldType.CUSTOM_FIELD.toString())) {
        String textValue = UtilMethods.isSet(value) ? (String) value : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
        textValue = field.getValues();
        String HTMLString = "";

        if(UtilMethods.isSet(textValue)){
            org.apache.velocity.context.Context velocityContext =  com.dotmarketing.util.web.VelocityWebUtil.getVelocityContext(request,response);
            // set the velocity variable for use in the code (if it has not already been set
            if(!UtilMethods.isSet(velocityContext.get(field.getVelocityVarName()))){
                if(UtilMethods.isSet(value)){
                    velocityContext.put(field.getVelocityVarName(), value);
                }
                else{
                    velocityContext.put(field.getVelocityVarName(), defaultValue);
                }
            }
            HTMLString = new VelocityUtil().parseVelocity(textValue,velocityContext);
        }

    %>
    <!--  variables -->
    <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>" value="<%=UtilMethods.htmlifyString((String) value) %>" />
    <!-- END variables -->
    <%=HTMLString %>

    <%
    }
//END of CUSTOM_FIELD
//KEY_VALUE Field
    else if(field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){

        java.util.Map<String, Object> keyValueMap = null;

        if(value instanceof Map){
            keyValueMap = new LinkedHashMap<>((Map)value);
        } else {
            keyValueMap = new LinkedHashMap();
        }

        if("metaData".equals(field.getVelocityVarName())){
            keyValueMap.put("content", "...");
        }

        final StringBuilder keyValueDataRaw = new StringBuilder("{");
        final StringBuilder dotKeyValueDataRaw = new StringBuilder("{");

        final Iterator<String> iterator = keyValueMap.keySet().iterator();

        while (iterator.hasNext()) {
            final String key = iterator.next();
            final Object object = keyValueMap.get(key);
            if(null != object) {
                keyValueDataRaw.append(key.replaceAll(":", "&#58;").replaceAll(",", "&#44;").replaceAll("<", "&lt;")).append(":").append(object.toString().replaceAll(":", "&#58;").replaceAll(",", "&#44;").replaceAll("<", "&lt;"));
                dotKeyValueDataRaw.append("&#x22;" + key.replaceAll(":", "&#58;").replaceAll(",", "&#44;").replaceAll("<", "&lt;") + "&#x22;").append(":").append("&#x22;" + object.toString().replaceAll(":", "&#58;").replaceAll(",", "&#44;").replaceAll("<", "&lt;") + "&#x22;");
                if (iterator.hasNext()) {
                    keyValueDataRaw.append(',');
                    dotKeyValueDataRaw.append(',');
                }
            }
        }
        keyValueDataRaw.append("}");
        dotKeyValueDataRaw.append("}");

        List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, true);
        String whiteListKeyValues = "";
        for(FieldVariable fv : fieldVariables) {
            if (fv.getKey().equals("whiteList")) {
                whiteListKeyValues = fv.getValue();
            }
        }
    %>
        <input type="hidden" class ="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>" />
        <style>
            dot-key-value key-value-table tr {
                cursor: move;
            }
            dot-key-value key-value-table tr:nth-child(even) {
                background-color: #f3f3f3;
            }
            dot-key-value .key-value-table-wc__placeholder-transit {
                padding: 0;
            }
            dot-key-value .key-value-table-wc__key,
            dot-key-value .key-value-table-wc__value,
            dot-key-value .key-value-table-wc__action {
                padding-left: 0.5rem;
            }
            dot-key-value key-value-form label {
                margin-bottom: 0.5rem;
            }
            dot-key-value button {
                background-color: #fff;
                border: solid 1px var(--color-sec);
                color: var(--color-sec);
                font-family: inherit;
                font-weight: 500;
                margin: 0.5rem 0;
                padding: 0.5rem 0.75rem;
                text-transform: uppercase;
                width: 100%;
            }
            dot-key-value button:focus,
            dot-key-value button:hover {
                border: solid 1px var(--color-main);
                color: var(--color-main);
            }
            dot-key-value button[disabled] {
                background: #f3f3f3;
                border: 1px solid #b3b1b8;
                color: #b3b1b8;
                cursor: not-allowed;
            }
            dot-key-value input,
            dot-key-value select {
                border: 1px solid #b3b1b8;
                padding: 0.75rem 0.75rem;
                width: 100%;
            }
            dot-key-value button {
                cursor: pointer;
            }
            dot-key-value key-value-form {
                margin-bottom: 1rem;
            }
            dot-key-value key-value-form .key-value-table-form__key,
            dot-key-value key-value-form .key-value-table-form__value {
                padding-right: 1.5rem;
            }
        </style>

        <dot-key-value id="<%=field.getVelocityVarName()%>KeyValue"></dot-key-value>
        <script>
            function escapeQuoteAndBackSlash(value) {
                return value.replaceAll('\"','&#34;').replaceAll(/\\/g, '&#92;');
            }

            function formatToJsonData(value) {
                var removedBrackets = value.trim().substring(1, value.length-1);
                var preformatted = removedBrackets.replaceAll(/:/g, '":"').replaceAll(/,/g, '","');
                return preformatted ? `{"${preformatted}"}` : '';
            }

            // Escape chars and set value to hidden input
            var dotKeyValueHiddenIput = document.getElementById('<%=field.getVelocityVarName()%>');
            dotKeyValueHiddenIput.value = formatToJsonData(escapeQuoteAndBackSlash("<%=keyValueDataRaw%>"));

            var dotKeyValue = document.querySelector('#<%=field.getVelocityVarName()%>KeyValue');
            dotKeyValue.uniqueKeys = "true";
            dotKeyValue.value = "<%=dotKeyValueDataRaw.toString()%>";
            dotKeyValue.disabled = '<%=field.isReadOnly()%>';
            dotKeyValue.whiteList = '<%=whiteListKeyValues%>';
            dotKeyValue.formKeyLabel = '<%= LanguageUtil.get(pageContext, "Key") %>'
            dotKeyValue.formValueLabel = '<%= LanguageUtil.get(pageContext, "Value") %>'
            dotKeyValue.formAddButtonLabel = '<%= LanguageUtil.get(pageContext, "Add") %>'
            dotKeyValue.listDeleteLabel = '<%= LanguageUtil.get(pageContext, "Delete") %>'
            dotKeyValue.whiteListEmptyOptionLabel = '<%= LanguageUtil.get(pageContext, "Pick-an-option") %>'
            dotKeyValue.requiredMessage = '<%= LanguageUtil.get(pageContext, "message.fieldvariables.key.required") %>'

            dotKeyValue.addEventListener('dotValueChange', function (event) {
                var escapedData = "{" + escapeQuoteAndBackSlash(event.detail.value) + "}";
                var formattedData = formatToJsonData(escapedData);
                var keyfieldId = document.getElementById('<%=field.getVelocityVarName()%>');
                keyfieldId.value = event.detail.value;
            }, false);

        </script>
    <%}%>

</div>
<div class="clear"></div>
</div>
