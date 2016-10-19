/**
 * This file contains all the Javascript function for adding the head code to template. 
 * 
 * @author Graziano Aliberti
 * 
 */

function addHeadCode() {
	dijit.byId('headerCode').hide();
	
	// gets the head code to add
	var editorText = editor.getCode();
	
	// create the span that contain this code
	var spanMT = document.createElement("span");
	spanMT.setAttribute("id", "span_head_code");
	spanMT.setAttribute("class", "headCodeSpan");
	spanIcon = document.createElement("span");
	spanIcon.setAttribute("class", "headCodeIcon");
	spanIcon.innerHTML="<h3>Head Code</h3>";
	spanMT.appendChild(spanIcon);
	
	// add to document 
	var divForSpanFiles = document.getElementById("fileContainerDiv");
	if(null==divForSpanFiles){
		divForSpanFiles = document.createElement("div");
		divForSpanFiles.setAttribute("id", "fileContainerDiv");
		document.getElementById("bodyTemplate").insertBefore(divForSpanFiles, document.getElementById("bodyTemplate").firstChild);
	}
	divForSpanFiles.appendChild(spanMT);
}