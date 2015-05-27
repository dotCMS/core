import {CONST,
  normalizeBlank,
  isPresent} from 'angular2/src/facade/lang';
import {ListWrapper,
  List} from 'angular2/src/facade/collection';
import {Injectable} from 'angular2/di';
import {DEFAULT} from 'angular2/change_detection';
export class Directive extends Injectable {
  constructor({selector,
    properties,
    events,
    hostListeners,
    hostProperties,
    lifecycle,
    compileChildren = true} = {}) {
    super();
    this.selector = selector;
    this.properties = properties;
    this.events = events;
    this.hostListeners = hostListeners;
    this.hostProperties = hostProperties;
    this.lifecycle = lifecycle;
    this.compileChildren = compileChildren;
  }
  hasLifecycleHook(hook) {
    return isPresent(this.lifecycle) ? ListWrapper.contains(this.lifecycle, hook) : false;
  }
}
Object.defineProperty(Directive, "annotations", {get: function() {
    return [new CONST()];
  }});
Object.defineProperty(Directive.prototype.hasLifecycleHook, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class Component extends Directive {
  constructor({selector,
    properties,
    events,
    hostListeners,
    hostProperties,
    injectables,
    lifecycle,
    changeDetection = DEFAULT,
    compileChildren = true} = {}) {
    super({
      selector: selector,
      properties: properties,
      events: events,
      hostListeners: hostListeners,
      hostProperties: hostProperties,
      lifecycle: lifecycle,
      compileChildren: compileChildren
    });
    this.changeDetection = changeDetection;
    this.injectables = injectables;
  }
}
Object.defineProperty(Component, "annotations", {get: function() {
    return [new CONST()];
  }});
export const onDestroy = "onDestroy";
export const onChange = "onChange";
export const onAllChangesDone = "onAllChangesDone";
//# sourceMappingURL=annotations.js.map

//# sourceMappingURL=./annotations.map