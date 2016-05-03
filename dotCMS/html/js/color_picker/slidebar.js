// WebFX Slidebar

var slidebarDragObj = null;
var slidebarPosX;
var slidebarPosY;
var slidebarType;
var onChange = "";

function slidebar_onMouseDown() {
	var temp = getReal(window.event.srcElement, "className", "slider-handle");

	if (temp.className == "slider-handle") {
		slidebarDragObj = temp;

		onChange = slidebarDragObj.getAttribute("onChange");
		if (onChange == null) {
			onChange = "";
		}

		slidebarType = slidebarDragObj.getAttribute("type");

		if (slidebarType == "y") {
			slidebarPosY = (window.event.clientY - slidebarDragObj.style.pixelTop);
		}
		else {
			slidebarPosX = (window.event.clientX - slidebarDragObj.style.pixelLeft);
		}

		window.event.cancelBubble = true;
		window.event.returnValue = false;
	}
	else {
		slidebarDragObj = null;
	}
}

function slidebar_onMouseUp() {
	if (slidebarDragObj) {
		slidebarDragObj = null;
	}
}

function slidebar_onMouseMove() {
	if (slidebarDragObj) {
		if (slidebarType == "y") {
			if(event.clientY >= 0) {
				if ((event.clientY - slidebarPosY >= 0) && (event.clientY - slidebarPosY <= slidebarDragObj.parentElement.offsetHeight - slidebarDragObj.offsetHeight)) {
					slidebarDragObj.style.top = event.clientY - slidebarPosY;
				}

				if (event.clientY - slidebarPosY < 0) {
					slidebarDragObj.style.top = "0";
				}

				if (event.clientY - slidebarPosY > slidebarDragObj.parentElement.offsetHeight - slidebarDragObj.offsetHeight - 0) {
					slidebarDragObj.style.top = slidebarDragObj.parentElement.offsetHeight - slidebarDragObj.offsetHeight;
				}

				slidebarDragObj.value = slidebarDragObj.style.pixelTop / (slidebarDragObj.parentElement.offsetHeight - slidebarDragObj.offsetHeight);
				eval(onChange.replace(/this/g, "slidebarDragObj"));
			}
		}
		else {
			if (event.clientX >= 0) {
				if ((event.clientX  - slidebarPosX >= 0) && (event.clientX - slidebarPosX <= slidebarDragObj.parentElement.offsetWidth - slidebarDragObj.offsetWidth)) {
					slidebarDragObj.style.left = event.clientX - slidebarPosX;
				}

				if (event.clientX - slidebarPosX < 0) {
					slidebarDragObj.style.left = "0";
				}

				if (event.clientX - slidebarPosX > slidebarDragObj.parentElement.clientWidth - slidebarDragObj.offsetWidth - 0) {
					slidebarDragObj.style.left = slidebarDragObj.parentElement.clientWidth - slidebarDragObj.offsetWidth;
				}

				slidebarDragObj.value = slidebarDragObj.style.pixelLeft / (slidebarDragObj.parentElement.clientWidth - slidebarDragObj.offsetWidth);
				eval(onChange.replace(/this/g, "slidebarDragObj"));
			}
		}

		window.event.cancelBubble = true;
		window.event.returnValue = false;
	}
}

function getReal(el, className, value) {
	var temp = el;

	while ((temp != null) && (temp.tagName != "body")) {
		if (eval("temp." + className) == value) {
			el = temp;

			return el;
		}

		temp = temp.parentElement;
	}

	return el;
}

function setValue(el, value) {
	el.value = value;

	if (el.getAttribute("type") == "x") {
		el.style.left =  value * (el.parentElement.clientWidth - el.offsetWidth);
	}
	else {
		el.style.top =  value * (el.parentElement.clientHeight - el.offsetHeight);
	}

	eval(el.onChange.replace(/this/g, "el"))
}

document.onmousedown = slidebar_onMouseDown;
document.onmouseup = slidebar_onMouseUp;
document.onmousemove = slidebar_onMouseMove;

document.write("<style type=\"text/css\">");
document.write("	.slider-handle {");
document.write("		cursor: default;");
document.write("		position: relative;");
document.write("	}");
document.write("</style>");