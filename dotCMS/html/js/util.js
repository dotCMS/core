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

var submitFormCount = 0;

function addItem(box, text, value, sort) {
	box[box.length] = new Option(text, value);

	if (sort == true) {
		sortBox(box);
	}
}

if (!Array.prototype.push) {
	function array_push() {
		for(var i = 0; i < arguments.length; i++) {
			this[this.length] = arguments[i];
		}

		return this.length;
	}

	Array.prototype.push = array_push;
}

if (!Array.prototype.pop) {
	function array_pop(){
		lastElement = this[this.length - 1];
		this.length = Math.max(this.length - 1, 0);

		return lastElement;
	}

	Array.prototype.pop = array_pop;
}

function autoComplete(box, text) {
	var prevStartPos;
	var prevMidPos;
	var prevEndPos;

	if ((box.length > 0) && (text != "")) {

		// The character '*' is ordered differently between Java and JavaScript

		text = text.toLowerCase().replace(/\*/g, "");

		// Use binary search to find the first instance of the selection

		var startPos = 0;
		var midPos = Math.floor(box.length / 2);
		var endPos = box.length;

		for (;;) {
			var sText = trimString(box.options[midPos].text.toLowerCase().replace(/\*/g, ""));

			if (midPos > 0) {
				var prevSText = trimString(box.options[midPos - 1].text.toLowerCase().replace(/\*/g, ""));

				if ((sText.indexOf(text) == 0) &&
					(prevSText.indexOf(text) != 0)) {

					box.selectedIndex = midPos;

					break;
				}
			}
			else {
				if (sText.indexOf(text) == 0) {
					box.selectedIndex = midPos;

					break;
				}
			}

			box.selectedIndex = -1;

			if (text < sText) {
				endPos = midPos;
				midPos = (Math.floor((endPos - startPos) / 2)) + startPos;
			}
			else if (text > sText) {
				startPos = midPos;
				midPos = (Math.floor((endPos - midPos) / 2)) + midPos;
			}

			if ((prevStartPos != null) && (prevMidPos != null) && (prevEndPos != null)) {

				// Break out of the loop when all positions repeat

				if ((prevStartPos == startPos) && (prevMidPos == midPos) && (prevEndPos == endPos)) {
					break;
				}
			}

			prevStartPos = startPos;
			prevMidPos = midPos;
			prevEndPos = endPos;
		}
	}
	else {
		box.selectedIndex = -1;
	}
}

function autoFill(fromBox, toBox) {
	i = fromBox.selectedIndex;

	if (i != -1) {
		s = fromBox.options[i].value;

		if (s != "") {
			to = toBox.value;

			if (to == "") {
				toBox.value = s;
			}
			else {
				toBox.value = to + ", " + s;
			}
		}
	}
}

function blink() {
	if (document.all) {
		var blinkArray = document.all.tags("blink");

		for (var i = 0; i < blinkArray.length; i++) {
			blinkArray[i].style.visibility = blinkArray[i].style.visibility == "" ? "hidden" : "";
		}
	}
}

if (document.all) {
	if(!dojo.isIE){//DOTCMS-5046
		setInterval("blink()", 750);
	}
}

function check(form, name, checked) {
	for (var i = 0; i < form.elements.length; i++) {
		var e = form.elements[i];

		if ((e.name == name) && (e.type == "checkbox")) {
			e.checked = checked;
		}
	}
}

function checkAll(form, name, allBox) {
	if (isArray(name)) {
		for (var i = 0; i < form.elements.length; i++) {
			var e = form.elements[i];

			if (e.type == "checkbox") {
				for (var j = 0; j < name.length; j++) {
					if (e.name == name[j]) {
						e.checked = allBox.checked;
					}
				}
			}
		}
	}
	else {
		for (var i = 0; i < form.elements.length; i++) {
			var e = form.elements[i];

			if ((e.name == name) && (e.type == "checkbox")) {
				e.checked = allBox.checked;
			}
		}
	}
}

function checkAllBox(form, name, allBox) {
	var totalBoxes = 0;
	var totalOn = 0;

	if (isArray(name)) {
		for (var i = 0; i < form.elements.length; i++) {
			var e = form.elements[i];

			if ((e.name != allBox.name) && (e.type == "checkbox")) {
				for (var j = 0; j < name.length; j++) {
					if (e.name == name[j]) {
						totalBoxes++;

						if (e.checked) {
							totalOn++;
						}
					}
				}
			}
		}
	}
	else {
		for (var i = 0; i < form.elements.length; i++) {
			var e = form.elements[i];

			if ((e.name != allBox.name) && (e.name == name) && (e.type == "checkbox")) {
				totalBoxes++;

				if (e.checked) {
					totalOn++;
				}
			}
		}
	}

	if (totalBoxes == totalOn) {
		allBox.checked = true;
	}
	else {
		allBox.checked = false;
	}
}

function checkTab(box) {
	if ((document.all) && (event.keyCode == 9)) {
		box.selection = document.selection.createRange();
		setTimeout("processTab(\"" + box.id + "\")", 0);
	}
}

function count(s, text) {
	if ((s == null) || (text == null)) {
		return 0;
	}

	var count = 0;

	var pos = s.indexOf(text);

	while (pos != -1) {
		pos = s.indexOf(text, pos + text.length);
		count++;
	}

	return count;
}

function disableEsc() {
	if ((document.all) && (event.keyCode == 27)) {
		event.returnValue = false;
	}
}

function disableFields(fields) {
	for (var i = 0; i < fields.length; i++) {
		if(!fields[i]) continue;
		if(fields[i].disabled)
			fields[i].disabled = true;
		if(fields[i].setAttribute)
			fields[i].setAttribute('disabled', true);

	}
}

function enableFields(fields) {
	for (var i = 0; i < fields.length; i++) {
		if(!fields[i]) continue;
		if(fields[i].disabled)
			fields[i].disabled = false;
		if(fields[i].setAttribute)
			fields[i].setAttribute('disabled', false);
	}
}

function getFileExtension(fileName){
	var pos = new String(fileName).lastIndexOf(".");
	var ext = new String(fileName).substr(pos + 1);
	return ext;
}

function getIndex(col, value) {
	for (var i = 0; i < col.length; i++) {
		if (col[i].value == value) {
			return i;
		}
	}

	return -1;
}

function getSelectedIndex(col) {
	for (var i = 0; i < col.length; i++) {
		if (col[i].checked == true) {
			return i;
		}
	}

	return -1;
}

function getSelectedRadioName(col) {
	var i = getSelectedIndex(col);

	if (i == -1) {
		var radioName = col.name;

		if (radioName == null) {
			radioName = "";
		}

		return radioName;
	}
	else {
		return col[i].name;
	}
}

function getSelectedRadioValue(col) {
	var i = getSelectedIndex(col);

	if (i == -1) {
		var radioValue = col.value;

		if (radioValue == null) {
			radioValue = "";
		}

		return radioValue;
	}
	else {
		return col[i].value;
	}
}

function hasItemWithText(box, text) {
	for (var i = 0; i < box.length; i ++) {
		if (box.options[i].text.toLowerCase() == text.toLowerCase()) {
			return true;
		}
	}

	return false;
}

function hasItemWithValue(box, value) {
	for (var i = 0; i < box.length; i ++) {
		if (box.options[i].value.toLowerCase() == value.toLowerCase()) {
			return true;
		}
	}

	return false;
}

function isArray(object) {
	if (!window.Array) {
		return false;
	}
	else {
		return object.constructor == window.Array;
	}
}

function isEven(x) {
	if ((x % 2) == 0) {
		return true;
	}

	return false;
}

function isOdd(x) {
	return !isEven(x);
}

function isRadioChecked(col) {
	var i = getSelectedIndex(col);

	return col[i].checked;
}

function isSelected(box) {
	if (box.selectedIndex >= 0) {
		return true;
	}
	else {
		return false;
	}
}

function listChecked(form) {
	var s = "";

	for (var i = 0; i < form.elements.length; i++) {
		var e = form.elements[i];

		if ((e.type == "checkbox") && (e.checked == true) && (e.value > "")) {
			s += e.value + ",";
		}
	}

	return s;
}

function listCheckedExcept(form, except) {
	var s = "";

	for (var i = 0; i < form.elements.length; i++) {
		var e = form.elements[i];

		if ((e.type == "checkbox") && (e.checked == true) && (e.value > "") && (e.name.indexOf(except) != 0)) {
			s += e.value + ",";
		}
	}

	return s;
}

function listSelect(box, delimeter) {
	var s = "";

	if (delimeter == null) {
		delimeter = ",";
	}

	if (box == null) {
		return "";
	}

	for (var i = 0; i < box.length; i++) {
    	if (box.options[i].value > "") {
			s += box.options[i].value + delimeter;
		}
	}

	if (s == ".none,") {
		return "";
	}
	else {
		return s;
	}
}

function listSelected(box, delimeter) {
	var s = "";

	if (delimeter == null) {
		delimeter = ",";
	}

	if (box == null) {
		return "";
	}

	for (var i = 0; i < box.length; i++) {
    	if ((box.options[i].value > "") && (box.options[i].selected)) {
			s += box.options[i].value + delimeter;
		}
	}

	if (s == ".none,") {
		return "";
	}
	else {
		return s;
	}
}

function loadPage(page) {
	if (page.indexOf("?") == -1) {
		page += "?";
	}

	page += "&r=" + random();

	parent.hidden_iframe.document.location = page;
}

function mathRound(n, places) {
	return n.toFixed(places);
}

function moveItem(fromBox, toBox, sort) {
	var newText = null;
	var newValue = null;
	var newOption = null;

	if (fromBox.selectedIndex >= 0) {
		for (var i = 0; i < fromBox.length; i++) {
			if (fromBox.options[i].selected) {
				newText = fromBox.options[i].text;
				newValue = fromBox.options[i].value;

				newOption = new Option(newText, newValue);

				toBox[toBox.length] = newOption;
			}
		}

		for (var i = 0; i < toBox.length; i++) {
			for (var j = 0; j < fromBox.length; j++) {
				if (fromBox[j].value == toBox[i].value) {
					fromBox[j] = null;

					break;
				}
			}
		}
	}

	if (newText != null) {
		if (sort == true) {
			sortBox(toBox);
		}
	}
}

function printCheck() {
	if (window.print) {
		return 1;
	}

	return 0;
}

function printWindow() {
	if (window.print) {
		window.print();
	}
}

function processTab(id) {
	document.all[id].selection.text = String.fromCharCode(9);
	document.all[id].focus();
}

function random() {
	return randomMinMax(0, 4294967296);
};

function randomMinMax(min, max) {
	return (Math.round(Math.random() * (max - min))) + min;
}

function redirect(form) {
	var url = form.options[form.options.selectedIndex].value;

	if (url != "null") {
		self.location = url;
	}
}

function removeItem(box, value) {
	if (value == null) {
		for (var i = box.length - 1; i >= 0; i--) {
			if (box.options[i].selected) {
				box[i] = null;
			}
		}
	}
	else {
		for (var i = box.length - 1; i >= 0; i--) {
			if (box.options[i].value == value) {
				box[i] = null;
			}
		}
	}
}

function reorder(box, down) {
	var si = box.selectedIndex;

	if (si == -1) {
		box.selectedIndex = 0;
	}
	else {
		sText = box.options[si].text;
		sValue = box.options[si].value;

		if ((box.options[si].value > "") && (si > 0) && (down == 0)) {
			box.options[si].text = box.options[si - 1].text;
			box.options[si].value = box.options[si - 1].value;
			box.options[si - 1].text = sText;
			box.options[si - 1].value = sValue;
			box.selectedIndex--;
		}
		else if ((si < box.length - 1) && (box.options[si + 1].value > "") && (down == 1)) {
			box.options[si].text = box.options[si + 1].text;
			box.options[si].value = box.options[si + 1].value;
			box.options[si + 1].text = sText;
			box.options[si + 1].value = sValue;
			box.selectedIndex++;
		}
		else if (si == 0) {
			for (var i = 0; i < (box.length - 1); i++) {
				box.options[i].text = box.options[i + 1].text;
				box.options[i].value = box.options[i + 1].value;
			}

			box.options[box.length - 1].text = sText;
			box.options[box.length - 1].value = sValue;

			box.selectedIndex = box.length - 1;
		}
		else if (si == (box.length - 1)) {
			for (var j = (box.length - 1); j > 0; j--) {
				box.options[j].text = box.options[j - 1].text;
				box.options[j].value = box.options[j - 1].value;
			}

			box.options[0].text = sText;
			box.options[0].value = sValue;

			box.selectedIndex = 0;
		}
	}
}

function selectAndCopy(el) {
	el.focus();
	el.select();

	if (document.all) {
		var textRange = el.createTextRange();

		textRange.execCommand("copy");
	}
}

function setBox(oldBox, newBox) {
	for (var i = oldBox.length - 1; i > -1; i--) {
		oldBox.options[i] = null;
	}

	for (var i = 0; i < newBox.length; i++) {
		oldBox.options[i] = new Option(newBox[i].value, i);
	}

	oldBox.options[0].selected = true;
}

function setSelectedValue(col, value) {
	for (var i = 0; i < col.length; i++) {
		if (col[i].value == value) {
			col.selectedIndex = i;

			break;
		}
	}
}

function sortBox(box) {
	var newBox = new Array();

	for (var i = 0; i < box.length; i++) {
		newBox[i] = new Array(box[i].value, box[i].text);
	}

	newBox.sort(sortByAscending);

	for (var i = box.length - 1; i > -1; i--) {
		box.options[i] = null;
	}

	for (var i = 0; i < newBox.length; i++) {
		box.options[box.length] = new Option(newBox[i][1], newBox[i][0]);
	}
}

function sortByAscending(a, b) {
	if (a[1].toLowerCase() > b[1].toLowerCase()) {
		return 1;
	}
	else if(a[1].toLowerCase() < b[1].toLowerCase()) {
		return -1;
	}
	else {
		return 0;
	}
}

function sortByDescending(a, b) {
	if (a[1].toLowerCase() > b[1].toLowerCase()) {
		return -1;
	}
	else if(a[1].toLowerCase() < b[1].toLowerCase()) {
		return 1;
	}
	else {
		return 0;
	}
}

function stripCarriageReturn(s) {
	return s.replace(/\r|\n|\r\n/g, " ");
}

function submitForm(form, action, singleSubmit) {
	if (submitFormCount == 0) {
		if (singleSubmit == null || singleSubmit) {
			submitFormCount++;

			for (var i = 0; i < form.length; i++){
				var e = form.elements[i];

				if (e.type != null && (e.type.toLowerCase() == "button" || e.type.toLowerCase() == "reset" || e.type.toLowerCase() == "submit")) {
					e.disabled=true;
				}
			}
		}

		if (action != null) {
			form.action = action;
		}

		form.submit();
	}
	else {
		if (this.submitFormAlert != null) {
  			submitFormAlert();
		}
	}
}

// String functions

function startsWith(str, x) {
	if (str.indexOf(x) == 0) {
		return true;
	}
	else {
		return false;
	}
}

function endsWith(str, x) {
	if (str.lastIndexOf(x) == str.length - x.length) {
		return true;
	}
	else {
		return false;
	}
}

function trimString(str) {
	str = str.replace(/^\s+/g, "").replace(/\s+$/g, "");

	var charCode = str.charCodeAt(0);

	while (charCode == 160) {
		str = str.substring(1, str.length);
		charCode = str.charCodeAt(0);
	}

	charCode = str.charCodeAt(str.length - 1);

	while (charCode == 160) {
		str = str.substring(0, str.length - 1);
		charCode = str.charCodeAt(str.length - 1);
	}

	return str;
}

//String.prototype.trim = trimString;

//Event related functions

function isRightClick (event) {
	if(navigator.platform.indexOf('Mac') != -1 || navigator.platform.indexOf('mac') != -1) {
		if(event.ctrlKey || event.button == 2)
			return true;
	} else 
		return !Event.isLeftClick(event);
}

function isLeftClick (event) {
	return Event.isLeftClick(event);
}

function nothing () {
	return false;
}

function getScrollX() {
  var scrOfX = 0, scrOfY = 0;
  if( typeof( window.pageYOffset ) == 'number' ) {
    //Netscape compliant
    scrOfX = top.window.pageXOffset;
  } else if( document.body.scrollLeft ) {
    //DOM compliant
    scrOfX = top.document.body.scrollLeft;
  } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
    //IE6 standards compliant mode
    scrOfX = top.document.documentElement.scrollLeft;
  }
  return scrOfX;
}

function getScrollY() {
  var scrOfY = 0;
  if( typeof( window.pageYOffset ) == 'number' ) {
    //Netscape compliant
    scrOfY = top.window.pageYOffset;
  } else if(top.document.body.scrollTop) {
    //DOM compliant
    scrOfY = top.document.body.scrollTop;
  } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
    //IE6 standards compliant mode
    scrOfY = top.document.documentElement.scrollTop;
  }
  return scrOfY;
}
//actively recording the mouse position

var _currentMouseX = 0;
var _currentMouseY = 0;

function captureMenuMove(event) {
	_currentMouseX = event.screenX; 
	_currentMouseY = event.screenY;
}

dojo.addOnLoad(function () {
	dojo.connect(dojo.body(), 'mousemove', captureMenuMove);
});

function getMouseXY () {
	return { mouseX: _currentMouseX, mouseY: _currentMouseY };
}

