import {Promise} from 'angular2/src/facade/async';
import {DOM} from 'angular2/src/dom/dom_adapter';
import * as viewModule from '../view/view';
import {LightDom} from './light_dom';
import {ShadowDomStrategy} from './shadow_dom_strategy';
import {StyleUrlResolver} from './style_url_resolver';
import {moveViewNodesIntoParent} from './util';
import {insertSharedStyleText} from './util';
export class EmulatedUnscopedShadowDomStrategy extends ShadowDomStrategy {
  constructor(styleUrlResolver, styleHost) {
    super();
    this.styleUrlResolver = styleUrlResolver;
    this.styleHost = styleHost;
  }
  hasNativeContentElement() {
    return false;
  }
  attachTemplate(el, view) {
    moveViewNodesIntoParent(el, view);
  }
  constructLightDom(lightDomView, shadowDomView, el) {
    return new LightDom(lightDomView, shadowDomView, el);
  }
  processStyleElement(hostComponentId, templateUrl, styleEl) {
    var cssText = DOM.getText(styleEl);
    cssText = this.styleUrlResolver.resolveUrls(cssText, templateUrl);
    DOM.setText(styleEl, cssText);
    DOM.remove(styleEl);
    insertSharedStyleText(cssText, this.styleHost, styleEl);
    return null;
  }
}
Object.defineProperty(EmulatedUnscopedShadowDomStrategy, "parameters", {get: function() {
    return [[StyleUrlResolver], []];
  }});
Object.defineProperty(EmulatedUnscopedShadowDomStrategy.prototype.attachTemplate, "parameters", {get: function() {
    return [[], [viewModule.RenderView]];
  }});
Object.defineProperty(EmulatedUnscopedShadowDomStrategy.prototype.constructLightDom, "parameters", {get: function() {
    return [[viewModule.RenderView], [viewModule.RenderView], []];
  }});
Object.defineProperty(EmulatedUnscopedShadowDomStrategy.prototype.processStyleElement, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], []];
  }});
//# sourceMappingURL=emulated_unscoped_shadow_dom_strategy.js.map

//# sourceMappingURL=./emulated_unscoped_shadow_dom_strategy.map