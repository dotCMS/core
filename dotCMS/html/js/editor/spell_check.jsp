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

<%@ page import="com.liferay.portlet.words.util.WordsUtil" %>
<%@ page import="com.liferay.util.ParamUtil" %>
<%@ page import="com.liferay.util.StringUtil" %>
<%@ page import="com.liferay.util.jazzy.InvalidWord" %>

<%@ page import="java.util.List" %>

<%
String text = ParamUtil.getString(request, "text");
text = StringUtil.replace(text, "\n", "\r\n");

List invalidWords = WordsUtil.checkSpelling(text);
%>

<html>
<head>
	<title>Spell Check</title>
	<style type="text/css">
		.button { width: 100px; }
	</style>

	<script language="JavaScript" src="../sniffer.js"></script>
	<script language="JavaScript" src="../util.js"></script>

	<script language="JavaScript">
		var invalidWords = new Array();
		var invalidWordsPos = new Array();

		<%
		for (int i = 0; i < invalidWords.size(); i++) {
			InvalidWord invalidWord = (InvalidWord)invalidWords.get(i);
		%>

			invalidWords[<%= i %>] = "<%= invalidWord.getInvalidWord() %>";
			invalidWordsPos[<%= i %>] = <%= invalidWord.getWordContextPosition() %>;

		<%
			List suggestions = invalidWord.getSuggestions();
		%>

			var suggestions_<%= i %> = new Array();

		<%
			for (int j = 0; j < suggestions.size(); j++) {
				String suggestion = (String)suggestions.get(j);
		%>

				suggestions_<%= i %>[<%= j %>]= new Option("<%= suggestion %>", "<%= suggestion %>");

		<%
			}
		}
		%>

		var invalidWordsCur = 0;
		var invalidWordsSize = <%= invalidWords.size() %>;

		var ignoreWords = new Array();
		var changeWords = new Array();

		var spanInvalidBegin = (is_ie) ? "<SPAN class=spell-check-error id=spellCheckError>" : "<SPAN class=\"spell-check-error\" id=\"spellCheckError\">";
		var spanInvalidEnd = "</SPAN>";

		function init() {
			var html = "";
			html += "<html>\n";
			html += "<head>\n";
			html += "\t<style type='text/css'>\n";
			html += "\t\t.spell-check-error { background-color: #99BBEE; border-bottom: 1px dashed #FF0000; cursor: default; font: bold; }\n";
			html += "\t</style>\n";
			html += "</head>\n";
			html += "<body bgcolor='#FFFFFF'>";
			html += opener.getHTML();
			html += "</body>\n";
			html += "\n";
			html += "</html>\n";

			textArea.document.open();
			textArea.document.write(html);
			textArea.document.close();

			window.status = " ";

			if (invalidWordsCur == invalidWordsSize) {
				//alert("The spell check is complete.");
				window.status = "The spell check is complete.";
			}
			else {
				highlight();
			}
		}

		function change() {
			var invalidWord = invalidWords[invalidWordsCur];
			var validWord = document.fm.change_to.value;

			replace(invalidWord, validWord);
		}

		function changeAll() {
			var invalidWord = invalidWords[invalidWordsCur];
			var validWord = document.fm.change_to.value;

			changeWords[invalidWord] = validWord;

			replace(invalidWord, validWord);
		}

		function finish() {
			var html = document.getElementById("textArea").contentWindow.document.body.innerHTML;

			var x = html.indexOf(spanInvalidBegin);
			var y = html.indexOf(spanInvalidEnd, x);

			if ((x != -1) && (y != -1)) {
				html = html.substring(0, x) + html.substring(x + spanInvalidBegin.length, y) + html.substring(y + spanInvalidEnd.length, html.length);
			}

			opener.setHTML(html);

			window.close();
		}

		function ignore() {
			var invalidWord = invalidWords[invalidWordsCur];

			replace(invalidWord, invalidWord);
		}

		function ignoreAll() {
			var invalidWord = invalidWords[invalidWordsCur];

			ignoreWords[invalidWord] = true;

			replace(invalidWord, invalidWord);
		}

		function highlight() {
			var html = document.getElementById("textArea").contentWindow.document.body.innerHTML;

			var invalidWord = invalidWords[invalidWordsCur];

			var x = invalidWordsPos[invalidWordsCur];
			var y = x + invalidWord.length;

			html = html.substring(0, x) + spanInvalidBegin + invalidWord + spanInvalidEnd + html.substring(y, html.length);

			document.getElementById("textArea").contentWindow.document.body.innerHTML = html;

			var testEl = textArea.document.getElementById("spellCheckError");

			var testX = testEl.offsetLeft;
			var testY = testEl.offsetTop;

			while ((testEl = testEl.offsetParent)) {
				testX += testEl.offsetLeft;
				testY += testEl.offsetTop;
			}

			textArea.window.scrollTo(testX, testY);

			eval("setBox(document.fm.suggestions, suggestions_" + invalidWordsCur + ");");
			document.fm.change_to.value = document.fm.suggestions[document.fm.suggestions.selectedIndex].text;
		}

		function replace(oldWord, newWord) {
			var html = document.getElementById("textArea").contentWindow.document.body.innerHTML;

			var x = invalidWordsPos[invalidWordsCur];
			var y = x + spanInvalidBegin.length + oldWord.length + spanInvalidEnd.length;

			html = html.substring(0, x) + newWord + html.substring(y, html.length);

			document.getElementById("textArea").contentWindow.document.body.innerHTML = html;

			var wordDiff = newWord.length - oldWord.length;

			for (var i = invalidWordsCur; i < invalidWordsSize; i++) {
				invalidWordsPos[i] = invalidWordsPos[i] + wordDiff;
			}

			invalidWordsCur++;

			if (invalidWordsCur == invalidWordsSize) {
				//alert("The spell check is complete.");
				window.status = "The spell check is complete.";
			}
			else {
				highlight();

				if (ignoreWords[invalidWords[invalidWordsCur]] == true) {
					ignore();
				}

				if (changeWords[invalidWords[invalidWordsCur]] != null) {
					document.fm.change_to.value = changeWords[invalidWords[invalidWordsCur]];

					change();
				}
			}
		}
	</script>
</head>

<body bgcolor="Silver" leftmargin="0" marginheight="0" marginwidth="0" rightmargin="0" topmargin="0" onLoad="init();">

<form name="fm">

<table border="0" cellpadding="0" cellspacing="0">
<tr>
	<td>
		<iframe height="300" id="textArea" name="textArea" src="../../common/null.html" width="640"></iframe>
	</td>
</tr>
</table>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr>
	<td align="center">
		<table border="0" cellpadding="0" cellspacing="2">
		<tr>
			<td>
				<font face="MS Sans Serif" size="1">Change To:</font>
			</td>
		</tr>
		<tr>
			<td>
				<input name="change_to" type="text" style="width: 300px;">
			</td>
		</tr>
		<tr>
			<td></td>
		</tr>
		<tr>
			<td>
				<font face="MS Sans Serif" size="1">Suggestions:</font>
			</td>
		</tr>
		<tr>
			<td>
				<select name="suggestions" size="5" style="width: 300px;" onChange="document.fm.change_to.value = document.fm.suggestions[document.fm.suggestions.selectedIndex].text;">
				</select>
			</td>
		</tr>
		</table>
	</td>
	<td align="center">
		<input class="button" type="button" value="Ignore" onClick="ignore();"> <input class="button" type="button" value="Ignore All" onClick="ignoreAll();"><br><br>

		<input class="button" type="button" value="Change" onClick="change();"> <input class="button" type="button" value="Change All" onClick="changeAll();"><br><br>

		<input class="button" type="button" value="Finish" onClick="finish();"> <input class="button" type="button" value="Cancel" onClick="self.close();">
	</td>
</tr>
</table>

</form>

</body>

</html>