<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Field" %>
<%@ page import="com.dotmarketing.util.WebKeys" %>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>

<%
	String structureInode = request.getParameter("structureInode");
	Structure st = StructureFactory.getStructureByInode(structureInode);
	List fields = st.getFields ();
%>


<div style="height: 450px; overflow: auto;">

	<table class="myVarTable">
		<tr>
			<td align="center">
                <button dojoType="dijit.form.Button" onClick="addIdentifierField('ContentIdentifier')">
                     <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                </button>
			</td>
			<td><%= LanguageUtil.get(pageContext, "Content-Identifier-value") %></td>
		</tr>
					
				<%
					Iterator it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TAG.toString())) {
				%>
				
	
					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td><%= field.getFieldName() %></td>
						</tr>
					

					<% } %>
				<% } %>

				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
				%>

					

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td><%= field.getFieldName() %></td>
						</tr>
						
					
					<% } %>
				<% } %>

                         <!-- // http://jira.dotmarketing.net/browse/DOTCMS-2869 -->
                <%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())) {
				%>

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td><%= field.getFieldName() %></td>
						</tr>
					
					<% } %>
				<% } %>

                         <!-- // http://jira.dotmarketing.net/browse/DOTCMS-3232 -->
                <%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
				%>

					

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('ConHostFolder')">
                                     <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= LanguageUtil.get(pageContext, "Host-Or-Folder-value") %>
							</td>
						</tr>
					
					<% } %>
				<% } %>

                         <!-- // http://jira.dotmarketing.net/browse/DOTCMS-3232 -->
                

				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
				%>
					

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="addImage('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> <%= LanguageUtil.get(pageContext, "Image") %> 
							</td>
						</tr>
						<tr>
							<td align="center"">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>ImageIdentifier')">
                                     <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= LanguageUtil.get(pageContext, "Image-Identifier") %>
							</td>
						</tr>

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>ImageWidth')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "Width") %>
							</td>
						</tr>


						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>ImageHeight')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "Height") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>ImageExtension')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "Image-Extension") %>
							</td>
						</tr>
					
					<% } %>
				<% } %>
				
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
				%>

					

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="addFile('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								 <%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "File") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>FileIdentifier')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "File-Identifier") %>
							</td>
						</tr>

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>FileExtension')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %> : <%= LanguageUtil.get(pageContext, "File-Extension") %>
							</td>
						</tr>
					

					<% } %>
				<% } %>
				<!-- http://jira.dotmarketing.net/browse/DOTCMS-2178 -->
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.BINARY.toString())) {
				%>

					

						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="addBinaryFile('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button> 
							</td>
							<td>
								<%= field.getFieldName() %>: <%= LanguageUtil.get(pageContext, "Binary-File") %> 
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="addBinaryResize('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %>: <%= LanguageUtil.get(pageContext, "Binary-File-Resized") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="addBinaryThumbnail('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %>: <%= LanguageUtil.get(pageContext, "Binary-File-Thumbnail") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>BinaryFileSize')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= field.getFieldName() %>: <%= LanguageUtil.get(pageContext, "Binary-File-Size") %>
							</td>
						</tr>
					
					<% } %>
				<% } %>
				
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.SELECT.toString()) || field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())) {
				%>
				

					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Selected-Value") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                               <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>SelectLabelsValues')"">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                               </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Dropdown Labels") %>&amp;Values, e.g.: USA|US Canada|CA
							</td>
						</tr>

						
					

				<%
						}
					}
				%>
				
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				%>


					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Selected-Value") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>RadioLabelsValues')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Radio-Button-Labels") %>&amp;<%= LanguageUtil.get(pageContext, "Example.message") %>
							</td>
						</tr>
					
				</fieldset>		
				<%
						}
					}
				%>
				
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				%>

					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
							<%=field.getFieldName()%> 	<%= LanguageUtil.get(pageContext, "Selected-Value") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>CheckboxLabelsValues')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
							<%=field.getFieldName()%> 	<%= LanguageUtil.get(pageContext, "Checkboxes Labels") %>&amp;Values, e.g.: USA|US Canada|CA
							</td>
						</tr>
					

				<%
						}
					}
				%>
				
				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
				%>

					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                               </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Date") %> <%= LanguageUtil.get(pageContext, "mm-dd-yyyy") %> 
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>DBFormat')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Date-Database-Format") %> <%= LanguageUtil.get(pageContext, "yyyy-mm-dd") %>
							</td>
						</tr>
					

				<%
						}
					}
				%>

				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
				%>

					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Time") %> <%= LanguageUtil.get(pageContext, "hh-mm-ss") %> 
							</td>
						</tr>
					

				<%
						}
					}
				%>

				<%
					it = fields.iterator();
					while (it.hasNext()) {
						Field field = (Field)it.next();
						if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				%>

					
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%=field.getFieldName()%> <%= LanguageUtil.get(pageContext, "Date") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                               <button dojoType="dijit.form.Button"  onClick="add('<%= field.getVelocityVarName() %>ShortFormat')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                               </button>
							</td>
							<td>
								<%= LanguageUtil.get(pageContext, "Date-Short-String") %> <%= LanguageUtil.get(pageContext, "mm-dd-yyyy") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>LongFormat')">
                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= LanguageUtil.get(pageContext, "Date Long String") %> <%= LanguageUtil.get(pageContext, "mm-dd-yyyy-hh-mm-ss") %>
							</td>
						</tr>
						<tr>
							<td align="center">
                                <button dojoType="dijit.form.Button" onClick="add('<%= field.getVelocityVarName() %>DBFormat')">
                                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add")) %>
                                </button>
							</td>
							<td>
								<%= LanguageUtil.get(pageContext, "Date-Database-Format") %> <%= LanguageUtil.get(pageContext, "yyyy-mm-dd") %>
							</td>
						</tr>
					

				<%
						}
					}
				%>

				
					<tr>
						<td align="center" nowrap colspan="2">
                            <button dojoType="dijit.form.Button" onClick="dijit.byId('variablesDialog').hide();">
                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
                            </button>
                            &nbsp;
						</td>
					</tr>
				</table>
	

</div>