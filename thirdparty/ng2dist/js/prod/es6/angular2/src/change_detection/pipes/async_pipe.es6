import {Observable,
  ObservableWrapper} from 'angular2/src/facade/async';
import {isBlank,
  isPresent} from 'angular2/src/facade/lang';
import {Pipe,
  WrappedValue} from './pipe';
import {ChangeDetectorRef} from '../change_detector_ref';
export class AsyncPipe extends Pipe {
  constructor(ref) {
    super();
    this._ref = ref;
    this._latestValue = null;
    this._latestReturnedValue = null;
    this._subscription = null;
    this._observable = null;
  }
  supports(obs) {
    return ObservableWrapper.isObservable(obs);
  }
  onDestroy() {
    if (isPresent(this._subscription)) {
      this._dispose();
    }
    ;
  }
  transform(obs) {
    if (isBlank(this._subscription)) {
      this._subscribe(obs);
      return null;
    }
    if (obs !== this._observable) {
      this._dispose();
      return this.transform(obs);
    }
    if (this._latestValue === this._latestReturnedValue) {
      return this._latestReturnedValue;
    } else {
      this._latestReturnedValue = this._latestValue;
      return WrappedValue.wrap(this._latestValue);
    }
  }
  _subscribe(obs) {
    this._observable = obs;
    this._subscription = ObservableWrapper.subscribe(obs, (value) => this._updateLatestValue(value), (e) => {
      throw e;
    });
  }
  _dispose() {
    ObservableWrapper.dispose(this._subscription);
    this._latestValue = null;
    this._latestReturnedValue = null;
    this._subscription = null;
    this._observable = null;
  }
  _updateLatestValue(value) {
    this._latestValue = value;
    this._ref.requestCheck();
  }
}
Object.defineProperty(AsyncPipe, "parameters", {get: function() {
    return [[ChangeDetectorRef]];
  }});
Object.defineProperty(AsyncPipe.prototype.transform, "parameters", {get: function() {
    return [[Observable]];
  }});
Object.defineProperty(AsyncPipe.prototype._subscribe, "parameters", {get: function() {
    return [[Observable]];
  }});
Object.defineProperty(AsyncPipe.prototype._updateLatestValue, "parameters", {get: function() {
    return [[Object]];
  }});
export class AsyncPipeFactory {
  supports(obs) {
    return ObservableWrapper.isObservable(obs);
  }
  create(cdRef) {
    return new AsyncPipe(cdRef);
  }
}
//# sourceMappingURL=async_pipe.js.map

//# sourceMappingURL=./async_pipe.map