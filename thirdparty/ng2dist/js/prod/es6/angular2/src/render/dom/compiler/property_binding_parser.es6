import {isPresent,
  RegExpWrapper} from 'angular2/src/facade/lang';
import {MapWrapper} from 'angular2/src/facade/collection';
import {Parser} from 'angular2/change_detection';
import {CompileStep} from './compile_step';
import {CompileElement} from './compile_element';
import {CompileControl} from './compile_control';
import {dashCaseToCamelCase} from '../util';
var BIND_NAME_REGEXP = RegExpWrapper.create('^(?:(?:(?:(bind-)|(var-|#)|(on-))(.+))|\\[([^\\]]+)\\]|\\(([^\\)]+)\\))$');
export class PropertyBindingParser extends CompileStep {
  constructor(parser) {
    super();
    this._parser = parser;
  }
  process(parent, current, control) {
    var attrs = current.attrs();
    var newAttrs = MapWrapper.create();
    MapWrapper.forEach(attrs, (attrValue, attrName) => {
      var bindParts = RegExpWrapper.firstMatch(BIND_NAME_REGEXP, attrName);
      if (isPresent(bindParts)) {
        if (isPresent(bindParts[1])) {
          this._bindProperty(bindParts[4], attrValue, current, newAttrs);
        } else if (isPresent(bindParts[2])) {
          var identifier = bindParts[4];
          var value = attrValue == '' ? '\$implicit' : attrValue;
          this._bindVariable(identifier, value, current, newAttrs);
        } else if (isPresent(bindParts[3])) {
          this._bindEvent(bindParts[4], attrValue, current, newAttrs);
        } else if (isPresent(bindParts[5])) {
          this._bindProperty(bindParts[5], attrValue, current, newAttrs);
        } else if (isPresent(bindParts[6])) {
          this._bindEvent(bindParts[6], attrValue, current, newAttrs);
        }
      } else {
        var expr = this._parser.parseInterpolation(attrValue, current.elementDescription);
        if (isPresent(expr)) {
          this._bindPropertyAst(attrName, expr, current, newAttrs);
        }
      }
    });
    MapWrapper.forEach(newAttrs, (attrValue, attrName) => {
      MapWrapper.set(attrs, attrName, attrValue);
    });
  }
  _bindVariable(identifier, value, current, newAttrs) {
    current.bindElement().bindVariable(dashCaseToCamelCase(identifier), value);
    MapWrapper.set(newAttrs, identifier, value);
  }
  _bindProperty(name, expression, current, newAttrs) {
    this._bindPropertyAst(name, this._parser.parseBinding(expression, current.elementDescription), current, newAttrs);
  }
  _bindPropertyAst(name, ast, current, newAttrs) {
    var binder = current.bindElement();
    var camelCaseName = dashCaseToCamelCase(name);
    binder.bindProperty(camelCaseName, ast);
    MapWrapper.set(newAttrs, name, ast.source);
  }
  _bindEvent(name, expression, current, newAttrs) {
    current.bindElement().bindEvent(dashCaseToCamelCase(name), this._parser.parseAction(expression, current.elementDescription));
  }
}
Object.defineProperty(PropertyBindingParser, "parameters", {get: function() {
    return [[Parser]];
  }});
Object.defineProperty(PropertyBindingParser.prototype.process, "parameters", {get: function() {
    return [[CompileElement], [CompileElement], [CompileControl]];
  }});
Object.defineProperty(PropertyBindingParser.prototype._bindVariable, "parameters", {get: function() {
    return [[], [], [CompileElement], []];
  }});
Object.defineProperty(PropertyBindingParser.prototype._bindProperty, "parameters", {get: function() {
    return [[], [], [CompileElement], []];
  }});
Object.defineProperty(PropertyBindingParser.prototype._bindPropertyAst, "parameters", {get: function() {
    return [[], [], [CompileElement], []];
  }});
Object.defineProperty(PropertyBindingParser.prototype._bindEvent, "parameters", {get: function() {
    return [[], [], [CompileElement], []];
  }});
//# sourceMappingURL=property_binding_parser.js.map

//# sourceMappingURL=./property_binding_parser.map