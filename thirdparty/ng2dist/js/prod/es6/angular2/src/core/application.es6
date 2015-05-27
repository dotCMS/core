import {Injector,
  bind,
  OpaqueToken,
  Optional} from 'angular2/di';
import {NumberWrapper,
  Type,
  isBlank,
  isPresent,
  BaseException,
  assertionsEnabled,
  print,
  stringify} from 'angular2/src/facade/lang';
import {BrowserDomAdapter} from 'angular2/src/dom/browser_adapter';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {Compiler,
  CompilerCache} from './compiler/compiler';
import {Reflector,
  reflector} from 'angular2/src/reflection/reflection';
import {Parser,
  Lexer,
  ChangeDetection,
  DynamicChangeDetection,
  PipeRegistry,
  defaultPipeRegistry} from 'angular2/change_detection';
import {ExceptionHandler} from './exception_handler';
import {TemplateLoader} from 'angular2/src/render/dom/compiler/template_loader';
import {TemplateResolver} from './compiler/template_resolver';
import {DirectiveMetadataReader} from './compiler/directive_metadata_reader';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {VmTurnZone} from 'angular2/src/core/zone/vm_turn_zone';
import {LifeCycle} from 'angular2/src/core/life_cycle/life_cycle';
import {ShadowDomStrategy} from 'angular2/src/render/dom/shadow_dom/shadow_dom_strategy';
import {EmulatedUnscopedShadowDomStrategy} from 'angular2/src/render/dom/shadow_dom/emulated_unscoped_shadow_dom_strategy';
import {XHR} from 'angular2/src/services/xhr';
import {XHRImpl} from 'angular2/src/services/xhr_impl';
import {EventManager,
  DomEventsPlugin} from 'angular2/src/render/dom/events/event_manager';
import {KeyEventsPlugin} from 'angular2/src/render/dom/events/key_events';
import {HammerGesturesPlugin} from 'angular2/src/render/dom/events/hammer_gestures';
import {Binding} from 'angular2/src/di/binding';
import {ComponentUrlMapper} from 'angular2/src/core/compiler/component_url_mapper';
import {UrlResolver} from 'angular2/src/services/url_resolver';
import {StyleUrlResolver} from 'angular2/src/render/dom/shadow_dom/style_url_resolver';
import {StyleInliner} from 'angular2/src/render/dom/shadow_dom/style_inliner';
import {ComponentRef,
  DynamicComponentLoader} from 'angular2/src/core/compiler/dynamic_component_loader';
import {TestabilityRegistry,
  Testability} from 'angular2/src/core/testability/testability';
import {AppViewPool,
  APP_VIEW_POOL_CAPACITY} from 'angular2/src/core/compiler/view_pool';
import {AppViewManager} from 'angular2/src/core/compiler/view_manager';
import {AppViewManagerUtils} from 'angular2/src/core/compiler/view_manager_utils';
import {ProtoViewFactory} from 'angular2/src/core/compiler/proto_view_factory';
import {Renderer} from 'angular2/src/render/api';
import {DirectDomRenderer} from 'angular2/src/render/dom/direct_dom_renderer';
import * as rc from 'angular2/src/render/dom/compiler/compiler';
import * as rvf from 'angular2/src/render/dom/view/view_factory';
import * as rvh from 'angular2/src/render/dom/view/view_hydrator';
import {internalView} from 'angular2/src/core/compiler/view_ref';
import {appComponentRefToken,
  appElementToken,
  appComponentAnnotatedTypeToken,
  appDocumentToken} from './application_tokens';
var _rootInjector;
var _rootBindings = [bind(Reflector).toValue(reflector), TestabilityRegistry];
function _injectorBindings(appComponentType) {
  return [bind(appDocumentToken).toValue(DOM.defaultDoc()), bind(appComponentAnnotatedTypeToken).toFactory((reader) => {
    return reader.read(appComponentType);
  }, [DirectiveMetadataReader]), bind(appElementToken).toFactory((appComponentAnnotatedType, appDocument) => {
    var selector = appComponentAnnotatedType.annotation.selector;
    var element = DOM.querySelector(appDocument, selector);
    if (isBlank(element)) {
      throw new BaseException(`The app selector "${selector}" did not match any elements`);
    }
    return element;
  }, [appComponentAnnotatedTypeToken, appDocumentToken]), bind(appComponentRefToken).toAsyncFactory((dynamicComponentLoader, injector, appElement, appComponentAnnotatedType, testability, registry) => {
    registry.registerApplication(appElement, testability);
    return dynamicComponentLoader.loadIntoNewLocation(appComponentAnnotatedType.type, null, appElement, injector);
  }, [DynamicComponentLoader, Injector, appElementToken, appComponentAnnotatedTypeToken, Testability, TestabilityRegistry]), bind(appComponentType).toFactory((ref) => ref.instance, [appComponentRefToken]), bind(LifeCycle).toFactory((exceptionHandler) => new LifeCycle(exceptionHandler, null, assertionsEnabled()), [ExceptionHandler]), bind(EventManager).toFactory((zone) => {
    var plugins = [new HammerGesturesPlugin(), new KeyEventsPlugin(), new DomEventsPlugin()];
    return new EventManager(plugins, zone);
  }, [VmTurnZone]), bind(ShadowDomStrategy).toFactory((styleUrlResolver, doc) => new EmulatedUnscopedShadowDomStrategy(styleUrlResolver, doc.head), [StyleUrlResolver, appDocumentToken]), DirectDomRenderer, bind(Renderer).toClass(DirectDomRenderer), bind(rc.Compiler).toClass(rc.DefaultCompiler), bind(rvf.ViewFactory).toFactory((capacity, eventManager, shadowDomStrategy) => new rvf.ViewFactory(capacity, eventManager, shadowDomStrategy), [rvf.VIEW_POOL_CAPACITY, EventManager, ShadowDomStrategy]), bind(rvf.VIEW_POOL_CAPACITY).toValue(10000), rvh.RenderViewHydrator, ProtoViewFactory, bind(AppViewPool).toFactory((capacity) => new AppViewPool(capacity), [APP_VIEW_POOL_CAPACITY]), bind(APP_VIEW_POOL_CAPACITY).toValue(10000), AppViewManager, AppViewManagerUtils, Compiler, CompilerCache, TemplateResolver, bind(PipeRegistry).toValue(defaultPipeRegistry), bind(ChangeDetection).toClass(DynamicChangeDetection), TemplateLoader, DirectiveMetadataReader, Parser, Lexer, ExceptionHandler, bind(XHR).toValue(new XHRImpl()), ComponentUrlMapper, UrlResolver, StyleUrlResolver, StyleInliner, DynamicComponentLoader, Testability];
}
function _createVmZone(givenReporter) {
  var defaultErrorReporter = (exception, stackTrace) => {
    var longStackTrace = ListWrapper.join(stackTrace, "\n\n-----async gap-----\n");
    DOM.logError(`${exception}\n\n${longStackTrace}`);
    throw exception;
  };
  var reporter = isPresent(givenReporter) ? givenReporter : defaultErrorReporter;
  var zone = new VmTurnZone({enableLongStackTrace: assertionsEnabled()});
  zone.initCallbacks({onErrorHandler: reporter});
  return zone;
}
Object.defineProperty(_createVmZone, "parameters", {get: function() {
    return [[Function]];
  }});
export function bootstrap(appComponentType, componentInjectableBindings = null, errorReporter = null) {
  BrowserDomAdapter.makeCurrent();
  var bootstrapProcess = PromiseWrapper.completer();
  var zone = _createVmZone(errorReporter);
  zone.run(() => {
    var appInjector = _createAppInjector(appComponentType, componentInjectableBindings, zone);
    PromiseWrapper.then(appInjector.asyncGet(appComponentRefToken), (componentRef) => {
      var appChangeDetector = internalView(componentRef.hostView).changeDetector;
      var lc = appInjector.get(LifeCycle);
      lc.registerWith(zone, appChangeDetector);
      lc.tick();
      bootstrapProcess.resolve(new ApplicationRef(componentRef, appInjector));
    }, (err) => {
      bootstrapProcess.reject(err);
    });
  });
  return bootstrapProcess.promise;
}
Object.defineProperty(bootstrap, "parameters", {get: function() {
    return [[Type], [assert.genericType(List, Binding)], [Function]];
  }});
export class ApplicationRef {
  constructor(hostComponent, injector) {
    this._hostComponent = hostComponent;
    this._injector = injector;
  }
  get hostComponent() {
    return this._hostComponent.instance;
  }
  dispose() {
    return this._hostComponent.dispose();
  }
  get injector() {
    return this._injector;
  }
}
Object.defineProperty(ApplicationRef, "parameters", {get: function() {
    return [[ComponentRef], [Injector]];
  }});
function _createAppInjector(appComponentType, bindings, zone) {
  if (isBlank(_rootInjector))
    _rootInjector = Injector.resolveAndCreate(_rootBindings);
  var mergedBindings = isPresent(bindings) ? ListWrapper.concat(_injectorBindings(appComponentType), bindings) : _injectorBindings(appComponentType);
  ListWrapper.push(mergedBindings, bind(VmTurnZone).toValue(zone));
  return _rootInjector.resolveAndCreateChild(mergedBindings);
}
Object.defineProperty(_createAppInjector, "parameters", {get: function() {
    return [[Type], [assert.genericType(List, Binding)], [VmTurnZone]];
  }});
//# sourceMappingURL=application.js.map

//# sourceMappingURL=./application.map