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
***************************************************************************** */
var t=function(i,e){return(t=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(t,i){t.__proto__=i}||function(t,i){for(var e in i)i.hasOwnProperty(e)&&(t[e]=i[e])})(i,e)};function i(i,e){function s(){this.constructor=i}t(i,e),i.prototype=null===e?Object.create(e):(s.prototype=e.prototype,new s)}var e=function(){return(e=Object.assign||function(t){for(var i,e=1,s=arguments.length;e<s;e++)for(var n in i=arguments[e])Object.prototype.hasOwnProperty.call(i,n)&&(t[n]=i[n]);return t}).apply(this,arguments)};function s(t,i,e,s){var n,o=arguments.length,r=o<3?i:null===s?s=Object.getOwnPropertyDescriptor(i,e):s;if("object"==typeof Reflect&&"function"==typeof Reflect.decorate)r=Reflect.decorate(t,i,e,s);else for(var c=t.length-1;c>=0;c--)(n=t[c])&&(r=(o<3?n(r):o>3?n(i,e,r):n(i,e))||r);return o>3&&r&&Object.defineProperty(i,e,r),r}function n(t){var i="function"==typeof Symbol&&t[Symbol.iterator],e=0;return i?i.call(t):{next:function(){return t&&e>=t.length&&(t=void 0),{value:t&&t[e++],done:!t}}}}
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
 */const o=new WeakMap,r=t=>(...i)=>{const e=t(...i);return o.set(e,!0),e},c=t=>"function"==typeof t&&o.has(t),h=void 0!==window.customElements&&void 0!==window.customElements.polyfillWrapFlushCallback,a=(t,i,e=null)=>{for(;i!==e;){const e=i.nextSibling;t.removeChild(i),i=e}},u={},l={},f=`{{lit-${String(Math.random()).slice(2)}}}`,d=`\x3c!--${f}--\x3e`,p=new RegExp(`${f}|${d}`),m="$lit$";class v{constructor(t,i){this.parts=[],this.element=i;const e=[],s=[],n=document.createTreeWalker(i.content,133,null,!1);let o=0,r=-1,c=0;const{strings:h,values:{length:a}}=t;for(;c<a;){const t=n.nextNode();if(null!==t){if(r++,1===t.nodeType){if(t.hasAttributes()){const i=t.attributes,{length:e}=i;let s=0;for(let t=0;t<e;t++)w(i[t].name,m)&&s++;for(;s-- >0;){const i=b.exec(h[c])[2],e=i.toLowerCase()+m,s=t.getAttribute(e);t.removeAttribute(e);const n=s.split(p);this.parts.push({type:"attribute",index:r,name:i,strings:n}),c+=n.length-1}}"TEMPLATE"===t.tagName&&(s.push(t),n.currentNode=t.content)}else if(3===t.nodeType){const i=t.data;if(i.indexOf(f)>=0){const s=t.parentNode,n=i.split(p),o=n.length-1;for(let i=0;i<o;i++){let e,o=n[i];if(""===o)e=y();else{const t=b.exec(o);null!==t&&w(t[2],m)&&(o=o.slice(0,t.index)+t[1]+t[2].slice(0,-m.length)+t[3]),e=document.createTextNode(o)}s.insertBefore(e,t),this.parts.push({type:"node",index:++r})}""===n[o]?(s.insertBefore(y(),t),e.push(t)):t.data=n[o],c+=o}}else if(8===t.nodeType)if(t.data===f){const i=t.parentNode;null!==t.previousSibling&&r!==o||(r++,i.insertBefore(y(),t)),o=r,this.parts.push({type:"node",index:r}),null===t.nextSibling?t.data="":(e.push(t),r--),c++}else{let i=-1;for(;-1!==(i=t.data.indexOf(f,i+1));)this.parts.push({type:"node",index:-1}),c++}}else n.currentNode=s.pop()}for(const u of e)u.parentNode.removeChild(u)}}const w=(t,i)=>{const e=t.length-i.length;return e>=0&&t.slice(e)===i},g=t=>-1!==t.index,y=()=>document.createComment(""),b=/([ \x09\x0a\x0c\x0d])([^\0-\x1F\x7F-\x9F "'>=/]+)([ \x09\x0a\x0c\x0d]*=[ \x09\x0a\x0c\x0d]*(?:[^ \x09\x0a\x0c\x0d"'`<>=]*|"[^"]*|'[^']*))$/;
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
 */
class _{constructor(t,i,e){this.__parts=[],this.template=t,this.processor=i,this.options=e}update(t){let i=0;for(const e of this.__parts)void 0!==e&&e.setValue(t[i]),i++;for(const e of this.__parts)void 0!==e&&e.commit()}_clone(){const t=h?this.template.element.content.cloneNode(!0):document.importNode(this.template.element.content,!0),i=[],e=this.template.parts,s=document.createTreeWalker(t,133,null,!1);let n,o=0,r=0,c=s.nextNode();for(;o<e.length;)if(g(n=e[o])){for(;r<n.index;)r++,"TEMPLATE"===c.nodeName&&(i.push(c),s.currentNode=c.content),null===(c=s.nextNode())&&(s.currentNode=i.pop(),c=s.nextNode());if("node"===n.type){const t=this.processor.handleTextExpression(this.options);t.insertAfterNode(c.previousSibling),this.__parts.push(t)}else this.__parts.push(...this.processor.handleAttributeExpressions(c,n.name,n.strings,this.options));o++}else this.__parts.push(void 0),o++;return h&&(document.adoptNode(t),customElements.upgrade(t)),t}}
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
 */const A=` ${f} `;class S{constructor(t,i,e,s){this.strings=t,this.values=i,this.type=e,this.processor=s}getHTML(){const t=this.strings.length-1;let i="",e=!1;for(let s=0;s<t;s++){const t=this.strings[s],n=t.lastIndexOf("\x3c!--");e=(n>-1||e)&&-1===t.indexOf("--\x3e",n+1);const o=b.exec(t);i+=null===o?t+(e?A:d):t.substr(0,o.index)+o[1]+o[2]+m+o[3]+f}return i+this.strings[t]}getTemplateElement(){const t=document.createElement("template");return t.innerHTML=this.getHTML(),t}}
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
 */const x=t=>null===t||!("object"==typeof t||"function"==typeof t),T=t=>Array.isArray(t)||!(!t||!t[Symbol.iterator]);class O{constructor(t,i,e){this.dirty=!0,this.element=t,this.name=i,this.strings=e,this.parts=[];for(let s=0;s<e.length-1;s++)this.parts[s]=this._createPart()}_createPart(){return new C(this)}_getValue(){const t=this.strings,i=t.length-1;let e="";for(let s=0;s<i;s++){e+=t[s];const i=this.parts[s];if(void 0!==i){const t=i.value;if(x(t)||!T(t))e+="string"==typeof t?t:String(t);else for(const i of t)e+="string"==typeof i?i:String(i)}}return e+t[i]}commit(){this.dirty&&(this.dirty=!1,this.element.setAttribute(this.name,this._getValue()))}}class C{constructor(t){this.value=void 0,this.committer=t}setValue(t){t===u||x(t)&&t===this.value||(this.value=t,c(t)||(this.committer.dirty=!0))}commit(){for(;c(this.value);){const t=this.value;this.value=u,t(this)}this.value!==u&&this.committer.commit()}}class E{constructor(t){this.value=void 0,this.__pendingValue=void 0,this.options=t}appendInto(t){this.startNode=t.appendChild(y()),this.endNode=t.appendChild(y())}insertAfterNode(t){this.startNode=t,this.endNode=t.nextSibling}appendIntoPart(t){t.__insert(this.startNode=y()),t.__insert(this.endNode=y())}insertAfterPart(t){t.__insert(this.startNode=y()),this.endNode=t.endNode,t.endNode=this.startNode}setValue(t){this.__pendingValue=t}commit(){for(;c(this.__pendingValue);){const t=this.__pendingValue;this.__pendingValue=u,t(this)}const t=this.__pendingValue;t!==u&&(x(t)?t!==this.value&&this.__commitText(t):t instanceof S?this.__commitTemplateResult(t):t instanceof Node?this.__commitNode(t):T(t)?this.__commitIterable(t):t===l?(this.value=l,this.clear()):this.__commitText(t))}__insert(t){this.endNode.parentNode.insertBefore(t,this.endNode)}__commitNode(t){this.value!==t&&(this.clear(),this.__insert(t),this.value=t)}__commitText(t){const i=this.startNode.nextSibling,e="string"==typeof(t=null==t?"":t)?t:String(t);i===this.endNode.previousSibling&&3===i.nodeType?i.data=e:this.__commitNode(document.createTextNode(e)),this.value=t}__commitTemplateResult(t){const i=this.options.templateFactory(t);if(this.value instanceof _&&this.value.template===i)this.value.update(t.values);else{const e=new _(i,t.processor,this.options),s=e._clone();e.update(t.values),this.__commitNode(s),this.value=e}}__commitIterable(t){Array.isArray(this.value)||(this.value=[],this.clear());const i=this.value;let e,s=0;for(const n of t)void 0===(e=i[s])&&(e=new E(this.options),i.push(e),0===s?e.appendIntoPart(this):e.insertAfterPart(i[s-1])),e.setValue(n),e.commit(),s++;s<i.length&&(i.length=s,this.clear(e&&e.endNode))}clear(t=this.startNode){a(this.startNode.parentNode,t.nextSibling,this.endNode)}}class j{constructor(t,i,e){if(this.value=void 0,this.__pendingValue=void 0,2!==e.length||""!==e[0]||""!==e[1])throw new Error("Boolean attributes can only contain a single expression");this.element=t,this.name=i,this.strings=e}setValue(t){this.__pendingValue=t}commit(){for(;c(this.__pendingValue);){const t=this.__pendingValue;this.__pendingValue=u,t(this)}if(this.__pendingValue===u)return;const t=!!this.__pendingValue;this.value!==t&&(t?this.element.setAttribute(this.name,""):this.element.removeAttribute(this.name),this.value=t),this.__pendingValue=u}}class M extends O{constructor(t,i,e){super(t,i,e),this.single=2===e.length&&""===e[0]&&""===e[1]}_createPart(){return new I(this)}_getValue(){return this.single?this.parts[0].value:super._getValue()}commit(){this.dirty&&(this.dirty=!1,this.element[this.name]=this._getValue())}}class I extends C{}let P=!1;try{const t={get capture(){return P=!0,!1}};window.addEventListener("test",t,t),window.removeEventListener("test",t,t)}catch(Vt){}class k{constructor(t,i,e){this.value=void 0,this.__pendingValue=void 0,this.element=t,this.eventName=i,this.eventContext=e,this.__boundHandleEvent=t=>this.handleEvent(t)}setValue(t){this.__pendingValue=t}commit(){for(;c(this.__pendingValue);){const t=this.__pendingValue;this.__pendingValue=u,t(this)}if(this.__pendingValue===u)return;const t=this.__pendingValue,i=this.value,e=null==t||null!=i&&(t.capture!==i.capture||t.once!==i.once||t.passive!==i.passive),s=null!=t&&(null==i||e);e&&this.element.removeEventListener(this.eventName,this.__boundHandleEvent,this.__options),s&&(this.__options=R(t),this.element.addEventListener(this.eventName,this.__boundHandleEvent,this.__options)),this.value=t,this.__pendingValue=u}handleEvent(t){"function"==typeof this.value?this.value.call(this.eventContext||this.element,t):this.value.handleEvent(t)}}const R=t=>t&&(P?{capture:t.capture,passive:t.passive,once:t.once}:t.capture),F=new
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
 */
class{handleAttributeExpressions(t,i,e,s){const n=i[0];return"."===n?new M(t,i.slice(1),e).parts:"@"===n?[new k(t,i.slice(1),s.eventContext)]:"?"===n?[new j(t,i.slice(1),e)]:new O(t,i,e).parts}handleTextExpression(t){return new E(t)}};
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
 */
function V(t){let i=N.get(t.type);void 0===i&&(i={stringsArray:new WeakMap,keyString:new Map},N.set(t.type,i));let e=i.stringsArray.get(t.strings);if(void 0!==e)return e;const s=t.strings.join(f);return void 0===(e=i.keyString.get(s))&&(e=new v(t,t.getTemplateElement()),i.keyString.set(s,e)),i.stringsArray.set(t.strings,e),e}const N=new Map,U=new WeakMap;
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
 */
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
 */
(window.litHtmlVersions||(window.litHtmlVersions=[])).push("1.1.2");const D=(t,...i)=>new S(t,i,"html",F),q=133;
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
 */function H(t,i){const{element:{content:e},parts:s}=t,n=document.createTreeWalker(e,q,null,!1);let o=L(s),r=s[o],c=-1,h=0;const a=[];let u=null;for(;n.nextNode();){c++;const t=n.currentNode;for(t.previousSibling===u&&(u=null),i.has(t)&&(a.push(t),null===u&&(u=t)),null!==u&&h++;void 0!==r&&r.index===c;)r.index=null!==u?-1:r.index-h,r=s[o=L(s,o)]}a.forEach(t=>t.parentNode.removeChild(t))}const z=t=>{let i=11===t.nodeType?0:1;const e=document.createTreeWalker(t,q,null,!1);for(;e.nextNode();)i++;return i},L=(t,i=-1)=>{for(let e=i+1;e<t.length;e++)if(g(t[e]))return e;return-1},$=(t,i)=>`${t}--${i}`;let G=!0;void 0===window.ShadyCSS?G=!1:void 0===window.ShadyCSS.prepareTemplateDom&&(console.warn("Incompatible ShadyCSS version detected. Please update to at least @webcomponents/webcomponentsjs@2.0.2 and @webcomponents/shadycss@1.3.1."),G=!1);const B=t=>i=>{const e=$(i.type,t);let s=N.get(e);void 0===s&&(s={stringsArray:new WeakMap,keyString:new Map},N.set(e,s));let n=s.stringsArray.get(i.strings);if(void 0!==n)return n;const o=i.strings.join(f);if(void 0===(n=s.keyString.get(o))){const e=i.getTemplateElement();G&&window.ShadyCSS.prepareTemplateDom(e,t),n=new v(i,e),s.keyString.set(o,n)}return s.stringsArray.set(i.strings,n),n},W=["html","svg"],J=new Set;window.JSCompiler_renameProperty=t=>t;const X={toAttribute(t,i){switch(i){case Boolean:return t?"":null;case Object:case Array:return null==t?t:JSON.stringify(t)}return t},fromAttribute(t,i){switch(i){case Boolean:return null!==t;case Number:return null===t?null:Number(t);case Object:case Array:return JSON.parse(t)}return t}},Y=(t,i)=>i!==t&&(i==i||t==t),Z={attribute:!0,type:String,converter:X,reflect:!1,hasChanged:Y},K=Promise.resolve(!0),Q=1,tt=4,it=8,et=16,st=32,nt="finalized";class ot extends HTMLElement{constructor(){super(),this._updateState=0,this._instanceProperties=void 0,this._updatePromise=K,this._hasConnectedResolver=void 0,this._changedProperties=new Map,this._reflectingProperties=void 0,this.initialize()}static get observedAttributes(){this.finalize();const t=[];return this._classProperties.forEach((i,e)=>{const s=this._attributeNameForProperty(e,i);void 0!==s&&(this._attributeToPropertyMap.set(s,e),t.push(s))}),t}static _ensureClassProperties(){if(!this.hasOwnProperty(JSCompiler_renameProperty("_classProperties",this))){this._classProperties=new Map;const t=Object.getPrototypeOf(this)._classProperties;void 0!==t&&t.forEach((t,i)=>this._classProperties.set(i,t))}}static createProperty(t,i=Z){if(this._ensureClassProperties(),this._classProperties.set(t,i),i.noAccessor||this.prototype.hasOwnProperty(t))return;const e="symbol"==typeof t?Symbol():`__${t}`;Object.defineProperty(this.prototype,t,{get(){return this[e]},set(i){const s=this[t];this[e]=i,this._requestUpdate(t,s)},configurable:!0,enumerable:!0})}static finalize(){const t=Object.getPrototypeOf(this);if(t.hasOwnProperty(nt)||t.finalize(),this[nt]=!0,this._ensureClassProperties(),this._attributeToPropertyMap=new Map,this.hasOwnProperty(JSCompiler_renameProperty("properties",this))){const t=this.properties,i=[...Object.getOwnPropertyNames(t),..."function"==typeof Object.getOwnPropertySymbols?Object.getOwnPropertySymbols(t):[]];for(const e of i)this.createProperty(e,t[e])}}static _attributeNameForProperty(t,i){const e=i.attribute;return!1===e?void 0:"string"==typeof e?e:"string"==typeof t?t.toLowerCase():void 0}static _valueHasChanged(t,i,e=Y){return e(t,i)}static _propertyValueFromAttribute(t,i){const e=i.converter||X,s="function"==typeof e?e:e.fromAttribute;return s?s(t,i.type):t}static _propertyValueToAttribute(t,i){if(void 0===i.reflect)return;const e=i.converter;return(e&&e.toAttribute||X.toAttribute)(t,i.type)}initialize(){this._saveInstanceProperties(),this._requestUpdate()}_saveInstanceProperties(){this.constructor._classProperties.forEach((t,i)=>{if(this.hasOwnProperty(i)){const t=this[i];delete this[i],this._instanceProperties||(this._instanceProperties=new Map),this._instanceProperties.set(i,t)}})}_applyInstanceProperties(){this._instanceProperties.forEach((t,i)=>this[i]=t),this._instanceProperties=void 0}connectedCallback(){this._updateState=this._updateState|st,this._hasConnectedResolver&&(this._hasConnectedResolver(),this._hasConnectedResolver=void 0)}disconnectedCallback(){}attributeChangedCallback(t,i,e){i!==e&&this._attributeToProperty(t,e)}_propertyToAttribute(t,i,e=Z){const s=this.constructor,n=s._attributeNameForProperty(t,e);if(void 0!==n){const t=s._propertyValueToAttribute(i,e);if(void 0===t)return;this._updateState=this._updateState|it,null==t?this.removeAttribute(n):this.setAttribute(n,t),this._updateState=this._updateState&~it}}_attributeToProperty(t,i){if(this._updateState&it)return;const e=this.constructor,s=e._attributeToPropertyMap.get(t);if(void 0!==s){const t=e._classProperties.get(s)||Z;this._updateState=this._updateState|et,this[s]=e._propertyValueFromAttribute(i,t),this._updateState=this._updateState&~et}}_requestUpdate(t,i){let e=!0;if(void 0!==t){const s=this.constructor,n=s._classProperties.get(t)||Z;s._valueHasChanged(this[t],i,n.hasChanged)?(this._changedProperties.has(t)||this._changedProperties.set(t,i),!0!==n.reflect||this._updateState&et||(void 0===this._reflectingProperties&&(this._reflectingProperties=new Map),this._reflectingProperties.set(t,n))):e=!1}!this._hasRequestedUpdate&&e&&this._enqueueUpdate()}requestUpdate(t,i){return this._requestUpdate(t,i),this.updateComplete}async _enqueueUpdate(){let t,i;this._updateState=this._updateState|tt;const e=this._updatePromise;this._updatePromise=new Promise((e,s)=>{t=e,i=s});try{await e}catch(s){}this._hasConnected||await new Promise(t=>this._hasConnectedResolver=t);try{const t=this.performUpdate();null!=t&&await t}catch(s){i(s)}t(!this._hasRequestedUpdate)}get _hasConnected(){return this._updateState&st}get _hasRequestedUpdate(){return this._updateState&tt}get hasUpdated(){return this._updateState&Q}performUpdate(){this._instanceProperties&&this._applyInstanceProperties();let t=!1;const i=this._changedProperties;try{(t=this.shouldUpdate(i))&&this.update(i)}catch(e){throw t=!1,e}finally{this._markUpdated()}t&&(this._updateState&Q||(this._updateState=this._updateState|Q,this.firstUpdated(i)),this.updated(i))}_markUpdated(){this._changedProperties=new Map,this._updateState=this._updateState&~tt}get updateComplete(){return this._getUpdateComplete()}_getUpdateComplete(){return this._updatePromise}shouldUpdate(t){return!0}update(t){void 0!==this._reflectingProperties&&this._reflectingProperties.size>0&&(this._reflectingProperties.forEach((t,i)=>this._propertyToAttribute(i,this[i],t)),this._reflectingProperties=void 0)}updated(t){}firstUpdated(t){}}ot[nt]=!0;
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
 */
const rt=t=>i=>"function"==typeof i?((t,i)=>(window.customElements.define(t,i),i))(t,i):((t,i)=>{const{kind:e,elements:s}=i;return{kind:e,elements:s,finisher(i){window.customElements.define(t,i)}}})(t,i),ct=(t,i)=>"method"!==i.kind||!i.descriptor||"value"in i.descriptor?{kind:"field",key:Symbol(),placement:"own",descriptor:{},initializer(){"function"==typeof i.initializer&&(this[i.key]=i.initializer.call(this))},finisher(e){e.createProperty(i.key,t)}}:Object.assign({},i,{finisher(e){e.createProperty(i.key,t)}}),ht=(t,i,e)=>{i.constructor.createProperty(e,t)};function at(t){return(i,e)=>void 0!==e?ht(t,i,e):ct(t,i)}function ut(t){return(i,e)=>{const s={get(){return this.renderRoot.querySelector(t)},enumerable:!0,configurable:!0};return void 0!==e?lt(s,i,e):ft(s,i)}}const lt=(t,i,e)=>{Object.defineProperty(i,e,t)},ft=(t,i)=>({kind:"method",placement:"prototype",key:i.key,descriptor:t}),dt="adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,pt=Symbol();class mt{constructor(t,i){if(i!==pt)throw new Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t}get styleSheet(){return void 0===this._styleSheet&&(dt?(this._styleSheet=new CSSStyleSheet,this._styleSheet.replaceSync(this.cssText)):this._styleSheet=null),this._styleSheet}toString(){return this.cssText}}const vt=(t,...i)=>{const e=i.reduce((i,e,s)=>i+(t=>{if(t instanceof mt)return t.cssText;if("number"==typeof t)return t;throw new Error(`Value passed to 'css' function must be a 'css' function result: ${t}. Use 'unsafeCSS' to pass non-literal values, but\n            take care to ensure page security.`)})(e)+t[s+1],t[0]);return new mt(e,pt)};
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
 */
(window.litElementVersions||(window.litElementVersions=[])).push("2.2.1");const wt=t=>t.flat?t.flat(1/0):function t(i,e=[]){for(let s=0,n=i.length;s<n;s++){const n=i[s];Array.isArray(n)?t(n,e):e.push(n)}return e}(t);class gt extends ot{static finalize(){super.finalize.call(this),this._styles=this.hasOwnProperty(JSCompiler_renameProperty("styles",this))?this._getUniqueStyles():this._styles||[]}static _getUniqueStyles(){const t=this.styles,i=[];return Array.isArray(t)?wt(t).reduceRight((t,i)=>(t.add(i),t),new Set).forEach(t=>i.unshift(t)):t&&i.push(t),i}initialize(){super.initialize(),this.renderRoot=this.createRenderRoot(),window.ShadowRoot&&this.renderRoot instanceof window.ShadowRoot&&this.adoptStyles()}createRenderRoot(){return this.attachShadow({mode:"open"})}adoptStyles(){const t=this.constructor._styles;0!==t.length&&(void 0===window.ShadyCSS||window.ShadyCSS.nativeShadow?dt?this.renderRoot.adoptedStyleSheets=t.map(t=>t.styleSheet):this._needsShimAdoptedStyleSheets=!0:window.ShadyCSS.ScopingShim.prepareAdoptedCssText(t.map(t=>t.cssText),this.localName))}connectedCallback(){super.connectedCallback(),this.hasUpdated&&void 0!==window.ShadyCSS&&window.ShadyCSS.styleElement(this)}update(t){super.update(t);const i=this.render();i instanceof S&&this.constructor.render(i,this.renderRoot,{scopeName:this.localName,eventContext:this}),this._needsShimAdoptedStyleSheets&&(this._needsShimAdoptedStyleSheets=!1,this.constructor._styles.forEach(t=>{const i=document.createElement("style");i.textContent=t.cssText,this.renderRoot.appendChild(i)}))}render(){}}
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
 */
function yt(t){return void 0===t&&(t=window),!!function(t){void 0===t&&(t=window);var i=!1;try{var e={get passive(){return i=!0,!1}},s=function(){};t.document.addEventListener("test",s,e),t.document.removeEventListener("test",s,e)}catch(n){i=!1}return i}
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
 */(t)&&{passive:!0}}gt.finalized=!0,gt.render=(t,i,e)=>{if(!e||"object"!=typeof e||!e.scopeName)throw new Error("The `scopeName` option is required.");const s=e.scopeName,n=U.has(i),o=G&&11===i.nodeType&&!!i.host,r=o&&!J.has(s),c=r?document.createDocumentFragment():i;if(((t,i,e)=>{let s=U.get(i);void 0===s&&(a(i,i.firstChild),U.set(i,s=new E(Object.assign({templateFactory:V},e))),s.appendInto(i)),s.setValue(t),s.commit()})(t,c,Object.assign({templateFactory:B(s)},e)),r){const t=U.get(c);U.delete(c),((t,i,e)=>{J.add(t);const s=e?e.element:document.createElement("template"),n=i.querySelectorAll("style"),{length:o}=n;if(0===o)return void window.ShadyCSS.prepareTemplateStyles(s,t);const r=document.createElement("style");for(let a=0;a<o;a++){const t=n[a];t.parentNode.removeChild(t),r.textContent+=t.textContent}(t=>{W.forEach(i=>{const e=N.get($(i,t));void 0!==e&&e.keyString.forEach(t=>{const{element:{content:i}}=t,e=new Set;Array.from(i.querySelectorAll("style")).forEach(t=>{e.add(t)}),H(t,e)})})})(t);const c=s.content;e?function(t,i,e=null){const{element:{content:s},parts:n}=t;if(null==e)return void s.appendChild(i);const o=document.createTreeWalker(s,q,null,!1);let r=L(n),c=0,h=-1;for(;o.nextNode();)for(h++,o.currentNode===e&&(c=z(i),e.parentNode.insertBefore(i,e));-1!==r&&n[r].index===h;){if(c>0){for(;-1!==r;)n[r].index+=c,r=L(n,r);return}r=L(n,r)}}
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
 */(e,r,c.firstChild):c.insertBefore(r,c.firstChild),window.ShadyCSS.prepareTemplateStyles(s,t);const h=c.querySelector("style");if(window.ShadyCSS.nativeShadow&&null!==h)i.insertBefore(h.cloneNode(!0),i.firstChild);else if(e){c.insertBefore(r,c.firstChild);const t=new Set;t.add(r),H(e,t)}})(s,c,t.value instanceof _?t.value.template:void 0),a(i,i.firstChild),i.appendChild(c),U.set(i,t)}!n&&o&&window.ShadyCSS.styleElement(i.host)};
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
 */
var bt,_t=function(){function t(t){void 0===t&&(t={}),this.adapter_=t}return Object.defineProperty(t,"cssClasses",{get:function(){return{}},enumerable:!0,configurable:!0}),Object.defineProperty(t,"strings",{get:function(){return{}},enumerable:!0,configurable:!0}),Object.defineProperty(t,"numbers",{get:function(){return{}},enumerable:!0,configurable:!0}),Object.defineProperty(t,"defaultAdapter",{get:function(){return{}},enumerable:!0,configurable:!0}),t.prototype.init=function(){},t.prototype.destroy=function(){},t}(),At={BG_FOCUSED:"mdc-ripple-upgraded--background-focused",FG_ACTIVATION:"mdc-ripple-upgraded--foreground-activation",FG_DEACTIVATION:"mdc-ripple-upgraded--foreground-deactivation",ROOT:"mdc-ripple-upgraded",UNBOUNDED:"mdc-ripple-upgraded--unbounded"},St={VAR_FG_SCALE:"--mdc-ripple-fg-scale",VAR_FG_SIZE:"--mdc-ripple-fg-size",VAR_FG_TRANSLATE_END:"--mdc-ripple-fg-translate-end",VAR_FG_TRANSLATE_START:"--mdc-ripple-fg-translate-start",VAR_LEFT:"--mdc-ripple-left",VAR_TOP:"--mdc-ripple-top"},xt={DEACTIVATION_TIMEOUT_MS:225,FG_DEACTIVATION_MS:150,INITIAL_ORIGIN_SCALE:.6,PADDING:10,TAP_DELAY_MS:300},Tt=["touchstart","pointerdown","mousedown","keydown"],Ot=["touchend","pointerup","mouseup","contextmenu"],Ct=[],Et=function(t){function s(i){var n=t.call(this,e({},s.defaultAdapter,i))||this;return n.activationAnimationHasEnded_=!1,n.activationTimer_=0,n.fgDeactivationRemovalTimer_=0,n.fgScale_="0",n.frame_={width:0,height:0},n.initialSize_=0,n.layoutFrame_=0,n.maxRadius_=0,n.unboundedCoords_={left:0,top:0},n.activationState_=n.defaultActivationState_(),n.activationTimerCallback_=function(){n.activationAnimationHasEnded_=!0,n.runDeactivationUXLogicIfReady_()},n.activateHandler_=function(t){return n.activate_(t)},n.deactivateHandler_=function(){return n.deactivate_()},n.focusHandler_=function(){return n.handleFocus()},n.blurHandler_=function(){return n.handleBlur()},n.resizeHandler_=function(){return n.layout()},n}return i(s,t),Object.defineProperty(s,"cssClasses",{get:function(){return At},enumerable:!0,configurable:!0}),Object.defineProperty(s,"strings",{get:function(){return St},enumerable:!0,configurable:!0}),Object.defineProperty(s,"numbers",{get:function(){return xt},enumerable:!0,configurable:!0}),Object.defineProperty(s,"defaultAdapter",{get:function(){return{addClass:function(){},browserSupportsCssVars:function(){return!0},computeBoundingRect:function(){return{top:0,right:0,bottom:0,left:0,width:0,height:0}},containsEventTarget:function(){return!0},deregisterDocumentInteractionHandler:function(){},deregisterInteractionHandler:function(){},deregisterResizeHandler:function(){},getWindowPageOffset:function(){return{x:0,y:0}},isSurfaceActive:function(){return!0},isSurfaceDisabled:function(){return!0},isUnbounded:function(){return!0},registerDocumentInteractionHandler:function(){},registerInteractionHandler:function(){},registerResizeHandler:function(){},removeClass:function(){},updateCssVariable:function(){}}},enumerable:!0,configurable:!0}),s.prototype.init=function(){var t=this,i=this.supportsPressRipple_();if(this.registerRootHandlers_(i),i){var e=s.cssClasses,n=e.ROOT,o=e.UNBOUNDED;requestAnimationFrame((function(){t.adapter_.addClass(n),t.adapter_.isUnbounded()&&(t.adapter_.addClass(o),t.layoutInternal_())}))}},s.prototype.destroy=function(){var t=this;if(this.supportsPressRipple_()){this.activationTimer_&&(clearTimeout(this.activationTimer_),this.activationTimer_=0,this.adapter_.removeClass(s.cssClasses.FG_ACTIVATION)),this.fgDeactivationRemovalTimer_&&(clearTimeout(this.fgDeactivationRemovalTimer_),this.fgDeactivationRemovalTimer_=0,this.adapter_.removeClass(s.cssClasses.FG_DEACTIVATION));var i=s.cssClasses,e=i.ROOT,n=i.UNBOUNDED;requestAnimationFrame((function(){t.adapter_.removeClass(e),t.adapter_.removeClass(n),t.removeCssVars_()}))}this.deregisterRootHandlers_(),this.deregisterDeactivationHandlers_()},s.prototype.activate=function(t){this.activate_(t)},s.prototype.deactivate=function(){this.deactivate_()},s.prototype.layout=function(){var t=this;this.layoutFrame_&&cancelAnimationFrame(this.layoutFrame_),this.layoutFrame_=requestAnimationFrame((function(){t.layoutInternal_(),t.layoutFrame_=0}))},s.prototype.setUnbounded=function(t){var i=s.cssClasses.UNBOUNDED;t?this.adapter_.addClass(i):this.adapter_.removeClass(i)},s.prototype.handleFocus=function(){var t=this;requestAnimationFrame((function(){return t.adapter_.addClass(s.cssClasses.BG_FOCUSED)}))},s.prototype.handleBlur=function(){var t=this;requestAnimationFrame((function(){return t.adapter_.removeClass(s.cssClasses.BG_FOCUSED)}))},s.prototype.supportsPressRipple_=function(){return this.adapter_.browserSupportsCssVars()},s.prototype.defaultActivationState_=function(){return{activationEvent:void 0,hasDeactivationUXRun:!1,isActivated:!1,isProgrammatic:!1,wasActivatedByPointer:!1,wasElementMadeActive:!1}},s.prototype.registerRootHandlers_=function(t){var i=this;t&&(Tt.forEach((function(t){i.adapter_.registerInteractionHandler(t,i.activateHandler_)})),this.adapter_.isUnbounded()&&this.adapter_.registerResizeHandler(this.resizeHandler_)),this.adapter_.registerInteractionHandler("focus",this.focusHandler_),this.adapter_.registerInteractionHandler("blur",this.blurHandler_)},s.prototype.registerDeactivationHandlers_=function(t){var i=this;"keydown"===t.type?this.adapter_.registerInteractionHandler("keyup",this.deactivateHandler_):Ot.forEach((function(t){i.adapter_.registerDocumentInteractionHandler(t,i.deactivateHandler_)}))},s.prototype.deregisterRootHandlers_=function(){var t=this;Tt.forEach((function(i){t.adapter_.deregisterInteractionHandler(i,t.activateHandler_)})),this.adapter_.deregisterInteractionHandler("focus",this.focusHandler_),this.adapter_.deregisterInteractionHandler("blur",this.blurHandler_),this.adapter_.isUnbounded()&&this.adapter_.deregisterResizeHandler(this.resizeHandler_)},s.prototype.deregisterDeactivationHandlers_=function(){var t=this;this.adapter_.deregisterInteractionHandler("keyup",this.deactivateHandler_),Ot.forEach((function(i){t.adapter_.deregisterDocumentInteractionHandler(i,t.deactivateHandler_)}))},s.prototype.removeCssVars_=function(){var t=this,i=s.strings;Object.keys(i).forEach((function(e){0===e.indexOf("VAR_")&&t.adapter_.updateCssVariable(i[e],null)}))},s.prototype.activate_=function(t){var i=this;if(!this.adapter_.isSurfaceDisabled()){var e=this.activationState_;if(!e.isActivated){var s=this.previousActivationEvent_;s&&void 0!==t&&s.type!==t.type||(e.isActivated=!0,e.isProgrammatic=void 0===t,e.activationEvent=t,e.wasActivatedByPointer=!e.isProgrammatic&&void 0!==t&&("mousedown"===t.type||"touchstart"===t.type||"pointerdown"===t.type),void 0!==t&&Ct.length>0&&Ct.some((function(t){return i.adapter_.containsEventTarget(t)}))?this.resetActivationState_():(void 0!==t&&(Ct.push(t.target),this.registerDeactivationHandlers_(t)),e.wasElementMadeActive=this.checkElementMadeActive_(t),e.wasElementMadeActive&&this.animateActivation_(),requestAnimationFrame((function(){Ct=[],e.wasElementMadeActive||void 0===t||" "!==t.key&&32!==t.keyCode||(e.wasElementMadeActive=i.checkElementMadeActive_(t),e.wasElementMadeActive&&i.animateActivation_()),e.wasElementMadeActive||(i.activationState_=i.defaultActivationState_())}))))}}},s.prototype.checkElementMadeActive_=function(t){return void 0===t||"keydown"!==t.type||this.adapter_.isSurfaceActive()},s.prototype.animateActivation_=function(){var t=this,i=s.strings,e=i.VAR_FG_TRANSLATE_START,n=i.VAR_FG_TRANSLATE_END,o=s.cssClasses,r=o.FG_DEACTIVATION,c=o.FG_ACTIVATION,h=s.numbers.DEACTIVATION_TIMEOUT_MS;this.layoutInternal_();var a="",u="";if(!this.adapter_.isUnbounded()){var l=this.getFgTranslationCoordinates_(),f=l.startPoint,d=l.endPoint;a=f.x+"px, "+f.y+"px",u=d.x+"px, "+d.y+"px"}this.adapter_.updateCssVariable(e,a),this.adapter_.updateCssVariable(n,u),clearTimeout(this.activationTimer_),clearTimeout(this.fgDeactivationRemovalTimer_),this.rmBoundedActivationClasses_(),this.adapter_.removeClass(r),this.adapter_.computeBoundingRect(),this.adapter_.addClass(c),this.activationTimer_=setTimeout((function(){return t.activationTimerCallback_()}),h)},s.prototype.getFgTranslationCoordinates_=function(){var t,i=this.activationState_;return{startPoint:t={x:(t=i.wasActivatedByPointer?function(t,i,e){if(!t)return{x:0,y:0};var s,n,o=i.x+e.left,r=i.y+e.top;return"touchstart"===t.type?(s=t.changedTouches[0].pageX-o,n=t.changedTouches[0].pageY-r):(s=t.pageX-o,n=t.pageY-r),{x:s,y:n}}
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
 */(i.activationEvent,this.adapter_.getWindowPageOffset(),this.adapter_.computeBoundingRect()):{x:this.frame_.width/2,y:this.frame_.height/2}).x-this.initialSize_/2,y:t.y-this.initialSize_/2},endPoint:{x:this.frame_.width/2-this.initialSize_/2,y:this.frame_.height/2-this.initialSize_/2}}},s.prototype.runDeactivationUXLogicIfReady_=function(){var t=this,i=s.cssClasses.FG_DEACTIVATION,e=this.activationState_;(e.hasDeactivationUXRun||!e.isActivated)&&this.activationAnimationHasEnded_&&(this.rmBoundedActivationClasses_(),this.adapter_.addClass(i),this.fgDeactivationRemovalTimer_=setTimeout((function(){t.adapter_.removeClass(i)}),xt.FG_DEACTIVATION_MS))},s.prototype.rmBoundedActivationClasses_=function(){this.adapter_.removeClass(s.cssClasses.FG_ACTIVATION),this.activationAnimationHasEnded_=!1,this.adapter_.computeBoundingRect()},s.prototype.resetActivationState_=function(){var t=this;this.previousActivationEvent_=this.activationState_.activationEvent,this.activationState_=this.defaultActivationState_(),setTimeout((function(){return t.previousActivationEvent_=void 0}),s.numbers.TAP_DELAY_MS)},s.prototype.deactivate_=function(){var t=this,i=this.activationState_;if(i.isActivated){var s=e({},i);i.isProgrammatic?(requestAnimationFrame((function(){return t.animateDeactivation_(s)})),this.resetActivationState_()):(this.deregisterDeactivationHandlers_(),requestAnimationFrame((function(){t.activationState_.hasDeactivationUXRun=!0,t.animateDeactivation_(s),t.resetActivationState_()})))}},s.prototype.animateDeactivation_=function(t){(t.wasActivatedByPointer||t.wasElementMadeActive)&&this.runDeactivationUXLogicIfReady_()},s.prototype.layoutInternal_=function(){this.frame_=this.adapter_.computeBoundingRect();var t=Math.max(this.frame_.height,this.frame_.width);this.maxRadius_=this.adapter_.isUnbounded()?t:Math.sqrt(Math.pow(this.frame_.width,2)+Math.pow(this.frame_.height,2))+s.numbers.PADDING;var i=Math.floor(t*s.numbers.INITIAL_ORIGIN_SCALE);this.initialSize_=this.adapter_.isUnbounded()&&i%2!=0?i-1:i,this.fgScale_=""+this.maxRadius_/this.initialSize_,this.updateLayoutCssVars_()},s.prototype.updateLayoutCssVars_=function(){var t=s.strings,i=t.VAR_LEFT,e=t.VAR_TOP,n=t.VAR_FG_SCALE;this.adapter_.updateCssVariable(t.VAR_FG_SIZE,this.initialSize_+"px"),this.adapter_.updateCssVariable(n,this.fgScale_),this.adapter_.isUnbounded()&&(this.unboundedCoords_={left:Math.round(this.frame_.width/2-this.initialSize_/2),top:Math.round(this.frame_.height/2-this.initialSize_/2)},this.adapter_.updateCssVariable(i,this.unboundedCoords_.left+"px"),this.adapter_.updateCssVariable(e,this.unboundedCoords_.top+"px"))},s}(_t);
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
 */
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
*/
const jt=vt`@keyframes mdc-ripple-fg-radius-in{from{animation-timing-function:cubic-bezier(0.4, 0, 0.2, 1);transform:translate(var(--mdc-ripple-fg-translate-start, 0)) scale(1)}to{transform:translate(var(--mdc-ripple-fg-translate-end, 0)) scale(var(--mdc-ripple-fg-scale, 1))}}@keyframes mdc-ripple-fg-opacity-in{from{animation-timing-function:linear;opacity:0}to{opacity:var(--mdc-ripple-fg-opacity, 0)}}@keyframes mdc-ripple-fg-opacity-out{from{animation-timing-function:linear;opacity:var(--mdc-ripple-fg-opacity, 0)}to{opacity:0}}`
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
*/,Mt=function(t,i){void 0===i&&(i=!1);var e,s=window.CSS;if("boolean"==typeof bt&&!i)return bt;if(!s||"function"!=typeof s.supports)return!1;var n=s.supports("--css-vars","yes"),o=s.supports("(--css-vars: yes)")&&s.supports("color","#00000000");return e=n||o,i||(bt=e),e}(),It=navigator.userAgent.match(/Safari/);let Pt=!1;const kt=t=>{It&&!Pt&&(()=>{Pt=!0;const t=document.createElement("style"),i=new E({templateFactory:V});i.appendInto(t),i.setValue(jt),i.commit(),document.head.appendChild(t)})();const i=t.surfaceNode,e=t.interactionNode||i;e.getRootNode()!==i.getRootNode()&&""===e.style.position&&(e.style.position="relative");const s=new Et({browserSupportsCssVars:()=>Mt,isUnbounded:()=>void 0===t.unbounded||t.unbounded,isSurfaceActive:()=>(function(t){return(t.matches||t.webkitMatchesSelector||t.msMatchesSelector).call(t,":active")})(e),isSurfaceDisabled:()=>Boolean(e.hasAttribute("disabled")),addClass:t=>i.classList.add(t),removeClass:t=>i.classList.remove(t),containsEventTarget:t=>e.contains(t),registerInteractionHandler:(t,i)=>e.addEventListener(t,i,yt()),deregisterInteractionHandler:(t,i)=>e.removeEventListener(t,i,yt()),registerDocumentInteractionHandler:(t,i)=>document.documentElement.addEventListener(t,i,yt()),deregisterDocumentInteractionHandler:(t,i)=>document.documentElement.removeEventListener(t,i,yt()),registerResizeHandler:t=>window.addEventListener("resize",t),deregisterResizeHandler:t=>window.removeEventListener("resize",t),updateCssVariable:(t,e)=>i.style.setProperty(t,e),computeBoundingRect:()=>i.getBoundingClientRect(),getWindowPageOffset:()=>({x:window.pageXOffset,y:window.pageYOffset})});return s.init(),s},Rt=new WeakMap,Ft=r((t={})=>i=>{const e=i.committer.element,s=t.interactionNode||e;let n=i.value;const o=Rt.get(n);void 0!==o&&o!==s&&(n.destroy(),n=u),n===u?(n=kt(Object.assign({},t,{surfaceNode:e})),Rt.set(n,s),i.setValue(n)):(void 0!==t.unbounded&&n.setUnbounded(t.unbounded),void 0!==t.disabled&&n.setUnbounded(t.disabled)),!0===t.active?n.activate():!1===t.active&&n.deactivate()});export{C as A,gt as L,I as P,i as _,e as a,s as b,vt as c,rt as d,r as e,n as f,Ft as g,D as h,at as p,ut as q,kt as r};