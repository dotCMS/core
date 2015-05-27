import {Component,
  onChange} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {isPresent} from 'angular2/src/facade/lang';
export class MdButton {}
Object.defineProperty(MdButton, "annotations", {get: function() {
    return [new Component({selector: '[md-button]:not([href])'}), new View({templateUrl: 'angular2_material/src/components/button/button.html'})];
  }});
export class MdAnchor {
  onClick(event) {
    if (isPresent(this.disabled) && this.disabled !== false) {
      event.preventDefault();
    }
  }
  onChange(_) {
    this.tabIndex = this.disabled ? -1 : 0;
  }
}
Object.defineProperty(MdAnchor, "annotations", {get: function() {
    return [new Component({
      selector: '[md-button][href]',
      properties: {'disabled': 'disabled'},
      hostListeners: {'click': 'onClick($event)'},
      hostProperties: {'tabIndex': 'tabIndex'},
      lifecycle: [onChange]
    }), new View({templateUrl: 'angular2_material/src/components/button/button.html'})];
  }});
//# sourceMappingURL=button.js.map

//# sourceMappingURL=./button.map