<%
/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
%>

<%@ include file="/html/portlet/polls/init.jsp" %>

<%
PollsQuestion question = (PollsQuestion)request.getAttribute(WebKeys.POLLS_QUESTION);

String questionId = null;
if (question != null) {
	questionId = question.getQuestionId();
}

String title = request.getParameter("question_title");
if ((title == null) || (title.equals("null"))) {
	title = "";

	if (question != null) {
		title = question.getTitle();
	}
}

String description = request.getParameter("question_desc");
if ((description == null) || (description.equals("null"))) {
	description = "";

	if (question != null) {
		description = question.getDescription();
	}
}

Calendar expirationDate = new GregorianCalendar();
expirationDate.add(Calendar.MONTH, 1);

if (question != null) {
	if (question.getExpirationDate() != null) {
		expirationDate.setTime(question.getExpirationDate());
	}
}

String expMonth = request.getParameter("question_exp_month");
if ((expMonth == null) || (expMonth.equals("null"))) {
	expMonth = "";

	if (expirationDate != null) {
		expMonth = Integer.toString(expirationDate.get(Calendar.MONTH));
	}
}

String expDay = request.getParameter("question_exp_day");
if ((expDay == null) || (expDay.equals("null"))) {
	expDay = "";

	if (expirationDate != null) {
		expDay = Integer.toString(expirationDate.get(Calendar.DATE));
	}
}

String expYear = request.getParameter("question_exp_year");
if ((expYear == null) || (expYear.equals("null"))) {
	expYear = "";

	if (expirationDate != null) {
		expYear = Integer.toString(expirationDate.get(Calendar.YEAR));
	}
}

boolean neverExpires = ParamUtil.get(request, "question_never_expires", true);
String neverExpiresParam = request.getParameter("question_never_expires");
if ((neverExpiresParam == null) || (neverExpiresParam.equals("null"))) {
	if (question != null) {
		if (question.getExpirationDate() != null) {
			neverExpires = false;
		}
	}
}

List choices = new ArrayList();
if (question != null) {
	choices = PollsChoiceManagerUtil.getChoices(questionId);
}

int numberOfChoices = 2;
if (choices.size() > 2) {
	numberOfChoices = choices.size();
}

try {
	numberOfChoices = Integer.parseInt(request.getParameter("n_of_choices"));
}
catch (NumberFormatException nfe) {
}

boolean delete = false;

int choiceId = 0;

try {
	choiceId = Integer.parseInt(request.getParameter("choice_id"));
}
catch (NumberFormatException nfe) {
}

if (choiceId > 0) {
	delete = true;
	numberOfChoices = numberOfChoices - 2;
}
%>

<script language="JavaScript">
	function <portlet:namespace />saveQuestion() {
	    updateExpDateHidden();
		document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = "<%= question == null ? Constants.ADD : Constants.UPDATE %>";
		document.<portlet:namespace />fm.<portlet:namespace />redirect.value = "<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_questions" /></portlet:renderURL>";
		document.<portlet:namespace />fm.<portlet:namespace />n_of_choices.value = "<%= numberOfChoices %>";
		submitForm(document.<portlet:namespace />fm);
	}
	
	function updateExpDateHidden() {
	    var date=dijit.byId('exp-date').value;
	    var month=date.getMonth();
	    var day=date.getDate();
	    var year=date.getFullYear();
	    dojo.byId('question_exp_day').value=day;
	    dojo.byId('question_exp_month').value=month;
	    dojo.byId('question_exp_year').value=year;
	}
	
	function toggleDisable() {
	    var el=dijit.byId('exp-date');
	    var dd=dijit.byId('never-expires').checked;
	    dijit.byId('exp-date').attr('disabled',dd);
        if(dd)
            document.<portlet:namespace />fm.<portlet:namespace />question_never_expires.value = '1'; 
        else 
            document.<portlet:namespace />fm.<portlet:namespace />question_never_expires.value = '0';
	}
	
</script>

<style media="all" type="text/css">
	@import url(/html/portlet/ext/contentlet/field/edit_field.css);
</style>

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/polls/edit_question" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm" onSubmit="<portlet:namespace />saveQuestion(); return false;">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
<input name="<portlet:namespace />redirect" type="hidden" value="">
<input name="<portlet:namespace />question_id" type="hidden" value="<%= questionId %>">
<input name="<portlet:namespace />question_never_expires" type="hidden" value="<%= neverExpires %>">
<input name="<portlet:namespace />n_of_choices" type="hidden" value="<%= (numberOfChoices + 1) %>">
<input name="<portlet:namespace />choice_id" type="hidden" value="">

<div class="shadowBoxLine headerBox">

	 	<div style="padding:7px;">
		 	<div>

				<h3><%= LanguageUtil.get(pageContext, "add-question") %></h3>
		  	</div>
				<br clear="all">
	  	</div>
	<c:if test="<%= !SessionErrors.isEmpty(renderRequest) %>">
		<div class="noResultsMessage fieldAlert"><%= LanguageUtil.get(pageContext, "you-have-entered-invalid-data") %></div>
	</c:if>

	<dl style="padding-top:10px;">
		<dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
		<dd><input class="form-text" name="<portlet:namespace />question_title" type="title" style="width:375px;" size="70" value="<%= title %>"></dd>
		<c:if test="<%= SessionErrors.contains(renderRequest, QuestionTitleException.class.getName()) %>">
			<dd class="inputCaption fieldAlert"><%= LanguageUtil.get(pageContext, "please-enter-a-valid-question") %></dd>
		</c:if>
		
		<dt><%= LanguageUtil.get(pageContext, "question") %>:</dt>
		<dd><textarea class="form-text" cols="70" name="<portlet:namespace />question_desc" rows="5" style="width:375px;" wrap="soft"><%= GetterUtil.getString(description) %></textarea></dd>
		<c:if test="<%= SessionErrors.contains(renderRequest, QuestionDescriptionException.class.getName()) %>">
			<dd class="inputCaption fieldAlert"><%= LanguageUtil.get(pageContext, "please-enter-a-valid-question-description") %></dd>
		</c:if>
	
		<dt><%= LanguageUtil.get(pageContext, "expiration-date") %>:</dt>
		<dd>

        <%
            int dd=Integer.parseInt(expDay);
            int mm=Integer.parseInt(expMonth);
            int yy=Integer.parseInt(expYear);
        %>

		<input type="text" value="<%= String.format("%4d-%02d-%02d",yy,mm+1,dd)%>" 
			       onChange="updateExpDateHidden()" id="exp-date"
			       dojoType="dijit.form.DateTextBox" 
			       name="exp-date" style="width:100px;">
        
        <input type="hidden" value="<%= dd %>" 
               name="<%= renderResponse.getNamespace() + "question_exp_day" %>"
               id="question_exp_day"/>
        <input type="hidden" value="<%= mm %>" 
               name="<%= renderResponse.getNamespace() + "question_exp_month" %>"
               id="question_exp_month"/>
        <input type="hidden" value="<%= yy %>" 
               name="<%= renderResponse.getNamespace() + "question_exp_year" %>"
               id="question_exp_year"/>
        
			&nbsp;&nbsp;&nbsp;&nbsp;
			<input <%= (neverExpires) ? "checked" : "" %> 
			       type="checkbox" 
			       dojoType="dijit.form.CheckBox" 
			       name="never-expires" 
			       id="never-expires" 
			       onClick="toggleDisable();"/>			
			
			<%= LanguageUtil.get(pageContext, "never-expires") %> 
		</dd>
		<c:if test="<%= SessionErrors.contains(renderRequest, QuestionExpirationDateException.class.getName()) %>">
			<dd class="inputCaption fieldAlert"><%= LanguageUtil.get(pageContext, "please-enter-a-valid-expiration-date") %></dd>
		</c:if>
		
		<dt><%= LanguageUtil.get(pageContext, "choices") %>:</dt>
		<%
		for (int i = 1; i <= numberOfChoices; i++) {
			char c = (char)(96 + i);
	
			String choiceDesc = null;
	
			if (delete) {
				if (i < choiceId) {
					choiceDesc = request.getParameter("choice_desc_" + c);
				}
				else {
					choiceDesc = request.getParameter("choice_desc_" + ((char)(96 + i + 1)));
				}
			}
			else {
				choiceDesc = request.getParameter("choice_desc_" + c);
			}
	
			if (Validator.isNull(choiceDesc)) {
				if (question != null) {
					if (i - 1 < choices.size()) {
						PollsChoice choice = (PollsChoice)choices.get(i - 1);
	
						choiceDesc = choice.getDescription();
					}
				}
			}
		%>
			<dd>
				<%= c %>.
				<input name="<portlet:namespace />choice_id_<%= c %>" type="hidden" value="<%= c %>">
				<input class="form-text" name="<portlet:namespace />choice_desc_<%= c %>" size="50" style="width:250px;" type="text" value="<%= GetterUtil.getString(choiceDesc) %>">
				<c:if test="<%= numberOfChoices > 2 %>">
					<button dojoType="dijit.form.Button" iconClass="minusIcon" onClick="document.<portlet:namespace />fm.<portlet:namespace />choice_id.value = '<%= i %>'; submitForm(document.<portlet:namespace />fm);">
						<%= LanguageUtil.get(pageContext, "remove") %>
					</button>
				</c:if>
			</dd>
		<% } %>
	
		<c:if test="<%= SessionErrors.contains(renderRequest, QuestionChoiceException.class.getName()) %>">
			<dd class="inputCaption fieldAlert"><%= LanguageUtil.get(pageContext, "please-enter-valid-choices") %></dd>
		</c:if>
	
	
		<dt>&nbsp;</dt>
		<dd style="padding-left:150px;padding-bottom:20px;">
			<button dojoType="dijit.form.Button" iconClass="plusIcon" onClick="submitForm(document.<portlet:namespace />fm);">
				<%= LanguageUtil.get(pageContext, "add-choice") %>
			</button>
		</dd>
		
	</dl>

</div>

<div class="buttonRow">
	<c:if test="<%= question == null %>">
		<button dojoType="dijit.form.Button" iconClass="saveIcon" onClick="<portlet:namespace />saveQuestion();"><%= LanguageUtil.get(pageContext, "save") %></button>
		
		<button dojoType="dijit.form.Button" iconClass="saveIcon" onClick="document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = '<%= Constants.ADD %>'; document.<portlet:namespace />fm.<portlet:namespace />redirect.value = '<portlet:actionURL><portlet:param name="struts_action" value="/polls/edit_question" /></portlet:actionURL>'; document.<portlet:namespace />fm.<portlet:namespace />n_of_choices.value = '<%= numberOfChoices %>'; submitForm(document.<portlet:namespace />fm);">
			<%= LanguageUtil.get(pageContext, "save-and-add-another") %>
		</button>
	</c:if>
	
	<c:if test="<%= question != null %>">
		<button dojoType="dijit.form.Button" iconClass="saveIcon" onClick="<portlet:namespace />saveQuestion();">
			<%= LanguageUtil.get(pageContext, "update") %>
		</button>
		
		<button dojoType="dijit.form.Button" iconClass="deleteIcon" onClick="document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = '<%= Constants.DELETE %>'; document.<portlet:namespace />fm.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_questions" /></portlet:renderURL>'; submitForm(document.<portlet:namespace />fm);">
			<%= LanguageUtil.get(pageContext, "delete") %>
		</button>
	</c:if>
	
	<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_questions" /></portlet:renderURL>';">
		<%= LanguageUtil.get(pageContext, "cancel") %>
	</button>
</div>

</form>

<script language="JavaScript">
	dojo.addOnLoad (function(){		
		setTimeout("document.<portlet:namespace />fm.<portlet:namespace />question_title.focus()",500);		
	});	
</script>
