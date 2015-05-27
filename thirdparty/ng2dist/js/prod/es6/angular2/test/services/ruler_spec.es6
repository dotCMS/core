import {AsyncTestCompleter,
  inject,
  ddescribe,
  describe,
  it,
  iit,
  xit,
  expect,
  SpyObject,
  proxy} from 'angular2/test_lib';
import {DOM,
  DomAdapter} from 'angular2/src/dom/dom_adapter';
import {ElementRef} from 'angular2/src/core/compiler/element_ref';
import {Ruler,
  Rectangle} from 'angular2/src/services/ruler';
import {createRectangle} from './rectangle_mock';
import {IMPLEMENTS} from 'angular2/src/facade/lang';
function assertDimensions(rect, left, right, top, bottom, width, height) {
  expect(rect.left).toEqual(left);
  expect(rect.right).toEqual(right);
  expect(rect.top).toEqual(top);
  expect(rect.bottom).toEqual(bottom);
  expect(rect.width).toEqual(width);
  expect(rect.height).toEqual(height);
}
Object.defineProperty(assertDimensions, "parameters", {get: function() {
    return [[Rectangle], [], [], [], [], [], []];
  }});
export function main() {
  describe('ruler service', () => {
    it('should allow measuring ElementRefs', inject([AsyncTestCompleter], (async) => {
      var ruler = new Ruler(SpyObject.stub(new SpyDomAdapter(), {'getBoundingClientRect': createRectangle(10, 20, 200, 100)}));
      var elRef = new SpyElementRef();
      ruler.measure(elRef).then((rect) => {
        assertDimensions(rect, 10, 210, 20, 120, 200, 100);
        async.done();
      });
    }));
    it('should return 0 for all rectangle values while measuring elements in a document fragment', inject([AsyncTestCompleter], (async) => {
      var ruler = new Ruler(DOM);
      var elRef = new SpyElementRef();
      elRef.domElement = DOM.createElement('div');
      ruler.measure(elRef).then((rect) => {
        assertDimensions(rect, 0, 0, 0, 0, 0, 0);
        async.done();
      });
    }));
  });
}
class SpyElementRef extends SpyObject {
  constructor() {
    super(ElementRef);
  }
  noSuchMethod(m) {
    return super.noSuchMethod(m);
  }
}
Object.defineProperty(SpyElementRef, "annotations", {get: function() {
    return [new proxy, new IMPLEMENTS(ElementRef)];
  }});
class SpyDomAdapter extends SpyObject {
  constructor() {
    super(DomAdapter);
  }
  noSuchMethod(m) {
    return super.noSuchMethod(m);
  }
}
Object.defineProperty(SpyDomAdapter, "annotations", {get: function() {
    return [new proxy, new IMPLEMENTS(DomAdapter)];
  }});
//# sourceMappingURL=ruler_spec.js.map

//# sourceMappingURL=./ruler_spec.map