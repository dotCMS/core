import {List,
  MapWrapper,
  ListWrapper} from 'angular2/src/facade/collection';
import {Directive} from 'angular2/src/core/annotations_impl/annotations';
export class BaseQueryList {
  constructor() {
    this._results = [];
    this._callbacks = [];
    this._dirty = false;
  }
  [Symbol.iterator]() {
    return this._results[Symbol.iterator]();
  }
  reset(newList) {
    this._results = newList;
    this._dirty = true;
  }
  add(obj) {
    ListWrapper.push(this._results, obj);
    this._dirty = true;
  }
  fireCallbacks() {
    if (this._dirty) {
      ListWrapper.forEach(this._callbacks, (c) => c());
      this._dirty = false;
    }
  }
  onChange(callback) {
    ListWrapper.push(this._callbacks, callback);
  }
  removeCallback(callback) {
    ListWrapper.remove(this._callbacks, callback);
  }
}
//# sourceMappingURL=base_query_list.es6.map

//# sourceMappingURL=./base_query_list.map