<script type="text/javascript" src="/dwr/interface/HostVariableAjax.js"></script>
<script type="text/javascript">

	dojo.declare("HostVariablesAdmin", null, {

		//I18n messages
		variableDeletedMsg: '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "host-variable-deleted")) %>',
		variableSavedMsg: '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "host-variable-saved")) %>',
		deleteHostVariableConfirmMsg: '<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.delete.hostvariable"))%>',

		//HTML Templates
		hostVariableRowTemplate:
			'<tr id="hostVariableRow-${rownum}"  style="" >' +
		  	'	<td class="listingTable__actions">' +
			'		<input type="hidden" id="hostVariableId-${rownum}" value="${id}" >' +
			'		<input type="hidden" id="hostVariableKey-${rownum}" value="${key}" >' +
		 	'		<input type="hidden" id="hostVariableName-${rownum}" value="${name}" >' +
		  	'		<input type="hidden" id="hostVariableValue-${rownum}" value="${value}" >' +
			'		<a href="javascript: hostVariablesAdmin.editVariable(\'${id}\',\'${rownum}\');">' +
		 	'			 <span class="editIcon"></span>' +
		 	'		</a>' +
		 	'		<a href="javascript: hostVariablesAdmin.deleteVariable(\'${id}\',${rownum});">' +
		 	'			 <span class="deleteIcon"></span>' +
		 	'		</a>' +
		 	'	</td>' +
		 	'	<td>${name}</td>' +
		 	'	<td>${key}</td>' +
			'	<td>${user}</td>' +
		 	'	<td style="white-space:nowrap">${date}</td>' +
		 	'</tr>',

	 	//Global variables
		currentIndex: 0,

		hostId: 0,

	 	//Methods
	 	showHostVariables: function (hostId) {
			HostVariableAjax.getHostVariables(hostId, dojo.hitch(this, hostVariablesAdmin.showHostVariablesCallback));
			this.hostId = hostId;
		},

	 	showHostVariablesCallback: function (variables) {

			dojo.html.set(dojo.byId('hostVariablesTable'), '');
			this.currentIndex = 0;

			dojo.forEach(variables, function (variable) {
				this.insertVariable(variable);
			}, this);

			if(variables.length > 0)
				dojo.style(dojo.byId('noHostVariables'), 'display', 'none');
			else
				dojo.style(dojo.byId('noHostVariables'), 'display', 'table-row-group');

			dijit.byId('viewHostVariablesDialog').show();
		},

		insertVariable: function (variable) {
			var dateFormatted = this.formatDate(variable.lastModDate);
			var buffer = dojo.string.substitute(this.hostVariableRowTemplate,
				{
					rownum: this.currentIndex,
					name: variable.name,
					key: variable.key,
					user: variable.lastModifierFullName,
					date: dateFormatted,
					id: variable.id,
					value: variable.value,
				});
			dojo.place( buffer, dojo.byId('hostVariablesTable'), "last");
			this.currentIndex++;
		},

		editVariable: function (id, row) {
			dojo.byId('hostVariableId').value = id;
			dojo.byId('hostVariableName').value = dojo.byId('hostVariableName-'+row).value;
			dojo.byId('hostVariableKey').value = dojo.byId('hostVariableKey-'+row).value;
			dojo.byId('hostVariableValue').value = dojo.byId('hostVariableValue-'+row).value;
			dojo.byId('editHostVariableErrorMessage').innerHTML = "";
			dijit.byId('editHostVariable').show();
		},

		deleteVariable: function (id) {

			if(confirm(this.deleteHostVariableConfirmMsg)){
				HostVariableAjax.deleteHostVariable(id, dojo.hitch(this, this.deleteVariableCallback));
			}
		},

		deleteVariableCallback: function () {
			this.showHostVariables(this.hostId);
			showDotCMSSystemMessage(this.variableDeletedMsg);
		},

		saveVariable: function()
		{
			var id = dojo.byId('hostVariableId').value;
			var name = dojo.byId('hostVariableName').value;
			var key= dojo.byId('hostVariableKey').value;
			var value = dojo.byId('hostVariableValue').value;
			HostVariableAjax.saveHostVariable(id, this.hostId, name, key, value, dojo.hitch(this, this.saveVariableCallback));
		},

    	saveVariableCallback: function (error) {
			if (error) {
				dojo.byId('editHostVariableErrorMessage').innerHTML = error;
				return;
			}
			showDotCMSSystemMessage(this.variableSavedMsg);
			this.showHostVariables(this.hostId);
			dijit.byId('editHostVariable').hide();
	    },

		clearFilter: function () {
			dojo.byId('hostVariablesFilter').value = "";
			this.filterResults();
		},

		filterResults: function () {
			var filterCriteria = dojo.byId('hostVariablesFilter').value;
			var found = 0;
			for(i = 0; i < this.currentIndex; i++) {
				if(filterCriteria != "" && dojo.byId('hostVariableKey-'+i).value.search(filterCriteria) < 0 &&
					dojo.byId('hostVariableName-'+i).value.search(filterCriteria) < 0) {
					dojo.byId('hostVariableRow-' + i).hide();
				} else {
					found++;
					dojo.byId('hostVariableRow-' + i).show();
				}
				if(found == 0 && filterCriteria != '') {
					dojo.style(dojo.byId('noHostVariables'), 'display', 'table-row-group');
				} else if(found > 0) {
					dojo.style(dojo.byId('noHostVariables'), 'display', 'none');
				}
			}

		},

		addNewVariable: function()
	   	{
	  		dojo.byId('hostVariableId').value ="";
			dojo.byId('hostVariableName').value ="";
			dojo.byId('hostVariableKey').value ="";
			dojo.byId('hostVariableValue').value ="";
			dojo.byId('editHostVariableErrorMessage').innerHTML = "";
	   	   	dijit.byId('editHostVariable').show();

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
        hostVariablesAdmin = new HostVariablesAdmin();
    }))

</script>

<div id="viewHostVariablesDialog" dojoType="dijit.Dialog" style="width: 800px;" title="<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Host-Variables")) %>" onCancel="javascript:hostVariablesAdmin.clearFilter();">
    <div class="portlet-toolbar">
        <div class="portlet-toolbar__actions-primary">
            <div class="inline-form">
                <input dojoType="dijit.form.TextBox" type="text" id="hostVariablesFilter" />
                <button dojoType="dijit.form.Button" onClick="hostVariablesAdmin.filterResults()">
                    <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
                </button>
                <button dojoType="dijit.form.Button" onClick="hostVariablesAdmin.clearFilter()" class="dijitButtonFlat">
                    <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
                </button>
            </div>
        </div>
        <div class="portlet-toolbar__actions-primary">
            <button dojoType="dijit.form.Button" onClick="hostVariablesAdmin.addNewVariable()" iconClass="plusIcon">
                <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-new-Host-Variable")) %>
            </button>
        </div>
    </div>
    <table class="listingTable">
    	<thead>
	        <tr>
	            <th width="45" align="center">
	                <%=LanguageUtil.get(pageContext, "Action") %>
	            </th>
	            <th>
	                <%=LanguageUtil.get(pageContext, "name") %>
	            </th>
	            <th width="75">
	                <%=LanguageUtil.get(pageContext, "Key") %>
	            </th>
	            <th width="125">
	                <%=LanguageUtil.get(pageContext, "Last-Editor") %>
	            </th>
	            <th width="100" align="center" style="white-space:nowrap">
	                <%=LanguageUtil.get(pageContext, "Last-Edit-Date") %>
	            </th>
	        </tr>
    	</thead>
		<tbody id="hostVariablesTable">

		</tbody>
		<tbody id="noHostVariables" style="display:none;">
			<tr>
				<td colspan="5" style="text-align: center;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Host-Variables-Found")) %></td>
			</tr>
		</tbody>
    </table>
</div>

<div id="editHostVariable" title="<%= LanguageUtil.get(pageContext, "Adding-or-Editing-a-Variable")%>" dojoType="dijit.Dialog" style="display: none; width: 400px;">
	<input type="hidden" id="hostVariableId" >
	<div id="editHostVariableErrorMessage" style="text-align: center; color: red;"></div>
	<div class="form-horizontal">
		<dl>
			<dt><label for="hostVariableName"><%= LanguageUtil.get(pageContext, "Name")%>:</label></dt>
			<dd><input dojoType="dijit.form.TextBox" type="text" id="hostVariableName" style="width:218px;"></dd>
		</dl>
		<dl>
			<dt><label for="hostVariableKey"><%= LanguageUtil.get(pageContext, "Key")%>:</label></dt>
			<dd><input dojoType="dijit.form.TextBox" type="text" id="hostVariableKey" style="width:218px;"></dd>
		</dl>
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Value")%></dt>
			<dd><textarea dojoType="dijit.form.Textarea" style="width:218px; min-height: 100px;" id="hostVariableValue" ></textarea></dd>
		</dl>
	</div>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" onClick="hostVariablesAdmin.saveVariable();">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		</button>
		<button dojoType="dijit.form.Button" onClick="dijit.byId('editHostVariable').hide();" class="dijitButtonFlat">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		</button>
	</div>
</div>
