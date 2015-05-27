import {isPresent,
  isBlank,
  BaseException,
  assertionsEnabled,
  RegExpWrapper,
  StringWrapper} from 'angular2/src/facade/lang';
import {List,
  MapWrapper,
  ListWrapper} from 'angular2/src/facade/collection';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {Parser} from 'angular2/change_detection';
import {SelectorMatcher,
  CssSelector} from 'angular2/src/render/dom/compiler/selector';
import {CompileStep} from './compile_step';
import {CompileElement} from './compile_element';
import {CompileControl} from './compile_control';
import {DirectiveMetadata} from '../../api';
import {dashCaseToCamelCase,
  camelCaseToDashCase,
  EVENT_TARGET_SEPARATOR} from '../util';
export class DirectiveParser extends CompileStep {
  constructor(parser, directives) {
    super();
    this._parser = parser;
    this._selectorMatcher = new SelectorMatcher();
    this._directives = directives;
    for (var i = 0; i < directives.length; i++) {
      var directive = directives[i];
      var selector = CssSelector.parse(directive.selector);
      this._ensureComponentOnlyHasElementSelector(selector, directive);
      this._selectorMatcher.addSelectables(selector, i);
    }
  }
  _ensureComponentOnlyHasElementSelector(selector, directive) {
    var isElementSelector = selector.length === 1 && selector[0].isElementSelector();
    if (!isElementSelector && directive.type === DirectiveMetadata.COMPONENT_TYPE) {
      throw new BaseException(`Component '${directive.id}' can only have an element selector, but had '${directive.selector}'`);
    }
  }
  process(parent, current, control) {
    var attrs = current.attrs();
    var classList = current.classList();
    var cssSelector = new CssSelector();
    var nodeName = DOM.nodeName(current.element);
    cssSelector.setElement(nodeName);
    for (var i = 0; i < classList.length; i++) {
      cssSelector.addClassName(classList[i]);
    }
    MapWrapper.forEach(attrs, (attrValue, attrName) => {
      cssSelector.addAttribute(attrName, attrValue);
    });
    var componentDirective;
    this._selectorMatcher.match(cssSelector, (selector, directiveIndex) => {
      var elementBinder = current.bindElement();
      var directive = this._directives[directiveIndex];
      var directiveBinderBuilder = elementBinder.bindDirective(directiveIndex);
      current.compileChildren = current.compileChildren && directive.compileChildren;
      if (isPresent(directive.properties)) {
        MapWrapper.forEach(directive.properties, (bindConfig, dirProperty) => {
          this._bindDirectiveProperty(dirProperty, bindConfig, current, directiveBinderBuilder);
        });
      }
      if (isPresent(directive.hostListeners)) {
        MapWrapper.forEach(directive.hostListeners, (action, eventName) => {
          this._bindDirectiveEvent(eventName, action, current, directiveBinderBuilder);
        });
      }
      if (isPresent(directive.hostProperties)) {
        MapWrapper.forEach(directive.hostProperties, (hostPropertyName, directivePropertyName) => {
          this._bindHostProperty(hostPropertyName, directivePropertyName, current, directiveBinderBuilder);
        });
      }
      if (isPresent(directive.readAttributes)) {
        ListWrapper.forEach(directive.readAttributes, (attrName) => {
          elementBinder.readAttribute(attrName);
        });
      }
      if (directive.type === DirectiveMetadata.COMPONENT_TYPE) {
        if (isPresent(componentDirective)) {
          throw new BaseException(`Only one component directive is allowed per element - check ${current.elementDescription}`);
        }
        componentDirective = directive;
        elementBinder.setComponentId(directive.id);
      }
    });
  }
  _bindDirectiveProperty(dirProperty, bindConfig, compileElement, directiveBinderBuilder) {
    var pipes = this._splitBindConfig(bindConfig);
    var elProp = ListWrapper.removeAt(pipes, 0);
    var bindingAst = MapWrapper.get(compileElement.bindElement().propertyBindings, dashCaseToCamelCase(elProp));
    if (isBlank(bindingAst)) {
      var attributeValue = MapWrapper.get(compileElement.attrs(), camelCaseToDashCase(elProp));
      if (isPresent(attributeValue)) {
        bindingAst = this._parser.wrapLiteralPrimitive(attributeValue, compileElement.elementDescription);
      }
    }
    if (isPresent(bindingAst)) {
      var fullExpAstWithBindPipes = this._parser.addPipes(bindingAst, pipes);
      directiveBinderBuilder.bindProperty(dirProperty, fullExpAstWithBindPipes);
    }
  }
  _bindDirectiveEvent(eventName, action, compileElement, directiveBinderBuilder) {
    var ast = this._parser.parseAction(action, compileElement.elementDescription);
    if (StringWrapper.contains(eventName, EVENT_TARGET_SEPARATOR)) {
      var parts = eventName.split(EVENT_TARGET_SEPARATOR);
      directiveBinderBuilder.bindEvent(parts[1], ast, parts[0]);
    } else {
      directiveBinderBuilder.bindEvent(eventName, ast);
    }
  }
  _bindHostProperty(hostPropertyName, directivePropertyName, compileElement, directiveBinderBuilder) {
    var ast = this._parser.parseBinding(directivePropertyName, `hostProperties of ${compileElement.elementDescription}`);
    directiveBinderBuilder.bindHostProperty(hostPropertyName, ast);
  }
  _splitBindConfig(bindConfig) {
    return ListWrapper.map(bindConfig.split('|'), (s) => s.trim());
  }
}
Object.defineProperty(DirectiveParser, "parameters", {get: function() {
    return [[Parser], [assert.genericType(List, DirectiveMetadata)]];
  }});
Object.defineProperty(DirectiveParser.prototype.process, "parameters", {get: function() {
    return [[CompileElement], [CompileElement], [CompileControl]];
  }});
Object.defineProperty(DirectiveParser.prototype._splitBindConfig, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=directive_parser.js.map

//# sourceMappingURL=./directive_parser.map