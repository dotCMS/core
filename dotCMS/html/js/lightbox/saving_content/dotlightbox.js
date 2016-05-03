/*
Created By: Chris Campbell
Website: http://particletree.com
Date: 2/1/2006

Adapted By: Simon de Haan
Website: http://blog.eight.nl
Date: 21/2/2006

further adapted by Jason Tesser and David Torres
www.dotcms.org
Date: 1/6/2008

Inspired by the lightbox implementation found at http://www.huddletogether.com/projects/lightbox/
And the lightbox gone wild by ParticleTree at http://particletree.com/features/lightbox-gone-wild/

*/

/*-------------------------------GLOBAL VARIABLES------------------------------------*/

var dotLBDetect = navigator.userAgent.toLowerCase();
var dotLBOS,dotLBBrowser,dotLBVersion,dotLBTotal,dotLBThestring;

/*-----------------------------------------------------------------------------------------------*/

// Turn everything on - mainly the IE fixes
	function dotActivate (divId){
		var myDiv = document.getElementById(divId);
		if (dotLBBrowser == 'Internet Explorer'){
			dotGetScroll(myDiv);
			dotPrepareIE('100%', 'hidden');
			dotSetScroll(0,0);
			dotHideSelects('hidden');
		}
		dotDisplayLightbox("block", myDiv);
	}
	
	// Ie requires height to 100% and overflow hidden or else you can scroll down past the lightbox
	function dotPrepareIE(height, overflow){
		bod = document.getElementsByTagName('body')[0];
		bod.style.height = height;
		bod.style.overflow = overflow;
  
		htm = document.getElementsByTagName('html')[0];
		htm.style.height = height;
		htm.style.overflow = overflow; 
	}
	
	// In IE, select elements hover on top of the lightbox
	function dotHideSelects(visibility){
		selects = document.getElementsByTagName('select');
		for(i = 0; i < selects.length; i++) {
			selects[i].style.visibility = visibility;
		}
	}
	
	// Taken from lightbox implementation found at http://www.huddletogether.com/projects/lightbox/
	function dotGetScroll(myDiv){
		if (self.pageYOffset) {
			myDiv.yPos = self.pageYOffset;
		} else if (document.documentElement && document.documentElement.scrollTop){
			myDiv.yPos = document.documentElement.scrollTop; 
		} else if (document.body) {
			myDiv.yPos = document.body.scrollTop;
		}
	}
	
	function dotSetScroll(x, y){
		window.scrollTo(x, y); 
	}
	
	function dotDisplayLightbox(display,div){
		document.getElementById('dotoverlay').style.display = display;
		div.style.display = display;	
	}
	
	// Example of creating your own functionality once lightbox is initiated
	function dotDeactivate(divId){
		var myDiv = document.getElementById(divId);
		if (dotLBBrowser == "Internet Explorer"){
			dotSetScroll(0,this.yPos);
			dotPrepareIE("auto", "auto");
			dotHideSelects("visible");
		}
		
		dotDisplayLightbox("none", myDiv);
	}

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

// Onload, make all links that need to trigger a lightbox active
function dotLBInitialize(){
	dotLBAddLightboxMarkup();
//	lbox = document.getElementsByClassName('dotlbOn');
//	for(i = 0; i < lbox.length; i++) {
//		valid = new top.frameMenu.dotlightbox(lbox[i]);
//	}
}

// Add in markup necessary to make this work. Basically two divs:
// Overlay holds the shadow
// Lightbox is the centered square that the content is put into.
function dotLBAddLightboxMarkup() {
	//dotLBGetBrowserInfo();
	//var bod 				= document.getElementsByTagName('body')[0];
	//var bod 				= document.body || document.documentElement;
	
	//var overlay 			= document.createElement('div');
	//overlay.id			= 'dotoverlay';
	//if (dotLBBrowser == "Internet Explorer"){
		//top.frameMenu.protoInsertionBottomWrapper(bod,overlay);
	//}else{
		//bod.appendChild(overlay);
	//}
}
dotLBInitialize();