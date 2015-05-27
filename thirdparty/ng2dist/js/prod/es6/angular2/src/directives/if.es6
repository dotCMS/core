import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {ViewContainerRef} from 'angular2/src/core/compiler/view_container_ref';
import {ProtoViewRef} from 'angular2/src/core/compiler/view_ref';
import {isBlank} from 'angular2/src/facade/lang';
export class If {
  constructor(viewContainer, protoViewRef) {
    this.viewContainer = viewContainer;
    this.prevCondition = null;
    this.protoViewRef = protoViewRef;
  }
  set condition(newCondition) {
    if (newCondition && (isBlank(this.prevCondition) || !this.prevCondition)) {
      this.prevCondition = true;
      this.viewContainer.create(this.protoViewRef);
    } else if (!newCondition && (isBlank(this.prevCondition) || this.prevCondition)) {
      this.prevCondition = false;
      this.viewContainer.clear();
    }
  }
}
Object.defineProperty(If, "annotations", {get: function() {
    return [new Directive({
      selector: '[if]',
      properties: {'condition': 'if'}
    })];
  }});
Object.defineProperty(If, "parameters", {get: function() {
    return [[ViewContainerRef], [ProtoViewRef]];
  }});
//# sourceMappingURL=if.js.map

//# sourceMappingURL=./if.map