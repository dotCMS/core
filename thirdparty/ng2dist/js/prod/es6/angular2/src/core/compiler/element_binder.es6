import {int,
  isBlank,
  isPresent,
  BaseException} from 'angular2/src/facade/lang';
import * as eiModule from './element_injector';
import {DirectiveBinding} from './element_injector';
import {List,
  StringMap} from 'angular2/src/facade/collection';
import * as viewModule from './view';
export class ElementBinder {
  constructor(index, parent, distanceToParent, protoElementInjector, componentDirective) {
    if (isBlank(index)) {
      throw new BaseException('null index not allowed.');
    }
    this.protoElementInjector = protoElementInjector;
    this.componentDirective = componentDirective;
    this.parent = parent;
    this.index = index;
    this.distanceToParent = distanceToParent;
    this.hostListeners = null;
    this.nestedProtoView = null;
  }
  hasStaticComponent() {
    return isPresent(this.componentDirective) && isPresent(this.nestedProtoView);
  }
  hasDynamicComponent() {
    return isPresent(this.componentDirective) && isBlank(this.nestedProtoView);
  }
  hasEmbeddedProtoView() {
    return !isPresent(this.componentDirective) && isPresent(this.nestedProtoView);
  }
}
Object.defineProperty(ElementBinder, "parameters", {get: function() {
    return [[int], [ElementBinder], [int], [eiModule.ProtoElementInjector], [DirectiveBinding]];
  }});
//# sourceMappingURL=element_binder.js.map

//# sourceMappingURL=./element_binder.map