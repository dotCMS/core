import {Component,
  onChange} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {Attribute} from 'angular2/src/core/annotations_impl/di';
import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import {Math} from 'angular2/src/facade/math';
export class MdProgressLinear {
  constructor(mode) {
    this.primaryBarTransform = '';
    this.secondaryBarTransform = '';
    this.role = 'progressbar';
    this.ariaValuemin = '0';
    this.ariaValuemax = '100';
    this.mode = isPresent(mode) ? mode : Mode.DETERMINATE;
  }
  get value() {
    return this.value_;
  }
  set value(v) {
    if (isPresent(v)) {
      this.value_ = MdProgressLinear.clamp(v);
    }
  }
  onChange(_) {
    if (this.mode == Mode['QUERY'] || this.mode == Mode['INDETERMINATE'] || isBlank(this.value)) {
      return ;
    }
    this.primaryBarTransform = this.transformForValue(this.value);
    if (this.mode == Mode['BUFFER']) {
      this.secondaryBarTransform = this.transformForValue(this.bufferValue);
    }
  }
  transformForValue(value) {
    var scale = value / 100;
    var translateX = (value - 100) / 2;
    return `translateX(${translateX}%) scale(${scale}, 1)`;
  }
  static clamp(v) {
    return Math.max(0, Math.min(100, v));
  }
}
Object.defineProperty(MdProgressLinear, "annotations", {get: function() {
    return [new Component({
      selector: 'md-progress-linear',
      lifecycle: [onChange],
      properties: {
        'value': 'value',
        'bufferValue': 'buffer-value'
      },
      hostProperties: {
        'role': 'attr.role',
        'ariaValuemin': 'attr.aria-valuemin',
        'ariaValuemax': 'attr.aria-valuemax',
        'value': 'attr.aria-valuenow'
      }
    }), new View({
      templateUrl: 'angular2_material/src/components/progress-linear/progress_linear.html',
      directives: []
    })];
  }});
Object.defineProperty(MdProgressLinear, "parameters", {get: function() {
    return [[assert.type.string, new Attribute('md-mode')]];
  }});
var Mode = {
  'DETERMINATE': 'determinate',
  'INDETERMINATE': 'indeterminate',
  'BUFFER': 'buffer',
  'QUERY': 'query'
};
//# sourceMappingURL=progress_linear.js.map

//# sourceMappingURL=./progress_linear.map