import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {isPresent} from 'angular2/src/facade/lang';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {ElementRef} from 'angular2/src/core/compiler/element_ref';
export class CSSClass {
  constructor(ngEl) {
    this._domEl = ngEl.domElement;
  }
  _toggleClass(className, enabled) {
    if (enabled) {
      DOM.addClass(this._domEl, className);
    } else {
      DOM.removeClass(this._domEl, className);
    }
  }
  set iterableChanges(changes) {
    if (isPresent(changes)) {
      changes.forEachAddedItem((record) => {
        this._toggleClass(record.key, record.currentValue);
      });
      changes.forEachChangedItem((record) => {
        this._toggleClass(record.key, record.currentValue);
      });
      changes.forEachRemovedItem((record) => {
        if (record.previousValue) {
          DOM.removeClass(this._domEl, record.key);
        }
      });
    }
  }
}
Object.defineProperty(CSSClass, "annotations", {get: function() {
    return [new Directive({
      selector: '[class]',
      properties: {'iterableChanges': 'class | keyValDiff'}
    })];
  }});
Object.defineProperty(CSSClass, "parameters", {get: function() {
    return [[ElementRef]];
  }});
//# sourceMappingURL=class.js.map

//# sourceMappingURL=./class.map