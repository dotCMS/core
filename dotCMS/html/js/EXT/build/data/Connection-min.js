/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */


(function(){var BEFOREREQUEST="beforerequest",REQUESTCOMPLETE="requestcomplete",REQUESTEXCEPTION="requestexception",UNDEFINED=undefined,LOAD='load',POST='POST',GET='GET',WINDOW=window;Ext.data.Connection=function(config){Ext.apply(this,config);this.addEvents(BEFOREREQUEST,REQUESTCOMPLETE,REQUESTEXCEPTION);Ext.data.Connection.superclass.constructor.call(this);};function handleResponse(response){this.transId=false;var options=response.argument.options;response.argument=options?options.argument:null;this.fireEvent(REQUESTCOMPLETE,this,response,options);if(options.success)options.success.call(options.scope,response,options);if(options.callback)options.callback.call(options.scope,options,true,response);}
function handleFailure(response,e){this.transId=false;var options=response.argument.options;response.argument=options?options.argument:null;this.fireEvent(REQUESTEXCEPTION,this,response,options,e);if(options.failure)options.failure.call(options.scope,response,options);if(options.callback)options.callback.call(options.scope,options,false,response);}
function doFormUpload(o,ps,url){var id=Ext.id(),doc=document,frame=doc.createElement('iframe'),form=Ext.getDom(o.form),hiddens=[],hd;frame.id=frame.name=id;frame.className='x-hidden';frame.src=Ext.SSL_SECURE_URL;doc.body.appendChild(frame);if(Ext.isIE){doc.frames[id].name=id;}
form.target=id;form.method=POST;form.enctype=form.encoding='multipart/form-data';form.action=url||"";ps=Ext.urlDecode(ps,false);for(var k in ps){if(ps.hasOwnProperty(k)){hd=doc.createElement('input');hd.type='hidden';hd.value=ps[hd.name=k];form.appendChild(hd);hiddens.push(hd);}}
function cb(){var me=this,r={responseText:'',responseXML:null,argument:o.argument},doc,firstChild;try{doc=frame.contentWindow.document||frame.contentDocument||WINDOW.frames[id].document;if(doc){if(doc.body){if(/textarea/i.test((firstChild=doc.body.firstChild||{}).tagName)){r.responseText=firstChild.value;}else{r.responseText=doc.body.innerHTML;}}else{r.responseXML=doc.XMLDocument||doc;}}}
catch(e){}
Ext.EventManager.removeListener(frame,LOAD,cb,me);me.fireEvent(REQUESTCOMPLETE,me,r,o);Ext.callback(o.success,o.scope,[r,o]);Ext.callback(o.callback,o.scope,[o,true,r]);if(!me.debugUploads){setTimeout(function(){Ext.removeNode(frame);},100);}}
Ext.EventManager.on(frame,LOAD,cb,this);form.submit();Ext.each(hiddens,function(h){Ext.removeNode(h);});}
Ext.extend(Ext.data.Connection,Ext.util.Observable,{timeout:30000,autoAbort:false,disableCaching:true,disableCachingParam:'_dc',request:function(o){var me=this;if(me.fireEvent(BEFOREREQUEST,me,o)){if(o.el){if(!Ext.isEmpty(o.indicatorText)){me.indicatorText='<div class="loading-indicator">'+o.indicatorText+"</div>";}
if(me.indicatorText){Ext.getDom(o.el).innerHTML=me.indicatorText;}
o.success=(Ext.isFunction(o.success)?o.success:function(){}).createInterceptor(function(response){Ext.getDom(o.el).innerHTML=response.responseText;});}
var p=o.params,url=o.url||me.url,method,cb={success:handleResponse,failure:handleFailure,scope:me,argument:{options:o},timeout:o.timeout||me.timeout},form,serForm;if(Ext.isFunction(p)){p=p.call(o.scope||WINDOW,o);}
p=Ext.urlEncode(me.extraParams,typeof p=='object'?Ext.urlEncode(p):p);if(Ext.isFunction(url)){url=url.call(o.scope||WINDOW,o);}
if(form=Ext.getDom(o.form)){url=url||form.action;if(o.isUpload||/multipart\/form-data/i.test(form.getAttribute("enctype"))){return doFormUpload.call(me,o,p,url);}
serForm=Ext.lib.Ajax.serializeForm(form);p=p?(p+'&'+serForm):serForm;}
method=o.method||me.method||((p||o.xmlData||o.jsonData)?POST:GET);if(method===GET&&(me.disableCaching&&o.disableCaching!==false)||o.disableCaching===true){var dcp=o.disableCachingParam||me.disableCachingParam;url+=(url.indexOf('?')!=-1?'&':'?')+dcp+'='+(new Date().getTime());}
o.headers=Ext.apply(o.headers||{},me.defaultHeaders||{});if(o.autoAbort===true||me.autoAbort){me.abort();}
if((method==GET||o.xmlData||o.jsonData)&&p){url+=(/\?/.test(url)?'&':'?')+p;p='';}
return me.transId=Ext.lib.Ajax.request(method,url,cb,p,o);}else{return o.callback?o.callback.apply(o.scope,[o,UNDEFINED,UNDEFINED]):null;}},isLoading:function(transId){return transId?Ext.lib.Ajax.isCallInProgress(transId):!!this.transId;},abort:function(transId){if(transId||this.isLoading()){Ext.lib.Ajax.abort(transId||this.transId);}}});})();Ext.Ajax=new Ext.data.Connection({autoAbort:false,serializeForm:function(form){return Ext.lib.Ajax.serializeForm(form);}});