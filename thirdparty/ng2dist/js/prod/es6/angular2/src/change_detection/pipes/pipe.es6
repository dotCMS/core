export class WrappedValue {
  constructor(wrapped) {
    this.wrapped = wrapped;
  }
  static wrap(value) {
    var w = _wrappedValues[_wrappedIndex++ % 5];
    w.wrapped = value;
    return w;
  }
}
Object.defineProperty(WrappedValue, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
Object.defineProperty(WrappedValue.wrap, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
var _wrappedValues = [new WrappedValue(null), new WrappedValue(null), new WrappedValue(null), new WrappedValue(null), new WrappedValue(null)];
var _wrappedIndex = 0;
export class Pipe {
  supports(obj) {
    return false;
  }
  onDestroy() {}
  transform(value) {
    return null;
  }
}
Object.defineProperty(Pipe.prototype.transform, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
//# sourceMappingURL=pipe.js.map

//# sourceMappingURL=./pipe.map