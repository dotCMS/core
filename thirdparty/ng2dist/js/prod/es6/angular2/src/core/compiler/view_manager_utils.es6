import {Injectable,
  Injector,
  Binding} from 'angular2/di';
import {ListWrapper,
  MapWrapper,
  Map,
  StringMapWrapper,
  List} from 'angular2/src/facade/collection';
import * as eli from './element_injector';
import {isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import * as viewModule from './view';
import * as avmModule from './view_manager';
import {Renderer} from 'angular2/src/render/api';
import {BindingPropagationConfig,
  Locals} from 'angular2/change_detection';
import {DirectiveMetadataReader} from './directive_metadata_reader';
export class AppViewManagerUtils {
  constructor(metadataReader) {
    this._metadataReader = metadataReader;
  }
  getComponentInstance(parentView, boundElementIndex) {
    var binder = parentView.proto.elementBinders[boundElementIndex];
    var eli = parentView.elementInjectors[boundElementIndex];
    if (binder.hasDynamicComponent()) {
      return eli.getDynamicallyLoadedComponent();
    } else {
      return eli.getComponent();
    }
  }
  createView(protoView, viewManager, renderer) {
    var view = new viewModule.AppView(renderer, protoView, protoView.protoLocals);
    var changeDetector = protoView.protoChangeDetector.instantiate(view);
    var binders = protoView.elementBinders;
    var elementInjectors = ListWrapper.createFixedSize(binders.length);
    var rootElementInjectors = [];
    var preBuiltObjects = ListWrapper.createFixedSize(binders.length);
    var componentChildViews = ListWrapper.createFixedSize(binders.length);
    for (var binderIdx = 0; binderIdx < binders.length; binderIdx++) {
      var binder = binders[binderIdx];
      var elementInjector = null;
      var protoElementInjector = binder.protoElementInjector;
      if (isPresent(protoElementInjector)) {
        if (isPresent(protoElementInjector.parent)) {
          var parentElementInjector = elementInjectors[protoElementInjector.parent.index];
          elementInjector = protoElementInjector.instantiate(parentElementInjector);
        } else {
          elementInjector = protoElementInjector.instantiate(null);
          ListWrapper.push(rootElementInjectors, elementInjector);
        }
      }
      elementInjectors[binderIdx] = elementInjector;
      if (isPresent(elementInjector)) {
        var embeddedProtoView = binder.hasEmbeddedProtoView() ? binder.nestedProtoView : null;
        preBuiltObjects[binderIdx] = new eli.PreBuiltObjects(viewManager, view, embeddedProtoView);
      }
    }
    view.init(changeDetector, elementInjectors, rootElementInjectors, preBuiltObjects, componentChildViews);
    return view;
  }
  attachComponentView(hostView, boundElementIndex, componentView) {
    var childChangeDetector = componentView.changeDetector;
    hostView.changeDetector.addShadowDomChild(childChangeDetector);
    hostView.componentChildViews[boundElementIndex] = componentView;
  }
  detachComponentView(hostView, boundElementIndex) {
    var componentView = hostView.componentChildViews[boundElementIndex];
    hostView.changeDetector.removeShadowDomChild(componentView.changeDetector);
    hostView.componentChildViews[boundElementIndex] = null;
  }
  hydrateComponentView(hostView, boundElementIndex, injector = null) {
    var elementInjector = hostView.elementInjectors[boundElementIndex];
    var componentView = hostView.componentChildViews[boundElementIndex];
    var component = this.getComponentInstance(hostView, boundElementIndex);
    this._hydrateView(componentView, injector, elementInjector, component, null);
  }
  attachAndHydrateInPlaceHostView(parentComponentHostView, parentComponentBoundElementIndex, hostView, injector = null) {
    var hostElementInjector = null;
    if (isPresent(parentComponentHostView)) {
      hostElementInjector = parentComponentHostView.elementInjectors[parentComponentBoundElementIndex];
      var parentView = parentComponentHostView.componentChildViews[parentComponentBoundElementIndex];
      parentView.changeDetector.addChild(hostView.changeDetector);
      ListWrapper.push(parentView.imperativeHostViews, hostView);
    }
    this._hydrateView(hostView, injector, hostElementInjector, new Object(), null);
  }
  detachInPlaceHostView(parentView, hostView) {
    if (isPresent(parentView)) {
      parentView.changeDetector.removeChild(hostView.changeDetector);
      ListWrapper.remove(parentView.imperativeHostViews, hostView);
    }
  }
  attachViewInContainer(parentView, boundElementIndex, atIndex, view) {
    parentView.changeDetector.addChild(view.changeDetector);
    var viewContainer = parentView.viewContainers[boundElementIndex];
    if (isBlank(viewContainer)) {
      viewContainer = new viewModule.AppViewContainer();
      parentView.viewContainers[boundElementIndex] = viewContainer;
    }
    ListWrapper.insert(viewContainer.views, atIndex, view);
    var sibling;
    if (atIndex == 0) {
      sibling = null;
    } else {
      sibling = ListWrapper.last(viewContainer.views[atIndex - 1].rootElementInjectors);
    }
    var elementInjector = parentView.elementInjectors[boundElementIndex];
    for (var i = view.rootElementInjectors.length - 1; i >= 0; i--) {
      view.rootElementInjectors[i].linkAfter(elementInjector, sibling);
    }
  }
  detachViewInContainer(parentView, boundElementIndex, atIndex) {
    var viewContainer = parentView.viewContainers[boundElementIndex];
    var view = viewContainer.views[atIndex];
    view.changeDetector.remove();
    ListWrapper.removeAt(viewContainer.views, atIndex);
    for (var i = 0; i < view.rootElementInjectors.length; ++i) {
      view.rootElementInjectors[i].unlink();
    }
  }
  hydrateViewInContainer(parentView, boundElementIndex, atIndex, injector) {
    var viewContainer = parentView.viewContainers[boundElementIndex];
    var view = viewContainer.views[atIndex];
    var elementInjector = parentView.elementInjectors[boundElementIndex];
    this._hydrateView(view, injector, elementInjector, parentView.context, parentView.locals);
  }
  hydrateDynamicComponentInElementInjector(hostView, boundElementIndex, componentBinding, injector = null) {
    var elementInjector = hostView.elementInjectors[boundElementIndex];
    if (isPresent(elementInjector.getDynamicallyLoadedComponent())) {
      throw new BaseException(`There already is a dynamic component loaded at element ${boundElementIndex}`);
    }
    if (isBlank(injector)) {
      injector = elementInjector.getLightDomAppInjector();
    }
    var annotation = this._metadataReader.read(componentBinding.token).annotation;
    var componentDirective = eli.DirectiveBinding.createFromBinding(componentBinding, annotation);
    var shadowDomAppInjector = this._createShadowDomAppInjector(componentDirective, injector);
    elementInjector.dynamicallyCreateComponent(componentDirective, shadowDomAppInjector);
  }
  _createShadowDomAppInjector(componentDirective, appInjector) {
    var shadowDomAppInjector = null;
    var injectables = componentDirective.resolvedInjectables;
    if (isPresent(injectables)) {
      shadowDomAppInjector = appInjector.createChildFromResolved(injectables);
    } else {
      shadowDomAppInjector = appInjector;
    }
    return shadowDomAppInjector;
  }
  _hydrateView(view, appInjector, hostElementInjector, context, parentLocals) {
    if (isBlank(appInjector)) {
      appInjector = hostElementInjector.getShadowDomAppInjector();
    }
    if (isBlank(appInjector)) {
      appInjector = hostElementInjector.getLightDomAppInjector();
    }
    view.context = context;
    view.locals.parent = parentLocals;
    var binders = view.proto.elementBinders;
    for (var i = 0; i < binders.length; ++i) {
      var elementInjector = view.elementInjectors[i];
      if (isPresent(elementInjector)) {
        var componentDirective = view.proto.elementBinders[i].componentDirective;
        var shadowDomAppInjector = null;
        if (isPresent(componentDirective)) {
          shadowDomAppInjector = this._createShadowDomAppInjector(componentDirective, appInjector);
        } else {
          shadowDomAppInjector = null;
        }
        elementInjector.instantiateDirectives(appInjector, hostElementInjector, shadowDomAppInjector, view.preBuiltObjects[i]);
        this._setUpEventEmitters(view, elementInjector, i);
        var exportImplicitName = elementInjector.getExportImplicitName();
        if (elementInjector.isExportingComponent()) {
          view.locals.set(exportImplicitName, elementInjector.getComponent());
        } else if (elementInjector.isExportingElement()) {
          view.locals.set(exportImplicitName, elementInjector.getElementRef().domElement);
        }
      }
    }
    view.changeDetector.hydrate(view.context, view.locals, view);
  }
  _setUpEventEmitters(view, elementInjector, boundElementIndex) {
    var emitters = elementInjector.getEventEmitterAccessors();
    for (var directiveIndex = 0; directiveIndex < emitters.length; ++directiveIndex) {
      var directiveEmitters = emitters[directiveIndex];
      var directive = elementInjector.getDirectiveAtIndex(directiveIndex);
      for (var eventIndex = 0; eventIndex < directiveEmitters.length; ++eventIndex) {
        var eventEmitterAccessor = directiveEmitters[eventIndex];
        eventEmitterAccessor.subscribe(view, boundElementIndex, directive);
      }
    }
  }
  dehydrateView(view) {
    var binders = view.proto.elementBinders;
    for (var i = 0; i < binders.length; ++i) {
      var elementInjector = view.elementInjectors[i];
      if (isPresent(elementInjector)) {
        elementInjector.clearDirectives();
      }
    }
    if (isPresent(view.locals)) {
      view.locals.clearValues();
    }
    view.context = null;
    view.changeDetector.dehydrate();
  }
}
Object.defineProperty(AppViewManagerUtils, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(AppViewManagerUtils, "parameters", {get: function() {
    return [[DirectiveMetadataReader]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.getComponentInstance, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.createView, "parameters", {get: function() {
    return [[viewModule.AppProtoView], [avmModule.AppViewManager], [Renderer]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.attachComponentView, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [viewModule.AppView]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.detachComponentView, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.hydrateComponentView, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [Injector]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.attachAndHydrateInPlaceHostView, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [viewModule.AppView], [Injector]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.detachInPlaceHostView, "parameters", {get: function() {
    return [[viewModule.AppView], [viewModule.AppView]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.attachViewInContainer, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [assert.type.number], [viewModule.AppView]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.detachViewInContainer, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [assert.type.number]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.hydrateViewInContainer, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [assert.type.number], [Injector]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.hydrateDynamicComponentInElementInjector, "parameters", {get: function() {
    return [[viewModule.AppView], [assert.type.number], [Binding], [Injector]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype._hydrateView, "parameters", {get: function() {
    return [[viewModule.AppView], [Injector], [eli.ElementInjector], [Object], [Locals]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype._setUpEventEmitters, "parameters", {get: function() {
    return [[viewModule.AppView], [eli.ElementInjector], [assert.type.number]];
  }});
Object.defineProperty(AppViewManagerUtils.prototype.dehydrateView, "parameters", {get: function() {
    return [[viewModule.AppView]];
  }});
//# sourceMappingURL=view_manager_utils.js.map

//# sourceMappingURL=./view_manager_utils.map