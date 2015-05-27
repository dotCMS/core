import {isBlank,
  isPresent,
  BaseException,
  StringWrapper} from 'angular2/src/facade/lang';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {MapWrapper,
  ListWrapper} from 'angular2/src/facade/collection';
import {Parser} from 'angular2/change_detection';
import {CompileStep} from './compile_step';
import {CompileElement} from './compile_element';
import {CompileControl} from './compile_control';
import {dashCaseToCamelCase} from '../util';
export class ViewSplitter extends CompileStep {
  constructor(parser) {
    super();
    this._parser = parser;
  }
  process(parent, current, control) {
    var attrs = current.attrs();
    var templateBindings = MapWrapper.get(attrs, 'template');
    var hasTemplateBinding = isPresent(templateBindings);
    MapWrapper.forEach(attrs, (attrValue, attrName) => {
      if (StringWrapper.startsWith(attrName, '*')) {
        var key = StringWrapper.substring(attrName, 1);
        if (hasTemplateBinding) {
          throw new BaseException(`Only one template directive per element is allowed: ` + `${templateBindings} and ${key} cannot be used simultaneously ` + `in ${current.elementDescription}`);
        } else {
          templateBindings = (attrValue.length == 0) ? key : key + ' ' + attrValue;
          hasTemplateBinding = true;
        }
      }
    });
    if (isPresent(parent)) {
      if (DOM.isTemplateElement(current.element)) {
        if (!current.isViewRoot) {
          var viewRoot = new CompileElement(DOM.createTemplate(''));
          viewRoot.inheritedProtoView = current.bindElement().bindNestedProtoView(viewRoot.element);
          viewRoot.elementDescription = current.elementDescription;
          viewRoot.isViewRoot = true;
          this._moveChildNodes(DOM.content(current.element), DOM.content(viewRoot.element));
          control.addChild(viewRoot);
        }
      }
      if (hasTemplateBinding) {
        var newParent = new CompileElement(DOM.createTemplate(''));
        newParent.inheritedProtoView = current.inheritedProtoView;
        newParent.inheritedElementBinder = current.inheritedElementBinder;
        newParent.distanceToInheritedBinder = current.distanceToInheritedBinder;
        newParent.elementDescription = current.elementDescription;
        current.inheritedProtoView = newParent.bindElement().bindNestedProtoView(current.element);
        current.inheritedElementBinder = null;
        current.distanceToInheritedBinder = 0;
        current.isViewRoot = true;
        this._parseTemplateBindings(templateBindings, newParent);
        this._addParentElement(current.element, newParent.element);
        control.addParent(newParent);
        DOM.remove(current.element);
      }
    }
  }
  _moveChildNodes(source, target) {
    var next = DOM.firstChild(source);
    while (isPresent(next)) {
      DOM.appendChild(target, next);
      next = DOM.firstChild(source);
    }
  }
  _addParentElement(currentElement, newParentElement) {
    DOM.insertBefore(currentElement, newParentElement);
    DOM.appendChild(newParentElement, currentElement);
  }
  _parseTemplateBindings(templateBindings, compileElement) {
    var bindings = this._parser.parseTemplateBindings(templateBindings, compileElement.elementDescription);
    for (var i = 0; i < bindings.length; i++) {
      var binding = bindings[i];
      if (binding.keyIsVar) {
        compileElement.bindElement().bindVariable(dashCaseToCamelCase(binding.key), binding.name);
        MapWrapper.set(compileElement.attrs(), binding.key, binding.name);
      } else if (isPresent(binding.expression)) {
        compileElement.bindElement().bindProperty(dashCaseToCamelCase(binding.key), binding.expression);
        MapWrapper.set(compileElement.attrs(), binding.key, binding.expression.source);
      } else {
        DOM.setAttribute(compileElement.element, binding.key, '');
      }
    }
  }
}
Object.defineProperty(ViewSplitter, "parameters", {get: function() {
    return [[Parser]];
  }});
Object.defineProperty(ViewSplitter.prototype.process, "parameters", {get: function() {
    return [[CompileElement], [CompileElement], [CompileControl]];
  }});
Object.defineProperty(ViewSplitter.prototype._parseTemplateBindings, "parameters", {get: function() {
    return [[assert.type.string], [CompileElement]];
  }});
//# sourceMappingURL=view_splitter.js.map

//# sourceMappingURL=./view_splitter.map