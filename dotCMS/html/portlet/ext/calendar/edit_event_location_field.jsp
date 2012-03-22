<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page	import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page	import="com.dotmarketing.portlets.languagesmanager.business.*"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>

<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%
	Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

	Field field = (Field) request.getAttribute("field");
	Object value = (Object) request.getAttribute("value");
	String hint = UtilMethods.isSet(field.getHint()) ? field.getHint()
			: null;
	boolean isReadOnly = field.isReadOnly();
	String defaultValue = field.getDefaultValue() != null ? field
			.getDefaultValue().trim() : "";
	String fieldValues = field.getValues() == null ? "" : field
			.getValues().trim();
	String textValue = UtilMethods.isSet(value) ? value.toString() : "";

%>
	<div class="fieldWrapper">
		<div class="fieldName">

	<%
		if(field.isRequired()) {
	%>
		<span class="required"></span>
	<%
		}
	%>

		<%=field.getFieldName()%>:
	<%
		if (hint != null) {
	%>
		<a href="javascript: ;" id="<%=field.getFieldContentlet()%>HintHook">?</a>
		<div id="<%=field.getFieldContentlet()%>Hint" class="fieldHint"><%=hint%></div>
		<script type="text/javascript">
		 	var hintHook = $('<%=field.getFieldContentlet()%>HintHook');
		 	var hint = $('<%=field.getFieldContentlet()%>Hint');
		 	hintHook.observe('mouseover', showHint, hint);
		 	hintHook.observe('mouseout', hideHint, hint);
		</script>
	<%
	 	}

	%>


	</div>


	<div class="fieldValue">
		<input type="text" name="<%=field.getFieldContentlet()%>" id="<%=field.getFieldContentlet()%>"
			class="editTextField form-text <%= isReadOnly?"disabledField":"" %>"
			value="<%= textValue %>" <%= isReadOnly?"readonly=\"readonly\"":"" %> />
			<span id="showAllLocationsImg" class="plusIcon" onclick="showAllLocations();"></span>
		&nbsp;&nbsp;<a id="locationMapLink" href="javascript: showMap()" style="display: none;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "map-it")) %></a>
		<br />
		<div id="locationSuggestions" class="locationSuggestions" style="position: absolute; display: none;"></div>
		<div id="locationMap" class="locationMap" style="display: none; position: absolute;">
			<div id="locationMapControls"><a href="javascript: hideMap();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close-Map")) %></a></div>
			<div id="locationMapMessage"></div>
			<div id="locationMapCanvas" style="border: 1px; width: 300px; height: 300px;">
			</div>
		</div>
	</div>
	<div class="clear"></div>
</div>
