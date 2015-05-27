import {List,
  Map,
  ListWrapper,
  MapWrapper} from 'angular2/src/facade/collection';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {int,
  isBlank,
  isPresent,
  Type,
  StringJoiner,
  assertionsEnabled} from 'angular2/src/facade/lang';
import {ProtoViewBuilder,
  ElementBinderBuilder} from '../view/proto_view_builder';
export class CompileElement {
  constructor(element, compilationUnit = '') {
    this.element = element;
    this._attrs = null;
    this._classList = null;
    this.isViewRoot = false;
    this.inheritedProtoView = null;
    this.inheritedElementBinder = null;
    this.distanceToInheritedBinder = 0;
    this.compileChildren = true;
    var tplDesc = assertionsEnabled() ? getElementDescription(element) : null;
    if (compilationUnit !== '') {
      this.elementDescription = compilationUnit;
      if (isPresent(tplDesc))
        this.elementDescription += ": " + tplDesc;
    } else {
      this.elementDescription = tplDesc;
    }
  }
  isBound() {
    return isPresent(this.inheritedElementBinder) && this.distanceToInheritedBinder === 0;
  }
  bindElement() {
    if (!this.isBound()) {
      var parentBinder = this.inheritedElementBinder;
      this.inheritedElementBinder = this.inheritedProtoView.bindElement(this.element, this.elementDescription);
      if (isPresent(parentBinder)) {
        this.inheritedElementBinder.setParent(parentBinder, this.distanceToInheritedBinder);
      }
      this.distanceToInheritedBinder = 0;
    }
    return this.inheritedElementBinder;
  }
  refreshAttrs() {
    this._attrs = null;
  }
  attrs() {
    if (isBlank(this._attrs)) {
      this._attrs = DOM.attributeMap(this.element);
    }
    return this._attrs;
  }
  refreshClassList() {
    this._classList = null;
  }
  classList() {
    if (isBlank(this._classList)) {
      this._classList = ListWrapper.create();
      var elClassList = DOM.classList(this.element);
      for (var i = 0; i < elClassList.length; i++) {
        ListWrapper.push(this._classList, elClassList[i]);
      }
    }
    return this._classList;
  }
}
function getElementDescription(domElement) {
  var buf = new StringJoiner();
  var atts = DOM.attributeMap(domElement);
  buf.add("<");
  buf.add(DOM.tagName(domElement).toLowerCase());
  addDescriptionAttribute(buf, "id", MapWrapper.get(atts, "id"));
  addDescriptionAttribute(buf, "class", MapWrapper.get(atts, "class"));
  MapWrapper.forEach(atts, (attValue, attName) => {
    if (attName !== "id" && attName !== "class") {
      addDescriptionAttribute(buf, attName, attValue);
    }
  });
  buf.add(">");
  return buf.toString();
}
function addDescriptionAttribute(buffer, attName, attValue) {
  if (isPresent(attValue)) {
    if (attValue.length === 0) {
      buffer.add(' ' + attName);
    } else {
      buffer.add(' ' + attName + '="' + attValue + '"');
    }
  }
}
Object.defineProperty(addDescriptionAttribute, "parameters", {get: function() {
    return [[StringJoiner], [assert.type.string], []];
  }});
//# sourceMappingURL=compile_element.js.map

//# sourceMappingURL=./compile_element.map