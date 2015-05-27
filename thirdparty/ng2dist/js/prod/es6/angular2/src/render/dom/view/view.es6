import {DOM} from 'angular2/src/dom/dom_adapter';
import {ListWrapper,
  MapWrapper,
  Map,
  StringMapWrapper,
  List} from 'angular2/src/facade/collection';
import {int,
  isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import {ViewContainer} from './view_container';
import {RenderProtoView} from './proto_view';
import {LightDom} from '../shadow_dom/light_dom';
import {Content} from '../shadow_dom/content_tag';
const NG_BINDING_CLASS = 'ng-binding';
export class RenderView {
  constructor(proto, rootNodes, boundTextNodes, boundElements, contentTags) {
    this.proto = proto;
    this.rootNodes = rootNodes;
    this.boundTextNodes = boundTextNodes;
    this.boundElements = boundElements;
    this.viewContainers = ListWrapper.createFixedSize(boundElements.length);
    this.contentTags = contentTags;
    this.lightDoms = ListWrapper.createFixedSize(boundElements.length);
    ListWrapper.fill(this.lightDoms, null);
    this.componentChildViews = ListWrapper.createFixedSize(boundElements.length);
    this.hostLightDom = null;
    this.hydrated = false;
    this.eventHandlerRemovers = [];
    this.imperativeHostViews = [];
  }
  getDirectParentLightDom(boundElementIndex) {
    var binder = this.proto.elementBinders[boundElementIndex];
    var destLightDom = null;
    if (binder.parentIndex !== -1 && binder.distanceToParent === 1) {
      destLightDom = this.lightDoms[binder.parentIndex];
    }
    return destLightDom;
  }
  getOrCreateViewContainer(binderIndex) {
    var vc = this.viewContainers[binderIndex];
    if (isBlank(vc)) {
      vc = new ViewContainer(this, binderIndex);
      this.viewContainers[binderIndex] = vc;
    }
    return vc;
  }
  setElementProperty(elementIndex, propertyName, value) {
    var setter = MapWrapper.get(this.proto.elementBinders[elementIndex].propertySetters, propertyName);
    setter(this.boundElements[elementIndex], value);
  }
  setText(textIndex, value) {
    DOM.setText(this.boundTextNodes[textIndex], value);
  }
  getViewContainer(index) {
    return this.viewContainers[index];
  }
  setEventDispatcher(dispatcher) {
    this._eventDispatcher = dispatcher;
  }
  dispatchEvent(elementIndex, eventName, event) {
    var allowDefaultBehavior = true;
    if (isPresent(this._eventDispatcher)) {
      var evalLocals = MapWrapper.create();
      MapWrapper.set(evalLocals, '$event', event);
      allowDefaultBehavior = this._eventDispatcher.dispatchEvent(elementIndex, eventName, evalLocals);
      if (!allowDefaultBehavior) {
        event.preventDefault();
      }
    }
    return allowDefaultBehavior;
  }
}
Object.defineProperty(RenderView, "parameters", {get: function() {
    return [[RenderProtoView], [List], [List], [List], [List]];
  }});
Object.defineProperty(RenderView.prototype.getDirectParentLightDom, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(RenderView.prototype.setElementProperty, "parameters", {get: function() {
    return [[assert.type.number], [assert.type.string], [assert.type.any]];
  }});
Object.defineProperty(RenderView.prototype.setText, "parameters", {get: function() {
    return [[assert.type.number], [assert.type.string]];
  }});
Object.defineProperty(RenderView.prototype.getViewContainer, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(RenderView.prototype.setEventDispatcher, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
//# sourceMappingURL=view.js.map

//# sourceMappingURL=./view.map