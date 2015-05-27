import {Key,
  Injector,
  Injectable,
  ResolvedBinding,
  Binding,
  bind} from 'angular2/di';
import {Compiler} from './compiler';
import {Type,
  BaseException,
  stringify,
  isPresent} from 'angular2/src/facade/lang';
import {Promise} from 'angular2/src/facade/async';
import {AppViewManager,
  ComponentCreateResult} from 'angular2/src/core/compiler/view_manager';
import {ElementRef} from './element_ref';
export class ComponentRef {
  constructor(location, instance, dispose) {
    this.location = location;
    this.instance = instance;
    this._dispose = dispose;
  }
  get hostView() {
    return this.location.parentView;
  }
  dispose() {
    this._dispose();
  }
}
Object.defineProperty(ComponentRef, "parameters", {get: function() {
    return [[ElementRef], [assert.type.any], [Function]];
  }});
export class DynamicComponentLoader {
  constructor(compiler, viewManager) {
    this._compiler = compiler;
    this._viewManager = viewManager;
  }
  loadIntoExistingLocation(typeOrBinding, location, injector = null) {
    var binding = this._getBinding(typeOrBinding);
    return this._compiler.compile(binding.token).then((componentProtoViewRef) => {
      this._viewManager.createDynamicComponentView(location, componentProtoViewRef, binding, injector);
      var component = this._viewManager.getComponent(location);
      var dispose = () => {
        throw new BaseException("Not implemented");
      };
      return new ComponentRef(location, component, dispose);
    });
  }
  loadIntoNewLocation(typeOrBinding, parentComponentLocation, elementOrSelector, injector = null) {
    return this._compiler.compileInHost(this._getBinding(typeOrBinding)).then((hostProtoViewRef) => {
      var hostViewRef = this._viewManager.createInPlaceHostView(parentComponentLocation, elementOrSelector, hostProtoViewRef, injector);
      var newLocation = new ElementRef(hostViewRef, 0);
      var component = this._viewManager.getComponent(newLocation);
      var dispose = () => {
        this._viewManager.destroyInPlaceHostView(parentComponentLocation, hostViewRef);
      };
      return new ComponentRef(newLocation, component, dispose);
    });
  }
  loadNextToExistingLocation(typeOrBinding, location, injector = null) {
    var binding = this._getBinding(typeOrBinding);
    return this._compiler.compileInHost(binding).then((hostProtoViewRef) => {
      var viewContainer = this._viewManager.getViewContainer(location);
      var hostViewRef = viewContainer.create(hostProtoViewRef, viewContainer.length, injector);
      var newLocation = new ElementRef(hostViewRef, 0);
      var component = this._viewManager.getComponent(newLocation);
      var dispose = () => {
        var index = viewContainer.indexOf(hostViewRef);
        viewContainer.remove(index);
      };
      return new ComponentRef(newLocation, component, dispose);
    });
  }
  _getBinding(typeOrBinding) {
    var binding;
    if (typeOrBinding instanceof Binding) {
      binding = typeOrBinding;
    } else {
      binding = bind(typeOrBinding).toClass(typeOrBinding);
    }
    return binding;
  }
}
Object.defineProperty(DynamicComponentLoader, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(DynamicComponentLoader, "parameters", {get: function() {
    return [[Compiler], [AppViewManager]];
  }});
Object.defineProperty(DynamicComponentLoader.prototype.loadIntoExistingLocation, "parameters", {get: function() {
    return [[], [ElementRef], [Injector]];
  }});
Object.defineProperty(DynamicComponentLoader.prototype.loadIntoNewLocation, "parameters", {get: function() {
    return [[], [ElementRef], [assert.type.any], [Injector]];
  }});
Object.defineProperty(DynamicComponentLoader.prototype.loadNextToExistingLocation, "parameters", {get: function() {
    return [[], [ElementRef], [Injector]];
  }});
//# sourceMappingURL=dynamic_component_loader.js.map

//# sourceMappingURL=./dynamic_component_loader.map