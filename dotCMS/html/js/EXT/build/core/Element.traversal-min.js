/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.Element.addMethods(function(){var PARENTNODE='parentNode',NEXTSIBLING='nextSibling',PREVIOUSSIBLING='previousSibling',DQ=Ext.DomQuery,GET=Ext.get;return{findParent:function(simpleSelector,maxDepth,returnEl){var p=this.dom,b=document.body,depth=0,stopEl;if(Ext.isGecko&&Object.prototype.toString.call(p)=='[object XULElement]'){return null;}
maxDepth=maxDepth||50;if(isNaN(maxDepth)){stopEl=Ext.getDom(maxDepth);maxDepth=10;}
while(p&&p.nodeType==1&&depth<maxDepth&&p!=b&&p!=stopEl){if(DQ.is(p,simpleSelector)){return returnEl?GET(p):p;}
depth++;p=p.parentNode;}
return null;},findParentNode:function(simpleSelector,maxDepth,returnEl){var p=Ext.fly(this.dom.parentNode,'_internal');return p?p.findParent(simpleSelector,maxDepth,returnEl):null;},up:function(simpleSelector,maxDepth){return this.findParentNode(simpleSelector,maxDepth,true);},select:function(selector,unique){return Ext.Element.select(selector,unique,this.dom);},query:function(selector,unique){return DQ.select(selector,this.dom);},child:function(selector,returnDom){var n=DQ.selectNode(selector,this.dom);return returnDom?n:GET(n);},down:function(selector,returnDom){var n=DQ.selectNode(" > "+selector,this.dom);return returnDom?n:GET(n);},parent:function(selector,returnDom){return this.matchNode(PARENTNODE,PARENTNODE,selector,returnDom);},next:function(selector,returnDom){return this.matchNode(NEXTSIBLING,NEXTSIBLING,selector,returnDom);},prev:function(selector,returnDom){return this.matchNode(PREVIOUSSIBLING,PREVIOUSSIBLING,selector,returnDom);},first:function(selector,returnDom){return this.matchNode(NEXTSIBLING,'firstChild',selector,returnDom);},last:function(selector,returnDom){return this.matchNode(PREVIOUSSIBLING,'lastChild',selector,returnDom);},matchNode:function(dir,start,selector,returnDom){var n=this.dom[start];while(n){if(n.nodeType==1&&(!selector||DQ.is(n,selector))){return!returnDom?GET(n):n;}
n=n[dir];}
return null;}}}());