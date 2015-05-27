import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {DomAdapter} from 'angular2/src/dom/dom_adapter';
import {ElementRef} from 'angular2/src/core/compiler/element_ref';
export class Rectangle {
  constructor(left, top, width, height) {
    this.left = left;
    this.right = left + width;
    this.top = top;
    this.bottom = top + height;
    this.height = height;
    this.width = width;
  }
}
export class Ruler {
  constructor(domAdapter) {
    this.domAdapter = domAdapter;
  }
  measure(el) {
    var clntRect = this.domAdapter.getBoundingClientRect(el.domElement);
    return PromiseWrapper.resolve(new Rectangle(clntRect.left, clntRect.top, clntRect.width, clntRect.height));
  }
}
Object.defineProperty(Ruler, "parameters", {get: function() {
    return [[DomAdapter]];
  }});
Object.defineProperty(Ruler.prototype.measure, "parameters", {get: function() {
    return [[ElementRef]];
  }});
//# sourceMappingURL=ruler.js.map

//# sourceMappingURL=./ruler.map