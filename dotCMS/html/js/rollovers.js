var imgRollOvers = new Array();
var imgRollOversCount = 0;

function findImage(name) {
	for (i = 0; i < imgRollOversCount; i++) {
		if (name == imgRollOvers[i][2]) {
			return imgRollOvers[i];
		}
	}
}

function loadImage(name, on, off) {
	imgRollOvers[imgRollOversCount] = new Array(3);
	imgRollOvers[imgRollOversCount][0] = new Image();
	imgRollOvers[imgRollOversCount][0].src = on;
	imgRollOvers[imgRollOversCount][1] = new Image();
	imgRollOvers[imgRollOversCount][1].src = off;
	imgRollOvers[imgRollOversCount][2] = name;
	imgRollOversCount++;
}

function offRollOver() {
	for (i = 0; i < imgRollOversCount; i++) {
		if (document.images[imgRollOvers[i][2]] != null) {
			document.images[imgRollOvers[i][2]].src = imgRollOvers[i][1].src;
		}
	}
}

function onRollOver(names) {
	if (names[0]) {
		//var x = findImage(names[0]);
		//var y = findImage(names[1]);
		var x = findImage(names);
		var y = findImage(names);

		document.images[x[2]].src = y[0].src;
	}
	else {
		document.images[names].src = findImage(names)[0].src;
	}
}