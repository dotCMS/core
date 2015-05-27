import {isPresent} from 'angular2/src/facade/lang';
import {Observable,
  EventEmitter,
  ObservableWrapper} from 'angular2/src/facade/async';
import {StringMap,
  StringMapWrapper,
  ListWrapper,
  List} from 'angular2/src/facade/collection';
import {Validators} from './validators';
export const VALID = "VALID";
export const INVALID = "INVALID";
class AbstractControl {
  constructor(validator) {
    this.validator = validator;
    this._pristine = true;
  }
  get value() {
    return this._value;
  }
  get status() {
    return this._status;
  }
  get valid() {
    return this._status === VALID;
  }
  get errors() {
    return this._errors;
  }
  get pristine() {
    return this._pristine;
  }
  get dirty() {
    return !this.pristine;
  }
  get valueChanges() {
    return this._valueChanges;
  }
  setParent(parent) {
    this._parent = parent;
  }
  _updateParent() {
    if (isPresent(this._parent)) {
      this._parent._updateValue();
    }
  }
}
Object.defineProperty(AbstractControl, "parameters", {get: function() {
    return [[Function]];
  }});
export class Control extends AbstractControl {
  constructor(value, validator = Validators.nullValidator) {
    super(validator);
    this._setValueErrorsStatus(value);
    this._valueChanges = new EventEmitter();
  }
  updateValue(value) {
    this._setValueErrorsStatus(value);
    this._pristine = false;
    ObservableWrapper.callNext(this._valueChanges, this._value);
    this._updateParent();
  }
  _setValueErrorsStatus(value) {
    this._value = value;
    this._errors = this.validator(this);
    this._status = isPresent(this._errors) ? INVALID : VALID;
  }
}
Object.defineProperty(Control, "parameters", {get: function() {
    return [[assert.type.any], [Function]];
  }});
Object.defineProperty(Control.prototype.updateValue, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
export class ControlGroup extends AbstractControl {
  constructor(controls, optionals = null, validator = Validators.group) {
    super(validator);
    this.controls = controls;
    this._optionals = isPresent(optionals) ? optionals : {};
    this._valueChanges = new EventEmitter();
    this._setParentForControls();
    this._setValueErrorsStatus();
  }
  include(controlName) {
    StringMapWrapper.set(this._optionals, controlName, true);
    this._updateValue();
  }
  exclude(controlName) {
    StringMapWrapper.set(this._optionals, controlName, false);
    this._updateValue();
  }
  contains(controlName) {
    var c = StringMapWrapper.contains(this.controls, controlName);
    return c && this._included(controlName);
  }
  _setParentForControls() {
    StringMapWrapper.forEach(this.controls, (control, name) => {
      control.setParent(this);
    });
  }
  _updateValue() {
    this._setValueErrorsStatus();
    this._pristine = false;
    ObservableWrapper.callNext(this._valueChanges, this._value);
    this._updateParent();
  }
  _setValueErrorsStatus() {
    this._value = this._reduceValue();
    this._errors = this.validator(this);
    this._status = isPresent(this._errors) ? INVALID : VALID;
  }
  _reduceValue() {
    return this._reduceChildren({}, (acc, control, name) => {
      acc[name] = control.value;
      return acc;
    });
  }
  _reduceChildren(initValue, fn) {
    var res = initValue;
    StringMapWrapper.forEach(this.controls, (control, name) => {
      if (this._included(name)) {
        res = fn(res, control, name);
      }
    });
    return res;
  }
  _included(controlName) {
    var isOptional = StringMapWrapper.contains(this._optionals, controlName);
    return !isOptional || StringMapWrapper.get(this._optionals, controlName);
  }
}
Object.defineProperty(ControlGroup, "parameters", {get: function() {
    return [[StringMap], [StringMap], [Function]];
  }});
Object.defineProperty(ControlGroup.prototype.include, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ControlGroup.prototype.exclude, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ControlGroup.prototype.contains, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ControlGroup.prototype._reduceChildren, "parameters", {get: function() {
    return [[assert.type.any], [Function]];
  }});
Object.defineProperty(ControlGroup.prototype._included, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class ControlArray extends AbstractControl {
  constructor(controls, validator = Validators.array) {
    super(validator);
    this.controls = controls;
    this._valueChanges = new EventEmitter();
    this._setParentForControls();
    this._setValueErrorsStatus();
  }
  at(index) {
    return this.controls[index];
  }
  push(control) {
    ListWrapper.push(this.controls, control);
    control.setParent(this);
    this._updateValue();
  }
  insert(index, control) {
    ListWrapper.insert(this.controls, index, control);
    control.setParent(this);
    this._updateValue();
  }
  removeAt(index) {
    ListWrapper.removeAt(this.controls, index);
    this._updateValue();
  }
  get length() {
    return this.controls.length;
  }
  _updateValue() {
    this._setValueErrorsStatus();
    this._pristine = false;
    ObservableWrapper.callNext(this._valueChanges, this._value);
    this._updateParent();
  }
  _setParentForControls() {
    ListWrapper.forEach(this.controls, (control) => {
      control.setParent(this);
    });
  }
  _setValueErrorsStatus() {
    this._value = ListWrapper.map(this.controls, (c) => c.value);
    this._errors = this.validator(this);
    this._status = isPresent(this._errors) ? INVALID : VALID;
  }
}
Object.defineProperty(ControlArray, "parameters", {get: function() {
    return [[assert.genericType(List, AbstractControl)], [Function]];
  }});
Object.defineProperty(ControlArray.prototype.at, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ControlArray.prototype.push, "parameters", {get: function() {
    return [[AbstractControl]];
  }});
Object.defineProperty(ControlArray.prototype.insert, "parameters", {get: function() {
    return [[assert.type.number], [AbstractControl]];
  }});
Object.defineProperty(ControlArray.prototype.removeAt, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
//# sourceMappingURL=model.js.map

//# sourceMappingURL=./model.map