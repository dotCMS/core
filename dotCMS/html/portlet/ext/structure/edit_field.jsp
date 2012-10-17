<%@ page import="java.util.ArrayList" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Field" %>
<%@ page import="com.dotmarketing.portlets.structure.struts.FieldForm" %>
<%@ page import="com.dotmarketing.portlets.categories.business.CategoryAPI" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.portlets.categories.business.*" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@page import="com.dotmarketing.portlets.structure.business.FieldAPI"%>
<%@ include file="/html/portlet/ext/structure/init.jsp" %>
<%	


String referer = request.getParameter("referer");
FieldForm fieldForm = (FieldForm) request.getAttribute("DotFieldForm");
Structure structure = StructureFactory.getStructureByInode(fieldForm.getStructureInode());
String fieldName = fieldForm.getFieldName();
boolean hasInode = (InodeUtils.isSet(fieldForm.getInode()));
boolean fixed = fieldForm.isFixed();

CategoryAPI catAPI = APILocator.getCategoryAPI();

String   textArea = UtilMethods.isSet(fieldForm.getValues()) ? fieldForm.getValues() : "";
if(textArea!=null){
	textArea = textArea.replaceAll("&", "&amp;");
	textArea = textArea.replaceAll("<", "&lt;");
	textArea = textArea.replaceAll(">", "&gt;");
}
		 
String s1 = "<textarea name=\"values\" style=\"width:537px;height:146px;\"  class=\"form-text\" id=\"textAreaValues\">" + textArea + "</textarea>"; 
String s2 = "<textarea name=\"values\" style=\"width:300px;height:120px;\"";
s2 += " class=\"form-text\" id=\"textAreaValues\">" + textArea + "</textarea>"; 

%>


<script type='text/javascript' src='/dwr/interface/FieldVariableAjax.js'></script>
<script language="javascript">



	<%-- This is the javascript Array that controls what is shown or hidden --%>
	<%@ include file="/html/portlet/ext/structure/field_type_js_array.jsp" %>


	


	function addNewField(){   
	
		var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_field' />";
		href = href + "<portlet:param name='cmd' value='add' />";
		href = href + "</portlet:actionURL>";	
		var form = document.getElementById("field");
		form.action = href;
			
		enableAllForFormSubmit(form);
		
		form.submit();
	}

	function enableAllForFormSubmit(form){
		dijit.byId("elementSelectBox").attr('disabled', false);
		dijit.byId('dataTypetext').attr('disabled', false);
		dijit.byId('dataTypebool').attr('disabled', false);
		dijit.byId('dataTypedate').attr('disabled', false);
		dijit.byId('dataTypefloat').attr('disabled', false);
		dijit.byId('dataTypeinteger').attr('disabled', false);
		dijit.byId('dataTypetext_area').attr('disabled', false);
		dijit.byId('dataTypesection_divider').attr('disabled', false);
		dijit.byId('dataTypebinary').attr('disabled', false);
		//Checks if the required checkbox has been checked
		ifRequiredChecked();
	}

	function ifUniqueChecked(){
		var form = document.getElementById("field");
		if(dijit.byId("uniqueCB").attr('value') == 'on'){	
			dijit.byId('requiredCB').attr('disabled', true);
			dijit.byId('indexedCB').attr('disabled', true);
		}else{
			if(form.searchable.checked){
				dijit.byId('indexedCB').attr('disabled', true);
			}else{
				dijit.byId('indexedCB').attr('disabled', false);
			}			
			dijit.byId('requiredCB').attr('disabled', false);
		}				
	}

	function ifUserSearchableChecked(){
		var form = document.getElementById("field");
		if(form.searchable.checked){
			dijit.byId('indexedCB').attr('disabled', true);
		}else{
			if(!form.unique.checked){
				dijit.byId('indexedCB').attr('disabled', false);
			}
		}			
	}

	function ifRequiredCBChecked(){
		var form = document.getElementById("field");
		if(form.requiredCB.checked){
			dijit.byId("indexedCB").attr('value', 'on');
			dijit.byId('indexedCB').attr('disabled', true);
		}	
	}
	
	
	function disableSelect(){
		var form = document.getElementById("field");
	    var fieldinode='<%=request.getParameter("inode")%>';
	    if(fieldinode!="null"){   
	    	dijit.byId("elementSelectBox").setDisabled(true);
	    	//document.getElementById('fieldVarLink').style.display = "";
	    }
	}


	//This function sets the hidden field with the id requiredId's value to the true if required has been checked, to false if it has not.
	//It is called each time the required checkbox is checked or unchecked
	function writeRequired(){
		var form = document.getElementById("field");
		document.getElementById("requiredId").value = form.requiredCB.checked;
	}

	//This function checks if the required checkbox has been checked or not
	function ifRequiredChecked(){
		
		if(document.getElementById("requiredId") != null){
			if(document.getElementById("requiredId").value == true){
				dijit.byId("indexedCB").attr('value', 'on')
			}else{
				
			}
		}	
	}

	function cancel(event) {
		var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
		href = href + "<portlet:param name='inode' value='<%=request.getParameter("structureInode")%>' />";
		href = href + "</portlet:actionURL>";
		document.location = href;
		dojo.stopEvent(event);
	}

	function updateSelectBoxImage(){
		var imagestyle = "url('" + dijit.byId("elementSelectBox").item.imageurl + "')";
		var selField = dojo.query('#elementSelect div.dijitInputField')[0];

		
		dojo.style(selField, "backgroundImage", imagestyle);
		dojo.style(selField, "backgroundRepeat", "no-repeat");
		dojo.style(selField, "padding", "0px 0px 0px 20px");
		dojo.style(selField, "backgroundColor", "transparent");
		dojo.style(selField, "backgroundPosition", "4px 4px");

	}
	
	function isTrue(x){
		if(x==undefined) return false;
		if("true" == x || x){
			return true;
		}
		return false;
	}
	
	function isSet(x){
		if(x==undefined) return false;
		if(x.toString().length > 0){
			return true;
		}
		return false;
	}
	
	
	
	function elementTypeChange(){
		updateSelectBoxImage();
		hideAllElements();
		var fieldObj = dijit.byId("elementSelectBox").item;
		document.getElementById("elementSelectBox").value =  fieldObj.displayName ;
		
		
		
		var form = document.getElementById("field");
		var selValue = "whatever";
		
		
		// get our object
		for(i=0;i<fieldObj.show.length;i++){
			try{
				showElement(dojo.byId(fieldObj.show[i]));
			}
			catch(err){
				alert("cant find obj to show from the field type array : " + fieldObj.show[i]);
			}
		}
		
		if(isSet(fieldObj.dataType)){
			if (dijit.byId("dataType"+fieldObj.dataType) != undefined) {
<%
			if (!UtilMethods.isSet(fieldForm.getDataType())) {
%>
				dijit.byId("dataType"+fieldObj.dataType).setValue(true);
<%
			}
%>
			} else if (fieldObj.dataType == "constant") {
				document.getElementById("element").value = "constant";
			}
		}
		
		if(isSet(fieldObj.helpText)){
			dojo.byId("values_eg").innerHTML = "<h3>" + fieldObj.displayName + "</h3>" + fieldObj.helpText;
			showElement(dojo.byId("values_eg"));
		}

		//http://jira.dotmarketing.net/browse/DOTCMS-5022
		dijit.byId("saveButton").attr('iconClass', 'saveIcon');
		dijit.byId("cancelButton").attr('iconClass', 'cancelIcon');
		
		
	}
	
	function hideAllElements(){
		hideElement(dojo.byId("valueRow"));
		hideElement(dojo.byId("defaultText"));
		hideElement(dojo.byId("validationRow"));
		hideElement(dojo.byId("dataTypeRow"));
		hideElement(dojo.byId("hintText"));
		hideElement(dojo.byId("listed"));
		hideElement(dojo.byId("indexed"));
		hideElement(dojo.byId("required"));
		hideElement(dojo.byId("unique"));
		hideElement(dojo.byId("radioText"));
		hideElement(dojo.byId("radioBool"));
		hideElement(dojo.byId("radioDate"));
		hideElement(dojo.byId("radioDecimal"));
		hideElement(dojo.byId("radioNumber"));
		hideElement(dojo.byId("radioBlockText"));
		hideElement(dojo.byId("radioSectionDivider"));
		hideElement(dojo.byId("radioSystemField"));
		hideElement(dojo.byId("radioBinary"));
		hideElement(dojo.byId("userSearchable"));
		hideElement(dojo.byId("labelRow"));	
		hideElement(dojo.byId("categoryRow"));	
		hideElement(dojo.byId("values_eg"));
	}
	
	function hideUnique(form){
		document.getElementById("unique").style.display = "none";
		
	}
	
	function typeChangeonload(){
		var form = document.getElementById("field");
	
		form.fieldType.value="<%= fieldForm.getFieldType()%>";

		<% // DOTCMS-4364
		if (UtilMethods.isSet(fieldForm.getDataType()) && fieldForm.getDataType().equalsIgnoreCase("constant")) {
		%>
		form.fieldType.value="<%= fieldForm.getDataType()%>";
		<%
		}
		%>
		
		if(form.fieldType.value!=""){
			dijit.byId("elementSelectBox").setValue(form.fieldType.value);
			elementTypeChange();
		}
	}
	
	
	



	function fillRegexp(selDropDown) {
		dijit.byId('regexCheck').attr('value', dijit.byId('validation').attr('value'));
	}

	function setInitialValues(){
		//fix to check required when struts eros thrown 
		<% if(fieldForm.isRequired()){%>
			dijit.byId("requiredCB").attr('value', 'on');
		<%}%>
	}

	function setSearchable(){
			var form = document.getElementById("field");
			var indexed = <%=fieldForm.isIndexed()%>;
		
		if(form.searchableCB.checked){
			dijit.byId("indexedCB").attr('value', 'on')
		   <%if(!hasInode){%>
		   	dijit.byId("indexedCB").attr('value', 'on')
		 	<%}%>
		}else{
		    
		if(!dijit.byId("uniqueCB").attr('value') == 'on'){ 
		 if(form.fieldType.value != "category"){ 
		  if(!indexed){
			  dijit.byId("indexedCB").attr('value', 'off')
		  }
		   <%if(!hasInode){%>
		   		dijit.byId("indexedCB").attr('value', 'off')
		   <%}%>
		  } 
		 }
		}      
		ifUserSearchableChecked();
	}
  
	function showCategories(show){ 
	  if(show){
	   document.getElementById("textAreaValues").style.display="none";
	   document.getElementById("categories").style.display="";
	   document.getElementById("valueRowLabel").innerHTML = '<span class="required"></span> &nbsp;<%= LanguageUtil.get(pageContext, "category") %>: ';
	     
	  }else{
	    document.getElementById("textAreaValues").style.display="";
	    document.getElementById("categories").style.display="none";
	    document.getElementById("valueRowLabel").innerHTML = '<span class="required"></span> &nbsp;<%= LanguageUtil.get(pageContext, "Code") %>: ';
	  }
	}


	function validateCategories(categories) {
		var form = document.getElementById("field");
		var selected = form.fieldType.value;
		if(selected == "categories"){
			var cats = categories.value.split("\|");
			if (cats.length==0) {
				return false;
			} else {
				return true;
			}
		}
	}

	function uniqueUnchecked(){
		var form = document.getElementById("field");
		if(!form.uniqueCB.checked){}
			
	}

	function setUnique(){
	
		var form = document.getElementById("field");
		var indexed = <%=fieldForm.isIndexed()%>;
		
	    if(form.unique.checked){
	    	dijit.byId("indexedCB").attr('value', 'on');
	       dijit.byId("requiredCB").attr('value', 'on');
	       <%if(!hasInode){%>
			dijit.byId('indexedCB').attr('disabled', true);
			dijit.byId('requiredCB').attr('disabled', true);
	     	<%}%>
	     	
	     	 
	    }else{
			dijit.byId('indexedCB').attr('disabled', false);
			dijit.byId('requiredCB').attr('disabled', false);
	            
	    	if(!form.searchable.checked){
	     		if(form.fieldType.value != "category"){ 
	      			if(!indexed){
	      				dijit.byId("indexedCB").attr('value', 'off');
	          			dijit.byId("requiredCB").attr('value', 'off');
	      			}
	      			
	       			<%if(hasInode){%>
		    			dijit.byId('indexedCB').attr('disabled', false);
		       			dijit.byId("requiredCB").attr('value', 'off');
	       			<%}%>
	      		} 
	     	}
	       	
	       <%if(!hasInode){%>
			  dijit.byId('requiredCB').attr('disabled', false);
	       <%}%>   
	    }  
	    ifUniqueChecked();  
	}
    

  
	function setUniqueDataType(dataType){
		var form=document.getElementById("field");
	
		if(form.fieldType.value != 'text'){
			//if(dataType != 'integer' && dataType != 'text'){
		         //document.getElementById("unique").style.display = "none";
		         //document.getElementById("unique").style.display = "none";
		   	//}else{ 		
		       document.getElementById("uniqueCB").style.display = "none";
		       document.getElementById("unique").style.display = "none";
		   	//}
		}
	} 


	dojo.require("dojo.data.ItemFileReadStore");




	function myLabelFunc(item, store) {
        return store.getValue(item, "label");
    }
    
	var myStore = new dojo.data.ItemFileReadStore({data: myData});
		

	dojo.addOnLoad(
			function() {	

		
				var myselect = new dijit.form.FilteringSelect({
					 id: "elementSelectBox",
					 name: "fieldType", 
					 value: '',
					 required: true,
					 store: myStore,
					 searchAttr: "id",
					 labelAttr: "label",
					 labelType: "html",
					 onChange: elementTypeChange,
					 labelFunc: myLabelFunc  
				}, 
				dojo.byId("elementSelectBox"));

	
				typeChangeonload();

			    disableSelect();
				//setSearchable();
				//ifRequiredChecked();
				//ifRequiredCBChecked();
				ifUniqueChecked();
				ifUserSearchableChecked();
				//setInitialValues();
                //ifRequiredCBChecked;
                
				var form=document.getElementById("field");
				dojo.place("<input type=\"hidden\" id=\"requiredId\" value=\"" + dijit.byId("requiredCB").attr('value') + "\"/>", dojo.body(), 'last');
				//http://jira.dotmarketing.net/browse/DOTCMS-5022
				dijit.byId("saveButton").attr('iconClass', 'saveIcon');
				dijit.byId("cancelButton").attr('iconClass', 'cancelIcon');
			}
	);
	
	function editFieldVariables(){
		var fieldId='<%=request.getParameter("inode")%>';
	    if(fieldId!="null"){
	    	fieldVariablesAdmin.showFieldVariables(fieldId,false);
	    }		
	}
</script>











<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<html:form action="/ext/structure/edit_field" method="post" styleId="field">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Edit-Field")) %>' />

		<html:hidden property="inode" />
		<html:hidden property="structureInode" />
		<html:hidden property="sortOrder" />
		<html:hidden property="fixed" />
		<html:hidden property="element" styleId="element" />
		<html:hidden property="readOnly" />
		<html:hidden property="fieldContentlet" />
		<input type="hidden" name="referer"  value="<%=referer%>" >
		<html:hidden property="fieldRelationType" />

<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
	
<!-- START Tab1 -->
<div id="TabOne" dojoType="dijit.layout.ContentPane" title="Overview">
	<!-- START Field Options -->	
	<%-- Hint Box  --%>
	<div style="position:absolute;top:80px;right:40px;width:300px;display:none;text-align:left;" class="callOutBox2" id="values_eg"></div>	
	
	
	
	<dl id="elementFormTable;">
		<dt>
			<span id="VariableIdTitle" <%if(!UtilMethods.isSet(fieldForm.getVelocityVarName())){%> style="display:none"<%}%>>
				<%= LanguageUtil.get(pageContext, "Variable-ID") %>:
			</span>
		</dt>
		<dd style="clear: none;">
			<html:text property="velocityVarName" readonly="true" style="width:250px;border:0px;" />	
		</dd>
		<dt>
			<div id="displayType">
				<span class="required"></span>  <%= LanguageUtil.get(pageContext, "message.field.fieldType") %>:
			</div>
		</dt>
		<dd>
			<div id="elementSelect">
				<input id="elementSelectBox" />				
			</div>
		</dd>
	</dl>
	
	<dl id="labelRow" style="display:none">
		<dt>
			<span class="required"></span>  <%= LanguageUtil.get(pageContext, "Label") %>:
		</dt>
		<dd>
			<% if(fixed) { %>
			<input type="text" dojoType="dijit.form.TextBox" name="fieldName" readonly="readonly" style="width:250px" value="<%= UtilMethods.isSet(fieldForm.getFieldName()) ? fieldForm.getFieldName() : "" %>" />
			<% } else { %>
			<input type="text" dojoType="dijit.form.TextBox" name="fieldName" style="width:250px" value="<%= UtilMethods.isSet(fieldForm.getFieldName()) ? fieldForm.getFieldName() : "" %>" />
			<% }  %>
		</dd>
	</dl>
	
	<dl id="dataTypeRow" style="display:none">
		<dt>
			<span id="req_data_type" class="required"></span> <%= LanguageUtil.get(pageContext, "Data-Type") %>:
		</dt>
		<dd>
			<span id="radioText">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypetext" <%=fixed?"readonly=\"readonly\"":"" %> value="text" onclick="setUniqueDataType('text');" <% if(fieldForm.getDataType().equals(Field.DataType.TEXT.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypetext"><%= LanguageUtil.get(pageContext, "Text") %></label> &nbsp;
			</span>
			<span id="radioBool">	
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypebool" <%=fixed?"readonly=\"readonly\"":"" %> value="bool" onclick="setUniqueDataType('bool');" <% if(fieldForm.getDataType().equals(Field.DataType.BOOL.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypebool"><%= LanguageUtil.get(pageContext, "True-False") %></label> &nbsp;
			</span>
			<span id="radioDate">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypedate" <%=fixed?"readonly=\"readonly\"":"" %> value="date" onclick="setUniqueDataType('date');" <% if(fieldForm.getDataType().equals(Field.DataType.DATE.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypedate"><%= LanguageUtil.get(pageContext, "Date") %></label> &nbsp;
			</span>
			<span id="radioDecimal">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypefloat" <%=fixed?"readonly=\"readonly\"":"" %> value="float" onclick="setUniqueDataType('float');" <% if(fieldForm.getDataType().equals(Field.DataType.FLOAT.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypefloat"><%= LanguageUtil.get(pageContext, "Decimal") %></label> &nbsp;
			</span>
			<span id="radioNumber">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypeinteger" <%=fixed?"readonly=\"readonly\"":"" %> value="integer" onclick="setUniqueDataType('integer');" <% if(fieldForm.getDataType().equals(Field.DataType.INTEGER.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypeinteger"><%= LanguageUtil.get(pageContext, "Whole-Number") %></label> &nbsp;
			</span>
			<span id="radioBlockText">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypetext_area" <%=fixed?"readonly=\"readonly\"":"" %> value="text_area" onclick="setUniqueDataType('textarea');" <% if(fieldForm.getDataType().equals(Field.DataType.LONG_TEXT.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypetext_area"><%= LanguageUtil.get(pageContext, "Large-Block-of-Text") %></label>
			</span>
			<span id="radioSectionDivider">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypesection_divider" <%=fixed?"readonly=\"readonly\"":"" %> value="section_divider" onclick="setUniqueDataType('divider');" <% if(fieldForm.getDataType().equals(Field.DataType.SECTION_DIVIDER.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypesection_divider"><%= LanguageUtil.get(pageContext, "Section-Divider") %></label> &nbsp;
			</span>
			<span id="radioSystemField">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypesystem_field" <%=fixed?"readonly=\"readonly\"":"" %> value="system_field" onclick="setUniqueDataType('system');" />
				<label for="dataTypesystem_field"><%= LanguageUtil.get(pageContext, "System-Field") %></label> &nbsp;
			</span>
			<span id="radioBinary">
				<input dojoType="dijit.form.RadioButton" type="radio" name="dataType" id="dataTypebinary" <%=fixed?"readonly=\"readonly\"":"" %> value="binary" onclick="setUniqueDataType('binary');" <% if(fieldForm.getDataType().equals(Field.DataType.BINARY.toString())){ %> checked="checked" <% } %>/>
				<label for="dataTypebinary"><%= LanguageUtil.get(pageContext, "Binary") %></label>
			</span>
		</dd>
	</dl>
		
	<dl id="valueRow" style="display:none">
		<dt id="valueRowLabel">
			<span class="required"></span> &nbsp;<%= LanguageUtil.get(pageContext, "Value") %>:
		</dt>
		<dd>
			<div id="valueRow_inner"><textarea dojoType="dijit.form.Textarea" name="values" style="width:300px;min-height:120px;" id="textAreaValues"><%=UtilMethods.htmlifyString(textArea)%></textarea></div>
		</dd>
	</dl>
	
	<dl id="categoryRow" style="display:none">
		<dt id="valueRowLabel">
			<span class="required"></span> &nbsp;<%= LanguageUtil.get(pageContext, "Category") %>:
		</dt>
		<dd>
			<select dojoType="dijit.form.FilteringSelect" name="categories" id="categories">
				<%
				List<Category> cats = catAPI.findTopLevelCategories(user, false);
				String selectedCategory = fieldForm.getValues();
					for (Category category : cats) {
						if (catAPI.canUseCategory(category, user, false)) {%>
						 <%if(selectedCategory != null && selectedCategory.trim().equalsIgnoreCase(category.getInode())){ %>
							<option selected value="<%=category.getInode()%>"><%=category.getCategoryName()%></option>	
						<%}else{%>
							<option value="<%=category.getInode()%>"><%=category.getCategoryName()%></option>	
						<%}	
						}
					}
				%>
			</select>
		</dd>
	</dl>
	
	
	
	<dl style="display:none;">
		<dt>&nbsp;</dt>
		<dd><div id="structureCode"></div>&nbsp;</dd>
	</dl>
		
	<dl id="validationRow" style="display:none">
		<dt><%= LanguageUtil.get(pageContext, "Validation-RegEx") %>:</dt>
		<dd>
			<input type="text" dojoType="dijit.form.TextBox" name="regexCheck" id="regexCheck" style="width:250px" readonly="<%=fieldForm.isFixed() || fieldForm.isReadOnly()%>" value="<%= UtilMethods.isSet(fieldForm.getRegexCheck()) ? fieldForm.getRegexCheck() : "" %>" />
			<select dojoType="dijit.form.FilteringSelect" name="validation" id="validation" onchange="fillRegexp(this)">
				<option value=""><--<%= LanguageUtil.get(pageContext, "Select-validation") %>--></option>
				<option value="^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4})$"><%= LanguageUtil.get(pageContext, "Email") %></option>
				<option value="[0-9]*"><%= LanguageUtil.get(pageContext, "Numbers-only") %></option>
				<option value="[a-zA-Z\s]*"><%= LanguageUtil.get(pageContext, "Letters-only") %></option>
				<option value="[0-9a-zA-Z\s]*"><%= LanguageUtil.get(pageContext, "Alphanumeric") %></option>
				<option value="(^\d{5}$)|(^\d{5}-\d{4}$)"><%= LanguageUtil.get(pageContext, "US-Zip-Code") %></option>
				<option value="^\(?[1-9]\d{2}[\)\-]\s?\d{3}\-\d{4}$"><%= LanguageUtil.get(pageContext, "US-Phone") %></option>
				<option value="^((http|ftp|https):\/\/w{3}[\d]*.|(http|ftp|https):\/\/|w{3}[\d]*.)([\w\d\._\-#\(\)\[\]\,;:]+@[\w\d\._\-#\(\)\[\]\,;:])?([a-z0-9]+.)*[a-z\-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\/[\/a-zA-Z0-9\._\-,\%\s]+)*(\/|\?[a-z0-9=%&\.\-,#]+)?$"><%= LanguageUtil.get(pageContext, "URL-Pattern") %></option>
				<option value="[^(<[.\n]+>)]*"><%= LanguageUtil.get(pageContext, "No-HTML") %></option>
			</select>
			
		</dd>
	</dl>
		
	<dl id="defaultText" style="display:none">
		<dt><span id="defaultText" ><%= LanguageUtil.get(pageContext, "Default-Value") %>:</span></dt>
		<dd><input type="text" dojoType="dijit.form.TextBox" name="defaultValue" style="width:250px" onblur="validateCategories(this);" value="<%= UtilMethods.isSet(fieldForm.getDefaultValue()) ? fieldForm.getDefaultValue() : "" %>" /></span></dd>
	</dl>
	<dl id="hintText" style="display:none">
		<dt><%= LanguageUtil.get(pageContext, "Hint") %>:</dt>
		<dd><input type="text" dojoType="dijit.form.TextBox" name="hint" style="width:250px" value="<%= UtilMethods.isSet(fieldForm.getHint()) ? UtilMethods.escapeDoubleQuotes(fieldForm.getHint()) : "" %>" /></dd>
	</dl>
	<!-- END Field Options -->
		
	<!-- START Check Boxes -->
	<dl id="required" style="display:none">
		<dt>&nbsp;</dt>
		<dd>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="required" id="requiredCB" <%=fixed?"readonly=\"readonly\"":"" %> onClick="writeRequired();" <% if(fieldForm.isRequired()){ %> checked="checked" <% } %> />
			<label for="requiredCB"><%= LanguageUtil.get(pageContext, "Required") %></label> 
		</dd>
	</dl>
	<dl id="userSearchable" style="display:none">
		<dt>&nbsp;</dt>
		<dd>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="searchable" id="searchableCB" onClick="setSearchable();" <% if(fieldForm.isSearchable()){ %> checked="checked" <% } %> />
			<label for="searchableCB"><%= LanguageUtil.get(pageContext, "User-Searchable") %></label>
		</dd>
	</dl>
	<dl id="indexed" style="display:none">
		<dt>&nbsp;</dt>
		<dd>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="indexed" id="indexedCB" <%=fixed?"readonly=\"readonly\"":"" %> <% if(fieldForm.isIndexed()){ %> checked="checked" <% } %> />
			<label for="indexedCB"><%= LanguageUtil.get(pageContext, "System-Indexed") %></label>
		</dd>
	</dl>
	<dl id="listed" style="display:none">
		<dt>&nbsp;</dt>
		<dd>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="listed" id="listedCB" <% if(fieldForm.isListed()){ %> checked="checked" <% } %> />
			<label for="listedCB"><%= LanguageUtil.get(pageContext, "Show-in-listing") %></label>
		</dd>
	</dl>
	<dl id="unique" style="display:none">
		<dt>&nbsp;</dt>
		<dd>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="unique" id="uniqueCB" <%=fixed?"readonly=\"readonly\"":"" %> onclick="setUnique();" <% if(fieldForm.isUnique()){ %> checked="checked" <% } %> />
			<label for="uniqueCB"><%= LanguageUtil.get(pageContext, "Unique") %></label>
		</dd>
	</dl>
	<!-- END Check Boxes -->
	
</div>
<!-- END Tab1 -->

<!-- START Tab2 -->
<div id="TabTwo" dojoType="dijit.layout.ContentPane" onShow='javascript:editFieldVariables();' title="Field Variables">
 
	<!--<dl id="fieldVarLink" style="display:none">
		<dt>&nbsp;</dt>
		<dd>			
			<button dojoType="dijit.form.Button" onClick="javascript:editFieldVariables();" iconClass="plusIcon">
                <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-new-Field-Variable")) %>
            </button>
		</dd>
	</dl>-->
	<%@ include file="/html/portlet/ext/structure/view_field_variables_inc.jsp" %>
</div>
<!-- END Tab2 -->

</div>

<!-- START Button Row -->
<div class="buttonRow">
	<button id="saveButton" dojoType="dijit.form.Button" type="button" onClick="addNewField();" >
       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Field")) %>
    </button>
    <button id="cancelButton" dojoType="dijit.form.Button" type="button" onClick="cancel" >
       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
    </button>
</div>
<!-- END Button Row -->


</html:form>
</liferay:box>

