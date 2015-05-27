import {Component} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {Attribute} from 'angular2/src/core/annotations_impl/di';
import {isPresent} from 'angular2/src/facade/lang';
import {KEY_SPACE} from 'angular2_material/src/core/constants';
import {KeyboardEvent} from 'angular2/src/facade/browser';
import {NumberWrapper} from 'angular2/src/facade/lang';
export class MdSwitch {
  constructor(tabindex) {
    this.role = 'checkbox';
    this.checked = false;
    this.tabindex = isPresent(tabindex) ? NumberWrapper.parseInt(tabindex, 10) : 0;
  }
  get disabled() {
    return this.disabled_;
  }
  set disabled(value) {
    this.disabled_ = isPresent(value) && value !== false;
  }
  onKeydown(event) {
    if (event.keyCode === KEY_SPACE) {
      event.preventDefault();
      this.toggle(event);
    }
  }
  toggle(event) {
    if (this.disabled) {
      event.stopPropagation();
      return ;
    }
    this.checked = !this.checked;
  }
}
Object.defineProperty(MdSwitch, "annotations", {get: function() {
    return [new Component({
      selector: 'md-switch',
      properties: {
        'checked': 'checked',
        'disabled': 'disabled'
      },
      hostListeners: {'keydown': 'onKeydown($event)'},
      hostProperties: {
        'checked': 'attr.aria-checked',
        'disabled_': 'attr.aria-disabled',
        'role': 'attr.role'
      }
    }), new View({
      templateUrl: 'angular2_material/src/components/switcher/switch.html',
      directives: []
    })];
  }});
Object.defineProperty(MdSwitch, "parameters", {get: function() {
    return [[assert.type.string, new Attribute('tabindex')]];
  }});
Object.defineProperty(MdSwitch.prototype.onKeydown, "parameters", {get: function() {
    return [[KeyboardEvent]];
  }});
//# sourceMappingURL=switch.js.map

//# sourceMappingURL=./switch.map