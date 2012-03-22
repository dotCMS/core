/*
Copyright (c) 2005, Fabricio Zuardi
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
stop();
//autoplay=true
//repeat_playlist = true;
//playlist_size = 3;
//player_title = "customizeable title test"
//song_url = "http://downloads.betterpropaganda.com/music/Imperial_Teen-Ivanka_128.mp3";
//playlist_url = "http://cchits.ning.com/recent/xspf/?xn_auth=no";
//playlist_url = "http://hideout.com.br/shows/radio-test.xspf";
//radio_mode = true;
//song_title = "Imperial Teen - Ivanka";
autoload=true;
//info_button_text = "Add to Cart"
//playlist_url = "http%3A%2F%2Fwebjay%2Eorg%2Fby%2Flucas%5Fgonze%2Flaconic%2Exspf"
//playlist_url= "http://hideout.com.br/tests/hideout2325.xspf"
//constants
DEFAULT_PLAYLIST_URL = "http://hideout.com.br/shows/allshows.xspf";
DEFAULT_WELCOME_MSG = "Hideout XSPF Music Player - by Fabricio Zuardi";
LOADING_PLAYLIST_MSG = "Loading Playlist...";
DEFAULT_LOADED_PLAYLIST_MSG = "- click to start"
DEFAULT_INFOBUTTON_TXT = "Info"
//playlists has priority over songs, if a playlist_url parameter is found the song_url is ignored
//default playlist if none is passed through query string
if(!playlist_url){
	if(!song_url){
		playlist_url = DEFAULT_PLAYLIST_URL;
	}else{
		single_music_playlist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><playlist version=\"1\" xmlns = \"http://xspf.org/ns/0/\"><trackList>";
		single_music_playlist += "<track><location>"+song_url+"</location><annotation>"+song_title+"</annotation></track>"
		single_music_playlist += "</trackList></playlist>"
	}
}
info_mc._visible=false;
if(!info_button_text){
	info_button_text = DEFAULT_INFOBUTTON_TXT;
}
info_mc.display_txt.text = info_button_text;

//variables initialization
playlist_array = [];
track_index = 0;
playlist_mc.track_count = 0;
pause_position = 0;
volume_level = 100;
playlist_xml = new XML();
playlist_xml.ignoreWhite = true;
playlist_xml.onLoad = playlistLoaded;
mysound = new Sound(this);
playlist_listener = new Object();
playlist_list.addEventListener("change", playlist_listener)
//play_btn.onPress = playTrack;
//functions
//xml parser
function playlistLoaded (success){
	if(success){
		var root_node = this.firstChild;
		for(var node = root_node.firstChild; node != null; node = node.nextSibling){
			if(node.nodeName == "title"){
				playlist_title = node.firstChild.nodeValue;
			}
			if(node.nodeName == "trackList"){
				//tracks
				var tracks_array = [];
				for(var track_node = node.firstChild; track_node != null; track_node = track_node.nextSibling){
					var track_obj = new Object()
					//track attributes
					for(var track_child = track_node.firstChild; track_child != null; track_child = track_child.nextSibling){
						if(track_child.nodeName=="location"){
							track_obj.location = track_child.firstChild.nodeValue
						}
						if(track_child.nodeName=="image"){
							track_obj.image = track_child.firstChild.nodeValue
						}
						if(track_child.nodeName=="title"){
							track_obj.title = track_child.firstChild.nodeValue
						}
						if(track_child.nodeName=="creator"){
							track_obj.creator = track_child.firstChild.nodeValue
						}
						if(track_child.nodeName=="annotation"){
							track_obj.annotation = track_child.firstChild.nodeValue
						}
						if(track_child.nodeName=="info"){
							track_obj.info = track_child.firstChild.nodeValue
						}
					}
					track_obj.label = (tracks_array.length+1) +". ";
					if(track_obj.title) {
						if(track_obj.creator) {
							track_obj.label += track_obj.creator+' - ';
						}
						track_obj.label += track_obj.title;
					} else {
						track_obj.label += track_obj.annotation;
					}
					tracks_array.push(track_obj)
					addTrack(track_obj.label);
				}
			}
		}
		playlist_array = tracks_array;
		if(!playlist_size) playlist_size = playlist_array.length;
		playlist_mc.tracks_mc["track_"+track_index+"_mc"].bg_mc.gotoAndStop(2)
		if(autoplay){
			loadTrack()
		}else{
			start_btn_mc.start_btn.onPress = loadTrack;
			track_display_mc.display_txt.text = playlist_title+" "+DEFAULT_LOADED_PLAYLIST_MSG;
			if(track_display_mc.display_txt._width>track_display_mc.mask_mc._width){
				track_display_mc.onEnterFrame = scrollTitle;
			}else{
				track_display_mc.onEnterFrame = null;
				track_display_mc.display_txt._x = 0;
			}
		}
	}else{
		annotation_txt.text = "Error opening "+playlist_url;
	}
}

playlist_listener.change = function(eventObject){
  annotation_txt.text = playlist_list.selectedItem.annotation;
  location_txt.text = playlist_list.selectedItem.location;
}

function loadTrack(){

	//Radio Mode feature by nosferathoo, more info in: https://sourceforge.net/tracker/index.php?func=detail&aid=1341940&group_id=128363&atid=711474
	if (radio_mode && track_index==playlist_size-1) {
		playlist_url=playlist_array[track_index].location;
		for (i=0;i<playlist_mc.track_count;++i) {
			removeMovieClip(playlist_mc.tracks_mc["track_"+i+"_mc"]);
		}
		playlist_mc.track_count=0;
		playlist_size=0;
		track_index=0;
		autoload=true;
		autoplay=true;
		loadPlaylist();
		return(0);
	}

	start_btn_mc.start_btn._visible = false;
	track_display_mc.display_txt.text = playlist_array[track_index].label;
	if(track_display_mc.display_txt._width>track_display_mc.mask_mc._width){
		track_display_mc.onEnterFrame = scrollTitle;
	}else{
		track_display_mc.onEnterFrame = null;
		track_display_mc.display_txt._x = 0;
	}
	cover_mc.content_mc["photo"+last_track_index].removeMovieClip();
	mysound.loadSound(playlist_array[track_index].location,true);
	play_mc.gotoAndStop(2)

	//image from playlist
	if(playlist_array[track_index].image!=undefined){
		cover_mc.content_mc.createEmptyMovieClip("photo"+track_index,track_index)
		cover_mc.content_mc["photo"+track_index].loadMovie(playlist_array[track_index].image)
	}else{
	}
	//info button
	if(playlist_array[track_index].info!=undefined){
		info_mc._visible = true;
		info_mc.info_btn.onPress = function(){
			getURL(playlist_array[track_index].info,"_blank")
		}
	}else{
		info_mc._visible = false;
	}
	_root.onEnterFrame=function(){
		//HACK doesnt need to set the volume at every enterframe
		mysound.setVolume(this.volume_level)
		var sound_load_percent = (mysound.getBytesLoaded()/mysound.getBytesTotal())*100
		track_display_mc.loader_mc.load_bar_mc._xscale = sound_load_percent;
		var image_load_percent = (cover_mc.content_mc["photo"+track_index].getBytesLoaded()/cover_mc.content_mc["photo"+track_index].getBytesTotal())*100
		cover_mc.load_bar_mc._xscale = image_load_percent;
		if((cover_mc.content_mc["photo"+track_index].getBytesLoaded()>4)&&(image_load_percent==100)){
			//image loaded
			//make image fit
			cover_mc.content_mc["photo"+track_index]._width = cover_mc.load_bar_mc._width
			cover_mc.content_mc["photo"+track_index]._height = cover_mc.load_bar_mc._height
		}
	}
}

stop_btn.onRelease = stopTrack;
play_mc.play_btn.onRelease = playTrack
next_btn.onRelease = nextTrack
prev_btn.onRelease = prevTrack
mysound.onSoundComplete = nextTrack;
volume_mc.volume_btn.onPress = volumeChange;
volume_mc.volume_btn.onRelease = volume_mc.volume_btn.onReleaseOutside = function(){
	this._parent.onMouseMove = this._parent.onMouseDown = null;
}

function volumeChange(){
	this._parent.onMouseMove = this._parent.onMouseDown = function(){
		var percent = (this._xmouse/this._width)*100
		if(percent>100)percent=100;
		if(percent<0)percent=0;
		this.volume_bar_mc._xscale = percent
		this._parent.volume_level = percent;
		mysound.setVolume(percent)
	}
}

function stopTrack() {
	mysound.stop();
	play_mc.gotoAndStop(1)
	mysound.stop();
	mysound.start();
	mysound.stop();
	_root.pause_position = 0;

};
function playTrack() {
	if(play_mc._currentframe==1){ //play
		seekTrack(_root.pause_position)
		play_mc.gotoAndStop(2)
	}else if(play_mc._currentframe==2){
		_root.pause_position = mysound.position;
		mysound.stop();
		play_mc.gotoAndStop(1)
	}

};
function seekTrack(p_offset:Number){
	mysound.stop()
	mysound.start(int((p_offset)/1000),1)
}
function nextTrack(){
	if(track_index<playlist_size-1){
		last_track_index = track_index;
		track_index ++;
		loadTrack();
	}else{
		if(repeat_playlist){
			last_track_index = track_index;
			track_index = 0;
			loadTrack()
		}
	}
	playlist_mc.tracks_mc["track_"+last_track_index+"_mc"].bg_mc.gotoAndStop(1)
	playlist_mc.tracks_mc["track_"+track_index+"_mc"].bg_mc.gotoAndStop(2)
}

function prevTrack(){
	if(track_index>0){
		last_track_index = track_index;
		track_index --;
		loadTrack();
	}
	playlist_mc.tracks_mc["track_"+last_track_index+"_mc"].bg_mc.gotoAndStop(1)
	playlist_mc.tracks_mc["track_"+track_index+"_mc"].bg_mc.gotoAndStop(2)
}

function scrollTitle(){
	track_display_mc.display_txt._x -= 5;
	if (track_display_mc.display_txt._x+track_display_mc.display_txt._width<0){
		track_display_mc.display_txt._x = track_display_mc.mask_mc._width;
	}
}

function resizeUI(){
	bg_mc._width = Stage.width;
	track_display_mc.loader_mc._width = Stage.width - track_display_mc._x - 2;
	track_display_mc.mask_mc._width = track_display_mc.loader_mc._width-3;
	if(track_display_mc.display_txt._width>track_display_mc.mask_mc._width){
		track_display_mc.onEnterFrame = scrollTitle;
	}else{
		track_display_mc.onEnterFrame = null;
		track_display_mc.display_txt._x = 0;
	}
	volume_mc._x = Stage.width - 22;
	start_btn_mc._xscale = Stage.width;
	//playlist area tinnier that the album cover
	if(Stage.width<2.5*cover_mc._width){
		//
		if (Stage.height>2.5*cover_mc._height){
			//send album cover to bottom
			cover_mc._y = Stage.height - cover_mc._height -2- info_mc._height -2;
			info_mc._y = Stage.height - info_mc._height -2;
			var covervisible =1;
		}else{
			var covervisible =0;
			//hide album cover
			cover_mc._y = Stage.height;
		}
		//send playlist to left
		playlist_mc._x = cover_mc._x;
		scrollbar_mc.bg_mc._height = Stage.height - (19+(cover_mc._height+info_mc._height+4)*covervisible);
		playlist_mc.bg_mc._height = Stage.height - (19+(cover_mc._height+info_mc._height+4)*covervisible);
		playlist_mc.mask_mc._height = Stage.height - (23+(cover_mc._height+info_mc._height+4)*covervisible);
	}else{
		cover_mc._y = 17;
		info_mc._y = 153;
		playlist_mc._x = 138;
		scrollbar_mc.bg_mc._height = Stage.height -19;
		playlist_mc.bg_mc._height = Stage.height - 19;
		playlist_mc.mask_mc._height = Stage.height - 23;
	}
	scrollbar_mc._x = Stage.width - 12;
	playlist_mc.mask_mc._width = Stage.width - (playlist_mc._x + 19);
	playlist_mc.bg_mc._width = Stage.width - (playlist_mc._x + 14);
}
function addTrack(track_label){
	if(playlist_mc.track_count>0) {
		playlist_mc.tracks_mc.track_0_mc.duplicateMovieClip("track_"+playlist_mc.track_count+"_mc",playlist_mc.track_count);
	}
	playlist_mc.tracks_mc["track_"+playlist_mc.track_count+"_mc"]._y += playlist_mc.track_count*14;
	playlist_mc.tracks_mc["track_"+playlist_mc.track_count+"_mc"].display_txt.autoSize = "left";
	playlist_mc.tracks_mc["track_"+playlist_mc.track_count+"_mc"].display_txt.text = track_label;
	playlist_mc.tracks_mc["track_"+playlist_mc.track_count+"_mc"].bg_mc.index = playlist_mc.track_count;
	playlist_mc.tracks_mc["track_"+playlist_mc.track_count+"_mc"].bg_mc.select_btn.onPress = function(){
		last_track_index = track_index;
		playlist_mc.tracks_mc["track_"+track_index+"_mc"].bg_mc.gotoAndStop(1)
		track_index = this._parent.index;
		playlist_mc.tracks_mc["track_"+track_index+"_mc"].bg_mc.gotoAndStop(2)
		loadTrack();
	}
	playlist_mc.track_count ++;
}
//scroll

scrollbar_mc.up_btn.onPress = function(){
	this._parent.v = -1;
	this._parent.onEnterFrame = scrollTracks;
}
scrollbar_mc.down_btn.onPress = function(){
	this._parent.v = 1;
	this._parent.onEnterFrame = scrollTracks;
}
scrollbar_mc.up_btn.onRelease = scrollbar_mc.down_btn.onRelease = function(){
	this._parent.onEnterFrame = null;
}
scrollbar_mc.handler_mc.drag_btn.onPress = function(){
	var scroll_top_limit = 19;
	var scroll_bottom_limit = scrollbar_mc.bg_mc._height - scrollbar_mc.handler_mc._height - 2;
	this._parent.startDrag(false,this._parent._x,scroll_top_limit,this._parent._x,scroll_bottom_limit)
	this._parent.isdragging = true;
	this._parent.onEnterFrame = scrollTracks;
}
scrollbar_mc.handler_mc.drag_btn.onRelease = scrollbar_mc.handler_mc.drag_btn.onReleaseOutside = function(){
	stopDrag()
	this._parent.isdragging = false;
	this._parent.onEnterFrame = null;
}
function scrollTracks(){
	var scroll_top_limit = 19;
	var scroll_bottom_limit = scrollbar_mc.bg_mc._height - scrollbar_mc.handler_mc._height - 2;
	var list_bottom_limit = 1;
	var list_top_limit = (1-Math.round(playlist_mc.tracks_mc._height))+Math.floor(playlist_mc.mask_mc._height/14)*14
	if(playlist_mc.tracks_mc._height>playlist_mc.mask_mc._height){
		if(scrollbar_mc.handler_mc.isdragging){
			var percent = (scrollbar_mc.handler_mc._y-scroll_top_limit)/(scroll_bottom_limit-scroll_top_limit)
			playlist_mc.tracks_mc._y = (list_top_limit-list_bottom_limit)*percent+list_bottom_limit;
		}else{
			if(scrollbar_mc.v==-1){
				if(playlist_mc.tracks_mc._y+14<list_bottom_limit){
					playlist_mc.tracks_mc._y += 14;
				}else{
					playlist_mc.tracks_mc._y = list_bottom_limit;
				}
				var percent = (playlist_mc.tracks_mc._y-1)/(list_top_limit-1)
				scrollbar_mc.handler_mc._y = percent*(scroll_bottom_limit - scroll_top_limit)+scroll_top_limit;
			}else if(scrollbar_mc.v==1){
				if(playlist_mc.tracks_mc._y-14>list_top_limit){
					playlist_mc.tracks_mc._y -= 14;
				}else{
					playlist_mc.tracks_mc._y = list_top_limit;
				}
				var percent = (playlist_mc.tracks_mc._y-1)/(list_top_limit-1)
				scrollbar_mc.handler_mc._y = percent*(scroll_bottom_limit - scroll_top_limit)+scroll_top_limit;
			}
		}
	}
}
function loadPlaylist(){
	track_display_mc.display_txt.text = LOADING_PLAYLIST_MSG;
	if(track_display_mc.display_txt._width>track_display_mc.mask_mc._width){
		track_display_mc.onEnterFrame = scrollTitle;
	}else{
		track_display_mc.onEnterFrame = null;
		track_display_mc.display_txt._x = 0;
	}

	//playlist
	if(playlist_url){
		playlist_xml.load(unescape(playlist_url))
	}else{
	//single track
		playlist_xml.parseXML(single_music_playlist)
		playlist_xml.onLoad(true);
	}
}

//first click - load playlist
start_btn_mc.start_btn.onPress = function(){
	autoplay = true;
	loadPlaylist();
}


//main
Stage.scaleMode = "noScale"
Stage.align = "LT";
this.onResize = resizeUI;
Stage.addListener(this);
if(!player_title) player_title = DEFAULT_WELCOME_MSG;
track_display_mc.display_txt.autoSize = "left";
track_display_mc.display_txt.text = player_title;
if(track_display_mc.display_txt._width>track_display_mc.mask_mc._width){
	track_display_mc.onEnterFrame = scrollTitle;
}else{
	track_display_mc.onEnterFrame = null;
	track_display_mc.display_txt._x = 0;
}
//start to play automatically if parameter autoplay is present
if(autoplay){
	start_btn_mc.start_btn.onPress();
} else if (autoload){
	loadPlaylist()
}
//customized menu
var my_cm:ContextMenu = new ContextMenu();
my_cm.customItems.push(new ContextMenuItem("Stop", stopTrack));
my_cm.customItems.push(new ContextMenuItem("Play!", playTrack));
my_cm.customItems.push(new ContextMenuItem("Next", nextTrack));
my_cm.customItems.push(new ContextMenuItem("Previous", prevTrack));
my_cm.customItems.push(new ContextMenuItem("Download this song", function(){getURL(playlist_array[track_index].location,"_blank")},true));
my_cm.customItems.push(new ContextMenuItem("Add song to Webjay playlist", function(){getURL("http://webjay.org/poster?media="+escape(playlist_array[track_index].location),"_blank")}));
my_cm.customItems.push(new ContextMenuItem("About Hideout", function(){getURL("http://www.hideout.com.br","_blank")},true));
//my_cm.customItems.push(new ContextMenuItem("Crossfade", function(){}));
//my_cm.customItems.push(new ContextMenuItem("Mando Diao - Paralyzed", function(){}));
my_cm.hideBuiltInItems();
this.menu = my_cm;
resizeUI();