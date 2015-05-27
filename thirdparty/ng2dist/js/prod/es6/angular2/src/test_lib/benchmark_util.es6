import {BrowserDomAdapter} from 'angular2/src/dom/browser_adapter';
import {document,
  window} from 'angular2/src/facade/browser';
import {NumberWrapper,
  BaseException,
  isBlank} from 'angular2/src/facade/lang';
var DOM = new BrowserDomAdapter();
export function getIntParameter(name) {
  return NumberWrapper.parseInt(getStringParameter(name), 10);
}
Object.defineProperty(getIntParameter, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export function getStringParameter(name) {
  var els = DOM.querySelectorAll(document, `input[name="${name}"]`);
  var value;
  var el;
  for (var i = 0; i < els.length; i++) {
    el = els[i];
    var type = DOM.type(el);
    if ((type != 'radio' && type != 'checkbox') || DOM.getChecked(el)) {
      value = DOM.getValue(el);
      break;
    }
  }
  if (isBlank(value)) {
    throw new BaseException(`Could not find and input field with name ${name}`);
  }
  return value;
}
Object.defineProperty(getStringParameter, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export function bindAction(selector, callback) {
  var el = DOM.querySelector(document, selector);
  DOM.on(el, 'click', function(_) {
    callback();
  });
}
Object.defineProperty(bindAction, "parameters", {get: function() {
    return [[assert.type.string], [Function]];
  }});
export function microBenchmark(name, iterationCount, callback) {
  var durationName = `${name}/${iterationCount}`;
  window.console.time(durationName);
  callback();
  window.console.timeEnd(durationName);
}
//# sourceMappingURL=benchmark_util.js.map

//# sourceMappingURL=./benchmark_util.map