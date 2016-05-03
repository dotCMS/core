
var fadeInLenth = 500;
var fadeOutLenth = 500;
var displayLength = 2000;
var fgcolor = "#666666";
var bgcolor = "#ffffff";
var fgcolorDescription = "#000000";
var bgcolorDescription = "#ffffff";

var steps = 10;

var titles = new Array();
var descriptions = new Array();
var links = new Array();

var fTimer = null;
var timer = null;
var itemCount = 0;
var running = false;
var fadeLevel = 0;
var stage = 0;

function initialiseTicker() {
	itemCount = 0;
	running = false;
}

function incrementCounter() {
	itemCount ++;
	if (itemCount > titles.length) {
		itemCount = 1;
	}
}

function decrementCounter() {
	itemCount --;
	if (itemCount < 1) {
		itemCount = titles.length - 1;
	}
}

function runTicker() {
	stage = 2;
	//timer = self.setTimeout("work()", displayLength);
	work();
}

function work() {
	//alert("running = " + running + ", stage = " + stage);
	if (running == true) {
		//stage = 1 Fading out
		//stage = 2 Swapping
		//stage = 3; Fading in
		//stage = 4; Waiting
	if (stage == 1) { // fade out
		if (running == true && fadeLevel <= steps) {
			setTickerColor(htmlColor(calculateColor(fgcolor, bgcolor, fadeLevel)))
			setTickerDescriptionColor(htmlColor(calculateColor(fgcolorDescription, bgcolorDescription, fadeLevel)))
			fadeLevel ++;
			fTimer = self.setTimeout("work()", Math.round(fadeOutLenth/steps));
		}
		if (fadeLevel > steps) {
			stage = 2;
		}
	} else if (stage == 2) { // swap
		incrementCounter();
		setTitle(titles[itemCount - 1]);
		setLink(links[itemCount -1]);
		
		if (itemCount <= descriptions.length) {
			if (descriptions[itemCount -1] == undefined) {
				setDescription("");
			} else {
				setDescription(descriptions[itemCount -1]);
			}
		}
		
		if (running == true) {
			// Set the timer
			timer = self.setTimeout("work()", 1);
		}
		stage = 3;
	} else if (stage == 3) { // Fade in
		if (running == true && fadeLevel >= 0) {
			fadeLevel --;
			setTickerColor(htmlColor(calculateColor(fgcolor, bgcolor, fadeLevel)))
			setTickerDescriptionColor(htmlColor(calculateColor(fgcolorDescription, bgcolorDescription, fadeLevel)))
			fTimer = self.setTimeout("work()", Math.round(fadeInLenth/steps));
		}
		if (fadeLevel < 0) {
			stage = 4;
		}
	} else if (stage == 4) { // Waiting
		if (running == true) {
			// Set the timer
			timer = self.setTimeout("work()", displayLength);
		}
		stage = 1; // next stage
	}
	}
}

function startTicker() {
	initialiseTicker();
	running = true;
	runTicker();
}

function stopTicker() {
	initialiseTicker();
	if (timer) {
		self.clearTimeout(timer);
	}
}

function pauseTicker() {
	running = false;
	if (timer) {
		self.clearTimeout(timer);
	}
	if (fTimer) {
		self.clearTimeout(fTimer);
	}
	// Restore color
	fadeLevel = 0;
	setTickerColor(htmlColor(calculateColor(fgcolor, bgcolor, fadeLevel)))
	setTickerDescriptionColor(htmlColor(calculateColor(fgcolorDescription, bgcolorDescription, fadeLevel)))

}

function unpauseTicker() {
	running = true;
	stage = 4;
	work();
}

function clearTimer() {
}

function fadeIn() {
	if (running == true && fadeLevel >= 0) {
		setTickerColor(htmlColor(calculateColor(fgcolor, bgcolor, fadeLevel)))
		fadeLevel --;
		//fTimer = self.setTimeout("fadeIn()", Math.round(fadeInLenth/steps));
	}
}

function fadeOut() {
	if (running == true && fadeLevel <= steps) {
		setTickerColor(htmlColor(calculateColor(fgcolor, bgcolor, fadeLevel)))
		fadeLevel ++;
		//fTimer = self.setTimeout("fadeOut()", Math.round(fadeOutLenth/steps));
	}
}

function setTickerColor(colour) {
	//alert("Setting color to " + colour);
	tickerContent = document.getElementById('ticker-link');
	if (tickerContent) {
		tickerContent.style.color = colour;
	}
}

function setTickerDescriptionColor(colour) {
	//alert("Setting color to " + colour);
	tickerContent = document.getElementById('ticker-description');
	if (tickerContent) {
		tickerContent.style.color = colour;
	}
}

function clearTitle() {
	document.getElementById('ticker-content').innerHTML = "";
	return true;
}

function calculateColor(start, end, step) {
	var startColors = makeColorArray(start);
	var endColors = makeColorArray(end);
	
	var colors = null;
	
	if (startColors && endColors) {
		var diff = new Array(endColors[0]-startColors[0], endColors[1]-startColors[1], endColors[2]-startColors[2]);
		colors = new Array(Math.round(startColors[0] + ((diff[0]*step)/steps)), Math.round(startColors[1] + ((diff[1]*step)/steps)), Math.round(startColors[2] + ((diff[2]*step)/steps)));
	}
	return colors;
}

function htmlColor(colors) {
	var htmlColor = "rgb(" + colors[0] + ", " + colors[1] + ", " + colors[2] + ")";
	return htmlColor;
}

function makeColorArray(colorStr) {

	var cArray = null;
	
	if (colorStr.length == 7) {
		cArray = new Array(parseInt("0x"+colorStr.substr(1, 2)), parseInt("0x"+colorStr.substr(3, 2)), parseInt("0x"+colorStr.substr(5, 2)));
	} else if (colorStr.length == 18) {
		cArray = new Array(parseInt(colorStr.substr(4, 3)), parseInt(colorStr.substr(9, 3)), parseInt(colorStr.substr(14, 3)));
	} else if (colorStr.length == 15) {
		cArray = new Array(parseInt(colorStr.substr(4, 2)), parseInt(colorStr.substr(8, 2)), parseInt(colorStr.substr(12, 2)));
	} else if (colorStr.length == 12) {
		cArray = new Array(parseInt(colorStr.substr(4, 1)), parseInt(colorStr.substr(7, 1)), parseInt(colorStr.substr(10, 1)));
	}
	
	return cArray;
}

function setTitle(title) {
	document.getElementById('ticker-link').innerHTML = title;
	return true;
}

function setLink(link) {
	document.getElementById('ticker-link').href = link;
	return true;
}

function setDescription(description) {
	document.getElementById('ticker-description').innerHTML = description;
	return true;
}