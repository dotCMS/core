import {Injectable} from 'angular2/di';
import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {isBlank,
  isPresent,
  BaseException} from 'angular2/src/facade/lang';
import * as api from '../api';
import {RenderView} from './view/view';
import {RenderProtoView} from './view/proto_view';
import {ViewFactory} from './view/view_factory';
import {RenderViewHydrator} from './view/view_hydrator';
import {Compiler} from './compiler/compiler';
import {ShadowDomStrategy} from './shadow_dom/shadow_dom_strategy';
import {ProtoViewBuilder} from './view/proto_view_builder';
import {ViewContainer} from './view/view_container';
function _resolveViewContainer(vc) {
  return _resolveView(vc.view).getOrCreateViewContainer(vc.elementIndex);
}
Object.defineProperty(_resolveViewContainer, "parameters", {get: function() {
    return [[api.RenderViewContainerRef]];
  }});
function _resolveView(viewRef) {
  return isPresent(viewRef) ? viewRef.delegate : null;
}
Object.defineProperty(_resolveView, "parameters", {get: function() {
    return [[DirectDomViewRef]];
  }});
function _resolveProtoView(protoViewRef) {
  return isPresent(protoViewRef) ? protoViewRef.delegate : null;
}
Object.defineProperty(_resolveProtoView, "parameters", {get: function() {
    return [[DirectDomProtoViewRef]];
  }});
function _wrapView(view) {
  return new DirectDomViewRef(view);
}
Object.defineProperty(_wrapView, "parameters", {get: function() {
    return [[RenderView]];
  }});
function _collectComponentChildViewRefs(view, target = null) {
  if (isBlank(target)) {
    target = [];
  }
  ListWrapper.push(target, _wrapView(view));
  ListWrapper.forEach(view.componentChildViews, (view) => {
    if (isPresent(view)) {
      _collectComponentChildViewRefs(view, target);
    }
  });
  return target;
}
export class DirectDomProtoViewRef extends api.RenderProtoViewRef {
  constructor(delegate) {
    super();
    this.delegate = delegate;
  }
}
Object.defineProperty(DirectDomProtoViewRef, "parameters", {get: function() {
    return [[RenderProtoView]];
  }});
export class DirectDomViewRef extends api.RenderViewRef {
  constructor(delegate) {
    super();
    this.delegate = delegate;
  }
}
Object.defineProperty(DirectDomViewRef, "parameters", {get: function() {
    return [[RenderView]];
  }});
export class DirectDomRenderer extends api.Renderer {
  constructor(compiler, viewFactory, viewHydrator, shadowDomStrategy) {
    super();
    this._compiler = compiler;
    this._viewFactory = viewFactory;
    this._viewHydrator = viewHydrator;
    this._shadowDomStrategy = shadowDomStrategy;
  }
  createHostProtoView(directiveMetadata) {
    return this._compiler.compileHost(directiveMetadata);
  }
  createImperativeComponentProtoView(rendererId) {
    var protoViewBuilder = new ProtoViewBuilder(null);
    protoViewBuilder.setImperativeRendererId(rendererId);
    return PromiseWrapper.resolve(protoViewBuilder.build());
  }
  compile(view) {
    return this._compiler.compile(view);
  }
  mergeChildComponentProtoViews(protoViewRef, protoViewRefs) {
    _resolveProtoView(protoViewRef).mergeChildComponentProtoViews(ListWrapper.map(protoViewRefs, _resolveProtoView));
  }
  createViewInContainer(vcRef, atIndex, protoViewRef) {
    var view = this._viewFactory.getView(_resolveProtoView(protoViewRef));
    var vc = _resolveViewContainer(vcRef);
    this._viewHydrator.hydrateViewInViewContainer(vc, view);
    vc.insert(view, atIndex);
    return _collectComponentChildViewRefs(view);
  }
  destroyViewInContainer(vcRef, atIndex) {
    var vc = _resolveViewContainer(vcRef);
    var view = vc.detach(atIndex);
    this._viewHydrator.dehydrateViewInViewContainer(vc, view);
    this._viewFactory.returnView(view);
  }
  insertViewIntoContainer(vcRef, atIndex = -1, viewRef) {
    _resolveViewContainer(vcRef).insert(_resolveView(viewRef), atIndex);
  }
  detachViewFromContainer(vcRef, atIndex) {
    _resolveViewContainer(vcRef).detach(atIndex);
  }
  createDynamicComponentView(hostViewRef, elementIndex, componentViewRef) {
    var hostView = _resolveView(hostViewRef);
    var componentView = this._viewFactory.getView(_resolveProtoView(componentViewRef));
    this._viewHydrator.hydrateDynamicComponentView(hostView, elementIndex, componentView);
    return _collectComponentChildViewRefs(componentView);
  }
  destroyDynamicComponentView(hostViewRef, elementIndex) {
    throw new BaseException('Not supported yet');
  }
  createInPlaceHostView(parentViewRef, hostElementSelector, hostProtoViewRef) {
    var parentView = _resolveView(parentViewRef);
    var hostView = this._viewFactory.createInPlaceHostView(hostElementSelector, _resolveProtoView(hostProtoViewRef));
    this._viewHydrator.hydrateInPlaceHostView(parentView, hostView);
    return _collectComponentChildViewRefs(hostView);
  }
  destroyInPlaceHostView(parentViewRef, hostViewRef) {
    var parentView = _resolveView(parentViewRef);
    var hostView = _resolveView(hostViewRef);
    this._viewHydrator.dehydrateInPlaceHostView(parentView, hostView);
  }
  setImperativeComponentRootNodes(parentViewRef, elementIndex, nodes) {
    var parentView = _resolveView(parentViewRef);
    var hostElement = parentView.boundElements[elementIndex];
    var componentView = parentView.componentChildViews[elementIndex];
    if (isBlank(componentView)) {
      throw new BaseException(`There is no componentChildView at index ${elementIndex}`);
    }
    if (isBlank(componentView.proto.imperativeRendererId)) {
      throw new BaseException(`This component view has no imperative renderer`);
    }
    ViewContainer.removeViewNodes(componentView);
    componentView.rootNodes = nodes;
    this._shadowDomStrategy.attachTemplate(hostElement, componentView);
  }
  setElementProperty(viewRef, elementIndex, propertyName, propertyValue) {
    _resolveView(viewRef).setElementProperty(elementIndex, propertyName, propertyValue);
  }
  setText(viewRef, textNodeIndex, text) {
    _resolveView(viewRef).setText(textNodeIndex, text);
  }
  setEventDispatcher(viewRef, dispatcher) {
    _resolveView(viewRef).setEventDispatcher(dispatcher);
  }
}
Object.defineProperty(DirectDomRenderer, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(DirectDomRenderer, "parameters", {get: function() {
    return [[Compiler], [ViewFactory], [RenderViewHydrator], [ShadowDomStrategy]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.createHostProtoView, "parameters", {get: function() {
    return [[api.DirectiveMetadata]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.compile, "parameters", {get: function() {
    return [[api.ViewDefinition]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.mergeChildComponentProtoViews, "parameters", {get: function() {
    return [[api.RenderProtoViewRef], [assert.genericType(List, api.RenderProtoViewRef)]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.createViewInContainer, "parameters", {get: function() {
    return [[api.RenderViewContainerRef], [assert.type.number], [api.RenderProtoViewRef]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.destroyViewInContainer, "parameters", {get: function() {
    return [[api.RenderViewContainerRef], [assert.type.number]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.insertViewIntoContainer, "parameters", {get: function() {
    return [[api.RenderViewContainerRef], [], [api.RenderViewRef]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.detachViewFromContainer, "parameters", {get: function() {
    return [[api.RenderViewContainerRef], [assert.type.number]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.createDynamicComponentView, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.number], [api.RenderProtoViewRef]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.destroyDynamicComponentView, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.number]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.createInPlaceHostView, "parameters", {get: function() {
    return [[api.RenderViewRef], [], [api.RenderProtoViewRef]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.destroyInPlaceHostView, "parameters", {get: function() {
    return [[api.RenderViewRef], [api.RenderViewRef]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.setImperativeComponentRootNodes, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.number], [List]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.setElementProperty, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.number], [assert.type.string], [assert.type.any]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.setText, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.number], [assert.type.string]];
  }});
Object.defineProperty(DirectDomRenderer.prototype.setEventDispatcher, "parameters", {get: function() {
    return [[api.RenderViewRef], [assert.type.any]];
  }});
//# sourceMappingURL=direct_dom_renderer.js.map

//# sourceMappingURL=./direct_dom_renderer.map