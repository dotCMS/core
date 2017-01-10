<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script language="Javascript">
<%--
 * This file contains all the Javascript function for drawing the template.
 *
 * @author Graziano Aliberti
 *
 --%>

var addContainerMSG;
var removeContainerMSG;

var countAddContainerLinks;
var countContainersAdded;
var containersAdded = [];


function drawDefault(overrideBody, addContainer, removeContainer,containersStr,containersLen){
	addContainerMSG = addContainer;
	removeContainerMSG = removeContainer;
	countAddContainerLinks = document.getElementById("countAddContainerLinks");
	countContainersAdded = document.getElementById("countContainersAdded");
	var mainTemplateDiv = document.getElementById("bodyTemplate");
	var textareaDrawedBodyHidden = document.getElementById("drawedBodyField");
	var textareaBodyHidden = document.getElementById("bodyField");
	if(!overrideBody){
		//set the main div
		var pageWidth = dijit.byId("pageWidth").attr("value");
		var mainDiv = document.createElement("div");
		mainDiv.setAttribute("id",pageWidth);
		mainDiv.setAttribute("name","globalContainer");
		mainTemplateDiv.insertBefore(mainDiv,mainTemplateDiv.firstChild);
		//adding the header
		addHeader(true);
		//adding the footer
		addFooter(true);
		//adding the body div
		var bodyDiv = document.createElement("div");
		bodyDiv.setAttribute("id","bd-template");
		var yuiMainDiv = document.createElement("div");
		yuiMainDiv.setAttribute("id","yui-main-template");
		var yuiBDiv1 = document.createElement("div");
		yuiBDiv1.setAttribute("class","yui-b-template");
		yuiBDiv1.setAttribute("id","splitBody0");
		yuiBDiv1.innerHTML=getAddContainer("splitBody0")+"<h1>Body</h1>";
		yuiMainDiv.appendChild(yuiBDiv1);
		bodyDiv.appendChild(yuiMainDiv);
		mainDiv.insertBefore(bodyDiv,document.getElementById("ft-template"));
	}else{
		mainTemplateDiv.innerHTML=textareaDrawedBodyHidden.value;
		var containerArray = new Array();
		 containerArray = containersStr.split(',',containersLen);
		 var existedContainers = mainTemplateDiv.getElementsByTagName("H2");
		 var existedContLen = existedContainers.length;
		 if(existedContLen == containersLen){
			 for(var i = 0; i < containersLen; i++) {
				 if(existedContainers[i].innerHTML.substring(11) != containerArray[i]){
					 mainTemplateDiv.getElementsByTagName("H2")[i].innerHTML = mainTemplateDiv.getElementsByTagName("H2")[i].innerHTML.replace(existedContainers[i].innerHTML.substring(11),containerArray[i]);
				 }
	        }
		 }
	}
	textareaDrawedBodyHidden.value="";
	textareaBodyHidden.value="";

    //In order to keep a list of the containers used by this template
    parseCurrentContainers();
}

function addRow(tableID,prefixSelect,prefixDiv) {
	var table = document.getElementById(tableID);
	var rowCount = table.rows.length;
    var row = table.insertRow(rowCount);
    row.setAttribute("id","_selectRow"+rowCount);
    row.setAttribute("class","spaceUnder");
    var cell1 = row.insertCell(0);
    var oImg = document.createElement("img");
    oImg.setAttribute("src", "/html/images/icons/cross.png");
    oImg.setAttribute("alt", "delete");
    oImg.setAttribute("title", "delete row");
    oImg.style.cursor="pointer";
    oImg.onclick = function(){deleteRow(tableID,prefixSelect+(rowCount),prefixDiv+(rowCount),rowCount);};
    cell1.appendChild(oImg);
    var cell2 = row.insertCell(1);
    var select = document.createElement("select");
    try{
	    select.setAttribute("name", prefixSelect+(rowCount));
	    select.setAttribute("id", prefixSelect+(rowCount));
	    select.setAttribute("dojoType", "dijit.form.FilteringSelect");
	    createOption(select, "1", "<%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %>",true);
	    createOption(select, "yui-g-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-5050") %>",false);
	    createOption(select, "yui-gc-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %>",false);
	    createOption(select, "yui-gd-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %>",false);
	    createOption(select, "yui-ge-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %>",false);
	    createOption(select, "yui-gf-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %>",false);
	    createOption(select, "yui-gb-template", "<%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %>",false);
	    createOption(select, "yui-js-template", "<%= LanguageUtil.get(pageContext, "body-rows-4-column-25252525") %>",false);
	    select.onchange=function(){addGrid(select.value, select.getAttribute("name"));};
	    cell2.appendChild(select);
	    dojo.parser.parse(cell2);
	    dojo.connect(dijit.byId(prefixSelect+(rowCount)), "onChange", this, function(e){addGrid(e, prefixDiv+(rowCount), rowCount);});
	    addGrid(1,prefixDiv+(rowCount),rowCount);
    }catch(err){
    	cell2.removeChild(select);
    	rowCount-=1;
	    cell2 = row.insertCell(1);
	    select = document.createElement("select");
	    select.setAttribute("name", prefixSelect+(rowCount));
	    select.setAttribute("id", prefixSelect+(rowCount));
	    select.setAttribute("dojoType", "dijit.form.FilteringSelect");
	    createOption(select, "1", "<%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %>",true);
	    createOption(select, "yui-g-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-5050") %>",false);
	    createOption(select, "yui-gc-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %>",false);
	    createOption(select, "yui-gd-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %>",false);
	    createOption(select, "yui-ge-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %>",false);
	    createOption(select, "yui-gf-template", "<%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %>",false);
	    createOption(select, "yui-gb-template", "<%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %>",false);
	    createOption(select, "yui-js-template", "<%= LanguageUtil.get(pageContext, "body-rows-4-column-25252525") %>",false);
	    select.onchange=function(){addGrid(select.value, select.getAttribute("name"));};
	    cell2.appendChild(select);
	    dojo.parser.parse(cell2);
	    dojo.connect(dijit.byId(prefixSelect+(rowCount)), "onChange", this, function(e){addGrid(e, prefixDiv+(rowCount), rowCount);});
	    addGrid(1,prefixDiv+(rowCount),rowCount);
    }
}

function createOption(select, attribute_value, inner_html, selected){
    var option;
    option = document.createElement("option");
    option.setAttribute("value", attribute_value);
    if(selected)
    	option.setAttribute("selected", "selected");
    option.innerHTML = inner_html;
    select.appendChild(option);

}

function deleteRow(tableID, row, div, rowCountId) {
	//delete the combo row
   	var table = document.getElementById(tableID);
   	var rowCount = table.rows.length;
   	for(var i=1; i<rowCount; i++){
   		var riga = table.rows[i];
   		if(null!=riga){
   			if(riga.id=="_selectRow"+rowCountId){
   				var selectDijit = dijit.byId(row);
   	        	if (selectDijit) {
   	        		selectDijit.destroy();
   	        	}
   	        	dojo.destroy(row);
   				table.deleteRow(i);
   			}
   		}
   	}
   	//remove the div into the body
   	removeGrid(div);

    //In order to keep a list of the containers used by this template
    parseCurrentContainers();
}

function addPageWidth(pageWidth){
	var mainDiv = document.getElementsByName("globalContainer")[0];
	mainDiv.removeAttribute("id");
	mainDiv.setAttribute("id",pageWidth);
}

function addHeader(checked){
	var pageWidth = dijit.byId("pageWidth").attr("value");

	if(checked){ //adding the header div
		var mainDiv = document.getElementById(pageWidth);
		var headerDiv = document.createElement("div");
		headerDiv.setAttribute("id","hd-template");
//		headerDiv.innerHTML=getAddContainer("hd-template")+"<h1>Header</h1>";
		headerDiv.innerHTML="<h1>Header</h1>";
		//adding at the first position
		mainDiv.insertBefore(headerDiv,mainDiv.firstChild);
	} else { //delete the header div
		var div = document.getElementById("hd-template");
		div.parentNode.removeChild(div);
	}
}

function addFooter(checked){
	var pageWidth = dijit.byId("pageWidth").attr("value");
	if(checked){ //adding the footer div
		var mainDiv = document.getElementById(pageWidth);
		var footerDiv = document.createElement("div");
		footerDiv.setAttribute("id","ft-template");
//		footerDiv.innerHTML=getAddContainer("ft-template")+"<h1>Footer</h1>";
		footerDiv.innerHTML="<h1>Footer</h1>";
		// adding at the last position (append)
		mainDiv.appendChild(footerDiv);
	} else { //delete the footer div
		var div = document.getElementById("ft-template");
		div.parentNode.removeChild(div);
	}
}

function addLayout(layout){
	var idMainDiv = dijit.byId("pageWidth").attr("value");
	var mainDiv = document.getElementById(idMainDiv);
	var yuiB2 = document.getElementById("yui-b2");
	var bodyDiv = document.getElementById("bd-template");
	if("none"!=layout){
		if(null!=mainDiv.getAttribute("class"))
			mainDiv.removeAttribute("class");
		mainDiv.setAttribute("class",layout);
		if(null==yuiB2){
			//adding the "yui-b" div for the sidebar
			var yuiBDiv = document.createElement("div");
			yuiBDiv.setAttribute("class","yui-b-template");
			yuiBDiv.setAttribute("id","yui-b2");
			yuiBDiv.style.height="70%";
			yuiBDiv.innerHTML=getAddContainer("yui-b2")+"<h1>Sidebar</h1>";
			bodyDiv.appendChild(yuiBDiv);
		}
	}else{
		mainDiv.removeAttribute("class");
		if(null!=yuiB2)
			bodyDiv.removeChild(yuiB2);
	}
}

function addGrid(gridId, yuiBId, rowCount){


	var mainDiv = document.getElementById("yui-main-template");
	var yuiBDiv = document.getElementById(yuiBId);
	var first = document.getElementById(rowCount+"_yui-u-grid-1");
    var second = document.getElementById(rowCount+"_yui-u-grid-2");
    var third = document.getElementById(rowCount+"_yui-u-grid-3");
    var existing=[ ];
	if(null==yuiBDiv){ // create
		yuiBDiv = document.createElement("div");
		yuiBDiv.setAttribute("class","yui-b-template");
		yuiBDiv.setAttribute("id",yuiBId);
		mainDiv.appendChild(yuiBDiv);
	}else{
		//delete its content
		while (yuiBDiv.hasChildNodes()) {
			existing.push(yuiBDiv.lastChild);
			yuiBDiv.removeChild(yuiBDiv.lastChild);
		}

		yuiBDiv.setAttribute("class","yui-b-template");
		yuiBDiv.setAttribute("id",yuiBId);
	}
	if(1!=gridId){
		// delete the first Add Container link created when was created the grid (100% body, default creation)
		var gridDiv = document.createElement("div");
		var yuiUFirst = document.createElement("div");
		var yuiU2 = document.createElement("div");
		var yuiU3 = document.createElement("div");
		var yuiU4 = document.createElement("div");

		var gridDiv_2 = null;
		var gridContainer = null;

		("yui-js-template"==gridId) ? gridDiv.setAttribute("class","yui-g-template") : gridDiv.setAttribute("class",gridId);
		("yui-js-template"==gridId) ? gridDiv.setAttribute("id","yui-g-template") : gridDiv.setAttribute("id",gridId);

		yuiUFirst.setAttribute("class","yui-u-template first");
		yuiUFirst.setAttribute("id",rowCount+"_yui-u-grid-1");
		if(first!= null){
            yuiUFirst.innerHTML=first.innerHTML;
    	}else{
			yuiUFirst.innerHTML=getAddContainer(rowCount+"_yui-u-grid-1")+"<h1>Body</h1>";
    	}
		
		yuiU2.setAttribute("class","yui-u-template");
		yuiU2.setAttribute("id",rowCount+"_yui-u-grid-2");
		if(second!= null){
            yuiU2.innerHTML=second.innerHTML;
    	}else{
			yuiU2.innerHTML=getAddContainer(rowCount+"_yui-u-grid-2")+"<h1>Body</h1>";
    	}
		gridDiv.appendChild(yuiUFirst);
		gridDiv.appendChild(yuiU2);

		if("yui-gb-template"==gridId){
			yuiU3.setAttribute("class","yui-u-template");
			yuiU3.setAttribute("id",rowCount+"_yui-u-grid-3");
			if(third!= null){
				yuiU3.innerHTML=third.innerHTML;
        	}else{
				yuiU3.innerHTML=getAddContainer(rowCount+"_yui-u-grid-3")+"<h1>Body</h1>";
        	}
			gridDiv.appendChild(yuiU3);
		}
		if("yui-js-template"==gridId){

			gridDiv_2 = document.createElement("div");
			gridDiv_2.setAttribute("class","yui-g-template");
			gridDiv_2.setAttribute("id","yui-g-template");
			gridDiv_2.setAttribute("style", "margin-left:10px");

			var yuiUFirst_2 = document.createElement("div");
			var yuiU2_2 = document.createElement("div");

			yuiUFirst_2.setAttribute("class","yui-u-template first");
			yuiUFirst_2.setAttribute("id",rowCount+"_yui-u-grid-3");
			if(third!= null){
				yuiUFirst_2.innerHTML=third.innerHTML;
        	}else{
				yuiUFirst_2.innerHTML=getAddContainer(rowCount+"_yui-u-grid-3")+"<h1>Body</h1>";
        	}
			gridDiv_2.appendChild(yuiUFirst_2);

			yuiU2_2.setAttribute("class","yui-u-template");
			yuiU2_2.setAttribute("id",rowCount+"_yui-u-grid-4");
			yuiU2_2.innerHTML=getAddContainer(rowCount+"_yui-u-grid-4")+"<h1>Body</h1>";
			gridDiv_2.appendChild(yuiU2_2);

			gridContainer = document.createElement("div");
			gridContainer.setAttribute("class","yui-g-template");
			gridContainer.setAttribute("id","yui-js-template");
			gridContainer.appendChild(gridDiv);
			gridContainer.appendChild(gridDiv_2);

		}


		if("yui-js-template"==gridId){
			yuiBDiv.appendChild(gridContainer);
		} else  {
			yuiBDiv.appendChild(gridDiv);
		}
		
		// recover containers [single column] -> [many columns]
		if(existing.length>1) {
			existing.forEach(function(el){
				if(el.className=="titleContainerSpan") {
					var cid=dojo.attr(el,"title").replace("container_","");
					addDrawedContainer({value:yuiUFirst.id},{title:el.getElementsByTagName("H2")[0].innerHTML.substring(11)},cid)
				}
			});
		}
	} else {
		if(second!= null) {
			// recover containers [many columns] -> [single column]
			yuiBDiv.innerHTML=getAddContainer(yuiBId)
			first.getElementsByClassName("titleContainerSpan").forEach(function(el){
				var cid=dojo.attr(el,"title").replace("container_","");
				addDrawedContainer({value:yuiBDiv.id},{title:el.getElementsByTagName("H2")[0].innerHTML.substring(11)},cid)
			});
		}
		else {
			yuiBDiv.innerHTML=getAddContainer(yuiBId)+"<h1>Body</h1>";
		}
	}




    //In order to keep a list of the containers used by this template
    parseCurrentContainers();
}


function removeGrid(yuiBId){
	var childToRemove = document.getElementById(yuiBId);
	var mainDiv = document.getElementById("yui-main-template");
	mainDiv.removeChild(childToRemove);
}

/**
 * This function add a container into a div in the design template phase.
 *
 * @param idDiv
 * @param container
 * @param value
 */
function addDrawedContainer(idDiv, container, value, error_msg, container_exist){
	var div_container = document.getElementById(idDiv.value+"_div_"+value);
	var span_container = document.getElementById(idDiv.value+"_span_"+value);

	if(null!=div_container && null!=span_container){
		alert(error_msg);
		return;
	}

	if(container.maxContentlets>0 && hasContainer(value)){
		alert(container_exist);
		return;
	}

	//create the container span
	var titleContainerSpan = document.createElement("span");
	titleContainerSpan.setAttribute("class", "titleContainerSpan");
	titleContainerSpan.setAttribute("id", idDiv.value+"_span_"+value);
	titleContainerSpan.setAttribute("title","container_"+value);
	titleContainerSpan.innerHTML=getRemoveContainer(idDiv.value,value)+"<div class=\"clear\"></div>"+getContainerMockContent(container.title);

	var containerDivHidden = document.createElement("div");

	containerDivHidden.setAttribute("style","display: none;");
	//set the title for better recognize the container's div
	containerDivHidden.setAttribute("title","container_"+value);
	containerDivHidden.setAttribute("id", idDiv.value+"_div_"+value);
	containerDivHidden.innerHTML='#parseContainer(\'' + value + '\')\n';

	var div = document.getElementById(idDiv.value);

	/*
		***************************************************************************************
		Check if the div has already containers: in this case we don't remove all the children
		***************************************************************************************
	*/
	var containers = new Array();
	if (div.hasChildNodes()){
		var nodes = div.childNodes;
		for(var i=0; i<nodes.length; i++) {
			var child = nodes[i];
			var child_title = child.getAttribute("title");
			if(null!=child_title && child_title.indexOf("container")>=0) //this element is a container
				containers.push(child);
		}
	}
	if(idDiv.value!="ft-template" && idDiv.value!="hd-template"){ //if the element is not a header or footer than I add the "add container" link
		//set the add container link + the previuos containers
		div.innerHTML=getAddContainer(idDiv.value);
		for(var i=0; i<containers.length; i++)
			div.appendChild(containers[i]);
	}else{ // in this case we don't need other containers...in header or footer only one container is predicted
		div.innerHTML="";
	}

	div.appendChild(titleContainerSpan);
	div.appendChild(containerDivHidden);

    //In order to keep a list of the containers used by this template
    parseCurrentContainers();
}

function removeDrawedContainer(idDiv,idContainer){
	var div = document.getElementById(idDiv);
	var containers = new Array();
	var containerToRemove = new Array();
	if (div.hasChildNodes()){
		var nodes = div.childNodes;
		for(var i=0; i<nodes.length; i++) {
			var child = nodes[i];
			var child_title = child.getAttribute("title");
			if(null!=child_title && child_title.indexOf("container")>=0) //this element is a container
				containers.push(child);
		}
	}
	if(div.hasChildNodes()){
		var nodes = div.childNodes;
		for(var i=0; i<nodes.length; i++) {
			var child = nodes[i];
			if(child.getAttribute("id")==idDiv+"_span_"+idContainer || child.getAttribute("id")==idDiv+"_div_"+idContainer) //this element is a container
				containerToRemove.push(child);
		}
	}

	//remove from div
	for(var i=0; i<containerToRemove.length; i++)
		div.removeChild(containerToRemove[i]);

	if(idDiv=="ft-template" || idDiv=="hd-template"){
		div.innerHTML=getAddContainer(idDiv);
		if(idDiv=="ft-template")
			div.innerHTML+="<h1>Footer</h1>";
		else
			div.innerHTML+="<h1>Header</h1>";
	}else if(containers.length==2){ //this container was the last one into the section...
		if(idDiv!="yui-b2")
			div.innerHTML+="<h1>Body</h1>";
		else
			div.innerHTML+="<h1>Sidebar</h1>";
	}

    //In order to keep a list of the containers used by this template
    parseCurrentContainers();
}

/**
 * This function adds a new file (JS or CSS) to template.
 *
 * 	-	Into a hidden div puts all the generated HTML for script/link;
 * 	-	Create for each file a div with right icon and file name.
 *
 * @param html
 */
function addFileToTemplate(html, file, error_msg){

	var spanFile = document.getElementById("span_"+file.inode+"_"+file.parent+"_"+file.isContentlet);
	if(null!=spanFile){ //this file already exists into the template
		alert(error_msg);
		return;
	}else{
		//create the span
		spanFile = document.createElement("span");
		spanFile.setAttribute("id", "span_"+file.inode+"_"+file.parent+"_"+file.isContentlet);
		spanFile.setAttribute("class", file.extension+"Span");

		spanIcon = document.createElement("span");
		spanIcon.setAttribute("class", file.extension+"Icon");
		spanIcon.innerHTML="<h3>"+file.fileName+"</h3>";

		//create the remove file link
		var removeFile = document.createElement("div");
		removeFile.setAttribute("class", "removeDiv");
		removeFile.innerHTML='<a href="javascript: removeFile(\''+file.inode+"_"+file.parent+"_"+file.isContentlet+'\');" title="Remove File"><span class="minusIcon"></span>Remove File</a></div>';
		spanFile.appendChild(spanIcon);
		spanFile.appendChild(removeFile);
		var divForSpanFiles = document.getElementById("fileContainerDiv");
		if(null==divForSpanFiles){
			divForSpanFiles = document.createElement("div");
			divForSpanFiles.setAttribute("id", "fileContainerDiv");
			document.getElementById("bodyTemplate").insertBefore(divForSpanFiles, document.getElementById("bodyTemplate").firstChild);
		}
		divForSpanFiles.appendChild(spanFile);
		var divHiddenForFiles = document.getElementById("jsCssToAdd");

		if(null==divHiddenForFiles){
			//we must create
			divHiddenForFiles = document.createElement("div");
			divHiddenForFiles.setAttribute("id", "jsCssToAdd");
			divHiddenForFiles.style.display="none";
		}

		// append the new file
		var divHiddenSingleFile = document.createElement("div");
		divHiddenSingleFile.setAttribute("id", "div_"+file.inode+"_"+file.parent+"_"+file.isContentlet);
		divHiddenSingleFile.style.display="none";
		divHiddenSingleFile.innerHTML='<!-- '+html+' -->';
		divHiddenForFiles.appendChild(divHiddenSingleFile);

		//add at the first position into the bodyTemplate div
		var bodyTemplate = document.getElementById("bodyTemplate");
		bodyTemplate.insertBefore(divHiddenForFiles, bodyTemplate.firstChild);
	}
}

function removeFile(inode){
	var divHiddenForFiles = document.getElementById("jsCssToAdd");
	if(divHiddenForFiles.hasChildNodes()) {
		var nodes = divHiddenForFiles.childNodes;
		for(var i=0; i<nodes.length; i++){
			var child = nodes[i];
			if(child.getAttribute("id")=="div_"+inode)
				divHiddenForFiles.removeChild(child);
		}
	}
	var divForSpanFiles = document.getElementById("fileContainerDiv");
	if(divForSpanFiles.hasChildNodes()) {
		var nodes = divForSpanFiles.childNodes;
		for(var i=0; i<nodes.length; i++){
			var child = nodes[i];
			if(child.getAttribute("id")=="span_"+inode)
				divForSpanFiles.removeChild(child);
		}
	}
}

function printBody(){
	var textareaBodyHidden = document.getElementById("bodyField");
	textareaBodyHidden.value=document.getElementById("bodyTemplate").innerHTML;
	alert(textareaBodyHidden.value);
}

function saveBody(){
	var textareaBodyHidden = document.getElementById("bodyField");
	textareaBodyHidden.value=document.getElementById("bodyTemplate").innerHTML;
}

function getMockContent(){
	return '<span class="mockContent">Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur?</span>';
}

function getAddContainer(idDiv){
	return '<div class="addContainerSpan"><a href="javascript: showAddContainerDialog(\''+idDiv+'\');" title="'+addContainerMSG+'"><span class="plusBlueIcon"></span>'+addContainerMSG+'</a></div>';
}

function getRemoveContainer(idDiv, idContainer){
	return '<div class="removeDiv"><a href="javascript: removeDrawedContainer(\''+idDiv+'\',\''+idContainer+'\');" title="'+removeContainerMSG+'"><span class="minusIcon"></span>'+removeContainerMSG+'</a></div>';
}

function hasContainer(value){
	var body = document.getElementById("bodyTemplate").innerHTML;
	return body.indexOf(value)>0;
}

/**
 * Update the counter of the "Add Container" links
 */
function updateAddContainerLinksCount() {
    countAddContainerLinks.value = dojo.query(".addContainerSpan").length;
}

/**
 * Method that will parse the current template code in order to get the list of containers
 * used by this template.
 */
function parseCurrentContainers() {

    containersAdded = [];
    dojo.query('div[title*="container_"]').forEach(function (node) {
        var title = dojo.attr(node, "title");
        title = title.replace("container_", "");
        containersAdded.push(title);
    });

    countContainersAdded.value = containersAdded.length;
}

</script>