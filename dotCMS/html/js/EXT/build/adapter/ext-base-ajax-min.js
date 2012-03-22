/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


Ext.lib.Ajax=function(){var activeX=['MSXML2.XMLHTTP.3.0','MSXML2.XMLHTTP','Microsoft.XMLHTTP'];function setHeader(o){var conn=o.conn,prop;function setTheHeaders(conn,headers){for(prop in headers){if(headers.hasOwnProperty(prop)){conn.setRequestHeader(prop,headers[prop]);}}}
if(pub.defaultHeaders){setTheHeaders(conn,pub.defaultHeaders);}
if(pub.headers){setTheHeaders(conn,pub.headers);pub.headers=null;}}
function createExceptionObject(tId,callbackArg,isAbort,isTimeout){return{tId:tId,status:isAbort?-1:0,statusText:isAbort?'transaction aborted':'communication failure',isAbort:true,isTimeout:true,argument:callbackArg};}
function initHeader(label,value){(pub.headers=pub.headers||{})[label]=value;}
function createResponseObject(o,callbackArg){var headerObj={},headerStr,conn=o.conn;try{headerStr=o.conn.getAllResponseHeaders();Ext.each(headerStr.split('\n'),function(v){var t=v.indexOf(':');headerObj[v.substr(0,t)]=v.substr(t+1);});}catch(e){}
return{tId:o.tId,status:conn.status,statusText:conn.statusText,getResponseHeader:function(header){return headerObj[header];},getAllResponseHeaders:function(){return headerStr},responseText:conn.responseText,responseXML:conn.responseXML,argument:callbackArg};}
function releaseObject(o){o.conn=null;o=null;}
function handleTransactionResponse(o,callback,isAbort,isTimeout){if(!callback){releaseObject(o);return;}
var httpStatus,responseObject;try{if(o.conn.status!==undefined&&o.conn.status!=0){httpStatus=o.conn.status;}
else{httpStatus=13030;}}
catch(e){httpStatus=13030;}
if((httpStatus>=200&&httpStatus<300)||(Ext.isIE&&httpStatus==1223)){responseObject=createResponseObject(o,callback.argument);if(callback.success){if(!callback.scope){callback.success(responseObject);}
else{callback.success.apply(callback.scope,[responseObject]);}}}
else{switch(httpStatus){case 12002:case 12029:case 12030:case 12031:case 12152:case 13030:responseObject=createExceptionObject(o.tId,callback.argument,(isAbort?isAbort:false),isTimeout);if(callback.failure){if(!callback.scope){callback.failure(responseObject);}
else{callback.failure.apply(callback.scope,[responseObject]);}}
break;default:responseObject=createResponseObject(o,callback.argument);if(callback.failure){if(!callback.scope){callback.failure(responseObject);}
else{callback.failure.apply(callback.scope,[responseObject]);}}}}
releaseObject(o);responseObject=null;}
function handleReadyState(o,callback){callback=callback||{};var conn=o.conn,tId=o.tId,poll=pub.poll,cbTimeout=callback.timeout||null;if(cbTimeout){pub.timeout[tId]=setTimeout(function(){pub.abort(o,callback,true);},cbTimeout);}
poll[tId]=setInterval(function(){if(conn&&conn.readyState==4){clearInterval(poll[tId]);poll[tId]=null;if(cbTimeout){clearTimeout(pub.timeout[tId]);pub.timeout[tId]=null;}
handleTransactionResponse(o,callback);}},pub.pollInterval);}
function asyncRequest(method,uri,callback,postData){var o=getConnectionObject()||null;if(o){o.conn.open(method,uri,true);if(pub.useDefaultXhrHeader){initHeader('X-Requested-With',pub.defaultXhrHeader);}
if(postData&&pub.useDefaultHeader&&(!pub.headers||!pub.headers['Content-Type'])){initHeader('Content-Type',pub.defaultPostHeader);}
if(pub.defaultHeaders||pub.headers){setHeader(o);}
handleReadyState(o,callback);o.conn.send(postData||null);}
return o;}
function getConnectionObject(){var o;try{if(o=createXhrObject(pub.transactionId)){pub.transactionId++;}}catch(e){}finally{return o;}}
function createXhrObject(transactionId){var http;try{http=new XMLHttpRequest();}catch(e){for(var i=0;i<activeX.length;++i){try{http=new ActiveXObject(activeX[i]);break;}catch(e){}}}finally{return{conn:http,tId:transactionId};}}
var pub={request:function(method,uri,cb,data,options){if(options){var me=this,xmlData=options.xmlData,jsonData=options.jsonData;Ext.applyIf(me,options);if(xmlData||jsonData){initHeader('Content-Type',xmlData?'text/xml':'application/json');data=xmlData||(Ext.isObject(jsonData)?Ext.encode(jsonData):jsonData);}}
return asyncRequest(method||options.method||"POST",uri,cb,data);},serializeForm:function(form){var fElements=form.elements||(document.forms[form]||Ext.getDom(form)).elements,hasSubmit=false,encoder=encodeURIComponent,element,options,name,val,data='',type;Ext.each(fElements,function(element){name=element.name;type=element.type;if(!element.disabled&&name){if(/select-(one|multiple)/i.test(type)){Ext.each(element.options,function(opt){if(opt.selected){data+=String.format("{0}={1}&",encoder(name),(opt.hasAttribute?opt.hasAttribute('value'):opt.getAttribute('value')!==null)?opt.value:opt.text);}});}else if(!/file|undefined|reset|button/i.test(type)){if(!(/radio|checkbox/i.test(type)&&!element.checked)&&!(type=='submit'&&hasSubmit)){data+=encoder(name)+'='+encoder(element.value)+'&';hasSubmit=/submit/i.test(type);}}}});return data.substr(0,data.length-1);},useDefaultHeader:true,defaultPostHeader:'application/x-www-form-urlencoded; charset=UTF-8',useDefaultXhrHeader:true,defaultXhrHeader:'XMLHttpRequest',poll:{},timeout:{},pollInterval:50,transactionId:0,abort:function(o,callback,isTimeout){var me=this,tId=o.tId,isAbort=false;if(me.isCallInProgress(o)){o.conn.abort();clearInterval(me.poll[tId]);me.poll[tId]=null;if(isTimeout){me.timeout[tId]=null;}
handleTransactionResponse(o,callback,(isAbort=true),isTimeout);}
return isAbort;},isCallInProgress:function(o){return o.conn&&!{0:true,4:true}[o.conn.readyState];}};return pub;}();