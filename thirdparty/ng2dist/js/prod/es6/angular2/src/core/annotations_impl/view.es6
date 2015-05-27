import {ABSTRACT,
  CONST,
  Type} from 'angular2/src/facade/lang';
export class View {
  constructor({templateUrl,
    template,
    directives,
    renderer}) {
    this.templateUrl = templateUrl;
    this.template = template;
    this.directives = directives;
    this.renderer = renderer;
  }
}
Object.defineProperty(View, "annotations", {get: function() {
    return [new CONST()];
  }});
//# sourceMappingURL=view.js.map

//# sourceMappingURL=./view.map