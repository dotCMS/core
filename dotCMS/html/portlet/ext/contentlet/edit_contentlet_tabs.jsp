<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>

<%

	List<Category> entityCategories = (List<Category>) request.getAttribute("entityCategories");
	List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = 
		(List<ContentletRelationships.ContentletRelationshipRecords>) request.getAttribute("relationshipRecords");	
	List<Map<String, Object>> references = 
		(List<Map<String, Object>>) request.getAttribute("references");	
	Contentlet contentlet =	(Contentlet) request.getAttribute("contentlet");	
	List<Field> fields = (List <Field>)request.getAttribute("fields");
	boolean categoriesTabFieldExists  = false;
	boolean permissionsTabFieldExists  = false;
	boolean relationshipsTabFieldExists = false;
	
	   if(fields != null || fields.size() > 0){			
	        for(Field field : fields){
	        	
	      	  if(field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()) 
                      || field.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString())
                      || field.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())){
	      		  
	      		 if(field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())){	
	     	    	categoriesTabFieldExists = true;
	      		 }else if(field.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString())){
	    	    	 permissionsTabFieldExists = true;
	      		 }else{
	    	    	 relationshipsTabFieldExists =  true; 
	      		 }
	      		  
	      	  }
	        }
	     }
	
	   PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	   boolean canEditAsset = conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
%>

<table id="tabs_table_1" class="beta" border="0" width="95%" cellpadding="0" cellspacing="0" >
	<tr>
		<td>
			<table id="tabs_table_2" border="0" cellpadding="0" cellspacing="0" class="portletMenu" width="100%" style="margin-bottom:6px;">
				<tr style="height:25px;">
				
		         	<td id="tabs_td" align="left">
					<% 
					
						if(fields != null && fields.size() > 0 && fields.get(0) != null && fields.get(0).getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
							Field f0 = fields.get(0);
					%>
				            <a class="alpha" href="javascript:displayProperties('<%=f0.getVelocityVarName()%>')" id="<%=f0.getVelocityVarName()%>_tab">
							 <%=f0.getFieldName()%>
						    </a>
							<script>
							    addTab('<%=f0.getVelocityVarName()%>');
							</script>
					<%
						} else {
					%>
						  	<a class="alpha" href="javascript:displayProperties('properties')" id="properties_tab">
								<%= LanguageUtil.get(pageContext, "Content") %>
							</a>
							<script>
							    addTab('properties');
							</script>
					<%	
						}
					%>
					
						
					<%
					   if(fields != null || fields.size() > 0){					
					        for(Field field : fields){
					            if(field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()) && field.getSortOrder() > 1){
					            	
					%>	
					            <a class="beta" href="javascript:displayProperties('<%=field.getVelocityVarName()%>')" id="<%=field.getVelocityVarName()%>_tab">
								 <%=field.getFieldName()%>
							    </a>
								<script>
								    addTab('<%=field.getVelocityVarName()%>');
								</script>
					<%
						  }
					    }
					  }
					            
					%> 
					
					
						
					<%
						if(entityCategories !=null &&  entityCategories.size() >0 && !categoriesTabFieldExists ){
					%>
							<a class="beta" href="javascript:displayProperties('categoriesTab')" id="categoriesTab_tab">
								<%= LanguageUtil.get(pageContext, "Categories") %>
							</a>
							<script>
							    addTab('categoriesTab');
							</script>
					<%
						}
					%> 
						
					<%
						if (relationshipRecords != null && relationshipRecords.size() > 0 && !relationshipsTabFieldExists) {
					%>
							<a class="beta" href="javascript:displayProperties('relationships')" id="relationships_tab">
								<%= LanguageUtil.get(pageContext, "Relationships") %>
							</a>
							<script>
							    addTab('relationships');
							</script>
					<%
						}
					%> 
					<%
						if (!permissionsTabFieldExists && canEditAsset) {
					%>	
							<a class="beta" href="javascript:displayProperties('permissions')" 	id="permissions_tab">
								<%= LanguageUtil.get(pageContext, "Permissions") %>
							</a>
							<script>
							    addTab('permissions');
							</script>
					<%
						}
					%> 	
						<%if(InodeUtils.isSet(contentlet.getInode())){ %>
							<a class="beta" href="javascript:displayProperties('versions')" id="versions_tab">
								<%= LanguageUtil.get(pageContext, "Versions") %>
							</a>
							<script>
							    addTab('versions');
							</script>
							
							<%if(references != null && references.size() > 0){ %>
							    <a class="beta" href="javascript:displayProperties('references')" id="references_tab">
									<%= LanguageUtil.get(pageContext, "References") %>
								</a>
								<script>
								    addTab('references');
								</script>
							<%}%>
						<%}%>
					</td>
				</tr>
				<tr class="blue_Border" >
					<td><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
			</table>
		</td>
	</tr>
</table>
