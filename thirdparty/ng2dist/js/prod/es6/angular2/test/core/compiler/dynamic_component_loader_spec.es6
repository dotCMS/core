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
  xit} from 'angular2/test_lib';
import {TestBed} from 'angular2/src/test_lib/test_bed';
import {Component} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {DynamicComponentLoader} from 'angular2/src/core/compiler/dynamic_component_loader';
import {ElementRef} from 'angular2/src/core/compiler/element_ref';
import {If} from 'angular2/src/directives/if';
import {DirectDomRenderer} from 'angular2/src/render/dom/direct_dom_renderer';
import {DOM} from 'angular2/src/dom/dom_adapter';
export function main() {
  describe('DynamicComponentLoader', function() {
    describe("loading into existing location", () => {
      it('should work', inject([TestBed, AsyncTestCompleter], (tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<dynamic-comp #dynamic></dynamic-comp>',
          directives: [DynamicComp]
        }));
        tb.createView(MyComp).then((view) => {
          var dynamicComponent = view.rawView.locals.get("dynamic");
          expect(dynamicComponent).toBeAnInstanceOf(DynamicComp);
          dynamicComponent.done.then((_) => {
            view.detectChanges();
            expect(view.rootNodes).toHaveText('hello');
            async.done();
          });
        });
      }));
      it('should inject dependencies of the dynamically-loaded component', inject([TestBed, AsyncTestCompleter], (tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<dynamic-comp #dynamic></dynamic-comp>',
          directives: [DynamicComp]
        }));
        tb.createView(MyComp).then((view) => {
          var dynamicComponent = view.rawView.locals.get("dynamic");
          dynamicComponent.done.then((ref) => {
            expect(ref.instance.dynamicallyCreatedComponentService).toBeAnInstanceOf(DynamicallyCreatedComponentService);
            async.done();
          });
        });
      }));
      it('should allow to destroy and create them via viewcontainer directives', inject([TestBed, AsyncTestCompleter], (tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<div><dynamic-comp #dynamic template="if: ctxBoolProp"></dynamic-comp></div>',
          directives: [DynamicComp, If]
        }));
        tb.createView(MyComp).then((view) => {
          view.context.ctxBoolProp = true;
          view.detectChanges();
          var dynamicComponent = view.rawView.viewContainers[0].views[0].locals.get("dynamic");
          dynamicComponent.done.then((_) => {
            view.detectChanges();
            expect(view.rootNodes).toHaveText('hello');
            view.context.ctxBoolProp = false;
            view.detectChanges();
            expect(view.rawView.viewContainers[0].views.length).toBe(0);
            expect(view.rootNodes).toHaveText('');
            view.context.ctxBoolProp = true;
            view.detectChanges();
            var dynamicComponent = view.rawView.viewContainers[0].views[0].locals.get("dynamic");
            return dynamicComponent.done;
          }).then((_) => {
            view.detectChanges();
            expect(view.rootNodes).toHaveText('hello');
            async.done();
          });
        });
      }));
    });
    describe("loading next to an existing location", () => {
      it('should work', inject([DynamicComponentLoader, TestBed, AsyncTestCompleter], (loader, tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<div><location #loc></location></div>',
          directives: [Location]
        }));
        tb.createView(MyComp).then((view) => {
          var location = view.rawView.locals.get("loc");
          loader.loadNextToExistingLocation(DynamicallyLoaded, location.elementRef).then((ref) => {
            expect(view.rootNodes).toHaveText("Location;DynamicallyLoaded;");
            async.done();
          });
        });
      }));
      it('should return a disposable component ref', inject([DynamicComponentLoader, TestBed, AsyncTestCompleter], (loader, tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<div><location #loc></location></div>',
          directives: [Location]
        }));
        tb.createView(MyComp).then((view) => {
          var location = view.rawView.locals.get("loc");
          loader.loadNextToExistingLocation(DynamicallyLoaded, location.elementRef).then((ref) => {
            loader.loadNextToExistingLocation(DynamicallyLoaded2, location.elementRef).then((ref2) => {
              expect(view.rootNodes).toHaveText("Location;DynamicallyLoaded;DynamicallyLoaded2;");
              ref2.dispose();
              expect(view.rootNodes).toHaveText("Location;DynamicallyLoaded;");
              async.done();
            });
          });
        });
      }));
      it('should update host properties', inject([DynamicComponentLoader, TestBed, AsyncTestCompleter], (loader, tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<div><location #loc></location></div>',
          directives: [Location]
        }));
        tb.createView(MyComp).then((view) => {
          var location = view.rawView.locals.get("loc");
          loader.loadNextToExistingLocation(DynamicallyLoadedWithHostProps, location.elementRef).then((ref) => {
            ref.instance.id = "new value";
            view.detectChanges();
            var newlyInsertedElement = DOM.childNodesAsList(view.rootNodes[0])[1];
            expect(newlyInsertedElement.id).toEqual("new value");
            async.done();
          });
        });
      }));
    });
    describe('loading into a new location', () => {
      it('should allow to create, update and destroy components', inject([TestBed, AsyncTestCompleter], (tb, async) => {
        tb.overrideView(MyComp, new View({
          template: '<imp-ng-cmp #impview></imp-ng-cmp>',
          directives: [ImperativeViewComponentUsingNgComponent]
        }));
        tb.createView(MyComp).then((view) => {
          var userViewComponent = view.rawView.locals.get("impview");
          userViewComponent.done.then((childComponentRef) => {
            view.detectChanges();
            expect(view.rootNodes).toHaveText('hello');
            childComponentRef.instance.ctxProp = 'new';
            view.detectChanges();
            expect(view.rootNodes).toHaveText('new');
            childComponentRef.dispose();
            expect(view.rootNodes).toHaveText('');
            async.done();
          });
        });
      }));
    });
  });
}
class ImperativeViewComponentUsingNgComponent {
  constructor(self, dynamicComponentLoader, renderer) {
    var div = el('<div></div>');
    renderer.setImperativeComponentRootNodes(self.parentView.render, self.boundElementIndex, [div]);
    this.done = dynamicComponentLoader.loadIntoNewLocation(ChildComp, self, div, null);
  }
}
Object.defineProperty(ImperativeViewComponentUsingNgComponent, "annotations", {get: function() {
    return [new Component({selector: 'imp-ng-cmp'}), new View({renderer: 'imp-ng-cmp-renderer'})];
  }});
Object.defineProperty(ImperativeViewComponentUsingNgComponent, "parameters", {get: function() {
    return [[ElementRef], [DynamicComponentLoader], [DirectDomRenderer]];
  }});
class ChildComp {
  constructor() {
    this.ctxProp = 'hello';
  }
}
Object.defineProperty(ChildComp, "annotations", {get: function() {
    return [new Component({selector: 'child-cmp'}), new View({template: '{{ctxProp}}'})];
  }});
class DynamicallyCreatedComponentService {}
class DynamicComp {
  constructor(loader, location) {
    this.done = loader.loadIntoExistingLocation(DynamicallyCreatedCmp, location);
  }
}
Object.defineProperty(DynamicComp, "annotations", {get: function() {
    return [new Component({selector: 'dynamic-comp'})];
  }});
Object.defineProperty(DynamicComp, "parameters", {get: function() {
    return [[DynamicComponentLoader], [ElementRef]];
  }});
class DynamicallyCreatedCmp {
  constructor(a) {
    this.greeting = "hello";
    this.dynamicallyCreatedComponentService = a;
  }
}
Object.defineProperty(DynamicallyCreatedCmp, "annotations", {get: function() {
    return [new Component({
      selector: 'hello-cmp',
      injectables: [DynamicallyCreatedComponentService]
    }), new View({template: "{{greeting}}"})];
  }});
Object.defineProperty(DynamicallyCreatedCmp, "parameters", {get: function() {
    return [[DynamicallyCreatedComponentService]];
  }});
class DynamicallyLoaded {}
Object.defineProperty(DynamicallyLoaded, "annotations", {get: function() {
    return [new Component({selector: 'dummy'}), new View({template: "DynamicallyLoaded;"})];
  }});
class DynamicallyLoaded2 {}
Object.defineProperty(DynamicallyLoaded2, "annotations", {get: function() {
    return [new Component({selector: 'dummy'}), new View({template: "DynamicallyLoaded2;"})];
  }});
class DynamicallyLoadedWithHostProps {
  constructor() {
    this.id = "default";
  }
}
Object.defineProperty(DynamicallyLoadedWithHostProps, "annotations", {get: function() {
    return [new Component({
      selector: 'dummy',
      hostProperties: {'id': 'id'}
    }), new View({template: "DynamicallyLoadedWithHostProps;"})];
  }});
class Location {
  constructor(elementRef) {
    this.elementRef = elementRef;
  }
}
Object.defineProperty(Location, "annotations", {get: function() {
    return [new Component({selector: 'location'}), new View({template: "Location;"})];
  }});
Object.defineProperty(Location, "parameters", {get: function() {
    return [[ElementRef]];
  }});
class MyComp {
  constructor() {
    this.ctxBoolProp = false;
  }
}
Object.defineProperty(MyComp, "annotations", {get: function() {
    return [new Component({selector: 'my-comp'}), new View({directives: []})];
  }});
//# sourceMappingURL=dynamic_component_loader_spec.js.map

//# sourceMappingURL=./dynamic_component_loader_spec.map