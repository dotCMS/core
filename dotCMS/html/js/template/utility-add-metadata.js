/**
 * This file contains all the Javascript function for adding the meta tag to template. 
 * 
 * @author Graziano Aliberti
 * 
 */

var NAME_DIV = "name-div";
var HTTP_EQUIV_DIV = "http-equiv-div";
var NAME_SELECT = "name-attribute";
var HTTP_EQUIV_SELECT = "http-equiv-attribute";
var CONTENT_INPUT_TEXT = "content-attribute";
var TABLE_CONTENTS = "metadataTable";


	
function showSelectedAttribute(choose){
	if(choose==0){ //name attribute
		document.getElementById(NAME_DIV).style.display="block";
		document.getElementById(HTTP_EQUIV_DIV).style.display="none";
	}else if(choose==1){ //http-equiv attribute
		document.getElementById(NAME_DIV).style.display="none";
		document.getElementById(HTTP_EQUIV_DIV).style.display="block";			
	}else{
		document.getElementById(NAME_DIV).style.display="none";
		document.getElementById(HTTP_EQUIV_DIV).style.display="none";
	}
}

function addMetatag(){
	var nameDiv = document.getElementById(NAME_DIV);
	var httpEquivDiv = document.getElementById(HTTP_EQUIV_DIV);
	
	if((nameDiv.style.display=="none") && (httpEquivDiv.style.display=="none")){
		alert('Yuo must set at least one of "Name" or "Http-Equiv" field.');
		return;
	}else if((nameDiv.style.display=="none")){ // http-equiv
		// get the httpEquivDiv value
		var httpEquivValue = dijit.byId(HTTP_EQUIV_SELECT).attr("value");
		// get the content value
		var contentValue = dijit.byId(CONTENT_INPUT_TEXT).attr("value");
		
		if(document.getElementById(httpEquivValue+"_"+contentValue)==null){
			// get the table
			var tableContent = document.getElementById(TABLE_CONTENTS);
			var rowCount = tableContent.rows.length;
			
			var row = tableContent.insertRow(rowCount);
			row.setAttribute("id",httpEquivValue+"_"+contentValue);
			var cellAction = row.insertCell(0);
			var oImg = document.createElement("img");
		    oImg.setAttribute("src", "/html/images/icons/cross.png");
		    oImg.setAttribute("alt", "delete");
		    oImg.setAttribute("title", "delete row");
		    oImg.style.cursor="pointer";
		    oImg.onclick = function(){deleteMetadataRow(httpEquivValue+"_"+contentValue);};
		    cellAction.appendChild(oImg);
			var cellAttribute = row.insertCell(1);
			cellAttribute.setAttribute("style","text-align: center;");
			cellAttribute.innerHTML="http-equiv";
			var cellAttributeValue = row.insertCell(2);
			cellAttributeValue.setAttribute("style","text-align: center;");
			cellAttributeValue.innerHTML=httpEquivValue;
			var cellContent = row.insertCell(3);
			cellContent.setAttribute("style","text-align: center;");
			cellContent.innerHTML=contentValue;		
			var cellHTML = row.insertCell(4);
			cellHTML.setAttribute("style","font-weight: bold; text-align: center;");
			text = document.createTextNode(buildMetaTagHTML("http-equiv",httpEquivValue,contentValue));
			cellHTML.appendChild(text);			
		}else{
			alert('This tag already exist.');
			return;
		}
	}else { // name
		// get the httpEquivDiv value
		var nameValue = dijit.byId(NAME_SELECT).attr("value");
		// get the content value
		var contentValue = dijit.byId(CONTENT_INPUT_TEXT).attr("value");
		
		if(document.getElementById(nameValue+"_"+contentValue)==null){
			// get the table
			var tableContent = document.getElementById(TABLE_CONTENTS);
			var rowCount = tableContent.rows.length;
			
			var row = tableContent.insertRow(rowCount);
			row.setAttribute("id",nameValue+"_"+contentValue);
			var cellAction = row.insertCell(0);
			var oImg = document.createElement("img");
		    oImg.setAttribute("src", "/html/images/icons/cross.png");
		    oImg.setAttribute("alt", "delete");
		    oImg.setAttribute("title", "delete row");
		    oImg.style.cursor="pointer";
		    oImg.onclick = function(){deleteMetadataRow(nameValue+"_"+contentValue);};
			cellAction.appendChild(oImg);
			var cellAttribute = row.insertCell(1);
			cellAttribute.setAttribute("style","text-align: center;");
			cellAttribute.innerHTML="name";
			var cellAttributeValue = row.insertCell(2);
			cellAttributeValue.setAttribute("style","text-align: center;");
			cellAttributeValue.innerHTML=nameValue;
			var cellContent = row.insertCell(3);
			cellContent.setAttribute("style","text-align: center;");
			cellContent.innerHTML=contentValue;		
			var cellHTML = row.insertCell(4);
			cellHTML.setAttribute("style","font-weight: bold; text-align: center;");
			text = document.createTextNode(buildMetaTagHTML("name",nameValue,contentValue));
			cellHTML.appendChild(text);
		}else{
			alert('This tag already exist.');
			return;
		}		
	}
}

function deleteMetadataRow(idRow){
	var tableContent = document.getElementById(TABLE_CONTENTS);
	var rowCount = tableContent.rows.length;
	for(var i=1; i<rowCount; i++){
		var row = tableContent.rows[i];
		if(row.id==idRow)
			tableContent.deleteRow(i);
	}
}

function getHTMLMetatagCode(){
	var metatags = new Array();
	var tableContent = document.getElementById(TABLE_CONTENTS);
	var rowCount = tableContent.rows.length;
	for(var i=1; i<rowCount; i++){
		var row = tableContent.rows[i];
		var cellCount = row.cells.length;
		var code = row.cells[cellCount-1].childNodes[0].nodeValue;
		metatags.push(code);
	}
	return metatags;
}

function saveMetaAndHeaderCode(){
	// get the array of HTML meta tag
	var metatagArr = getHTMLMetatagCode();
	for(var i=0; i<metatagArr.length; i++){
		
	}
}

function buildMetaTagHTML(attribute, attributeValue, contentValue){
	return '<meta '+attribute+'="'+attributeValue+'" content="'+contentValue+'" />';
}
