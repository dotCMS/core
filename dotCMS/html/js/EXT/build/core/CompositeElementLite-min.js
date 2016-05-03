/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.CompositeElementLite=function(els,root){this.elements=[];this.add(els,root);this.el=new Ext.Element.Flyweight();};Ext.CompositeElementLite.prototype={isComposite:true,getCount:function(){return this.elements.length;},add:function(els){if(els){if(Ext.isArray(els)){this.elements=this.elements.concat(els);}else{var yels=this.elements;Ext.each(els,function(e){yels.push(e);});}}
return this;},invoke:function(fn,args){var els=this.elements,el=this.el;Ext.each(els,function(e){el.dom=e;Ext.Element.prototype[fn].apply(el,args);});return this;},item:function(index){var me=this;if(!me.elements[index]){return null;}
me.el.dom=me.elements[index];return me.el;},addListener:function(eventName,handler,scope,opt){Ext.each(this.elements,function(e){Ext.EventManager.on(e,eventName,handler,scope||e,opt);});return this;},each:function(fn,scope){var me=this,el=me.el;Ext.each(me.elements,function(e,i){el.dom=e;return fn.call(scope||el,el,me,i);});return me;},indexOf:function(el){return this.elements.indexOf(Ext.getDom(el));},replaceElement:function(el,replacement,domReplace){var index=!isNaN(el)?el:this.indexOf(el),d;if(index>-1){replacement=Ext.getDom(replacement);if(domReplace){d=this.elements[index];d.parentNode.insertBefore(replacement,d);Ext.removeNode(d);}
this.elements.splice(index,1,replacement);}
return this;},clear:function(){this.elements=[];}}
Ext.CompositeElementLite.prototype.on=Ext.CompositeElementLite.prototype.addListener;(function(){var fnName,ElProto=Ext.Element.prototype,CelProto=Ext.CompositeElementLite.prototype;for(var fnName in ElProto){if(Ext.isFunction(ElProto[fnName])){(function(fnName){CelProto[fnName]=CelProto[fnName]||function(){return this.invoke(fnName,arguments);};}).call(CelProto,fnName);}};})();if(Ext.DomQuery){Ext.Element.selectorFunction=Ext.DomQuery.select;}
Ext.Element.select=function(selector,unique,root){var els;if(typeof selector=="string"){els=Ext.Element.selectorFunction(selector,root);}else if(selector.length!==undefined){els=selector;}else{throw"Invalid selector";}
return new Ext.CompositeElementLite(els);};Ext.select=Ext.Element.select;