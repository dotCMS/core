/**
 * FlashObject v1.3d: Flash detection and embed - http://blog.deconcept.com/flashobject/
 *
 * FlashObject is (c) 2006 Geoff Stearns and is released under the MIT License:
 * http://www.opensource.org/licenses/mit-license.php
 *
 */
if(typeof com=="undefined"){var com=new Object();}
if(typeof com.deconcept=="undefined"){com.deconcept=new Object();}
if(typeof com.deconcept.util=="undefined"){com.deconcept.util=new Object();}
if(typeof com.deconcept.FlashObjectUtil=="undefined"){com.deconcept.FlashObjectUtil=new Object();}
com.deconcept.FlashObject=function(_1,id,w,h,_5,c,_7,_8,_9,_a,_b){
if(!document.createElement||!document.getElementById){return;}
this.DETECT_KEY=_b?_b:"detectflash";
this.skipDetect=com.deconcept.util.getRequestParameter(this.DETECT_KEY);
this.params=new Object();
this.variables=new Object();
this.attributes=new Array();
this.useExpressInstall=_7;
if(_1){this.setAttribute("swf",_1);}
if(id){this.setAttribute("id",id);}
if(w){this.setAttribute("width",w);}
if(h){this.setAttribute("height",h);}
if(_5){this.setAttribute("version",new com.deconcept.PlayerVersion(_5.toString().split(".")));}
this.installedVer=com.deconcept.FlashObjectUtil.getPlayerVersion(this.getAttribute("version"),_7);
if(c){this.addParam("bgcolor",c);}
var q=_8?_8:"high";
this.addParam("quality",q);
var _d=(_9)?_9:window.location;
this.setAttribute("xiRedirectUrl",_d);
this.setAttribute("redirectUrl","");
if(_a){this.setAttribute("redirectUrl",_a);}
};
com.deconcept.FlashObject.prototype={setAttribute:function(_e,_f){
this.attributes[_e]=_f;
},getAttribute:function(_10){
return this.attributes[_10];
},addParam:function(_11,_12){
this.params[_11]=_12;
},getParams:function(){
return this.params;
},addVariable:function(_13,_14){
this.variables[_13]=_14;
},getVariable:function(_15){
return this.variables[_15];
},getVariables:function(){
return this.variables;
},createParamTag:function(n,v){
var p=document.createElement("param");
p.setAttribute("name",n);
p.setAttribute("value",v);
return p;
},getVariablePairs:function(){
var _19=new Array();
var key;
var _1b=this.getVariables();
for(key in _1b){_19.push(key+"="+_1b[key]);}
return _19;
},getFlashHTML:function(){
var _1c="";
if(navigator.plugins&&navigator.mimeTypes&&navigator.mimeTypes.length){
if(this.getAttribute("doExpressInstall")){
this.addVariable("MMplayerType","PlugIn");
}
_1c="<embed type=\"application/x-shockwave-flash\" src=\""+this.getAttribute("swf")+"\" width=\""+this.getAttribute("width")+"\" height=\""+this.getAttribute("height")+"\"";
_1c+=" id=\""+this.getAttribute("id")+"\" name=\""+this.getAttribute("id")+"\" ";
var _1d=this.getParams();
for(var key in _1d){_1c+=[key]+"=\""+_1d[key]+"\" ";}
var _1f=this.getVariablePairs().join("&");
if(_1f.length>0){_1c+="flashvars=\""+_1f+"\"";}
_1c+="/>";
}else{
if(this.getAttribute("doExpressInstall")){this.addVariable("MMplayerType","ActiveX");}
_1c="<object id=\""+this.getAttribute("id")+"\" classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" width=\""+this.getAttribute("width")+"\" height=\""+this.getAttribute("height")+"\">";
_1c+="<param name=\"movie\" value=\""+this.getAttribute("swf")+"\" />";
var _20=this.getParams();
for(var key in _20){_1c+="<param name=\""+key+"\" value=\""+_20[key]+"\" />";}
var _22=this.getVariablePairs().join("&");
if(_22.length>0){_1c+="<param name=\"flashvars\" value=\""+_22+"\" />";
}_1c+="</object>";}
return _1c;
},write:function(_23){
if(this.useExpressInstall){
var _24=new com.deconcept.PlayerVersion([6,0,65]);
if(this.installedVer.versionIsValid(_24)&&!this.installedVer.versionIsValid(this.getAttribute("version"))){
this.setAttribute("doExpressInstall",true);
this.addVariable("MMredirectURL",escape(this.getAttribute("xiRedirectUrl")));
document.title=document.title.slice(0,47)+" - Flash Player Installation";
this.addVariable("MMdoctitle",document.title);}
}else{this.setAttribute("doExpressInstall",false);}
if(this.skipDetect||this.getAttribute("doExpressInstall")||this.installedVer.versionIsValid(this.getAttribute("version"))){
var n=(typeof _23=="string")?document.getElementById(_23):_23;
n.innerHTML=this.getFlashHTML();
}else{if(this.getAttribute("redirectUrl")!=""){document.location.replace(this.getAttribute("redirectUrl"));}}}};
com.deconcept.FlashObjectUtil.getPlayerVersion=function(_26,_27){
var _28=new com.deconcept.PlayerVersion(0,0,0);
if(navigator.plugins&&navigator.mimeTypes.length){
var x=navigator.plugins["Shockwave Flash"];
if(x&&x.description){_28=new com.deconcept.PlayerVersion(x.description.replace(/([a-z]|[A-Z]|\s)+/,"").replace(/(\s+r|\s+b[0-9]+)/,".").split("."));}
}else{
try{var axo=new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
for(var i=3;axo!=null;i++){
axo=new ActiveXObject("ShockwaveFlash.ShockwaveFlash."+i);
_28=new com.deconcept.PlayerVersion([i,0,0]);}}
catch(e){}
if(_26&&_28.major>_26.major){return _28;}
if(!_26||((_26.minor!=0||_26.rev!=0)&&_28.major==_26.major)||_28.major!=6||_27){
try{
_28=new com.deconcept.PlayerVersion(axo.GetVariable("$version").split(" ")[1].split(","));
}catch(e){}}}
return _28;
};
com.deconcept.PlayerVersion=function(_2c){
this.major=parseInt(_2c[0])||0;
this.minor=parseInt(_2c[1])||0;
this.rev=parseInt(_2c[2])||0;
};
com.deconcept.PlayerVersion.prototype.versionIsValid=function(fv){
if(this.major<fv.major){return false;}
if(this.major>fv.major){return true;}
if(this.minor<fv.minor){return false;}
if(this.minor>fv.minor){return true;}
if(this.rev<fv.rev){return false;}
return true;
};
com.deconcept.util={getRequestParameter:function(_2e){
var q=document.location.search||document.location.hash;
if(q){var _30=q.indexOf(_2e+"=");
var _31=(q.indexOf("&",_30)>-1)?q.indexOf("&",_30):q.length;
if(q.length>1&&_30>-1){
return q.substring(q.indexOf("=",_30)+1,_31);}}return "";
},removeChildren:function(n){
while(n.hasChildNodes()){
n.removeChild(n.firstChild);}}};
if(Array.prototype.push==null){
Array.prototype.push=function(_33){
this[this.length]=_33;
return this.length;};}
var getQueryParamValue=com.deconcept.util.getRequestParameter;
var FlashObject=com.deconcept.FlashObject;

