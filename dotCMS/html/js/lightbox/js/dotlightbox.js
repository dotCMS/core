/*
Created By: Chris Campbell
Website: http://particletree.com
Date: 2/1/2006

Adapted By: Simon de Haan
Website: http://blog.eight.nl
Date: 21/2/2006

Inspired by the lightbox implementation found at http://www.huddletogether.com/projects/lightbox/
And the lightbox gone wild by ParticleTree at http://particletree.com/features/lightbox-gone-wild/

*/

/*-------------------------------GLOBAL VARIABLES------------------------------------*/

var dotLBDetect = navigator.userAgent.toLowerCase();
var dotLBOS,dotLBBrowser,dotLBVersion,dotLBTotal,dotLBThestring;

/*-----------------------------------------------------------------------------------------------*/

//Browser detect script origionally created by Peter Paul Koch at http://www.quirksmode.org/

function dotLBGetBrowserInfo() {
	if (dotLBCheckIt('konqueror')) {
		dotLBBrowser = "Konqueror";
		dotLBOS = "Linux";
	}
	else if (dotLBCheckIt('safari')) dotLBBrowser 	= "Safari"
	else if (dotLBCheckIt('omniweb')) dotLBBrowser 	= "OmniWeb"
	else if (dotLBCheckIt('opera')) dotLBBrowser 		= "Opera"
	else if (dotLBCheckIt('webtv')) dotLBBrowser 		= "WebTV";
	else if (dotLBCheckIt('icab')) dotLBBrowser 		= "iCab"
	else if (dotLBCheckIt('msie')) dotLBBrowser 		= "Internet Explorer"
	else if (!dotLBCheckIt('compatible')) {
		dotLBBrowser = "Netscape Navigator"
		dotLBVersion = dotLBDetect.charAt(8);
	}
	else dotLBBrowser = "An unknown browser";

	if (!dotLBVersion) dotLBVersion = dotLBDetect.charAt(place + dotLBThestring.length);

	if (!dotLBOS) {
		if (dotLBCheckIt('linux')) dotLBOS 		= "Linux";
		else if (dotLBCheckIt('x11')) dotLBOS 	= "Unix";
		else if (dotLBCheckIt('mac')) dotLBOS 	= "Mac"
		else if (dotLBCheckIt('win')) dotLBOS 	= "Windows"
		else dotLBOS 								= "an unknown operating system";
	}
}

function dotLBCheckIt(string) {
	place = dotLBDetect.indexOf(string) + 1;
	dotLBThestring = string;
	return place;
}

/*-----------------------------------------------------------------------------------------------*/

parent.Event.observe(window, 'load', dotLBInitialize, false);
parent.Event.observe(window, 'load', dotLBGetBrowserInfo, false);
parent.Event.observe(window, 'unload', parent.Event.unloadCache, false);

var dotlightbox = Class.create();

dotlightbox.prototype = {

	yPos : 0,
	xPos : 0,

	initialize: function(ctrl) {
		this.content = ctrl.rel;
		parent.Event.observe(ctrl, 'click', this.activate.bindAsEventListener(this), false);
		ctrl.onclick = function(){return false;};
	},
	
	// Turn everything on - mainly the IE fixes
	activate: function(){
		if (dotLBBrowser == 'Internet Explorer'){
			this.getScroll();
			this.prepareIE('100%', 'hidden');
			this.setScroll(0,0);
			this.hideSelects('hidden');
		}
		this.displayLightbox("block");
	},
	
	// Ie requires height to 100% and overflow hidden or else you can scroll down past the lightbox
	prepareIE: function(height, overflow){
		bod = document.getElementsByTagName('body')[0];
		bod.style.height = height;
		bod.style.overflow = overflow;
  
		htm = document.getElementsByTagName('html')[0];
		htm.style.height = height;
		htm.style.overflow = overflow; 
	},
	
	// In IE, select elements hover on top of the lightbox
	hideSelects: function(visibility){
		selects = document.getElementsByTagName('select');
		for(i = 0; i < selects.length; i++) {
			selects[i].style.visibility = visibility;
		}
	},
	
	// Taken from lightbox implementation found at http://www.huddletogether.com/projects/lightbox/
	getScroll: function(){
		if (self.pageYOffset) {
			this.yPos = self.pageYOffset;
		} else if (document.documentElement && document.documentElement.scrollTop){
			this.yPos = document.documentElement.scrollTop; 
		} else if (document.body) {
			this.yPos = document.body.scrollTop;
		}
	},
	
	setScroll: function(x, y){
		window.scrollTo(x, y); 
	},
	
	displayLightbox: function(display){
		$('dotoverlay').style.display = display;
		$(this.content).style.display = display;
		if(display != 'none') this.actions();		
	},
	
	// Search through new links within the lightbox, and attach click event
	actions: function(){
		lbActions = document.getElementsByClassName('lbAction');

		for(i = 0; i < lbActions.length; i++) {
			parent.Event.observe(lbActions[i], 'click', this[lbActions[i].rel].bindAsEventListener(this), false);
			lbActions[i].onclick = function(){return false;};
		}

	},
	
	// Example of creating your own functionality once lightbox is initiated
	deactivate: function(){
		if (dotLBBrowser == "Internet Explorer"){
			this.setScroll(0,this.yPos);
			this.prepareIE("auto", "auto");
			this.hideSelects("visible");
		}
		
		this.displayLightbox("none");
	}
}

/*-----------------------------------------------------------------------------------------------*/

// Onload, make all links that need to trigger a lightbox active
function dotLBInitialize(){
	addLightboxMarkup();
//	lbox = document.getElementsByClassName('lbOn');
//	for(i = 0; i < lbox.length; i++) {
//		valid = new lightbox(lbox[i]);
//	}
}

// Add in markup necessary to make this work. Basically two divs:
// Overlay holds the shadow
// Lightbox is the centered square that the content is put into.
function dotLBAddLightboxMarkup() {

	bod 				= document.getElementsByTagName('body')[0];

	overlay 			= document.createElement('div');
	overlay.id			= 'dotoverlay';

	bod.appendChild(overlay);
}