import {SpyObject,
  proxy} from 'angular2/test_lib';
import {isBlank,
  isPresent,
  IMPLEMENTS} from 'angular2/src/facade/lang';
import {EventEmitter,
  ObservableWrapper} from 'angular2/src/facade/async';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {Location} from 'angular2/src/router/location';
export class SpyLocation extends SpyObject {
  constructor() {
    super();
    this._path = '/';
    this.urlChanges = ListWrapper.create();
    this._subject = new EventEmitter();
  }
  setInitialPath(url) {
    this._path = url;
  }
  path() {
    return this._path;
  }
  simulateUrlPop(pathname) {
    ObservableWrapper.callNext(this._subject, {'url': pathname});
  }
  go(url) {
    if (this._path === url) {
      return ;
    }
    this._path = url;
    ListWrapper.push(this.urlChanges, url);
  }
  forward() {}
  back() {}
  subscribe(onNext, onThrow = null, onReturn = null) {
    ObservableWrapper.subscribe(this._subject, onNext, onThrow, onReturn);
  }
  noSuchMethod(m) {
    return super.noSuchMethod(m);
  }
}
Object.defineProperty(SpyLocation, "annotations", {get: function() {
    return [new proxy, new IMPLEMENTS(Location)];
  }});
Object.defineProperty(SpyLocation.prototype.setInitialPath, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(SpyLocation.prototype.simulateUrlPop, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(SpyLocation.prototype.go, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=location_mock.js.map

//# sourceMappingURL=./location_mock.map