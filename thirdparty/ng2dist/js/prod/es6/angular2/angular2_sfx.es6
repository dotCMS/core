import * as angular from './angular2';
var _prevAngular = window.angular;
angular.noConflict = function() {
  window.angular = _prevAngular;
  return angular;
};
window.angular = angular;
//# sourceMappingURL=angular2_sfx.es6.map

//# sourceMappingURL=./angular2_sfx.map