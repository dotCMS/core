import {CONST} from 'angular2/src/facade/lang';
import {DependencyAnnotation} from 'angular2/di';
export class Parent extends DependencyAnnotation {
  constructor() {
    super();
  }
}
Object.defineProperty(Parent, "annotations", {get: function() {
    return [new CONST()];
  }});
export class Ancestor extends DependencyAnnotation {
  constructor() {
    super();
  }
}
Object.defineProperty(Ancestor, "annotations", {get: function() {
    return [new CONST()];
  }});
//# sourceMappingURL=visibility.js.map

//# sourceMappingURL=./visibility.map