import {ListWrapper,
  List} from 'angular2/src/facade/collection';
import {Injector} from 'angular2/di';
import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import * as avmModule from './view_manager';
import {ElementRef} from './element_ref';
import {ViewRef,
  ProtoViewRef,
  internalView} from './view_ref';
export class ViewContainerRef {
  constructor(viewManager, element) {
    this._viewManager = viewManager;
    this._element = element;
  }
  _getViews() {
    var vc = internalView(this._element.parentView).viewContainers[this._element.boundElementIndex];
    return isPresent(vc) ? vc.views : [];
  }
  clear() {
    for (var i = this.length - 1; i >= 0; i--) {
      this.remove(i);
    }
  }
  get(index) {
    return new ViewRef(this._getViews()[index]);
  }
  get length() {
    return this._getViews().length;
  }
  create(protoViewRef = null, atIndex = -1, injector = null) {
    if (atIndex == -1)
      atIndex = this.length;
    return this._viewManager.createViewInContainer(this._element, atIndex, protoViewRef, injector);
  }
  insert(viewRef, atIndex = -1) {
    if (atIndex == -1)
      atIndex = this.length;
    return this._viewManager.attachViewInContainer(this._element, atIndex, viewRef);
  }
  indexOf(viewRef) {
    return ListWrapper.indexOf(this._getViews(), internalView(viewRef));
  }
  remove(atIndex = -1) {
    if (atIndex == -1)
      atIndex = this.length - 1;
    this._viewManager.destroyViewInContainer(this._element, atIndex);
  }
  detach(atIndex = -1) {
    if (atIndex == -1)
      atIndex = this.length - 1;
    return this._viewManager.detachViewInContainer(this._element, atIndex);
  }
}
Object.defineProperty(ViewContainerRef, "parameters", {get: function() {
    return [[avmModule.AppViewManager], [ElementRef]];
  }});
Object.defineProperty(ViewContainerRef.prototype.get, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ViewContainerRef.prototype.create, "parameters", {get: function() {
    return [[ProtoViewRef], [assert.type.number], [Injector]];
  }});
Object.defineProperty(ViewContainerRef.prototype.insert, "parameters", {get: function() {
    return [[ViewRef], [assert.type.number]];
  }});
Object.defineProperty(ViewContainerRef.prototype.indexOf, "parameters", {get: function() {
    return [[ViewRef]];
  }});
Object.defineProperty(ViewContainerRef.prototype.remove, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ViewContainerRef.prototype.detach, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
//# sourceMappingURL=view_container_ref.js.map

//# sourceMappingURL=./view_container_ref.map