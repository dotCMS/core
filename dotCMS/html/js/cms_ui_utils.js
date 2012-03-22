
function makeTaller(x){

	var ele = document.getElementById(x);
	var h = parseInt(ele.style.minHeight.replace("px", ""));
	ele.style.minHeight = (h + 100) + "px";
}

function makeShorter(x){
	var ele = document.getElementById(x);
	var h = parseInt(ele.style.minHeight.replace("px", ""));
	if(h > 150){
		ele.style.minHeight = (h - 100) + "px";
	}
	if(h < 150){
		ele.style.minHeight = "150px";
	}

}

function makeNarrower(x){
	var ele = document.getElementById(x);
	var w = parseInt(ele.style.width.replace("px", ""));
	if(w > 100){
		ele.style.width = (w - 100) + "px";
	}
	if(	ele.style.width  < 100){
		ele.style.width = "100px";
	}

}

function makeWider( x){
	var ele = document.getElementById(x);
	var w = parseInt(ele.style.width.replace("px", ""));
	ele.style.width = (w + 100) + "px";
}


function setWidth(x,w) {
	var ele = document.getElementById(x);
	ele.style.width = w;
}

function setHeight(x,h) {
	var ele = document.getElementById(x);
	ele.style.height = h;
}

//TAB Catch for the textareas

function setSelectionRange(input, selectionStart, selectionEnd) {
  if (input.setSelectionRange) {
    input.focus();
    input.setSelectionRange(selectionStart, selectionEnd);
  }
  else if (input.createTextRange) {
    var range = input.createTextRange();
    range.collapse(true);
    range.moveEnd('character', selectionEnd);
    range.moveStart('character', selectionStart);
    range.select();
  }
}

function replaceSelection (input, replaceString) {
	if (input.setSelectionRange) {
		var selectionStart = input.selectionStart;
		var selectionEnd = input.selectionEnd;
		input.value = input.value.substring(0, selectionStart)+ replaceString + input.value.substring(selectionEnd);
    
		if (selectionStart != selectionEnd){ 
			setSelectionRange(input, selectionStart, selectionStart + 	replaceString.length);
		}else{
			setSelectionRange(input, selectionStart + replaceString.length, selectionStart + replaceString.length);
		}

	}else if (document.selection) {
		var range = document.selection.createRange();

		if (range.parentElement() == input) {
			var isCollapsed = range.text == '';
			range.text = replaceString;

			 if (!isCollapsed)  {
				range.moveStart('character', -replaceString.length);
				range.select();
			}
		}
	}
}


// We are going to catch the TAB key so that we can use it, Hooray!
function catchTab(item,e){
	if(navigator.userAgent.match("Gecko")){
		c=e.which;
	}else{
		c=e.keyCode;
	}
	if(c==9){
		replaceSelection(item,String.fromCharCode(9));
		setTimeout("document.getElementById('"+item.id+"').focus();",0);	
		return false;
	}
		    
}

function showElement(domNode /* either domObject or id */) {
	dojo.style(domNode, { display: '' });
}

function hideElement(domNode /* either domObject or id */) {
	dojo.style(domNode, { display: 'none' });
}

function removeElement(domNode /* either domObject or id */) {
	if(dojo.isString(domNode))
		domNode = dojo.byId(domNode);
	if(dojo.isIE) {
		domNode.parentElement.removeChild(domNode);
	} else {
		domNode.remove();
	}
}

