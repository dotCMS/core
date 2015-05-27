import {AsyncTestCompleter,
  beforeEach,
  ddescribe,
  xdescribe,
  describe,
  el,
  dispatchEvent,
  expect,
  iit,
  inject,
  beforeEachBindings,
  it,
  xit,
  SpyObject,
  proxy} from 'angular2/test_lib';
import {MapWrapper} from 'angular2/src/facade/collection';
import {IMPLEMENTS,
  isBlank,
  isPresent} from 'angular2/src/facade/lang';
import {AppView,
  AppProtoView,
  AppViewContainer} from 'angular2/src/core/compiler/view';
import {ProtoViewRef,
  ViewRef,
  internalView} from 'angular2/src/core/compiler/view_ref';
import {ElementRef} from 'angular2/src/core/compiler/element_ref';
import {ViewContainerRef} from 'angular2/src/core/compiler/view_container_ref';
import {AppViewManager} from 'angular2/src/core/compiler/view_manager';
export function main() {
  describe('ViewContainerRef', () => {
    var location;
    var view;
    var viewManager;
    function wrapView(view) {
      return new ViewRef(view);
    }
    Object.defineProperty(wrapView, "parameters", {get: function() {
        return [[AppView]];
      }});
    function createProtoView() {
      return new AppProtoView(null, null, null, null, null);
    }
    function createView() {
      return new AppView(null, createProtoView(), MapWrapper.create());
    }
    function createViewContainer() {
      return new ViewContainerRef(viewManager, location);
    }
    beforeEach(() => {
      viewManager = new AppViewManagerSpy();
      view = createView();
      view.viewContainers = [null];
      location = new ElementRef(wrapView(view), 0);
    });
    it('should return a 0 length if there is no underlying ViewContainerRef', () => {
      var vc = createViewContainer();
      expect(vc.length).toBe(0);
    });
    it('should return the size of the underlying ViewContainerRef', () => {
      var vc = createViewContainer();
      view.viewContainers = [new AppViewContainer()];
      view.viewContainers[0].views = [createView()];
      expect(vc.length).toBe(1);
    });
  });
}
class AppViewManagerSpy extends SpyObject {
  constructor() {
    super(AppViewManager);
  }
  noSuchMethod(m) {
    return super.noSuchMethod(m);
  }
}
Object.defineProperty(AppViewManagerSpy, "annotations", {get: function() {
    return [new proxy, new IMPLEMENTS(AppViewManager)];
  }});
//# sourceMappingURL=view_container_ref_spec.js.map

//# sourceMappingURL=./view_container_ref_spec.map