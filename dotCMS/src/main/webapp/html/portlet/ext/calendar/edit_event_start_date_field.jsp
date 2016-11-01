<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page	import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
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
%>



<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>

<div class="fieldWrapper">
	<div class="fieldName">

	<%if(field.isRequired()) {%>
		<span class="required"></span>
	<%}%>

		<%=field.getFieldName()%>:
	<%if (hint != null) {%>
		<a href="javascript: ;" id='<%=field.getFieldContentlet()%>HintHook'>?</a>
		<div id='<%=field.getFieldContentlet()%>Hint' class="fieldHint"><%=hint%></div>
		<script type="text/javascript">
		 	var hintHook = $('<%=field.getFieldContentlet()%>HintHook');
		 	var hint = $('<%=field.getFieldContentlet()%>Hint');
		 	hintHook.observe('mouseover', showHint, hint);
		 	hintHook.observe('mouseout', hideHint, hint);
		</script>
	<%}
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
	Date dateValue = new Date();
	if(value instanceof String && value != null) {
		dateValue = df.parse((String) value);
	} else if(value != null) {
		dateValue = (Date)value;
	}



	GregorianCalendar cal = new GregorianCalendar();
	cal.setTime((Date) dateValue);
	int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
	int month = cal.get(GregorianCalendar.MONTH) + 1;
	int year = cal.get(GregorianCalendar.YEAR) ;
	%>

</div>


<div class="fieldValue">

	<input type="hidden" id="<%=field.getVelocityVarName()%>"
		name="<%=field.getFieldContentlet()%>"
		value="<%= df.format(dateValue) %>" />
	<%if (field.getFieldType().equals(Field.FieldType.DATE.toString())
		 			|| field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) { %>
		 <input type="text"
			value="<%= df2.format(dateValue) %>"
		 	onChange="updateDate('<%=field.getVelocityVarName()%>');updateStartRecurrenceDate('<%=field.getVelocityVarName()%>');"
		 	dojoType="dijit.form.DateTextBox"
		 	name="<%=field.getFieldContentlet()%>Date"
		 	id="<%=field.getVelocityVarName()%>Date"
		 	style="width:120px;">
	<%}%>

	<%if (field.getFieldType().equals(Field.FieldType.TIME.toString())
			|| field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {

		String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
        String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);%>
		<input type="text" id='<%=field.getVelocityVarName()%>Time'
			name='<%=field.getFieldContentlet()%>Time'
			onChange="updateDate('<%=field.getVelocityVarName()%>'); updateStartRecurrenceDate('<%=field.getVelocityVarName()%>');"
			value='T<%=hour+":"+min%>:00'

			dojoType="dijit.form.TimeTextBox" style="width: 100px;"
			<%=field.isReadOnly()?"disabled=\"disabled\"":""%>/>

		<input type="checkbox" dojoType="dijit.form.CheckBox" name="<%=field.getFieldContentlet()%>AllDay" id="alldayevent" value="true" onclick="setAllDayEvent()"/><label for="alldayevent"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All-day-event")) %></label>

	<%}%>

</div>
<div class="clear"></div>
</div>