import {ListWrapper,
  List} from 'angular2/src/facade/collection';
import {stringify} from 'angular2/src/facade/lang';
function findFirstClosedCycle(keys) {
  var res = [];
  for (var i = 0; i < keys.length; ++i) {
    if (ListWrapper.contains(res, keys[i])) {
      ListWrapper.push(res, keys[i]);
      return res;
    } else {
      ListWrapper.push(res, keys[i]);
    }
  }
  return res;
}
Object.defineProperty(findFirstClosedCycle, "parameters", {get: function() {
    return [[List]];
  }});
function constructResolvingPath(keys) {
  if (keys.length > 1) {
    var reversed = findFirstClosedCycle(ListWrapper.reversed(keys));
    var tokenStrs = ListWrapper.map(reversed, (k) => stringify(k.token));
    return " (" + tokenStrs.join(' -> ') + ")";
  } else {
    return "";
  }
}
Object.defineProperty(constructResolvingPath, "parameters", {get: function() {
    return [[List]];
  }});
export class AbstractBindingError extends Error {
  constructor(key, constructResolvingMessage) {
    super();
    this.keys = [key];
    this.constructResolvingMessage = constructResolvingMessage;
    this.message = this.constructResolvingMessage(this.keys);
  }
  addKey(key) {
    ListWrapper.push(this.keys, key);
    this.message = this.constructResolvingMessage(this.keys);
  }
  toString() {
    return this.message;
  }
}
Object.defineProperty(AbstractBindingError, "parameters", {get: function() {
    return [[], [Function]];
  }});
export class NoBindingError extends AbstractBindingError {
  constructor(key) {
    super(key, function(keys) {
      var first = stringify(ListWrapper.first(keys).token);
      return `No provider for ${first}!${constructResolvingPath(keys)}`;
    });
  }
}
export class AsyncBindingError extends AbstractBindingError {
  constructor(key) {
    super(key, function(keys) {
      var first = stringify(ListWrapper.first(keys).token);
      return `Cannot instantiate ${first} synchronously. ` + `It is provided as a promise!${constructResolvingPath(keys)}`;
    });
  }
}
export class CyclicDependencyError extends AbstractBindingError {
  constructor(key) {
    super(key, function(keys) {
      return `Cannot instantiate cyclic dependency!${constructResolvingPath(keys)}`;
    });
  }
}
export class InstantiationError extends AbstractBindingError {
  constructor(cause, key) {
    super(key, function(keys) {
      var first = stringify(ListWrapper.first(keys).token);
      return `Error during instantiation of ${first}!${constructResolvingPath(keys)}.` + ` ORIGINAL ERROR: ${cause}`;
    });
    this.cause = cause;
    this.causeKey = key;
  }
}
export class InvalidBindingError extends Error {
  constructor(binding) {
    super();
    this.message = `Invalid binding - only instances of Binding and Type are allowed, got: ${binding}`;
  }
  toString() {
    return this.message;
  }
}
export class NoAnnotationError extends Error {
  constructor(typeOrFunc) {
    super();
    this.message = `Cannot resolve all parameters for ${stringify(typeOrFunc)}.` + ` Make sure they all have valid type or annotations.`;
  }
  toString() {
    return this.message;
  }
}
//# sourceMappingURL=exceptions.js.map

//# sourceMappingURL=./exceptions.map