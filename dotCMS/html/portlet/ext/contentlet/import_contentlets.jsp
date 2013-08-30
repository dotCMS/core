<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>

<!-- JSP Imports -->
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@ page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>

<%@ page import="com.dotmarketing.util.UtilMethods" %>
<script type='text/javascript' src='/dwr/interface/ImportContentletAjax.js'></script>
<!--  Initialization Code -->
<%
	List<Structure> structures = StructureFactory.getStructuresWithWritePermissions(user, false);
	request.setAttribute("structures", structures);
	String selectedStructure = null;
	if(UtilMethods.isSet(session.getAttribute("selectedStructure"))){
		selectedStructure= (String)session.getAttribute("selectedStructure");
	}


	List<com.dotmarketing.portlets.languagesmanager.model.Language> languages = APILocator.getLanguageAPI().getLanguages();
	List<HashMap<String, Object>> languagesMap = new ArrayList<HashMap<String, Object>>();
	HashMap<String, Object> languageMap;
	for (com.dotmarketing.portlets.languagesmanager.model.Language language: languages) {
		languageMap = new HashMap<String, Object>();
		languageMap.put("id", language.getId());
		languageMap.put("description", language.getCountry() + " - " + language.getLanguage());
		languagesMap.add(languageMap);
	}
	languageMap = new HashMap<String, Object>();
	languageMap.put("id", -1);
	languageMap.put("description", "Multilingual File");
	languagesMap.add(languageMap);
	request.setAttribute("languages", languagesMap);

	ImportContentletsForm form = (ImportContentletsForm)request.getAttribute("ImportContentletsForm");

	if (form.getLanguage() == 0) {
		form.setLanguage(APILocator.getLanguageAPI().getDefaultLanguage().getId());
	}
%>



<%@page import="com.dotmarketing.portlets.contentlet.action.ImportAuditUtil.ImportAuditResults"%>
<%@page import="com.dotmarketing.portlets.contentlet.action.ImportAuditUtil"%><script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>

<script type='text/javascript'>
	function structureChanged () {
		var inode = dijit.byId("structuresSelect").attr('value');
		StructureAjax.getKeyStructureFields(inode, fillFields);
	}

	function fillFields (data) {
		if(data["allowImport"] == false){
			document.getElementById('importDetails').style.display = "none";
			document.getElementById('cantImportMessage').style.display = "block";
			return;
		}
		document.getElementById('cantImportMessage').style.display = "none";
		document.getElementById('importDetails').style.display = "block";
		currentStructureFields = data["keyStructureFields"];
		dwr.util.removeAllRows("import_fields_table");
		dwr.util.addRows("import_fields_table", currentStructureFields, [fieldCheckbox], { escapeHtml: false });
		dojo.parser.parse('import_fields_table');
	}

	function fieldCheckbox (field) {
		var fieldName = field["fieldName"];
		var fieldInode = field["inode"];
		var fieldIndexed = field["fieldIndexed"];
		var disableField = "";
		if(!fieldIndexed){
		   disableField = "disabled"
		}

		<%
			String[] fields = form.getFields();
			for (int i = 0; i < fields.length; i++)
			{

		%>
		if ((fieldInode == "<%=fields[i]%>") && fieldIndexed) {
			if (dijit.byId(fieldInode + 'Field'))
				dijit.byId(fieldInode + 'Field').destroy();
			return "<div><input checked type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" id=\"" + fieldInode + "Field\" name=\"fields\" value=\"" + fieldInode + "\" "+disableField+" /> "  + fieldName + "</div>";
		}
		<%

			}
		%>
		if (dijit.byId(fieldInode + 'Field'))
			dijit.byId(fieldInode + 'Field').destroy()
		return "<div style='margin:2px 0px;'><input type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" id=\"" + fieldInode + "Field\" name=\"fields\" value=\"" + fieldInode + "\" "+disableField+" /> <label>" +  fieldName + "</label></div>";
	}

	function submitForm () {
		var button = document.getElementById("goToPreviewButton");
		button.value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Generating-Preview-Info-Please-be-patient")) %>';
		button.disabled = true;
		var href =  '<portlet:actionURL>';
			href +=		'<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />';
			href +=		'<portlet:param name="cmd" value="preview" />';
			href +=	'</portlet:actionURL>';
		var form = document.getElementById("importForm");
		form.action = href;
		form.fileName.value = document.getElementById("file").value;
		form.cmd.value = "preview";
		form.submit();
	}

	function downloadCSVExample() {
		var href =  '<portlet:actionURL>';
			href +=		'<portlet:param name="struts_action" value="/ext/contentlet/import_contentlets" />';
			href +=		'<portlet:param name="cmd" value="downloadCSVTemplate" />';
			href +=	'</portlet:actionURL>';
		var form = document.getElementById("importForm");

		form.action = href;
		form.cmd.value = "downloadCSVTemplate";
		form.submit();
	}

	function languageChanged() {
		var languageId = dijit.byId("languageSelect").attr('value');
		if (-1 < languageId) {
				document.getElementById("multiLingualImportNotes").style.display="none";
		} else {
			document.getElementById("multiLingualImportNotes").style.display="block";
		}
	}

	function importCancelCallback(response){
		var tr = document.getElementById('audit' + response);
		if (tr) {
		    if (tr.nodeName == 'TR') {
		      var tbl = tr; // Look up the hierarchy for TABLE
		      while (tbl != document && tbl.nodeName != 'TABLE') {
		        tbl = tbl.parentNode;
		      }

		      if (tbl && tbl.nodeName == 'TABLE') {
		        while (tr.hasChildNodes()) {
		          tr.removeChild( tr.lastChild );
		        }
		    	tr.parentNode.removeChild( tr );
		      }
		    }
	  	}
	}

	function importCancel(id){
    	ImportContentletAjax.cancelImport(id,importCancelCallback);
    }
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">


      <liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"import-contentlets\") %>" />

      			<%
			     	ImportAuditUtil.ImportAuditResults iar = request.getAttribute("audits") == null ? null:(ImportAuditUtil.ImportAuditResults)request.getAttribute("audits");
			     	if(iar != null && iar.getUserRecords().size() > 0){
			     %>
			     <div style="position:absolute;left:60%;top:16%; padding: 30px;">
					<table class="listingTable">
						<tr>
							<th><%= LanguageUtil.get(pageContext, "Time-Started") %></th>
							<th><%= LanguageUtil.get(pageContext, "File-Name") %></th>
							<th><%= LanguageUtil.get(pageContext, "Total") %></th>
							<th></th>
						</tr>
						<% for(Map<String, Object> recs : iar.getUserRecords()){ %>
						<tr id="audit<%= recs.get("id") %>">
						<%
						String dateData="";
						Date dateValue = null;
						if(recs.get("start_date")instanceof java.util.Date){
							dateValue=(java.util.Date) recs.get("start_date");
						}
						else {
							dateData= recs.get("start_date").toString();
							SimpleDateFormat dateFormatter = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.LONGDBDATE);
							dateValue = dateFormatter.parse(dateData);
						}


						%>
							<td><%= dateValue %></td>
							<td><%= recs.get("filename") %></td>
							<td><%= recs.get("records_to_import") == null || recs.get("records_to_import").toString().equals(0) ? "Still Processing": recs.get("records_to_import").toString() %></td>
							<td><button dojoType="dijit.form.Button" iconClass="resetIcon" onclick="importCancel(<%= recs.get("id") %>);"><%= LanguageUtil.get(pageContext, "Cancel") %></button></td>
						</tr>
						<% } %>
						<tr>
							<td colspan="3"><%= LanguageUtil.get(pageContext, "Total-number-of-other-imports") %>: <%= iar.getOtherUsersJobs() %></td>
						</tr>
					</table>
				</div>
				<% } %>

      		<html:form action="/ext/contentlet/import_contentlets" styleId="importForm" method="POST" enctype="multipart/form-data">

				<input type="hidden" name="cmd" value="preview" />
				<input type="hidden" name="fileName" value="" />

			     <fieldset>

				<div>
		         	<dl>

			            <dt><%= LanguageUtil.get(pageContext, "Structure-to-Import") %>:</dt>
			            <dd>
			                <select dojoType="dijit.form.FilteringSelect" name="structure" id="structuresSelect" onchange="structureChanged()" value="<%= UtilMethods.isSet(form.getStructure()) ? form.getStructure() : "" %>" >
			<%
							for (Structure structure: structures) {
			%>
								<option <%=(selectedStructure !=null && selectedStructure.equals(structure.getInode())) ? "selected='true'" : "" %>value="<%= structure.getInode() %>"><%= structure.getName() %></option>
			<%
							}
			%>
			                </select>
			            </dd>
			    	</dl>
			        <dl id="importDetails">

			            <dt><%= LanguageUtil.get(pageContext, "Language-of-the-Contents-to-Import") %>:</dt>
			            <dd>
			                <select dojoType="dijit.form.FilteringSelect" name="language" id="languageSelect" onchange="languageChanged()" value="<%= UtilMethods.isSet(form.getLanguage()) ? form.getLanguage() : "" %>" >
			<%

							for (HashMap<String, Object> language: languagesMap) {
			%>
								<option value="<%= language.get("id") %>"><%= language.get("description") %></option>
			<%
							}
			%>
			                </select>
			                <div id="multiLingualImportNotes" style="display: none">
			                    <%= LanguageUtil.get(pageContext, "Note") %>:
			                    <p>
			                        <%= LanguageUtil.get(pageContext, "In-order-to-import-correctly-a-multilingual-file") %>:
			                        <ol>
			                            <li><%= LanguageUtil.get(pageContext, "The-CSV-file-must-saved-using--UTF-8--enconding") %></li>
			                            <li><%= LanguageUtil.get(pageContext, "There-CSV-file-must-have-two-extra-fields") %></li>
			                            <li><%= LanguageUtil.get(pageContext, "A-key-field-must-be-selected") %></li>
			                        </ol>
			                    </p>
			                </div>
			            </dd>

			            <dt><%= LanguageUtil.get(pageContext, "Key-Fields") %>:</dt>
			            <dd>
			                <table>
			                    <tbody id="import_fields_table"> </tbody>
			                </table>
			            </dd>

			            <dt><%= LanguageUtil.get(pageContext, "File-to-Import-CSV-File-Required") %>:</dt>
			            <dd>
			                <input type="file" name="file" id="file" /> <br/>
			                <a href="javascript: downloadCSVExample()"><%= LanguageUtil.get(pageContext, "Click-here-to-download-a-csv-sample-file") %></a>
			            </dd>

			            <dt>&nbsp;</dt>
			            <dd>
			                <button dojoType="dijit.form.Button" onclick="submitForm()" id="goToPreviewButton" iconClass="previewIcon">
			                  <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Go-to-Preview")) %>
			                </button>
			            </dd>
		       		</dl>
			        <dl id="cantImportMessage">
			        	<div class="warningText"><%= LanguageUtil.get(pageContext, "import-not-allowed-structure-has-madatory-scheme-no-default-action")%></div>
		       		</dl>
					</div>
		    	</fieldset>
			</html:form>


</liferay:box>
<script type="text/javascript">
	dojo.addOnLoad(function() {
		document.getElementById('cantImportMessage').style.display = "none";
		var structure = dijit.byId("structuresSelect").attr('value');
		if ((structure != null) && (structure != '')) {
			structureChanged(structure);
		}
		languageChanged();
	});
</script>