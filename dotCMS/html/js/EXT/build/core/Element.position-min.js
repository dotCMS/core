/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


(function(){var D=Ext.lib.Dom,LEFT="left",RIGHT="right",TOP="top",BOTTOM="bottom",POSITION="position",STATIC="static",RELATIVE="relative",AUTO="auto",ZINDEX="z-index";function animTest(args,animate,i){return this.preanim&&!!animate?this.preanim(args,i):false}
Ext.Element.addMethods({getX:function(){return D.getX(this.dom);},getY:function(){return D.getY(this.dom);},getXY:function(){return D.getXY(this.dom);},getOffsetsTo:function(el){var o=this.getXY(),e=Ext.fly(el,'_internal').getXY();return[o[0]-e[0],o[1]-e[1]];},setX:function(x,animate){return this.setXY([x,this.getY()],animTest.call(this,arguments,animate,1));},setY:function(y,animate){return this.setXY([this.getX(),y],animTest.call(this,arguments,animate,1));},setLeft:function(left){this.setStyle(LEFT,this.addUnits(left));return this;},setTop:function(top){this.setStyle(TOP,this.addUnits(top));return this;},setRight:function(right){this.setStyle(RIGHT,this.addUnits(right));return this;},setBottom:function(bottom){this.setStyle(BOTTOM,this.addUnits(bottom));return this;},setXY:function(pos,animate){var me=this;if(!animate||!me.anim){D.setXY(me.dom,pos);}else{me.anim({points:{to:pos}},me.preanim(arguments,1),'motion');}
return me;},setLocation:function(x,y,animate){return this.setXY([x,y],animTest.call(this,arguments,animate,2));},moveTo:function(x,y,animate){return this.setXY([x,y],animTest.call(this,arguments,animate,2));},getLeft:function(local){return!local?this.getX():parseInt(this.getStyle(LEFT),10)||0;},getRight:function(local){var me=this;return!local?me.getX()+me.getWidth():(me.getLeft(true)+me.getWidth())||0;},getTop:function(local){return!local?this.getY():parseInt(this.getStyle(TOP),10)||0;},getBottom:function(local){var me=this;return!local?me.getY()+me.getHeight():(me.getTop(true)+me.getHeight())||0;},position:function(pos,zIndex,x,y){var me=this;if(!pos&&me.isStyle(POSITION,STATIC)){me.setStyle(POSITION,RELATIVE);}else if(pos){me.setStyle(POSITION,pos);}
if(zIndex){me.setStyle(ZINDEX,zIndex);}
if(x||y)me.setXY([x||false,y||false]);},clearPositioning:function(value){value=value||'';this.setStyle({left:value,right:value,top:value,bottom:value,"z-index":"",position:STATIC});return this;},getPositioning:function(){var l=this.getStyle(LEFT);var t=this.getStyle(TOP);return{"position":this.getStyle(POSITION),"left":l,"right":l?"":this.getStyle(RIGHT),"top":t,"bottom":t?"":this.getStyle(BOTTOM),"z-index":this.getStyle(ZINDEX)};},setPositioning:function(pc){var me=this,style=me.dom.style;me.setStyle(pc);if(pc.right==AUTO){style.right="";}
if(pc.bottom==AUTO){style.bottom="";}
return me;},translatePoints:function(x,y){y=isNaN(x[1])?y:x[1];x=isNaN(x[0])?x:x[0];var me=this,relative=me.isStyle(POSITION,RELATIVE),o=me.getXY(),l=parseInt(me.getStyle(LEFT),10),t=parseInt(me.getStyle(TOP),10);l=!isNaN(l)?l:(relative?0:me.dom.offsetLeft);t=!isNaN(t)?t:(relative?0:me.dom.offsetTop);return{left:(x-o[0]+l),top:(y-o[1]+t)};},animTest:animTest});})();