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

function showAddMetatagDialog(){
	
	
	dijit.byId("dialogOne").show();
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

function getMetatagTableRows(){
	var metatags = new Array();
	var tableContent = document.getElementById(TABLE_CONTENTS);
	var rowCount = tableContent.rows.length;
	for(var i=1; i<rowCount; i++){
		var row = tableContent.rows[i];
		metatags.push(row);
	}
	return metatags;	
}

function saveMetaAndHeaderCode(){
	// get the array of meta tags
	var metatagArr = getMetatagTableRows();
	removeAllMetadataTag();
	for(var i=0; i<metatagArr.length; i++){
		var metaTag = metatagArr[i];
		// create span for view the metatag
		var spanID = "span_METATAG_"+metaTag.cells[1].innerHTML+"_"+metaTag.cells[2].innerHTML+"_"+metaTag.cells[3].innerHTML;
		var divID = "div_METATAG_"+metaTag.cells[1].innerHTML+"_"+metaTag.cells[2].innerHTML+"_"+metaTag.cells[3].innerHTML;
		if(null!=document.getElementById(spanID))
			removeMetadataTag(spanID,divID);
		
		spanMT = document.createElement("span");
		spanMT.setAttribute("id", spanID);
		spanMT.setAttribute("class", "metatagSpan");
		spanIcon = document.createElement("span");
		spanIcon.setAttribute("class", "metatagIcon");
		spanIcon.innerHTML="<h3>Metatag: "+metaTag.cells[1].innerHTML+"=\""+metaTag.cells[2].innerHTML+"\" content=\""+metaTag.cells[3].innerHTML+"\"</h3>";
		//create the remove MT link
		var removeMT = document.createElement("div");
		removeMT.setAttribute("class", "removeDiv");
		removeMT.innerHTML='<a href="javascript: removeMetadataTag(\''+spanID+'\',\''+divID+'\');" title="Remove Metatag"><span class="minusIcon"></span>Remove Metatag</a></div>';
		spanMT.appendChild(spanIcon);
		spanMT.appendChild(removeMT);
		
		var divForSpanFiles = document.getElementById("fileContainerDiv");
		if(null==divForSpanFiles){
			divForSpanFiles = document.createElement("div");
			divForSpanFiles.setAttribute("id", "fileContainerDiv");
			document.getElementById("bodyTemplate").insertBefore(divForSpanFiles, document.getElementById("bodyTemplate").firstChild);
		}
		divForSpanFiles.appendChild(spanMT);			

		var divHiddenForMT = document.getElementById("metatagToAdd");
		
		if(null==divHiddenForMT){
			//we must create
			divHiddenForMT = document.createElement("div");
			divHiddenForMT.setAttribute("id", "metatagToAdd");
			divHiddenForMT.style.display="none";
		}
		
		// append the new Metatag
		var divHiddenSingleMT = document.createElement("div");
		divHiddenSingleMT.setAttribute("id", divID);
		divHiddenSingleMT.style.display="none";
		divHiddenSingleMT.innerHTML='<!-- '+metaTag.cells[4].childNodes[0].nodeValue+' -->';
		divHiddenForMT.appendChild(divHiddenSingleMT);
		
		//add at the first position into the bodyTemplate div
		var bodyTemplate = document.getElementById("bodyTemplate");
		bodyTemplate.insertBefore(divHiddenForMT, bodyTemplate.firstChild);
		
	}
	dijit.byId('dialogOne').hide();
}

function removeMetadataTag(idSpan, idDiv){
	var divForSpanFiles = document.getElementById("fileContainerDiv");
	var hiddenMetatagDiv = document.getElementById("metatagToAdd");
	if(null!=divForSpanFiles){
		divForSpanFiles.removeChild(document.getElementById(idSpan));
	}
	if(null!=hiddenMetatagDiv){
		hiddenMetatagDiv.removeChild(document.getElementById(idDiv));
	}
	
}

function removeAllMetadataTag(){	
	var divForSpanFiles = document.getElementById("fileContainerDiv");
	var hiddenMetatagDiv = document.getElementById("metatagToAdd");
	if(null!=divForSpanFiles){
		if(divForSpanFiles.hasChildNodes()) {
			var nodes = divForSpanFiles.childNodes;
			for(var i=0; i<nodes.length; i++){
				var child = nodes[i];
				if(beginWith(child.getAttribute("id"),"span_METATAG_"))
					divForSpanFiles.removeChild(child);
			}
		}
	}
	if(null!=hiddenMetatagDiv){
		if(hiddenMetatagDiv.hasChildNodes()) {
			var nodes = hiddenMetatagDiv.childNodes;
			for(var i=0; i<nodes.length; i++){
				var child = nodes[i];
				hiddenMetatagDiv.removeChild(child);
			}
		}
	}
}

function beginWith(stringToCheck, valueToCheck){
	if(stringToCheck.substr(0, valueToCheck.length) == valueToCheck){
		return true;
    }
	return false;
}

function buildMetaTagHTML(attribute, attributeValue, contentValue){
	return '<meta '+attribute+'="'+attributeValue+'" content="'+contentValue+'" />';
}
