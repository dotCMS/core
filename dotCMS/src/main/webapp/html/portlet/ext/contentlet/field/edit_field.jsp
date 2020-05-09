
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
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
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="com.dotmarketing.util.PortletID"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="com.dotcms.contenttype.model.type.BaseContentType" %>


<%
    long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    final Structure structure = Structure.class.cast(request.getAttribute("structure"));
    final Contentlet contentlet = Contentlet.class.cast(request.getAttribute("contentlet"));
    long contentLanguage = contentlet.getLanguageId();
    final Field field = Field.class.cast(request.getAttribute("field"));

    Object value = (Object) request.getAttribute("value");
    String hint = UtilMethods.isSet(field.getHint()) ? field.getHint() : null;
    boolean isReadOnly = field.isReadOnly();
    String defaultValue = field.getDefaultValue() != null ? field
            .getDefaultValue().trim() : "";
    String fieldValues = field.getValues() == null ? "" : field
            .getValues().trim();
    Object inodeObj =(Object) request.getAttribute("inode");
    String inode = inodeObj != null ? inodeObj.toString() : "";

    String counter = (String) request.getAttribute("counter");

%>
<div class="fieldWrapper">

    <div class="fieldName" id="<%=field.getVelocityVarName()%>_tag">
        <% if (hint != null) {%>
        <a href="#" id="tip-<%=field.getVelocityVarName()%>"><span class="hintIcon"></span></a>
        <span dojoType="dijit.Tooltip" connectId="tip-<%=field.getVelocityVarName()%>" position="above" style="width:100px;">
				<span class="contentHint"><%=hint%></span>
			</span>
        <%}%>

        <% if(field.isRequired()) {%>
        <span class="required2">
		<%} else {%>
			<span>
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
     		<%=field.getFieldName()%>:</span>
		<% } %>
    </div>

    <div class="fieldValue field__<%=field.getFieldType()%>" id="<%=field.getVelocityVarName()%>_field">
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

                String regex = (isNumber) ? "[0-9]*" : (isFloat) ? "[+-]?([0-9]*[.])?[0-9]+" : "";
        %>
        <%---  Renders the field it self --%>
        <input type="text" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>"
                <%=(isFloat || isNumber) ? "dojoType='dijit.form.ValidationTextBox' data-dojo-props=\"regExp:'"+regex+"', invalidMessage:'Invalid data.'\" style='width:120px;'" : "dojoType='dijit.form.TextBox'" %>
               value="<%= UtilMethods.htmlifyString(textValue) %>" <%= isReadOnly?"readonly=\"readonly\"":"" %> />
        <%
        }
        //END of TEXT field

        //TEXTAREA kind of field rendering
        else if (field.getFieldType().equals(
                Field.FieldType.TEXT_AREA.toString())) {
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
                aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>');
                <%} %>
            });
        </script>
        <div id="aceTextArea_<%=field.getVelocityVarName()%>" class="classAce"></div>
        <textarea <%= isReadOnly?"readonly=\"readonly\" style=\"background-color:#eeeeee;\"":"" %> dojoType="dijit.form.SimpleTextarea"  <%=isWidget?"style=\"overflow:auto;min-height:362px;max-height: 400px\"":"style=\"overflow:auto;min-height:100px;max-height: 600px\""%>
                                                                                                   name="<%=field.getFieldContentlet()%>"
                                                                                                   id="<%=field.getVelocityVarName()%>" class="editTextAreaField" onchange="emmitFieldDataChange(true)"><%= UtilMethods.htmlifyString(textValue) %></textarea>
        <%
            if (!isReadOnly) {
        %>
        <div class="editor-toolbar">
            <div class="toggleEditorField checkbox">
                <%if(toggleOn){ %>
                <input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="true" checked="true"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>');" />
                <%}else{ %>
                <input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="false"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>');" />
                <%} %>
                <label for="toggleEditor_<%=field.getVelocityVarName()%>"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
            </div>
            <div class="langVariablesField inline-form">
                <label for="glossary_term_<%= field.getVelocityVarName() %>">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Language-Variables")) %>:
                </label>
                <input type="text"
                       dojoType="dijit.form.TextBox"
                       id="glossary_term_<%= field.getVelocityVarName() %>"
                       name="glossary_term_<%= field.getVelocityVarName() %>"
                       style="margin-right: 0"
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

        <div class="wysiwyg-wrapper">
            <div id="<%=field.getVelocityVarName()%>aceEditor" class="classAce aceTall" style="display: none"></div>
                <div class="wysiwyg-container">
                  <dot-asset-drop-zone id="dot-asset-drop-zone-<%=field.getVelocityVarName()%>" class="wysiwyg__dot-asset-drop-zone"></dot-asset-drop-zone>
                  <textarea <%= isReadOnly?"readonly=\"readonly\"":"" %>
                          class="editWYSIWYGField aceText aceTall"
                          name="<%=field.getFieldContentlet()%>"
                          id="<%=field.getVelocityVarName()%>"><%=UtilMethods.htmlifyString(textValue)%>
                  </textarea>
                </div>
            <div class="wysiwyg-tools">
              <select  autocomplete="false" dojoType="dijit.form.Select" id="<%=field.getVelocityVarName()%>_toggler" onChange="enableDisableWysiwygCodeOrPlain('<%=field.getVelocityVarName()%>');emmitFieldDataChange(true)">
                  <option value="WYSIWYG">WYSIWYG</option>
                  <option value="CODE" <%= !wysiwygPlain&&wysiwygDisabled?"selected='true'":"" %>>CODE</option>
                  <option value="PLAIN" <%= wysiwygPlain?"selected='true'":"" %>>PLAIN</option>
              </select>

              <div class="langVariablesField inline-form">
                  <label for="glossary_term_<%= field.getVelocityVarName() %>">
                      <%= LanguageUtil.get(pageContext, "Language-Variables") %>:
                  </label>
                  <input type="text" dojoType="dijit.form.TextBox"
                          id="glossary_term_<%= field.getVelocityVarName() %>"
                          name="glossary_term_<%= field.getVelocityVarName() %>"
                          style="margin: 0"
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
        <script type="text/javascript">
            dojo.addOnLoad(function () {
                <% if (!wysiwygDisabled) { %>
                    enableWYSIWYG('<%=field.getVelocityVarName()%>', false);
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

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
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
                dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
                month = cal.get(GregorianCalendar.MONTH) + 1;
                year = cal.get(GregorianCalendar.YEAR) ;
            }%>


        <input type="hidden" id="<%=field.getVelocityVarName()%>"
               name="<%=field.getFieldContentlet()%>"
               value="<%= dateValue!=null ? df.format(dateValue) : "" %>" />

        <%if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>

        <%if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>
        <div class="inline-form">
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
            <input type="text" id="<%=field.getVelocityVarName()%>Time"
                   name="<%=field.getFieldContentlet()%>Time"
                   value='<%=cal!=null ? "T"+hour+":"+min+":00" : ""%>'
                   onChange="updateDate('<%=field.getVelocityVarName()%>');emmitFieldDataChange(true)"
                   dojoType="dijit.form.TimeTextBox"
                    <%=field.isReadOnly()?"disabled=\"disabled\"":""%>/>

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
                Field.FieldType.IMAGE.toString())) {%>
        <input type="text" name="<%=field.getFieldContentlet()%>" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" contentLanguage="<%=contentLanguage%>"
               value="<%= UtilMethods.isSet(value)?value:"" %>" mimeTypes="image" onlyFiles="true" showThumbnail="true" id="<%=field.getVelocityVarName()%>" onChange="emmitFieldDataChange(true)"/>

        <%
            //END IMAGE Field

            //FILE kind of field rendering
        } else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
        %>
        <input type="text" name="<%=field.getFieldContentlet()%>" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="details" contentLanguage="<%=contentLanguage%>"
               value="<%= value %>" onlyFiles="true" showThumbnail="false" id="<%=field.getVelocityVarName()%>"  onChange="emmitFieldDataChange(true)"/>

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

        <!--  display -->
        <% if(UtilMethods.isSet(value)){
            try{
                java.io.File fileValue = (java.io.File)value;
                fileName = fileValue.getName();
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

    <%}else{ %>
       <div id="thumbnailParent<%=field.getVelocityVarName()%>">
           <div dojoType="dotcms.dijit.image.ImageEditor"
                editImageText="<%= LanguageUtil.get(pageContext, "Edit-Image") %>"
                inode="<%= binInode%>"
                fieldName="<%=field.getVelocityVarName()%>"
                binaryFieldId="<%=field.getFieldContentlet()%>"
                fieldContentletId="<%=field.getFieldContentlet()%>"
                saveAsFileName="<%=fileName %>"
                class="thumbnailDiv<%=field.getVelocityVarName()%>"
           >
           </div>
       </div>
    <%} %>



    <%}else{%>

            <% if(UtilMethods.isSet(resourceLink) && !resourceLink.isDownloadRestricted()){ %>

                <div id="<%=field.getVelocityVarName()%>ThumbnailSliderWrapper">
                    <a class="bg" href="javascript: serveFile('','<%=binInode%>','<%=field.getVelocityVarName()%>');"
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

        if(UtilMethods.isSet(value) && UtilMethods.isSet(resourceLink)){

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

    <!--  END display -->
    <!-- javascript -->
    <script type="text/javascript">

        function serveFile(doStuff,conInode,velVarNm){

            if(doStuff != ''){
                window.open('/contentAsset/' + doStuff + '/' + conInode + '/' + velVarNm ,'fileWin','toolbar=no,resizable=yes,width=400,height=300');
            }else{
            }
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
            var tagField = dojo.byId("<%=field.getVelocityVarName()%>");
            dojo.connect(tagField, "onkeyup", suggestTagsForSearch);
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
    <select dojoType="dijit.form.FilteringSelect"   onChange="emmitFieldDataChange(true)" autocomplete="true" id="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" <%=field.isReadOnly()?"readonly=\"\"":""%>>
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
    <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>" value="<%= value %>"/>
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
            $('<%=field.getVelocityVarName()%>').value = valuesList;
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
    <input type="hidden" name="<%=fieldName%>" id="<%=field.getVelocityVarName()%>"
           value="<%=value%>">

    <script type="text/javascript">
        function update<%=field.getVelocityVarName()%>Checkbox() {
        	emmitFieldDataChange(true);
            var valuesList = [];
            var checkedInputs = dojo.query("input:checkbox[name^='<%=fieldName%>Checkbox']:checked");
            checkedInputs.forEach(function(checkedInput) {
                valuesList.push(checkedInput.value);
            });
            $("<%=field.getVelocityVarName()%>").value = valuesList.join(",");
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
 
    %>
    <script>
        dojo.ready(function () {
            setKVValue('<%=field.getFieldContentlet()%>', '<%=field.getVelocityVarName()%>');
            recolorTable('<%=field.getFieldContentlet()%>');
        });
    </script>
    <%

        java.util.Map<String, Object> keyValueMap = new HashMap<>();
        String JSONValue = UtilMethods.isSet(value)? (String)value:"";
        //Convert JSON to Table Display {key, value, order}
        if(UtilMethods.isSet(JSONValue)){
            keyValueMap =  com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(JSONValue);
            if(field.getVelocityVarName().equals("metaData")){
               keyValueMap.put("content", "...");
            }
        }
    %>
    <div class="key-value-form" style="display:<%=field.isReadOnly()?"none":"flex"%>">
        <input type="hidden" class ="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>" value="" />
        <input
                type="text"
                placeholder="<%=LanguageUtil.get(pageContext, "Key")%>"
                name="<%=field.getFieldContentlet()%>_key"
                id="<%=field.getVelocityVarName()%>_key"
                dojoType='dijit.form.TextBox'
                value="" <%=field.isReadOnly()?"disabled":""%> />
        <input
                type="text"
                placeholder="<%=LanguageUtil.get(pageContext, "Value")%>"
                name="<%=field.getFieldContentlet()%>_value"
                id="<%=field.getVelocityVarName()%>_value"
                dojoType='dijit.form.TextBox'
                value="" <%=field.isReadOnly()?"disabled":""%> />
        <button type="submit" dojoType="dijit.form.Button" id="<%=field.getFieldContentlet()%>_addbutton" onClick="addKVPair('<%=field.getFieldContentlet()%>', '<%=field.getVelocityVarName()%>');emmitFieldDataChange(true);" iconClass="plusIcon" <%=field.isReadOnly()?"disabled":""%> type="button"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add")) %></button>
    </div>
    <div id="mainHolder" class="key-value-items">

        <table class="listingTable" id="<%=field.getFieldContentlet()%>_kvtable">
            <% boolean showAlt=false;
            for(String key : keyValueMap.keySet()){%>
               <input type="hidden" id="<%=field.getFieldContentlet()+"_"+key+"_k"%>" value="<%= key %>" />
               <input type="hidden" id="<%=field.getFieldContentlet()+"_"+key+"_v"%>" value="<%= UtilMethods.htmlifyString(UtilMethods.escapeDoubleQuotes(keyValueMap.get(key).toString())) %>" />
               <tr id="<%=field.getFieldContentlet()+"_"+key%>" class="dojoDndItem <%=showAlt ?  "alternate_1" :"alternate_2"%>">
                   <td>
                       <%if(!field.isReadOnly()){ %>
                        <a href="javascript:deleteKVPair('<%=field.getFieldContentlet()%>','<%=field.getVelocityVarName()%>','<%=UtilMethods.escapeSingleQuotes(key)%>');"><span class="deleteIcon"></span></a>
                       <%} %>
                   </td>
                   <td><span><%= key %></span></td>
                   <td><span><%= UtilMethods.htmlifyString(keyValueMap.get(key).toString()) %></span></td>
               </tr>
                <%showAlt=!showAlt;%>
            <%}%>
        </table>

    </div>
    <%if(!field.isReadOnly()){ %>
    <script>
        var source<%=field.getFieldContentlet()%> = new dojo.dnd.Source(dojo.byId('<%=field.getFieldContentlet()%>_kvtable'));
        dojo.connect(source<%=field.getFieldContentlet()%>, "insertNodes", function(){
            setKVValue('<%=field.getFieldContentlet()%>', '<%=field.getVelocityVarName()%>');
            recolorTable('<%=field.getFieldContentlet()%>');
        });
    </script>
    <%}%>

    <%}%>

</div>
<div class="clear"></div>
</div>
