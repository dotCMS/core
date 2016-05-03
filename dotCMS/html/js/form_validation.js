/* 

    dotmarketing formchecker created 03/05/02 by Ben Barreth

*-------------------------------------------------------------*
    Altered 10/16/2002 by Will
    Modified by monkeys on 3/25/2003
    
    Incorporated Steve's util js functions.
    Added a DOM automatic form checker.  This works for 5+ browsers.
    The checker iterates over all input boxes  on the page and looks 
    for an attribute called "required"  All you have to do is include the form_validation.js in 
    your page:
    
        <SCRIPT LANGUAGE="JavaScript" TYPE="text/javascript" src='/js/form_validation.js'></SCRIPT>

    and put the "checkForm()" method in the doSubmit attribute in your form:
    
        <form name="myForm" action="/doSomthing" onSubmit="return checkForm();">

    Required can have three values (text | date | email | number).  The checker
    validates the field based on the required attribute. 

        // requires the values fit the required style
        <input type="text" name="myFirstName" value="" required="text" />
        <input type="text" name="emailAddress" value="" required="email" />
        <input type="text" name="DOB" value="" required="date" />
        <input type="text" name="myIDNumber" value="" required="number" />
		
        // requires the length to be 9
        <input type="text" name="myLastName" value="" required="text" pretty="My Last Name" requiredLength="9" />
        
        // require a radio button be checked
        <input type="radio" name="myGender" value="Male" required="radio" pretty="My Gender">
        <input type="radio" name="myGender" value="Female">
        
        // require 2 out of 4 checkboxes checked
        <input type="checkbox" name="favoriteFruit" value="Male" required="checkbox" pretty="My Gender" requiredNumber="2">
        <input type="checkbox" name="favoriteFruit" value="Apple">
        <input type="checkbox" name="favoriteFruit" value="Orange">
        <input type="checkbox" name="favoriteFruit" value="Female">
        
    	Feel free to add any other checks as you see fit.


*/




function checkForm(form){

    if (form) {
	var elements = form.elements;
	var inputs = new Array();
	var selects = new Array();
	var textarea = new Array();
	
	for (k=0;k<elements.length;k++) {
		if (elements[k].tagName == "INPUT") {
			inputs[inputs.length] = elements[k];
		} else if (elements[k].tagName == "SELECT") {
			selects[selects.length] = elements[k];
		}
		else if (elements[k].tagName == "TEXTAREA") {
			textarea[textarea.length] = elements[k];
		
		}
	}
    
    }
    else {
	    inputs = document.getElementsByTagName("input");
	    selects = document.getElementsByTagName("select");
	    textarea = document.getElementsByTagName("textarea");
    }
	
    for(i =0; i<inputs.length ; i++){
        var required = inputs[i].getAttribute("required");
        
        if(required != undefined && required == "radio"){
        	
        	fieldName = inputs[i].name;	
       		prettyText = (inputs[i].getAttribute("pretty")!= undefined) ? inputs[i].getAttribute("pretty") : inputs[i].name;
       		radios = document.getElementsByName(fieldName);
       		radiosLength = radios.length;
       		if(!radioValid(radios,radiosLength,prettyText)){
       			return false;
       		}
       		
        }
		else if(required != undefined && required == "checkbox"){
        	
            requiredNumber = (inputs[i].getAttribute("requiredNumber")!= undefined) ? inputs[i].getAttribute("requiredNumber") : 1;
        	fieldName = inputs[i].name;	
       		prettyText = (inputs[i].getAttribute("pretty")!= undefined) ? inputs[i].getAttribute("pretty") : inputs[i].name;
       		checkboxes = document.getElementsByName(fieldName);
       		checkboxesLength = checkboxes.length;
       		if(!checkboxValid(checkboxes,checkboxesLength,requiredNumber,prettyText)){
       			return false;
       		}
       		
        }
		else if(required != undefined){


            fieldName = (inputs[i].getAttribute("pretty")!= undefined) ? inputs[i].getAttribute("pretty") : inputs[i].name;
            requiredLength = (inputs[i].getAttribute("requiredLength")!= undefined) ? inputs[i].getAttribute("requiredLength") : 1;

            if(! lengthValid(inputs[i], requiredLength, fieldName)){
            	return false;
            }
            if(required == "date" && ! dateValid(inputs[i], fieldName)){
            	return false;
            }
            if(required == "email" && ! emailValid(inputs[i], fieldName)){
            	return false;
            }
			if(required == "number" && ! numberValid(inputs[i], fieldName)){
				return false;
			}
			if(required == "alpha" && ! alphaValid(inputs[i], fieldName)){
				return false;
			}
			if(required == "alpha_numeric" && ! alphanumericValid(inputs[i], fieldName)){
				return false;
			}
        }
		
		var validationFunction = inputs[i].getAttribute("validationFunction");
		var retValue = false;
		if(validationFunction != undefined) {
			eval("retValue = " + validationFunction);
			if(!retValue) {
				return false;
			}
		}
    }




	/*this is to check the textarea*/
	 for(i =0; i<textarea.length ; i++){
		required = textarea[i].getAttribute("required");
		if(required != undefined)
		{
			fieldName = (textarea[i].getAttribute("pretty")!= undefined) ? textarea[i].getAttribute("pretty") : textarea[i].name;
			requiredLength = (textarea[i].getAttribute("requiredLength")!= undefined) ? textarea[i].getAttribute("requiredLength") : 1;
			if(! lengthValidText(textarea[i], requiredLength , fieldName))
			{
				return false;
			}
		}
		var validationFunction = textarea[i].getAttribute("validationFunction");
		var retValue = false;
		if(validationFunction != undefined) {
			eval("retValue = " + validationFunction);
			if(!retValue) {
				return false;
			}
		}		
	}
	
	
	
	
	
	
    
    for(i =0; i<selects.length ; i++){
        required = selects[i].getAttribute("required");

       if(required != undefined){


            fieldName = (selects[i].getAttribute("pretty")!= undefined) ? selects[i].getAttribute("pretty") : selects[i].name;

            if(required == "select" && ! selectValid(selects[i], fieldName)){
                    return false;
            }
        }
		
		var validationFunction = selects[i].getAttribute("validationFunction");
		var retValue = false;
		if(validationFunction != undefined) {
			eval("retValue = " + validationFunction);
			if(!retValue) {
				return false;
			}
		}		
		
    }
    return true;

}

function checkField(formName, fieldName, prettyText, required, requiredLength){

        if(required != undefined && required == "radio"){
       		radios = eval("document." + formName + "." + fieldName);
       		radiosLength = radios.length;
       		if(!radioValid(radios,radiosLength,prettyText)){
       			return false;
       		}
       		
        }
	else if(required != undefined && required == "checkbox"){
       		checkboxes = eval("document." + formName + "." + fieldName);
       		checkboxesLength = checkboxes.length;
       		if(!checkboxValid(checkboxes,checkboxesLength,requiredLength,prettyText)){
       			return false;
       		}
       		
        }
	else if(required != undefined && required == "textarea"){
		textarea = eval("document." + formName + "." + fieldName);
		if(! lengthValidText(textarea, requiredLength , prettyText))
		{
			return false;
		}
	}
	else if(required != undefined && required == "select"){
		selectField = eval("document." + formName + "." + fieldName);
	        if(!selectValid(selectField, prettyText)){
		    return false;
	        }
	}
	else if(required != undefined){

	    inputField = eval("document." + formName + "." + fieldName);
	    if (requiredLength > 0) {
	    	if(! lengthValid(inputField, requiredLength, prettyText)){
		    	return false;
		    }
	    }
	    
	    if(required == "date" && ! dateValid(inputField, prettyText)){
	    	return false;
	    }
	    
	    if(required == "email" && ! emailValid(inputField, prettyText)){
	    	return false;
	    }
	    
	    if(required == "number" && ! numberValid(inputField, prettyText)){
	    	return false;
	    }
	    
	    if(required == "alpha" && ! alphaValid(inputField, prettyText)){
	    	return false;
	    }
	    
	    if(required == "alpha_numeric" && ! alphanumericValid(inputField, prettyText)){
	    	return false;
	    }
	}
	
	return true;

}


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
                    alert( text + " is a not a valid date");
                    element.focus();
                    return false;
        }
        return true;
}





//function to validate by length	

function lengthValid(element, len, text) {

    text = Trim(text);

	if (element.value.length < len)
  	{
 	  	alert("Please enter a valid " + text + ".");
		element.focus();
		return false;
	}else{
		return true;
	}
}

//function to validate select drop-downs
function selectValid(element, text) {
    text = Trim(text);
	if (element[0].selected)
  	{
 	  	alert("Please select a " + text + ".");
		element.focus();
		return false;
	}else{
		return true;
	}
}

//function to validate numerical fields
function numberValid(element, text) {
    text = Trim(text);
	if (isNaN(element.value))
  	{
 	  	alert("Please enter a valid " + text + ".");
		element.focus();
		return false;
	}else{
		return true;
	}
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
            alert("Please enter a valid " + text + ".");
            element.focus();
            return false;
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
 	  	alert("Please select " + text + ".");
		element[0].focus();
		return false;
	}else{
		return true;
	}
}

//function to validate alphabetic fields 
function alphaValid(element, text) {
	text = Trim(text);
	
	for (var i = 0; i < element.value.length; ++i) {
		var char = element.value.charAt(i);
		var charCode = char.charCodeAt(0);
		if (!(((64 < charCode) && (charCode < 91)) ||
			  ((96 < charCode) && (charCode < 123)))) {
			alert("Please enter a valid " + text + ".");
			return false;
		}
	}
	
	return true;
}

//function to validate alpha-numeric fields 
function alphanumericValid(element, text) {
	text = Trim(text);
	
	for (var i = 0; i < element.value.length; ++i) {
		var char = element.value.charAt(i);
		var charCode = char.charCodeAt(0);
		if (!(((47 < charCode) && (charCode < 58)) ||
			  ((64 < charCode) && (charCode < 91)) ||
			  ((96 < charCode) && (charCode < 123)))) {
			alert("Please enter a valid " + text + ".");
			return false;
		}
	}
	
	return true;
}

//function to validate at a given number of checkboxes are checked	
function checkboxValid(element, checkboxesLength, requiredNumber,text) {
    text = Trim(text);
	var varChecked=0;
	if(checkboxesLength == undefined)
		{
		if(element.checked)
		{
			varChecked++;
		}
	}else{
		checkboxesLength=checkboxesLength-1;
		for(var checkboxestoCheck=0;checkboxestoCheck<=checkboxesLength;checkboxestoCheck++)
		{
			if(element[checkboxestoCheck].checked)
			{
				varChecked++;
			}
		}
	}
	if (varChecked < requiredNumber)
  	{
		alert("Please select at least " + requiredNumber + " " + text + ".");
		if(checkboxesLength == undefined){
			element.focus();
		}else{
			element[0].focus();
		}
		return false;
	}else{
		return true;
	}
}

//function to validate "other box" is not empty if checked	
function otherboxValid(otherradio,otherfield,len,text) {
    text = Trim(text);
	if (otherradio.checked && (otherfield.value.length<len))
  	{
 	  	alert("Please specify the " + text + ".");
		otherfield.focus();
		return false;
	}else{
		return true;
	}
}

//function to validate radio checked if checkbox checked	
function checkboxRadio(checkfield, radiofield, radios, text) {
    text = Trim(text);
	if(checkfield.checked)
	{	
		radioValid(radiofield, radios, text);
	}
}





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
=============================================================
Credit Card Returns a copy of a string without leading or trailing spaces
=============================================================
*/

function isCreditCard(st) {
  if (st.length > 19)  return (false);
  sum = 0; mul = 1; l = st.length;
  for (i = 0; i < l; i++) {
    digit = st.substring(l-i-1,l-i);
    tproduct = parseInt(digit ,10)*mul;
    if (tproduct >= 10)
      sum += (tproduct % 10) + 1;
    else
      sum += tproduct;
    if (mul == 1)
      mul++;
    else
      mul--;
  }
  if ((sum % 10) == 0)
    return (true);
  else
    return (false);
} 


function isVisa(cc)
{
  if (((cc.length == 16) || (cc.length == 13)) &&
      (cc.substring(0,1) == 4))
    return isCreditCard(cc);
  return false;
}  


function isMasterCard(cc)
{
  firstdig = cc.substring(0,1);
  seconddig = cc.substring(1,2);
  if ((cc.length == 16) && (firstdig == 5) &&
      ((seconddig >= 1) && (seconddig <= 5)))
    return isCreditCard(cc);
  return false;

}


function isAmericanExpress(cc)
{
  firstdig = cc.substring(0,1);
  seconddig = cc.substring(1,2);
  if ((cc.length == 15) && (firstdig == 3) &&
      ((seconddig == 4) || (seconddig == 7)))
    return isCreditCard(cc);
  return false;

} 


function isDiscover(cc)
{
  first4digs = cc.substring(0,4);
  if ((cc.length == 16) && (first4digs == "6011"))
    return isCreditCard(cc);
  return false;

} 

function isCardMatch (cardType, cardNumber)
{

   cardType = cardType.toUpperCase();
   var doesMatch = true;

   if ((cardType == "VISA") && (!isVisa(cardNumber)))
      doesMatch = false;
   if ((cardType == "MASTER CARD") && (!isMasterCard(cardNumber)))
      doesMatch = false;
   if ((cardType == "AMERICAN EXPRESS") && (!isAmericanExpress(cardNumber))) 
      doesMatch = false;
   if ((cardType == "DISCOVER") && (!isDiscover(cardNumber)))
      doesMatch = false;
   return doesMatch;

}  


//function to validate by length in the textarea	
function lengthValidText(element, len, text) {
	
	 text = Trim(text);
	 value = Trim(element.value);
	 if (value.length < len)
	{
		alert("Please \"" + text + "\" field is required.");
		element.focus();
		return false;
	}else{
		return true;
	}


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
            alert( text + " is a not a valid date");
            element.focus();
            return false;
        }
        today = new Date();
		expiry = new Date(year + 2000, month);
        if (today.getTime() > expiry.getTime()) {
            alert( text + " is an expired date");
            element.focus();
            return false;
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
            alert( text + " is a not a valid date");
            return false;
        }
        today = new Date();
		expiry = new Date(year + 2000, month);
        if (today.getTime() > expiry.getTime()) {
            alert( text + " is an expired date");
            return false;
        }
        return true;
}