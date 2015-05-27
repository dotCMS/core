import {ProtoRecord} from './proto_record';
export class ExpressionChangedAfterItHasBeenChecked extends Error {
  constructor(proto, change) {
    super();
    this.message = `Expression '${proto.expressionAsString}' has changed after it was checked. ` + `Previous value: '${change.previousValue}'. Current value: '${change.currentValue}'`;
  }
  toString() {
    return this.message;
  }
}
Object.defineProperty(ExpressionChangedAfterItHasBeenChecked, "parameters", {get: function() {
    return [[ProtoRecord], [assert.type.any]];
  }});
export class ChangeDetectionError extends Error {
  constructor(proto, originalException) {
    super();
    this.originalException = originalException;
    this.location = proto.expressionAsString;
    this.message = `${this.originalException} in [${this.location}]`;
  }
  toString() {
    return this.message;
  }
}
Object.defineProperty(ChangeDetectionError, "parameters", {get: function() {
    return [[ProtoRecord], [assert.type.any]];
  }});
//# sourceMappingURL=exceptions.js.map

//# sourceMappingURL=./exceptions.map