var __extends=this&&this.__extends||function(){var e=function(t,r){e=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var r in t)if(t.hasOwnProperty(r))e[r]=t[r]};return e(t,r)};return function(t,r){e(t,r);function n(){this.constructor=t}t.prototype=r===null?Object.create(r):(n.prototype=r.prototype,new n)}}();var __makeTemplateObject=this&&this.__makeTemplateObject||function(e,t){if(Object.defineProperty){Object.defineProperty(e,"raw",{value:t})}else{e.raw=t}return e};var __awaiter=this&&this.__awaiter||function(e,t,r,n){function i(e){return e instanceof r?e:new r((function(t){t(e)}))}return new(r||(r=Promise))((function(r,a){function o(e){try{u(n.next(e))}catch(t){a(t)}}function s(e){try{u(n["throw"](e))}catch(t){a(t)}}function u(e){e.done?r(e.value):i(e.value).then(o,s)}u((n=n.apply(e,t||[])).next())}))};var __generator=this&&this.__generator||function(e,t){var r={label:0,sent:function(){if(a[0]&1)throw a[1];return a[1]},trys:[],ops:[]},n,i,a,o;return o={next:s(0),throw:s(1),return:s(2)},typeof Symbol==="function"&&(o[Symbol.iterator]=function(){return this}),o;function s(e){return function(t){return u([e,t])}}function u(o){if(n)throw new TypeError("Generator is already executing.");while(r)try{if(n=1,i&&(a=o[0]&2?i["return"]:o[0]?i["throw"]||((a=i["return"])&&a.call(i),0):i.next)&&!(a=a.call(i,o[1])).done)return a;if(i=0,a)o=[o[0]&2,a.value];switch(o[0]){case 0:case 1:a=o;break;case 4:r.label++;return{value:o[1],done:false};case 5:r.label++;i=o[1];o=[0];continue;case 7:o=r.ops.pop();r.trys.pop();continue;default:if(!(a=r.trys,a=a.length>0&&a[a.length-1])&&(o[0]===6||o[0]===2)){r=0;continue}if(o[0]===3&&(!a||o[1]>a[0]&&o[1]<a[3])){r.label=o[1];break}if(o[0]===6&&r.label<a[1]){r.label=a[1];a=o;break}if(a&&r.label<a[2]){r.label=a[2];r.ops.push(o);break}if(a[2])r.ops.pop();r.trys.pop();continue}o=t.call(e,r)}catch(s){o=[6,s];i=0}finally{n=a=0}if(o[0]&5)throw o[1];return{value:o[0]?o[1]:void 0,done:true}}};var __spreadArrays=this&&this.__spreadArrays||function(){for(var e=0,t=0,r=arguments.length;t<r;t++)e+=arguments[t].length;for(var n=Array(e),i=0,t=0;t<r;t++)for(var a=arguments[t],o=0,s=a.length;o<s;o++,i++)n[i]=a[o];return n};System.register([],(function(e){"use strict";return{execute:function(){e({_:r,b:i,f:a,p:be,q:Se});
/*! *****************************************************************************
            Copyright (c) Microsoft Corporation. All rights reserved.
            Licensed under the Apache License, Version 2.0 (the "License"); you may not use
            this file except in compliance with the License. You may obtain a copy of the
            License at http://www.apache.org/licenses/LICENSE-2.0

            THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
            KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
            WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
            MERCHANTABLITY OR NON-INFRINGEMENT.

            See the Apache Version 2.0 License for specific language governing permissions
            and limitations under the License.
            ***************************************************************************** */var t=function(e,r){t=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var r in t)if(t.hasOwnProperty(r))e[r]=t[r]};return t(e,r)};function r(e,r){t(e,r);function n(){this.constructor=e}e.prototype=r===null?Object.create(r):(n.prototype=r.prototype,new n)}var n=e("a",(function(){n=e("a",Object.assign||function e(t){for(var r,n=1,i=arguments.length;n<i;n++){r=arguments[n];for(var a in r)if(Object.prototype.hasOwnProperty.call(r,a))t[a]=r[a]}return t});return n.apply(this,arguments)}));function i(e,t,r,n){var i=arguments.length,a=i<3?t:n===null?n=Object.getOwnPropertyDescriptor(t,r):n,o;if(typeof Reflect==="object"&&typeof Reflect.decorate==="function")a=Reflect.decorate(e,t,r,n);else for(var s=e.length-1;s>=0;s--)if(o=e[s])a=(i<3?o(a):i>3?o(t,r,a):o(t,r))||a;return i>3&&a&&Object.defineProperty(t,r,a),a}function a(e){var t=typeof Symbol==="function"&&e[Symbol.iterator],r=0;if(t)return t.call(e);return{next:function(){if(e&&r>=e.length)e=void 0;return{value:e&&e[r++],done:!e}}}}
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var o=new WeakMap;var s=e("e",(function(e){return function(){var t=[];for(var r=0;r<arguments.length;r++){t[r]=arguments[r]}var n=e.apply(void 0,t);o.set(n,true);return n}}));var u=function(e){return typeof e==="function"&&o.has(e)};
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var c=window.customElements!==undefined&&window.customElements.polyfillWrapFlushCallback!==undefined;var d=function(e,t,r){if(r===void 0){r=null}while(t!==r){var n=t.nextSibling;e.removeChild(t);t=n}};
/**
             * @license
             * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var l={};var f={};
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var p="{{lit-"+String(Math.random()).slice(2)+"}}";var h="\x3c!--"+p+"--\x3e";var v=new RegExp(p+"|"+h);var _="$lit$";var m=function(){function e(e,t){this.parts=[];this.element=t;var r=[];var n=[];var i=document.createTreeWalker(t.content,133,null,false);var a=0;var o=-1;var s=0;var u=e.strings,c=e.values.length;while(s<c){var d=i.nextNode();if(d===null){i.currentNode=n.pop();continue}o++;if(d.nodeType===1){if(d.hasAttributes()){var l=d.attributes;var f=l.length;var h=0;for(var m=0;m<f;m++){if(y(l[m].name,_)){h++}}while(h-- >0){var g=u[s];var w=S.exec(g)[2];var A=w.toLowerCase()+_;var C=d.getAttribute(A);d.removeAttribute(A);var T=C.split(v);this.parts.push({type:"attribute",index:o,name:w,strings:T});s+=T.length-1}}if(d.tagName==="TEMPLATE"){n.push(d);i.currentNode=d.content}}else if(d.nodeType===3){var P=d.data;if(P.indexOf(p)>=0){var E=d.parentNode;var x=P.split(v);var N=x.length-1;for(var m=0;m<N;m++){var O=void 0;var R=x[m];if(R===""){O=b()}else{var V=S.exec(R);if(V!==null&&y(V[2],_)){R=R.slice(0,V.index)+V[1]+V[2].slice(0,-_.length)+V[3]}O=document.createTextNode(R)}E.insertBefore(O,d);this.parts.push({type:"node",index:++o})}if(x[N]===""){E.insertBefore(b(),d);r.push(d)}else{d.data=x[N]}s+=N}}else if(d.nodeType===8){if(d.data===p){var E=d.parentNode;if(d.previousSibling===null||o===a){o++;E.insertBefore(b(),d)}a=o;this.parts.push({type:"node",index:o});if(d.nextSibling===null){d.data=""}else{r.push(d);o--}s++}else{var m=-1;while((m=d.data.indexOf(p,m+1))!==-1){this.parts.push({type:"node",index:-1});s++}}}}for(var I=0,k=r;I<k.length;I++){var H=k[I];H.parentNode.removeChild(H)}}return e}();var y=function(e,t){var r=e.length-t.length;return r>=0&&e.slice(r)===t};var g=function(e){return e.index!==-1};var b=function(){return document.createComment("")};var S=/([ \x09\x0a\x0c\x0d])([^\0-\x1F\x7F-\x9F "'>=/]+)([ \x09\x0a\x0c\x0d]*=[ \x09\x0a\x0c\x0d]*(?:[^ \x09\x0a\x0c\x0d"'`<>=]*|"[^"]*|'[^']*))$/;
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var w=function(){function e(e,t,r){this.__parts=[];this.template=e;this.processor=t;this.options=r}e.prototype.update=function(e){var t=0;for(var r=0,n=this.__parts;r<n.length;r++){var i=n[r];if(i!==undefined){i.setValue(e[t])}t++}for(var a=0,o=this.__parts;a<o.length;a++){var i=o[a];if(i!==undefined){i.commit()}}};e.prototype._clone=function(){var e;var t=c?this.template.element.content.cloneNode(true):document.importNode(this.template.element.content,true);var r=[];var n=this.template.parts;var i=document.createTreeWalker(t,133,null,false);var a=0;var o=0;var s;var u=i.nextNode();while(a<n.length){s=n[a];if(!g(s)){this.__parts.push(undefined);a++;continue}while(o<s.index){o++;if(u.nodeName==="TEMPLATE"){r.push(u);i.currentNode=u.content}if((u=i.nextNode())===null){i.currentNode=r.pop();u=i.nextNode()}}if(s.type==="node"){var d=this.processor.handleTextExpression(this.options);d.insertAfterNode(u.previousSibling);this.__parts.push(d)}else{(e=this.__parts).push.apply(e,this.processor.handleAttributeExpressions(u,s.name,s.strings,this.options))}a++}if(c){document.adoptNode(t);customElements.upgrade(t)}return t};return e}();
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var A=" "+p+" ";var C=function(){function e(e,t,r,n){this.strings=e;this.values=t;this.type=r;this.processor=n}e.prototype.getHTML=function(){var e=this.strings.length-1;var t="";var r=false;for(var n=0;n<e;n++){var i=this.strings[n];var a=i.lastIndexOf("\x3c!--");r=(a>-1||r)&&i.indexOf("--\x3e",a+1)===-1;var o=S.exec(i);if(o===null){t+=i+(r?A:h)}else{t+=i.substr(0,o.index)+o[1]+o[2]+_+o[3]+p}}t+=this.strings[e];return t};e.prototype.getTemplateElement=function(){var e=document.createElement("template");e.innerHTML=this.getHTML();return e};return e}();
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var T=function(e){return e===null||!(typeof e==="object"||typeof e==="function")};var P=function(e){return Array.isArray(e)||!!(e&&e[Symbol.iterator])};var E=function(){function e(e,t,r){this.dirty=true;this.element=e;this.name=t;this.strings=r;this.parts=[];for(var n=0;n<r.length-1;n++){this.parts[n]=this._createPart()}}e.prototype._createPart=function(){return new x(this)};e.prototype._getValue=function(){var e=this.strings;var t=e.length-1;var r="";for(var n=0;n<t;n++){r+=e[n];var i=this.parts[n];if(i!==undefined){var a=i.value;if(T(a)||!P(a)){r+=typeof a==="string"?a:String(a)}else{for(var o=0,s=a;o<s.length;o++){var u=s[o];r+=typeof u==="string"?u:String(u)}}}}r+=e[t];return r};e.prototype.commit=function(){if(this.dirty){this.dirty=false;this.element.setAttribute(this.name,this._getValue())}};return e}();var x=function(){function e(e){this.value=undefined;this.committer=e}e.prototype.setValue=function(e){if(e!==l&&(!T(e)||e!==this.value)){this.value=e;if(!u(e)){this.committer.dirty=true}}};e.prototype.commit=function(){while(u(this.value)){var e=this.value;this.value=l;e(this)}if(this.value===l){return}this.committer.commit()};return e}();e("A",x);var N=function(){function e(e){this.value=undefined;this.__pendingValue=undefined;this.options=e}e.prototype.appendInto=function(e){this.startNode=e.appendChild(b());this.endNode=e.appendChild(b())};e.prototype.insertAfterNode=function(e){this.startNode=e;this.endNode=e.nextSibling};e.prototype.appendIntoPart=function(e){e.__insert(this.startNode=b());e.__insert(this.endNode=b())};e.prototype.insertAfterPart=function(e){e.__insert(this.startNode=b());this.endNode=e.endNode;e.endNode=this.startNode};e.prototype.setValue=function(e){this.__pendingValue=e};e.prototype.commit=function(){while(u(this.__pendingValue)){var e=this.__pendingValue;this.__pendingValue=l;e(this)}var t=this.__pendingValue;if(t===l){return}if(T(t)){if(t!==this.value){this.__commitText(t)}}else if(t instanceof C){this.__commitTemplateResult(t)}else if(t instanceof Node){this.__commitNode(t)}else if(P(t)){this.__commitIterable(t)}else if(t===f){this.value=f;this.clear()}else{this.__commitText(t)}};e.prototype.__insert=function(e){this.endNode.parentNode.insertBefore(e,this.endNode)};e.prototype.__commitNode=function(e){if(this.value===e){return}this.clear();this.__insert(e);this.value=e};e.prototype.__commitText=function(e){var t=this.startNode.nextSibling;e=e==null?"":e;var r=typeof e==="string"?e:String(e);if(t===this.endNode.previousSibling&&t.nodeType===3){t.data=r}else{this.__commitNode(document.createTextNode(r))}this.value=e};e.prototype.__commitTemplateResult=function(e){var t=this.options.templateFactory(e);if(this.value instanceof w&&this.value.template===t){this.value.update(e.values)}else{var r=new w(t,e.processor,this.options);var n=r._clone();r.update(e.values);this.__commitNode(n);this.value=r}};e.prototype.__commitIterable=function(t){if(!Array.isArray(this.value)){this.value=[];this.clear()}var r=this.value;var n=0;var i;for(var a=0,o=t;a<o.length;a++){var s=o[a];i=r[n];if(i===undefined){i=new e(this.options);r.push(i);if(n===0){i.appendIntoPart(this)}else{i.insertAfterPart(r[n-1])}}i.setValue(s);i.commit();n++}if(n<r.length){r.length=n;this.clear(i&&i.endNode)}};e.prototype.clear=function(e){if(e===void 0){e=this.startNode}d(this.startNode.parentNode,e.nextSibling,this.endNode)};return e}();var O=function(){function e(e,t,r){this.value=undefined;this.__pendingValue=undefined;if(r.length!==2||r[0]!==""||r[1]!==""){throw new Error("Boolean attributes can only contain a single expression")}this.element=e;this.name=t;this.strings=r}e.prototype.setValue=function(e){this.__pendingValue=e};e.prototype.commit=function(){while(u(this.__pendingValue)){var e=this.__pendingValue;this.__pendingValue=l;e(this)}if(this.__pendingValue===l){return}var t=!!this.__pendingValue;if(this.value!==t){if(t){this.element.setAttribute(this.name,"")}else{this.element.removeAttribute(this.name)}this.value=t}this.__pendingValue=l};return e}();var R=function(e){r(t,e);function t(t,r,n){var i=e.call(this,t,r,n)||this;i.single=n.length===2&&n[0]===""&&n[1]==="";return i}t.prototype._createPart=function(){return new V(this)};t.prototype._getValue=function(){if(this.single){return this.parts[0].value}return e.prototype._getValue.call(this)};t.prototype.commit=function(){if(this.dirty){this.dirty=false;this.element[this.name]=this._getValue()}};return t}(E);var V=function(e){r(t,e);function t(){return e!==null&&e.apply(this,arguments)||this}return t}(x);e("P",V);var I=false;try{var k={get capture(){I=true;return false}};window.addEventListener("test",k,k);window.removeEventListener("test",k,k)}catch(et){}var H=function(){function e(e,t,r){var n=this;this.value=undefined;this.__pendingValue=undefined;this.element=e;this.eventName=t;this.eventContext=r;this.__boundHandleEvent=function(e){return n.handleEvent(e)}}e.prototype.setValue=function(e){this.__pendingValue=e};e.prototype.commit=function(){while(u(this.__pendingValue)){var e=this.__pendingValue;this.__pendingValue=l;e(this)}if(this.__pendingValue===l){return}var t=this.__pendingValue;var r=this.value;var n=t==null||r!=null&&(t.capture!==r.capture||t.once!==r.once||t.passive!==r.passive);var i=t!=null&&(r==null||n);if(n){this.element.removeEventListener(this.eventName,this.__boundHandleEvent,this.__options)}if(i){this.__options=D(t);this.element.addEventListener(this.eventName,this.__boundHandleEvent,this.__options)}this.value=t;this.__pendingValue=l};e.prototype.handleEvent=function(e){if(typeof this.value==="function"){this.value.call(this.eventContext||this.element,e)}else{this.value.handleEvent(e)}};return e}();var D=function(e){return e&&(I?{capture:e.capture,passive:e.passive,once:e.once}:e.capture)};
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var U=function(){function e(){}e.prototype.handleAttributeExpressions=function(e,t,r,n){var i=t[0];if(i==="."){var a=new R(e,t.slice(1),r);return a.parts}if(i==="@"){return[new H(e,t.slice(1),n.eventContext)]}if(i==="?"){return[new O(e,t.slice(1),r)]}var o=new E(e,t,r);return o.parts};e.prototype.handleTextExpression=function(e){return new N(e)};return e}();var F=new U;
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */function M(e){var t=j.get(e.type);if(t===undefined){t={stringsArray:new WeakMap,keyString:new Map};j.set(e.type,t)}var r=t.stringsArray.get(e.strings);if(r!==undefined){return r}var n=e.strings.join(p);r=t.keyString.get(n);if(r===undefined){r=new m(e,e.getTemplateElement());t.keyString.set(n,r)}t.stringsArray.set(e.strings,r);return r}var j=new Map;
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var L=new WeakMap;var z=function(e,t,r){var n=L.get(t);if(n===undefined){d(t,t.firstChild);L.set(t,n=new N(Object.assign({templateFactory:M},r)));n.appendInto(t)}n.setValue(e);n.commit()};
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */(window["litHtmlVersions"]||(window["litHtmlVersions"]=[])).push("1.1.2");var B=e("h",(function(e){var t=[];for(var r=1;r<arguments.length;r++){t[r-1]=arguments[r]}return new C(e,t,"html",F)}));
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var q=133;function G(e,t){var r=e.element.content,n=e.parts;var i=document.createTreeWalker(r,q,null,false);var a=X(n);var o=n[a];var s=-1;var u=0;var c=[];var d=null;while(i.nextNode()){s++;var l=i.currentNode;if(l.previousSibling===d){d=null}if(t.has(l)){c.push(l);if(d===null){d=l}}if(d!==null){u++}while(o!==undefined&&o.index===s){o.index=d!==null?-1:o.index-u;a=X(n,a);o=n[a]}}c.forEach((function(e){return e.parentNode.removeChild(e)}))}var W=function(e){var t=e.nodeType===11?0:1;var r=document.createTreeWalker(e,q,null,false);while(r.nextNode()){t++}return t};var X=function(e,t){if(t===void 0){t=-1}for(var r=t+1;r<e.length;r++){var n=e[r];if(g(n)){return r}}return-1};function J(e,t,r){if(r===void 0){r=null}var n=e.element.content,i=e.parts;if(r===null||r===undefined){n.appendChild(t);return}var a=document.createTreeWalker(n,q,null,false);var o=X(i);var s=0;var u=-1;while(a.nextNode()){u++;var c=a.currentNode;if(c===r){s=W(t);r.parentNode.insertBefore(t,r)}while(o!==-1&&i[o].index===u){if(s>0){while(o!==-1){i[o].index+=s;o=X(i,o)}return}o=X(i,o)}}}
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var Y=function(e,t){return e+"--"+t};var $=true;if(typeof window.ShadyCSS==="undefined"){$=false}else if(typeof window.ShadyCSS.prepareTemplateDom==="undefined"){console.warn("Incompatible ShadyCSS version detected. "+"Please update to at least @webcomponents/webcomponentsjs@2.0.2 and "+"@webcomponents/shadycss@1.3.1.");$=false}var Z=function(e){return function(t){var r=Y(t.type,e);var n=j.get(r);if(n===undefined){n={stringsArray:new WeakMap,keyString:new Map};j.set(r,n)}var i=n.stringsArray.get(t.strings);if(i!==undefined){return i}var a=t.strings.join(p);i=n.keyString.get(a);if(i===undefined){var o=t.getTemplateElement();if($){window.ShadyCSS.prepareTemplateDom(o,e)}i=new m(t,o);n.keyString.set(a,i)}n.stringsArray.set(t.strings,i);return i}};var K=["html","svg"];var Q=function(e){K.forEach((function(t){var r=j.get(Y(t,e));if(r!==undefined){r.keyString.forEach((function(e){var t=e.element.content;var r=new Set;Array.from(t.querySelectorAll("style")).forEach((function(e){r.add(e)}));G(e,r)}))}}))};var ee=new Set;var te=function(e,t,r){ee.add(e);var n=!!r?r.element:document.createElement("template");var i=t.querySelectorAll("style");var a=i.length;if(a===0){window.ShadyCSS.prepareTemplateStyles(n,e);return}var o=document.createElement("style");for(var s=0;s<a;s++){var u=i[s];u.parentNode.removeChild(u);o.textContent+=u.textContent}Q(e);var c=n.content;if(!!r){J(r,o,c.firstChild)}else{c.insertBefore(o,c.firstChild)}window.ShadyCSS.prepareTemplateStyles(n,e);var d=c.querySelector("style");if(window.ShadyCSS.nativeShadow&&d!==null){t.insertBefore(d.cloneNode(true),t.firstChild)}else if(!!r){c.insertBefore(o,c.firstChild);var l=new Set;l.add(o);G(r,l)}};var re=function(e,t,r){if(!r||typeof r!=="object"||!r.scopeName){throw new Error("The `scopeName` option is required.")}var n=r.scopeName;var i=L.has(t);var a=$&&t.nodeType===11&&!!t.host;var o=a&&!ee.has(n);var s=o?document.createDocumentFragment():t;z(e,s,Object.assign({templateFactory:Z(n)},r));if(o){var u=L.get(s);L.delete(s);var c=u.value instanceof w?u.value.template:undefined;te(n,s,c);d(t,t.firstChild);t.appendChild(s);L.set(t,u)}if(!i&&a){window.ShadyCSS.styleElement(t.host)}};
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var ne;window.JSCompiler_renameProperty=function(e,t){return e};var ie={toAttribute:function(e,t){switch(t){case Boolean:return e?"":null;case Object:case Array:return e==null?e:JSON.stringify(e)}return e},fromAttribute:function(e,t){switch(t){case Boolean:return e!==null;case Number:return e===null?null:Number(e);case Object:case Array:return JSON.parse(e)}return e}};var ae=function(e,t){return t!==e&&(t===t||e===e)};var oe={attribute:true,type:String,converter:ie,reflect:false,hasChanged:ae};var se=Promise.resolve(true);var ue=1;var ce=1<<2;var de=1<<3;var le=1<<4;var fe=1<<5;var pe="finalized";var he=function(e){r(t,e);function t(){var t=e.call(this)||this;t._updateState=0;t._instanceProperties=undefined;t._updatePromise=se;t._hasConnectedResolver=undefined;t._changedProperties=new Map;t._reflectingProperties=undefined;t.initialize();return t}Object.defineProperty(t,"observedAttributes",{get:function(){var e=this;this.finalize();var t=[];this._classProperties.forEach((function(r,n){var i=e._attributeNameForProperty(n,r);if(i!==undefined){e._attributeToPropertyMap.set(i,n);t.push(i)}}));return t},enumerable:true,configurable:true});t._ensureClassProperties=function(){var e=this;if(!this.hasOwnProperty(JSCompiler_renameProperty("_classProperties",this))){this._classProperties=new Map;var t=Object.getPrototypeOf(this)._classProperties;if(t!==undefined){t.forEach((function(t,r){return e._classProperties.set(r,t)}))}}};t.createProperty=function(e,t){if(t===void 0){t=oe}this._ensureClassProperties();this._classProperties.set(e,t);if(t.noAccessor||this.prototype.hasOwnProperty(e)){return}var r=typeof e==="symbol"?Symbol():"__"+e;Object.defineProperty(this.prototype,e,{get:function(){return this[r]},set:function(t){var n=this[e];this[r]=t;this._requestUpdate(e,n)},configurable:true,enumerable:true})};t.finalize=function(){var e=Object.getPrototypeOf(this);if(!e.hasOwnProperty(pe)){e.finalize()}this[pe]=true;this._ensureClassProperties();this._attributeToPropertyMap=new Map;if(this.hasOwnProperty(JSCompiler_renameProperty("properties",this))){var t=this.properties;var r=__spreadArrays(Object.getOwnPropertyNames(t),typeof Object.getOwnPropertySymbols==="function"?Object.getOwnPropertySymbols(t):[]);for(var n=0,i=r;n<i.length;n++){var a=i[n];this.createProperty(a,t[a])}}};t._attributeNameForProperty=function(e,t){var r=t.attribute;return r===false?undefined:typeof r==="string"?r:typeof e==="string"?e.toLowerCase():undefined};t._valueHasChanged=function(e,t,r){if(r===void 0){r=ae}return r(e,t)};t._propertyValueFromAttribute=function(e,t){var r=t.type;var n=t.converter||ie;var i=typeof n==="function"?n:n.fromAttribute;return i?i(e,r):e};t._propertyValueToAttribute=function(e,t){if(t.reflect===undefined){return}var r=t.type;var n=t.converter;var i=n&&n.toAttribute||ie.toAttribute;return i(e,r)};t.prototype.initialize=function(){this._saveInstanceProperties();this._requestUpdate()};t.prototype._saveInstanceProperties=function(){var e=this;this.constructor._classProperties.forEach((function(t,r){if(e.hasOwnProperty(r)){var n=e[r];delete e[r];if(!e._instanceProperties){e._instanceProperties=new Map}e._instanceProperties.set(r,n)}}))};t.prototype._applyInstanceProperties=function(){var e=this;this._instanceProperties.forEach((function(t,r){return e[r]=t}));this._instanceProperties=undefined};t.prototype.connectedCallback=function(){this._updateState=this._updateState|fe;if(this._hasConnectedResolver){this._hasConnectedResolver();this._hasConnectedResolver=undefined}};t.prototype.disconnectedCallback=function(){};t.prototype.attributeChangedCallback=function(e,t,r){if(t!==r){this._attributeToProperty(e,r)}};t.prototype._propertyToAttribute=function(e,t,r){if(r===void 0){r=oe}var n=this.constructor;var i=n._attributeNameForProperty(e,r);if(i!==undefined){var a=n._propertyValueToAttribute(t,r);if(a===undefined){return}this._updateState=this._updateState|de;if(a==null){this.removeAttribute(i)}else{this.setAttribute(i,a)}this._updateState=this._updateState&~de}};t.prototype._attributeToProperty=function(e,t){if(this._updateState&de){return}var r=this.constructor;var n=r._attributeToPropertyMap.get(e);if(n!==undefined){var i=r._classProperties.get(n)||oe;this._updateState=this._updateState|le;this[n]=r._propertyValueFromAttribute(t,i);this._updateState=this._updateState&~le}};t.prototype._requestUpdate=function(e,t){var r=true;if(e!==undefined){var n=this.constructor;var i=n._classProperties.get(e)||oe;if(n._valueHasChanged(this[e],t,i.hasChanged)){if(!this._changedProperties.has(e)){this._changedProperties.set(e,t)}if(i.reflect===true&&!(this._updateState&le)){if(this._reflectingProperties===undefined){this._reflectingProperties=new Map}this._reflectingProperties.set(e,i)}}else{r=false}}if(!this._hasRequestedUpdate&&r){this._enqueueUpdate()}};t.prototype.requestUpdate=function(e,t){this._requestUpdate(e,t);return this.updateComplete};t.prototype._enqueueUpdate=function(){return __awaiter(this,void 0,void 0,(function(){var e,t,r,n,i,a;var o=this;return __generator(this,(function(s){switch(s.label){case 0:this._updateState=this._updateState|ce;r=this._updatePromise;this._updatePromise=new Promise((function(r,n){e=r;t=n}));s.label=1;case 1:s.trys.push([1,3,,4]);return[4,r];case 2:s.sent();return[3,4];case 3:n=s.sent();return[3,4];case 4:if(!!this._hasConnected)return[3,6];return[4,new Promise((function(e){return o._hasConnectedResolver=e}))];case 5:s.sent();s.label=6;case 6:s.trys.push([6,9,,10]);i=this.performUpdate();if(!(i!=null))return[3,8];return[4,i];case 7:s.sent();s.label=8;case 8:return[3,10];case 9:a=s.sent();t(a);return[3,10];case 10:e(!this._hasRequestedUpdate);return[2]}}))}))};Object.defineProperty(t.prototype,"_hasConnected",{get:function(){return this._updateState&fe},enumerable:true,configurable:true});Object.defineProperty(t.prototype,"_hasRequestedUpdate",{get:function(){return this._updateState&ce},enumerable:true,configurable:true});Object.defineProperty(t.prototype,"hasUpdated",{get:function(){return this._updateState&ue},enumerable:true,configurable:true});t.prototype.performUpdate=function(){if(this._instanceProperties){this._applyInstanceProperties()}var e=false;var t=this._changedProperties;try{e=this.shouldUpdate(t);if(e){this.update(t)}}catch(r){e=false;throw r}finally{this._markUpdated()}if(e){if(!(this._updateState&ue)){this._updateState=this._updateState|ue;this.firstUpdated(t)}this.updated(t)}};t.prototype._markUpdated=function(){this._changedProperties=new Map;this._updateState=this._updateState&~ce};Object.defineProperty(t.prototype,"updateComplete",{get:function(){return this._getUpdateComplete()},enumerable:true,configurable:true});t.prototype._getUpdateComplete=function(){return this._updatePromise};t.prototype.shouldUpdate=function(e){return true};t.prototype.update=function(e){var t=this;if(this._reflectingProperties!==undefined&&this._reflectingProperties.size>0){this._reflectingProperties.forEach((function(e,r){return t._propertyToAttribute(r,t[r],e)}));this._reflectingProperties=undefined}};t.prototype.updated=function(e){};t.prototype.firstUpdated=function(e){};return t}(HTMLElement);ne=pe;he[ne]=true;
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */var ve=function(e,t){window.customElements.define(e,t);return t};var _e=function(e,t){var r=t.kind,n=t.elements;return{kind:r,elements:n,finisher:function(t){window.customElements.define(e,t)}}};var me=e("d",(function(e){return function(t){return typeof t==="function"?ve(e,t):_e(e,t)}}));var ye=function(e,t){if(t.kind==="method"&&t.descriptor&&!("value"in t.descriptor)){return Object.assign({},t,{finisher:function(r){r.createProperty(t.key,e)}})}else{return{kind:"field",key:Symbol(),placement:"own",descriptor:{},initializer:function(){if(typeof t.initializer==="function"){this[t.key]=t.initializer.call(this)}},finisher:function(r){r.createProperty(t.key,e)}}}};var ge=function(e,t,r){t.constructor.createProperty(r,e)};function be(e){return function(t,r){return r!==undefined?ge(e,t,r):ye(e,t)}}function Se(e){return function(t,r){var n={get:function(){return this.renderRoot.querySelector(e)},enumerable:true,configurable:true};return r!==undefined?we(n,t,r):Ae(n,t)}}var we=function(e,t,r){Object.defineProperty(t,r,e)};var Ae=function(e,t){return{kind:"method",placement:"prototype",key:t.key,descriptor:e}};
/**
            @license
            Copyright (c) 2019 The Polymer Project Authors. All rights reserved.
            This code may only be used under the BSD style license found at
            http://polymer.github.io/LICENSE.txt The complete set of authors may be found at
            http://polymer.github.io/AUTHORS.txt The complete set of contributors may be
            found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by Google as
            part of the polymer project is also subject to an additional IP rights grant
            found at http://polymer.github.io/PATENTS.txt
            */var Ce="adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype;var Te=Symbol();var Pe=function(){function e(e,t){if(t!==Te){throw new Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.")}this.cssText=e}Object.defineProperty(e.prototype,"styleSheet",{get:function(){if(this._styleSheet===undefined){if(Ce){this._styleSheet=new CSSStyleSheet;this._styleSheet.replaceSync(this.cssText)}else{this._styleSheet=null}}return this._styleSheet},enumerable:true,configurable:true});e.prototype.toString=function(){return this.cssText};return e}();var Ee=function(e){if(e instanceof Pe){return e.cssText}else if(typeof e==="number"){return e}else{throw new Error("Value passed to 'css' function must be a 'css' function result: "+e+". Use 'unsafeCSS' to pass non-literal values, but\n            take care to ensure page security.")}};var xe=e("c",(function(e){var t=[];for(var r=1;r<arguments.length;r++){t[r-1]=arguments[r]}var n=t.reduce((function(t,r,n){return t+Ee(r)+e[n+1]}),e[0]);return new Pe(n,Te)}));
/**
             * @license
             * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
             * This code may only be used under the BSD style license found at
             * http://polymer.github.io/LICENSE.txt
             * The complete set of authors may be found at
             * http://polymer.github.io/AUTHORS.txt
             * The complete set of contributors may be found at
             * http://polymer.github.io/CONTRIBUTORS.txt
             * Code distributed by Google as part of the polymer project is also
             * subject to an additional IP rights grant found at
             * http://polymer.github.io/PATENTS.txt
             */(window["litElementVersions"]||(window["litElementVersions"]=[])).push("2.2.1");function Ne(e,t){if(t===void 0){t=[]}for(var r=0,n=e.length;r<n;r++){var i=e[r];if(Array.isArray(i)){Ne(i,t)}else{t.push(i)}}return t}var Oe=function(e){return e.flat?e.flat(Infinity):Ne(e)};var Re=function(e){r(t,e);function t(){return e!==null&&e.apply(this,arguments)||this}t.finalize=function(){e.finalize.call(this);this._styles=this.hasOwnProperty(JSCompiler_renameProperty("styles",this))?this._getUniqueStyles():this._styles||[]};t._getUniqueStyles=function(){var e=this.styles;var t=[];if(Array.isArray(e)){var r=Oe(e);var n=r.reduceRight((function(e,t){e.add(t);return e}),new Set);n.forEach((function(e){return t.unshift(e)}))}else if(e){t.push(e)}return t};t.prototype.initialize=function(){e.prototype.initialize.call(this);this.renderRoot=this.createRenderRoot();if(window.ShadowRoot&&this.renderRoot instanceof window.ShadowRoot){this.adoptStyles()}};t.prototype.createRenderRoot=function(){return this.attachShadow({mode:"open"})};t.prototype.adoptStyles=function(){var e=this.constructor._styles;if(e.length===0){return}if(window.ShadyCSS!==undefined&&!window.ShadyCSS.nativeShadow){window.ShadyCSS.ScopingShim.prepareAdoptedCssText(e.map((function(e){return e.cssText})),this.localName)}else if(Ce){this.renderRoot.adoptedStyleSheets=e.map((function(e){return e.styleSheet}))}else{this._needsShimAdoptedStyleSheets=true}};t.prototype.connectedCallback=function(){e.prototype.connectedCallback.call(this);if(this.hasUpdated&&window.ShadyCSS!==undefined){window.ShadyCSS.styleElement(this)}};t.prototype.update=function(t){var r=this;e.prototype.update.call(this,t);var n=this.render();if(n instanceof C){this.constructor.render(n,this.renderRoot,{scopeName:this.localName,eventContext:this})}if(this._needsShimAdoptedStyleSheets){this._needsShimAdoptedStyleSheets=false;this.constructor._styles.forEach((function(e){var t=document.createElement("style");t.textContent=e.cssText;r.renderRoot.appendChild(t)}))}};t.prototype.render=function(){};return t}(he);e("L",Re);Re["finalized"]=true;Re.render=re;
/**
             * @license
             * Copyright 2019 Google Inc.
             *
             * Permission is hereby granted, free of charge, to any person obtaining a copy
             * of this software and associated documentation files (the "Software"), to deal
             * in the Software without restriction, including without limitation the rights
             * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
             * copies of the Software, and to permit persons to whom the Software is
             * furnished to do so, subject to the following conditions:
             *
             * The above copyright notice and this permission notice shall be included in
             * all copies or substantial portions of the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
             * THE SOFTWARE.
             */function Ve(e){if(e===void 0){e=window}return Ie(e)?{passive:true}:false}function Ie(e){if(e===void 0){e=window}var t=false;try{var r={get passive(){t=true;return false}};var n=function(){};e.document.addEventListener("test",n,r);e.document.removeEventListener("test",n,r)}catch(i){t=false}return t}
/**
             * @license
             * Copyright 2018 Google Inc.
             *
             * Permission is hereby granted, free of charge, to any person obtaining a copy
             * of this software and associated documentation files (the "Software"), to deal
             * in the Software without restriction, including without limitation the rights
             * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
             * copies of the Software, and to permit persons to whom the Software is
             * furnished to do so, subject to the following conditions:
             *
             * The above copyright notice and this permission notice shall be included in
             * all copies or substantial portions of the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
             * THE SOFTWARE.
             */function ke(e,t){var r=e.matches||e.webkitMatchesSelector||e.msMatchesSelector;return r.call(e,t)}
/**
             * @license
             * Copyright 2016 Google Inc.
             *
             * Permission is hereby granted, free of charge, to any person obtaining a copy
             * of this software and associated documentation files (the "Software"), to deal
             * in the Software without restriction, including without limitation the rights
             * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
             * copies of the Software, and to permit persons to whom the Software is
             * furnished to do so, subject to the following conditions:
             *
             * The above copyright notice and this permission notice shall be included in
             * all copies or substantial portions of the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
             * THE SOFTWARE.
             */var He=function(){function e(e){if(e===void 0){e={}}this.adapter_=e}Object.defineProperty(e,"cssClasses",{get:function(){return{}},enumerable:true,configurable:true});Object.defineProperty(e,"strings",{get:function(){return{}},enumerable:true,configurable:true});Object.defineProperty(e,"numbers",{get:function(){return{}},enumerable:true,configurable:true});Object.defineProperty(e,"defaultAdapter",{get:function(){return{}},enumerable:true,configurable:true});e.prototype.init=function(){};e.prototype.destroy=function(){};return e}();
/**
             * @license
             * Copyright 2016 Google Inc.
             *
             * Permission is hereby granted, free of charge, to any person obtaining a copy
             * of this software and associated documentation files (the "Software"), to deal
             * in the Software without restriction, including without limitation the rights
             * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
             * copies of the Software, and to permit persons to whom the Software is
             * furnished to do so, subject to the following conditions:
             *
             * The above copyright notice and this permission notice shall be included in
             * all copies or substantial portions of the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
             * THE SOFTWARE.
             */var De={BG_FOCUSED:"mdc-ripple-upgraded--background-focused",FG_ACTIVATION:"mdc-ripple-upgraded--foreground-activation",FG_DEACTIVATION:"mdc-ripple-upgraded--foreground-deactivation",ROOT:"mdc-ripple-upgraded",UNBOUNDED:"mdc-ripple-upgraded--unbounded"};var Ue={VAR_FG_SCALE:"--mdc-ripple-fg-scale",VAR_FG_SIZE:"--mdc-ripple-fg-size",VAR_FG_TRANSLATE_END:"--mdc-ripple-fg-translate-end",VAR_FG_TRANSLATE_START:"--mdc-ripple-fg-translate-start",VAR_LEFT:"--mdc-ripple-left",VAR_TOP:"--mdc-ripple-top"};var Fe={DEACTIVATION_TIMEOUT_MS:225,FG_DEACTIVATION_MS:150,INITIAL_ORIGIN_SCALE:.6,PADDING:10,TAP_DELAY_MS:300};var Me;function je(e,t){if(t===void 0){t=false}var r=e.CSS;var n=Me;if(typeof Me==="boolean"&&!t){return Me}var i=r&&typeof r.supports==="function";if(!i){return false}var a=r.supports("--css-vars","yes");var o=r.supports("(--css-vars: yes)")&&r.supports("color","#00000000");n=a||o;if(!t){Me=n}return n}function Le(e,t,r){if(!e){return{x:0,y:0}}var n=t.x,i=t.y;var a=n+r.left;var o=i+r.top;var s;var u;if(e.type==="touchstart"){var c=e;s=c.changedTouches[0].pageX-a;u=c.changedTouches[0].pageY-o}else{var d=e;s=d.pageX-a;u=d.pageY-o}return{x:s,y:u}}
/**
             * @license
             * Copyright 2016 Google Inc.
             *
             * Permission is hereby granted, free of charge, to any person obtaining a copy
             * of this software and associated documentation files (the "Software"), to deal
             * in the Software without restriction, including without limitation the rights
             * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
             * copies of the Software, and to permit persons to whom the Software is
             * furnished to do so, subject to the following conditions:
             *
             * The above copyright notice and this permission notice shall be included in
             * all copies or substantial portions of the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
             * THE SOFTWARE.
             */var ze=["touchstart","pointerdown","mousedown","keydown"];var Be=["touchend","pointerup","mouseup","contextmenu"];var qe=[];var Ge=function(e){r(t,e);function t(r){var i=e.call(this,n({},t.defaultAdapter,r))||this;i.activationAnimationHasEnded_=false;i.activationTimer_=0;i.fgDeactivationRemovalTimer_=0;i.fgScale_="0";i.frame_={width:0,height:0};i.initialSize_=0;i.layoutFrame_=0;i.maxRadius_=0;i.unboundedCoords_={left:0,top:0};i.activationState_=i.defaultActivationState_();i.activationTimerCallback_=function(){i.activationAnimationHasEnded_=true;i.runDeactivationUXLogicIfReady_()};i.activateHandler_=function(e){return i.activate_(e)};i.deactivateHandler_=function(){return i.deactivate_()};i.focusHandler_=function(){return i.handleFocus()};i.blurHandler_=function(){return i.handleBlur()};i.resizeHandler_=function(){return i.layout()};return i}Object.defineProperty(t,"cssClasses",{get:function(){return De},enumerable:true,configurable:true});Object.defineProperty(t,"strings",{get:function(){return Ue},enumerable:true,configurable:true});Object.defineProperty(t,"numbers",{get:function(){return Fe},enumerable:true,configurable:true});Object.defineProperty(t,"defaultAdapter",{get:function(){return{addClass:function(){return undefined},browserSupportsCssVars:function(){return true},computeBoundingRect:function(){return{top:0,right:0,bottom:0,left:0,width:0,height:0}},containsEventTarget:function(){return true},deregisterDocumentInteractionHandler:function(){return undefined},deregisterInteractionHandler:function(){return undefined},deregisterResizeHandler:function(){return undefined},getWindowPageOffset:function(){return{x:0,y:0}},isSurfaceActive:function(){return true},isSurfaceDisabled:function(){return true},isUnbounded:function(){return true},registerDocumentInteractionHandler:function(){return undefined},registerInteractionHandler:function(){return undefined},registerResizeHandler:function(){return undefined},removeClass:function(){return undefined},updateCssVariable:function(){return undefined}}},enumerable:true,configurable:true});t.prototype.init=function(){var e=this;var r=this.supportsPressRipple_();this.registerRootHandlers_(r);if(r){var n=t.cssClasses,i=n.ROOT,a=n.UNBOUNDED;requestAnimationFrame((function(){e.adapter_.addClass(i);if(e.adapter_.isUnbounded()){e.adapter_.addClass(a);e.layoutInternal_()}}))}};t.prototype.destroy=function(){var e=this;if(this.supportsPressRipple_()){if(this.activationTimer_){clearTimeout(this.activationTimer_);this.activationTimer_=0;this.adapter_.removeClass(t.cssClasses.FG_ACTIVATION)}if(this.fgDeactivationRemovalTimer_){clearTimeout(this.fgDeactivationRemovalTimer_);this.fgDeactivationRemovalTimer_=0;this.adapter_.removeClass(t.cssClasses.FG_DEACTIVATION)}var r=t.cssClasses,n=r.ROOT,i=r.UNBOUNDED;requestAnimationFrame((function(){e.adapter_.removeClass(n);e.adapter_.removeClass(i);e.removeCssVars_()}))}this.deregisterRootHandlers_();this.deregisterDeactivationHandlers_()};t.prototype.activate=function(e){this.activate_(e)};t.prototype.deactivate=function(){this.deactivate_()};t.prototype.layout=function(){var e=this;if(this.layoutFrame_){cancelAnimationFrame(this.layoutFrame_)}this.layoutFrame_=requestAnimationFrame((function(){e.layoutInternal_();e.layoutFrame_=0}))};t.prototype.setUnbounded=function(e){var r=t.cssClasses.UNBOUNDED;if(e){this.adapter_.addClass(r)}else{this.adapter_.removeClass(r)}};t.prototype.handleFocus=function(){var e=this;requestAnimationFrame((function(){return e.adapter_.addClass(t.cssClasses.BG_FOCUSED)}))};t.prototype.handleBlur=function(){var e=this;requestAnimationFrame((function(){return e.adapter_.removeClass(t.cssClasses.BG_FOCUSED)}))};t.prototype.supportsPressRipple_=function(){return this.adapter_.browserSupportsCssVars()};t.prototype.defaultActivationState_=function(){return{activationEvent:undefined,hasDeactivationUXRun:false,isActivated:false,isProgrammatic:false,wasActivatedByPointer:false,wasElementMadeActive:false}};t.prototype.registerRootHandlers_=function(e){var t=this;if(e){ze.forEach((function(e){t.adapter_.registerInteractionHandler(e,t.activateHandler_)}));if(this.adapter_.isUnbounded()){this.adapter_.registerResizeHandler(this.resizeHandler_)}}this.adapter_.registerInteractionHandler("focus",this.focusHandler_);this.adapter_.registerInteractionHandler("blur",this.blurHandler_)};t.prototype.registerDeactivationHandlers_=function(e){var t=this;if(e.type==="keydown"){this.adapter_.registerInteractionHandler("keyup",this.deactivateHandler_)}else{Be.forEach((function(e){t.adapter_.registerDocumentInteractionHandler(e,t.deactivateHandler_)}))}};t.prototype.deregisterRootHandlers_=function(){var e=this;ze.forEach((function(t){e.adapter_.deregisterInteractionHandler(t,e.activateHandler_)}));this.adapter_.deregisterInteractionHandler("focus",this.focusHandler_);this.adapter_.deregisterInteractionHandler("blur",this.blurHandler_);if(this.adapter_.isUnbounded()){this.adapter_.deregisterResizeHandler(this.resizeHandler_)}};t.prototype.deregisterDeactivationHandlers_=function(){var e=this;this.adapter_.deregisterInteractionHandler("keyup",this.deactivateHandler_);Be.forEach((function(t){e.adapter_.deregisterDocumentInteractionHandler(t,e.deactivateHandler_)}))};t.prototype.removeCssVars_=function(){var e=this;var r=t.strings;var n=Object.keys(r);n.forEach((function(t){if(t.indexOf("VAR_")===0){e.adapter_.updateCssVariable(r[t],null)}}))};t.prototype.activate_=function(e){var t=this;if(this.adapter_.isSurfaceDisabled()){return}var r=this.activationState_;if(r.isActivated){return}var n=this.previousActivationEvent_;var i=n&&e!==undefined&&n.type!==e.type;if(i){return}r.isActivated=true;r.isProgrammatic=e===undefined;r.activationEvent=e;r.wasActivatedByPointer=r.isProgrammatic?false:e!==undefined&&(e.type==="mousedown"||e.type==="touchstart"||e.type==="pointerdown");var a=e!==undefined&&qe.length>0&&qe.some((function(e){return t.adapter_.containsEventTarget(e)}));if(a){this.resetActivationState_();return}if(e!==undefined){qe.push(e.target);this.registerDeactivationHandlers_(e)}r.wasElementMadeActive=this.checkElementMadeActive_(e);if(r.wasElementMadeActive){this.animateActivation_()}requestAnimationFrame((function(){qe=[];if(!r.wasElementMadeActive&&e!==undefined&&(e.key===" "||e.keyCode===32)){r.wasElementMadeActive=t.checkElementMadeActive_(e);if(r.wasElementMadeActive){t.animateActivation_()}}if(!r.wasElementMadeActive){t.activationState_=t.defaultActivationState_()}}))};t.prototype.checkElementMadeActive_=function(e){return e!==undefined&&e.type==="keydown"?this.adapter_.isSurfaceActive():true};t.prototype.animateActivation_=function(){var e=this;var r=t.strings,n=r.VAR_FG_TRANSLATE_START,i=r.VAR_FG_TRANSLATE_END;var a=t.cssClasses,o=a.FG_DEACTIVATION,s=a.FG_ACTIVATION;var u=t.numbers.DEACTIVATION_TIMEOUT_MS;this.layoutInternal_();var c="";var d="";if(!this.adapter_.isUnbounded()){var l=this.getFgTranslationCoordinates_(),f=l.startPoint,p=l.endPoint;c=f.x+"px, "+f.y+"px";d=p.x+"px, "+p.y+"px"}this.adapter_.updateCssVariable(n,c);this.adapter_.updateCssVariable(i,d);clearTimeout(this.activationTimer_);clearTimeout(this.fgDeactivationRemovalTimer_);this.rmBoundedActivationClasses_();this.adapter_.removeClass(o);this.adapter_.computeBoundingRect();this.adapter_.addClass(s);this.activationTimer_=setTimeout((function(){return e.activationTimerCallback_()}),u)};t.prototype.getFgTranslationCoordinates_=function(){var e=this.activationState_,t=e.activationEvent,r=e.wasActivatedByPointer;var n;if(r){n=Le(t,this.adapter_.getWindowPageOffset(),this.adapter_.computeBoundingRect())}else{n={x:this.frame_.width/2,y:this.frame_.height/2}}n={x:n.x-this.initialSize_/2,y:n.y-this.initialSize_/2};var i={x:this.frame_.width/2-this.initialSize_/2,y:this.frame_.height/2-this.initialSize_/2};return{startPoint:n,endPoint:i}};t.prototype.runDeactivationUXLogicIfReady_=function(){var e=this;var r=t.cssClasses.FG_DEACTIVATION;var n=this.activationState_,i=n.hasDeactivationUXRun,a=n.isActivated;var o=i||!a;if(o&&this.activationAnimationHasEnded_){this.rmBoundedActivationClasses_();this.adapter_.addClass(r);this.fgDeactivationRemovalTimer_=setTimeout((function(){e.adapter_.removeClass(r)}),Fe.FG_DEACTIVATION_MS)}};t.prototype.rmBoundedActivationClasses_=function(){var e=t.cssClasses.FG_ACTIVATION;this.adapter_.removeClass(e);this.activationAnimationHasEnded_=false;this.adapter_.computeBoundingRect()};t.prototype.resetActivationState_=function(){var e=this;this.previousActivationEvent_=this.activationState_.activationEvent;this.activationState_=this.defaultActivationState_();setTimeout((function(){return e.previousActivationEvent_=undefined}),t.numbers.TAP_DELAY_MS)};t.prototype.deactivate_=function(){var e=this;var t=this.activationState_;if(!t.isActivated){return}var r=n({},t);if(t.isProgrammatic){requestAnimationFrame((function(){return e.animateDeactivation_(r)}));this.resetActivationState_()}else{this.deregisterDeactivationHandlers_();requestAnimationFrame((function(){e.activationState_.hasDeactivationUXRun=true;e.animateDeactivation_(r);e.resetActivationState_()}))}};t.prototype.animateDeactivation_=function(e){var t=e.wasActivatedByPointer,r=e.wasElementMadeActive;if(t||r){this.runDeactivationUXLogicIfReady_()}};t.prototype.layoutInternal_=function(){var e=this;this.frame_=this.adapter_.computeBoundingRect();var r=Math.max(this.frame_.height,this.frame_.width);var n=function(){var r=Math.sqrt(Math.pow(e.frame_.width,2)+Math.pow(e.frame_.height,2));return r+t.numbers.PADDING};this.maxRadius_=this.adapter_.isUnbounded()?r:n();var i=Math.floor(r*t.numbers.INITIAL_ORIGIN_SCALE);if(this.adapter_.isUnbounded()&&i%2!==0){this.initialSize_=i-1}else{this.initialSize_=i}this.fgScale_=""+this.maxRadius_/this.initialSize_;this.updateLayoutCssVars_()};t.prototype.updateLayoutCssVars_=function(){var e=t.strings,r=e.VAR_FG_SIZE,n=e.VAR_LEFT,i=e.VAR_TOP,a=e.VAR_FG_SCALE;this.adapter_.updateCssVariable(r,this.initialSize_+"px");this.adapter_.updateCssVariable(a,this.fgScale_);if(this.adapter_.isUnbounded()){this.unboundedCoords_={left:Math.round(this.frame_.width/2-this.initialSize_/2),top:Math.round(this.frame_.height/2-this.initialSize_/2)};this.adapter_.updateCssVariable(n,this.unboundedCoords_.left+"px");this.adapter_.updateCssVariable(i,this.unboundedCoords_.top+"px")}};return t}(He);
/**
            @license
            Copyright 2018 Google Inc. All Rights Reserved.

            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            */var We=xe(__makeTemplateObject(["@keyframes mdc-ripple-fg-radius-in{from{animation-timing-function:cubic-bezier(0.4, 0, 0.2, 1);transform:translate(var(--mdc-ripple-fg-translate-start, 0)) scale(1)}to{transform:translate(var(--mdc-ripple-fg-translate-end, 0)) scale(var(--mdc-ripple-fg-scale, 1))}}@keyframes mdc-ripple-fg-opacity-in{from{animation-timing-function:linear;opacity:0}to{opacity:var(--mdc-ripple-fg-opacity, 0)}}@keyframes mdc-ripple-fg-opacity-out{from{animation-timing-function:linear;opacity:var(--mdc-ripple-fg-opacity, 0)}to{opacity:0}}"],["@keyframes mdc-ripple-fg-radius-in{from{animation-timing-function:cubic-bezier(0.4, 0, 0.2, 1);transform:translate(var(--mdc-ripple-fg-translate-start, 0)) scale(1)}to{transform:translate(var(--mdc-ripple-fg-translate-end, 0)) scale(var(--mdc-ripple-fg-scale, 1))}}@keyframes mdc-ripple-fg-opacity-in{from{animation-timing-function:linear;opacity:0}to{opacity:var(--mdc-ripple-fg-opacity, 0)}}@keyframes mdc-ripple-fg-opacity-out{from{animation-timing-function:linear;opacity:var(--mdc-ripple-fg-opacity, 0)}to{opacity:0}}"]));
/**
            @license
            Copyright 2018 Google Inc. All Rights Reserved.

            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            */var Xe=je(window);var Je=navigator.userAgent.match(/Safari/);var Ye=false;var $e=function(){Ye=true;var e=document.createElement("style");var t=new N({templateFactory:M});t.appendInto(e);t.setValue(We);t.commit();document.head.appendChild(e)};var Ze=e("r",(function(e){if(Je&&!Ye){$e()}var t=e.surfaceNode;var r=e.interactionNode||t;if(r.getRootNode()!==t.getRootNode()){if(r.style.position===""){r.style.position="relative"}}var n={browserSupportsCssVars:function(){return Xe},isUnbounded:function(){return e.unbounded===undefined?true:e.unbounded},isSurfaceActive:function(){return ke(r,":active")},isSurfaceDisabled:function(){return Boolean(r.hasAttribute("disabled"))},addClass:function(e){return t.classList.add(e)},removeClass:function(e){return t.classList.remove(e)},containsEventTarget:function(e){return r.contains(e)},registerInteractionHandler:function(e,t){return r.addEventListener(e,t,Ve())},deregisterInteractionHandler:function(e,t){return r.removeEventListener(e,t,Ve())},registerDocumentInteractionHandler:function(e,t){return document.documentElement.addEventListener(e,t,Ve())},deregisterDocumentInteractionHandler:function(e,t){return document.documentElement.removeEventListener(e,t,Ve())},registerResizeHandler:function(e){return window.addEventListener("resize",e)},deregisterResizeHandler:function(e){return window.removeEventListener("resize",e)},updateCssVariable:function(e,r){return t.style.setProperty(e,r)},computeBoundingRect:function(){return t.getBoundingClientRect()},getWindowPageOffset:function(){return{x:window.pageXOffset,y:window.pageYOffset}}};var i=new Ge(n);i.init();return i}));var Ke=new WeakMap;var Qe=e("g",s((function(e){if(e===void 0){e={}}return function(t){var r=t.committer.element;var n=e.interactionNode||r;var i=t.value;var a=Ke.get(i);if(a!==undefined&&a!==n){i.destroy();i=l}if(i===l){i=Ze(Object.assign({},e,{surfaceNode:r}));Ke.set(i,n);t.setValue(i)}else{if(e.unbounded!==undefined){i.setUnbounded(e.unbounded)}if(e.disabled!==undefined){i.setUnbounded(e.disabled)}}if(e.active===true){i.activate()}else if(e.active===false){i.deactivate()}}})))}}}));