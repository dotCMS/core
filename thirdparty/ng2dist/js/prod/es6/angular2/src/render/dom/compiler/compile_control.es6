import {isBlank} from 'angular2/src/facade/lang';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {CompileElement} from './compile_element';
import {CompileStep} from './compile_step';
export class CompileControl {
  constructor(steps) {
    this._steps = steps;
    this._currentStepIndex = 0;
    this._parent = null;
    this._results = null;
    this._additionalChildren = null;
  }
  internalProcess(results, startStepIndex, parent, current) {
    this._results = results;
    var previousStepIndex = this._currentStepIndex;
    var previousParent = this._parent;
    this._ignoreCurrentElement = false;
    for (var i = startStepIndex; i < this._steps.length && !this._ignoreCurrentElement; i++) {
      var step = this._steps[i];
      this._parent = parent;
      this._currentStepIndex = i;
      step.process(parent, current, this);
      parent = this._parent;
    }
    if (!this._ignoreCurrentElement) {
      ListWrapper.push(results, current);
    }
    this._currentStepIndex = previousStepIndex;
    this._parent = previousParent;
    var localAdditionalChildren = this._additionalChildren;
    this._additionalChildren = null;
    return localAdditionalChildren;
  }
  addParent(newElement) {
    this.internalProcess(this._results, this._currentStepIndex + 1, this._parent, newElement);
    this._parent = newElement;
  }
  addChild(element) {
    if (isBlank(this._additionalChildren)) {
      this._additionalChildren = ListWrapper.create();
    }
    ListWrapper.push(this._additionalChildren, element);
  }
  ignoreCurrentElement() {
    this._ignoreCurrentElement = true;
  }
}
Object.defineProperty(CompileControl.prototype.internalProcess, "parameters", {get: function() {
    return [[], [], [CompileElement], [CompileElement]];
  }});
Object.defineProperty(CompileControl.prototype.addParent, "parameters", {get: function() {
    return [[CompileElement]];
  }});
Object.defineProperty(CompileControl.prototype.addChild, "parameters", {get: function() {
    return [[CompileElement]];
  }});
//# sourceMappingURL=compile_control.js.map

//# sourceMappingURL=./compile_control.map