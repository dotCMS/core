import {isPresent,
  BaseException} from 'angular2/src/facade/lang';
import {ListWrapper,
  MapWrapper} from 'angular2/src/facade/collection';
export class Locals {
  constructor(parent, current) {
    this.parent = parent;
    this.current = current;
  }
  contains(name) {
    if (MapWrapper.contains(this.current, name)) {
      return true;
    }
    if (isPresent(this.parent)) {
      return this.parent.contains(name);
    }
    return false;
  }
  get(name) {
    if (MapWrapper.contains(this.current, name)) {
      return MapWrapper.get(this.current, name);
    }
    if (isPresent(this.parent)) {
      return this.parent.get(name);
    }
    throw new BaseException(`Cannot find '${name}'`);
  }
  set(name, value) {
    if (MapWrapper.contains(this.current, name)) {
      MapWrapper.set(this.current, name, value);
    } else {
      throw new BaseException('Setting of new keys post-construction is not supported.');
    }
  }
  clearValues() {
    MapWrapper.clearValues(this.current);
  }
}
Object.defineProperty(Locals, "parameters", {get: function() {
    return [[Locals], [Map]];
  }});
Object.defineProperty(Locals.prototype.contains, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(Locals.prototype.get, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(Locals.prototype.set, "parameters", {get: function() {
    return [[assert.type.string], []];
  }});
//# sourceMappingURL=locals.js.map

//# sourceMappingURL=./locals.map