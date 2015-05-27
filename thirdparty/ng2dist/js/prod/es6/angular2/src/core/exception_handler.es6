import {Injectable} from 'angular2/di';
import {isPresent,
  print} from 'angular2/src/facade/lang';
import {ListWrapper,
  isListLikeIterable} from 'angular2/src/facade/collection';
import {DOM} from 'angular2/src/dom/dom_adapter';
export class ExceptionHandler {
  call(error, stackTrace = null, reason = null) {
    var longStackTrace = isListLikeIterable(stackTrace) ? ListWrapper.join(stackTrace, "\n\n") : stackTrace;
    var reasonStr = isPresent(reason) ? `\n${reason}` : '';
    DOM.logError(`${error}${reasonStr}\nSTACKTRACE:\n${longStackTrace}`);
  }
}
Object.defineProperty(ExceptionHandler, "annotations", {get: function() {
    return [new Injectable()];
  }});
//# sourceMappingURL=exception_handler.js.map

//# sourceMappingURL=./exception_handler.map