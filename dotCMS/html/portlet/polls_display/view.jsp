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

<%@ include file="/html/portlet/polls_display/init.jsp" %>

<%
PollsDisplay pollsDisplay = (PollsDisplay)request.getAttribute(WebKeys.POLLS_DISPLAY);
PollsQuestion question = PollsQuestionManagerUtil.getQuestion(pollsDisplay.getQuestionId());
List choices = PollsChoiceManagerUtil.getChoices(question.getQuestionId());

String voteUserId = "";

boolean hasVoted = false;
try {
	hasVoted = PollsQuestionManagerUtil.hasVoted(question.getQuestionId());

	voteUserId = user.getUserId();
}
catch (PrincipalException pe) {
	if (session.getAttribute(PollsQuestion.class.getName() + "." + question.getQuestionId() + "._voted") != null) {
		hasVoted = true;
	}

	voteUserId = Long.toString(CounterManagerUtil.increment(PollsQuestion.class.getName() + ".anonymous"));
}

if (!hasVoted) {
	String cmd = ParamUtil.getString(request, Constants.CMD);

	if (cmd.equals(Constants.ADD)) {
		String choiceId = ParamUtil.getString(request, "choice_id");

		try  {
			PollsQuestionLocalManagerUtil.vote(voteUserId, question.getQuestionId(), choiceId);

			SessionMessages.add(renderRequest, "vote_added");

			session.setAttribute(PollsQuestion.class.getName() + "." + question.getQuestionId() + "._voted", new Boolean(true));

			hasVoted = true;
		}
		catch (DuplicateVoteException dve) {
			SessionErrors.add(renderRequest, dve.getClass().getName());
		}
		catch (NoSuchChoiceException nsce) {
			SessionErrors.add(renderRequest, nsce.getClass().getName());
		}
		catch (PrincipalException pe) {
			SessionErrors.add(renderRequest, "vote_requires_user_id");
		}
	}
}
%>

<c:if test="<%= !hasVoted %>">
	<table border="0" cellpadding="4" cellspacing="0" width="95%">

	<form action="<portlet:actionURL><portlet:param name="struts_action" value="/polls_display/view" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="<%= Constants.ADD %>">
	<input name="<portlet:namespace />question_id" type="hidden" value="<%= question.getQuestionId() %>">

	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="bg" size="2">
					<%= question.getDescription() %>
					</font>
				</td>
			</tr>
			<tr>
				<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			</table>

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchChoiceException.class.getName()) %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-neg-alert"><%= LanguageUtil.get(pageContext, "please-select-an-option") %></span></font>
					</td>
				</tr>
				<tr>
					<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
				</table>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, DuplicateVoteException.class.getName()) %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-neg-alert"><%= LanguageUtil.get(pageContext, "you-may-only-vote-once") %></span></font>
					</td>
				</tr>
				<tr>
					<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
				</table>
			</c:if>

			<table border="0" cellpadding="0" cellspacing="0">

			<%
			Iterator itr = choices.iterator();

			while (itr.hasNext()) {
				PollsChoice choice = (PollsChoice)itr.next();
			%>

				<tr>
					<td valign="top">
						<input name="<portlet:namespace />choice_id" type="radio" value="<%= choice.getChoiceId() %>">
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td valign="top">
						<font class="bg" size="2"><b>
						<%= choice.getChoiceId() %>.
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td valign="top">
						<font class="bg" size="2">
						<%= choice.getDescription() %>
						</font>
					</td>
				</tr>

			<%
			}
			%>

			<tr>
				<td colspan="3"><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			</table>

			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
                    <button dojoType="dijit.form.Button" onClick="submitForm(document.<portlet:namespace />fm);"><%= LanguageUtil.get(pageContext, "vote") %></button>
				</td>
			</tr>
			</table>
		</td>
	</tr>

	</form>

	</table>
</c:if>

<c:if test="<%= hasVoted %>">

	<%
	NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
	NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);
	%>

	<table border="0" cellpadding="4" cellspacing="0" width="95%">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="gamma" size="2">
					<%= question.getDescription() %>
					</font>
				</td>
			</tr>
			<tr>
				<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			</table>

			<c:if test="<%= SessionMessages.contains(renderRequest, \"vote_added\") %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-pos-alert"><%= LanguageUtil.get(pageContext, "thank-you-for-your-vote") %></span></font>
					</td>
				</tr>
				<tr>
					<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
				</table>
			</c:if>

			<table border="0" cellpadding="4" cellspacing="0" width="100%">
			<tr>
				<td class="beta">
					<table border="0" cellpadding="4" cellspacing="0" width="100%">
					<tr class="beta">
						<td>
							<font class="beta" size="2"><b>
							%
							</b></font>
						</td>
						<td colspan="3">
							<font class="beta" size="2"><b>
							<%= LanguageUtil.get(pageContext, "votes") %>
							</b></font>
						</td>
					</tr>

					<%
					int totalVotes = PollsVoteManagerUtil.getVotesSize(question.getQuestionId());

					for (int i = 0; i < choices.size(); i++) {
						PollsChoice choice = (PollsChoice)choices.get(i);

						int choiceVotes = PollsVoteManagerUtil.getVotesSize(question.getQuestionId(), choice.getChoiceId());

						String className = "gamma";
						if (MathUtil.isOdd(i)) {
							className = "bg";
						}

						double votesPercent = 0.0;
						if (totalVotes > 0) {
							votesPercent = (double)choiceVotes / totalVotes;
						}

						int votesPixelWidth = 35;
						int votesPercentWidth = (int)Math.floor(votesPercent * votesPixelWidth);
					%>

						<tr class="<%= className %>">
							<td valign="top">
								<font class="<%= className %>" size="2">
								<%= percentFormat.format(votesPercent) %>
								</font>
							</td>
							<td colspan="<%= votesPercentWidth > 0 ? "1" : "2" %>" valign="top">
								<font class="<%= className %>" size="2">
								<%= numberFormat.format(choiceVotes) %>
								</font>
							</td>

							<c:if test="<%= votesPercentWidth > 0 %>">
								<td valign="middle">
									<table border="0" cellpadding="0" cellspacing="0">
									<tr>
										<td class="alpha"><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="<%= votesPercentWidth %>"></td>
										<td class="beta"><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="<%= votesPixelWidth - votesPercentWidth %>"></td>
									</tr>
									</table>
								</td>
							</c:if>

							<td valign="top" width="99%">
								<font class="<%= className %>" size="2">
								<%= choice.getDescription() %>
								</font>
							</td>
						</tr>

					<%
					}
					%>

					</table>
				</td>
			</tr>
			</table>

			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			<tr>
				<td>
					<font class="gamma" size="2">
					<b><%= LanguageUtil.get(pageContext, "total-votes") %>:</b>
					</font>
				</td>
				<td width="10">&nbsp;
					
				</td>
				<td>
					<font class="gamma" size="2">
					<%= numberFormat.format(totalVotes) %>
					</font>
				</td>
			</tr>
			</table>
		</td>
	</tr>
	</table>
</c:if>

<c:if test="<%= pollsDisplayAdmin %>">
	<table width="100%">
	<tr>
		<td align="right"><font class="bg" size="1">[<a class="bg" href="<portlet:actionURL><portlet:param name="struts_action" value="/polls_display/setup" /></portlet:actionURL>" styleClass="bg"><bean:message key="setup" /></a>]</font></td>
	</tr>
	</table>
</c:if>