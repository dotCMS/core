import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {isBlank} from 'angular2/src/facade/lang';
import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {Attribute} from 'angular2/src/core/annotations_impl/di';
import {Compiler,
  ViewContainerRef} from 'angular2/core';
import {Injector,
  bind} from 'angular2/di';
import * as routerMod from './router';
import {Instruction,
  RouteParams} from './instruction';
export class RouterOutlet {
  constructor(viewContainer, compiler, router, injector, nameAttr) {
    if (isBlank(nameAttr)) {
      nameAttr = 'default';
    }
    this._router = router;
    this._viewContainer = viewContainer;
    this._compiler = compiler;
    this._injector = injector;
    this._router.registerOutlet(this, nameAttr);
  }
  activate(instruction) {
    return this._compiler.compileInHost(instruction.component).then((pv) => {
      var outletInjector = this._injector.resolveAndCreateChild([bind(RouteParams).toValue(new RouteParams(instruction.params)), bind(routerMod.Router).toValue(instruction.router)]);
      this._viewContainer.clear();
      this._viewContainer.create(pv, 0, outletInjector);
    });
  }
  canActivate(instruction) {
    return PromiseWrapper.resolve(true);
  }
  canDeactivate(instruction) {
    return PromiseWrapper.resolve(true);
  }
}
Object.defineProperty(RouterOutlet, "annotations", {get: function() {
    return [new Directive({selector: 'router-outlet'})];
  }});
Object.defineProperty(RouterOutlet, "parameters", {get: function() {
    return [[ViewContainerRef], [Compiler], [routerMod.Router], [Injector], [new Attribute('name')]];
  }});
Object.defineProperty(RouterOutlet.prototype.activate, "parameters", {get: function() {
    return [[Instruction]];
  }});
Object.defineProperty(RouterOutlet.prototype.canActivate, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
Object.defineProperty(RouterOutlet.prototype.canDeactivate, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
//# sourceMappingURL=router_outlet.js.map

//# sourceMappingURL=./router_outlet.map