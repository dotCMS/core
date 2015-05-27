import {Injectable} from 'angular2/di';
import {ChangeDetector} from 'angular2/change_detection';
import {VmTurnZone} from 'angular2/src/core/zone/vm_turn_zone';
import {ExceptionHandler} from 'angular2/src/core/exception_handler';
import {isPresent} from 'angular2/src/facade/lang';
export class LifeCycle {
  constructor(exceptionHandler, changeDetector = null, enforceNoNewChanges = false) {
    this._errorHandler = (exception, stackTrace) => {
      exceptionHandler.call(exception, stackTrace);
      throw exception;
    };
    this._changeDetector = changeDetector;
    this._enforceNoNewChanges = enforceNoNewChanges;
  }
  registerWith(zone, changeDetector = null) {
    if (isPresent(changeDetector)) {
      this._changeDetector = changeDetector;
    }
    zone.initCallbacks({
      onErrorHandler: this._errorHandler,
      onTurnDone: () => this.tick()
    });
  }
  tick() {
    this._changeDetector.detectChanges();
    if (this._enforceNoNewChanges) {
      this._changeDetector.checkNoChanges();
    }
  }
}
Object.defineProperty(LifeCycle, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(LifeCycle, "parameters", {get: function() {
    return [[ExceptionHandler], [ChangeDetector], [assert.type.boolean]];
  }});
Object.defineProperty(LifeCycle.prototype.registerWith, "parameters", {get: function() {
    return [[VmTurnZone], [ChangeDetector]];
  }});
//# sourceMappingURL=life_cycle.js.map

//# sourceMappingURL=./life_cycle.map