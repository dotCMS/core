<%@page import="com.dotmarketing.portlets.fileassets.business.IFileAsset"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.beans.Identifier"%>


<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.*"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%@page import="com.dotmarketing.portlets.languagesmanager.business.*"%>

<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>


<%@page import="com.dotmarketing.business.web.WebAPILocator"%>


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
Contentlet content = capi.find(contentletId, user, false);


Language lang = APILocator.getLanguageAPI().getLanguage(((Contentlet) content).getLanguageId()) ;
Structure structure = content.getStructure(); 
List<Field> fields = structure.getFields();

Identifier id = APILocator.getIdentifierAPI().find(content);
String conPath ="";
try{
	Host conHost = APILocator.getHostAPI().find(content.getHost() , user, true);
	
	if(!APILocator.getHostAPI().findSystemHost().getIdentifier().equals(conHost.getIdentifier())){
		conPath = conHost.getHostname();
		if(!APILocator.getFolderAPI().findSystemFolder().getInode().equals(content.getFolder())){
			conPath+=id.getPath();
		}
	}
	
}
catch(Exception e){
	Logger.error(this.getClass(), "unable to find host for contentlet"  + content.getIdentifier());
}

String cssPath = Config.getStringProperty("WYSIWYG_CSS");
String content_css=null;
if(UtilMethods.isSet(cssPath)){
	content_css = "content_css : \"" + cssPath + "\",";
}else{
	content_css = "content_css : \"/css/base.css\",";
}      
%>

<script language="javascript" type="text/javascript" src="/html/js/tinymce/jscripts/tiny_mce/tiny_mce_gzip.js"></script>

<script type="text/javascript">
function setupTinyMce(){
	tinyMCE_GZ.init({
		themes : "advanced",
		plugins : "noneditable",
		languages : '<%= user.getLanguageId().substring(0,2) %>',
		disk_cache : true,
		readonly:true,
		<%=content_css%>   
		
		}, function() {
			tinyMCE.init({
			mode : "textareas",
			editor_deselector : "mceNoEditor",
			theme : "advanced",
			readonly:true,
			plugins : "noneditable",
			languages : '<%= user.getLanguageId().substring(0,2) %>',
			disk_cache : true,
			<%=content_css%>    
		});
	});
}


dojo.ready(function(){
	setupTinyMce();
	
});
</script>





<style>

	.previewCon{
		align:center;
		border:1px solid silver;
	}
	
	.previewCon .tRow{
		margin-bottom:5px;
		border-bottom:1px dotted silver;
	}
	
	
	.previewCon .fColumn{
		font-weight:bold;
		padding-right:10px;
		text-align:right;
		vertical-align: top;
		border-right:1px dotted silver;
	}

	.previewCon td{
		padding:5px;
		padding-left:10px;

	}
	.textAreaDiv{
		max-height: 250px; 
		width: 600px;
		font-size:12px;
		vertical-align: top;
		overflow:auto;
	}


</style>

<div id="contentPreviewDialog">

	<div>
		<table class="previewCon" align="center">
			<%if (fields.size() > 0)  {%>
				<tr class="tRow">
					<td class="fColumn">
						<%= LanguageUtil.get(pageContext, "Title") %>
					</td>
					<td>
						<%= content.getTitle()%>
					</td>
				</tr>
				<tr class="tRow">
					<td class="fColumn">
						<%= LanguageUtil.get(pageContext, "Identifier") %>
					</td>
					<td>
						<%= content.get("identifier")%>
					</td>
				</tr>
				<tr class="tRow">
					<td class="fColumn">
						<%= LanguageUtil.get(pageContext, "Viewing-Language") %>
					</td>
					<td>
						<%= lang.getCountry()%> - <%= lang.getLanguage()%>
					</td>
				</tr>
				<%if(conPath != null) {%>
					<tr class="tRow">
						<td class="fColumn">
							<%= LanguageUtil.get(pageContext, "Host-Folder") %>
						</td>
						<td>
							<%=conPath %>
						</td>
					</tr>
				<%} %>
				<tr class="tRow">
					<td class="fColumn">
						<%= LanguageUtil.get(pageContext, "Status") %>
					</td>
					<td>
						<%if(content.isArchived()){%><%= LanguageUtil.get(pageContext, "Archived") %><%}else if(content.isLive()){%><%=LanguageUtil.get(pageContext, "Live")%><%}else{%><%= LanguageUtil.get(pageContext, "Working1") %><% } %>
					</td>
				</tr>
			<% } 
			for(int i = 0; i < fields.size();i++){
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
				
				
				%>
				<tr class="tRow">
					<td class="fColumn">
							<%=field.getFieldName()%>
					</td>
					<td>
					<%if (field.getFieldType().equals(Field.FieldType.TEXT.toString())){ %>
						
							<%=(UtilMethods.isSet(capi.getFieldValue(content, field))
									? UtilMethods.xmlEscape(String.valueOf(capi.getFieldValue(content, field)))
											: LanguageUtil.get(pageContext, "No")+" " + field.getFieldName() +" "+ LanguageUtil.get(pageContext, "configured"))%>
						
				<% }else if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())){
			        String textValue = String.valueOf(capi.getFieldValue(content, field)) ;
			        textValue = textValue.replaceAll("&", "&amp;");

			        boolean wysiwygPlain = false;
			        for (String fieldVelocityVarName: content.getDisabledWysiwyg()) {

			    		if(fieldVelocityVarName.startsWith(field.getVelocityVarName())){
			    			wysiwygPlain=true;
			    		}
			        }%>
					
					
					
					<%if(!wysiwygPlain){%>
						<textarea style="width:600px;height:400px;"><%=textValue %></textarea>
					<%}else{ %>
						<div class="textAreaDiv">
						<% textValue = textValue.replaceAll("<", "&lt;");%>
						<% textValue = textValue.replaceAll(">", "&gt;");%>
						<% textValue = UtilMethods.htmlLineBreak(textValue);%>
							<%=textValue %>
						</div>
					<%} %>
					
					
					
					
					<% }else if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())){ %>
						
							<div style="max-height: 150px; width: 500px;font-size:12px;vertical-align: top;overflow:auto;">
						    	<%=(UtilMethods.isSet(capi.getFieldValue(content, field))
						    			? UtilMethods.xmlEscape(String.valueOf(capi.getFieldValue(content, field))) 
						    					: LanguageUtil.get(pageContext, "No")+" " +  field.getFieldName() + " "+ LanguageUtil.get(pageContext, "configured"))%>
							</div>
						
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         
			         <% }else if (
			        		 field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
			        		 field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) 
			        		 
			         
			         
			         
			         ){ 
													
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
						}
					%> 
						
						<% }else if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || 
				 											 field.getFieldType().equals(Field.FieldType.TIME.toString()) ||
				 											 field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
												{ %>
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
			                












						<% }else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())){ %>
							
							<%
							String x ="";
							if(UtilMethods.isSet(content.get(field.getVelocityVarName()))){
								x = String.valueOf(content.get(field.getVelocityVarName())) ;
								if(x.indexOf(java.io.File.separator) > -1){
									x=x.substring(x.lastIndexOf(java.io.File.separator)+1, x.length());	%>
									
										<%if(UtilMethods.isImage(x)){%>
											<%=x %>
											<br/>
											<a target="_blank" href="/contentAsset/raw-data/<%=content.getInode() %>/<%=field.getVelocityVarName() %>/?byInode=true">
												<img src="/contentAsset/image/<%=content.getInode() %>/<%=field.getVelocityVarName() %>?byInode=1&filter=Thumbnail&thumbnail_w=150&thumbnail_h=150&" style="border:2px dotted silver"/>
											</a>
										<%}else{ %>
									
									
									
											<a target="_blank" href="/contentAsset/raw-data/<%=content.getInode() %>/<%=field.getVelocityVarName() %>/?byInode=true"><%=x %></a>
										<%} %>
									<%}%>
								
							<%}%>
							
						








						<% }else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())){ %>
							<!-- display -->
							
							<%
								String inode = String.valueOf(capi.getFieldValue(content, field));
								if(InodeUtils.isSet(inode)){
							%>
							<img id="<%=field.getFieldContentlet()%>Thumbnail" src="/thumbnail?id=<%=inode %>" width="100" height="100" border="1">
							<%  }else{ %><%=LanguageUtil.get(pageContext, "No-Image-configured")  %><%} %>
							
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 
						 <% } else if (field.getFieldType().equals(Field.FieldType.RADIO.toString()) ||
								 field.getFieldType().equals(Field.FieldType.SELECT.toString())
						 
						 ) { 
							%>
						
							<%					
								Object originalValue = String.valueOf(capi.getFieldValue(content, field));
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
										if ((UtilMethods.isSet(originalValue) && value.equals(originalValue)) ||
																		(UtilMethods.isSet(defaultValue) && defaultValue.equals(value))){%>
											<%=name%><br>
										<%}%> 
									<%}%> 
								<%}%> 
																							
						
	
							
							
							
							
							
							
							
							
							
							
			         
			         
			              <% } else if (field.getFieldType().equals(Field.FieldType.TAG.toString())){ %>

									<%=(UtilMethods.isSet(capi.getFieldValue(content, field))?String.valueOf(capi.getFieldValue(content, field)):"")%>
								
						
						
						
						
								

			         
			         
			         
			         
			         
			         
			         
			         
			              <% } else if (field.getFieldType().equals(Field.FieldType.TAG.toString())){ %>

									<%=(UtilMethods.isSet(capi.getFieldValue(content, field))?String.valueOf(capi.getFieldValue(content, field)):"")%>
								
						
						
						
						
						
						<%}else if (field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){ 
						
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
													: keyValueMap.get(x)
													
													
													%></td>
											</tr>
										
										<%} %>
									</table>
								</div>
						
						<%} %>
						
						
						
						<%}else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){ %>
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
	               		<% }else if(field.getFieldType().equals(Field.FieldType.FILE.toString())){%>
	               		    <!-- display -->
							
							<%
								String inode = String.valueOf(content.get(field.getVelocityVarName())) ;
								if(InodeUtils.isSet(inode)){
								    IFileAsset file=null;
								    Identifier identifier=APILocator.getIdentifierAPI().find(inode);
								    if(identifier.getAssetType().equals("file_asset"))
								        file = APILocator.getFileAPI().getWorkingFileById(inode,APILocator.getUserAPI().getSystemUser(), false);
								    else {
								        Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(
								                inode, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), 
								                APILocator.getUserAPI().getSystemUser(), false);
								        file = APILocator.getFileAssetAPI().fromContentlet(cont);
								    }
									if(file!=null){							
							%>
							<a target="_blank" href="<%=file.getURI()%>"><%=file.getFileName() %></a>
							<%  }
							     }%>
	               		
	               		<%} %>
	                </td>
               </tr>
	        <%} %>
		</table>
	</div>

</div>
