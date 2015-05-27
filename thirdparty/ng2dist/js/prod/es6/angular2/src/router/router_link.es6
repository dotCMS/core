import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {ElementRef} from 'angular2/core';
import {isPresent} from 'angular2/src/facade/lang';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {Router} from './router';
export class RouterLink {
  constructor(elementRef, router) {
    this._domEl = elementRef.domElement;
    this._router = router;
  }
  set route(changes) {
    this._route = changes;
    this.updateHref();
  }
  set params(changes) {
    this._params = changes;
    this.updateHref();
  }
  updateHref() {
    if (isPresent(this._route) && isPresent(this._params)) {
      var newHref = this._router.generate(this._route, this._params);
      DOM.setAttribute(this._domEl, 'href', newHref);
    }
  }
}
Object.defineProperty(RouterLink, "annotations", {get: function() {
    return [new Directive({
      selector: '[router-link]',
      properties: {
        'route': 'routerLink',
        'params': 'routerParams'
      }
    })];
  }});
Object.defineProperty(RouterLink, "parameters", {get: function() {
    return [[ElementRef], [Router]];
  }});
//# sourceMappingURL=router_link.js.map

//# sourceMappingURL=./router_link.map