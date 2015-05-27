import {ABSTRACT} from 'angular2/src/facade/lang';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {isPresent,
  isFunction} from 'angular2/src/facade/lang';
import {DomAdapter} from './dom_adapter';
export class GenericBrowserDomAdapter extends DomAdapter {
  getDistributedNodes(el) {
    return el.getDistributedNodes();
  }
  resolveAndSetHref(el, baseUrl, href) {
    el.href = href == null ? baseUrl : baseUrl + '/../' + href;
  }
  cssToRules(css) {
    var style = this.createStyleElement(css);
    this.appendChild(this.defaultDoc().head, style);
    var rules = ListWrapper.create();
    if (isPresent(style.sheet)) {
      try {
        var rawRules = style.sheet.cssRules;
        rules = ListWrapper.createFixedSize(rawRules.length);
        for (var i = 0; i < rawRules.length; i++) {
          rules[i] = rawRules[i];
        }
      } catch (e) {}
    } else {}
    this.remove(style);
    return rules;
  }
  supportsDOMEvents() {
    return true;
  }
  supportsNativeShadowDOM() {
    return isFunction(this.defaultDoc().body.createShadowRoot);
  }
}
Object.defineProperty(GenericBrowserDomAdapter, "annotations", {get: function() {
    return [new ABSTRACT()];
  }});
Object.defineProperty(GenericBrowserDomAdapter.prototype.resolveAndSetHref, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(GenericBrowserDomAdapter.prototype.cssToRules, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=generic_browser_adapter.js.map

//# sourceMappingURL=./generic_browser_adapter.map