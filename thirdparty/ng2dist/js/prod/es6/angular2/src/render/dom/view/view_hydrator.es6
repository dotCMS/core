import {Injectable} from 'angular2/di';
import {int,
  isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import {ListWrapper,
  MapWrapper,
  Map,
  StringMapWrapper,
  List} from 'angular2/src/facade/collection';
import * as ldModule from '../shadow_dom/light_dom';
import {EventManager} from '../events/event_manager';
import {ViewFactory} from './view_factory';
import * as vcModule from './view_container';
import * as viewModule from './view';
import {ShadowDomStrategy} from '../shadow_dom/shadow_dom_strategy';
export class RenderViewHydrator {
  constructor(eventManager, viewFactory, shadowDomStrategy) {
    this._eventManager = eventManager;
    this._viewFactory = viewFactory;
    this._shadowDomStrategy = shadowDomStrategy;
  }
  hydrateDynamicComponentView(hostView, boundElementIndex, componentView) {
    ViewFactory.setComponentView(this._shadowDomStrategy, hostView, boundElementIndex, componentView);
    var lightDom = hostView.lightDoms[boundElementIndex];
    this._viewHydrateRecurse(componentView, lightDom);
    if (isPresent(lightDom)) {
      lightDom.redistribute();
    }
  }
  dehydrateDynamicComponentView(parentView, boundElementIndex) {
    throw new BaseException('Not supported yet');
  }
  hydrateInPlaceHostView(parentView, hostView) {
    if (isPresent(parentView)) {
      ListWrapper.push(parentView.imperativeHostViews, hostView);
    }
    this._viewHydrateRecurse(hostView, null);
  }
  dehydrateInPlaceHostView(parentView, hostView) {
    if (isPresent(parentView)) {
      ListWrapper.remove(parentView.imperativeHostViews, hostView);
    }
    vcModule.ViewContainer.removeViewNodes(hostView);
    hostView.rootNodes = [];
    this._viewDehydrateRecurse(hostView);
  }
  hydrateViewInViewContainer(viewContainer, view) {
    this._viewHydrateRecurse(view, viewContainer.parentView.hostLightDom);
  }
  dehydrateViewInViewContainer(viewContainer, view) {
    this._viewDehydrateRecurse(view);
  }
  _viewHydrateRecurse(view, hostLightDom) {
    if (view.hydrated)
      throw new BaseException('The view is already hydrated.');
    view.hydrated = true;
    view.hostLightDom = hostLightDom;
    for (var i = 0; i < view.contentTags.length; i++) {
      var destLightDom = view.getDirectParentLightDom(i);
      var ct = view.contentTags[i];
      if (isPresent(ct)) {
        ct.hydrate(destLightDom);
      }
    }
    for (var i = 0; i < view.componentChildViews.length; i++) {
      var cv = view.componentChildViews[i];
      if (isPresent(cv)) {
        this._viewHydrateRecurse(cv, view.lightDoms[i]);
      }
    }
    for (var i = 0; i < view.lightDoms.length; ++i) {
      var lightDom = view.lightDoms[i];
      if (isPresent(lightDom)) {
        lightDom.redistribute();
      }
    }
    view.eventHandlerRemovers = ListWrapper.create();
    var binders = view.proto.elementBinders;
    for (var binderIdx = 0; binderIdx < binders.length; binderIdx++) {
      var binder = binders[binderIdx];
      if (isPresent(binder.globalEvents)) {
        for (var i = 0; i < binder.globalEvents.length; i++) {
          var globalEvent = binder.globalEvents[i];
          var remover = this._createGlobalEventListener(view, binderIdx, globalEvent.name, globalEvent.target, globalEvent.fullName);
          ListWrapper.push(view.eventHandlerRemovers, remover);
        }
      }
    }
  }
  _createGlobalEventListener(view, elementIndex, eventName, eventTarget, fullName) {
    return this._eventManager.addGlobalEventListener(eventTarget, eventName, (event) => {
      view.dispatchEvent(elementIndex, fullName, event);
    });
  }
  _viewDehydrateRecurse(view) {
    for (var i = 0; i < view.componentChildViews.length; i++) {
      var cv = view.componentChildViews[i];
      if (isPresent(cv)) {
        this._viewDehydrateRecurse(cv);
        if (view.proto.elementBinders[i].hasDynamicComponent()) {
          vcModule.ViewContainer.removeViewNodes(cv);
          this._viewFactory.returnView(cv);
          view.lightDoms[i] = null;
          view.componentChildViews[i] = null;
        }
      }
    }
    for (var i = 0; i < view.imperativeHostViews.length; i++) {
      var hostView = view.imperativeHostViews[i];
      this._viewDehydrateRecurse(hostView);
      vcModule.ViewContainer.removeViewNodes(hostView);
      hostView.rootNodes = [];
      this._viewFactory.returnView(hostView);
    }
    view.imperativeHostViews = [];
    if (isPresent(view.viewContainers)) {
      for (var i = 0; i < view.viewContainers.length; i++) {
        var vc = view.viewContainers[i];
        if (isPresent(vc)) {
          this._viewContainerDehydrateRecurse(vc);
        }
        var ct = view.contentTags[i];
        if (isPresent(ct)) {
          ct.dehydrate();
        }
      }
    }
    for (var i = 0; i < view.eventHandlerRemovers.length; i++) {
      view.eventHandlerRemovers[i]();
    }
    view.hostLightDom = null;
    view.eventHandlerRemovers = null;
    view.setEventDispatcher(null);
    view.hydrated = false;
  }
  _viewContainerDehydrateRecurse(viewContainer) {
    for (var i = 0; i < viewContainer.views.length; i++) {
      var view = viewContainer.views[i];
      this._viewDehydrateRecurse(view);
      this._viewFactory.returnView(view);
    }
    viewContainer.clear();
  }
}
Object.defineProperty(RenderViewHydrator, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(RenderViewHydrator, "parameters", {get: function() {
    return [[EventManager], [ViewFactory], [ShadowDomStrategy]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.hydrateDynamicComponentView, "parameters", {get: function() {
    return [[viewModule.RenderView], [assert.type.number], [viewModule.RenderView]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.dehydrateDynamicComponentView, "parameters", {get: function() {
    return [[viewModule.RenderView], [assert.type.number]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.hydrateInPlaceHostView, "parameters", {get: function() {
    return [[viewModule.RenderView], [viewModule.RenderView]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.dehydrateInPlaceHostView, "parameters", {get: function() {
    return [[viewModule.RenderView], [viewModule.RenderView]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.hydrateViewInViewContainer, "parameters", {get: function() {
    return [[vcModule.ViewContainer], [viewModule.RenderView]];
  }});
Object.defineProperty(RenderViewHydrator.prototype.dehydrateViewInViewContainer, "parameters", {get: function() {
    return [[vcModule.ViewContainer], [viewModule.RenderView]];
  }});
Object.defineProperty(RenderViewHydrator.prototype._viewHydrateRecurse, "parameters", {get: function() {
    return [[], [ldModule.LightDom]];
  }});
//# sourceMappingURL=view_hydrator.js.map

//# sourceMappingURL=./view_hydrator.map