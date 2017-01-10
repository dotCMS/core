
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.*"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.contentlet.util.ContentletUtil"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.portlets.structure.business.FieldAPI"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.structure.model.FieldVariable"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>


<%@page import="com.dotcms.enterprise.LicenseUtil"%>

<%
    long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    Contentlet contentlet = (Contentlet) request.getAttribute("contentlet");
    long contentLanguage = contentlet.getLanguageId();
    Field field = (Field) request.getAttribute("field");

    Object value = (Object) request.getAttribute("value");
    String hint = UtilMethods.isSet(field.getHint()) ? field.getHint()
            : null;
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

	<div class="fieldValue" id="<%=field.getVelocityVarName()%>_field">
<%
    //TEXT kind of field rendering
    if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
        String textValue = UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
        if(textValue != null){
            textValue = textValue.replaceAll("&", "&amp;");
            textValue = textValue.replaceAll("<", "&lt;");
            textValue = textValue.replaceAll(">", "&gt;");
        }

        boolean isNumber = (field.getFieldContentlet().startsWith(Field.DataType.INTEGER.toString())
                || field.getFieldContentlet().startsWith(Field.DataType.FLOAT.toString())
        );
%>
    <input type="text" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>"
        <%=(isNumber) ? "dojoType='dijit.form.ValidationTextBox' data-dojo-props=\"regExp:'\\\\d*\\.?\\\\d*', invalidMessage:'Invalid data.'\" style='width:120px;'" : "dojoType='dijit.form.TextBox' style='width:400px'" %>
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
        boolean toggleOn = false;
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
    <textarea <%= isReadOnly?"readonly=\"readonly\" style=\"background-color:#eeeeee;\"":"" %> dojoType="dijit.form.SimpleTextarea"  <%=isWidget?"style=\"overflow:auto;width:682px;min-height:362px;max-height: 400px\"":"style=\"overflow:auto;width:450px;min-height:100px;max-height: 600px\""%>
        name="<%=field.getFieldContentlet()%>"
        id="<%=field.getVelocityVarName()%>" class="editTextAreaField"><%= UtilMethods.htmlifyString(textValue) %></textarea>
<%
    if (!isReadOnly) {
 %>
    <br />
    <div style="padding-right:10px;width:475px;float:left;">
    	<div style="float: left;padding-top: 10px; padding-left: 2px;">
 		<%if(toggleOn){ %>
    		<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="true" checked="true"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>');" />
    		<%}else{ %>
    		<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor_<%=field.getVelocityVarName()%>" value="false"  id="toggleEditor_<%=field.getVelocityVarName()%>"  onclick="aceText('<%=field.getVelocityVarName()%>','<%=keyValue%>','<%=isWidget%>');" />
        <%} %>
        	<label for="toggleEditor"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
        </div>
        <br /> <br />
        <div style="float: left; position: absolute; z-index: 10;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Language-Variables")) %>: <input
            type="text" id="glossary_term_<%= field.getVelocityVarName() %>"
            name="glossary_term_<%= field.getVelocityVarName() %>"
            class="form-text"
            onkeyup="lookupGlossaryTerm('<%= field.getVelocityVarName() %>','<%= contentLanguage %>');" />
            <div style="position: absolute; display: none;"
                id="glossary_term_popup_<%= field.getVelocityVarName() %>">
                <div id="glossary_term_table_<%= field.getVelocityVarName() %>"></div>
            </div>
            <script type="text/javascript">
                dojo.connect(dojo.byId('glossary_term_<%= field.getVelocityVarName() %>'), 'blur', '<%= field.getVelocityVarName() %>', clearGlossaryTermsDelayed);
            </script>
        </div>
    </div>
<div class="clear"></div>
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


     <div id="HostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" onChange="updateHostFolderValues('<%=field.getVelocityVarName()%>');"
            value="<%= selectorValue %>"></div>
     <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>"
            value="<%= selectorValue %>"/>
     <input type="hidden" name="hostId" id="hostId" value="<%=host%>"/>
     <input type="hidden" name="folderInode" id="folderInode" value="<%=folder%>"/>
     <br />
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
    <div style="margin-bottom:20px;width:845px;overflow:visible;border:0px red solid">
    	<div id="<%=field.getVelocityVarName()%>aceEditor" class="classAce"></div>
        <textarea  <%= isReadOnly?"readonly=\"readonly\"":"" %>
            class="editWYSIWYGField" rows="7"
            name="<%=field.getFieldContentlet()%>"
            id="<%=field.getVelocityVarName()%>" style="width:100%; height:450px;font-family:monospace;clear:both;"><%=UtilMethods.htmlifyString(textValue)%>
		</textarea>

   		<table style="margin:10px 5px 20px 5px;">
            <tr>
				<td class="WYSIWYGControls">
	                <select  autocomplete="false" dojoType="dijit.form.Select" id="<%=field.getVelocityVarName()%>_toggler" onChange="enableDisableWysiwygCodeOrPlain('<%=field.getVelocityVarName()%>')">
	                        <option value="WYSIWYG">WYSIWYG</option>
	                        <option value="CODE" <%= !wysiwygPlain&&wysiwygDisabled?"selected='true'":"" %>>CODE</option>
							<option value="PLAIN" <%= wysiwygPlain?"selected='true'":"" %>>PLAIN</option>
	                </select>
	            </td>
	            <td style="text-align:right;padding:0 0 0 30px;">
	              	<div style="position:relative;">
						<%= LanguageUtil.get(pageContext, "Language-Variables") %>:
	                	<input type="text" dojoType="dijit.form.TextBox" id="glossary_term_<%= field.getVelocityVarName() %>"
							name="glossary_term_<%= field.getVelocityVarName() %>"
	                    	class="form-text"
							onkeyup="lookupGlossaryTerm('<%= field.getVelocityVarName() %>','<%= contentLanguage %>');" />

							<div style="display:none;position:absolute;border:1px solid #ddd;padding:5px 10px;z-index:1" id="glossary_term_popup_<%= field.getVelocityVarName() %>">
	                    		<div id="glossary_term_table_<%= field.getVelocityVarName() %>"></div>
		                	</div>
			                <script type="text/javascript">
			                    dojo.connect(dojo.byId('glossary_term_<%= field.getVelocityVarName() %>'), 'blur', '<%= field.getVelocityVarName() %>', clearGlossaryTermsDelayed);
			                </script>
					</div>
	            </td>
			</tr>
        </table>

        <!-- AChecker errors -->
        <div id="acheck<%=field.getVelocityVarName()%>"></div>

    </div>
    <script type="text/javascript">
        dojo.addOnLoad(function () {
        <% if(!wysiwygDisabled) {%>
            enableWYSIWYG('<%=field.getVelocityVarName()%>', false);
        <% }else if(wysiwygPlain){ %>
            toPlainView('<%=field.getVelocityVarName()%>');
        <% }else {%>
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

        <%if (field.getFieldType().equals(Field.FieldType.DATE.toString())
                    || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>

             <input type="text"
                value="<%= dateValue!=null ? df2.format(dateValue) : "" %>"
                onChange="updateDate('<%=field.getVelocityVarName()%>');"
                dojoType="dijit.form.DateTextBox"
                name="<%=field.getFieldContentlet()%>Date"
                id="<%=field.getVelocityVarName()%>Date"
                style="width:120px;">

        <% }

        if (field.getFieldType().equals(Field.FieldType.TIME.toString())
            || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {

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
                onChange="updateDate('<%=field.getVelocityVarName()%>');"
                dojoType="dijit.form.TimeTextBox" style="width: 100px;"
                <%=field.isReadOnly()?"disabled=\"disabled\"":""%>/>
        <% }

            if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
           	    ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
           	   if(contentletForm != null){
	       		    String expireDateVar = contentletForm.getStructure().getExpireDateVar();
	                if (field.getVelocityVarName().equals(expireDateVar)) {
	                	 if (UtilMethods.isSet( value )) {%>
	                     &nbsp;&nbsp;<input type="checkbox" onclick="toggleExpire('<%=field.getVelocityVarName()%>')" dojoType="dijit.form.CheckBox"  name="fieldNeverExpire" id="fieldNeverExpire" > <label for="fieldNeverExpire"><%= LanguageUtil.get(pageContext, "never") %></label>
	                 <%} else {%>
	                     &nbsp;&nbsp;<input type="checkbox" onclick="toggleExpire('<%=field.getVelocityVarName()%>')" dojoType="dijit.form.CheckBox"  checked ="true" name="fieldNeverExpire"  id="fieldNeverExpire" > <label for="fieldNeverExpire"><%= LanguageUtil.get(pageContext, "never") %></label>
	                 <%}%>
                    <script type="text/javascript">
                    function toggleExpire(velocityVarName) {
                        var never = dijit.byId("fieldNeverExpire").getValue();
                        if (never) {
                            dijit.byId(velocityVarName+"Date").set("value", null);
                            dijit.byId(velocityVarName+"Time").set("value", null);
                            document.forms["fm"].elements["fieldNeverExpire"].value ="true";
                        }else{
                        	document.forms["fm"].elements["fieldNeverExpire"].value ="false";
                        }
                      	dijit.byId(velocityVarName+"Date").disabled = never;
                      	dijit.byId(velocityVarName+"Time").disabled = never;
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
                        value="<%= UtilMethods.isSet(value)?value:"" %>" mimeTypes="image" onlyFiles="true" showThumbnail="true" id="<%=field.getVelocityVarName()%>"/>

<%
    //END IMAGE Field

    //FILE kind of field rendering
    } else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
 %>
    <input type="text" name="<%=field.getFieldContentlet()%>" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="details" contentLanguage="<%=contentLanguage%>"
                    value="<%= value %>" onlyFiles="true" showThumbnail="false" id="<%=field.getVelocityVarName()%>"/>

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
                <%if(LicenseUtil.getLevel() < 199 ){ %>
                <div id="thumbnailParent<%=field.getVelocityVarName()%>">
                    <div style="position:relative;width:<%=showDim+40 %>px;">
                        <%
                            String src = null;
                            if(!fileName.toLowerCase().endsWith("svg")){
                                src = String.format("/contentAsset/image/%s/%s/?filter=Thumbnail&thumbnail_w=%d&thumbnail_h=%d&language_id=%s", contentlet.getIdentifier(), field.getVelocityVarName(), showDim, showDim, contentlet.getLanguageId());
                            }else{
                                src = String.format("/contentAsset/image/%s/%s", contentlet.getIdentifier(), field.getVelocityVarName());
                            }
                        %>
                        <img src="<%=src%>"
                             class="thumbnailDiv thumbnailDiv<%=field.getVelocityVarName()%>"
                             onmouseover="dojo.attr(this, 'className', 'thumbnailDivHover');"
                             onmouseout="dojo.attr(this, 'className', 'thumbnailDiv');"
                             onclick="dijit.byId('fileDia<%=field.getVelocityVarName()%>').show()">
                    </div>
               </div>

                    <div dojoType="dijit.Dialog" id="fileDia<%=field.getVelocityVarName()%>" title="<%=LanguageUtil.get(pageContext,"Image") %>"  style="width:760px;height:500px;display:none;"">
                        <div style="text-align:center;margin:auto;overflow:auto;width:700px;height:400px;">
                            <img src="/contentAsset/image/<%=binInode %>/<%=field.getVelocityVarName() %>/?byInode=true" />
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
	                            identifier="<%=contentlet.getIdentifier()%>"
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
                <div id="<%=field.getVelocityVarName()%>ThumbnailSliderWrapper">
                    <a class="bg" href="javascript: serveFile('','<%=binInode%>','<%=field.getVelocityVarName()%>');"
                        id="<%=field.getVelocityVarName()%>BinaryFile"><%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "download"))%></a>
                    <br/>
                </div>


            <%}

            }%>
            <div id="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" <%= UtilMethods.isSet(fileName)?"fileName=\"" + fileName.replaceAll("\"", "\\\"") +"\"":"" %>
               fieldName="<%=field.getVelocityVarName()%>"
               inode="<%= binInode%>"
               lang="<%=contentlet.getLanguageId() %>"
               identifier="<%=contentlet.getIdentifier()%>" onRemove="removeThumbnail('<%=field.getVelocityVarName()%>', '<%= binInode %>')"
               dojoType="dotcms.dijit.form.FileAjaxUploader" onUploadFinish="saveBinaryFileOnContent<%=field.getVelocityVarName()%>">
            </div>
            <script type="text/javascript">
            function saveBinaryFileOnContent<%=field.getVelocityVarName()%>(fileName, dijitReference){
            		saveBinaryFileOnContent('<%=field.getInode()%>','<%=field.getVelocityVarName()%>','<%=field.getFieldContentlet()%>', dijitReference.fileNameField.value);
        	}
            </script>


               <%
                com.dotmarketing.portlets.structure.model.Structure structure   = (com.dotmarketing.portlets.structure.model.Structure) request.getAttribute("structure");
            	boolean canUserWriteToContentlet = APILocator.getPermissionAPI().doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);
                if(UtilMethods.isSet(value) && structure.getStructureType()==com.dotmarketing.portlets.structure.model.Structure.STRUCTURE_TYPE_FILEASSET && field.getVelocityVarName().equals(FileAssetAPI.BINARY_FIELD)){ %>

                      <%if(canUserWriteToContentlet){%>
						<div class="clear"></div>
						<div id="<%=field.getVelocityVarName()%>dt"><%= LanguageUtil.get(pageContext, "Resource-Link") %>:


						  <%
						  StringBuffer resourceLink = new StringBuffer();
						  String resourceLinkUri = "";
						  Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
						  Host host = APILocator.getHostAPI().find((String)request.getAttribute("host") , user, false);
						  if (identifier!=null && InodeUtils.isSet(identifier.getInode())){
						  	if(request.isSecure()){
						  		resourceLink.append("https://");
						  	}else{
						  		resourceLink.append("http://");
						  	}
						  	resourceLink.append(host.getHostname());
						  	if(request.getServerPort() != 80 && request.getServerPort() != 443){
						  		resourceLink.append(":" + request.getServerPort());
						  	}
						  	resourceLinkUri = identifier.getParentPath()+contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
						  	resourceLink.append(UtilMethods.encodeURIComponent(resourceLinkUri));
                            //resourceLinkUri.concat("?language_id="+contentlet.getLanguageId());
                            resourceLinkUri+="?language_id="+contentlet.getLanguageId();
                            resourceLink.append("?language_id="+contentlet.getLanguageId());
						  }

						  com.dotmarketing.portlets.fileassets.business.FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(contentlet);
						  String mimeType = fa.getMimeType();
						  String fileAssetName = fa.getFileName();
						 %>

							<a href="<%=resourceLink %>" target="_new"><%=resourceLinkUri %></a>
								<% if (mimeType.indexOf("officedocument")==-1 && (mimeType.indexOf("text")!=-1 || mimeType.indexOf("javascript")!=-1
                                        || mimeType.indexOf("xml")!=-1 || mimeType.indexOf("php")!=-1) || fileAssetName.endsWith(".vm")) { %>
									<% if (InodeUtils.isSet(binInode) && canUserWriteToContentlet) { %>
											<button iconClass="editIcon" dojoType="dijit.form.Button" onClick="editText($('contentletInode').value)" type="button">
												<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit-text")) %>
											</button>
									<% } %>
							<% } %></div>

					<% } %>
               <% } %>

        <!--  END display -->
        <!-- javascript -->
        <script type="text/javascript">

            function serveFile(doStuff,conInode,velVarNm){

                if(doStuff != ''){
                window.open('/contentAsset/' + doStuff + '/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
                }else{
                window.open('/contentAsset/raw-data/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
                }
            }

            function change<%=field.getFieldContentlet()%>ThumbnailSize(newValue) {
                <%=field.getFieldContentlet()%>ThumbSize = newValue;
                $('<%=field.getFieldContentlet()%>Thumbnail').src =
                    "/contentAsset/image-thumbnail/<%=inode+"/"+field.getVelocityVarName()%>?byInode=true&w=" + newValue + "&rand=" + Math.random();
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
      <input type="hidden" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>Content" value="<%=hiddenTextValue%>" />
      <input type="text" name="name" value="" dojoType="dijit.form.TextBox" id="<%=field.getVelocityVarName()%>" />
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
        String[] pairs = fieldValues.split("\r\n");
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
    <div style="height:20px;vertical-align:middle">
        <input type="radio" dojoType="dijit.form.RadioButton" name="<%=radio%>" id="<%=field.getVelocityVarName() + j %>" value="<%=pairValue%>"<%=field.isReadOnly()?" disabled=\"disabled\" ":"" %><%=checked%>>&nbsp;<label for="<%=field.getVelocityVarName() + j %>"><%=name%></label>
    </div>
<%
        }
%> <%
    //SELECT kind of field rendering
    } else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
 %>
    <select dojoType="dijit.form.FilteringSelect" autocomplete="true" id="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" <%=field.isReadOnly()?"readonly=\"\"":""%>>
    <%
        String[] pairs = fieldValues.split("\r\n");
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
    <option value="<%=pairvalue%>" <%=selected%>><%=name%></option>
    <%
        }
    %>
</select>
<%
    //END of select kind of field

    //MULTISELECT kind of field rendering

    } else if (field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())) {

        String[] pairs = fieldValues.split("\r\n");
 %>
    <select multiple="multiple" size="scrollable"
        name="<%=field.getFieldContentlet()%>MultiSelect"
        id="<%=field.getVelocityVarName()%>MultiSelect"
        onchange="update<%=field.getVelocityVarName()%>MultiSelect()"
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
        <option value="<%=pairvalue%>" <%=selected%>><%=name%></option>
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
                    valuesList += multiselect.options[i].value + ",";
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
        String[] pairs = fieldValues.split("\r\n");
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
 <div style="height:20px;vertical-align:middle">
    <input type="checkbox" dojoType="dijit.form.CheckBox" name="<%=fieldName%>Checkbox" id="<%=fieldName + j%>Checkbox"
        value="<%=pairValue%>" <%=checked%> <%=field.isReadOnly()?"disabled":""%>
        onchange="update<%=field.getVelocityVarName()%>Checkbox()">&nbsp;<label for="<%=fieldName + j%>Checkbox"><%=name%></label>
 </div>
<%
        }
%>
    <input type="hidden" name="<%=fieldName%>" id="<%=field.getVelocityVarName()%>"
        value="<%=value%>">

    <script type="text/javascript">
        function update<%=field.getVelocityVarName()%>Checkbox() {
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
							<div id="previewCats<%=counter%>" class="catPreview" style="margin-top: 10px;  margin-left: 2px; border: 0;  max-width: 600px; border: 0.5px solid #B3B3B3; overflow:hidden; height:1%">
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

	  java.util.Map<String, Object> keyValueMap = null;
	  String JSONValue = UtilMethods.isSet(value)? (String)value:"";
	  //Convert JSON to Table Display {key, value, order}
	  if(UtilMethods.isSet(JSONValue)){
		   keyValueMap =  com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(JSONValue);
	  }
 %>
 <div style="display:<%=field.isReadOnly()?"none":"block"%>">
	  <input type="hidden" class ="<%=field.getVelocityVarName()%>" name="<%=field.getFieldContentlet()%>" id="<%=field.getVelocityVarName()%>" value="" />
	  <input type="text" name="<%=field.getFieldContentlet()%>_key" id="<%=field.getVelocityVarName()%>_key" dojoType='dijit.form.TextBox' style='width:200px' value="" <%=field.isReadOnly()?"disabled":""%> />
	  <%=LanguageUtil.get(pageContext, "Value")%>: <input type="text" name="<%=field.getFieldContentlet()%>_value" id="<%=field.getVelocityVarName()%>_value" dojoType='dijit.form.TextBox' style='width:300px' value="" <%=field.isReadOnly()?"disabled":""%> />
	  <button dojoType="dijit.form.Button" id="<%=field.getFieldContentlet()%>_addbutton" onClick="addKVPair('<%=field.getFieldContentlet()%>', '<%=field.getVelocityVarName()%>');" iconClass="plusIcon" <%=field.isReadOnly()?"disabled":""%> type="button"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add")) %></button>
  </div>
  <div id="mainHolder" style="margin:0 20px 0 0;">

  <%
  String licenseMessage = LanguageUtil.get(pageContext, "Go-Enterprise-To-Access") + "!" ;
  String licenseURL = "http://dotcms.com/buy-now";
  List<Layout> layoutListForLicenseManager=APILocator.getLayoutAPI().findAllLayouts();
  for (Layout layoutForLicenseManager:layoutListForLicenseManager) {
      List<String> portletIdsForLicenseManager=layoutForLicenseManager.getPortletIds();
      if (portletIdsForLicenseManager.contains("9")) {
          licenseURL = "/c/portal/layout?p_l_id=" + layoutForLicenseManager.getId() +"&p_p_id=9&p_p_action=0&tab=licenseTab";
          break;
      }
  }

  if(!field.getVelocityVarName().equals("metaData") || LicenseUtil.getLevel()>199) {  %>
	  <table class="listingTable" id="<%=field.getFieldContentlet()%>_kvtable">
	   <%if(keyValueMap!=null && !keyValueMap.isEmpty()){
	      int k = 0;
		   for(String key : keyValueMap.keySet()){
			   if(key.equals("content")){
			   continue;
			   }
		     String str_style = "";
	         if ((k%2)==0) {
	           str_style = "class=\"dojoDndItem alternate_1\"";
	         }else{
	           str_style = "class=\"dojoDndItem alternate_2\"";
	         }
	         %>
	        <input type="hidden" id="<%=field.getFieldContentlet()+"_"+key+"_k"%>" value="<%= key %>" />
			<input type="hidden" id="<%=field.getFieldContentlet()+"_"+key+"_v"%>" value="<%= keyValueMap.get(key) %>" />
	        <tr id="<%=field.getFieldContentlet()+"_"+key%>" <%=str_style %>>
			    <td style="width:20px">
			    <%if(!field.isReadOnly()){ %>
			       <a href="javascript:deleteKVPair('<%=field.getFieldContentlet()%>','<%=field.getVelocityVarName()%>','<%=UtilMethods.escapeSingleQuotes(key)%>');"><span class="deleteIcon"></span></a>
			     <%} %>
			    </td>
				<td><span><%= key %></span></td>
				<td><span><%= keyValueMap.get(key) %></span></td>
			</tr>
	      <%k++;}
	   }%>
	   </table>
   <%} else  {%>
    <a class="goEnterpriseLink" href="<%=licenseURL%>"><span class="keyIcon"></span><%=licenseMessage%></a>
   <%} %>
   </div>

<%}%>

</div>
	<div class="clear"></div>
</div>
