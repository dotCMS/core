/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.DomHelper=function(){var tempTableEl=null,emptyTags=/^(?:br|frame|hr|img|input|link|meta|range|spacer|wbr|area|param|col)$/i,tableRe=/^table|tbody|tr|td$/i,pub,afterbegin="afterbegin",afterend="afterend",beforebegin="beforebegin",beforeend="beforeend",ts='<table>',te='</table>',tbs=ts+'<tbody>',tbe='</tbody>'+te,trs=tbs+'<tr>',tre='</tr>'+tbe;function doInsert(el,o,returnElement,pos,sibling,append){var newNode=pub.insertHtml(pos,Ext.getDom(el),createHtml(o));return returnElement?Ext.get(newNode,true):newNode;}
function createHtml(o){var b="",attr,val,key,keyVal,cn;if(typeof o=='string'){b=o;}else if(Ext.isArray(o)){Ext.each(o,function(v){b+=createHtml(v);});}else{b+="<"+(o.tag=o.tag||"div");for(attr in o){val=o[attr];if(!/tag|children|cn|html$/i.test(attr)&&!Ext.isFunction(val)){if(Ext.isObject(val)){b+=" "+attr+"='";for(key in val){keyVal=val[key];b+=!Ext.isFunction(keyVal)?key+":"+keyVal+";":"";}
b+="'";}else{b+=" "+({cls:"class",htmlFor:"for"}[attr]||attr)+"='"+val+"'";}}}
if(emptyTags.test(o.tag)){b+="/>";}else{b+=">";if(cn=o.children||o.cn){b+=createHtml(cn);}else if(o.html){b+=o.html;}
b+="</"+o.tag+">";}}
return b;};function ieTable(depth,s,h,e){tempTableEl.innerHTML=[s,h,e].join('');var i=-1,el=tempTableEl;while(++i<depth){el=el.firstChild;}
return el;};function insertIntoTable(tag,where,el,html){var node,before;tempTableEl=tempTableEl||document.createElement('div');if(tag=='td'&&(where==afterbegin||where==beforeend)||!/td|tr|tbody/i.test(tag)&&(where==beforebegin||where==afterend)){return;}
before=where==beforebegin?el:where==afterend?el.nextSibling:where==afterbegin?el.firstChild:null;if(where==beforebegin||where==afterend){el=el.parentNode;}
if(tag=='td'||(tag=="tr"&&(where==beforeend||where==afterbegin))){node=ieTable(4,trs,html,tre);}else if((tag=="tbody"&&(where==beforeend||where==afterbegin))||(tag=="tr"&&(where==beforebegin||where==afterend))){node=ieTable(3,tbs,html,tbe);}else{node=ieTable(2,ts,html,te);}
el.insertBefore(node,before);return node;};pub={markup:function(o){return createHtml(o);},insertHtml:function(where,el,html){var hash={},hashVal,setStart,range,frag,rangeEl,rs;where=where.toLowerCase();hash[beforebegin]=['BeforeBegin','previousSibling'];hash[afterend]=['AfterEnd','nextSibling'];if(el.insertAdjacentHTML){if(tableRe.test(el.tagName)&&(rs=insertIntoTable(el.tagName.toLowerCase(),where,el,html))){return rs;}
hash[afterbegin]=['AfterBegin','firstChild'];hash[beforeend]=['BeforeEnd','lastChild'];if(hashVal=hash[where]){el.insertAdjacentHTML(hashVal[0],html);return el[hashVal[1]];}}else{range=el.ownerDocument.createRange();setStart="setStart"+(/end/i.test(where)?"After":"Before");if(hash[where]){range[setStart](el);frag=range.createContextualFragment(html);el.parentNode.insertBefore(frag,where==beforebegin?el:el.nextSibling);return el[(where==beforebegin?"previous":"next")+"Sibling"];}else{rangeEl=(where==afterbegin?"first":"last")+"Child";if(el.firstChild){range[setStart](el[rangeEl]);frag=range.createContextualFragment(html);where==afterbegin?el.insertBefore(frag,el.firstChild):el.appendChild(frag);}else{el.innerHTML=html;}
return el[rangeEl];}}
throw'Illegal insertion point -> "'+where+'"';},insertBefore:function(el,o,returnElement){return doInsert(el,o,returnElement,beforebegin);},insertAfter:function(el,o,returnElement){return doInsert(el,o,returnElement,afterend,"nextSibling");},insertFirst:function(el,o,returnElement){return doInsert(el,o,returnElement,afterbegin,"firstChild");},append:function(el,o,returnElement){return doInsert(el,o,returnElement,beforeend,"",true);},overwrite:function(el,o,returnElement){el=Ext.getDom(el);el.innerHTML=createHtml(o);return returnElement?Ext.get(el.firstChild):el.firstChild;},createHtml:createHtml};return pub;}();