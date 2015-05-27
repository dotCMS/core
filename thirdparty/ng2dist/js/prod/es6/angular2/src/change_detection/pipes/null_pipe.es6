import {isBlank} from 'angular2/src/facade/lang';
import {Pipe,
  WrappedValue} from './pipe';
export class NullPipeFactory {
  supports(obj) {
    return NullPipe.supportsObj(obj);
  }
  create(cdRef) {
    return new NullPipe();
  }
}
export class NullPipe extends Pipe {
  constructor() {
    super();
    this.called = false;
  }
  static supportsObj(obj) {
    return isBlank(obj);
  }
  supports(obj) {
    return NullPipe.supportsObj(obj);
  }
  transform(value) {
    if (!this.called) {
      this.called = true;
      return WrappedValue.wrap(null);
    } else {
      return null;
    }
  }
}
//# sourceMappingURL=null_pipe.js.map

//# sourceMappingURL=./null_pipe.map