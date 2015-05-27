import {Promise} from 'angular2/src/facade/async';
import {DOM} from 'angular2/src/dom/dom_adapter';
import * as viewModule from '../view/view';
import {StyleUrlResolver} from './style_url_resolver';
import {ShadowDomStrategy} from './shadow_dom_strategy';
import {moveViewNodesIntoParent} from './util';
export class NativeShadowDomStrategy extends ShadowDomStrategy {
  constructor(styleUrlResolver) {
    super();
    this.styleUrlResolver = styleUrlResolver;
  }
  attachTemplate(el, view) {
    moveViewNodesIntoParent(DOM.createShadowRoot(el), view);
  }
  processStyleElement(hostComponentId, templateUrl, styleEl) {
    var cssText = DOM.getText(styleEl);
    cssText = this.styleUrlResolver.resolveUrls(cssText, templateUrl);
    DOM.setText(styleEl, cssText);
    return null;
  }
}
Object.defineProperty(NativeShadowDomStrategy, "parameters", {get: function() {
    return [[StyleUrlResolver]];
  }});
Object.defineProperty(NativeShadowDomStrategy.prototype.attachTemplate, "parameters", {get: function() {
    return [[], [viewModule.RenderView]];
  }});
Object.defineProperty(NativeShadowDomStrategy.prototype.processStyleElement, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], []];
  }});
//# sourceMappingURL=native_shadow_dom_strategy.js.map

//# sourceMappingURL=./native_shadow_dom_strategy.map