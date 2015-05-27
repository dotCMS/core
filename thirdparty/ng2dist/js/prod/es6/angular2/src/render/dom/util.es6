import {StringWrapper,
  RegExpWrapper,
  isPresent} from 'angular2/src/facade/lang';
export const NG_BINDING_CLASS_SELECTOR = '.ng-binding';
export const NG_BINDING_CLASS = 'ng-binding';
export const EVENT_TARGET_SEPARATOR = ':';
var CAMEL_CASE_REGEXP = RegExpWrapper.create('([A-Z])');
var DASH_CASE_REGEXP = RegExpWrapper.create('-([a-z])');
export function camelCaseToDashCase(input) {
  return StringWrapper.replaceAllMapped(input, CAMEL_CASE_REGEXP, (m) => {
    return '-' + m[1].toLowerCase();
  });
}
Object.defineProperty(camelCaseToDashCase, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export function dashCaseToCamelCase(input) {
  return StringWrapper.replaceAllMapped(input, DASH_CASE_REGEXP, (m) => {
    return m[1].toUpperCase();
  });
}
Object.defineProperty(dashCaseToCamelCase, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=util.js.map

//# sourceMappingURL=./util.map