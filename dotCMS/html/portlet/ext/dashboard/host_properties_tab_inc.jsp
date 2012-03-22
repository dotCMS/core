<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.cms.factories.*"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="org.apache.commons.beanutils.PropertyUtils"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>

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

<div id="divContent<%=content.getInode()%>_pop_up"  style="text-align: left;">
	<div class="x-dlg-bd" style="background-color: white" align="center">
		<dl>
		<%
			Structure structure = content.getStructure(); 
			List<Field> fields = structure.getFields();
			int[] popupmonthIds = CalendarUtil.getMonthIds();
			String[] popupmonths = CalendarUtil.getMonths(Locale.getDefault());
			String[] popupDays = CalendarUtil.getDays(Locale.getDefault());
			
			if (fields.size() > 0)  {
		%>
			<!-- LANGUAGE -->
			<dt>
			  <B><%= LanguageUtil.get(pageContext, "Viewing-Language") %>:&nbsp;</B>
			<dd>

			<%
							Language lang = APILocator.getLanguageAPI().getLanguage(((Contentlet) content).getLanguageId()) ;
						%>
			<%= lang.getCountry()%> - <%= lang.getLanguage()%>
			</dd>
			</dt>
			<!-- END LANGUAGE -->
			<!--  HOST -->
			<dt>
			  <B><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</B>
			   <dd>
				<%=WebAPILocator.getHostWebAPI().getCurrentHost(request).getHostname()%>
			  </dd>
			</dt>
			<dt>
			   <B><%= LanguageUtil.get(pageContext, "Status") %>:&nbsp;</B>
			   <dd>
				<%if(content.isArchived()){%><%= LanguageUtil.get(pageContext, "Archived") %><%}else if(content.isLive()){%><%=LanguageUtil.get(pageContext, "Live")%><%}else{%><%= LanguageUtil.get(pageContext, "Working1") %><% } %>
			  </dd>
			</dt>
			<!--  END HOST -->
		
		<% } 
			for(int i = 0; i < fields.size();i++){
				Field field = (Field) fields.get(i);
				if(APILocator.getContentletAPI().getFieldValue(content, field)!=null){
				 if (field.getFieldType().equals(Field.FieldType.TEXT.toString())){ %>
					<dt>
						<b>
							<%=field.getFieldName()%>:
						</b>
						 <dd>
						<%=(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(content, field))
								? UtilMethods.xmlEscape(String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field)))
										: LanguageUtil.get(pageContext, "No")+" " + field.getFieldName() +" "+ LanguageUtil.get(pageContext, "configured"))%>
					  </dd>					
					</dt>
				<% }else if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || 
						field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())){ %>
						<dt><B>
								<%=field.getFieldName()+":&nbsp;</B> "%>
							 <dd>	
								<div style="border:1px solid gray;height: 150px; width: 400px;font-size:12px;vertical-align: top;overflow:auto;">
							    	<%=(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(content, field))
							    			? UtilMethods.xmlEscape(String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field))) 
							    					: LanguageUtil.get(pageContext, "No")+" " +  field.getFieldName() + " "+ LanguageUtil.get(pageContext, "configured"))%>
								</div>
							 </dd>	
						</dt>
		         <% }else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())){ 							
		               String originalValue = String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field));
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
		               <dt><b><%=field.getFieldName() %>:&nbsp;</B>
		               <dd>
		               <div>
		               <%
		               for(int j = 0;j < pairs.length;j++) {
		                String pair = pairs[j];
		                String[] tokens = pair.split("\\|");
		                if (0 < tokens.length) {
			                String name = tokens[0];
							String value = (tokens.length > 1 ? tokens[1] : name);                                  
			                String checked = "";
			                if (UtilMethods.isSet(originalValue)){
			                	if (originalValue.contains(value + ",")){
									checked = "CHECKED";
								}
			                } else{
			                  if (UtilMethods.isSet(defaultValue) && (defaultValue.contains("|" + value) || defaultValue.contains(value + "|") || defaultValue.equals(value))){
			                  	checked = "CHECKED";
			                  }
			                }
		               %> 
		               <input disabled type="checkbox" name="<%=fieldName%>CheckBox" id="<%=fieldName%>CheckBox" value="<%=value%>" <%=checked%>> <%=name%> <BR>
					<%
							}
						}
					%> 
					  
					 </div>
					  </dd>
					</dt>
					<% }else if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || 
			 											 field.getFieldType().equals(Field.FieldType.TIME.toString()) ||
			 											 field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
											{ %>
					<dt><B><%=field.getFieldName()+":&nbsp;</B> " %>				<!-- DISPLAY DATE-->
						 <dd><%  java.util.Date startDate = new Date();
							
							try
							{	
								Object oDate = APILocator.getContentletAPI().getFieldValue(content, field);
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
							
							Calendar startDateCal = new GregorianCalendar();
							startDateCal.setTime(startDate);
							if (field.getFieldType().equals(Field.FieldType.DATE.toString()) || 
			 										  field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) 
							{%>
							<select name="<%=field.getFieldContentlet()%><%=LanguageUtil.get(pageContext, "Month")%>" disabled="disabled" onChange="updateDate('<%=field.getFieldContentlet()%>');">
								<%String sdMonth = Integer.toString(startDateCal.get(Calendar.MONTH));
								  for (int j = 0; j < popupmonths.length; j++) {
								%>
									<option <%= (sdMonth.equals(Integer.toString(popupmonthIds[j]))) ? LanguageUtil.get(pageContext, "selected") : "" %> value="<%= popupmonthIds[j] %>"><%= popupmonths[j] %></option>
								<%}%>
							</select>
							<select name="<%=field.getFieldContentlet()%><%=LanguageUtil.get(pageContext, "Day")%>" disabled="disabled" onChange="updateDate('<%=field.getFieldContentlet()%>');">
								<%String sdDay = Integer.toString(startDateCal.get(Calendar.DATE));
									for (int j = 1; j <= 31; j++) {
								%>
										<option <%= (sdDay.equals(Integer.toString(j))) ? LanguageUtil.get(pageContext, "selected") : "" %>value="<%= j %>"><%= j %></option>
									<%}%>
								</select>
								<select name="<%=field.getFieldContentlet()%><%=LanguageUtil.get(pageContext, "Year")%>" disabled="disabled" onChange="updateDate('<%=field.getFieldContentlet()%>');">
									<%  int popupCurrentYear = startDateCal.get(Calendar.YEAR);
										String sdYear = Integer.toString(startDateCal.get(Calendar.YEAR));
										int popupPrevious = 100;
										for (int j = popupCurrentYear - popupPrevious; j <= popupCurrentYear + 10; j++) {
									%>
										<option <%= (sdYear.equals(Integer.toString(j))) ? LanguageUtil.get(pageContext, "selected") : "" %> value="<%= j %>"><%= j %></option>
									 <%}%>
								</select>
								<% }%>
								
							<!-- DISPLAY TIME -->
						<% if (field.getFieldType().equals(Field.FieldType.TIME.toString()) || 
			 							  field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) 
							{%>
								<select name="<%=field.getFieldContentlet()%>Hour" disabled="disabled"	onChange="amPm('<%=field.getFieldContentlet()%>');updateDate('<%=field.getFieldContentlet()%>');">
								<%
									int sdHour = startDateCal.get(Calendar.HOUR_OF_DAY);
									for (int j = 0; j < 24; j++) {
										int val = j > 12 ?  j - 12: j;
										if(val == 0) val = 12;
								%>
											<option <%= (sdHour == j) ? LanguageUtil.get(pageContext, "selected") : "" %>	value="<%= j %>"><%= val %></option>
									<%}%>
								</select> :<select name="<%=field.getFieldContentlet()%>Minute" disabled="disabled" onChange="updateDate('<%=field.getFieldContentlet()%>');">
								<%
									int currentMin  = startDateCal.get(Calendar.MINUTE);
									boolean selected = false;
									for (int j = 0; j < 60; j= (j+5)) {
										String val = (j< 10) ? "0" + j: String.valueOf(j);
								%>
									<option <%= (j >= currentMin && ! selected) ? "selected" : "" %> value="<%= val %>"><%= val %></option>
								<%		if(j >= currentMin) selected = true;
									}%>
								</select>&nbsp;<span id="<%=field.getFieldContentlet()%>PM"><%=(sdHour > 11) ? LanguageUtil.get(pageContext, "PM") : LanguageUtil.get(pageContext, "AM")%></span>
						<%} %>
						 </dd>				
		                </dt>
					<% }else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())){ %>
						<!-- display -->
						<dt><B><%=field.getFieldName()+":&nbsp;</B> " %>
						 <dd>
						<%
							String inode = String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field));
							if(InodeUtils.isSet(inode)){
						%>
						<img id="<%=field.getFieldContentlet()%>Thumbnail" src="/thumbnail?inode=<%=inode %>" width="100" height="100" border="1">
						<%  }else{ %><%=LanguageUtil.get(pageContext, "No-Image-configured")  %><%} %>
						</dd>
						</dt>
					 <% } else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) { 
						%>
						<dt><B><%=field.getFieldName()+":&nbsp;</B>" %>
						 <dd>
						<%					
							Object originalValue = String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field));
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
									
									String checked = "";
									if ((UtilMethods.isSet(originalValue) && value.equals(originalValue)) ||
																	(UtilMethods.isSet(defaultValue) && defaultValue.equals(value)))
									{
										checked = "CHECKED";
									}
						%> 
							<input disabled type="radio" name="<%=radio+content.getInode()%>" value="<%=value%>" <%=checked%>> <%=name%> <BR>
						<%
								}
							}
						%> 
						 </dd>
						</dt>																
						<%}else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())){ LanguageUtil.get(pageContext, "This-is-a-select"); %>
							<dt><B><%=field.getFieldName()+":&nbsp;</B> " %>
							 <dd>
							<select name="<%=field.getFieldContentlet()%>" disabled="disabled"> 										
							<%  String originalValue = String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field));
								String values = field.getValues();
								if (values != null)
			                     	values = values.trim();
			                    else 
			                       	values = "";
								String defaultValue = field.getDefaultValue();
								if (defaultValue != null)
			                     	defaultValue = defaultValue.trim();
			                    else 
			                    	defaultValue = "";
								String[] pairs = values.split("\r\n");
								for(int j = 0;j < pairs.length;j++){
									String pair = pairs[j];
									String[] tokens = pair.split("\\|");
									if (0 < tokens.length) {
										String name = tokens[0];
										String value = (tokens.length > 1 ? tokens[1] : name);
										String selected = "";
										String compareValue = (UtilMethods.isSet(originalValue) ? originalValue : defaultValue);
										if ((UtilMethods.isSet(compareValue) && compareValue.equals(value))){													
											selected = LanguageUtil.get(pageContext, "SELECTED");
										}						
									%>
										<option value="<%=value%>" <%=selected%>><%=name%></option>
								<%
									}
								}
								%>
							</select>  
							 </dd>
							</dt>
						<% }else if (field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())){
							 String originalValue = String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field));
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
						<dt><B><%=field.getFieldName()+":&nbsp;</B> " %>
						 <dd>
							 <select multiple="true" size="<%= pairs.length %>" disabled="disabled" name="<%=field.getFieldContentlet()%>MultiSelect" style="width:200px;">
							<% for(int j = 0;j < pairs.length;j++){
								String pair = pairs[j];
								String[] tokens = pair.split("\\|");
								if (0 < tokens.length) {
									String name = tokens[0];
									String value = (tokens.length > 1 ? tokens[1] : name);
									String selected = "";
									if (UtilMethods.isSet(defaultValue) && (defaultValue.contains("|" + value) || defaultValue.contains(value + "|") || defaultValue.equals(value)))												
									{
										selected = LanguageUtil.get(pageContext, "SELECTED");
									} else {
										if (UtilMethods.isSet(originalValue) && originalValue.contains(value)){
												selected = LanguageUtil.get(pageContext, "SELECTED");
										}
									}
							%>
								<option value="<%=value%>" <%=selected%>><%=name%></option>
							<%
								}
							}
							%>
							</select> 
							 </dd>
							</dt>
		              <% } else if (field.getFieldType().equals(Field.FieldType.TAG.toString())){ %>
							<dt><B><%=field.getFieldName()+":&nbsp;</B> " %>
							 <dd>
							<% String functionTag = "suggestTagsForSearch(this, '" + field.getFieldContentlet() + "suggestedTagsDiv');";%>
							<textarea disabled="disabled" style="height: 100px; width: 250px;font-size:12px; border-color: #CCCCCC; border-style: solid; border-width: 1px; font-family: Verdana, Arial,Helvetica;vertical-align: top;"
								name="<%=field.getFieldContentlet()%>" id="<%=field.getFieldContentlet()%>" onkeyup="<%= functionTag %>" >
								<%=(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(content, field))?String.valueOf(APILocator.getContentletAPI().getFieldValue(content, field)):"")%>
							</textarea>
							 </dd>
							</dt>
					<%}else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){ %>
					       <%
					   	    CategoryAPI categoryAPI = APILocator.getCategoryAPI();
					     	%>
					       <dt><B><%=field.getFieldName()+":&nbsp;</B> " %>
					        <dd>
					       <% 
					     
	                     
					       		String[] selectedCategories = null;
										    if(InodeUtils.isSet(content.getInode()))
										    {										    											   										     
										       List<com.dotmarketing.portlets.categories.model.Category> categoriesList = categoryAPI.getParents(content, user, false);
										       selectedCategories =  new String[categoriesList.size()];
										       int counter_k = 0;
										       for(com.dotmarketing.portlets.categories.model.Category cat: categoriesList){
										    	   if(cat != null)
										    	   {
										    	   		selectedCategories[counter_k] = cat.getInode();
										    	   		counter_k++;
										    	   }
										       }
											}else{
									         selectedCategories = field.getDefaultValue().split("\\|");
									         int l = 0;
									            for(String selectedCat: selectedCategories){
									            	com.dotmarketing.portlets.categories.model.Category selectedCategory = categoryAPI.findByName(selectedCat, user, false);
									            	selectedCategories[l] = selectedCategory.getInode();
									            	++l;
									            }
											 }
												com.dotmarketing.portlets.categories.model.Category category = categoryAPI.find(field.getValues(), APILocator.getUserAPI().getSystemUser(), false);
												java.util.List children = categoryAPI.getChildren(category, APILocator.getUserAPI().getSystemUser(), false);
												if (children.size() >= 1 && categoryAPI.canUseCategory(category, user, false)) {
													 String catOptions = com.dotmarketing.util.UtilHTML.getSelectCategories(category,1,selectedCategories, user, false); 
													 if(catOptions.length() > 0){
									            %>
													<table border="0" cellpadding="0" cellspacing="0">
													<tr>
													 <td>
													 <select multiple="true"  style="width:200px;" name="<%=field.getFieldContentlet()%>"  disabled>
														<%= catOptions %>
												     </select>
												     </td>
												    </tr>
												    </table>
														
													<%}
												}%>	
										  </dd>		
									    </dt>
                <% }%>
        <%}
				}%>
		</dl>
	</div>

	
</div>
