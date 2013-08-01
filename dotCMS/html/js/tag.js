	function tagAction(action, element, returnDiv) {
		var tagName = document.getElementById(element).value;
		tagName = RTrim(tagName);
		tagName = LTrim(tagName);
		if (tagName != "") {
			var doAction = false;
			var fullCommand = "false";
			try
			{
				checkFullCommand();
				fullCommand = document.getElementById("fullCommand").value;
			}
			catch(e)
			{}
			if(fullCommand == "true")
			{
				if (action == 'add')
				{
					TagAjax.addTagFullCommand(tagName, userId);
					returnAction = 'added';
				}
				else if (action == 'remove')
				{
					TagAjax.deleteTagFullCommand(tagName);
					returnAction = 'removed';
				}
				returnAction = "Tags " + returnAction;
				displayTagReturn(returnDiv, returnAction, 20);
			}
			else
			{
		   		if ((cbUserIdList != null) || (0 < cbUserIdList.length)) {
		   			var returnAction = 'added';
		   			var users = 0;
	   				for (var i = 0; i < cbUserIdList.length; ++i) {
	   					if (document.getElementById(cbUserIdList[i]).checked) {
	   						doAction = true;
							if (action == 'add')
							{
								users++;
								addTag(tagName, cbUserProxyInodeList[i]);
								returnAction = 'added';
							}
							else if (action == 'remove')
							{
								users++;
								deleteTagInode(tagName, cbUserProxyInodeList[i]);
								returnAction = 'removed';
							}
		   				}
		   			}
	   				if (users > 1) {
	   					returnAction = "Tags " + returnAction;
		   			}
		   			else {
	   					returnAction = "Tag " + returnAction;
		   			}
		   			displayTagReturn(returnDiv, returnAction, 20);
		   		}
		   		if (!doAction) {
			   		displayTagReturn(returnDiv, "", 0);
					alert("There's no user checked");
		   		}
	   		}
		}
		else {
	   		displayTagReturn(returnDiv, "", 0);
			alert("There's no tag selected");
		}
	}
	function deleteTagInode(tagName, inode) {
		TagAjax.deleteTagInode(tagName, inode);
	}
	function addTag(tagName, inode) {
		TagAjax.addTag(tagName, userId, inode);
	}

	function displayTagReturn(div, message, height)	{
		if(document.layers)	{   //NN4+
	    	var obj = document.layers[div];
	    	var objMsg = document.layers[div];
	    	if (message == '') {
	        	obj.visibility = "hide";
	        	objMsg.innerHTML = '';
				obj.height = '0px';
	        }
	        else {
	        	obj.visibility = "show";
	        	objMsg.innerHTML = message;
				obj.height = height + 'px';
	        }
	    }
	    else if(document.getElementById) {	  //gecko(NN6) + IE 5+
	        var obj = document.getElementById(div);
	    	var objMsg = document.getElementById(div);
	    	if (message == '') {
	        	obj.style.display = "none";
	        	objMsg.innerHTML = '';
				obj.style.height = '0px';
			}
			else {
	        	obj.style.display = "inline";
	        	objMsg.innerHTML = message;
				obj.style.height = height + 'px';
	        }
	    }
	    else if(document.all) {	// IE 4
	    	var obj = document.all[div]
	    	var objMsg = document.all[div];
	    	if (message == '') {
	        	obj.style.display = "none";
	        	objMsg.innerHTML = '';
				obj.style.height = '0px';
	        }
	        else {
	        	obj.style.display = "inline";
	        	objMsg.innerHTML = message;
				obj.style.height = height + 'px';
	        }
	    }
	}

	var suggestedTag;
	var suggestedDiv;
	function suggestTagsForSearch(element, divToSuggest) {
		suggestedTag = element.id;
		suggestedDiv = divToSuggest;
		var inputTags = document.getElementById(suggestedTag).value;
		inputTags = RTrim(inputTags);
		inputTags = LTrim(inputTags);
		var tagName = "";
		if (inputTags != "" & inputTags.length >= 3) {
			var arrayTags = inputTags.split(',');
			if (arrayTags.length >= 1) {
				tagName = arrayTags[arrayTags.length - 1];
				tagName = RTrim(tagName);
				tagName = LTrim(tagName);
			}
		}
		//in backend, check if structure has host or folder field and dynamically suggest tags for that field's value
		var selectedHostId  ="";

		if(dojo.byId('hostId')){
			selectedHostId = dojo.byId('hostId').value;
		}

		//in frontend, check for hidden field with host identifier
		if(dojo.byId('currentHostIdForTagSuggestion')){
			selectedHostId = dojo.byId('currentHostIdForTagSuggestion').value;
		}

		if (tagName != "" & tagName.length >= 3) {
			TagAjax.getSuggestedTag(tagName, selectedHostId, showTagsForSearch);
		}
		else {
			clearSuggestTagsForSearch();
		}
	}
	function showTagsForSearch(result) {
		var html;
		DWRUtil.setValues(result);

		if (result.length > 0) {

			html = "<div class'suggestedTagsLinks'>";

			for (var i = 0; i < result.length; i++) {
				var tagName = result[i]["tagName"];
				tagName = RTrim(tagName);
				tagName = LTrim(tagName);
				var tagNameParam = tagName.replace("'", "\\\'");
				html += "<a href=\"javascript: useThisTagForSearch('"+tagNameParam+"');\">" + tagName + "</a>";
				/* if (i+1 < result.length) {
					html += ", ";
				}*/
			}
			html += "</div>";

			if ((suggestedTag != null) && (suggestedTag != "")) {
				var tagDiv = document.getElementById(suggestedDiv);
				tagDiv.innerHTML = html;
				dojo.style(suggestedTag+"suggestedTagsWrapper", "display", "block");
			}
		}
		else {
			clearSuggestTagsForSearch();
		}
	}
	function clearSuggestTagsForSearch() {
		if ((suggestedTag != null) && (suggestedTag != "")) {
			var tagDiv = document.getElementById(suggestedDiv);
			tagDiv.innerHTML = "";
			suggestedTag = "";
			suggestedDiv = "";
			dojo.style("tagssuggestedTagsWrapper", "display", "none");
		}
	}
	function useThisTagForSearch(tagSuggested) {
		if ((suggestedTag != null) && (suggestedTag != "")) {

			var inputTags = document.getElementById(suggestedTag).value;
			inputTags = RTrim(inputTags);
			inputTags = LTrim(inputTags);
			var tagName = "";
			if (inputTags != "") {
				var arrayTags = inputTags.split(',');
				for (var i = 0; i < (arrayTags.length - 1); i++) {
					tagName = RTrim(tagName);
					tagName = LTrim(tagName);
					tagName += arrayTags[i] + ", ";
				}
				tagName += tagSuggested;
			}

			document.getElementById(suggestedTag).value = tagName;
			clearSuggestTagsForSearch();
		}
	}
	function showHint(element) {
		var div = document.getElementById(element);
		var mouseXY = getMouseXY();
		div.style.left = ( mouseXY.mouseX + 10 );
		div.style.top = ( mouseXY.mouseY - 10 );
		div.style.display = 'inline';
	}
	function hideHint(element) {
		var div = document.getElementById(element);
		div.style.display = 'none';
	}

	//Utility functions

	/*
	==================================================================
	LTrim(string) : Returns a copy of a string without leading spaces.
	==================================================================
	*/
	function LTrim(str)
	/*
	   PURPOSE: Remove leading blanks from our string.
	   IN: str - the string we want to LTrim
	*/
	{
	   var whitespace = new String(" \t\n\r");

	   var s = new String(str);

	   if (whitespace.indexOf(s.charAt(0)) != -1) {
	      // We have a string with leading blank(s)...

	      var j=0, i = s.length;

	      // Iterate from the far left of string until we
	      // don't have any more whitespace...
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
	*/
	function RTrim(str)
	/*
	   PURPOSE: Remove trailing blanks from our string.
	   IN: str - the string we want to RTrim

	*/
	{
	   // We don't want to trip JUST spaces, but also tabs,
	   // line feeds, etc.  Add anything else you want to
	   // "trim" here in Whitespace
	   var whitespace = new String(" \t\n\r");

	   var s = new String(str);

	   if (whitespace.indexOf(s.charAt(s.length-1)) != -1) {
	      // We have a string with trailing blank(s)...

	      var i = s.length - 1;       // Get length of string

	      // Iterate from the far right of string until we
	      // don't have any more whitespace...
	      while (i >= 0 && whitespace.indexOf(s.charAt(i)) != -1)
	         i--;


	      // Get the substring from the front of the string to
	      // where the last non-whitespace character is...
	      s = s.substring(0, i+1);
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



