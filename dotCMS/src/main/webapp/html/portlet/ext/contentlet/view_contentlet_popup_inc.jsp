<%@page import="com.dotcms.uuid.shorty.ShortyIdAPI"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.IFileAsset"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.*"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%
String contentletId = (String) request.getParameter("contentletId");
if(contentletId == null){
	 contentletId = (String) request.getAttribute("contentletId");
}

if(contentletId == null){
	out.println(LanguageUtil.get(pageContext, "the-selected-content-cannot-be-found"));
	return;
}
ContentletAPI capi = APILocator.getContentletAPI();

Contentlet content=null;
String vinode = "";
String videntifier = "";
Language lang=null;
Structure structure=null;
List<Field> fields=null;
Identifier id=null;
String conPath ="";
boolean hasPermissions=true;
try {
	content = capi.find(contentletId, user, false);
	if(content == null){
		out.println(LanguageUtil.get(pageContext, "the-selected-content-cannot-be-found"));
		return;
	}
	
	lang = APILocator.getLanguageAPI().getLanguage(((Contentlet) content).getLanguageId()) ;
	structure = content.getStructure(); 
	fields = structure.getFields();
	
	id = APILocator.getIdentifierAPI().find(content);

    vinode = content.getInode();
    videntifier = content.getIdentifier();

}
catch(DotSecurityException dse) {
    hasPermissions=false;
}
ShortyIdAPI shorty=  APILocator.getShortyAPI();



// get URLMap Preview links (this can be on one host or many hosts)
boolean isUrlMap = (structure.getDetailPage() !=null && structure.getUrlMapPattern()!=null);


String editPath = isUrlMap ?  capi.getUrlMapForContentlet(content, user, false) : id.getPath();






if(!hasPermissions) {
    %>
<div style="padding:20px;text-align: center">
    <%=LanguageUtil.get(pageContext, "you-do-not-have-the-required-permissions") %> 
</div>
<%}else {
    
	
	try{
		Host conHost = APILocator.getHostAPI().find(content.getHost() , user, true);
		
		if(!APILocator.getHostAPI().findSystemHost().getIdentifier().equals(conHost.getIdentifier())){
			conPath = "//" + conHost.getHostname();
			if(!APILocator.getFolderAPI().findSystemFolder().getInode().equals(content.getFolder())){
				conPath+=id.getPath();
			}
		}
		
	}
	catch(Exception e){
		Logger.warn(this.getClass(), "unable to find host for contentlet"  + content.getIdentifier());
	}



	String content_css = "content_css : \"" + Config.getStringProperty("WYSIWYG_CSS", "/html/css/tiny_mce.css") + "\",";
%>

<script type="text/javascript" src="/html/js/tinymce/js/tinymce/tinymce.min.js"></script>


<script type="text/javascript">

	tinymce.init({
	    selector: "textarea",
	    toolbar: "mybutton",
	    toolbar: false,
	    menubar: false,
	    statusbar:false,
	    plugins: "autoresize",
	    autoresize_max_height: 500,
	    autoresize_min_height: 50,
	    autoresize_bottom_margin: 50,
	    preview_styles:false,
	    setup: function(editor) {
	        editor.addMenuItem('myitem', {
	            text: 'My menu item',
	            context: 'tools',
	            onclick: function() {
	                editor.insertContent('Some content');
	            }
	        });
	    }
	});
	

	function dotPreviewPage(){

	    var editPath="/api/v1/page/render<%= editPath%>";
	    if(editPath.indexOf("?")<0){
	        editPath+="?"
	    }
        if(editPath.indexOf("language_id")<0){
            editPath+="&language_id=<%=content.getLanguageId()%>";
        }
        if(editPath.indexOf("host_id")<0){
            editPath+="&host_id=<%=id.getHostId()%>";
        }
        if(editPath.indexOf("mode")<0){
            editPath+="&mode=preview";
        }
        console.log(editPath);
	    var url = editPath;

	    console.log("url:" + url);
        var req = new XMLHttpRequest();
        req.open("GET", url, true);
        req.onreadystatechange = function() {
            if (req.readyState === 4 && req.status == "200") {
       
                dotPreviewPageCallBack(JSON.parse(req.responseText));
            }
            else if(req.status != "200"){
                alert("Error:" + req.status)
            }
        }
        req.send();

	}
	
	
	var win;
	function dotPreviewPageCallBack(data){

	    win = window.open("", "dotCMS Preview", "toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=1024,height=800");

	    var parser = new DOMParser();
	    var doc= win.document;

	    doc.open();
	    win.document.write(data.entity.page.rendered);
	    doc.close();

	}
	
	   function dotPreviewFile(url){
	       
	        win = window.open(url, "dotCMS Preview", "toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=1024,height=800");

	   }

    function emmitCompareEvent(inode, identifier, language) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "compare-contentlet",
            data: { inode, identifier, language }
        });
        document.dispatchEvent(customEvent)
    }

</script>





<style>
	.textAreaDiv{
		max-height: 250px; 
		width: 100%;
		font-size:12px;
		vertical-align: top;
		overflow:auto;
		border:0px;
	}
</style>


<table class="listingTable" style="position:relative;left:0px;right:0px;">
	<%if (fields.size() > 0)  {%>
		<tr>
			<th>
				<%= LanguageUtil.get(pageContext, "Title") %>
			</th>
			<td style="display: flex; align-items: center; justify-content: space-between;">
				<%= content.getTitle()%>
                <button dojoType="dijit.form.Button" type="button" 
                    onclick="emmitCompareEvent('<%= vinode %>', '<%= videntifier %>', '<%=lang.getLanguageCode() %>-<%=lang.getCountryCode().toLowerCase()%>');return false;" >
                    <%= LanguageUtil.get(pageContext, "compare.to.previous.versions") %>
                </button>

			</td>
		</tr>
        <tr>
            <th>
                <%= LanguageUtil.get(pageContext, "Identifier") %>
            </th>
            <td>
                <div><%= content.get("identifier")%><div>
            </td>
        </tr>
        <%if(structure.getStructureType()==Structure.STRUCTURE_TYPE_HTMLPAGE || isUrlMap || structure.getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET ){ %>
           <tr>
               <th>
                   <%= LanguageUtil.get(pageContext, "url") %>
               </th>
               <td>
                   <%if(structure.getStructureType()==Structure.STRUCTURE_TYPE_HTMLPAGE || isUrlMap  ){ %>
                       <%session.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, content.getHost()); %>
                       <div style="padding:3px;"><a style="color:#0E80CB;" target="workflowWindow" text-decoration: underline;" href="/dotAdmin/#/edit-page/content?url=<%= editPath+ "&language_id=" + lang.getId()%>"><%= editPath%></a></div>
                   <%}else  if(structure.getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET ){ %>
                       <div style="padding:3px;"><a style="color:#0E80CB; text-decoration: underline;" href="#" onclick="dotPreviewFile('/dA/<%=shorty.shortify(content.getInode()) %>/fileAsset/<%=id.getAssetName() %>?mode=PREVIEW_MODE')" >/dA/<%=shorty.shortify(content.getInode()) %>/fileAsset/<%=id.getAssetName() %>?mode=PREVIEW_MODE</a></div>
                   <%} %>
               </td>
           </tr>
        <%} %>

		<tr>
			<th class="fColumn">
				<%= LanguageUtil.get(pageContext, "Language") %>
			</th>
			<td>
				<%= lang.getCountry()%> - <%= lang.getLanguage()%>
			</td>
		</tr>
		<%if(UtilMethods.isSet(conPath)) {%>
			<tr>
				<th class="fColumn">
					<%= LanguageUtil.get(pageContext, "Host-Folder") %>
				</th>
				<td>
					<%=conPath %>
				</td>
			</tr>
		<%} %>

	<% }%>
	

	
	
	
	
	
	
	<% for(int i = 0; i < fields.size();i++){
		Field field = (Field) fields.get(i);
		
		
		/**************************
		*
		*  Ignore these field types
		*
		***************************/
		if(
				
			Field.FieldType.HOST_OR_FOLDER.toString().equals(field.getFieldType()) ||
			Field.FieldType.TAB_DIVIDER.toString().equals(field.getFieldType()) ||
			Field.FieldType.PERMISSIONS_TAB.toString().equals(field.getFieldType()) ||
			Field.FieldType.BUTTON.toString().equals(field.getFieldType()) ||
			Field.FieldType.HIDDEN.toString().equals(field.getFieldType()) ||
			Field.FieldType.CONSTANT.toString().equals(field.getFieldType()) ||
			Field.FieldType.CUSTOM_FIELD.toString().equals(field.getFieldType()) ||
			Field.FieldType.RELATIONSHIPS_TAB.toString().equals(field.getFieldType())
		){continue;}
		
		if(field.getFieldName().equals(LanguageUtil.get(pageContext, "Title"))){
				continue;	
		}
		
		if(content ==null 
				|| !UtilMethods.isSet( content.get(field.getVelocityVarName()))
				|| capi.getFieldValue(content, field) ==null 
				|| !UtilMethods.isSet(capi.getFieldValue(content, field).toString().trim())
						
						
				
				
				){
			continue;	
		}%>
		
		<tr>
			<th valign="top" style="vertical-align: top">
				<%=field.getFieldName()%>
			</th>
			<td>
			
			
			
			
				<%--   Text Box --%>
				<%if (field.getFieldType().equals(Field.FieldType.TEXT.toString())){ %>
						<%=(UtilMethods.isSet(capi.getFieldValue(content, field))
								? UtilMethods.xmlEscape(String.valueOf(capi.getFieldValue(content, field)))
										: LanguageUtil.get(pageContext, "No")+" " + field.getFieldName() +" "+ LanguageUtil.get(pageContext, "configured"))%>
					
				<%}%>
			
			
			
			
			
			
				
				<%--   WYSIWYG --%>
				<%if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())){
			        String textValue = String.valueOf(capi.getFieldValue(content, field)) ;
			        textValue = textValue.replaceAll("&", "&amp;");
			        boolean wysiwygPlain = false;
			        for (String fieldVelocityVarName: content.getDisabledWysiwyg()) {
			    		if(fieldVelocityVarName.startsWith(field.getVelocityVarName())){
			    			wysiwygPlain=true;
			    		}
			        }
					if(!wysiwygPlain){%>
						<%if (textValue.contains("<a href=")){
			        		textValue = textValue.replaceAll("<a href=", "<a target='_blank' href=");
			        	}%>
						<textarea style="width:100%;border:0px;"><%=textValue %></textarea>
					<%}else{ %>
						<div class="textAreaDiv">
						<% textValue = textValue.replaceAll("<", "&lt;");%>
						<% textValue = textValue.replaceAll(">", "&gt;");%>
						<% textValue = UtilMethods.htmlLineBreak(textValue);%>
							<%=textValue %>
						</div>
					<%} %>
				<%}%>
				
				
				
				
				
			
			
				
				
				
				
				<%--   TextArea --%>
				<% if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())){ %>

					<div style="max-height: 150px; width: 500px;font-size:12px;vertical-align: top;overflow:auto;">
				    	<%=(UtilMethods.isSet(capi.getFieldValue(content, field))
				    			? UtilMethods.xmlEscape(String.valueOf(capi.getFieldValue(content, field))) 
				    					: LanguageUtil.get(pageContext, "No")+" " +  field.getFieldName() + " "+ LanguageUtil.get(pageContext, "configured"))%>
					</div>
		        <% }%>
		         
		         
		         
		         
		         
	         
		         
		        <%--   Checkbox/Multiselect --%>
		        <% if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) || field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ){ 
												
		               String originalValue = String.valueOf(capi.getFieldValue(content, field));
		               String fieldName = field.getFieldContentlet();
		               String defaultValue = field.getDefaultValue();
		               if (defaultValue != null)
		                 	defaultValue = defaultValue.trim();
		               else 
		               	defaultValue = "";
		               
		               String values = field.getValues();
		               if (values != null)
		               	values = values.trim();
		               else 
		               	values = "";
		               String[] pairs = values.split("\r\n");
		               %>
						
		               <%
		               for(int j = 0;j < pairs.length;j++) {
		                String pair = pairs[j];
		                String[] tokens = pair.split("\\|");
		                if (0 < tokens.length) {
			                String name = tokens[0];
							String value = (tokens.length > 1 ? tokens[1] : name);                                  

			                if (UtilMethods.isSet(originalValue)){
			                	if (originalValue.contains(value + ",")){%>
			                		<%=name%><br>
								<%}
			                } else{
			                  if (UtilMethods.isSet(defaultValue) && (defaultValue.contains("|" + value) || defaultValue.contains(value + "|") || defaultValue.equals(value))){%>
			                	  <%=name%><br>
			                  <%}
			                }

						}
					}%> 
				<%}%> 
			
			
			
				
				
			 	<%--   Date/DateTime/Time --%>
				<% if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.TIME.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){ %>
					<!-- DISPLAY DATE-->
					<%  java.util.Date startDate = new Date();
						
						try
						{	
							Object oDate = capi.getFieldValue(content, field);
				            if (oDate instanceof Date) {
				            	startDate = (Date)oDate;
				            } else {
				                String sDate = oDate.toString();
				                SimpleDateFormat dateFormatter = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.LONGDBDATE);
				                try {
				                	startDate = dateFormatter.parse(sDate);
				                } catch (Exception e) { }
				                dateFormatter = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DBDATE);
				                try {
				                	startDate= dateFormatter.parse(sDate);
				                } catch (Exception e) { }
				                dateFormatter = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.SHORTDATE);
				                try {
				                	startDate= dateFormatter.parse(sDate);
				                } catch (Exception e) { }
				            }
						}
						catch(Exception ex)
						{			
							startDate = new Date();
						}
						
						if (field.getFieldType().equals(Field.FieldType.DATE.toString())){%>
							<%=UtilMethods.dateToHTMLDate(startDate) %>
						<%} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {%>
							<%=UtilMethods.dateToLongPrettyHTMLDate(startDate) %>
						<%} else if (field.getFieldType().equals(Field.FieldType.TIME.toString()) ) {%>
							<%=UtilMethods.dateToHTMLTime(startDate) %>
					<%} %>				
				<%}%>
				
				
				
					
				
				<%--   BINARY FILE --%>
				<% if (field.getFieldType().equals(Field.FieldType.BINARY.toString())){ %>
					
					<%
					String x ="";
					if(UtilMethods.isSet(content.get(field.getVelocityVarName()))){
						x = String.valueOf(content.get(field.getVelocityVarName())) ;
						if(x.indexOf(java.io.File.separator) > -1){
							x=x.substring(x.lastIndexOf(java.io.File.separator)+1, x.length());	%>
							<%if(UtilMethods.isImage(x)){%>
								<%=x %>
								<br/>
								<a  href="#" onclick="dotPreviewFile('/dA/<%=content.getInode() %>/<%=field.getVelocityVarName() %>/<%=content.getBinary(field.getVelocityVarName()).getName()%>')">
									<img src="/dA/<%=content.getInode() %>/<%=field.getVelocityVarName() %>/300w/20q" style="border:1px dotted silver;max-width:400px"/>
								</a>
							<%}else{ %>
								<a href="#" onclick="dotPreviewFile('/dA/<%=content.getInode() %>/<%=field.getVelocityVarName() %>/150w/20q')"><%=x %></a>
							<%} %>
						<%}%>
					<%}%>
				<%} %>
					







				<%--   Associated IMAGE --%>
				<% if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())){ %>
					<!-- display -->
					<% String inode = String.valueOf(capi.getFieldValue(content, field));
					if(InodeUtils.isSet(inode)){%>
						<img id="<%=field.getFieldContentlet()%>Thumbnail" src="/contentAsset/image/<%= inode %>/fileAsset/filter/Thumbnail/thumbnail_w/100/thumbnail_h/100" border="1">
					<% }else{ %>
						<%=LanguageUtil.get(pageContext, "No-Image-configured")  %>
					<%} %>
				<%} %>
				 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
					 
				 
				 <%--   Radio / Select IMAGE --%>
				 <% if (field.getFieldType().equals(Field.FieldType.RADIO.toString()) ||field.getFieldType().equals(Field.FieldType.SELECT.toString())) { 
					Object originalValue = capi.getFieldValue(content, field);
					String defaultValue = field.getDefaultValue();
					String radio = field.getFieldContentlet();
					String values = field.getValues();
					if (values != null)
                       values = values.trim();
                    else 
                       values = "";
                    String[] pairs = values.split("\r\n");      
					for(int j = 0;j < pairs.length;j++){
						String pair = pairs[j];
						String[] tokens = pair.split("\\|");
						if (0 < tokens.length) {
							String name = tokens[0];
							Object value = (tokens.length > 1 ? tokens[1] : name);
							if(originalValue instanceof Boolean)
								value = Parameter.getBooleanFromString((String) value);
							else if (originalValue instanceof Long) 
								value = Parameter.getLong((String) value);
							else if (originalValue instanceof Double) 
								value = Parameter.getDouble((String) value);
							else if (originalValue instanceof Float) 
								value = Parameter.getFloat((String) value);
							if ((UtilMethods.isSet(originalValue) && value.equals(originalValue)) || (UtilMethods.isSet(defaultValue) && defaultValue.equals(value))){%>
								<%=name%><br>
							<%}%> 
						<%}%> 
					<%}%> 
				<%}%>																
			

						
						
						
						
						
						
					
					
					
					
	         
	         	<%--   TAGS  --%>
	            <% if (field.getFieldType().equals(Field.FieldType.TAG.toString())){ %>
					<%=(UtilMethods.isSet(capi.getFieldValue(content, field))?String.valueOf(capi.getFieldValue(content, field)):"")%>
				<%} %>		






				
				<%--   KEY / VALUE / METADATA  --%>
				<% if (field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){ 
					java.util.Map<String, Object> keyValueMap =  content.getKeyValueProperty(field.getVelocityVarName());
					if(keyValueMap!=null && keyValueMap.size() > 0){ %>
						<div class="textAreaDiv">
							<table class="listingTable">
								<tr>
									<th><%=LanguageUtil.get(pageContext, "Key") %></th>
									<th><%=LanguageUtil.get(pageContext, "Value") %></th>
								</tr>
								<%for(String x : keyValueMap.keySet()){ %>
									<tr>
										<td><%=x %></td>
										<td><%=("content".equals(x)) 
											? "<em>" + LanguageUtil.get(pageContext, "indexed-content")  + "</em>"
											: keyValueMap.get(x) %></td>
									</tr>
								<%} %>
							</table>
						</div>
					<%} %>
				<%} %>
				
			
			
			
			
				
				
				<%--   CATEGORIES  --%>
				<%if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){ %>
			       	<%
			       	CategoryAPI categoryAPI = APILocator.getCategoryAPI();
			        Set<com.dotmarketing.portlets.categories.model.Category> selectedCats = (Set<com.dotmarketing.portlets.categories.model.Category>) capi.getFieldValue(content, field) ;
					com.dotmarketing.portlets.categories.model.Category category = categoryAPI.find(field.getValues(), APILocator.getUserAPI().getSystemUser(), false);
					java.util.List<com.dotmarketing.portlets.categories.model.Category> children = categoryAPI.getChildren(category, user, false);
					for(com.dotmarketing.portlets.categories.model.Category child : children){%>		
						<%for(com.dotmarketing.portlets.categories.model.Category sel : selectedCats){%>		
							<%if(child.getInode().equals(sel.getInode())){ %>
								<%=child.getCategoryName() %> <br>
							<%} %>
						<%} %>
					<%} %>
				<%} %>	
					
				
				
				
					
					
					
				<%--   ASSOCIATED FILES  --%>	
              		<% if(field.getFieldType().equals(Field.FieldType.FILE.toString())){
              		    String inode = String.valueOf(content.get(field.getVelocityVarName())) ;
					if(InodeUtils.isSet(inode)){
					    IFileAsset file;
						Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier( inode, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
						file = APILocator.getFileAssetAPI().fromContentlet(cont);

						if(file!=null){	%>
							<a target="_blank" href="<%=file.getURI()%>"><%=file.getFileName() %></a>
						<%}%>
		  			<%}%>
				<%} %>
				
				
				
				
				
				
             		</td>
          	</tr>
      	<%} %>
</table>
<% } %>
