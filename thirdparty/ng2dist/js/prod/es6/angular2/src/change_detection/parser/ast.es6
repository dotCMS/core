import {autoConvertAdd,
  isBlank,
  isPresent,
  FunctionWrapper,
  BaseException} from "angular2/src/facade/lang";
import {List,
  Map,
  ListWrapper,
  StringMapWrapper} from "angular2/src/facade/collection";
export class AST {
  eval(context, locals) {
    throw new BaseException("Not supported");
  }
  get isAssignable() {
    return false;
  }
  assign(context, locals, value) {
    throw new BaseException("Not supported");
  }
  visit(visitor) {}
  toString() {
    return "AST";
  }
}
export class EmptyExpr extends AST {
  eval(context, locals) {
    return null;
  }
  visit(visitor) {}
}
export class ImplicitReceiver extends AST {
  eval(context, locals) {
    return context;
  }
  visit(visitor) {
    return visitor.visitImplicitReceiver(this);
  }
}
export class Chain extends AST {
  constructor(expressions) {
    super();
    this.expressions = expressions;
  }
  eval(context, locals) {
    var result;
    for (var i = 0; i < this.expressions.length; i++) {
      var last = this.expressions[i].eval(context, locals);
      if (isPresent(last))
        result = last;
    }
    return result;
  }
  visit(visitor) {
    return visitor.visitChain(this);
  }
}
Object.defineProperty(Chain, "parameters", {get: function() {
    return [[List]];
  }});
export class Conditional extends AST {
  constructor(condition, trueExp, falseExp) {
    super();
    this.condition = condition;
    this.trueExp = trueExp;
    this.falseExp = falseExp;
  }
  eval(context, locals) {
    if (this.condition.eval(context, locals)) {
      return this.trueExp.eval(context, locals);
    } else {
      return this.falseExp.eval(context, locals);
    }
  }
  visit(visitor) {
    return visitor.visitConditional(this);
  }
}
Object.defineProperty(Conditional, "parameters", {get: function() {
    return [[AST], [AST], [AST]];
  }});
export class AccessMember extends AST {
  constructor(receiver, name, getter, setter) {
    super();
    this.receiver = receiver;
    this.name = name;
    this.getter = getter;
    this.setter = setter;
  }
  eval(context, locals) {
    if (this.receiver instanceof ImplicitReceiver && isPresent(locals) && locals.contains(this.name)) {
      return locals.get(this.name);
    } else {
      var evaluatedReceiver = this.receiver.eval(context, locals);
      return this.getter(evaluatedReceiver);
    }
  }
  get isAssignable() {
    return true;
  }
  assign(context, locals, value) {
    var evaluatedContext = this.receiver.eval(context, locals);
    if (this.receiver instanceof ImplicitReceiver && isPresent(locals) && locals.contains(this.name)) {
      throw new BaseException(`Cannot reassign a variable binding ${this.name}`);
    } else {
      return this.setter(evaluatedContext, value);
    }
  }
  visit(visitor) {
    return visitor.visitAccessMember(this);
  }
}
Object.defineProperty(AccessMember, "parameters", {get: function() {
    return [[AST], [assert.type.string], [Function], [Function]];
  }});
export class KeyedAccess extends AST {
  constructor(obj, key) {
    super();
    this.obj = obj;
    this.key = key;
  }
  eval(context, locals) {
    var obj = this.obj.eval(context, locals);
    var key = this.key.eval(context, locals);
    return obj[key];
  }
  get isAssignable() {
    return true;
  }
  assign(context, locals, value) {
    var obj = this.obj.eval(context, locals);
    var key = this.key.eval(context, locals);
    obj[key] = value;
    return value;
  }
  visit(visitor) {
    return visitor.visitKeyedAccess(this);
  }
}
Object.defineProperty(KeyedAccess, "parameters", {get: function() {
    return [[AST], [AST]];
  }});
export class Pipe extends AST {
  constructor(exp, name, args, inBinding) {
    super();
    this.exp = exp;
    this.name = name;
    this.args = args;
    this.inBinding = inBinding;
  }
  visit(visitor) {
    return visitor.visitPipe(this);
  }
}
Object.defineProperty(Pipe, "parameters", {get: function() {
    return [[AST], [assert.type.string], [List], [assert.type.boolean]];
  }});
export class LiteralPrimitive extends AST {
  constructor(value) {
    super();
    this.value = value;
  }
  eval(context, locals) {
    return this.value;
  }
  visit(visitor) {
    return visitor.visitLiteralPrimitive(this);
  }
}
export class LiteralArray extends AST {
  constructor(expressions) {
    super();
    this.expressions = expressions;
  }
  eval(context, locals) {
    return ListWrapper.map(this.expressions, (e) => e.eval(context, locals));
  }
  visit(visitor) {
    return visitor.visitLiteralArray(this);
  }
}
Object.defineProperty(LiteralArray, "parameters", {get: function() {
    return [[List]];
  }});
export class LiteralMap extends AST {
  constructor(keys, values) {
    super();
    this.keys = keys;
    this.values = values;
  }
  eval(context, locals) {
    var res = StringMapWrapper.create();
    for (var i = 0; i < this.keys.length; ++i) {
      StringMapWrapper.set(res, this.keys[i], this.values[i].eval(context, locals));
    }
    return res;
  }
  visit(visitor) {
    return visitor.visitLiteralMap(this);
  }
}
Object.defineProperty(LiteralMap, "parameters", {get: function() {
    return [[List], [List]];
  }});
export class Interpolation extends AST {
  constructor(strings, expressions) {
    super();
    this.strings = strings;
    this.expressions = expressions;
  }
  eval(context, locals) {
    throw new BaseException("evaluating an Interpolation is not supported");
  }
  visit(visitor) {
    visitor.visitInterpolation(this);
  }
}
Object.defineProperty(Interpolation, "parameters", {get: function() {
    return [[List], [List]];
  }});
export class Binary extends AST {
  constructor(operation, left, right) {
    super();
    this.operation = operation;
    this.left = left;
    this.right = right;
  }
  eval(context, locals) {
    var left = this.left.eval(context, locals);
    switch (this.operation) {
      case '&&':
        return left && this.right.eval(context, locals);
      case '||':
        return left || this.right.eval(context, locals);
    }
    var right = this.right.eval(context, locals);
    switch (this.operation) {
      case '+':
        return left + right;
      case '-':
        return left - right;
      case '*':
        return left * right;
      case '/':
        return left / right;
      case '%':
        return left % right;
      case '==':
        return left == right;
      case '!=':
        return left != right;
      case '===':
        return left === right;
      case '!==':
        return left !== right;
      case '<':
        return left < right;
      case '>':
        return left > right;
      case '<=':
        return left <= right;
      case '>=':
        return left >= right;
      case '^':
        return left ^ right;
      case '&':
        return left & right;
    }
    throw 'Internal error [$operation] not handled';
  }
  visit(visitor) {
    return visitor.visitBinary(this);
  }
}
Object.defineProperty(Binary, "parameters", {get: function() {
    return [[assert.type.string], [AST], [AST]];
  }});
export class PrefixNot extends AST {
  constructor(expression) {
    super();
    this.expression = expression;
  }
  eval(context, locals) {
    return !this.expression.eval(context, locals);
  }
  visit(visitor) {
    return visitor.visitPrefixNot(this);
  }
}
Object.defineProperty(PrefixNot, "parameters", {get: function() {
    return [[AST]];
  }});
export class Assignment extends AST {
  constructor(target, value) {
    super();
    this.target = target;
    this.value = value;
  }
  eval(context, locals) {
    return this.target.assign(context, locals, this.value.eval(context, locals));
  }
  visit(visitor) {
    return visitor.visitAssignment(this);
  }
}
Object.defineProperty(Assignment, "parameters", {get: function() {
    return [[AST], [AST]];
  }});
export class MethodCall extends AST {
  constructor(receiver, name, fn, args) {
    super();
    this.receiver = receiver;
    this.fn = fn;
    this.args = args;
    this.name = name;
  }
  eval(context, locals) {
    var evaluatedArgs = evalList(context, locals, this.args);
    if (this.receiver instanceof ImplicitReceiver && isPresent(locals) && locals.contains(this.name)) {
      var fn = locals.get(this.name);
      return FunctionWrapper.apply(fn, evaluatedArgs);
    } else {
      var evaluatedReceiver = this.receiver.eval(context, locals);
      return this.fn(evaluatedReceiver, evaluatedArgs);
    }
  }
  visit(visitor) {
    return visitor.visitMethodCall(this);
  }
}
Object.defineProperty(MethodCall, "parameters", {get: function() {
    return [[AST], [assert.type.string], [Function], [List]];
  }});
export class FunctionCall extends AST {
  constructor(target, args) {
    super();
    this.target = target;
    this.args = args;
  }
  eval(context, locals) {
    var obj = this.target.eval(context, locals);
    if (!(obj instanceof Function)) {
      throw new BaseException(`${obj} is not a function`);
    }
    return FunctionWrapper.apply(obj, evalList(context, locals, this.args));
  }
  visit(visitor) {
    return visitor.visitFunctionCall(this);
  }
}
Object.defineProperty(FunctionCall, "parameters", {get: function() {
    return [[AST], [List]];
  }});
export class ASTWithSource extends AST {
  constructor(ast, source, location) {
    super();
    this.source = source;
    this.location = location;
    this.ast = ast;
  }
  eval(context, locals) {
    return this.ast.eval(context, locals);
  }
  get isAssignable() {
    return this.ast.isAssignable;
  }
  assign(context, locals, value) {
    return this.ast.assign(context, locals, value);
  }
  visit(visitor) {
    return this.ast.visit(visitor);
  }
  toString() {
    return `${this.source} in ${this.location}`;
  }
}
Object.defineProperty(ASTWithSource, "parameters", {get: function() {
    return [[AST], [assert.type.string], [assert.type.string]];
  }});
export class TemplateBinding {
  constructor(key, keyIsVar, name, expression) {
    this.key = key;
    this.keyIsVar = keyIsVar;
    this.name = name;
    this.expression = expression;
  }
}
Object.defineProperty(TemplateBinding, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.boolean], [assert.type.string], [ASTWithSource]];
  }});
export class AstVisitor {
  visitAccessMember(ast) {}
  visitAssignment(ast) {}
  visitBinary(ast) {}
  visitChain(ast) {}
  visitConditional(ast) {}
  visitPipe(ast) {}
  visitFunctionCall(ast) {}
  visitImplicitReceiver(ast) {}
  visitKeyedAccess(ast) {}
  visitLiteralArray(ast) {}
  visitLiteralMap(ast) {}
  visitLiteralPrimitive(ast) {}
  visitMethodCall(ast) {}
  visitPrefixNot(ast) {}
}
Object.defineProperty(AstVisitor.prototype.visitAccessMember, "parameters", {get: function() {
    return [[AccessMember]];
  }});
Object.defineProperty(AstVisitor.prototype.visitAssignment, "parameters", {get: function() {
    return [[Assignment]];
  }});
Object.defineProperty(AstVisitor.prototype.visitBinary, "parameters", {get: function() {
    return [[Binary]];
  }});
Object.defineProperty(AstVisitor.prototype.visitChain, "parameters", {get: function() {
    return [[Chain]];
  }});
Object.defineProperty(AstVisitor.prototype.visitConditional, "parameters", {get: function() {
    return [[Conditional]];
  }});
Object.defineProperty(AstVisitor.prototype.visitPipe, "parameters", {get: function() {
    return [[Pipe]];
  }});
Object.defineProperty(AstVisitor.prototype.visitFunctionCall, "parameters", {get: function() {
    return [[FunctionCall]];
  }});
Object.defineProperty(AstVisitor.prototype.visitImplicitReceiver, "parameters", {get: function() {
    return [[ImplicitReceiver]];
  }});
Object.defineProperty(AstVisitor.prototype.visitKeyedAccess, "parameters", {get: function() {
    return [[KeyedAccess]];
  }});
Object.defineProperty(AstVisitor.prototype.visitLiteralArray, "parameters", {get: function() {
    return [[LiteralArray]];
  }});
Object.defineProperty(AstVisitor.prototype.visitLiteralMap, "parameters", {get: function() {
    return [[LiteralMap]];
  }});
Object.defineProperty(AstVisitor.prototype.visitLiteralPrimitive, "parameters", {get: function() {
    return [[LiteralPrimitive]];
  }});
Object.defineProperty(AstVisitor.prototype.visitMethodCall, "parameters", {get: function() {
    return [[MethodCall]];
  }});
Object.defineProperty(AstVisitor.prototype.visitPrefixNot, "parameters", {get: function() {
    return [[PrefixNot]];
  }});
export class AstTransformer {
  visitImplicitReceiver(ast) {
    return ast;
  }
  visitInterpolation(ast) {
    return new Interpolation(ast.strings, this.visitAll(ast.expressions));
  }
  visitLiteralPrimitive(ast) {
    return new LiteralPrimitive(ast.value);
  }
  visitAccessMember(ast) {
    return new AccessMember(ast.receiver.visit(this), ast.name, ast.getter, ast.setter);
  }
  visitMethodCall(ast) {
    return new MethodCall(ast.receiver.visit(this), ast.name, ast.fn, this.visitAll(ast.args));
  }
  visitFunctionCall(ast) {
    return new FunctionCall(ast.target.visit(this), this.visitAll(ast.args));
  }
  visitLiteralArray(ast) {
    return new LiteralArray(this.visitAll(ast.expressions));
  }
  visitLiteralMap(ast) {
    return new LiteralMap(ast.keys, this.visitAll(ast.values));
  }
  visitBinary(ast) {
    return new Binary(ast.operation, ast.left.visit(this), ast.right.visit(this));
  }
  visitPrefixNot(ast) {
    return new PrefixNot(ast.expression.visit(this));
  }
  visitConditional(ast) {
    return new Conditional(ast.condition.visit(this), ast.trueExp.visit(this), ast.falseExp.visit(this));
  }
  visitPipe(ast) {
    return new Pipe(ast.exp.visit(this), ast.name, this.visitAll(ast.args), ast.inBinding);
  }
  visitKeyedAccess(ast) {
    return new KeyedAccess(ast.obj.visit(this), ast.key.visit(this));
  }
  visitAll(asts) {
    var res = ListWrapper.createFixedSize(asts.length);
    for (var i = 0; i < asts.length; ++i) {
      res[i] = asts[i].visit(this);
    }
    return res;
  }
}
Object.defineProperty(AstTransformer.prototype.visitImplicitReceiver, "parameters", {get: function() {
    return [[ImplicitReceiver]];
  }});
Object.defineProperty(AstTransformer.prototype.visitInterpolation, "parameters", {get: function() {
    return [[Interpolation]];
  }});
Object.defineProperty(AstTransformer.prototype.visitLiteralPrimitive, "parameters", {get: function() {
    return [[LiteralPrimitive]];
  }});
Object.defineProperty(AstTransformer.prototype.visitAccessMember, "parameters", {get: function() {
    return [[AccessMember]];
  }});
Object.defineProperty(AstTransformer.prototype.visitMethodCall, "parameters", {get: function() {
    return [[MethodCall]];
  }});
Object.defineProperty(AstTransformer.prototype.visitFunctionCall, "parameters", {get: function() {
    return [[FunctionCall]];
  }});
Object.defineProperty(AstTransformer.prototype.visitLiteralArray, "parameters", {get: function() {
    return [[LiteralArray]];
  }});
Object.defineProperty(AstTransformer.prototype.visitLiteralMap, "parameters", {get: function() {
    return [[LiteralMap]];
  }});
Object.defineProperty(AstTransformer.prototype.visitBinary, "parameters", {get: function() {
    return [[Binary]];
  }});
Object.defineProperty(AstTransformer.prototype.visitPrefixNot, "parameters", {get: function() {
    return [[PrefixNot]];
  }});
Object.defineProperty(AstTransformer.prototype.visitConditional, "parameters", {get: function() {
    return [[Conditional]];
  }});
Object.defineProperty(AstTransformer.prototype.visitPipe, "parameters", {get: function() {
    return [[Pipe]];
  }});
Object.defineProperty(AstTransformer.prototype.visitKeyedAccess, "parameters", {get: function() {
    return [[KeyedAccess]];
  }});
Object.defineProperty(AstTransformer.prototype.visitAll, "parameters", {get: function() {
    return [[List]];
  }});
var _evalListCache = [[], [0], [0, 0], [0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0]];
function evalList(context, locals, exps) {
  var length = exps.length;
  if (length > 10) {
    throw new BaseException("Cannot have more than 10 argument");
  }
  var result = _evalListCache[length];
  for (var i = 0; i < length; i++) {
    result[i] = exps[i].eval(context, locals);
  }
  return result;
}
Object.defineProperty(evalList, "parameters", {get: function() {
    return [[], [], [List]];
  }});
//# sourceMappingURL=ast.js.map

//# sourceMappingURL=./ast.map