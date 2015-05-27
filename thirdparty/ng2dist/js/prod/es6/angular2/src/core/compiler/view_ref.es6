import {isPresent} from 'angular2/src/facade/lang';
import * as viewModule from './view';
import {RenderViewRef} from 'angular2/src/render/api';
export function internalView(viewRef) {
  return viewRef._view;
}
Object.defineProperty(internalView, "parameters", {get: function() {
    return [[ViewRef]];
  }});
export function internalProtoView(protoViewRef) {
  return isPresent(protoViewRef) ? protoViewRef._protoView : null;
}
Object.defineProperty(internalProtoView, "parameters", {get: function() {
    return [[ProtoViewRef]];
  }});
export class ViewRef {
  constructor(view) {
    this._view = view;
  }
  get render() {
    return this._view.render;
  }
  setLocal(contextName, value) {
    this._view.setLocal(contextName, value);
  }
}
Object.defineProperty(ViewRef, "parameters", {get: function() {
    return [[viewModule.AppView]];
  }});
Object.defineProperty(ViewRef.prototype.setLocal, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.any]];
  }});
export class ProtoViewRef {
  constructor(protoView) {
    this._protoView = protoView;
  }
}
//# sourceMappingURL=view_ref.js.map

//# sourceMappingURL=./view_ref.map