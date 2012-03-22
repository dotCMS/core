/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.Element.addMethods(function(){var propCache={},camelRe=/(-[a-z])/gi,classReCache={},view=document.defaultView,propFloat=Ext.isIE?'styleFloat':'cssFloat',opacityRe=/alpha\(opacity=(.*)\)/i,trimRe=/^\s+|\s+$/g,EL=Ext.Element,PADDING="padding",MARGIN="margin",BORDER="border",LEFT="-left",RIGHT="-right",TOP="-top",BOTTOM="-bottom",WIDTH="-width",borders={l:BORDER+LEFT+WIDTH,r:BORDER+RIGHT+WIDTH,t:BORDER+TOP+WIDTH,b:BORDER+BOTTOM+WIDTH},paddings={l:PADDING+LEFT,r:PADDING+RIGHT,t:PADDING+TOP,b:PADDING+BOTTOM},margins={l:MARGIN+LEFT,r:MARGIN+RIGHT,t:MARGIN+TOP,b:MARGIN+BOTTOM},data=Ext.Element.data;function camelFn(m,a){return a.charAt(1).toUpperCase();}
function addStyles(sides,styles){var val=0;Ext.each(sides.match(/\w/g),function(s){if(s=parseInt(this.getStyle(styles[s]),10)){val+=Math.abs(s);}},this);return val;}
function chkCache(prop){return propCache[prop]||(propCache[prop]=prop=='float'?propFloat:prop.replace(camelRe,camelFn));}
return{adjustWidth:function(width){var me=this;if(typeof width=="number"&&me.autoBoxAdjust&&!me.isBorderBox()){width-=(me.getBorderWidth("lr")+me.getPadding("lr"));width=width<0?0:width;}
return width;},adjustHeight:function(height){var me=this;if(typeof height=="number"&&me.autoBoxAdjust&&!me.isBorderBox()){height-=(me.getBorderWidth("tb")+me.getPadding("tb"));height=height<0?0:height;}
return height;},addClass:function(className){var me=this;Ext.each(className,function(v){me.dom.className+=(!me.hasClass(v)&&v?" "+v:"");});return me;},radioClass:function(className){Ext.each(this.dom.parentNode.childNodes,function(v){if(v.nodeType==1){Ext.fly(v).removeClass(className);}});return this.addClass(className);},removeClass:function(className){var me=this;if(me.dom.className){Ext.each(className,function(v){me.dom.className=me.dom.className.replace(classReCache[v]=classReCache[v]||new RegExp('(?:^|\\s+)'+v+'(?:\\s+|$)',"g")," ");});}
return me;},toggleClass:function(className){return this.hasClass(className)?this.removeClass(className):this.addClass(className);},hasClass:function(className){return className&&(' '+this.dom.className+' ').indexOf(' '+className+' ')!=-1;},replaceClass:function(oldClassName,newClassName){return this.removeClass(oldClassName).addClass(newClassName);},isStyle:function(style,val){return this.getStyle(style)==val;},getStyle:function(){return view&&view.getComputedStyle?function(prop){var el=this.dom,v,cs;if(el==document)return null;prop=chkCache(prop);return(v=el.style[prop])?v:(cs=view.getComputedStyle(el,""))?cs[prop]:null;}:function(prop){var el=this.dom,m,cs;if(el==document)return null;if(prop=='opacity'){if(el.style.filter.match){if(m=el.style.filter.match(opacityRe)){var fv=parseFloat(m[1]);if(!isNaN(fv)){return fv?fv/100:0;}}}
return 1;}
prop=chkCache(prop);return el.style[prop]||((cs=el.currentStyle)?cs[prop]:null);};}(),getColor:function(attr,defaultValue,prefix){var h,v=this.getStyle(attr),color=prefix||"#";if(!v||v=="transparent"||v=="inherit"){return defaultValue;}
if(/^r/.test(v)){Ext.each(v.slice(4,v.length-1).split(","),function(s){h=(s*1).toString(16);color+=h<16?"0"+h:h;});}else{color+=v.replace("#","").replace(/^(\w)(\w)(\w)$/,"$1$1$2$2$3$3");}
return color.length>5?color.toLowerCase():defaultValue;},setStyle:function(prop,value){var tmp,style,camel;if(!Ext.isObject(prop)){tmp={};tmp[prop]=value;prop=tmp;}
for(style in prop){value=prop[style];style=='opacity'?this.setOpacity(value):this.dom.style[chkCache(style)]=value;}
return this;},setOpacity:function(opacity,animate){var me=this,s=me.dom.style;if(!animate||!me.anim){if(Ext.isIE){var opac=opacity<1?'alpha(opacity='+opacity*100+')':'',val=s.filter.replace(opacityRe,'').replace(trimRe,'');s.zoom=1;s.filter=val+(val.length>0?' ':'')+opac;}else{s.opacity=opacity;}}else{me.anim({opacity:{to:opacity}},me.preanim(arguments,1),null,.35,'easeIn');}
return me;},clearOpacity:function(){var style=this.dom.style;if(Ext.isIE){if(!Ext.isEmpty(style.filter)){style.filter=style.filter.replace(opacityRe,'').replace(trimRe,'');}}else{style.opacity=style['-moz-opacity']=style['-khtml-opacity']='';}
return this;},getHeight:function(contentHeight){var h=this.dom.offsetHeight||0;h=!contentHeight?h:h-this.getBorderWidth("tb")-this.getPadding("tb");return h<0?0:h;},getWidth:function(contentWidth){var w=this.dom.offsetWidth||0;w=!contentWidth?w:w-this.getBorderWidth("lr")-this.getPadding("lr");return w<0?0:w;},setWidth:function(width,animate){var me=this;width=me.adjustWidth(width);!animate||!me.anim?me.dom.style.width=me.addUnits(width):me.anim({width:{to:width}},me.preanim(arguments,1));return me;},setHeight:function(height,animate){var me=this;height=me.adjustHeight(height);!animate||!me.anim?me.dom.style.height=me.addUnits(height):me.anim({height:{to:height}},me.preanim(arguments,1));return me;},getBorderWidth:function(side){return addStyles.call(this,side,borders);},getPadding:function(side){return addStyles.call(this,side,paddings);},clip:function(){var me=this
dom=me.dom;if(!data(dom,'isClipped')){data(dom,'isClipped',true);data(dom,'originalClip,',{o:me.getStyle("overflow"),x:me.getStyle("overflow-x"),y:me.getStyle("overflow-y")});me.setStyle("overflow","hidden");me.setStyle("overflow-x","hidden");me.setStyle("overflow-y","hidden");}
return me;},unclip:function(){var me=this,dom=me.dom;if(data(dom,'isClipped')){data(dom,'isClipped',false);var o=data(dom,'originalClip');if(o.o){me.setStyle("overflow",o.o);}
if(o.x){me.setStyle("overflow-x",o.x);}
if(o.y){me.setStyle("overflow-y",o.y);}}
return me;},addStyles:addStyles,margins:margins}}());