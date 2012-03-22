/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


(function(){var DOC=document;Ext.Element=function(element,forceNew){var dom=typeof element=="string"?DOC.getElementById(element):element,id;if(!dom)return null;id=dom.id;if(!forceNew&&id&&Ext.Element.cache[id]){return Ext.Element.cache[id];}
this.dom=dom;this.id=id||Ext.id(dom);};var D=Ext.lib.Dom,DH=Ext.DomHelper,E=Ext.lib.Event,A=Ext.lib.Anim,El=Ext.Element;El.prototype={set:function(o,useSet){var el=this.dom,attr,val;for(attr in o){val=o[attr];if(attr!="style"&&!Ext.isFunction(val)){if(attr=="cls"){el.className=val;}else if(o.hasOwnProperty(attr)){if(useSet||!!el.setAttribute)el.setAttribute(attr,val);else el[attr]=val;}}}
if(o.style){Ext.DomHelper.applyStyles(el,o.style);}
return this;},defaultUnit:"px",is:function(simpleSelector){return Ext.DomQuery.is(this.dom,simpleSelector);},focus:function(defer,dom){var me=this,dom=dom||me.dom;try{if(Number(defer)){me.focus.defer(defer,null,[null,dom]);}else{dom.focus();}}catch(e){}
return me;},blur:function(){try{this.dom.blur();}catch(e){}
return this;},getValue:function(asNumber){var val=this.dom.value;return asNumber?parseInt(val,10):val;},addListener:function(eventName,fn,scope,options){Ext.EventManager.on(this.dom,eventName,fn,scope||this,options);return this;},removeListener:function(eventName,fn,scope){Ext.EventManager.removeListener(this.dom,eventName,fn,scope||this);return this;},removeAllListeners:function(){Ext.EventManager.removeAll(this.dom);return this;},addUnits:function(size){if(size===""||size=="auto"||size===undefined){size=size||'';}else if(!isNaN(size)||!unitPattern.test(size)){size=size+(this.defaultUnit||'px');}
return size;},load:function(url,params,cb){Ext.Ajax.request(Ext.apply({params:params,url:url.url||url,callback:cb,el:this.dom,indicatorText:url.indicatorText||''},Ext.isObject(url)?url:{}));return this;},isBorderBox:function(){return noBoxAdjust[(this.dom.tagName||"").toLowerCase()]||Ext.isBorderBox;},remove:function(){var me=this,dom=me.dom;me.removeAllListeners();delete El.cache[dom.id];delete El.dataCache[dom.id]
Ext.removeNode(dom);},hover:function(overFn,outFn,scope,options){var me=this;me.on('mouseenter',overFn,scope||me.dom,options);me.on('mouseleave',outFn,scope||me.dom,options);return me;},contains:function(el){return!el?false:Ext.lib.Dom.isAncestor(this.dom,el.dom?el.dom:el);},getAttributeNS:function(ns,name){return this.getAttribute(name,ns);},getAttribute:Ext.isIE?function(name,ns){var d=this.dom,type=typeof d[ns+":"+name];if(['undefined','unknown'].indexOf(type)==-1){return d[ns+":"+name];}
return d[name];}:function(name,ns){var d=this.dom;return d.getAttributeNS(ns,name)||d.getAttribute(ns+":"+name)||d.getAttribute(name)||d[name];},update:function(html){this.dom.innerHTML=html;}};var ep=El.prototype;El.addMethods=function(o){Ext.apply(ep,o);};ep.on=ep.addListener;ep.un=ep.removeListener;ep.autoBoxAdjust=true;var unitPattern=/\d+(px|em|%|en|ex|pt|in|cm|mm|pc)$/i,docEl;El.cache={};El.dataCache={};El.get=function(el){var ex,elm,id;if(!el){return null;}
if(typeof el=="string"){if(!(elm=DOC.getElementById(el))){return null;}
if(ex=El.cache[el]){ex.dom=elm;}else{ex=El.cache[el]=new El(elm);}
return ex;}else if(el.tagName){if(!(id=el.id)){id=Ext.id(el);}
if(ex=El.cache[id]){ex.dom=el;}else{ex=El.cache[id]=new El(el);}
return ex;}else if(el instanceof El){if(el!=docEl){el.dom=DOC.getElementById(el.id)||el.dom;El.cache[el.id]=el;}
return el;}else if(el.isComposite){return el;}else if(Ext.isArray(el)){return El.select(el);}else if(el==DOC){if(!docEl){var f=function(){};f.prototype=El.prototype;docEl=new f();docEl.dom=DOC;}
return docEl;}
return null;};El.data=function(el,key,value){var c=El.dataCache[el.id];if(!c){c=El.dataCache[el.id]={};}
if(arguments.length==2){return c[key];}else{c[key]=value;}};function garbageCollect(){if(!Ext.enableGarbageCollector){clearInterval(El.collectorThread);}else{var eid,el,d;for(eid in El.cache){el=El.cache[eid];d=el.dom;if(!d||!d.parentNode||(!d.offsetParent&&!DOC.getElementById(eid))){delete El.cache[eid];if(d&&Ext.enableListenerCollection){Ext.EventManager.removeAll(d);}}}}}
El.collectorThreadId=setInterval(garbageCollect,30000);var flyFn=function(){};flyFn.prototype=El.prototype;El.Flyweight=function(dom){this.dom=dom;};El.Flyweight.prototype=new flyFn();El.Flyweight.prototype.isFlyweight=true;El._flyweights={};El.fly=function(el,named){var ret=null;named=named||'_global';if(el=Ext.getDom(el)){(El._flyweights[named]=El._flyweights[named]||new El.Flyweight()).dom=el;ret=El._flyweights[named];}
return ret;};Ext.get=El.get;Ext.fly=El.fly;var noBoxAdjust=Ext.isStrict?{select:1}:{input:1,select:1,textarea:1};if(Ext.isIE||Ext.isGecko){noBoxAdjust['button']=1;}
Ext.EventManager.on(window,'unload',function(){delete El.cache;delete El.dataCache;delete El._flyweights;});})();