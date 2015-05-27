import {isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import {ListWrapper,
  MapWrapper,
  List} from 'angular2/src/facade/collection';
import {DOM} from 'angular2/src/dom/dom_adapter';
import * as viewModule from './view';
export class ViewContainer {
  constructor(parentView, boundElementIndex) {
    this.parentView = parentView;
    this.boundElementIndex = boundElementIndex;
    this.views = [];
  }
  get(index) {
    return this.views[index];
  }
  size() {
    return this.views.length;
  }
  _siblingToInsertAfter(index) {
    if (index == 0)
      return this.parentView.boundElements[this.boundElementIndex];
    return ListWrapper.last(this.views[index - 1].rootNodes);
  }
  _checkHydrated() {
    if (!this.parentView.hydrated)
      throw new BaseException('Cannot change dehydrated ViewContainer');
  }
  _getDirectParentLightDom() {
    return this.parentView.getDirectParentLightDom(this.boundElementIndex);
  }
  clear() {
    this._checkHydrated();
    for (var i = this.views.length - 1; i >= 0; i--) {
      this.detach(i);
    }
    if (isPresent(this._getDirectParentLightDom())) {
      this._getDirectParentLightDom().redistribute();
    }
  }
  insert(view, atIndex = -1) {
    this._checkHydrated();
    if (atIndex == -1)
      atIndex = this.views.length;
    ListWrapper.insert(this.views, atIndex, view);
    if (isBlank(this._getDirectParentLightDom())) {
      ViewContainer.moveViewNodesAfterSibling(this._siblingToInsertAfter(atIndex), view);
    } else {
      this._getDirectParentLightDom().redistribute();
    }
    if (isPresent(this.parentView.hostLightDom)) {
      this.parentView.hostLightDom.redistribute();
    }
    return view;
  }
  detach(atIndex) {
    this._checkHydrated();
    var detachedView = this.get(atIndex);
    ListWrapper.removeAt(this.views, atIndex);
    if (isBlank(this._getDirectParentLightDom())) {
      ViewContainer.removeViewNodes(detachedView);
    } else {
      this._getDirectParentLightDom().redistribute();
    }
    if (isPresent(this.parentView.hostLightDom)) {
      this.parentView.hostLightDom.redistribute();
    }
    return detachedView;
  }
  contentTagContainers() {
    return this.views;
  }
  nodes() {
    var r = [];
    for (var i = 0; i < this.views.length; ++i) {
      r = ListWrapper.concat(r, this.views[i].rootNodes);
    }
    return r;
  }
  static moveViewNodesAfterSibling(sibling, view) {
    for (var i = view.rootNodes.length - 1; i >= 0; --i) {
      DOM.insertAfter(sibling, view.rootNodes[i]);
    }
  }
  static removeViewNodes(view) {
    var len = view.rootNodes.length;
    if (len == 0)
      return ;
    var parent = view.rootNodes[0].parentNode;
    for (var i = len - 1; i >= 0; --i) {
      DOM.removeChild(parent, view.rootNodes[i]);
    }
  }
}
Object.defineProperty(ViewContainer, "parameters", {get: function() {
    return [[viewModule.RenderView], [assert.type.number]];
  }});
Object.defineProperty(ViewContainer.prototype.get, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ViewContainer.prototype._siblingToInsertAfter, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ViewContainer.prototype.detach, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
//# sourceMappingURL=view_container.js.map

//# sourceMappingURL=./view_container.map