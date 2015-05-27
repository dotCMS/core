import {List,
  ListWrapper} from 'angular2/src/facade/collection';
export class MdRadioDispatcher {
  constructor() {
    this.listeners_ = [];
  }
  notify(name) {
    ListWrapper.forEach(this.listeners_, (f) => f(name));
  }
  listen(listener) {
    ListWrapper.push(this.listeners_, listener);
  }
}
Object.defineProperty(MdRadioDispatcher.prototype.notify, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=radio_dispatcher.js.map

//# sourceMappingURL=./radio_dispatcher.map