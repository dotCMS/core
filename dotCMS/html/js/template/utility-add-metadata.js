/**
 * This file contains all the Javascript function for adding the meta tag to template. 
 * 
 * @author Graziano Aliberti
 * 
 */

/**
 * This function add a metatag container to template.
 */
function addDrawedMetadataContainer(container, value, container_exist){
	var divID = "div_"+value;
	var spanID = "span_"+value;
	var spanMT = document.getElementById(spanID);
	var divHiddenSingleMT = document.getElementById(divID);	
	if(null!=spanMT && null!=divHiddenSingleMT){
		alert(container_exist);
		return;
	}else{
		spanMT = document.createElement("span");
		spanMT.setAttribute("id", "span_"+value);
		spanMT.setAttribute("class", "metatagSpan");
		spanIcon = document.createElement("span");
		spanIcon.setAttribute("class", "metatagIcon");
		spanIcon.innerHTML="<h3>Metatag: "+container.title+"</h3>";
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
		divHiddenSingleMT = document.createElement("div");
		divHiddenSingleMT.setAttribute("id", divID);
		divHiddenSingleMT.style.display="none";
		var parseText = document.createTextNode('#parseContainer(\'' + value + '\')\n');
		divHiddenSingleMT.appendChild(parseText);
		divHiddenForMT.appendChild(divHiddenSingleMT);
		
		//add at the first position into the bodyTemplate div
		var bodyTemplate = document.getElementById("bodyTemplate");
		bodyTemplate.insertBefore(divHiddenForMT, bodyTemplate.firstChild);
	}
}

/**
 * This function remove a metatag container from template.
 */
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