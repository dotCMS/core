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
var lastLength = 0;
var contentSearchField;

function suggestTagsForSearch(e, searchField, hostInSession) {
	if (searchField) {
		contentSearchField = searchField;
	}

    tagVelocityVarName = e.target.id;

	if (!tagsContainer || tagsContainer == "") {
		tagsContainer = document.getElementById("widget_" + tagVelocityVarName);
	}
	suggestedDiv = tagVelocityVarName.replace(".", "") + "SuggestedTagsDiv";
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

	/*
     NOTE: The hostInSession parameter will be passed only when searching tags in the content search so it is safe to use it
     as the final host value filter for the getSuggestedTag method.
	 */
	if (hostInSession) {
		selectedHostOrFolderId = hostInSession;
	}

	if (e.keyCode !== keys.UP_ARROW && e.keyCode !== keys.DOWN_ARROW) {
		// semicolon
		if (e.keyCode === 188) {
			useThisTagForSearch(e);
		} else if (e.keyCode === keys.ENTER) {
			var suggestedTagFocus = query(".suggestedTagFocus");
			if (suggestedTagFocus.length) {
				suggestedTagFocus[0].click();
			} else {
				useThisTagForSearch(e);
			}
		} else if (e.keyCode === keys.BACKSPACE && e.target.value.length === 0 && lastLength === 0) {
			removeLastTag();
			lastLength = 0;
		} else if (e.keyCode === keys.ESCAPE) {
			clearSuggestTagsForSearch();
		} else if (tagName.length >= 3) {
			TagAjax.getSuggestedTag(tagName, selectedHostOrFolderId, showTagsForSearch);
		} else {
			clearSuggestTagsForSearch();
		}
	}

	lastLength = e.target.value.length;
}

function closeSuggetionBox(e) {
	setTimeout(function() {
		if (document.activeElement.parentElement.id != "tagsSuggestedTagsDiv") {
			clearSuggestTagsForSearch();
			e.target.value = "";
			e.target.blur();
		}
	}, 500)
}

function removeAllTags() {
    var tags = query(".tagLink");
    if (tags.length) {
        for (i = 0; tags.length > i; i++) {
            var tagToRemove = tags[i];
            clearTag(tagToRemove);
        }
    }
}

function removeLastTag() {
	var tags = query(".tagLink");
	if (tags.length) {
		var tagToRemove = tags[tags.length - 1];
		clearTag(tagToRemove);
	}
}

var pos;
var keyboardEvents;
var keys = dojo.require("dojo.keys");
var query = dojo.require("dojo.query");

function focusSelectedTag(e) {
	var tagsOptionsLinks = query("#" + suggestedDiv + " a");

	if (tagsOptionsLinks.length) {
		var lastPos = tagsOptionsLinks.length - 1;
		switch(e.keyCode) {
			case keys.UP_ARROW:
				e.preventDefault();
				if (pos === null) {
					pos = lastPos;
				} else if (pos > 0) {
					pos--;
				} else {
					return dijit.focus(dojo.byId(tagVelocityVarName));
				}
				var item = tagsOptionsLinks[pos];
				item.classList.add("suggestedTagFocus");
				if (item.nextSibling) {
					item.nextSibling.classList.remove("suggestedTagFocus");
				}
				break;
			case keys.DOWN_ARROW:
				e.preventDefault();

				if (pos === null) {
					pos = 0
				} else if (pos < lastPos) {
					pos++;
				}
				var item = tagsOptionsLinks[pos];
				item.classList.add("suggestedTagFocus");
				if (item.previousSibling) {
					item.previousSibling.classList.remove("suggestedTagFocus");
				}
				break;
		}
	}
}

function showTagsForSearch(result) {
	if (result.length > 0) {
		var tags = "";
		var personasTags = "";
        let tag = null;
        for (let i = 0; i < result.length; ++i) {
            tag = result[i];
            var tagName = tag.tagName;
            tagName = RTrim(tagName);
            tagName = LTrim(tagName);
            if (tag.persona) {
                personasTags += "<a href=\"#\" class=\"persona\" onClick=\"useThisTagForSearch(event)\"><span class=\"personaIcon\"></span>" + tagName + "</a>";
            } else {
                tags += "<a href=\"#\" onClick=\"useThisTagForSearch(event)\"><span class=\"tagIcon\"></span>" + tagName + "</a>";
            }

        }

		if (tagVelocityVarName) {
			var tagDiv = document.getElementById(suggestedDiv);
			tagDiv.innerHTML = personasTags + tags;

			if (dojo.byId(suggestedDiv)) {
				dojo.style(suggestedDiv, "display", "block");
				dojo.style(suggestedDiv, "left", getInputPosition());
				dojo.style(suggestedDiv, "top", getInputHeight());
			}
			pos = null;
			if (!keyboardEvents) {
				var tagsOptionsLinksWrapper = dojo.byId(tagVelocityVarName + "Wrapper");
				keyboardEvents = dojo.require("dojo.on")(tagsOptionsLinksWrapper, "keydown", focusSelectedTag);
			}
		}
	} else {
		clearSuggestTagsForSearch();
	}
}

function clearSuggestTagsForSearch() {
	if (tagVelocityVarName) {
		if (dojo.byId(suggestedDiv)) {
			dojo.style(suggestedDiv, "display", "none");
		}
		if (suggestedDiv) {
            if (dojo.byId(suggestedDiv)) {
                dojo.byId(suggestedDiv).innerHTML = "";
            }
		}
		dojo.byId(tagVelocityVarName).focus();
		tagVelocityVarName = null;
		suggestedDiv = null;
		tagsContainer = null;
	}
}

function animateExitingTag(tagSuggested) {
	var existingTag = dojo.byId(getTagId(tagSuggested));
	var endColor = dojo.style(existingTag, "background-color");
	dojo.animateProperty({
		node: dojo.byId(getTagId(tagSuggested)),
		duration: 1000,
		properties: {
			backgroundColor: {
				start: "#FA5858",
				end: endColor
			},

		},
		onEnd: function() {}
	}).play();
}

function useThisTagForSearch(e) {
	var tagLink = e.target;
	var tagSuggested = tagLink.text || tagLink.value.replace(",", "");
	var tagExists = isTagAdded(tagSuggested);
	if (tagExists) {
		animateExitingTag(tagSuggested);
	} else if (tagVelocityVarName) {
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
			addTagToMap(tagSuggested, tagLink.className.indexOf("persona") > -1);
			addTagLink(tagSuggested);
			addTagValue(tagSuggested);
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

	if (contentSearchField) {
		var contentSearchFieldEl = document.getElementById(contentSearchField)

		if (contentSearchFieldEl.value.length) {
			contentSearchFieldEl.value += ", ";
		}
		contentSearchFieldEl.value += tag;
		contentSearchFieldEl.onchange();
	}

	if (tagValues.value.length) {
		tagValues.value += ", ";
	}
	tagValues.value += tag;
}

function clearTag(e) {
	if (!e.target) {
		var node = e;
		e = {};
		e.target = node;
	}
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

function fillExistingTags(elementId, value, searchField) {
	if (searchField) {
		contentSearchField = searchField;
	}

	if (!tagVelocityVarName) {
		tagVelocityVarName = elementId;
	}
	if (!tagsContainer) {
		tagsContainer = document.getElementById("widget_" + tagVelocityVarName);
	}
	var existingTags = value.split(",");
	fillExistingTagsMap(existingTags);
	fillExistingTagsOptionsLinks(existingTags);

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

function fillExistingTagsOptionsLinks(tags) {
	tags.forEach(function(tag) {

		tag = RTrim(tag);
		tag = LTrim(tag);

		if (tag.indexOf(":persona") != -1) {
			tag = tag.replace(":persona","");
		}

		addTagLink(tag);
	});
}

function getHashCode(tag) {
    var hash = 0;
    var chr;

    if (tag.length === 0) return hash;

    for (var i = 0; i < tag.length; i++) {
        chr   = tag.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
}

function getTagId(tag) {
	return "tag-" + getHashCode(tag);
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

		if (contentSearchField) {
			var contentSearchFieldEl = document.getElementById(contentSearchField);
			contentSearchFieldEl.value = tagValues.value;
			contentSearchFieldEl.onchange();
		}
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
