import {EventManagerPlugin} from './event_manager';
import {StringMapWrapper} from 'angular2/src/facade/collection';
var _eventNames = {
  'pan': true,
  'panstart': true,
  'panmove': true,
  'panend': true,
  'pancancel': true,
  'panleft': true,
  'panright': true,
  'panup': true,
  'pandown': true,
  'pinch': true,
  'pinchstart': true,
  'pinchmove': true,
  'pinchend': true,
  'pinchcancel': true,
  'pinchin': true,
  'pinchout': true,
  'press': true,
  'pressup': true,
  'rotate': true,
  'rotatestart': true,
  'rotatemove': true,
  'rotateend': true,
  'rotatecancel': true,
  'swipe': true,
  'swipeleft': true,
  'swiperight': true,
  'swipeup': true,
  'swipedown': true,
  'tap': true
};
export class HammerGesturesPluginCommon extends EventManagerPlugin {
  constructor() {
    super();
  }
  supports(eventName) {
    eventName = eventName.toLowerCase();
    return StringMapWrapper.contains(_eventNames, eventName);
  }
}
Object.defineProperty(HammerGesturesPluginCommon.prototype.supports, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=hammer_common.js.map

//# sourceMappingURL=./hammer_common.map