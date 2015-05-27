import {Directive} from 'angular2/src/core/annotations_impl/annotations';
export class NonBindable {}
Object.defineProperty(NonBindable, "annotations", {get: function() {
    return [new Directive({
      selector: '[non-bindable]',
      compileChildren: false
    })];
  }});
//# sourceMappingURL=non_bindable.js.map

//# sourceMappingURL=./non_bindable.map