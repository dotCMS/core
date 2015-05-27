import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {Instruction} from './instruction';
export class Pipeline {
  constructor() {
    this.steps = [(instruction) => instruction.traverseSync((parentInstruction, childInstruction) => {
      childInstruction.router = parentInstruction.router.childRouter(childInstruction.component);
    }), (instruction) => instruction.router.traverseOutlets((outlet, name) => {
      return outlet.canDeactivate(instruction.getChildInstruction(name));
    }), (instruction) => instruction.router.traverseOutlets((outlet, name) => {
      return outlet.canActivate(instruction.getChildInstruction(name));
    }), (instruction) => instruction.router.activateOutlets(instruction)];
  }
  process(instruction) {
    var steps = this.steps,
        currentStep = 0;
    function processOne(result = true) {
      if (currentStep >= steps.length) {
        return PromiseWrapper.resolve(result);
      }
      var step = steps[currentStep];
      currentStep += 1;
      return PromiseWrapper.resolve(step(instruction)).then(processOne);
    }
    Object.defineProperty(processOne, "parameters", {get: function() {
        return [[assert.type.any]];
      }});
    return processOne();
  }
}
Object.defineProperty(Pipeline.prototype.process, "parameters", {get: function() {
    return [[Instruction]];
  }});
//# sourceMappingURL=pipeline.js.map

//# sourceMappingURL=./pipeline.map