import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {ViewContainerRef} from 'angular2/src/core/compiler/view_container_ref';
import {ProtoViewRef} from 'angular2/src/core/compiler/view_ref';
import {isPresent,
  isBlank,
  normalizeBlank} from 'angular2/src/facade/lang';
import {ListWrapper,
  List,
  MapWrapper,
  Map} from 'angular2/src/facade/collection';
import {Parent} from 'angular2/src/core/annotations_impl/visibility';
class SwitchView {
  constructor(viewContainerRef, protoViewRef) {
    this._protoViewRef = protoViewRef;
    this._viewContainerRef = viewContainerRef;
  }
  create() {
    this._viewContainerRef.create(this._protoViewRef);
  }
  destroy() {
    this._viewContainerRef.clear();
  }
}
Object.defineProperty(SwitchView, "parameters", {get: function() {
    return [[ViewContainerRef], [ProtoViewRef]];
  }});
export class Switch {
  constructor() {
    this._valueViews = MapWrapper.create();
    this._activeViews = ListWrapper.create();
    this._useDefault = false;
  }
  set value(value) {
    this._emptyAllActiveViews();
    this._useDefault = false;
    var views = MapWrapper.get(this._valueViews, value);
    if (isBlank(views)) {
      this._useDefault = true;
      views = normalizeBlank(MapWrapper.get(this._valueViews, _whenDefault));
    }
    this._activateViews(views);
    this._switchValue = value;
  }
  _onWhenValueChanged(oldWhen, newWhen, view) {
    this._deregisterView(oldWhen, view);
    this._registerView(newWhen, view);
    if (oldWhen === this._switchValue) {
      view.destroy();
      ListWrapper.remove(this._activeViews, view);
    } else if (newWhen === this._switchValue) {
      if (this._useDefault) {
        this._useDefault = false;
        this._emptyAllActiveViews();
      }
      view.create();
      ListWrapper.push(this._activeViews, view);
    }
    if (this._activeViews.length === 0 && !this._useDefault) {
      this._useDefault = true;
      this._activateViews(MapWrapper.get(this._valueViews, _whenDefault));
    }
  }
  _emptyAllActiveViews() {
    var activeContainers = this._activeViews;
    for (var i = 0; i < activeContainers.length; i++) {
      activeContainers[i].destroy();
    }
    this._activeViews = ListWrapper.create();
  }
  _activateViews(views) {
    if (isPresent(views)) {
      for (var i = 0; i < views.length; i++) {
        views[i].create();
      }
      this._activeViews = views;
    }
  }
  _registerView(value, view) {
    var views = MapWrapper.get(this._valueViews, value);
    if (isBlank(views)) {
      views = ListWrapper.create();
      MapWrapper.set(this._valueViews, value, views);
    }
    ListWrapper.push(views, view);
  }
  _deregisterView(value, view) {
    if (value == _whenDefault)
      return ;
    var views = MapWrapper.get(this._valueViews, value);
    if (views.length == 1) {
      MapWrapper.delete(this._valueViews, value);
    } else {
      ListWrapper.remove(views, view);
    }
  }
}
Object.defineProperty(Switch, "annotations", {get: function() {
    return [new Directive({
      selector: '[switch]',
      properties: {'value': 'switch'}
    })];
  }});
Object.defineProperty(Switch.prototype._onWhenValueChanged, "parameters", {get: function() {
    return [[], [], [SwitchView]];
  }});
Object.defineProperty(Switch.prototype._activateViews, "parameters", {get: function() {
    return [[assert.genericType(List, SwitchView)]];
  }});
Object.defineProperty(Switch.prototype._registerView, "parameters", {get: function() {
    return [[], [SwitchView]];
  }});
Object.defineProperty(Switch.prototype._deregisterView, "parameters", {get: function() {
    return [[], [SwitchView]];
  }});
export class SwitchWhen {
  constructor(viewContainer, protoViewRef, sswitch) {
    this._value = _whenDefault;
    this._switch = sswitch;
    this._view = new SwitchView(viewContainer, protoViewRef);
  }
  onDestroy() {
    this._switch;
  }
  set when(value) {
    this._switch._onWhenValueChanged(this._value, value, this._view);
    this._value = value;
  }
}
Object.defineProperty(SwitchWhen, "annotations", {get: function() {
    return [new Directive({
      selector: '[switch-when]',
      properties: {'when': 'switch-when'}
    })];
  }});
Object.defineProperty(SwitchWhen, "parameters", {get: function() {
    return [[ViewContainerRef], [ProtoViewRef], [Switch, new Parent()]];
  }});
export class SwitchDefault {
  constructor(viewContainer, protoViewRef, sswitch) {
    sswitch._registerView(_whenDefault, new SwitchView(viewContainer, protoViewRef));
  }
}
Object.defineProperty(SwitchDefault, "annotations", {get: function() {
    return [new Directive({selector: '[switch-default]'})];
  }});
Object.defineProperty(SwitchDefault, "parameters", {get: function() {
    return [[ViewContainerRef], [ProtoViewRef], [Switch, new Parent()]];
  }});
var _whenDefault = new Object();
//# sourceMappingURL=switch.js.map

//# sourceMappingURL=./switch.map