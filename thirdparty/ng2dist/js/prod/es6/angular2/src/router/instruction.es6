import {Map,
  MapWrapper,
  StringMap,
  StringMapWrapper,
  List,
  ListWrapper} from 'angular2/src/facade/collection';
import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {isPresent} from 'angular2/src/facade/lang';
export class RouteParams {
  constructor(params) {
    this.params = params;
  }
  get(param) {
    return StringMapWrapper.get(this.params, param);
  }
}
Object.defineProperty(RouteParams, "parameters", {get: function() {
    return [[StringMap]];
  }});
Object.defineProperty(RouteParams.prototype.get, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class Instruction {
  constructor({params,
    component,
    children,
    matchedUrl} = {}) {
    this.matchedUrl = matchedUrl;
    if (isPresent(children)) {
      this._children = children;
      var childUrl;
      StringMapWrapper.forEach(this._children, (child, _) => {
        childUrl = child.matchedUrl;
      });
      if (isPresent(childUrl)) {
        this.matchedUrl += childUrl;
      }
    } else {
      this._children = StringMapWrapper.create();
    }
    this.component = component;
    this.params = params;
  }
  getChildInstruction(outletName) {
    return StringMapWrapper.get(this._children, outletName);
  }
  forEachChild(fn) {
    StringMapWrapper.forEach(this._children, fn);
  }
  mapChildrenAsync(fn) {
    return mapObjAsync(this._children, fn);
  }
  traverseSync(fn) {
    this.forEachChild((childInstruction, _) => fn(this, childInstruction));
    this.forEachChild((childInstruction, _) => childInstruction.traverseSync(fn));
  }
}
Object.defineProperty(Instruction.prototype.getChildInstruction, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(Instruction.prototype.forEachChild, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(Instruction.prototype.traverseSync, "parameters", {get: function() {
    return [[Function]];
  }});
function mapObjAsync(obj, fn) {
  return PromiseWrapper.all(mapObj(obj, fn));
}
Object.defineProperty(mapObjAsync, "parameters", {get: function() {
    return [[StringMap], []];
  }});
function mapObj(obj, fn) {
  var result = ListWrapper.create();
  StringMapWrapper.forEach(obj, (value, key) => ListWrapper.push(result, fn(value, key)));
  return result;
}
Object.defineProperty(mapObj, "parameters", {get: function() {
    return [[StringMap], []];
  }});
export var noopInstruction = new Instruction();
//# sourceMappingURL=instruction.js.map

//# sourceMappingURL=./instruction.map