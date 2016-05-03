/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.Element.addMethods({isScrollable:function(){var dom=this.dom;return dom.scrollHeight>dom.clientHeight||dom.scrollWidth>dom.clientWidth;},scrollTo:function(side,value){this.dom["scroll"+(/top/i.test(side)?"Top":"Left")]=value;return this;},getScroll:function(){var d=this.dom,doc=document,body=doc.body,docElement=doc.documentElement,l,t,ret;if(d==doc||d==body){if(Ext.isIE&&Ext.isStrict){l=docElement.scrollLeft;t=docElement.scrollTop;}else{l=window.pageXOffset;t=window.pageYOffset;}
ret={left:l||(body?body.scrollLeft:0),top:t||(body?body.scrollTop:0)};}else{ret={left:d.scrollLeft,top:d.scrollTop};}
return ret;}});