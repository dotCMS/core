import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {Validators} from './validators';
import {ControlDirective} from './directives';
export class RequiredValidatorDirective {
  constructor(c) {
    c.validator = Validators.compose([c.validator, Validators.required]);
  }
}
Object.defineProperty(RequiredValidatorDirective, "annotations", {get: function() {
    return [new Directive({selector: '[required]'})];
  }});
Object.defineProperty(RequiredValidatorDirective, "parameters", {get: function() {
    return [[ControlDirective]];
  }});
//# sourceMappingURL=validator_directives.js.map

//# sourceMappingURL=./validator_directives.map