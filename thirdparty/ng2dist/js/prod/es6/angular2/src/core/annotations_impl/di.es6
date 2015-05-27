import {CONST} from 'angular2/src/facade/lang';
import {DependencyAnnotation} from 'angular2/di';
export class Attribute extends DependencyAnnotation {
  constructor(attributeName) {
    super();
    this.attributeName = attributeName;
  }
  get token() {
    return this;
  }
}
Object.defineProperty(Attribute, "annotations", {get: function() {
    return [new CONST()];
  }});
export class Query extends DependencyAnnotation {
  constructor(directive) {
    super();
    this.directive = directive;
  }
}
Object.defineProperty(Query, "annotations", {get: function() {
    return [new CONST()];
  }});
//# sourceMappingURL=di.js.map

//# sourceMappingURL=./di.map