import {RegExp,
  RegExpWrapper,
  RegExpMatcherWrapper,
  StringWrapper,
  isPresent} from 'angular2/src/facade/lang';
import {Map,
  MapWrapper,
  StringMap,
  StringMapWrapper,
  List,
  ListWrapper} from 'angular2/src/facade/collection';
import {escapeRegex} from './url';
class StaticSegment {
  constructor(string) {
    this.string = string;
    this.name = '';
    this.regex = escapeRegex(string);
  }
  generate(params) {
    return this.string;
  }
}
Object.defineProperty(StaticSegment, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
class DynamicSegment {
  constructor(name) {
    this.name = name;
    this.regex = "([^/]+)";
  }
  generate(params) {
    return StringMapWrapper.get(params, this.name);
  }
}
Object.defineProperty(DynamicSegment, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(DynamicSegment.prototype.generate, "parameters", {get: function() {
    return [[StringMap]];
  }});
class StarSegment {
  constructor(name) {
    this.name = name;
    this.regex = "(.+)";
  }
  generate(params) {
    return StringMapWrapper.get(params, this.name);
  }
}
Object.defineProperty(StarSegment, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(StarSegment.prototype.generate, "parameters", {get: function() {
    return [[StringMap]];
  }});
var paramMatcher = RegExpWrapper.create("^:([^\/]+)$");
var wildcardMatcher = RegExpWrapper.create("^\\*([^\/]+)$");
function parsePathString(route) {
  if (route[0] === "/") {
    route = StringWrapper.substring(route, 1);
  }
  var segments = splitBySlash(route);
  var results = ListWrapper.create();
  for (var i = 0; i < segments.length; i++) {
    var segment = segments[i],
        match;
    if (isPresent(match = RegExpWrapper.firstMatch(paramMatcher, segment))) {
      ListWrapper.push(results, new DynamicSegment(match[1]));
    } else if (isPresent(match = RegExpWrapper.firstMatch(wildcardMatcher, segment))) {
      ListWrapper.push(results, new StarSegment(match[1]));
    } else if (segment.length > 0) {
      ListWrapper.push(results, new StaticSegment(segment));
    }
  }
  return results;
}
Object.defineProperty(parsePathString, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
var SLASH_RE = RegExpWrapper.create('/');
function splitBySlash(url) {
  return StringWrapper.split(url, SLASH_RE);
}
Object.defineProperty(splitBySlash, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class PathRecognizer {
  constructor(path, handler) {
    this.handler = handler;
    this.segments = ListWrapper.create();
    var segments = parsePathString(path);
    var regexString = '^';
    ListWrapper.forEach(segments, (segment) => {
      regexString += '/' + segment.regex;
    });
    this.regex = RegExpWrapper.create(regexString);
    this.segments = segments;
  }
  parseParams(url) {
    var params = StringMapWrapper.create();
    var urlPart = url;
    for (var i = 0; i < this.segments.length; i++) {
      var segment = this.segments[i];
      var match = RegExpWrapper.firstMatch(RegExpWrapper.create('/' + segment.regex), urlPart);
      urlPart = StringWrapper.substring(urlPart, match[0].length);
      if (segment.name.length > 0) {
        StringMapWrapper.set(params, segment.name, match[1]);
      }
    }
    return params;
  }
  generate(params) {
    return ListWrapper.join(ListWrapper.map(this.segments, (segment) => '/' + segment.generate(params)), '');
  }
}
Object.defineProperty(PathRecognizer, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.any]];
  }});
Object.defineProperty(PathRecognizer.prototype.parseParams, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(PathRecognizer.prototype.generate, "parameters", {get: function() {
    return [[StringMap]];
  }});
//# sourceMappingURL=path_recognizer.js.map

//# sourceMappingURL=./path_recognizer.map