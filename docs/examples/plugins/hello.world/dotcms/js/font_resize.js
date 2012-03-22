//------------------------------ FONT SIZER FUNCTIONS ---------------------------------//

function fontBigger(){
	var size = getCookie("fontsize");
	size =  (size != null && size != "") ? size = parseInt(size) + 1 : size = 2;
	size = (size > 3) ? 3 : size;
	setCookie("fontsize", size);
	setActiveStyleSheet(size);
}

function fontSmaller(){
	var size = getCookie("fontsize");
	size = (size != null && size != "") ? size = parseInt(size) -1 : size = 2;
	size = (size <1) ? 1 : size;
	setCookie("fontsize", size);
	setActiveStyleSheet(size);
}

function toggleFontSize(){
	var size = getCookie("fontsize");
	size = (size != null && size != "") ? size = parseInt(size) + 1 : size = 2;
	size = (size >3 ) ? 1 : size;
	setCookie("fontsize", size);
	setActiveStyleSheet(size);
}

function getCookie(name) { // use: getCookie("name");
	var yummy = document.cookie;
	var index = yummy.indexOf(name + "=");
	if (index == -1) return null;
	index = yummy.indexOf("=", index) + 1;
	var endstr = yummy.indexOf(";", index);
	if (endstr == -1) endstr = yummy.length;
	return unescape(yummy.substring(index, endstr));
}



function setCookie(name, value) { // use: setCookie("name", value);
	var yummy = document.cookie;
  	var today = new Date();
  	var expiry = new Date(today.getTime() + 365 * 24 * 60 * 60 * 1000); // plus a year 

	if (value != null && value != ""){
		document.cookie=name + "=" + escape(value) + "; path=/; expires=" + expiry.toGMTString();
		yummy = document.cookie; // update yummy
	}
}


 
function setActiveStyleSheet(title) {
	var i, a, main;
	for(i=0; (a = document.getElementsByTagName("link")[i]); i++) {
		if(a.getAttribute("rel").indexOf("style") != -1  && a.getAttribute("title")) {
			a.disabled = true;
			if(a.getAttribute("title") == "size" + title) {
				a.disabled = false;
			}
		}
	}
}