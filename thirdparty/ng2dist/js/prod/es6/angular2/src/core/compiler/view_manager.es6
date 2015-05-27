import {Injector,
  Injectable,
  Binding} from 'angular2/di';
import {ListWrapper,
  MapWrapper,
  Map,
  StringMapWrapper,
  List} from 'angular2/src/facade/collection';
import {isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import * as viewModule from './view';
import {ElementRef} from './element_ref';
import {ProtoViewRef,
  ViewRef,
  internalView,
  internalProtoView} from './view_ref';
import {ViewContainerRef} from './view_container_ref';
import {Renderer,
  RenderViewRef,
  RenderViewContainerRef} from 'angular2/src/render/api';
import {AppViewManagerUtils} from './view_manager_utils';
import {AppViewPool} from './view_pool';
export class AppViewManager {
  constructor(viewPool, utils, renderer) {
    this._renderer = renderer;
    this._viewPool = viewPool;
    this._utils = utils;
  }
  getViewContainer(location) {
    var hostView = internalView(location.parentView);
    return hostView.elementInjectors[location.boundElementIndex].getViewContainerRef();
  }
  getComponent(hostLocation) {
    var hostView = internalView(hostLocation.parentView);
    var boundElementIndex = hostLocation.boundElementIndex;
    return this._utils.getComponentInstance(hostView, boundElementIndex);
  }
  createDynamicComponentView(hostLocation, componentProtoViewRef, componentBinding, injector) {
    var componentProtoView = internalProtoView(componentProtoViewRef);
    var hostView = internalView(hostLocation.parentView);
    var boundElementIndex = hostLocation.boundElementIndex;
    var binder = hostView.proto.elementBinders[boundElementIndex];
    if (!binder.hasDynamicComponent()) {
      throw new BaseException(`There is no dynamic component directive at element ${boundElementIndex}`);
    }
    var componentView = this._createViewRecurse(componentProtoView);
    var renderViewRefs = this._renderer.createDynamicComponentView(hostView.render, boundElementIndex, componentProtoView.render);
    componentView.render = renderViewRefs[0];
    this._utils.attachComponentView(hostView, boundElementIndex, componentView);
    this._utils.hydrateDynamicComponentInElementInjector(hostView, boundElementIndex, componentBinding, injector);
    this._utils.hydrateComponentView(hostView, boundElementIndex);
    this._viewHydrateRecurse(componentView, renderViewRefs, 1);
    return new ViewRef(componentView);
  }
  createInPlaceHostView(parentComponentLocation, hostElementSelector, hostProtoViewRef, injector) {
    var hostProtoView = internalProtoView(hostProtoViewRef);
    var parentComponentHostView = null;
    var parentComponentBoundElementIndex = null;
    var parentRenderViewRef = null;
    if (isPresent(parentComponentLocation)) {
      parentComponentHostView = internalView(parentComponentLocation.parentView);
      parentComponentBoundElementIndex = parentComponentLocation.boundElementIndex;
      parentRenderViewRef = parentComponentHostView.componentChildViews[parentComponentBoundElementIndex].render;
    }
    var hostView = this._createViewRecurse(hostProtoView);
    var renderViewRefs = this._renderer.createInPlaceHostView(parentRenderViewRef, hostElementSelector, hostProtoView.render);
    hostView.render = renderViewRefs[0];
    this._utils.attachAndHydrateInPlaceHostView(parentComponentHostView, parentComponentBoundElementIndex, hostView, injector);
    this._viewHydrateRecurse(hostView, renderViewRefs, 1);
    return new ViewRef(hostView);
  }
  destroyInPlaceHostView(parentComponentLocation, hostViewRef) {
    var hostView = internalView(hostViewRef);
    var parentView = null;
    var parentRenderViewRef = null;
    if (isPresent(parentComponentLocation)) {
      parentView = internalView(parentComponentLocation.parentView).componentChildViews[parentComponentLocation.boundElementIndex];
      parentRenderViewRef = parentView.render;
    }
    var hostViewRenderRef = hostView.render;
    this._viewDehydrateRecurse(hostView);
    this._utils.detachInPlaceHostView(parentView, hostView);
    this._renderer.destroyInPlaceHostView(parentRenderViewRef, hostViewRenderRef);
    this._destroyView(hostView);
  }
  createViewInContainer(viewContainerLocation, atIndex, protoViewRef, injector = null) {
    var protoView = internalProtoView(protoViewRef);
    var parentView = internalView(viewContainerLocation.parentView);
    var boundElementIndex = viewContainerLocation.boundElementIndex;
    var view = this._createViewRecurse(protoView);
    var renderViewRefs = this._renderer.createViewInContainer(this._getRenderViewContainerRef(parentView, boundElementIndex), atIndex, view.proto.render);
    view.render = renderViewRefs[0];
    this._utils.attachViewInContainer(parentView, boundElementIndex, atIndex, view);
    this._utils.hydrateViewInContainer(parentView, boundElementIndex, atIndex, injector);
    this._viewHydrateRecurse(view, renderViewRefs, 1);
    return new ViewRef(view);
  }
  destroyViewInContainer(viewContainerLocation, atIndex) {
    var parentView = internalView(viewContainerLocation.parentView);
    var boundElementIndex = viewContainerLocation.boundElementIndex;
    var viewContainer = parentView.viewContainers[boundElementIndex];
    var view = viewContainer.views[atIndex];
    this._viewDehydrateRecurse(view);
    this._utils.detachViewInContainer(parentView, boundElementIndex, atIndex);
    this._renderer.destroyViewInContainer(this._getRenderViewContainerRef(parentView, boundElementIndex), atIndex);
    this._destroyView(view);
  }
  attachViewInContainer(viewContainerLocation, atIndex, viewRef) {
    var view = internalView(viewRef);
    var parentView = internalView(viewContainerLocation.parentView);
    var boundElementIndex = viewContainerLocation.boundElementIndex;
    this._utils.attachViewInContainer(parentView, boundElementIndex, atIndex, view);
    this._renderer.insertViewIntoContainer(this._getRenderViewContainerRef(parentView, boundElementIndex), atIndex, view.render);
    return viewRef;
  }
  detachViewInContainer(viewContainerLocation, atIndex) {
    var parentView = internalView(viewContainerLocation.parentView);
    var boundElementIndex = viewContainerLocation.boundElementIndex;
    var viewContainer = parentView.viewContainers[boundElementIndex];
    var view = viewContainer.views[atIndex];
    this._utils.detachViewInContainer(parentView, boundElementIndex, atIndex);
    this._renderer.detachViewFromContainer(this._getRenderViewContainerRef(parentView, boundElementIndex), atIndex);
    return new ViewRef(view);
  }
  _getRenderViewContainerRef(parentView, boundElementIndex) {
    return new RenderViewContainerRef(parentView.render, boundElementIndex);
  }
  _createViewRecurse(protoView) {
    var view = this._viewPool.getView(protoView);
    if (isBlank(view)) {
      view = this._utils.createView(protoView, this, this._renderer);
      var binders = protoView.elementBinders;
      for (var binderIdx = 0; binderIdx < binders.length; binderIdx++) {
        var binder = binders[binderIdx];
        if (binder.hasStaticComponent()) {
          var childView = this._createViewRecurse(binder.nestedProtoView);
          this._utils.attachComponentView(view, binderIdx, childView);
        }
      }
    }
    return view;
  }
  _destroyView(view) {
    this._viewPool.returnView(view);
  }
  _viewHydrateRecurse(view, renderComponentViewRefs, renderComponentIndex) {
    this._renderer.setEventDispatcher(view.render, view);
    var binders = view.proto.elementBinders;
    for (var i = 0; i < binders.length; ++i) {
      if (binders[i].hasStaticComponent()) {
        var childView = view.componentChildViews[i];
        childView.render = renderComponentViewRefs[renderComponentIndex++];
        this._utils.hydrateComponentView(view, i);
        renderComponentIndex = this._viewHydrateRecurse(view.componentChildViews[i], renderComponentViewRefs, renderComponentIndex);
      }
    }
    return renderComponentIndex;
  }
  _viewDehydrateRecurse(view) {
    this._utils.dehydrateView(view);
    var binders = view.proto.elementBinders;
    for (var i = 0; i < binders.length; i++) {
      var componentView = view.componentChildViews[i];
      if (isPresent(componentView)) {
        this._viewDehydrateRecurse(componentView);
        if (binders[i].hasDynamicComponent()) {
          this._utils.detachComponentView(view, i);
          this._destroyView(componentView);
        }
      }
      var vc = view.viewContainers[i];
      if (isPresent(vc)) {
        for (var j = vc.views.length - 1; j >= 0; j--) {
          var childView = vc.views[j];
          this._viewDehydrateRecurse(childView);
          this._utils.detachViewInContainer(view, i, j);
          this._destroyView(childView);
        }
      }
    }
    for (var i = 0; i < view.imperativeHostViews.length; i++) {
      var hostView = view.imperativeHostViews[i];
      this._viewDehydrateRecurse(hostView);
      this._utils.detachInPlaceHostView(view, hostView);
      this._destroyView(hostView);
    }
    view.render = null;
  }
}
Object.defineProperty(AppViewManager, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(AppViewManager, "parameters", {get: function() {
    return [[AppViewPool], [AppViewManagerUtils], [Renderer]];
  }});
Object.defineProperty(AppViewManager.prototype.getViewContainer, "parameters", {get: function() {
    return [[ElementRef]];
  }});
Object.defineProperty(AppViewManager.prototype.getComponent, "parameters", {get: function() {
    return [[ElementRef]];
  }});
Object.defineProperty(AppViewManager.prototype.createDynamicComponentView, "parameters", {get: function() {
    return [[ElementRef], [ProtoViewRef], [Binding], [Injector]];
  }});
Object.defineProperty(AppViewManager.prototype.createInPlaceHostView, "parameters", {get: function() {
    return [[ElementRef], [], [ProtoViewRef], [Injector]];
  }});
Object.defineProperty(AppViewManager.prototype.destroyInPlaceHostView, "parameters", {get: function() {
    return [[ElementRef], [ViewRef]];
  }});
Object.defineProperty(AppViewManager.prototype.createViewInContainer, "parameters", {get: function() {
    return [[ElementRef], [assert.type.number], [ProtoViewRef], [Injector]];
  }});
Object.defineProperty(AppViewManager.prototype.destroyViewInContainer, "parameters", {get: function() {
    return [[ElementRef], [assert.type.number]];
  }});
Object.defineProperty(AppViewManager.prototype.attachViewInContainer, "parameters", {get: function() {
    return [[ElementRef], [assert.type.number], [ViewRef]];
  }});
Object.defineProperty(AppViewManager.prototype.detachViewInContainer, "parameters", {get: function() {
    return [[ElementRef], [assert.type.number]];
  }});
Object.defineProperty(AppViewManager.prototype._getRenderViewContainerRef, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number]];
  }});
Object.defineProperty(AppViewManager.prototype._createViewRecurse, "parameters", {get: function() {
    return [[viewModule.AppProtoView]];
  }});
Object.defineProperty(AppViewManager.prototype._destroyView, "parameters", {get: function() {
    return [[viewModule.AppView]];
  }});
Object.defineProperty(AppViewManager.prototype._viewHydrateRecurse, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.genericType(List, RenderViewRef)], [assert.type.number]];
  }});
Object.defineProperty(AppViewManager.prototype._viewDehydrateRecurse, "parameters", {get: function() {
    return [[viewModule.AppView]];
  }});
//# sourceMappingURL=view_manager.js.map

//# sourceMappingURL=./view_manager.map