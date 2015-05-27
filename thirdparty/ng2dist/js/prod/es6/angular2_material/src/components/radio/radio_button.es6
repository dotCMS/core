import {Component,
  onChange} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {Parent,
  Ancestor} from 'angular2/src/core/annotations_impl/visibility';
import {Attribute} from 'angular2/src/core/annotations_impl/di';
import {Optional} from 'angular2/src/di/annotations';
import {MdRadioDispatcher} from 'angular2_material/src/components/radio/radio_dispatcher';
import {isPresent,
  StringWrapper,
  NumberWrapper} from 'angular2/src/facade/lang';
import {ObservableWrapper,
  EventEmitter} from 'angular2/src/facade/async';
import {ListWrapper} from 'angular2/src/facade/collection';
import {KEY_UP,
  KEY_DOWN,
  KEY_SPACE} from 'angular2_material/src/core/constants';
import {Event,
  KeyboardEvent} from 'angular2/src/facade/browser';
var _uniqueIdCounter = 0;
export class MdRadioButton {
  constructor(radioGroup, id, tabindex, radioDispatcher) {
    this.radioGroup = radioGroup;
    this.radioDispatcher = radioDispatcher;
    this.value = null;
    this.role = 'radio';
    this.checked = false;
    this.id = isPresent(id) ? id : `md-radio-${_uniqueIdCounter++}`;
    ;
    radioDispatcher.listen((name) => {
      if (name == this.name) {
        this.checked = false;
      }
    });
    if (isPresent(radioGroup)) {
      this.name = radioGroup.getName();
      this.radioGroup.register(this);
    }
    if (!isPresent(radioGroup)) {
      this.tabindex = isPresent(tabindex) ? NumberWrapper.parseInt(tabindex, 10) : 0;
    } else {
      this.tabindex = -1;
    }
  }
  onChange(_) {
    if (isPresent(this.radioGroup)) {
      this.name = this.radioGroup.getName();
    }
  }
  isDisabled() {
    return this.disabled || (isPresent(this.disabled) && StringWrapper.equals(this.disabled, '')) || (isPresent(this.radioGroup) && this.radioGroup.disabled);
  }
  get disabled() {
    return this.disabled_;
  }
  set disabled(value) {
    this.disabled_ = isPresent(value) && value !== false;
  }
  select(event) {
    if (this.isDisabled()) {
      event.stopPropagation();
      return ;
    }
    this.radioDispatcher.notify(this.name);
    this.checked = true;
    if (isPresent(this.radioGroup)) {
      this.radioGroup.updateValue(this.value, this.id);
    }
  }
  onKeydown(event) {
    if (event.keyCode == KEY_SPACE) {
      event.preventDefault();
      this.select(event);
    }
  }
}
Object.defineProperty(MdRadioButton, "annotations", {get: function() {
    return [new Component({
      selector: 'md-radio-button',
      lifecycle: [onChange],
      properties: {
        'id': 'id',
        'name': 'name',
        'value': 'value',
        'checked': 'checked',
        'disabled': 'disabled'
      },
      hostListeners: {'keydown': 'onKeydown($event)'},
      hostProperties: {
        'id': 'id',
        'tabindex': 'tabindex',
        'role': 'attr.role',
        'checked': 'attr.aria-checked',
        'disabled': 'attr.aria-disabled'
      }
    }), new View({
      templateUrl: 'angular2_material/src/components/radio/radio_button.html',
      directives: []
    })];
  }});
Object.defineProperty(MdRadioButton, "parameters", {get: function() {
    return [[MdRadioGroup, new Optional(), new Parent()], [assert.type.string, new Attribute('id')], [assert.type.string, new Attribute('tabindex')], [MdRadioDispatcher]];
  }});
Object.defineProperty(MdRadioButton.prototype.select, "parameters", {get: function() {
    return [[Event]];
  }});
Object.defineProperty(MdRadioButton.prototype.onKeydown, "parameters", {get: function() {
    return [[KeyboardEvent]];
  }});
export class MdRadioGroup {
  constructor(tabindex, disabled, radioDispatcher) {
    this.name_ = `md-radio-group-${_uniqueIdCounter++}`;
    this.radios_ = [];
    this.change = new EventEmitter();
    this.radioDispatcher = radioDispatcher;
    this.selectedRadioId = '';
    this.disabled_ = false;
    this.role = 'radiogroup';
    this.disabled = isPresent(disabled);
    this.tabindex = isPresent(tabindex) ? NumberWrapper.parseInt(tabindex, 10) : 0;
  }
  getName() {
    return this.name_;
  }
  get disabled() {
    return this.disabled_;
  }
  set disabled(value) {
    this.disabled_ = isPresent(value) && value !== false;
  }
  onChange(_) {
    this.disabled = isPresent(this.disabled) && this.disabled !== false;
    if (isPresent(this.value) && this.value != '') {
      this.radioDispatcher.notify(this.name_);
      ListWrapper.forEach(this.radios_, (radio) => {
        if (radio.value == this.value) {
          radio.checked = true;
          this.selectedRadioId = radio.id;
          this.activedescendant = radio.id;
        }
      });
    }
  }
  updateValue(value, id) {
    this.value = value;
    this.selectedRadioId = id;
    this.activedescendant = id;
    ObservableWrapper.callNext(this.change, null);
  }
  register(radio) {
    ListWrapper.push(this.radios_, radio);
  }
  onKeydown(event) {
    if (this.disabled) {
      return ;
    }
    switch (event.keyCode) {
      case KEY_UP:
        this.stepSelectedRadio(-1);
        event.preventDefault();
        break;
      case KEY_DOWN:
        this.stepSelectedRadio(1);
        event.preventDefault();
        break;
    }
  }
  getSelectedRadioIndex() {
    for (var i = 0; i < this.radios_.length; i++) {
      if (this.radios_[i].id == this.selectedRadioId) {
        return i;
      }
    }
    return -1;
  }
  stepSelectedRadio(step) {
    var index = this.getSelectedRadioIndex() + step;
    if (index < 0 || index >= this.radios_.length) {
      return ;
    }
    var radio = this.radios_[index];
    if (radio.disabled) {
      this.stepSelectedRadio(step + (step < 0 ? -1 : 1));
      return ;
    }
    this.radioDispatcher.notify(this.name_);
    radio.checked = true;
    ObservableWrapper.callNext(this.change, null);
    this.value = radio.value;
    this.selectedRadioId = radio.id;
    this.activedescendant = radio.id;
  }
}
Object.defineProperty(MdRadioGroup, "annotations", {get: function() {
    return [new Component({
      selector: 'md-radio-group',
      lifecycle: [onChange],
      events: ['change'],
      properties: {
        'disabled': 'disabled',
        'value': 'value'
      },
      hostListeners: {'^keydown': 'onKeydown($event)'},
      hostProperties: {
        'tabindex': 'tabindex',
        'role': 'attr.role',
        'disabled': 'attr.aria-disabled',
        'activedescendant': 'attr.aria-activedescendant'
      }
    }), new View({templateUrl: 'angular2_material/src/components/radio/radio_group.html'})];
  }});
Object.defineProperty(MdRadioGroup, "parameters", {get: function() {
    return [[assert.type.string, new Attribute('tabindex')], [assert.type.string, new Attribute('disabled')], [MdRadioDispatcher]];
  }});
Object.defineProperty(MdRadioGroup.prototype.updateValue, "parameters", {get: function() {
    return [[assert.type.any], [assert.type.string]];
  }});
Object.defineProperty(MdRadioGroup.prototype.register, "parameters", {get: function() {
    return [[MdRadioButton]];
  }});
Object.defineProperty(MdRadioGroup.prototype.onKeydown, "parameters", {get: function() {
    return [[KeyboardEvent]];
  }});
//# sourceMappingURL=radio_button.js.map

//# sourceMappingURL=./radio_button.map