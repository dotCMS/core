<script type="text/javascript" src="/dwr/interface/FieldVariableAjax.js"></script>
<script type="text/javascript">

	dojo.declare("FieldVariablesAdmin", null, {

		//I18n messages
		variableDeletedMsg: '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field-variable-deleted")) %>',
		variableSavedMsg: '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field-variable-saved")) %>',
		deleteFieldVariableConfirmMsg: '<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.delete.fieldvariable"))%>',
	
		//HTML Templates
		fieldVariableRowTemplate:
			'<tr ${str_style} id="fieldVariableRow-${rownum}"  style="" >' +
		  	'	<td align="center">' +
			'		<input type="hidden" id="fieldVariableId-${rownum}" value="${id}" >' +
			'		<input type="hidden" id="fieldVariableKey-${rownum}" value="${key}" >' +
		 	//'		<input type="hidden" id="fieldVariableName-${rownum}" value="${name}" >' +
		  	'		<input type="hidden" id="fieldVariableValue-${rownum}" value="${value}" >' +
			'		<a href="javascript: fieldVariablesAdmin.editVariable(\'${id}\',\'${rownum}\');">' +
		 	'			 <span class="editIcon"></span>' +
		 	'		</a>' +
		 	'		<a href="javascript: fieldVariablesAdmin.deleteVariable(\'${id}\',${rownum});">' +
		 	'			 <span class="deleteIcon"></span>' +
		 	'		</a>' +
		 	'	</td>' +
		 	//'	<td>${name}</td>' +
		 	'	<td>${key}</td>' +
			'	<td>${value}</td>' +
		 	//'	<td>${user}</td>' +
		 	//'	<td align="center">${date}</td>' +
		 	'</tr>',

	 	//Global variables
		currentIndex: 0,

		fieldId: 0,

		isDialog:true,

	 	//Methods
	 	showFieldVariables: function (fieldId,isDialog) {
	 	    this.isDialog = isDialog;
			FieldVariableAjax.getFieldVariablesForField(fieldId, dojo.hitch(this, fieldVariablesAdmin.showFieldVariablesCallback));
			this.fieldId = fieldId;
		},
		showFieldVariablesCallback: function (variables) {

			if(this.isDialog)
				dojo.html.set(dojo.byId('fieldVariablesDialogTable'), '');
			else
				dojo.html.set(dojo.byId('fieldVariablesTabTable'), '');

			this.currentIndex = 0;

			dojo.forEach(variables, function (variable) {
				this.insertVariable(variable);
			}, this);

			if(variables.length > 0){
				if(this.isDialog)
					dojo.style(dojo.byId('noFieldVariablesDialogTable'), 'visibility', 'hidden');
				else
					dojo.style(dojo.byId('noFieldVariablesTabTable'), 'visibility', 'hidden');
			}
			else{
				if(this.isDialog)
					dojo.style(dojo.byId('noFieldVariablesDialogTable'), 'visibility', 'visible');
				else
					dojo.style(dojo.byId('noFieldVariablesTabTable'), 'visibility', 'visible');
			}

			if(this.isDialog)
				dijit.byId('viewFieldVariablesDialog').show();
			else{
				dojo.style(dojo.byId('viewFieldVariablesTab'), 'height', '100%');
				dojo.style(dojo.byId('viewFieldVariablesTab'), 'visibility', 'visible');
			}
		},
		showInitFieldVariables: function (){
			dojo.style(dojo.byId('viewFieldVariablesInitTab'), 'height', '100px');
			dojo.style(dojo.byId('viewFieldVariablesInitTab'), 'visibility', 'visible');
		},
		insertVariable: function (variable) {

			if (this.currentIndex % 2 == 0){
				str_style="class=\"alternate_1\"";
			} else {
		    	str_style="class=\"alternate_2\"";
			}

			var dateFormatted = this.formatDate(variable.lastModDate);

			var buffer = dojo.string.substitute(this.fieldVariableRowTemplate,
				{
					rownum: this.currentIndex,
					//name: variable.name,
					key: variable.key,
					user: variable.lastModifierFullName,
					date: dateFormatted,
					id: variable.id,
					value: variable.value,
					str_style: str_style
				});
			if(this.isDialog)
				dojo.place( buffer, dojo.byId('fieldVariablesDialogTable'), "last");
			else
				dojo.place( buffer, dojo.byId('fieldVariablesTabTable'), "last");
			this.currentIndex++;
		},

		editVariable: function (id, row) {
			dojo.byId('fieldVariableId').value = id;
			//dojo.byId('fieldVariableName').value = dojo.byId('fieldVariableName-'+row).value;
			dojo.byId('fieldVariableKey').value = dojo.byId('fieldVariableKey-'+row).value;
			dojo.byId('fieldVariableValue').value = dojo.byId('fieldVariableValue-'+row).value;
			dojo.byId('editFieldVariableErrorMessage').innerHTML = "";
			dijit.byId('editFieldVariable').show();
		},

		deleteVariable: function (id) {

			if(confirm(this.deleteFieldVariableConfirmMsg)){
				FieldVariableAjax.deleteFieldVariable(id, dojo.hitch(this, this.deleteVariableCallback));
			}
		},

		deleteVariableCallback: function () {
			this.showFieldVariables(this.fieldId,this.isDialog);
			showDotCMSSystemMessage(this.variableDeletedMsg);
		},

		saveVariable: function()
		{
			var id = dojo.byId('fieldVariableId').value;
			//var name = dojo.byId('fieldVariableName').value;
			var key= dojo.byId('fieldVariableKey').value;
			var value = dojo.byId('fieldVariableValue').value;
			FieldVariableAjax.saveFieldVariable(id, this.fieldId, '', key, value, dojo.hitch(this, this.saveVariableCallback));
		},

    	saveVariableCallback: function (error) {
			if (error) {
				dojo.byId('editFieldVariableErrorMessage').innerHTML = error;
				return;
			}
			showDotCMSSystemMessage(this.variableSavedMsg);
			this.showFieldVariables(this.fieldId,this.isDialog);
			dijit.byId('editFieldVariable').hide();
	    },

		clearFilter: function () {
			dojo.byId('fieldVariablesFilter').value = "";
			this.filterResults();
		},

		filterResults: function () {
			var filterCriteria = dojo.byId('fieldVariablesFilter').value;
			var found = 0;
			for(i = 0; i < this.currentIndex; i++) {
				if(filterCriteria != "" && dojo.byId('fieldVariableKey-'+i).value.search(filterCriteria) < 0 && 
					dojo.byId('fieldVariableName-'+i).value.search(filterCriteria) < 0) {
					dojo.byId('fieldVariableRow-' + i).hide();
				} else {
					found++;
					dojo.byId('fieldVariableRow-' + i).show();
				}
				if(found == 0 && filterCriteria != '') {
					if(this.isDialog)
						dojo.byId('noFieldVariablesDialogTable').show();
					else
						dojo.byId('noFieldVariablesTabTable').show();
				} else if(found > 0) {
					if(this.isDialog)
						dojo.byId('noFieldVariablesDialogTable').hide();
					else
						dojo.byId('noFieldVariablesTabTable').hide();
				}
			}
		
		},
	
		addNewVariable: function() 
	   	{
	  		dojo.byId('fieldVariableId').value ="";
			//dojo.byId('fieldVariableName').value ="";
			dojo.byId('fieldVariableKey').value ="";
			dojo.byId('fieldVariableValue').value ="";
	   	   	dijit.byId('editFieldVariable').show();
	   	   	
		},
		
		formatDate: function(date) {
			if(!date)
				return "";
				
			var dateHours = date.getHours();
			var dateAMPM = "AM";
			if(dateHours > 12) {
				dateAMPM = "PM";
				dateHours = dateHours - 12;				
			}
			var dateMinutes = date.getMinutes();
			
			if(dateHours < 10)
				dateHours = "0" + dateHours;

			if(dateMinutes < 10)
				dateMinutes = "0" + dateMinutes;
				
			return (date.getMonth() + 1) + "/" + 
				date.getDate() + "/" + date.getFullYear() + " " +
				dateHours + ":" + dateMinutes + dateAMPM;		
					
		}
	});
		
    dojo.addOnLoad(dojo.hitch(this, function(){
        fieldVariablesAdmin = new FieldVariablesAdmin();
    }));

</script>
	
<div id="viewFieldVariablesTab">
	<div class="yui-u" style="text-align:right;margin:5px 20px 10px 0;">
	    <button dojoType="dijit.form.Button" onClick="fieldVariablesAdmin.addNewVariable(); return false;" iconClass="plusIcon">
	        <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-new-Field-Variable")) %>
	    </button>
	</div>
    <table class="listingTable">
    	<thead>
	        <tr>
	            <th width="5%" align="center">
	                <%=LanguageUtil.get(pageContext, "Action") %>
	            </th>
	           <!--  <th>
	                <%=LanguageUtil.get(pageContext, "name") %>
	            </th>-->
	            <th width="35%">
	                <%=LanguageUtil.get(pageContext, "Key") %>
	            </th>
				<th width="60%">
	                <%=LanguageUtil.get(pageContext, "Value") %>
	            </th>
	            <!-- <th width="125">
	                <%=LanguageUtil.get(pageContext, "Last-Editor") %>
	            </th>
	            <th width="100" align="center">
	                <%=LanguageUtil.get(pageContext, "Last-Edit-Date") %>
	            </th>-->
	        </tr>
    	</thead>
		<tbody id="fieldVariablesTabTable">
		
		</tbody>
		<tbody id="noFieldVariablesTabTable" style="visibility:hidden;">
			<tr>
				<td colspan="5" style="text-align: center;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Field-Variables-Found")) %></td>
			</tr>			
		</tbody>
    </table>
</div>
 
<div id="viewFieldVariablesDialog" dojoType="dijit.Dialog" style="width: 800px;">
	<div class="yui-u" style="text-align:right;margin:5px 20px 10px 0;">
	    <button dojoType="dijit.form.Button" onClick="fieldVariablesAdmin.addNewVariable()" iconClass="plusIcon">
	        <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-new-Field-Variable")) %>
	    </button>
	</div>
    <table class="listingTable">
    	<thead>
	        <tr>
	            <th width="45" align="center">
	                <%=LanguageUtil.get(pageContext, "Action") %>
	            </th>
	           <!--  <th>
	                <%=LanguageUtil.get(pageContext, "name") %>
	            </th>-->
	            <th width="30%">
	                <%=LanguageUtil.get(pageContext, "Key") %>
	            </th>
				<th width="70%">
	                <%=LanguageUtil.get(pageContext, "Value") %>
	            </th>
	            <!-- <th width="125">
	                <%=LanguageUtil.get(pageContext, "Last-Editor") %>
	            </th>
	            <th width="100" align="center">
	                <%=LanguageUtil.get(pageContext, "Last-Edit-Date") %>
	            </th>-->
	        </tr>
    	</thead>
		<tbody id="fieldVariablesDialogTable">
		  
		</tbody>
		<tbody id="noFieldVariablesDialogTable" style="visibility:hidden;">
			<tr>
				<td colspan="5" style="text-align: center;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Field-Variables-Found")) %></td>
			</tr>			
		</tbody>
    </table>
</div>

<div id="editFieldVariable"  dojoType="dijit.Dialog" style="display: none; width: 400px;">
	<input type="hidden" id="fieldVariableId" >
	<%= LanguageUtil.get(pageContext, "Adding-or-Editing-a-Variable")%>
	<div id="editFieldVariableErrorMessage" style="text-align: center; color: red;"></div>
	<dl>
		<!-- <dt><label for="name"><%= LanguageUtil.get(pageContext, "Name")%>:</label></dt>
		<dd><input dojoType="dijit.form.TextBox" type="text" id="fieldVariableName"></dd>-->
		
		<dt><label for="key"><%= LanguageUtil.get(pageContext, "Key")%>:</label></dt>
		<dd><input dojoType="dijit.form.TextBox" type="text" id="fieldVariableKey"></dd>

		<dt><%= LanguageUtil.get(pageContext, "Value")%></dt>
		<dd><textarea  style="width:200px; height: 100px;" id="fieldVariableValue" ></textarea></dd>
	</dl>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" onClick="fieldVariablesAdmin.saveVariable();" iconClass="saveIcon">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		</button>
		<button dojoType="dijit.form.Button" onClick="dijit.byId('editFieldVariable').hide();" iconClass="cancelIcon">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		</button>
	</div>
</div> 
<div id="viewFieldVariablesInitTab" style="visibility:hidden;">
	<div class="yui-u" style="text-align:left;width:800px;">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Field-No-Field-Variable")) %>
	</div>
</div>

<script type="text/javascript">

	dojo.addOnLoad (function(){
		dijit.byId('viewFieldVariablesDialog').hide();
		dojo.style(dojo.byId('viewFieldVariablesTab'), 'height', '0px');
		dojo.style(dojo.byId('viewFieldVariablesTab'), 'visibility', 'hidden');
	});

</script>