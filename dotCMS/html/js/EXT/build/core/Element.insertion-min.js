/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.Element.addMethods(function(){var GETDOM=Ext.getDom,GET=Ext.get,DH=Ext.DomHelper,isEl=function(el){return(el.nodeType||el.dom||typeof el=='string');};return{appendChild:function(el){return GET(el).appendTo(this);},appendTo:function(el){GETDOM(el).appendChild(this.dom);return this;},insertBefore:function(el){(el=GETDOM(el)).parentNode.insertBefore(this.dom,el);return this;},insertAfter:function(el){(el=GETDOM(el)).parentNode.insertBefore(this.dom,el.nextSibling);return this;},insertFirst:function(el,returnDom){el=el||{};if(isEl(el)){el=GETDOM(el);this.dom.insertBefore(el,this.dom.firstChild);return!returnDom?GET(el):el;}else{return this.createChild(el,this.dom.firstChild,returnDom);}},replace:function(el){el=GET(el);this.insertBefore(el);el.remove();return this;},replaceWith:function(el){var me=this,Element=Ext.Element;if(isEl(el)){el=GETDOM(el);me.dom.parentNode.insertBefore(el,me.dom);}else{el=DH.insertBefore(me.dom,el);}
delete Element.cache[me.id];Ext.removeNode(me.dom);me.id=Ext.id(me.dom=el);return Element.cache[me.id]=me;},createChild:function(config,insertBefore,returnDom){config=config||{tag:'div'};return insertBefore?DH.insertBefore(insertBefore,config,returnDom!==true):DH[!this.dom.firstChild?'overwrite':'append'](this.dom,config,returnDom!==true);},wrap:function(config,returnDom){var newEl=DH.insertBefore(this.dom,config||{tag:"div"},!returnDom);newEl.dom?newEl.dom.appendChild(this.dom):newEl.appendChild(this.dom);return newEl;},insertHtml:function(where,html,returnEl){var el=DH.insertHtml(where,this.dom,html);return returnEl?Ext.get(el):el;}}}());