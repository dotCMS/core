import {isBlank,
  isPresent} from 'angular2/src/facade/lang';
import {PromiseWrapper,
  Promise} from 'angular2/src/facade/async';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {StyleInliner} from 'angular2/src/render/dom/shadow_dom/style_inliner';
import {StyleUrlResolver} from 'angular2/src/render/dom/shadow_dom/style_url_resolver';
import {EmulatedUnscopedShadowDomStrategy} from './emulated_unscoped_shadow_dom_strategy';
import {getContentAttribute,
  getHostAttribute,
  getComponentId,
  shimCssForComponent,
  insertStyleElement} from './util';
export class EmulatedScopedShadowDomStrategy extends EmulatedUnscopedShadowDomStrategy {
  constructor(styleInliner, styleUrlResolver, styleHost) {
    super(styleUrlResolver, styleHost);
    this.styleInliner = styleInliner;
  }
  processStyleElement(hostComponentId, templateUrl, styleEl) {
    var cssText = DOM.getText(styleEl);
    cssText = this.styleUrlResolver.resolveUrls(cssText, templateUrl);
    var css = this.styleInliner.inlineImports(cssText, templateUrl);
    if (PromiseWrapper.isPromise(css)) {
      DOM.setText(styleEl, '');
      return css.then((css) => {
        css = shimCssForComponent(css, hostComponentId);
        DOM.setText(styleEl, css);
      });
    } else {
      css = shimCssForComponent(css, hostComponentId);
      DOM.setText(styleEl, css);
    }
    DOM.remove(styleEl);
    insertStyleElement(this.styleHost, styleEl);
    return null;
  }
  processElement(hostComponentId, elementComponentId, element) {
    if (isPresent(hostComponentId)) {
      var contentAttribute = getContentAttribute(getComponentId(hostComponentId));
      DOM.setAttribute(element, contentAttribute, '');
    }
    if (isPresent(elementComponentId)) {
      var hostAttribute = getHostAttribute(getComponentId(elementComponentId));
      DOM.setAttribute(element, hostAttribute, '');
    }
  }
}
Object.defineProperty(EmulatedScopedShadowDomStrategy, "parameters", {get: function() {
    return [[StyleInliner], [StyleUrlResolver], []];
  }});
Object.defineProperty(EmulatedScopedShadowDomStrategy.prototype.processStyleElement, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], []];
  }});
Object.defineProperty(EmulatedScopedShadowDomStrategy.prototype.processElement, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], []];
  }});
//# sourceMappingURL=emulated_scoped_shadow_dom_strategy.js.map

//# sourceMappingURL=./emulated_scoped_shadow_dom_strategy.map