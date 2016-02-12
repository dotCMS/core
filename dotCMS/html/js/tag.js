function tagAction(action, element, returnDiv) {
		var tagName = document.getElementById(element).value;
		tagName = RTrim(tagName);
		tagName = LTrim(tagName);
		if (tagName != "") {
				var doAction = false;
				var fullCommand = "false";
				try {
						checkFullCommand();
						fullCommand = document.getElementById("fullCommand").value;
				} catch (e) {}
				if (fullCommand == "true") {
						if (action == "add") {
								TagAjax.addTagFullCommand(tagName, userId);
								returnAction = "added";
						} else if (action == "remove") {
								TagAjax.deleteTagFullCommand(tagName);
								returnAction = "removed";
						}
						returnAction = "Tags " + returnAction;
						displayTagReturn(returnDiv, returnAction, 20);
				} else {
						if ((cbUserIdList != null) || (0 < cbUserIdList.length)) {
								var returnAction = "added";
								var users = 0;
								for (var i = 0; i < cbUserIdList.length; ++i) {
										if (document.getElementById(cbUserIdList[i]).checked) {
												doAction = true;
												if (action == "add") {
														users++;
														addTag(tagName, cbUserProxyInodeList[i]);
														returnAction = "added";
												} else if (action == "remove") {
														users++;
														deleteTagInode(tagName, cbUserProxyInodeList[i]);
														returnAction = "removed";
												}
										}
								}
								if (users > 1) {
										returnAction = "Tags " + returnAction;
								} else {
										returnAction = "Tag " + returnAction;
								}
								displayTagReturn(returnDiv, returnAction, 20);
						}
						if (!doAction) {
								displayTagReturn(returnDiv, "", 0);
								alert("There's no user checked");
						}
				}
		} else {
				displayTagReurn(returnDiv, "", 0);
				alert("There's no tag selected");
		}
}

function deleteTagInode(tagName, inode) {
		TagAjax.deleteTagInode(tagName, inode);
}

function addTag(tagName, inode) {
		TagAjax.addTag(tagName, userId, inode);
}

function displayTagReturn(div, message, height) {
		if (document.layers) { //NN4+
				var obj = document.layers[div];
				var objMsg = document.layers[div];
				if (message == "") {
						obj.visibility = "hide";
						objMsg.innerHTML = "";
						obj.height = "0px";
				} else {
						obj.visibility = "show";
						objMsg.innerHTML = message;
						obj.height = height + "px";
				}
		} else if (document.getElementById) { //gecko(NN6) + IE 5+
				var obj = document.getElementById(div);
				var objMsg = document.getElementById(div);
				if (message == "") {
						obj.style.display = "none";
						objMsg.innerHTML = "";
						obj.style.height = "0px";
				} else {
						obj.style.display = "inline";
						objMsg.innerHTML = message;
						obj.style.height = height + "px";
				}
		} else if (document.all) { // IE 4
				var obj = document.all[div]
				var objMsg = document.all[div];
				if (message == "") {
						obj.style.display = "none";
						objMsg.innerHTML = "";
						obj.style.height = "0px";
				} else {
						obj.style.display = "inline";
						objMsg.innerHTML = message;
						obj.style.height = height + "px";
				}
		}
}

var tagVelocityVarName;
var suggestedDiv;
var tagsContainer;
var tagsMap = {};

function suggestTagsForSearch(e) {
		if (!tagVelocityVarName || tagVelocityVarName == "") {
				tagVelocityVarName = e.target.id;
		}
		if (!tagsContainer || tagsContainer == "") {
				tagsContainer = document.getElementById("widget_" + tagVelocityVarName);
		}
		suggestedDiv = tagVelocityVarName + "suggestedTagsDiv";
		var inputTags = document.getElementById(tagVelocityVarName).value;
		inputTags = RTrim(inputTags);
		inputTags = LTrim(inputTags);
		var tagName = "";
		if (inputTags != "") {
				var arrayTags = inputTags.split(",");
				if (arrayTags.length >= 1) {
						tagName = arrayTags[arrayTags.length - 1];
						tagName = RTrim(tagName);
						tagName = LTrim(tagName);
				}
		}
		//in backend, check if structure has host or folder field and dynamically suggest tags for that field"s value
		var selectedHostOrFolderId = "";

		if (dojo.byId("hostId")) {
				selectedHostOrFolderId = dojo.byId("hostId").value;
				if (selectedHostOrFolderId == "") {
						selectedHostOrFolderId = dojo.byId("folderInode").value;
				}
		}

		//in frontend, check for hidden field with host identifier
		if (dojo.byId("currentHostIdForTagSuggestion")) {
				selectedHostOrFolderId = dojo.byId("currentHostIdForTagSuggestion").value;
		}

		if (e.keyCode === 13) {
				useThisTagForSearch(event);
		} else if (tagName.length >= 3) {
				TagAjax.getSuggestedTag(tagName, selectedHostOrFolderId, showTagsForSearch);
		} else {
				clearSuggestTagsForSearch();
		}
}

function showTagsForSearch(result) {

		if (result.length > 0) {
				var tags = "<h3>Tags</h3>";
				var personasTags = "<h3>Personas</h3>";
				result.each(function(tag) {
						var tagName = tag.tagName;
						tagName = RTrim(tagName);
						tagName = LTrim(tagName);
						if (tag.persona) {
								personasTags += "<a class=\"persona\" onClick=\"useThisTagForSearch(event)\">" + tagName + "</a>";
						} else {
								tags += "<a onClick=\"useThisTagForSearch(event)\">" + tagName + "</a>";
						}
				});

				if (tagVelocityVarName) {
						var tagDiv = document.getElementById(suggestedDiv);
						tagDiv.innerHTML = tags + personasTags;

						if (dojo.byId(tagVelocityVarName + "suggestedTagsWrapper")) {
								dojo.style(tagVelocityVarName + "suggestedTagsWrapper", "display", "block");
								dojo.style(tagVelocityVarName + "suggestedTagsWrapper", "left", getInputPosition());
								dojo.style(tagVelocityVarName + "suggestedTagsWrapper", "top", getInputHeight());
						}
				}
		} else {
				clearSuggestTagsForSearch();
		}
}

function clearSuggestTagsForSearch() {
		if (tagVelocityVarName) {

				if (dojo.byId(tagVelocityVarName + "suggestedTagsWrapper")) {
						dojo.style(tagVelocityVarName + "suggestedTagsWrapper", "display", "none");
				}
				document.getElementById(suggestedDiv).innerHTML = "";
				tagVelocityVarName = null;
				suggestedDiv = null;
				tagsContainer = null;
		}
}

function useThisTagForSearch(e) {
		var tagLink = e.target;
		var tagSuggested = tagLink.text || tagLink.value;
		if (tagVelocityVarName && !isTagAdded(tagSuggested)) {
				var inputTags = document.getElementById(tagVelocityVarName).value;
				inputTags = RTrim(inputTags);
				inputTags = LTrim(inputTags);
				var tagName = "";
				if (inputTags != "") {
						var arrayTags = inputTags.split(",");
						for (var i = 0; i < (arrayTags.length - 1); i++) {
								tagName = RTrim(tagName);
								tagName = LTrim(tagName);
								tagName += arrayTags[i] + ", ";
						}
						tagName += tagSuggested;
						addTagToMap(tagSuggested, tagLink.className === "persona");
						addTagLink(tagSuggested);
						addTagValue(tagSuggested)
				}
		}
		document.getElementById(tagVelocityVarName).value = "";
		clearSuggestTagsForSearch();
}

function showHint(element) {
		var div = document.getElementById(element);
		var mouseXY = getMouseXY();
		div.style.left = (mouseXY.mouseX + 10);
		div.style.top = (mouseXY.mouseY - 10);
		div.style.display = "inline";
}

function hideHint(element) {
		var div = document.getElementById(element);
		div.style.display = "none";
}

function addTagToMap(tag, isPersona) {
		var id = getTagId(tag);
		if (!tagsMap[tagVelocityVarName]) {
				tagsMap[tagVelocityVarName] = {}
		}
		tagsMap[tagVelocityVarName][id] = {
				title: tag,
				persona: isPersona
		}
}

function addTagLink(tag) {
		tagsContainer.insertBefore(createTagLink(tag), tagsContainer.lastChild);
}

function addTagValue(tag) {
		var tagValues = document.getElementById(tagVelocityVarName + "Content");

		if (tagValues.value.length) {
				tagValues.value += ", ";
		}
		tagValues.value += tag;
}

function clearTag(e) {
		tagVelocityVarName = e.target.dataset.field;
		removeTagValue(tagsMap[tagVelocityVarName][e.target.id].title);
		delete tagsMap[tagVelocityVarName][e.target.id];
		e.target.remove();
}

function isPersona(tagId) {
	return tagsMap[tagVelocityVarName][tagId].persona;
}

function createTagLink(tag) {
		var tagId = getTagId(tag);
		var node = document.createElement("a");
		node.dataset.field = tagVelocityVarName;
		node.className = "tagLink" + (isPersona(tagId) ? " persona" : "");
		node.dataset.field = tagVelocityVarName;
		node.href = "#";
		node.onclick = clearTag;
		node.id = tagId;
		node.innerText = node.textContent = tagsMap[tagVelocityVarName][tagId].title;
		return node;
}

function fillExistingTags(elementId, value) {
		if (!tagVelocityVarName) {
				tagVelocityVarName = elementId;
		}
		if (!tagsContainer) {
				tagsContainer = document.getElementById("widget_" + tagVelocityVarName);
		}
		var existingTags = value.split(",");
		fillExistingTagsMap(existingTags);
		fillExistingTagsLinks(existingTags);

		tagVelocityVarName = null;
		tagsContainer = null;
}

function fillExistingTagsMap(tags) {
		tags.forEach(function(tag) {

				tag = RTrim(tag);
				tag = LTrim(tag);

				if (tag.indexOf(":persona") != -1) {
					tag = tag.replace(":persona","");
					addTagToMap(tag, true);
				} else {
					addTagToMap(tag, false);
				}

		});
}

function fillExistingTagsLinks(tags) {
		tags.forEach(function(tag) {

				tag = RTrim(tag);
				tag = LTrim(tag);

				if (tag.indexOf(":persona") != -1) {
					tag = tag.replace(":persona","");
				}

				addTagLink(tag);
		});
}

function getTagId(tag) {
		return tag.toLowerCase()
				.replace(/ /g, "-")
				.replace(/[^\w-]+/g, "");
}

function getInputPosition() {
		return document.getElementById(tagVelocityVarName).offsetLeft + "px";
}

function getInputHeight() {
		return (document.getElementById("widget_" + tagVelocityVarName).offsetHeight + 2) + "px";
}

function isTagAdded(tag) {
		if (!tagsMap[tagVelocityVarName]) {
				tagsMap[tagVelocityVarName] = {};
		}
		return tagsMap[tagVelocityVarName][getTagId(tag)] ? true : false;
}

function removeTagValue(tagToRemove) {

        tagToRemove = RTrim(tagToRemove);
        tagToRemove = LTrim(tagToRemove);

		var tagValues = document.getElementById(tagVelocityVarName + "Content");
		if (tagValues.value.length) {
				var tagsExisting = tagValues.value.split(",");
				var updatedTagValues = [];
				tagsExisting.forEach(function(tag) {

						tag = RTrim(tag);
						tag = LTrim(tag);

						if (tag.toString() != tagToRemove.toString()) {
								updatedTagValues.push(tag);
						}
				});
				tagValues.value = updatedTagValues.join(", ");
		}
}

//Utility functions

/*
==================================================================
LTrim(string) : Returns a copy of a string without leading spaces.
==================================================================
PURPOSE: Remove leading blanks from our string.
IN: str - the string we want to LTrim
*/
function LTrim(str) {
	var whitespace = new String(" \t\n\r");

	var s = new String(str);

	if (whitespace.indexOf(s.charAt(0)) != -1) {
			// We have a string with leading blank(s)...

			var j = 0,
					i = s.length;

			// Iterate from the far left of string until we
			// don"t have any more whitespace...
			while (j < i && whitespace.indexOf(s.charAt(j)) != -1)
					j++;

			// Get the substring from the first non-whitespace
			// character to the end of the string...
			s = s.substring(j, i);
	}
		return s;
}

/*
==================================================================
RTrim(string) : Returns a copy of a string without trailing spaces.
==================================================================
PURPOSE: Remove trailing blanks from our string.
IN: str - the string we want to RTrim
*/
function RTrim(str) {
		// We don"t want to trip JUST spaces, but also tabs,
		// line feeds, etc.  Add anything else you want to
		// "trim" here in Whitespace
		var whitespace = new String(" \t\n\r");

		var s = new String(str);

		if (whitespace.indexOf(s.charAt(s.length - 1)) != -1) {
				// We have a string with trailing blank(s)...

				var i = s.length - 1; // Get length of string

				// Iterate from the far right of string until we
				// don"t have any more whitespace...
				while (i >= 0 && whitespace.indexOf(s.charAt(i)) != -1)
						i--;


				// Get the substring from the front of the string to
				// where the last non-whitespace character is...
				s = s.substring(0, i + 1);
		}

		return s;
}

/*
=============================================================
Trim(string) : Returns a copy of a string without leading or trailing spaces
=============================================================
*/
function Trim(str)
/*
	 PURPOSE: Remove trailing and leading blanks from our string.
	 IN: str - the string we want to Trim

	 RETVAL: A Trimmed string!
*/
{
		return RTrim(LTrim(str));
}
