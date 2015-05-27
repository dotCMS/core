import {Injectable,
  Binding} from 'angular2/di';
import {Type,
  isBlank,
  isPresent,
  BaseException,
  normalizeBlank,
  stringify} from 'angular2/src/facade/lang';
import {Promise,
  PromiseWrapper} from 'angular2/src/facade/async';
import {List,
  ListWrapper,
  Map,
  MapWrapper} from 'angular2/src/facade/collection';
import {DirectiveMetadataReader} from './directive_metadata_reader';
import {Component,
  Directive} from '../annotations_impl/annotations';
import {AppProtoView} from './view';
import {ProtoViewRef} from './view_ref';
import {DirectiveBinding} from './element_injector';
import {TemplateResolver} from './template_resolver';
import {View} from '../annotations_impl/view';
import {ComponentUrlMapper} from './component_url_mapper';
import {ProtoViewFactory} from './proto_view_factory';
import {UrlResolver} from 'angular2/src/services/url_resolver';
import * as renderApi from 'angular2/src/render/api';
export class CompilerCache {
  constructor() {
    this._cache = MapWrapper.create();
  }
  set(component, protoView) {
    MapWrapper.set(this._cache, component, protoView);
  }
  get(component) {
    var result = MapWrapper.get(this._cache, component);
    return normalizeBlank(result);
  }
  clear() {
    MapWrapper.clear(this._cache);
  }
}
Object.defineProperty(CompilerCache, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(CompilerCache.prototype.set, "parameters", {get: function() {
    return [[Type], [AppProtoView]];
  }});
Object.defineProperty(CompilerCache.prototype.get, "parameters", {get: function() {
    return [[Type]];
  }});
export class Compiler {
  constructor(reader, cache, templateResolver, componentUrlMapper, urlResolver, renderer, protoViewFactory) {
    this._reader = reader;
    this._compilerCache = cache;
    this._compiling = MapWrapper.create();
    this._templateResolver = templateResolver;
    this._componentUrlMapper = componentUrlMapper;
    this._urlResolver = urlResolver;
    this._appUrl = urlResolver.resolve(null, './');
    this._renderer = renderer;
    this._protoViewFactory = protoViewFactory;
  }
  _bindDirective(directiveTypeOrBinding) {
    if (directiveTypeOrBinding instanceof DirectiveBinding) {
      return directiveTypeOrBinding;
    } else if (directiveTypeOrBinding instanceof Binding) {
      let meta = this._reader.read(directiveTypeOrBinding.token);
      return DirectiveBinding.createFromBinding(directiveTypeOrBinding, meta.annotation);
    } else {
      let meta = this._reader.read(directiveTypeOrBinding);
      return DirectiveBinding.createFromType(meta.type, meta.annotation);
    }
  }
  compileInHost(componentTypeOrBinding) {
    var componentBinding = this._bindDirective(componentTypeOrBinding);
    this._assertTypeIsComponent(componentBinding);
    var directiveMetadata = Compiler.buildRenderDirective(componentBinding);
    return this._renderer.createHostProtoView(directiveMetadata).then((hostRenderPv) => {
      return this._compileNestedProtoViews(null, null, hostRenderPv, [componentBinding], true);
    }).then((appProtoView) => {
      return new ProtoViewRef(appProtoView);
    });
  }
  compile(component) {
    var componentBinding = this._bindDirective(component);
    this._assertTypeIsComponent(componentBinding);
    var protoView = this._compile(componentBinding);
    var pvPromise = PromiseWrapper.isPromise(protoView) ? protoView : PromiseWrapper.resolve(protoView);
    return pvPromise.then((appProtoView) => {
      return new ProtoViewRef(appProtoView);
    });
  }
  _compile(componentBinding) {
    var component = componentBinding.key.token;
    var protoView = this._compilerCache.get(component);
    if (isPresent(protoView)) {
      return protoView;
    }
    var pvPromise = MapWrapper.get(this._compiling, component);
    if (isPresent(pvPromise)) {
      return pvPromise;
    }
    var template = this._templateResolver.resolve(component);
    if (isBlank(template)) {
      return null;
    }
    if (isPresent(template.renderer)) {
      var directives = [];
      pvPromise = this._renderer.createImperativeComponentProtoView(template.renderer).then((renderPv) => {
        return this._compileNestedProtoViews(null, componentBinding, renderPv, directives, true);
      });
    } else {
      var directives = ListWrapper.map(this._flattenDirectives(template), (directive) => this._bindDirective(directive));
      var renderTemplate = this._buildRenderTemplate(component, template, directives);
      pvPromise = this._renderer.compile(renderTemplate).then((renderPv) => {
        return this._compileNestedProtoViews(null, componentBinding, renderPv, directives, true);
      });
    }
    MapWrapper.set(this._compiling, component, pvPromise);
    return pvPromise;
  }
  _compileNestedProtoViews(parentProtoView, componentBinding, renderPv, directives, isComponentRootView) {
    var nestedPVPromises = [];
    var protoView = this._protoViewFactory.createProtoView(parentProtoView, componentBinding, renderPv, directives);
    if (isComponentRootView && isPresent(componentBinding)) {
      var component = componentBinding.key.token;
      this._compilerCache.set(component, protoView);
      MapWrapper.delete(this._compiling, component);
    }
    var binderIndex = 0;
    ListWrapper.forEach(protoView.elementBinders, (elementBinder) => {
      var nestedComponent = elementBinder.componentDirective;
      var nestedRenderProtoView = renderPv.elementBinders[binderIndex].nestedProtoView;
      var elementBinderDone = (nestedPv) => {
        elementBinder.nestedProtoView = nestedPv;
      };
      var nestedCall = null;
      if (isPresent(nestedComponent)) {
        nestedCall = this._compile(nestedComponent);
      } else if (isPresent(nestedRenderProtoView)) {
        nestedCall = this._compileNestedProtoViews(protoView, componentBinding, nestedRenderProtoView, directives, false);
      }
      if (PromiseWrapper.isPromise(nestedCall)) {
        ListWrapper.push(nestedPVPromises, nestedCall.then(elementBinderDone));
      } else if (isPresent(nestedCall)) {
        elementBinderDone(nestedCall);
      }
      binderIndex++;
    });
    var protoViewDone = (_) => {
      var childComponentRenderPvRefs = [];
      ListWrapper.forEach(protoView.elementBinders, (eb) => {
        if (isPresent(eb.componentDirective)) {
          var componentPv = eb.nestedProtoView;
          ListWrapper.push(childComponentRenderPvRefs, isPresent(componentPv) ? componentPv.render : null);
        }
      });
      this._renderer.mergeChildComponentProtoViews(protoView.render, childComponentRenderPvRefs);
      return protoView;
    };
    if (nestedPVPromises.length > 0) {
      return PromiseWrapper.all(nestedPVPromises).then(protoViewDone);
    } else {
      return protoViewDone(null);
    }
  }
  _buildRenderTemplate(component, view, directives) {
    var componentUrl = this._urlResolver.resolve(this._appUrl, this._componentUrlMapper.getUrl(component));
    var templateAbsUrl = null;
    if (isPresent(view.templateUrl)) {
      templateAbsUrl = this._urlResolver.resolve(componentUrl, view.templateUrl);
    } else if (isPresent(view.template)) {
      templateAbsUrl = componentUrl;
    }
    return new renderApi.ViewDefinition({
      componentId: stringify(component),
      absUrl: templateAbsUrl,
      template: view.template,
      directives: ListWrapper.map(directives, Compiler.buildRenderDirective)
    });
  }
  static buildRenderDirective(directiveBinding) {
    var ann = directiveBinding.annotation;
    var renderType;
    var compileChildren = ann.compileChildren;
    if (ann instanceof Component) {
      renderType = renderApi.DirectiveMetadata.COMPONENT_TYPE;
    } else {
      renderType = renderApi.DirectiveMetadata.DIRECTIVE_TYPE;
    }
    var readAttributes = [];
    ListWrapper.forEach(directiveBinding.dependencies, (dep) => {
      if (isPresent(dep.attributeName)) {
        ListWrapper.push(readAttributes, dep.attributeName);
      }
    });
    return new renderApi.DirectiveMetadata({
      id: stringify(directiveBinding.key.token),
      type: renderType,
      selector: ann.selector,
      compileChildren: compileChildren,
      hostListeners: isPresent(ann.hostListeners) ? MapWrapper.createFromStringMap(ann.hostListeners) : null,
      hostProperties: isPresent(ann.hostProperties) ? MapWrapper.createFromStringMap(ann.hostProperties) : null,
      properties: isPresent(ann.properties) ? MapWrapper.createFromStringMap(ann.properties) : null,
      readAttributes: readAttributes
    });
  }
  _flattenDirectives(template) {
    if (isBlank(template.directives))
      return [];
    var directives = [];
    this._flattenList(template.directives, directives);
    return directives;
  }
  _flattenList(tree, out) {
    for (var i = 0; i < tree.length; i++) {
      var item = tree[i];
      if (ListWrapper.isList(item)) {
        this._flattenList(item, out);
      } else {
        ListWrapper.push(out, item);
      }
    }
  }
  _assertTypeIsComponent(directiveBinding) {
    if (!(directiveBinding.annotation instanceof Component)) {
      throw new BaseException(`Could not load '${stringify(directiveBinding.key.token)}' because it is not a component.`);
    }
  }
}
Object.defineProperty(Compiler, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(Compiler, "parameters", {get: function() {
    return [[DirectiveMetadataReader], [CompilerCache], [TemplateResolver], [ComponentUrlMapper], [UrlResolver], [renderApi.Renderer], [ProtoViewFactory]];
  }});
Object.defineProperty(Compiler.prototype.compileInHost, "parameters", {get: function() {
    return [[assert.type.any]];
  }});
Object.defineProperty(Compiler.prototype.compile, "parameters", {get: function() {
    return [[Type]];
  }});
Object.defineProperty(Compiler.prototype._compile, "parameters", {get: function() {
    return [[DirectiveBinding]];
  }});
Object.defineProperty(Compiler.prototype._flattenDirectives, "parameters", {get: function() {
    return [[View]];
  }});
Object.defineProperty(Compiler.prototype._flattenList, "parameters", {get: function() {
    return [[assert.genericType(List, assert.type.any)], [assert.genericType(List, Type)]];
  }});
Object.defineProperty(Compiler.prototype._assertTypeIsComponent, "parameters", {get: function() {
    return [[DirectiveBinding]];
  }});
//# sourceMappingURL=compiler.js.map

//# sourceMappingURL=./compiler.map