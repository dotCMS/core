function roller(type, tag, parentId) {
	if (window.attachEvent) {
		window.attachEvent("onload", function() {
			var sfEls = (parentId==null)?document.getElementsByTagName(tag):document.getElementById(parentId).getElementsByTagName(tag);
			type(sfEls);
		});
	}
}

sfHover = function(sfEls) {
	for (var i=0; i<sfEls.length; i++) {
		sfEls[i].onmouseover=function() {
			this.className+=" sfhover";
		}
		sfEls[i].onmouseout=function() {
			this.className=this.className.replace(new RegExp(" sfhover\\b"), "");
		}
	}
}

sfFocus = function(sfEls) {
	for (var i=0; i<sfEls.length; i++) {
		sfEls[i].onfocus=function() {
			this.className+=" sffocus";
		}
		sfEls[i].onblur=function() {
			this.className=this.className.replace(new RegExp(" sffocus\\b"), "");
		}
	}
}

sfActive = function(sfEls) {
	for (var i=0; i<sfEls.length; i++) {
		sfEls[i].onmousedown=function() {
			this.className+=" sfactive";
		}
		sfEls[i].onmouseup=function() {
			this.className=this.className.replace(new RegExp(" sfactive\\b"), "");
		}
	}
}

sfTarget = function(sfEls) {
	var aEls = document.getElementsByTagName("A");
	document.lastTarget = null;
	for (var i=0; i<sfEls.length; i++) {
		if (sfEls[i].id) {
			if (location.hash==("#" + sfEls[i].id)) {
				sfEls[i].className+=" sftarget";
				document.lastTarget=sfEls[i];
			}
			for (var j=0; j<aEls.length; j++) {
				if (aEls[j].hash==("#" + sfEls[i].id)) aEls[j].targetEl = sfEls[i];
				aEls[j].onclick = function() {
					if (document.lastTarget) document.lastTarget.className = document.lastTarget.className.replace(new RegExp(" sftarget\\b"), "");
					if (this.targetEl) this.targetEl.className+=" sftarget";
					document.lastTarget=this.targetEl;
					return true;
				}
			}
		}
	}
}