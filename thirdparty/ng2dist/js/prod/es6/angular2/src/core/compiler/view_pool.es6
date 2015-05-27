import {Inject,
  OpaqueToken} from 'angular2/di';
import {ListWrapper,
  MapWrapper,
  Map,
  List} from 'angular2/src/facade/collection';
import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import * as viewModule from './view';
export const APP_VIEW_POOL_CAPACITY = 'AppViewPool.viewPoolCapacity';
export class AppViewPool {
  constructor(poolCapacityPerProtoView) {
    this._poolCapacityPerProtoView = poolCapacityPerProtoView;
    this._pooledViewsPerProtoView = MapWrapper.create();
  }
  getView(protoView) {
    var pooledViews = MapWrapper.get(this._pooledViewsPerProtoView, protoView);
    if (isPresent(pooledViews) && pooledViews.length > 0) {
      return ListWrapper.removeLast(pooledViews);
    }
    return null;
  }
  returnView(view) {
    var protoView = view.proto;
    var pooledViews = MapWrapper.get(this._pooledViewsPerProtoView, protoView);
    if (isBlank(pooledViews)) {
      pooledViews = [];
      MapWrapper.set(this._pooledViewsPerProtoView, protoView, pooledViews);
    }
    if (pooledViews.length < this._poolCapacityPerProtoView) {
      ListWrapper.push(pooledViews, view);
    }
  }
}
Object.defineProperty(AppViewPool, "parameters", {get: function() {
    return [[new Inject(APP_VIEW_POOL_CAPACITY)]];
  }});
Object.defineProperty(AppViewPool.prototype.getView, "parameters", {get: function() {
    return [[viewModule.AppProtoView]];
  }});
Object.defineProperty(AppViewPool.prototype.returnView, "parameters", {get: function() {
    return [[viewModule.AppView]];
  }});
//# sourceMappingURL=view_pool.js.map

//# sourceMappingURL=./view_pool.map