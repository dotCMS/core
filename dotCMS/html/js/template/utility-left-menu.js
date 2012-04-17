function drawDefault(){
//	var mainFrame = frames["mainFrame"];
	var mainTemplateDiv = document.getElementById("bodyTemplate");
	//setto il main div
	var pageWidth = dijit.byId("pageWidth").attr("value");
	var mainDiv = document.createElement("div");
	mainDiv.setAttribute("id",pageWidth);
	mainDiv.setAttribute("name","globalContainer");
	mainTemplateDiv.insertBefore(mainDiv,mainTemplateDiv.firstChild);
	//inserisco header
	addHeader(true);
	//inserisco footer
	addFooter(true);
	//inserisco anche il div contenente il body del documento
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
}

function addRow(tableID,prefixSelect,prefixDiv) { 
	var table = document.getElementById(tableID);
	var rowCount = table.rows.length;
    var row = table.insertRow(rowCount);
    row.setAttribute("class","spaceUnder");
    var cell1 = row.insertCell(0);  
    var oImg = document.createElement("img");
    oImg.setAttribute("src", "/html/images/icons/cross.png");
    oImg.setAttribute("alt", "delete");
    oImg.setAttribute("title", "delete row");
    oImg.style.cursor="pointer";
    oImg.onclick = function(){deleteRow(tableID,prefixSelect+(rowCount),prefixDiv+(rowCount));};
    cell1.appendChild(oImg);

    var cell2 = row.insertCell(1);
    var select = document.createElement("select");    
    select.setAttribute("name", prefixSelect+(rowCount));
    select.setAttribute("id", prefixSelect+(rowCount));
    select.setAttribute("dojoType", "dijit.form.FilteringSelect");
//    select.style.width="200px";    
    createOption(select, "1", "1 Column (100)",true);
    createOption(select, "yui-gc-template", "2 Column (66/33)",false);
    createOption(select, "yui-gd-template", "2 Column (33/66)",false);
    createOption(select, "yui-ge-template", "2 Column (75/25)",false);
    createOption(select, "yui-gf-template", "2 Column (25/75)",false);            
    createOption(select, "yui-gb-template", "3 Column (33/33/33)",false);
    select.onchange=function(){addGrid(select.value, select.getAttribute("name"));};
    cell2.appendChild(select);
    dojo.parser.parse(cell2);
    dojo.connect(dijit.byId(prefixSelect+(rowCount)), "onChange", this, function(e){addGrid(e, prefixDiv+(rowCount), rowCount);}); 
    addGrid(1,prefixDiv+(rowCount),rowCount);
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

function deleteRow(tableID, row, div) {		
	//cancello la riga delle combo
   	var table = document.getElementById(tableID);
   	var rowCount = table.rows.length;
   	for(var i=1; i<rowCount; i++){
   		var riga = table.rows[i];
   		if(null!=riga){
   	        var select = riga.cells[1].childNodes[0];                
   	        if(select.id=="widget_"+row){
   	        	//distruggo l'elemento dojo
   	        	var selectDijit = dijit.byId(row);
   	        	if (selectDijit) {
   	        		selectDijit.destroy();
   	        	}
   	        	dojo.destroy(row);
   	        	table.deleteRow(i);
   	        }
   		}
   	}   	
   	//cancello il div relativo
   	removeGrid(div);   	
   	
}

function addPageWidth(pageWidth){	
//	var mainFrame = top.mainFrame;	
	var mainDiv = document.getElementsByName("globalContainer")[0];
	mainDiv.removeAttribute("id");
	mainDiv.setAttribute("id",pageWidth);	
}

function addHeader(checked){
	var pageWidth = dijit.byId("pageWidth").attr("value");
//	var mainFrame = frames["mainFrame"];
	if(checked){ //aggiungo il div dell'header
		var mainDiv = document.getElementById(pageWidth);		
		var headerDiv = document.createElement("div");
		headerDiv.setAttribute("id","hd-template");
		headerDiv.innerHTML=getAddContainer("hd-template")+"<h1>Header</h1>";
		//inserisco alla prima posizione
		mainDiv.insertBefore(headerDiv,mainDiv.firstChild);
	} else { //elimino il div dell'header
		var div = document.getElementById("hd-template");
		div.parentNode.removeChild(div);
	} 
}

function addFooter(checked){
	var pageWidth = dijit.byId("pageWidth").attr("value");
//	var mainFrame = frames["mainFrame"];
	if(checked){ //aggiungo il div del footer
		var mainDiv = document.getElementById(pageWidth);		
		var footerDiv = document.createElement("div");
		footerDiv.setAttribute("id","ft-template");
		footerDiv.innerHTML=getAddContainer("ft-template")+"<h1>Footer</h1>";		
		mainDiv.appendChild(footerDiv);
	} else { //elimino il div del footer
		var div = document.getElementById("ft-template");
		div.parentNode.removeChild(div);
	} 
}

function addLayout(layout){
	var idMainDiv = dijit.byId("pageWidth").attr("value");
//	var mainFrame = top.mainFrame;
	var mainDiv = document.getElementById(idMainDiv);
	var yuiB2 = document.getElementById("yui-b2");
	var bodyDiv = document.getElementById("bd-template");	
	if("none"!=layout){
		if(null!=mainDiv.getAttribute("class"))
			mainDiv.removeAttribute("class");
		mainDiv.setAttribute("class",layout);		
		if(null==yuiB2){
			//aggiungo il div yui-b per la sidebar
			var yuiBDiv = document.createElement("div");
			yuiBDiv.setAttribute("class","yui-b-template");
			yuiBDiv.setAttribute("id","yui-b2");
			yuiBDiv.style.height="70%";
			yuiBDiv.innerHTML=getAddContainer("yui-b2")+"<h1>Sidebar</h1>"+getMockContent();
			bodyDiv.appendChild(yuiBDiv);		
		}		
	}else{
		mainDiv.removeAttribute("class");
		if(null!=yuiB2)
			bodyDiv.removeChild(yuiB2);
	}
}

function printBody(){
//	var mainFrame = top.mainFrame;
	alert(document.getElementById("bodyTemplate").innerHTML);	
}

function addGrid(gridId, yuiBId, rowCount){
	var mainDiv = document.getElementById("yui-main-template");
	var yuiBDiv = document.getElementById(yuiBId);
	if(null==yuiBDiv){ //lo creo da zero
		yuiBDiv = document.createElement("div");
		yuiBDiv.setAttribute("class","yui-b-template");
		yuiBDiv.setAttribute("id",yuiBId);
		mainDiv.appendChild(yuiBDiv);
	}else{
		//rimuovo il suo contenuto
		while (yuiBDiv.hasChildNodes()) {
			yuiBDiv.removeChild(yuiBDiv.lastChild);
		}
	}
	if(1!=gridId){
		var gridDiv = document.createElement("div");
		var yuiUFirst = document.createElement("div");
		var yuiU2 = document.createElement("div");
		var yuiU3 = document.createElement("div");
			
		gridDiv.setAttribute("class",gridId);
		gridDiv.setAttribute("id",gridId);
		yuiUFirst.setAttribute("class","yui-u-template first");
		yuiUFirst.setAttribute("id",rowCount+"_yui-u-grid-1");
		yuiUFirst.innerHTML=getAddContainer(rowCount+"_yui-u-grid-1")+"<h1>Body</h1>";
		yuiUFirst.style.height="50%";
		yuiU2.setAttribute("class","yui-u-template");
		yuiU2.setAttribute("id",rowCount+"_yui-u-grid-2");
		yuiU2.innerHTML=getAddContainer(rowCount+"_yui-u-grid-2")+"<h1>Body</h1>";
		yuiU2.style.height="50%";
		gridDiv.appendChild(yuiUFirst);
		gridDiv.appendChild(yuiU2);
		if("yui-gb-template"==gridId){
			yuiU3.setAttribute("class","yui-u-template");
			yuiU3.setAttribute("id",rowCount+"_yui-u-grid-3");
			yuiU3.innerHTML=getAddContainer(rowCount+"_yui-u-grid-3")+"<h1>Body</h1>";
			yuiU3.style.height="50%";
			gridDiv.appendChild(yuiU3);
		}		
		yuiBDiv.appendChild(gridDiv);		
	}else{
		yuiBDiv.innerHTML=getAddContainer(yuiBId)+"<h1>Body</h1>";
	}
}

function removeGrid(yuiBId){
//	var mainFrame = top.mainFrame;
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
function addDrawedContainer(idDiv, container, value){
	
	var div_container = document.getElementById(idDiv.value+"_div_"+value);
	var span_container = document.getElementById(idDiv.value+"_span_"+value);
	
	if(null!=div_container && null!=span_container){
		alert("The container that you want to add already exist into this section.");
		return;
	}
	
	//create the container span
	var titleContainerSpan = document.createElement("span");
	titleContainerSpan.setAttribute("class", "titleContainerSpan");
	titleContainerSpan.setAttribute("id", idDiv.value+"_span_"+value);
	titleContainerSpan.setAttribute("title","container_"+value);
	titleContainerSpan.innerHTML=getRemoveContainer(idDiv.value,value)+"<div class=\"clear\"></div>"+getContainerMockContent(container.title);
	
	var containerDivHidden = document.createElement("div");
	containerDivHidden.style.display="none";
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
			div.innerHTML+="<h1>Sidebar</h1>"+getMockContent();
	}
	
}

function getMockContent(){
	return '<span class="mockContent">Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur?</span>';
}

function getAddContainer(idDiv){
	return '<div class="addContainerSpan"><a href="javascript: showAddContainerDialog(\''+idDiv+'\');" title="Add Container"><span class="plusBlueIcon"></span>Add Container</a></div>';	
}

function getRemoveContainer(idDiv, idContainer){
	return '<div class="removeContainerSpan"><a href="javascript: removeDrawedContainer(\''+idDiv+'\',\''+idContainer+'\');" title="Remove Container"><span class="minusIcon"></span>Remove Container</a></div>';	
}