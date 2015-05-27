import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import {SetterFn} from 'angular2/src/reflection/types';
import {AST} from './parser/ast';
import {DirectiveIndex,
  DirectiveRecord} from './directive_record';
const DIRECTIVE = "directive";
const ELEMENT = "element";
const TEXT_NODE = "textNode";
export class BindingRecord {
  constructor(mode, implicitReceiver, ast, elementIndex, propertyName, setter, directiveRecord) {
    this.mode = mode;
    this.implicitReceiver = implicitReceiver;
    this.ast = ast;
    this.elementIndex = elementIndex;
    this.propertyName = propertyName;
    this.setter = setter;
    this.directiveRecord = directiveRecord;
  }
  callOnChange() {
    return isPresent(this.directiveRecord) && this.directiveRecord.callOnChange;
  }
  isOnPushChangeDetection() {
    return isPresent(this.directiveRecord) && this.directiveRecord.isOnPushChangeDetection();
  }
  isDirective() {
    return this.mode === DIRECTIVE;
  }
  isElement() {
    return this.mode === ELEMENT;
  }
  isTextNode() {
    return this.mode === TEXT_NODE;
  }
  static createForDirective(ast, propertyName, setter, directiveRecord) {
    return new BindingRecord(DIRECTIVE, 0, ast, 0, propertyName, setter, directiveRecord);
  }
  static createForElement(ast, elementIndex, propertyName) {
    return new BindingRecord(ELEMENT, 0, ast, elementIndex, propertyName, null, null);
  }
  static createForHostProperty(directiveIndex, ast, propertyName) {
    return new BindingRecord(ELEMENT, directiveIndex, ast, directiveIndex.elementIndex, propertyName, null, null);
  }
  static createForTextNode(ast, elementIndex) {
    return new BindingRecord(TEXT_NODE, 0, ast, elementIndex, null, null, null);
  }
}
Object.defineProperty(BindingRecord, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.any], [AST], [assert.type.number], [assert.type.string], [SetterFn], [DirectiveRecord]];
  }});
Object.defineProperty(BindingRecord.createForDirective, "parameters", {get: function() {
    return [[AST], [assert.type.string], [SetterFn], [DirectiveRecord]];
  }});
Object.defineProperty(BindingRecord.createForElement, "parameters", {get: function() {
    return [[AST], [assert.type.number], [assert.type.string]];
  }});
Object.defineProperty(BindingRecord.createForHostProperty, "parameters", {get: function() {
    return [[DirectiveIndex], [AST], [assert.type.string]];
  }});
Object.defineProperty(BindingRecord.createForTextNode, "parameters", {get: function() {
    return [[AST], [assert.type.number]];
  }});
//# sourceMappingURL=binding_record.js.map

//# sourceMappingURL=./binding_record.map