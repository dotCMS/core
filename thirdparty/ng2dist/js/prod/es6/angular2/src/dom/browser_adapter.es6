import {List,
  MapWrapper,
  ListWrapper} from 'angular2/src/facade/collection';
import {isBlank,
  isPresent} from 'angular2/src/facade/lang';
import {setRootDomAdapter} from './dom_adapter';
import {GenericBrowserDomAdapter} from './generic_browser_adapter';
var _attrToPropMap = {
  'innerHtml': 'innerHTML',
  'readonly': 'readOnly',
  'tabindex': 'tabIndex'
};
const DOM_KEY_LOCATION_NUMPAD = 3;
var _keyMap = {
  '\b': 'Backspace',
  '\t': 'Tab',
  '\x7F': 'Delete',
  '\x1B': 'Escape',
  'Del': 'Delete',
  'Esc': 'Escape',
  'Left': 'ArrowLeft',
  'Right': 'ArrowRight',
  'Up': 'ArrowUp',
  'Down': 'ArrowDown',
  'Menu': 'ContextMenu',
  'Scroll': 'ScrollLock',
  'Win': 'OS'
};
var _chromeNumKeyPadMap = {
  'A': '1',
  'B': '2',
  'C': '3',
  'D': '4',
  'E': '5',
  'F': '6',
  'G': '7',
  'H': '8',
  'I': '9',
  'J': '*',
  'K': '+',
  'M': '-',
  'N': '.',
  'O': '/',
  '\x60': '0',
  '\x90': 'NumLock'
};
export class BrowserDomAdapter extends GenericBrowserDomAdapter {
  static makeCurrent() {
    setRootDomAdapter(new BrowserDomAdapter());
  }
  logError(error) {
    window.console.error(error);
  }
  get attrToPropMap() {
    return _attrToPropMap;
  }
  query(selector) {
    return document.querySelector(selector);
  }
  querySelector(el, selector) {
    return el.querySelector(selector);
  }
  querySelectorAll(el, selector) {
    return el.querySelectorAll(selector);
  }
  on(el, evt, listener) {
    el.addEventListener(evt, listener, false);
  }
  onAndCancel(el, evt, listener) {
    el.addEventListener(evt, listener, false);
    return () => {
      el.removeEventListener(evt, listener, false);
    };
  }
  dispatchEvent(el, evt) {
    el.dispatchEvent(evt);
  }
  createMouseEvent(eventType) {
    var evt = new MouseEvent(eventType);
    evt.initEvent(eventType, true, true);
    return evt;
  }
  createEvent(eventType) {
    return new Event(eventType, true);
  }
  getInnerHTML(el) {
    return el.innerHTML;
  }
  getOuterHTML(el) {
    return el.outerHTML;
  }
  nodeName(node) {
    return node.nodeName;
  }
  nodeValue(node) {
    return node.nodeValue;
  }
  type(node) {
    return node.type;
  }
  content(node) {
    if (this.hasProperty(node, "content")) {
      return node.content;
    } else {
      return node;
    }
  }
  firstChild(el) {
    return el.firstChild;
  }
  nextSibling(el) {
    return el.nextSibling;
  }
  parentElement(el) {
    return el.parentElement;
  }
  childNodes(el) {
    return el.childNodes;
  }
  childNodesAsList(el) {
    var childNodes = el.childNodes;
    var res = ListWrapper.createFixedSize(childNodes.length);
    for (var i = 0; i < childNodes.length; i++) {
      res[i] = childNodes[i];
    }
    return res;
  }
  clearNodes(el) {
    for (var i = 0; i < el.childNodes.length; i++) {
      this.remove(el.childNodes[i]);
    }
  }
  appendChild(el, node) {
    el.appendChild(node);
  }
  removeChild(el, node) {
    el.removeChild(node);
  }
  replaceChild(el, newChild, oldChild) {
    el.replaceChild(newChild, oldChild);
  }
  remove(el) {
    var parent = el.parentNode;
    parent.removeChild(el);
    return el;
  }
  insertBefore(el, node) {
    el.parentNode.insertBefore(node, el);
  }
  insertAllBefore(el, nodes) {
    ListWrapper.forEach(nodes, (n) => {
      el.parentNode.insertBefore(n, el);
    });
  }
  insertAfter(el, node) {
    el.parentNode.insertBefore(node, el.nextSibling);
  }
  setInnerHTML(el, value) {
    el.innerHTML = value;
  }
  getText(el) {
    return el.textContent;
  }
  setText(el, value) {
    el.textContent = value;
  }
  getValue(el) {
    return el.value;
  }
  setValue(el, value) {
    el.value = value;
  }
  getChecked(el) {
    return el.checked;
  }
  setChecked(el, value) {
    el.checked = value;
  }
  createTemplate(html) {
    var t = document.createElement('template');
    t.innerHTML = html;
    return t;
  }
  createElement(tagName, doc = document) {
    return doc.createElement(tagName);
  }
  createTextNode(text, doc = document) {
    return doc.createTextNode(text);
  }
  createScriptTag(attrName, attrValue, doc = document) {
    var el = doc.createElement('SCRIPT');
    el.setAttribute(attrName, attrValue);
    return el;
  }
  createStyleElement(css, doc = document) {
    var style = doc.createElement('STYLE');
    style.innerText = css;
    return style;
  }
  createShadowRoot(el) {
    return el.createShadowRoot();
  }
  getShadowRoot(el) {
    return el.shadowRoot;
  }
  getHost(el) {
    return el.host;
  }
  clone(node) {
    return node.cloneNode(true);
  }
  hasProperty(element, name) {
    return name in element;
  }
  getElementsByClassName(element, name) {
    return element.getElementsByClassName(name);
  }
  getElementsByTagName(element, name) {
    return element.getElementsByTagName(name);
  }
  classList(element) {
    return Array.prototype.slice.call(element.classList, 0);
  }
  addClass(element, classname) {
    element.classList.add(classname);
  }
  removeClass(element, classname) {
    element.classList.remove(classname);
  }
  hasClass(element, classname) {
    return element.classList.contains(classname);
  }
  setStyle(element, stylename, stylevalue) {
    element.style[stylename] = stylevalue;
  }
  removeStyle(element, stylename) {
    element.style[stylename] = null;
  }
  getStyle(element, stylename) {
    return element.style[stylename];
  }
  tagName(element) {
    return element.tagName;
  }
  attributeMap(element) {
    var res = MapWrapper.create();
    var elAttrs = element.attributes;
    for (var i = 0; i < elAttrs.length; i++) {
      var attrib = elAttrs[i];
      MapWrapper.set(res, attrib.name, attrib.value);
    }
    return res;
  }
  getAttribute(element, attribute) {
    return element.getAttribute(attribute);
  }
  setAttribute(element, name, value) {
    element.setAttribute(name, value);
  }
  removeAttribute(element, attribute) {
    return element.removeAttribute(attribute);
  }
  templateAwareRoot(el) {
    return this.isTemplateElement(el) ? this.content(el) : el;
  }
  createHtmlDocument() {
    return document.implementation.createHTMLDocument('fakeTitle');
  }
  defaultDoc() {
    return document;
  }
  getBoundingClientRect(el) {
    return el.getBoundingClientRect();
  }
  getTitle() {
    return document.title;
  }
  setTitle(newTitle) {
    document.title = newTitle;
  }
  elementMatches(n, selector) {
    return n instanceof HTMLElement && n.matches(selector);
  }
  isTemplateElement(el) {
    return el instanceof HTMLElement && el.nodeName == "TEMPLATE";
  }
  isTextNode(node) {
    return node.nodeType === Node.TEXT_NODE;
  }
  isCommentNode(node) {
    return node.nodeType === Node.COMMENT_NODE;
  }
  isElementNode(node) {
    return node.nodeType === Node.ELEMENT_NODE;
  }
  hasShadowRoot(node) {
    return node instanceof HTMLElement && isPresent(node.shadowRoot);
  }
  isShadowRoot(node) {
    return node instanceof ShadowRoot;
  }
  importIntoDoc(node) {
    var result = document.importNode(node, true);
    if (this.isTemplateElement(result) && !this.content(result).childNodes.length && this.content(node).childNodes.length) {
      var childNodes = this.content(node).childNodes;
      for (var i = 0; i < childNodes.length; ++i) {
        this.content(result).appendChild(this.importIntoDoc(childNodes[i]));
      }
    }
    return result;
  }
  isPageRule(rule) {
    return rule.type === CSSRule.PAGE_RULE;
  }
  isStyleRule(rule) {
    return rule.type === CSSRule.STYLE_RULE;
  }
  isMediaRule(rule) {
    return rule.type === CSSRule.MEDIA_RULE;
  }
  isKeyframesRule(rule) {
    return rule.type === CSSRule.KEYFRAMES_RULE;
  }
  getHref(el) {
    return el.href;
  }
  getEventKey(event) {
    var key = event.key;
    if (isBlank(key)) {
      key = event.keyIdentifier;
      if (isBlank(key)) {
        return 'Unidentified';
      }
      if (key.startsWith('U+')) {
        key = String.fromCharCode(parseInt(key.substring(2), 16));
        if (event.location === DOM_KEY_LOCATION_NUMPAD && _chromeNumKeyPadMap.hasOwnProperty(key)) {
          key = _chromeNumKeyPadMap[key];
        }
      }
    }
    if (_keyMap.hasOwnProperty(key)) {
      key = _keyMap[key];
    }
    return key;
  }
  getGlobalEventTarget(target) {
    if (target == "window") {
      return window;
    } else if (target == "document") {
      return document;
    } else if (target == "body") {
      return document.body;
    }
  }
}
Object.defineProperty(BrowserDomAdapter.prototype.query, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.querySelector, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.querySelectorAll, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.nodeName, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.nodeValue, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.type, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.content, "parameters", {get: function() {
    return [[HTMLElement]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.replaceChild, "parameters", {get: function() {
    return [[Node], [], []];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setText, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setValue, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setChecked, "parameters", {get: function() {
    return [[], [assert.type.boolean]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.createTextNode, "parameters", {get: function() {
    return [[assert.type.string], []];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.createScriptTag, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], []];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.createStyleElement, "parameters", {get: function() {
    return [[assert.type.string], []];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.createShadowRoot, "parameters", {get: function() {
    return [[HTMLElement]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getShadowRoot, "parameters", {get: function() {
    return [[HTMLElement]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getHost, "parameters", {get: function() {
    return [[HTMLElement]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.clone, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.hasProperty, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getElementsByClassName, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getElementsByTagName, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.addClass, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.removeClass, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.hasClass, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setStyle, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.removeStyle, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getStyle, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getAttribute, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setAttribute, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.removeAttribute, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.setTitle, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.elementMatches, "parameters", {get: function() {
    return [[], [assert.type.string]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.isTemplateElement, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.isTextNode, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.isCommentNode, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.isElementNode, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.importIntoDoc, "parameters", {get: function() {
    return [[Node]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getHref, "parameters", {get: function() {
    return [[Element]];
  }});
Object.defineProperty(BrowserDomAdapter.prototype.getGlobalEventTarget, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=browser_adapter.es6.map

//# sourceMappingURL=./browser_adapter.map