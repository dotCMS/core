import {Directive} from 'angular2/angular2';
export class MdTheme {
  constructor() {
    this.color = 'sky-blue';
  }
}
Object.defineProperty(MdTheme, "annotations", {get: function() {
    return [new Directive({selector: '[md-theme]'})];
  }});
//# sourceMappingURL=theme.js.map

//# sourceMappingURL=./theme.map