/// <reference path="../../typings/es6-promise/es6-promise.d.ts" />
/// <reference path="../../typings/rx/rx.all.d.ts" />
// HACK: workaround for Traceur behavior.
// It expects all transpiled modules to contain this marker.
// TODO: remove this when we no longer use traceur
export var __esModule = true;
import { global } from 'angular2/src/facade/lang';
import * as Rx from 'rx';
export class PromiseWrapper {
    static resolve(obj) {
        return Promise.resolve(obj);
    }
    static reject(obj) {
        return Promise.reject(obj);
    }
    // Note: We can't rename this method into `catch`, as this is not a valid
    // method name in Dart.
    static catchError(promise, onError) {
        return promise.catch(onError);
    }
    static all(promises) {
        if (promises.length == 0)
            return Promise.resolve([]);
        return Promise.all(promises);
    }
    static then(promise, success, rejection) {
        return promise.then(success, rejection);
    }
    static completer() {
        var resolve;
        var reject;
        var p = new Promise(function (res, rej) {
            resolve = res;
            reject = rej;
        });
        return {
            promise: p,
            resolve: resolve,
            reject: reject
        };
    }
    static setTimeout(fn, millis) {
        global.setTimeout(fn, millis);
    }
    static isPromise(maybePromise) {
        return maybePromise instanceof Promise;
    }
}
export class ObservableWrapper {
    static subscribe(emitter, onNext, onThrow = null, onReturn = null) {
        return emitter.observer({
            next: onNext,
            throw: onThrow,
            return: onReturn
        });
    }
    static isObservable(obs) {
        return obs instanceof Observable;
    }
    static dispose(subscription) {
        subscription.dispose();
    }
    static callNext(emitter, value) {
        emitter.next(value);
    }
    static callThrow(emitter, error) {
        emitter.throw(error);
    }
    static callReturn(emitter) {
        emitter.return(null);
    }
}
// TODO: vsavkin change to interface
export class Observable {
    observer(generator) {
    }
}
/**
 * Use Rx.Observable but provides an adapter to make it work as specified here:
 * https://github.com/jhusain/observable-spec
 *
 * Once a reference implementation of the spec is available, switch to it.
 */
export class EventEmitter extends Observable {
    constructor() {
        super();
        // System creates a different object for import * than Typescript es5 emit.
        if (Rx.hasOwnProperty('default')) {
            this._subject = new Rx.default.Rx.Subject();
            this._immediateScheduler = Rx.default.Rx.Scheduler.immediate;
        }
        else {
            this._subject = new Rx.Subject();
            this._immediateScheduler = Rx.Scheduler.immediate;
        }
    }
    observer(generator) {
        return this._subject.observeOn(this._immediateScheduler).subscribe((value) => {
            setTimeout(() => generator.next(value));
        }, (error) => generator.throw ? generator.throw(error) : null, () => generator.return ? generator.return() : null);
    }
    toRx() {
        return this._subject;
    }
    next(value) {
        this._subject.onNext(value);
    }
    throw(error) {
        this._subject.onError(error);
    }
    return(value) {
        this._subject.onCompleted();
    }
}
//# sourceMappingURL=async.js.map