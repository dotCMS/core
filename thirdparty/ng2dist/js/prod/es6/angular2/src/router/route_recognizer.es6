import {RegExp,
  RegExpWrapper,
  StringWrapper,
  isPresent} from 'angular2/src/facade/lang';
import {Map,
  MapWrapper,
  List,
  ListWrapper,
  StringMap,
  StringMapWrapper} from 'angular2/src/facade/collection';
import {PathRecognizer} from './path_recognizer';
export class RouteRecognizer {
  constructor() {
    this.names = MapWrapper.create();
    this.matchers = MapWrapper.create();
    this.redirects = MapWrapper.create();
  }
  addRedirect(path, target) {
    MapWrapper.set(this.redirects, path, target);
  }
  addConfig(path, handler, alias = null) {
    var recognizer = new PathRecognizer(path, handler);
    MapWrapper.set(this.matchers, recognizer.regex, recognizer);
    if (isPresent(alias)) {
      MapWrapper.set(this.names, alias, recognizer);
    }
  }
  recognize(url) {
    var solutions = [];
    MapWrapper.forEach(this.redirects, (target, path) => {
      if (StringWrapper.startsWith(url, path)) {
        url = target + StringWrapper.substring(url, path.length);
      }
    });
    MapWrapper.forEach(this.matchers, (pathRecognizer, regex) => {
      var match;
      if (isPresent(match = RegExpWrapper.firstMatch(regex, url))) {
        var solution = StringMapWrapper.create();
        StringMapWrapper.set(solution, 'handler', pathRecognizer.handler);
        StringMapWrapper.set(solution, 'params', pathRecognizer.parseParams(url));
        if (url === '/') {
          StringMapWrapper.set(solution, 'matchedUrl', '/');
          StringMapWrapper.set(solution, 'unmatchedUrl', '');
        } else {
          StringMapWrapper.set(solution, 'matchedUrl', match[0]);
          var unmatchedUrl = StringWrapper.substring(url, match[0].length);
          StringMapWrapper.set(solution, 'unmatchedUrl', unmatchedUrl);
        }
        ListWrapper.push(solutions, solution);
      }
    });
    return solutions;
  }
  hasRoute(name) {
    return MapWrapper.contains(this.names, name);
  }
  generate(name, params) {
    var pathRecognizer = MapWrapper.get(this.names, name);
    return pathRecognizer.generate(params);
  }
}
Object.defineProperty(RouteRecognizer.prototype.addRedirect, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(RouteRecognizer.prototype.addConfig, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.any], [assert.type.string]];
  }});
Object.defineProperty(RouteRecognizer.prototype.recognize, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(RouteRecognizer.prototype.hasRoute, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(RouteRecognizer.prototype.generate, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.any]];
  }});
//# sourceMappingURL=route_recognizer.js.map

//# sourceMappingURL=./route_recognizer.map