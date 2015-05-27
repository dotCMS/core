import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {isBlank,
  isPresent,
  BaseException,
  CONST} from 'angular2/src/facade/lang';
import {Pipe} from './pipe';
import {Injectable} from 'angular2/di';
import {ChangeDetectorRef} from '../change_detector_ref';
export class PipeRegistry {
  constructor(config) {
    this.config = config;
  }
  get(type, obj, cdRef) {
    var listOfConfigs = this.config[type];
    if (isBlank(listOfConfigs)) {
      throw new BaseException(`Cannot find '${type}' pipe supporting object '${obj}'`);
    }
    var matchingConfig = ListWrapper.find(listOfConfigs, (pipeConfig) => pipeConfig.supports(obj));
    if (isBlank(matchingConfig)) {
      throw new BaseException(`Cannot find '${type}' pipe supporting object '${obj}'`);
    }
    return matchingConfig.create(cdRef);
  }
}
Object.defineProperty(PipeRegistry, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(PipeRegistry.prototype.get, "parameters", {get: function() {
    return [[assert.type.string], [], [ChangeDetectorRef]];
  }});
//# sourceMappingURL=pipe_registry.js.map

//# sourceMappingURL=./pipe_registry.map