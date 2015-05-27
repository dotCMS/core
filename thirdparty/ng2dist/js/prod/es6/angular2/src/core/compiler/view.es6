import {ListWrapper,
  MapWrapper,
  Map,
  StringMapWrapper,
  List} from 'angular2/src/facade/collection';
import {AST,
  Locals,
  ChangeDispatcher,
  ProtoChangeDetector,
  ChangeDetector,
  ChangeRecord,
  BindingRecord,
  DirectiveRecord,
  DirectiveIndex,
  ChangeDetectorRef} from 'angular2/change_detection';
import {ProtoElementInjector,
  ElementInjector,
  PreBuiltObjects,
  DirectiveBinding} from './element_injector';
import {ElementBinder} from './element_binder';
import {IMPLEMENTS,
  int,
  isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import * as renderApi from 'angular2/src/render/api';
export class AppViewContainer {
  constructor() {
    this.views = [];
  }
}
export class AppView {
  constructor(renderer, proto, protoLocals) {
    this.render = null;
    this.proto = proto;
    this.changeDetector = null;
    this.elementInjectors = null;
    this.rootElementInjectors = null;
    this.componentChildViews = null;
    this.viewContainers = ListWrapper.createFixedSize(this.proto.elementBinders.length);
    this.preBuiltObjects = null;
    this.context = null;
    this.locals = new Locals(null, MapWrapper.clone(protoLocals));
    this.renderer = renderer;
    this.imperativeHostViews = [];
  }
  init(changeDetector, elementInjectors, rootElementInjectors, preBuiltObjects, componentChildViews) {
    this.changeDetector = changeDetector;
    this.elementInjectors = elementInjectors;
    this.rootElementInjectors = rootElementInjectors;
    this.preBuiltObjects = preBuiltObjects;
    this.componentChildViews = componentChildViews;
  }
  setLocal(contextName, value) {
    if (!this.hydrated())
      throw new BaseException('Cannot set locals on dehydrated view.');
    if (!MapWrapper.contains(this.proto.variableBindings, contextName)) {
      return ;
    }
    var templateName = MapWrapper.get(this.proto.variableBindings, contextName);
    this.locals.set(templateName, value);
  }
  hydrated() {
    return isPresent(this.context);
  }
  triggerEventHandlers(eventName, eventObj, binderIndex) {
    var locals = MapWrapper.create();
    MapWrapper.set(locals, '$event', eventObj);
    this.dispatchEvent(binderIndex, eventName, locals);
  }
  notifyOnBinding(b, currentValue) {
    if (b.isElement()) {
      this.renderer.setElementProperty(this.render, b.elementIndex, b.propertyName, currentValue);
    } else {
      this.renderer.setText(this.render, b.elementIndex, currentValue);
    }
  }
  getDirectiveFor(directive) {
    var elementInjector = this.elementInjectors[directive.elementIndex];
    return elementInjector.getDirectiveAtIndex(directive.directiveIndex);
  }
  getDetectorFor(directive) {
    var childView = this.componentChildViews[directive.elementIndex];
    return isPresent(childView) ? childView.changeDetector : null;
  }
  dispatchEvent(elementIndex, eventName, locals) {
    var allowDefaultBehavior = true;
    if (this.hydrated()) {
      var elBinder = this.proto.elementBinders[elementIndex];
      if (isBlank(elBinder.hostListeners))
        return allowDefaultBehavior;
      var eventMap = elBinder.hostListeners[eventName];
      if (isBlank(eventMap))
        return allowDefaultBehavior;
      MapWrapper.forEach(eventMap, (expr, directiveIndex) => {
        var context;
        if (directiveIndex === -1) {
          context = this.context;
        } else {
          context = this.elementInjectors[elementIndex].getDirectiveAtIndex(directiveIndex);
        }
        var result = expr.eval(context, new Locals(this.locals, locals));
        if (isPresent(result)) {
          allowDefaultBehavior = allowDefaultBehavior && result;
        }
      });
    }
    return allowDefaultBehavior;
  }
}
Object.defineProperty(AppView, "annotations", {get: function() {
    return [new IMPLEMENTS(ChangeDispatcher)];
  }});
Object.defineProperty(AppView, "parameters", {get: function() {
    return [[renderApi.Renderer], [AppProtoView], [Map]];
  }});
Object.defineProperty(AppView.prototype.init, "parameters", {get: function() {
    return [[ChangeDetector], [List], [List], [List], [List]];
  }});
Object.defineProperty(AppView.prototype.setLocal, "parameters", {get: function() {
    return [[assert.type.string], []];
  }});
Object.defineProperty(AppView.prototype.triggerEventHandlers, "parameters", {get: function() {
    return [[assert.type.string], [], [int]];
  }});
Object.defineProperty(AppView.prototype.notifyOnBinding, "parameters", {get: function() {
    return [[BindingRecord], [assert.type.any]];
  }});
Object.defineProperty(AppView.prototype.getDirectiveFor, "parameters", {get: function() {
    return [[DirectiveIndex]];
  }});
Object.defineProperty(AppView.prototype.getDetectorFor, "parameters", {get: function() {
    return [[DirectiveIndex]];
  }});
Object.defineProperty(AppView.prototype.dispatchEvent, "parameters", {get: function() {
    return [[assert.type.number], [assert.type.string], [assert.genericType(Map, assert.type.string, assert.type.any)]];
  }});
export class AppProtoView {
  constructor(render, protoChangeDetector, variableBindings, protoLocals, variableNames) {
    this.render = render;
    this.elementBinders = [];
    this.variableBindings = variableBindings;
    this.protoLocals = protoLocals;
    this.variableNames = variableNames;
    this.protoChangeDetector = protoChangeDetector;
  }
  bindElement(parent, distanceToParent, protoElementInjector, componentDirective = null) {
    var elBinder = new ElementBinder(this.elementBinders.length, parent, distanceToParent, protoElementInjector, componentDirective);
    ListWrapper.push(this.elementBinders, elBinder);
    return elBinder;
  }
  bindEvent(eventBindings, boundElementIndex, directiveIndex = -1) {
    var elBinder = this.elementBinders[boundElementIndex];
    var events = elBinder.hostListeners;
    if (isBlank(events)) {
      events = StringMapWrapper.create();
      elBinder.hostListeners = events;
    }
    for (var i = 0; i < eventBindings.length; i++) {
      var eventBinding = eventBindings[i];
      var eventName = eventBinding.fullName;
      var event = StringMapWrapper.get(events, eventName);
      if (isBlank(event)) {
        event = MapWrapper.create();
        StringMapWrapper.set(events, eventName, event);
      }
      MapWrapper.set(event, directiveIndex, eventBinding.source);
    }
  }
}
Object.defineProperty(AppProtoView, "parameters", {get: function() {
    return [[renderApi.RenderProtoViewRef], [ProtoChangeDetector], [Map], [Map], [List]];
  }});
Object.defineProperty(AppProtoView.prototype.bindElement, "parameters", {get: function() {
    return [[ElementBinder], [int], [ProtoElementInjector], [DirectiveBinding]];
  }});
Object.defineProperty(AppProtoView.prototype.bindEvent, "parameters", {get: function() {
    return [[assert.genericType(List, renderApi.EventBinding)], [assert.type.number], [int]];
  }});
//# sourceMappingURL=view.js.map

//# sourceMappingURL=./view.map