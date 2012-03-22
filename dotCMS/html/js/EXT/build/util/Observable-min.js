/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


(function(){var EXTUTIL=Ext.util,TOARRAY=Ext.toArray,EACH=Ext.each,ISOBJECT=Ext.isObject,TRUE=true,FALSE=false;EXTUTIL.Observable=function(){var me=this,e=me.events;if(me.listeners){me.on(me.listeners);delete me.listeners;}
me.events=e||{};};EXTUTIL.Observable.prototype=function(){var filterOptRe=/^(?:scope|delay|buffer|single)$/,toLower=function(s){return s.toLowerCase();};return{fireEvent:function(){var a=TOARRAY(arguments),ename=toLower(a[0]),me=this,ret=TRUE,ce=me.events[ename],q,c;if(me.eventsSuspended===TRUE){if(q=me.suspendedEventsQueue){q.push(a);}}
else if(ISOBJECT(ce)&&ce.bubble){if(ce.fire.apply(ce,a.slice(1))===FALSE){return FALSE;}
c=me.getBubbleTarget&&me.getBubbleTarget();if(c&&c.enableBubble){c.enableBubble(ename);return c.fireEvent.apply(c,a);}}
else{if(ISOBJECT(ce)){a.shift();ret=ce.fire.apply(ce,a);}}
return ret;},addListener:function(eventName,fn,scope,o){var me=this,e,oe,isF,ce;if(ISOBJECT(eventName)){o=eventName;for(e in o){oe=o[e];if(!filterOptRe.test(e)){me.addListener(e,oe.fn||oe,oe.scope||o.scope,oe.fn?oe:o);}}}else{eventName=toLower(eventName);ce=me.events[eventName]||TRUE;if(typeof ce=="boolean"){me.events[eventName]=ce=new EXTUTIL.Event(me,eventName);}
ce.addListener(fn,scope,ISOBJECT(o)?o:{});}},removeListener:function(eventName,fn,scope){var ce=this.events[toLower(eventName)];if(ISOBJECT(ce)){ce.removeListener(fn,scope);}},purgeListeners:function(){var events=this.events,evt,key;for(key in events){evt=events[key];if(ISOBJECT(evt)){evt.clearListeners();}}},addEvents:function(o){var me=this;me.events=me.events||{};if(typeof o=='string'){EACH(arguments,function(a){me.events[a]=me.events[a]||TRUE;});}else{Ext.applyIf(me.events,o);}},hasListener:function(eventName){var e=this.events[eventName];return ISOBJECT(e)&&e.listeners.length>0;},suspendEvents:function(queueSuspended){this.eventsSuspended=TRUE;if(queueSuspended){this.suspendedEventsQueue=[];}},resumeEvents:function(){var me=this;me.eventsSuspended=!delete me.suspendedEventQueue;EACH(me.suspendedEventsQueue,function(e){me.fireEvent.apply(me,e);});}}}();var OBSERVABLE=EXTUTIL.Observable.prototype;OBSERVABLE.on=OBSERVABLE.addListener;OBSERVABLE.un=OBSERVABLE.removeListener;EXTUTIL.Observable.releaseCapture=function(o){o.fireEvent=OBSERVABLE.fireEvent;};function createTargeted(h,o,scope){return function(){if(o.target==arguments[0]){h.apply(scope,TOARRAY(arguments));}};};function createBuffered(h,o,scope){var task=new EXTUTIL.DelayedTask();return function(){task.delay(o.buffer,h,scope,TOARRAY(arguments));};}
function createSingle(h,e,fn,scope){return function(){e.removeListener(fn,scope);return h.apply(scope,arguments);};}
function createDelayed(h,o,scope){return function(){var args=TOARRAY(arguments);(function(){h.apply(scope,args);}).defer(o.delay||10);};};EXTUTIL.Event=function(obj,name){this.name=name;this.obj=obj;this.listeners=[];};EXTUTIL.Event.prototype={addListener:function(fn,scope,options){var me=this,l;scope=scope||me.obj;if(!me.isListening(fn,scope)){l=me.createListener(fn,scope,options);if(me.firing){me.listeners=me.listeners.slice(0);}
me.listeners.push(l);}},createListener:function(fn,scope,o){o=o||{},scope=scope||this.obj;var l={fn:fn,scope:scope,options:o},h=fn;if(o.target){h=createTargeted(h,o,scope);}
if(o.delay){h=createDelayed(h,o,scope);}
if(o.single){h=createSingle(h,this,fn,scope);}
if(o.buffer){h=createBuffered(h,o,scope);}
l.fireFn=h;return l;},findListener:function(fn,scope){var s,ret=-1;EACH(this.listeners,function(l,i){s=l.scope;if(l.fn==fn&&(s==scope||s==this.obj)){ret=i;return FALSE;}},this);return ret;},isListening:function(fn,scope){return this.findListener(fn,scope)!=-1;},removeListener:function(fn,scope){var index,me=this,ret=FALSE;if((index=me.findListener(fn,scope))!=-1){if(me.firing){me.listeners=me.listeners.slice(0);}
me.listeners.splice(index,1);ret=TRUE;}
return ret;},clearListeners:function(){this.listeners=[];},fire:function(){var me=this,args=TOARRAY(arguments),ret=TRUE;EACH(me.listeners,function(l){me.firing=TRUE;if(l.fireFn.apply(l.scope||me.obj||window,args)===FALSE){return ret=me.firing=FALSE;}});me.firing=FALSE;return ret;}};})();