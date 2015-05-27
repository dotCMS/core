import {CONST} from 'angular2/src/facade/lang';
import {List,
  Map} from 'angular2/src/facade/collection';
export class RouteConfig {
  constructor(configs) {
    this.configs = configs;
  }
}
Object.defineProperty(RouteConfig, "annotations", {get: function() {
    return [new CONST()];
  }});
Object.defineProperty(RouteConfig, "parameters", {get: function() {
    return [[assert.genericType(List, Map)]];
  }});
//# sourceMappingURL=route_config.js.map

//# sourceMappingURL=./route_config.map