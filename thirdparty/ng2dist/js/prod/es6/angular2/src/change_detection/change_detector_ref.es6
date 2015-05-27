import {ChangeDetector} from './interfaces';
import {CHECK_ONCE,
  DETACHED,
  CHECK_ALWAYS} from './constants';
export class ChangeDetectorRef {
  constructor(cd) {
    this._cd = cd;
  }
  requestCheck() {
    this._cd.markPathToRootAsCheckOnce();
  }
  detach() {
    this._cd.mode = DETACHED;
  }
  reattach() {
    this._cd.mode = CHECK_ALWAYS;
    this.requestCheck();
  }
}
Object.defineProperty(ChangeDetectorRef, "parameters", {get: function() {
    return [[ChangeDetector]];
  }});
//# sourceMappingURL=change_detector_ref.js.map

//# sourceMappingURL=./change_detector_ref.map