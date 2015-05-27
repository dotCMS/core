import {Injectable} from 'angular2/di';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {Map,
  MapWrapper,
  List,
  ListWrapper} from 'angular2/src/facade/collection';
import {StringWrapper,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import * as getTestabilityModule from 'angular2/src/core/testability/get_testability';
export class Testability {
  constructor() {
    this._pendingCount = 0;
    this._callbacks = ListWrapper.create();
  }
  increaseCount(delta = 1) {
    this._pendingCount += delta;
    if (this._pendingCount < 0) {
      throw new BaseException('pending async requests below zero');
    } else if (this._pendingCount == 0) {
      this._runCallbacks();
    }
    return this._pendingCount;
  }
  _runCallbacks() {
    while (this._callbacks.length !== 0) {
      ListWrapper.removeLast(this._callbacks)();
    }
  }
  whenStable(callback) {
    ListWrapper.push(this._callbacks, callback);
    if (this._pendingCount === 0) {
      this._runCallbacks();
    }
  }
  getPendingCount() {
    return this._pendingCount;
  }
  findBindings(using, binding, exactMatch) {
    return [];
  }
}
Object.defineProperty(Testability, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(Testability.prototype.increaseCount, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(Testability.prototype.whenStable, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(Testability.prototype.findBindings, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.boolean]];
  }});
export class TestabilityRegistry {
  constructor() {
    this._applications = MapWrapper.create();
    getTestabilityModule.GetTestability.addToWindow(this);
  }
  registerApplication(token, testability) {
    MapWrapper.set(this._applications, token, testability);
  }
  findTestabilityInTree(elem) {
    if (elem == null) {
      return null;
    }
    if (MapWrapper.contains(this._applications, elem)) {
      return MapWrapper.get(this._applications, elem);
    }
    if (DOM.isShadowRoot(elem)) {
      return this.findTestabilityInTree(DOM.getHost(elem));
    }
    return this.findTestabilityInTree(DOM.parentElement(elem));
  }
}
Object.defineProperty(TestabilityRegistry, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(TestabilityRegistry.prototype.registerApplication, "parameters", {get: function() {
    return [[], [Testability]];
  }});
//# sourceMappingURL=testability.js.map

//# sourceMappingURL=./testability.map