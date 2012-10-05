//Display Tabs
var tabsList = new Array();
var customErrorMessages = new Array();
var tabsRequiredList = new Array();
var currentTabIdx=0;
var enabledWYSIWYG = new Array();
var lastTabId = '';
var errorFieldName='';

function submitContent(tabName){
	dijit.byId("buttonSubmitButton").setAttribute('disabled', true);
	errorFieldName='';
	var isValid = checkTabFields(tabName); 
	if(isValid){	
		document.getElementById('submitContentForm').submit();	
	}else{
		dijit.byId("buttonSubmitButton").setAttribute('disabled', false);
		if(errorFieldName != null && errorFieldName != ''){
			document.getElementById(errorFieldName).focus();	
		}
		return false;	
	}
}
function updateTabCounter(tabname){
    tabsRequiredList[tabname]=tabsRequiredList[tabname]+1;
}
function updateTabList(){
	var tempTabsList = new Array();
    for(x=0;x<tabsList.length ;x++){
		if(tabsRequiredList[tabsList[x]] > 0){
			tempTabsList[tempTabsList.length]=tabsList[x];
		}else{
			document.getElementById(tabsList[x]+'Tab').style.display='none';
		}
	}
	tabsList = tempTabsList;
}
function showPreviousTab(){
    var actIdx = currentTabIdx-1;
	displayTab(tabsList[actIdx],actIdx);
}
function showNextTab(){
	var actIdx = currentTabIdx+1;
	displayTab(tabsList[actIdx],actIdx);
}
function displayTab(tabName, newIdx){
    var visitedTab = 0;
    var newTabIndex = 1;
    errorFieldName='';
	for(var tabsIndex=0; tabsIndex < tabsList.length ; tabsIndex++){
	    var divName = tabsList[tabsIndex]; 
	    var divNameButton = 'tabButton'+tabsIndex; 
	    dojo.byId(divName).style.display='none';
	    dojo.byId(divName+'Tab').className = 'beta';
	    dojo.byId(divNameButton).style.display='none';
	    if(divName == tabName){
	    	visitedTab=tabsIndex;
	    	newTabIndex = tabsIndex;
	    }
	}
	if(visitedTab > 0)
		visitedTab = visitedTab - 1;
		
	var lastDivName = tabsList[visitedTab]; 
	var isValid = checkTabFields(lastDivName); 
	if(isValid){
		dojo.byId(tabName).style.display='';
		dojo.byId(tabName+'Tab').className = 'alpha';
		currentTabIdx = newIdx;
		if(tabsList.length - 1 == newTabIndex){
			dojo.byId('submitButton').style.display='';
		}else {
		    dojo.byId('tabButton'+newTabIndex).style.display='';
			dojo.byId('submitButton').style.display='none';			
		}
		window.scrollTo(0,0);
	}else{
		
		dojo.byId(lastDivName).style.display='';
		dojo.byId(lastDivName+'Tab').className = 'alpha';
		dojo.byId('tabButton'+visitedTab).style.display='';
		dojo.byId('submitButton').style.display='none';
		if(errorFieldName != null && errorFieldName != ''){
			document.getElementById(errorFieldName).focus();	
		}		
	}
	var lastTabClass = document.getElementById(lastTabId).className;
	lastTabClass = lastTabClass + ' last';
	//alert('The lastTabClass is: ' + lastTabClass);
	document.getElementById(lastTabId).className = lastTabClass;
}
//Categories controls

var showAllCategories;

function showCategoriesBox(fieldName, catInode){

    var keyword = $(fieldName + 'KeywordBox').value;
    
    //Cleaning all the old categories before repulling them
    $(fieldName + 'CategoriesBox').update('Loading...');
    showAllCategories = false;

    CategoryAjax.findChildrenCategories(catInode, "(?i).*" + keyword + ".*", function(data){
		loadCategoriesCallback(fieldName, catInode, data);
	});
    
    $(fieldName + 'CategoriesBox').show({
        duration: .4
    });
}

function hideCategoriesBox(fieldName){
    var categoriesBox = $(fieldName + 'CategoriesBox');
    categoriesBox.hide({
        duration: .4
    });
}

function loadAllCategories(fieldName, catInode){
    var keyword = $(fieldName + 'KeywordBox').value;
    
    //Cleaning all the old categories before repulling them
    $(fieldName + 'CategoriesBox').update('Loading...');
    showAllCategories = true;
	
	var loadCategoriesCallbackProxy = function(data){
		loadCategoriesCallback(fieldName, catInode, data);
	};
	
    CategoryAjax.getChildrenCategories(catInode, "(?i).*" + keyword + ".*", loadCategoriesCallbackProxy);
    
}

function loadCategoriesCallback(fieldName, catInode, data){

    var keyword = $(fieldName + 'KeywordBox').value;
    
    var lastLevel = 0;
    var currentLevel = 0;
    var secondLevelCount = 0;
    var showAllShowed = false;
    var strHTML = '';
	var maxidx = showAllCategories?data.length:(data.length > 10?10:data.length);
    for (var i = 0; i < maxidx; i++) {
		var cat = data[i];
		currentLevel = cat.categoryLevel;
		if (lastLevel > currentLevel) 
			strHTML += '</ul>';
		if (currentLevel > lastLevel) {
			strHTML += '<ul>';
		}
		if (i == 0 && data.length > 10) {
			secondLevelCount = 0;
			showAllShowed = false;
			strHTML += '<li>' +
			'	<a href="javascript: addCategory(\'' +
			fieldName +
			'\', ' +
			cat.inode +
			', \'' +
			cat.categoryOrigName +
			'\')">' +
			cat.categoryOrigName +
			'	</a>' +
			' (<a href="javascript: loadAllCategories(\'' +
			fieldName +
			'\', ' +
			catInode +
			')">all</a>)' +
			'</li>';
		}
		else {
			var catClass = "";
			if (keyword != "" && cat.categoryOrigName.toLowerCase().indexOf(keyword.toLowerCase()) > -1) 
				catClass = "category_higlighted";
			
			strHTML += '<li>' +
			'	<a href="javascript: addCategory(\'' +
			fieldName +
			'\', ' +
			cat.inode +
			', \'' +
			cat.categoryOrigName +
			'\')">' +
			cat.categoryOrigName +
			'	</a>' +
			'</li>';
		}
        lastLevel = currentLevel;
	}
	if(!showAllCategories && data.length > 10) {
		strHTML += '<li>...</li>';
    }
    strHTML += '</ul>';
	showAllCategories = false;
	
    $(fieldName + 'CategoriesBox').update(strHTML);
}

function countLevelCategories(data, startIndex, level){
    var count = 0;
    for (var i = startIndex; i < data.length; i++) {
        var cat = data[i];
        if (cat.categoryLevel != level) 
            break;
        count++;
    }
    return count;
}

function addCategory(fieldName, catInode, catLabel){
    var catsBox = $(fieldName + 'SelectedCategoriesBox');
    var strHTML = '<div id="category' + catInode + 'Box">' +
		'<div id="removeCat' + catInode + '" onclick="removeCategory(\'' + catInode + '\');" class="removeCategoryBtn"></div>' +
    	'<label for="cat' +	catInode + '">' + catLabel + '</label>' +
    	'<input type="hidden" name="categories" id="cat' + catInode + '" value="' + catInode + '"/>' +
    	'<input type="hidden" name="categoryNames" id="catLabel' + catInode + '" value="' + catLabel + '"/>' +
    	'<br/></div>';
    catsBox.update(catsBox.innerHTML + strHTML);
    hideCategoriesBox(fieldName);
    $(fieldName + 'KeywordBox').value = '';
}

function removeCategory(catInode){
    $('category' + catInode + 'Box').remove();
}

function keywordsCheckKeys(fieldName, catInode, event){
    if ($(fieldName + 'KeywordBox').value.length == 0) {
        hideCategoriesBox(fieldName);
    }
    else 
        if ($(fieldName + 'KeywordBox').value.length >= 3 || event.keyCode == 8) {
            showCategoriesBox(fieldName, catInode);
        }
}

function initializeCategoryFilter(fieldName, catInode) {

    var keywordBox = $(fieldName + "KeywordBox");
    keywordBox.observe("keyup", function(event){
		keywordsCheckKeys(fieldName, catInode, event);
	});
    
    var showCategoriesButton = $(fieldName + "ShowCategoriesBtn");
    showCategoriesButton.observe("click", function(event){
		showCategoriesBox(fieldName, catInode);
	});
    
}

//Tag management functions
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
	var inputTags = element.attr('value');
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

		html = "<fieldset style='padding: 0px; border: 0px;'>";
		
		for (var i = 0; i < result.length; i++) {
			var tagName = result[i]["tagName"];
			tagName = RTrim(tagName);
			tagName = LTrim(tagName);
			var tagNameParam = tagName.replace("'", "\\\'");
			html += "<a style='font-family: Verdana,Arial,Helvetica; color: #000000; font-size: 12px; font-weight: normal;' href=\"javascript: useThisTagForSearch('"+tagNameParam+"');\">" + tagName + "</a>";
			if (i+1 < result.length) {
				html += ", ";
			}
		}
		html += "</fieldset>";

		if ((suggestedTag != null) && (suggestedTag != "")) {
			var tagDiv = document.getElementById(suggestedDiv);
			tagDiv.innerHTML = html;
			tagDiv.style.display = "";
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

/*
========================================================================
Fields validation
========================================================================
*/


//function to validate US dates
function dateValid(element, text){

        good = true;
        dar =element.value.split("/");
        if(dar.length < 3){
                good = false;
        }
        if(isNaN(parseInt(dar[0], 10)) || isNaN(parseInt(dar[1], 10)) || isNaN(parseInt(dar[2], 10))){
                good = false;
        }
        month = parseInt(dar[0], 10);
        day = parseInt(dar[1], 10);
        year = parseInt(dar[2], 10);
        if(month< 1 || month > 12){
                good = false;
        }
        else if(day < 1 || day > 31){
                good = false;
        }
        else if(year < 1900 || year > 2100){
                good = false;
        }
        else if(
        (year % 4 != 0 && day > 28 && month == 2) || 	(month == 4 || month ==6 || month == 9 || month == 11) && day > 30 	|| (year % 4 == 0 && day > 29 && month == 2)){
                good = false;
        }
        if(! good){
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
        	 document.getElementById('alert'+element.name).innerHTML = text + " is a not a valid date";
    		}
            errorFieldName=element.id;
            return false;
        }else{
        	 document.getElementById('alert'+element.name).innerHTML = "";
        }
        return true;
}





//function to validate by length	

function lengthValid(element, len, text) {

    text = Trim(text);

	if (element.value.length < len)
  	{
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
		 document.getElementById('alert'+element.name).innerHTML ="Please enter a valid " + text + ".";
		}
		errorFieldName=element.id;
        return false;
	}else{
		 document.getElementById('alert'+element.name).innerHTML = "";
    }

	return true;
	
}

function lengthValidCustomField(element, len, text) {

    text = Trim(text);

	if (element.value.length < len)
  	{
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
		 document.getElementById('alert'+element.name).innerHTML ="Please enter a valid " + text + ".";
		}
		errorFieldName="customAlert"+element.id;
        return false;
	}else{
		 document.getElementById('alert'+element.name).innerHTML = "";
    }

	return true;
	
}
//function to validate select drop-downs
function selectValid(element, text) {
    text = Trim(text);
    try{
    	if(element.getValue() == ""){
    		if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
    		 document.getElementById('alert'+element.attr('name')).innerHTML ="Please select a " + text + ".";
    		}
    		errorFieldName=element.id;
    		return false;
        }else{
        	 document.getElementById('alert'+element.attr('name')).innerHTML = "";
        }
    	return true;
	}catch(e){}
	return false;

}

//function to validate numerical fields
function numberValid(element, text) {
    text = Trim(text);
	if(element.value == '' || isNaN(element.value))
  	{
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
 	  	 document.getElementById('alert'+element.name).innerHTML ="Please enter a valid " + text + ".";
		}
		errorFieldName=element.id;
		return false;
	}else{
       	 document.getElementById('alert'+element.name).innerHTML = "";
    }
	return true;

}

//function to validate email
function emailValid(element, text) {
        text = Trim(text);
        good = true;
	if(element.value.length < 5)
	{
            good = false;
	}

	if(element.value.indexOf("@") < 1 || element.value.lastIndexOf("@")  > element.value.length - 3 )
	{
            good = false;
	}
	if(element.value.indexOf(".") == -1 || element.value.lastIndexOf(".")  > element.value.length - 3 )
	{
            good = false;
	}
	if(element.value.lastIndexOf("@") !=  element.value.indexOf("@") )
	{
            good = false;
	}
	if(element.value.lastIndexOf("@") !=  element.value.indexOf("@") )
	{
            good = false;
	}
	if(element.value.lastIndexOf("@") >= element.value.lastIndexOf(".")-1 )
	{
            good = false;
	}


        if(!good){
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
             document.getElementById('alert'+element.name).innerHTML ="Please enter a valid " + text + ".";
    		}
        	errorFieldName=element.id;
            return false;
        }else{
        	 document.getElementById('alert'+element.name).innerHTML = "";
        }
	return true;

}

//function to validate at least 1 radio button is checked	
function radioValid(element, radios, text) {
    text = Trim(text);
	radios=radios-1;
	var varChecked=false;
	for(var radiostoCheck=0;radiostoCheck<=radios;radiostoCheck++)
	{
		if(element[radiostoCheck].checked)
		{
			varChecked=true;
		}
	}
	if (varChecked==false)
  	{
		if(customErrorMessages[element[0].name] != ''){
			document.getElementById('alert'+element[0].name).innerHTML = text+" "+customErrorMessages[element[0].name];
		}else{
 	  	 	document.getElementById('alert'+element[0].name).innerHTML ="Please select " + text + ".";
		}
		errorFieldName=element[0].id;
		return false;
	}else{
      	 document.getElementById('alert'+element[0].name).innerHTML = "";
    }
	
	return true;

}


//function to validate at a given number of checkboxes are checked	
function checkboxValid(element, checkboxesLength, requiredNumber,text) {
    text = Trim(text);
	checkboxesLength=checkboxesLength-1;
	var varChecked=0;
	for(var checkboxestoCheck=0;checkboxestoCheck<=checkboxesLength;checkboxestoCheck++)
	{
		if(element[checkboxestoCheck].checked)
		{
			varChecked++;
		}
	}
	if (varChecked < requiredNumber)
  	{
		if(customErrorMessages[element[0].name] != ''){
			document.getElementById('alert'+element[0].name).innerHTML = text+" "+customErrorMessages[element[0].name];
		}else{
 	  	 document.getElementById('alert'+element[0].name).innerHTML = "Please select at least " + requiredNumber + " " + text + ".";
		}
		errorFieldName=element[0].id;
		return false;
	}else{
         document.getElementById('alert'+element[0].name).innerHTML = "";
    }
	return true;
	
}

//function to validate "other box" is not empty if checked	
function otherboxValid(otherradio,otherfield,len,text) {
    text = Trim(text);
	if (otherradio.checked && (otherfield.value.length<len))
  	{
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
 	  	 document.getElementById('alert'+element.name).innerHTML = "Please specify the " + text + ".";
		}
		errorFieldName=otherfield.id;
		return false;
	}else{
       	 document.getElementById('alert'+element.name).innerHTML = "";
    }
	
	return true;
	
}

//function to validate radio checked if checkbox checked	
function checkboxRadio(checkfield, radiofield, radios, text) {
    text = Trim(text);
	if(checkfield.checked)
	{	
		radioValid(radiofield, radios, text);
	}
}

//function to validate by length in the textarea	
function lengthValidText(element, len, text) {
	
	 text = Trim(text);
	 value = Trim(element.value);
	 if (value.length < len)
	{
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
			document.getElementById('alert'+element.name).innerHTML = text + " field is required.";
		}
		errorFieldName=element.id;
		return false;
	}else{
		 document.getElementById('alert'+element.name).innerHTML = "";
    }
        
	return true;
	


}

//function to validate US dates
function expDateValid(element, text){
        good = true;
        dar = element.value.split("/");
        if(dar.length < 2){
            good = false;
        }
        if(isNaN(parseInt(dar[0], 10)) || isNaN(parseInt(dar[1], 10))){
			good = false;
        }
        month = parseInt(dar[0], 10);
        year = parseInt(dar[1], 10);
        
        if(month< 1 || month > 12){
            good = false;
        }
        else if(year < 0 || year > 99){
			good = false;
        }
        if(! good){
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
             document.getElementById('alert'+element.name).innerHTML = text + " is a not a valid date";
    		}
        	errorFieldName=element.id;
            return false;
        }
        today = new Date();
		expiry = new Date(year + 2000, month);
        if (today.getTime() > expiry.getTime()) {
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
    			document.getElementById('alert'+element.name).innerHTML = text + " is an expired date";
    		}
        	errorFieldName=element.id;
            return false;
        }else{
        	 document.getElementById('alert'+element.name).innerHTML = "";
        }
        return true;
}

//function to validate String US dates
function expDateValidString(element, text){
        good = true;
        dar = element.split("/");
        if(dar.length < 2){
            good = false;
        }
        if(isNaN(parseInt(dar[0], 10)) || isNaN(parseInt(dar[1], 10))){
			good = false;
        }
        month = parseInt(dar[0], 10);
        year = parseInt(dar[1], 10);
        
        if(month< 1 || month > 12){
            good = false;
        }
        else if(year < 0 || year > 99){
			good = false;
        }
        if(! good){
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
             document.getElementById('alert'+element.name).innerHTML =  text + " is a not a valid date";
    		}
        	errorFieldName=element.id;
            return false;
        }
        today = new Date();
		expiry = new Date(year + 2000, month);
        if (today.getTime() > expiry.getTime()) {
        	if(customErrorMessages[element.name] != ''){
    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
    		}else{
    			document.getElementById('alert'+element.name).innerHTML = text + " is an expired date";
    		}
            return false;
        }else{
        	 document.getElementById('alert'+element.name).innerHTML = "";
        }
        return true;
}

//Validate regular expression
function validateRegularExpresion(element, regexpression, text){
		if(regexpression.indexOf('^') == -1)
			regexpression = '^'+regexpression;
			
		if(regexpression.indexOf('$') == -1)
			regexpression = regexpression+'$';
		try{	
			
			var pattern = new RegExp(regexpression);
	        
	        if (element.value == '' || pattern.test(element.value)) {
	        	 document.getElementById('alert'+element.name).innerHTML = "";
	            return true;
	        }else{
	        	if(customErrorMessages[element.name] != ''){
	    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
	    		}else{
	        	 document.getElementById('alert'+element.name).innerHTML = text + " doesn\'t comply the specified format";
	    		}
	        	errorFieldName=element.id;
	            return false;
	        }
		}catch(e){
			document.getElementById('alert'+element.name).innerHTML = e;
			return false;
		}        
}

function validateRegularExpresionNotHTML(element, regexpression, text){
		if(regexpression.indexOf('^') == -1)
			regexpression = '^'+regexpression;
			
		if(regexpression.indexOf('$') == -1)
			regexpression = regexpression+'$';
		try{	
			
			var pattern = new RegExp(regexpression);
	        
	        if(pattern.test(element.value)) {
	        	if(customErrorMessages[element.name] != ''){
	    			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
	    		}else{
	    			document.getElementById('alert'+element.name).innerHTML = text + " doesn\'t comply the specified format";
	    		}
	        	errorFieldName=element.id;
	            return false;
	        }else{
	        	document.getElementById('alert'+element.name).innerHTML = "";
	            return true;
	        }
		}catch(e){
			document.getElementById('alert'+element.name).innerHTML = e;
			return false;
		}        
}

function categoryValid( varName, text){
	
	if(dojo.byId(varName).value == ""){
		if(customErrorMessages[varName] != ''){
			document.getElementById('alert'+varName).innerHTML = text+" "+customErrorMessages[varName];
		}else{
		    document.getElementById('alert'+varName).innerHTML = " The category is required";
		}
		errorFieldName=varName;
		return false;
	}else{
		 document.getElementById('alert'+varName).innerHTML = "";
		return true;
	}
}

function fileValid(element, text){
	if(element.value.length == 0){
		if(customErrorMessages[element.name] != ''){
			document.getElementById('alert'+element.name).innerHTML = text+" "+customErrorMessages[element.name];
		}else{
			document.getElementById('alert'+element.name).innerHTML = text+" is required";
		}
		errorFieldName=element.id;
		return false;
	}else{
		 document.getElementById('alert'+element.name).innerHTML = "";
	}	
	return true;
}

//Verify is the textarea belong to a wysiwyg field
function isWYSIWYGEnabled(id){
	return (enabledWYSIWYG[id] == true);
}

function updateDateTime(fieldname){
	var dtDate = document.getElementById(fieldname+'Date').value;
	var dtTime = document.getElementById(fieldname+'Time').value;
	document.getElementById(fieldname).value = dtDate+' '+dtTime;	
}


function updateCategoriesList(inode, name, selectval){	
 var f=dijit.byId(selectval).attr('value');
 var c=dojo.byId(name).value.indexOf(dojo.byId(selectval).value);

    if (c == -1){
         var categorias  = f.split(',');
        if(dojo.byId(inode).value == ""){
			dojo.byId(name).value=categorias[0];
			dojo.byId(inode).value=categorias[1];
		}
		else {
		dojo.byId(name).value= dojo.byId(name).value + ", " + categorias[0];
		dojo.byId(inode).value= dojo.byId(inode).value + ", " + categorias[1];
		}
		dojo.byId(inode+'categorieslist').innerHTML = '';
		catnames=dojo.byId(name).value.split(",");
		var buffer='';
		for(i = 0; i < catnames.length; i++){
			buffer+=catnames[i] + " <br/> ";
		}
		buffer+="";
		dojo.byId(inode+'categorieslist').innerHTML = buffer;
	}
	else alert("Category already selected");
}

function clearCategoriesList(inode, name){	
	dojo.byId(name).value="";
	dojo.byId(inode).value="";
	dojo.byId(inode+'categorieslist').innerHTML = '';
	var buffer='No categories selected ';
	dojo.byId(inode+'categorieslist').innerHTML = buffer;
}

function serveFile(doStuff,conInode,velVarNm){

    if(doStuff != ''){
    window.open('/contentAsset/' + doStuff + '/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
    }else{
    window.open('/contentAsset/raw-data/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
    }
}

function removeFile(fileInode){
	dojo.empty(fileInode+"Container");
	dojo.style(fileInode, "visibility", "visible");
}