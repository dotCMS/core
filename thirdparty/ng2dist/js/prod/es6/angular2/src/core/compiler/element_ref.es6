import {DOM} from 'angular2/src/dom/dom_adapter';
import {normalizeBlank} from 'angular2/src/facade/lang';
import {ViewRef} from './view_ref';
import {DirectDomViewRef} from 'angular2/src/render/dom/direct_dom_renderer';
export class ElementRef {
  constructor(parentView, boundElementIndex) {
    this.parentView = parentView;
    this.boundElementIndex = boundElementIndex;
  }
  get domElement() {
    var renderViewRef = this.parentView.render;
    return renderViewRef.delegate.boundElements[this.boundElementIndex];
  }
  getAttribute(name) {
    return normalizeBlank(DOM.getAttribute(this.domElement, name));
  }
}
Object.defineProperty(ElementRef, "parameters", {get: function() {
    return [[ViewRef], [assert.type.number]];
  }});
Object.defineProperty(ElementRef.prototype.getAttribute, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=element_ref.js.map

//# sourceMappingURL=./element_ref.map