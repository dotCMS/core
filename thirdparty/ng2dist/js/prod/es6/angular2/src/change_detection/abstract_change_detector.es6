import {isPresent} from 'angular2/src/facade/lang';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {ChangeDetectorRef} from './change_detector_ref';
import {ChangeDetector} from './interfaces';
import {CHECK_ALWAYS,
  CHECK_ONCE,
  CHECKED,
  DETACHED,
  ON_PUSH} from './constants';
export class AbstractChangeDetector extends ChangeDetector {
  constructor() {
    super();
    this.lightDomChildren = [];
    this.shadowDomChildren = [];
    this.ref = new ChangeDetectorRef(this);
    this.mode = null;
  }
  addChild(cd) {
    ListWrapper.push(this.lightDomChildren, cd);
    cd.parent = this;
  }
  removeChild(cd) {
    ListWrapper.remove(this.lightDomChildren, cd);
  }
  addShadowDomChild(cd) {
    ListWrapper.push(this.shadowDomChildren, cd);
    cd.parent = this;
  }
  removeShadowDomChild(cd) {
    ListWrapper.remove(this.shadowDomChildren, cd);
  }
  remove() {
    this.parent.removeChild(this);
  }
  detectChanges() {
    this._detectChanges(false);
  }
  checkNoChanges() {
    this._detectChanges(true);
  }
  _detectChanges(throwOnChange) {
    if (this.mode === DETACHED || this.mode === CHECKED)
      return ;
    this.detectChangesInRecords(throwOnChange);
    this._detectChangesInLightDomChildren(throwOnChange);
    this.callOnAllChangesDone();
    this._detectChangesInShadowDomChildren(throwOnChange);
    if (this.mode === CHECK_ONCE)
      this.mode = CHECKED;
  }
  detectChangesInRecords(throwOnChange) {}
  callOnAllChangesDone() {}
  _detectChangesInLightDomChildren(throwOnChange) {
    var c = this.lightDomChildren;
    for (var i = 0; i < c.length; ++i) {
      c[i]._detectChanges(throwOnChange);
    }
  }
  _detectChangesInShadowDomChildren(throwOnChange) {
    var c = this.shadowDomChildren;
    for (var i = 0; i < c.length; ++i) {
      c[i]._detectChanges(throwOnChange);
    }
  }
  markAsCheckOnce() {
    this.mode = CHECK_ONCE;
  }
  markPathToRootAsCheckOnce() {
    var c = this;
    while (isPresent(c) && c.mode != DETACHED) {
      if (c.mode === CHECKED)
        c.mode = CHECK_ONCE;
      c = c.parent;
    }
  }
}
Object.defineProperty(AbstractChangeDetector.prototype.addChild, "parameters", {get: function() {
    return [[ChangeDetector]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype.removeChild, "parameters", {get: function() {
    return [[ChangeDetector]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype.addShadowDomChild, "parameters", {get: function() {
    return [[ChangeDetector]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype.removeShadowDomChild, "parameters", {get: function() {
    return [[ChangeDetector]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype._detectChanges, "parameters", {get: function() {
    return [[assert.type.boolean]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype.detectChangesInRecords, "parameters", {get: function() {
    return [[assert.type.boolean]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype._detectChangesInLightDomChildren, "parameters", {get: function() {
    return [[assert.type.boolean]];
  }});
Object.defineProperty(AbstractChangeDetector.prototype._detectChangesInShadowDomChildren, "parameters", {get: function() {
    return [[assert.type.boolean]];
  }});
//# sourceMappingURL=abstract_change_detector.js.map

//# sourceMappingURL=./abstract_change_detector.map