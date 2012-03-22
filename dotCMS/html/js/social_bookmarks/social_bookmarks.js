var pageURL = window.location.href.replace( /(.*)#.*/, "$1");
var pageTitle = "";

function dotCMS_SB_bookmark(site, pageTitle, pageURL){

	if(site == "digg"){
		window.open('http://digg.com/submit?phase=2&url='+encodeURIComponent(pageURL)+'&bodytext=&tags=&title='+encodeURIComponent(pageTitle));
	}
	if(site == "delicious"){
		window.open('http://del.icio.us/post?v=2&url='+encodeURIComponent(pageURL)+'&notes=&tags=&title='+encodeURIComponent(pageTitle));return false;
	}
	if(site == "yahoo"){	
		window.open('http://myweb2.search.yahoo.com/myresults/bookmarklet?t='+encodeURIComponent(pageTitle)+'&d=&tag=&u='+encodeURIComponent(pageURL));return false;
	}
	if(site == "google"){	
		window.open('http://www.google.com/bookmarks/mark?op=add&hl=en&bkmk='+encodeURIComponent(pageURL)+'&annotation=&labels=&title='+encodeURIComponent(pageTitle));
	}
	if(site == "newsvine"){
		window.open('http://www.newsvine.com/_wine/save?popoff=1&u='+encodeURIComponent(pageURL)+'&tags=&blurb='+encodeURIComponent(pageTitle));
	}
	if(site == "reddit"){
        window.open('http://reddit.com/submit?url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "stumble"){
	    window.open('http://www.stumbleupon.com/submit?url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "technorati"){
	    window.open('http://technorati.com/faves?add='+encodeURIComponent(pageURL)+'&amp;tag=');
	}
	if(site == "wists"){
	    window.open('http://wists.com/r.php?c=&r='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "spurl"){
	    window.open('http://www.spurl.net/spurl.php?v=3&title='+encodeURIComponent(pageTitle)+'&amp;url='+encodeURIComponent(pageURL));
	}
	if(site == "connotea"){
	    window.open('http://www.connotea.org/addpopup?continue=confirm&uri='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "comments"){
	    window.open('http://co.mments.com/track?url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "blogmarks"){
	    window.open('http://blogmarks.net/my/new.php?mini=1&simple=1&url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "blinklist"){
	    window.open('http://www.blinklist.com/index.php?Action=Blink/addblink.php&Description=&Url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
	if(site == "blinkbits"){
	    window.open('http://www.blinkbits.com/bookmarklets/save.php?v=1&source_url='+encodeURIComponent(pageURL)+'&amp;title='+encodeURIComponent(pageTitle));
	}
}

function dotCMS_SB_toggle(id){ 
  var element = document.getElementById(id); 
  var el = document.getElementById(id);
	if ( el.style.display != 'none' ) {	el.style.display = 'none';	}
	else {		el.style.display = '';	}
 } 



