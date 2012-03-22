/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.EventManager=function(){var docReadyEvent,docReadyProcId,docReadyState=false,E=Ext.lib.Event,D=Ext.lib.Dom,DOC=document,WINDOW=window,IEDEFERED="ie-deferred-loader",DOMCONTENTLOADED="DOMContentLoaded",elHash={},propRe=/^(?:scope|delay|buffer|single|stopEvent|preventDefault|stopPropagation|normalized|args|delegate)$/;function addListener(el,ename,fn,wrap,scope){var id=Ext.id(el),es=elHash[id]=elHash[id]||{};(es[ename]=es[ename]||[]).push([fn,wrap,scope]);E.on(el,ename,wrap);if(ename=="mousewheel"&&el.addEventListener){var args=["DOMMouseScroll",wrap,false];el.addEventListener.apply(el,args);E.on(window,'unload',function(){el.removeEventListener.apply(el,args);});}
if(ename=="mousedown"&&el==document){Ext.EventManager.stoppedMouseDownEvent.addListener(wrap);}};function fireDocReady(){if(!docReadyState){Ext.isReady=docReadyState=true;if(docReadyProcId){clearInterval(docReadyProcId);}
if(Ext.isGecko||Ext.isOpera){DOC.removeEventListener(DOMCONTENTLOADED,fireDocReady,false);}
if(Ext.isIE){var defer=DOC.getElementById(IEDEFERED);if(defer){defer.onreadystatechange=null;defer.parentNode.removeChild(defer);}}
if(docReadyEvent){docReadyEvent.fire();docReadyEvent.clearListeners();}}};function initDocReady(){var COMPLETE="complete";docReadyEvent=new Ext.util.Event();if(Ext.isGecko||Ext.isOpera){DOC.addEventListener(DOMCONTENTLOADED,fireDocReady,false);}else if(Ext.isIE){DOC.write("<s"+'cript id='+IEDEFERED+' defer="defer" src="/'+'/:"></s'+"cript>");DOC.getElementById(IEDEFERED).onreadystatechange=function(){if(this.readyState==COMPLETE){fireDocReady();}};}else if(Ext.isWebKit){docReadyProcId=setInterval(function(){if(DOC.readyState==COMPLETE){fireDocReady();}},10);}
E.on(WINDOW,"load",fireDocReady);};function createTargeted(h,o){return function(){var args=Ext.toArray(arguments);if(o.target==Ext.EventObject.setEvent(args[0]).target){h.apply(this,args);}};};function createBuffered(h,o){var task=new Ext.util.DelayedTask(h);return function(e){task.delay(o.buffer,h,null,[new Ext.EventObjectImpl(e)]);};};function createSingle(h,el,ename,fn,scope){return function(e){Ext.EventManager.removeListener(el,ename,fn,scope);h(e);};};function createDelayed(h,o){return function(e){e=new Ext.EventObjectImpl(e);setTimeout(function(){h(e);},o.delay||10);};};function listen(element,ename,opt,fn,scope){var o=!Ext.isObject(opt)?{}:opt,el=Ext.getDom(element);fn=fn||o.fn;scope=scope||o.scope;if(!el){throw"Error listening for \""+ename+'\". Element "'+element+'" doesn\'t exist.';}
function h(e){if(!Ext){return;}
e=Ext.EventObject.setEvent(e);var t;if(o.delegate){if(!(t=e.getTarget(o.delegate,el))){return;}}else{t=e.target;}
if(o.stopEvent){e.stopEvent();}
if(o.preventDefault){e.preventDefault();}
if(o.stopPropagation){e.stopPropagation();}
if(o.normalized){e=e.browserEvent;}
fn.call(scope||el,e,t,o);};if(o.target){h=createTargeted(h,o);}
if(o.delay){h=createDelayed(h,o);}
if(o.single){h=createSingle(h,el,ename,fn,scope);}
if(o.buffer){h=createBuffered(h,o);}
addListener(el,ename,fn,h,scope);return h;};var pub={addListener:function(element,eventName,fn,scope,options){if(Ext.isObject(eventName)){var o=eventName,e,val;for(e in o){val=o[e];if(!propRe.test(e)){if(Ext.isFunction(val)){listen(element,e,o,val,o.scope);}else{listen(element,e,val);}}}}else{listen(element,eventName,options,fn,scope);}},removeListener:function(element,eventName,fn,scope){var el=Ext.getDom(element),id=Ext.id(el),wrap;Ext.each((elHash[id]||{})[eventName],function(v,i,a){if(Ext.isArray(v)&&v[0]==fn&&(!scope||v[2]==scope)){E.un(el,eventName,wrap=v[1]);a.splice(i,1);return false;}});if(eventName=="mousewheel"&&el.addEventListener&&wrap){el.removeEventListener("DOMMouseScroll",wrap,false);}
if(eventName=="mousedown"&&el==DOC&&wrap){Ext.EventManager.stoppedMouseDownEvent.removeListener(wrap);}},removeAll:function(el){var id=Ext.id(el=Ext.getDom(el)),es=elHash[id],ename;for(ename in es){if(es.hasOwnProperty(ename)){Ext.each(es[ename],function(v){E.un(el,ename,v.wrap);});}}
elHash[id]=null;},onDocumentReady:function(fn,scope,options){if(docReadyState){docReadyEvent.addListener(fn,scope,options);docReadyEvent.fire();docReadyEvent.clearListeners();}else{if(!docReadyEvent)initDocReady();options=options||{};options.delay=options.delay||1;docReadyEvent.addListener(fn,scope,options);}},elHash:elHash};pub.on=pub.addListener;pub.un=pub.removeListener;pub.stoppedMouseDownEvent=new Ext.util.Event();return pub;}();Ext.onReady=Ext.EventManager.onDocumentReady;(function(){var initExtCss=function(){var bd=document.body||document.getElementsByTagName('body')[0];if(!bd){return false;}
var cls=[' ',Ext.isIE?"ext-ie "+(Ext.isIE6?'ext-ie6':(Ext.isIE7?'ext-ie7':'ext-ie8')):Ext.isGecko?"ext-gecko "+(Ext.isGecko2?'ext-gecko2':'ext-gecko3'):Ext.isOpera?"ext-opera":Ext.isWebKit?"ext-webkit":""];if(Ext.isSafari){cls.push("ext-safari "+(Ext.isSafari2?'ext-safari2':(Ext.isSafari3?'ext-safari3':'ext-safari4')));}else if(Ext.isChrome){cls.push("ext-chrome");}
if(Ext.isMac){cls.push("ext-mac");}
if(Ext.isLinux){cls.push("ext-linux");}
if(Ext.isBorderBox){cls.push('ext-border-box');}
if(Ext.isStrict){var p=bd.parentNode;if(p){p.className+=' ext-strict';}}
bd.className+=cls.join(' ');return true;}
if(!initExtCss()){Ext.onReady(initExtCss);}})();Ext.EventObject=function(){var E=Ext.lib.Event,safariKeys={3:13,63234:37,63235:39,63232:38,63233:40,63276:33,63277:34,63272:46,63273:36,63275:35},btnMap=Ext.isIE?{1:0,4:1,2:2}:(Ext.isWebKit?{1:0,2:1,3:2}:{0:0,1:1,2:2});Ext.EventObjectImpl=function(e){if(e){this.setEvent(e.browserEvent||e);}};Ext.EventObjectImpl.prototype={setEvent:function(e){var me=this;if(e==me||(e&&e.browserEvent)){return e;}
me.browserEvent=e;if(e){me.button=e.button?btnMap[e.button]:(e.which?e.which-1:-1);if(e.type=='click'&&me.button==-1){me.button=0;}
me.type=e.type;me.shiftKey=e.shiftKey;me.ctrlKey=e.ctrlKey||e.metaKey||false;me.altKey=e.altKey;me.keyCode=e.keyCode;me.charCode=e.charCode;me.target=E.getTarget(e);me.xy=E.getXY(e);}else{me.button=-1;me.shiftKey=false;me.ctrlKey=false;me.altKey=false;me.keyCode=0;me.charCode=0;me.target=null;me.xy=[0,0];}
return me;},stopEvent:function(){var me=this;if(me.browserEvent){if(me.browserEvent.type=='mousedown'){Ext.EventManager.stoppedMouseDownEvent.fire(me);}
E.stopEvent(me.browserEvent);}},preventDefault:function(){if(this.browserEvent){E.preventDefault(this.browserEvent);}},stopPropagation:function(){var me=this;if(me.browserEvent){if(me.browserEvent.type=='mousedown'){Ext.EventManager.stoppedMouseDownEvent.fire(me);}
E.stopPropagation(me.browserEvent);}},getCharCode:function(){return this.charCode||this.keyCode;},getKey:function(){return this.normalizeKey(this.keyCode||this.charCode)},normalizeKey:function(k){return Ext.isSafari?(safariKeys[k]||k):k;},getPageX:function(){return this.xy[0];},getPageY:function(){return this.xy[1];},getXY:function(){return this.xy;},getTarget:function(selector,maxDepth,returnEl){return selector?Ext.fly(this.target).findParent(selector,maxDepth,returnEl):(returnEl?Ext.get(this.target):this.target);},getRelatedTarget:function(){return this.browserEvent?E.getRelatedTarget(this.browserEvent):null;},getWheelDelta:function(){var e=this.browserEvent;var delta=0;if(e.wheelDelta){delta=e.wheelDelta/120;}else if(e.detail){delta=-e.detail/3;}
return delta;},within:function(el,related,allowEl){if(el){var t=this[related?"getRelatedTarget":"getTarget"]();return t&&((allowEl?(t==Ext.getDom(el)):false)||Ext.fly(el).contains(t));}
return false;}};return new Ext.EventObjectImpl();}();