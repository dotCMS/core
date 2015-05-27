import {isPresent,
  isBlank,
  BaseException,
  Type,
  isString} from 'angular2/src/facade/lang';
import {List,
  ListWrapper,
  MapWrapper,
  StringMapWrapper} from 'angular2/src/facade/collection';
import {AccessMember,
  Assignment,
  AST,
  ASTWithSource,
  AstVisitor,
  Binary,
  Chain,
  Conditional,
  Pipe,
  FunctionCall,
  ImplicitReceiver,
  Interpolation,
  KeyedAccess,
  LiteralArray,
  LiteralMap,
  LiteralPrimitive,
  MethodCall,
  PrefixNot} from './parser/ast';
import {ChangeDispatcher,
  ChangeDetector,
  ProtoChangeDetector} from './interfaces';
import {ChangeDetectionUtil} from './change_detection_util';
import {DynamicChangeDetector} from './dynamic_change_detector';
import {ChangeDetectorJITGenerator} from './change_detection_jit_generator';
import {PipeRegistry} from './pipes/pipe_registry';
import {BindingRecord} from './binding_record';
import {DirectiveRecord,
  DirectiveIndex} from './directive_record';
import {coalesce} from './coalesce';
import {ProtoRecord,
  RECORD_TYPE_SELF,
  RECORD_TYPE_PROPERTY,
  RECORD_TYPE_LOCAL,
  RECORD_TYPE_INVOKE_METHOD,
  RECORD_TYPE_CONST,
  RECORD_TYPE_INVOKE_CLOSURE,
  RECORD_TYPE_PRIMITIVE_OP,
  RECORD_TYPE_KEYED_ACCESS,
  RECORD_TYPE_PIPE,
  RECORD_TYPE_BINDING_PIPE,
  RECORD_TYPE_INTERPOLATE} from './proto_record';
export class DynamicProtoChangeDetector extends ProtoChangeDetector {
  constructor(pipeRegistry, bindingRecords, variableBindings, directiveRecords, changeControlStrategy) {
    super();
    this._pipeRegistry = pipeRegistry;
    this._bindingRecords = bindingRecords;
    this._variableBindings = variableBindings;
    this._directiveRecords = directiveRecords;
    this._changeControlStrategy = changeControlStrategy;
  }
  instantiate(dispatcher) {
    this._createRecordsIfNecessary();
    return new DynamicChangeDetector(this._changeControlStrategy, dispatcher, this._pipeRegistry, this._records, this._directiveRecords);
  }
  _createRecordsIfNecessary() {
    if (isBlank(this._records)) {
      var recordBuilder = new ProtoRecordBuilder();
      ListWrapper.forEach(this._bindingRecords, (b) => {
        recordBuilder.addAst(b, this._variableBindings);
      });
      this._records = coalesce(recordBuilder.records);
    }
  }
}
Object.defineProperty(DynamicProtoChangeDetector, "parameters", {get: function() {
    return [[PipeRegistry], [List], [List], [List], [assert.type.string]];
  }});
Object.defineProperty(DynamicProtoChangeDetector.prototype.instantiate, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
var _jitProtoChangeDetectorClassCounter = 0;
export class JitProtoChangeDetector extends ProtoChangeDetector {
  constructor(pipeRegistry, bindingRecords, variableBindings, directiveRecords, changeControlStrategy) {
    super();
    this._pipeRegistry = pipeRegistry;
    this._factory = null;
    this._bindingRecords = bindingRecords;
    this._variableBindings = variableBindings;
    this._directiveRecords = directiveRecords;
    this._changeControlStrategy = changeControlStrategy;
  }
  instantiate(dispatcher) {
    this._createFactoryIfNecessary();
    return this._factory(dispatcher, this._pipeRegistry);
  }
  _createFactoryIfNecessary() {
    if (isBlank(this._factory)) {
      var recordBuilder = new ProtoRecordBuilder();
      ListWrapper.forEach(this._bindingRecords, (b) => {
        recordBuilder.addAst(b, this._variableBindings);
      });
      var c = _jitProtoChangeDetectorClassCounter++;
      var records = coalesce(recordBuilder.records);
      var typeName = `ChangeDetector${c}`;
      this._factory = new ChangeDetectorJITGenerator(typeName, this._changeControlStrategy, records, this._directiveRecords).generate();
    }
  }
}
Object.defineProperty(JitProtoChangeDetector, "parameters", {get: function() {
    return [[], [List], [List], [List], [assert.type.string]];
  }});
Object.defineProperty(JitProtoChangeDetector.prototype.instantiate, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
class ProtoRecordBuilder {
  constructor() {
    this.records = [];
  }
  addAst(b, variableBindings = null) {
    var last = ListWrapper.last(this.records);
    if (isPresent(last) && last.bindingRecord.directiveRecord == b.directiveRecord) {
      last.lastInDirective = false;
    }
    var pr = _ConvertAstIntoProtoRecords.convert(b, this.records.length, variableBindings);
    if (!ListWrapper.isEmpty(pr)) {
      var last = ListWrapper.last(pr);
      last.lastInBinding = true;
      last.lastInDirective = true;
      this.records = ListWrapper.concat(this.records, pr);
    }
  }
}
Object.defineProperty(ProtoRecordBuilder.prototype.addAst, "parameters", {get: function() {
    return [[BindingRecord], [List]];
  }});
class _ConvertAstIntoProtoRecords {
  constructor(bindingRecord, contextIndex, expressionAsString, variableBindings) {
    this.protoRecords = [];
    this.bindingRecord = bindingRecord;
    this.contextIndex = contextIndex;
    this.expressionAsString = expressionAsString;
    this.variableBindings = variableBindings;
  }
  static convert(b, contextIndex, variableBindings) {
    var c = new _ConvertAstIntoProtoRecords(b, contextIndex, b.ast.toString(), variableBindings);
    b.ast.visit(c);
    return c.protoRecords;
  }
  visitImplicitReceiver(ast) {
    return this.bindingRecord.implicitReceiver;
  }
  visitInterpolation(ast) {
    var args = this._visitAll(ast.expressions);
    return this._addRecord(RECORD_TYPE_INTERPOLATE, "interpolate", _interpolationFn(ast.strings), args, ast.strings, 0);
  }
  visitLiteralPrimitive(ast) {
    return this._addRecord(RECORD_TYPE_CONST, "literal", ast.value, [], null, 0);
  }
  visitAccessMember(ast) {
    var receiver = ast.receiver.visit(this);
    if (isPresent(this.variableBindings) && ListWrapper.contains(this.variableBindings, ast.name) && ast.receiver instanceof ImplicitReceiver) {
      return this._addRecord(RECORD_TYPE_LOCAL, ast.name, ast.name, [], null, receiver);
    } else {
      return this._addRecord(RECORD_TYPE_PROPERTY, ast.name, ast.getter, [], null, receiver);
    }
  }
  visitMethodCall(ast) {
    ;
    var receiver = ast.receiver.visit(this);
    var args = this._visitAll(ast.args);
    if (isPresent(this.variableBindings) && ListWrapper.contains(this.variableBindings, ast.name)) {
      var target = this._addRecord(RECORD_TYPE_LOCAL, ast.name, ast.name, [], null, receiver);
      return this._addRecord(RECORD_TYPE_INVOKE_CLOSURE, "closure", null, args, null, target);
    } else {
      return this._addRecord(RECORD_TYPE_INVOKE_METHOD, ast.name, ast.fn, args, null, receiver);
    }
  }
  visitFunctionCall(ast) {
    var target = ast.target.visit(this);
    var args = this._visitAll(ast.args);
    return this._addRecord(RECORD_TYPE_INVOKE_CLOSURE, "closure", null, args, null, target);
  }
  visitLiteralArray(ast) {
    var primitiveName = `arrayFn${ast.expressions.length}`;
    return this._addRecord(RECORD_TYPE_PRIMITIVE_OP, primitiveName, _arrayFn(ast.expressions.length), this._visitAll(ast.expressions), null, 0);
  }
  visitLiteralMap(ast) {
    return this._addRecord(RECORD_TYPE_PRIMITIVE_OP, _mapPrimitiveName(ast.keys), ChangeDetectionUtil.mapFn(ast.keys), this._visitAll(ast.values), null, 0);
  }
  visitBinary(ast) {
    var left = ast.left.visit(this);
    var right = ast.right.visit(this);
    return this._addRecord(RECORD_TYPE_PRIMITIVE_OP, _operationToPrimitiveName(ast.operation), _operationToFunction(ast.operation), [left, right], null, 0);
  }
  visitPrefixNot(ast) {
    var exp = ast.expression.visit(this);
    return this._addRecord(RECORD_TYPE_PRIMITIVE_OP, "operation_negate", ChangeDetectionUtil.operation_negate, [exp], null, 0);
  }
  visitConditional(ast) {
    var c = ast.condition.visit(this);
    var t = ast.trueExp.visit(this);
    var f = ast.falseExp.visit(this);
    return this._addRecord(RECORD_TYPE_PRIMITIVE_OP, "cond", ChangeDetectionUtil.cond, [c, t, f], null, 0);
  }
  visitPipe(ast) {
    var value = ast.exp.visit(this);
    var type = ast.inBinding ? RECORD_TYPE_BINDING_PIPE : RECORD_TYPE_PIPE;
    return this._addRecord(type, ast.name, ast.name, [], null, value);
  }
  visitKeyedAccess(ast) {
    var obj = ast.obj.visit(this);
    var key = ast.key.visit(this);
    return this._addRecord(RECORD_TYPE_KEYED_ACCESS, "keyedAccess", ChangeDetectionUtil.keyedAccess, [key], null, obj);
  }
  _visitAll(asts) {
    var res = ListWrapper.createFixedSize(asts.length);
    for (var i = 0; i < asts.length; ++i) {
      res[i] = asts[i].visit(this);
    }
    return res;
  }
  _addRecord(type, name, funcOrValue, args, fixedArgs, context) {
    var selfIndex = ++this.contextIndex;
    if (context instanceof DirectiveIndex) {
      ListWrapper.push(this.protoRecords, new ProtoRecord(type, name, funcOrValue, args, fixedArgs, -1, context, selfIndex, this.bindingRecord, this.expressionAsString, false, false));
    } else {
      ListWrapper.push(this.protoRecords, new ProtoRecord(type, name, funcOrValue, args, fixedArgs, context, null, selfIndex, this.bindingRecord, this.expressionAsString, false, false));
    }
    return selfIndex;
  }
}
Object.defineProperty(_ConvertAstIntoProtoRecords, "parameters", {get: function() {
    return [[BindingRecord], [assert.type.number], [assert.type.string], [List]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.convert, "parameters", {get: function() {
    return [[BindingRecord], [assert.type.number], [List]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitImplicitReceiver, "parameters", {get: function() {
    return [[ImplicitReceiver]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitInterpolation, "parameters", {get: function() {
    return [[Interpolation]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitLiteralPrimitive, "parameters", {get: function() {
    return [[LiteralPrimitive]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitAccessMember, "parameters", {get: function() {
    return [[AccessMember]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitMethodCall, "parameters", {get: function() {
    return [[MethodCall]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitFunctionCall, "parameters", {get: function() {
    return [[FunctionCall]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitLiteralArray, "parameters", {get: function() {
    return [[LiteralArray]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitLiteralMap, "parameters", {get: function() {
    return [[LiteralMap]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitBinary, "parameters", {get: function() {
    return [[Binary]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitPrefixNot, "parameters", {get: function() {
    return [[PrefixNot]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitConditional, "parameters", {get: function() {
    return [[Conditional]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitPipe, "parameters", {get: function() {
    return [[Pipe]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype.visitKeyedAccess, "parameters", {get: function() {
    return [[KeyedAccess]];
  }});
Object.defineProperty(_ConvertAstIntoProtoRecords.prototype._visitAll, "parameters", {get: function() {
    return [[List]];
  }});
function _arrayFn(length) {
  switch (length) {
    case 0:
      return ChangeDetectionUtil.arrayFn0;
    case 1:
      return ChangeDetectionUtil.arrayFn1;
    case 2:
      return ChangeDetectionUtil.arrayFn2;
    case 3:
      return ChangeDetectionUtil.arrayFn3;
    case 4:
      return ChangeDetectionUtil.arrayFn4;
    case 5:
      return ChangeDetectionUtil.arrayFn5;
    case 6:
      return ChangeDetectionUtil.arrayFn6;
    case 7:
      return ChangeDetectionUtil.arrayFn7;
    case 8:
      return ChangeDetectionUtil.arrayFn8;
    case 9:
      return ChangeDetectionUtil.arrayFn9;
    default:
      throw new BaseException(`Does not support literal maps with more than 9 elements`);
  }
}
Object.defineProperty(_arrayFn, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
function _mapPrimitiveName(keys) {
  var stringifiedKeys = ListWrapper.join(ListWrapper.map(keys, (k) => isString(k) ? `"${k}"` : `${k}`), ", ");
  return `mapFn([${stringifiedKeys}])`;
}
Object.defineProperty(_mapPrimitiveName, "parameters", {get: function() {
    return [[List]];
  }});
function _operationToPrimitiveName(operation) {
  switch (operation) {
    case '+':
      return "operation_add";
    case '-':
      return "operation_subtract";
    case '*':
      return "operation_multiply";
    case '/':
      return "operation_divide";
    case '%':
      return "operation_remainder";
    case '==':
      return "operation_equals";
    case '!=':
      return "operation_not_equals";
    case '<':
      return "operation_less_then";
    case '>':
      return "operation_greater_then";
    case '<=':
      return "operation_less_or_equals_then";
    case '>=':
      return "operation_greater_or_equals_then";
    case '&&':
      return "operation_logical_and";
    case '||':
      return "operation_logical_or";
    default:
      throw new BaseException(`Unsupported operation ${operation}`);
  }
}
Object.defineProperty(_operationToPrimitiveName, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
function _operationToFunction(operation) {
  switch (operation) {
    case '+':
      return ChangeDetectionUtil.operation_add;
    case '-':
      return ChangeDetectionUtil.operation_subtract;
    case '*':
      return ChangeDetectionUtil.operation_multiply;
    case '/':
      return ChangeDetectionUtil.operation_divide;
    case '%':
      return ChangeDetectionUtil.operation_remainder;
    case '==':
      return ChangeDetectionUtil.operation_equals;
    case '!=':
      return ChangeDetectionUtil.operation_not_equals;
    case '<':
      return ChangeDetectionUtil.operation_less_then;
    case '>':
      return ChangeDetectionUtil.operation_greater_then;
    case '<=':
      return ChangeDetectionUtil.operation_less_or_equals_then;
    case '>=':
      return ChangeDetectionUtil.operation_greater_or_equals_then;
    case '&&':
      return ChangeDetectionUtil.operation_logical_and;
    case '||':
      return ChangeDetectionUtil.operation_logical_or;
    default:
      throw new BaseException(`Unsupported operation ${operation}`);
  }
}
Object.defineProperty(_operationToFunction, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
function s(v) {
  return isPresent(v) ? `${v}` : '';
}
function _interpolationFn(strings) {
  var length = strings.length;
  var c0 = length > 0 ? strings[0] : null;
  var c1 = length > 1 ? strings[1] : null;
  var c2 = length > 2 ? strings[2] : null;
  var c3 = length > 3 ? strings[3] : null;
  var c4 = length > 4 ? strings[4] : null;
  var c5 = length > 5 ? strings[5] : null;
  var c6 = length > 6 ? strings[6] : null;
  var c7 = length > 7 ? strings[7] : null;
  var c8 = length > 8 ? strings[8] : null;
  var c9 = length > 9 ? strings[9] : null;
  switch (length - 1) {
    case 1:
      return (a1) => c0 + s(a1) + c1;
    case 2:
      return (a1, a2) => c0 + s(a1) + c1 + s(a2) + c2;
    case 3:
      return (a1, a2, a3) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3;
    case 4:
      return (a1, a2, a3, a4) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4;
    case 5:
      return (a1, a2, a3, a4, a5) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4 + s(a5) + c5;
    case 6:
      return (a1, a2, a3, a4, a5, a6) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4 + s(a5) + c5 + s(a6) + c6;
    case 7:
      return (a1, a2, a3, a4, a5, a6, a7) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4 + s(a5) + c5 + s(a6) + c6 + s(a7) + c7;
    case 8:
      return (a1, a2, a3, a4, a5, a6, a7, a8) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4 + s(a5) + c5 + s(a6) + c6 + s(a7) + c7 + s(a8) + c8;
    case 9:
      return (a1, a2, a3, a4, a5, a6, a7, a8, a9) => c0 + s(a1) + c1 + s(a2) + c2 + s(a3) + c3 + s(a4) + c4 + s(a5) + c5 + s(a6) + c6 + s(a7) + c7 + s(a8) + c8 + s(a9) + c9;
    default:
      throw new BaseException(`Does not support more than 9 expressions`);
  }
}
Object.defineProperty(_interpolationFn, "parameters", {get: function() {
    return [[List]];
  }});
//# sourceMappingURL=proto_change_detector.js.map

//# sourceMappingURL=./proto_change_detector.map