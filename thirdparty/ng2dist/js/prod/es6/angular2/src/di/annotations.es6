import {CONST} from "angular2/src/facade/lang";
export class Inject {
  constructor(token) {
    this.token = token;
  }
}
Object.defineProperty(Inject, "annotations", {get: function() {
    return [new CONST()];
  }});
export class InjectPromise {
  constructor(token) {
    this.token = token;
  }
}
Object.defineProperty(InjectPromise, "annotations", {get: function() {
    return [new CONST()];
  }});
export class InjectLazy {
  constructor(token) {
    this.token = token;
  }
}
Object.defineProperty(InjectLazy, "annotations", {get: function() {
    return [new CONST()];
  }});
export class Optional {
  constructor() {}
}
Object.defineProperty(Optional, "annotations", {get: function() {
    return [new CONST()];
  }});
export class DependencyAnnotation {
  constructor() {}
  get token() {
    return null;
  }
}
Object.defineProperty(DependencyAnnotation, "annotations", {get: function() {
    return [new CONST()];
  }});
export class Injectable {
  constructor() {}
}
Object.defineProperty(Injectable, "annotations", {get: function() {
    return [new CONST()];
  }});
//# sourceMappingURL=annotations.js.map

//# sourceMappingURL=./annotations.map