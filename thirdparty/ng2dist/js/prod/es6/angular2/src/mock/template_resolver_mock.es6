import {Map,
  MapWrapper,
  ListWrapper} from 'angular2/src/facade/collection';
import {Type,
  isPresent,
  BaseException,
  stringify,
  isBlank} from 'angular2/src/facade/lang';
import {View} from 'angular2/src/core/annotations_impl/view';
import {TemplateResolver} from 'angular2/src/core/compiler/template_resolver';
export class MockTemplateResolver extends TemplateResolver {
  constructor() {
    super();
    this._templates = MapWrapper.create();
    this._inlineTemplates = MapWrapper.create();
    this._templateCache = MapWrapper.create();
    this._directiveOverrides = MapWrapper.create();
  }
  setView(component, view) {
    this._checkOverrideable(component);
    MapWrapper.set(this._templates, component, view);
  }
  setInlineTemplate(component, template) {
    this._checkOverrideable(component);
    MapWrapper.set(this._inlineTemplates, component, template);
  }
  overrideTemplateDirective(component, from, to) {
    this._checkOverrideable(component);
    var overrides = MapWrapper.get(this._directiveOverrides, component);
    if (isBlank(overrides)) {
      overrides = MapWrapper.create();
      MapWrapper.set(this._directiveOverrides, component, overrides);
    }
    MapWrapper.set(overrides, from, to);
  }
  resolve(component) {
    var view = MapWrapper.get(this._templateCache, component);
    if (isPresent(view))
      return view;
    view = MapWrapper.get(this._templates, component);
    if (isBlank(view)) {
      view = super.resolve(component);
    }
    if (isBlank(view)) {
      return null;
    }
    var directives = view.directives;
    var overrides = MapWrapper.get(this._directiveOverrides, component);
    if (isPresent(overrides) && isPresent(directives)) {
      directives = ListWrapper.clone(view.directives);
      MapWrapper.forEach(overrides, (to, from) => {
        var srcIndex = directives.indexOf(from);
        if (srcIndex == -1) {
          throw new BaseException(`Overriden directive ${stringify(from)} not found in the template of ${stringify(component)}`);
        }
        directives[srcIndex] = to;
      });
      view = new View({
        template: view.template,
        templateUrl: view.templateUrl,
        directives: directives
      });
    }
    var inlineTemplate = MapWrapper.get(this._inlineTemplates, component);
    if (isPresent(inlineTemplate)) {
      view = new View({
        template: inlineTemplate,
        templateUrl: null,
        directives: view.directives
      });
    }
    MapWrapper.set(this._templateCache, component, view);
    return view;
  }
  _checkOverrideable(component) {
    var cached = MapWrapper.get(this._templateCache, component);
    if (isPresent(cached)) {
      throw new BaseException(`The component ${stringify(component)} has already been compiled, its configuration can not be changed`);
    }
  }
}
Object.defineProperty(MockTemplateResolver.prototype.setView, "parameters", {get: function() {
    return [[Type], [View]];
  }});
Object.defineProperty(MockTemplateResolver.prototype.setInlineTemplate, "parameters", {get: function() {
    return [[Type], [assert.type.string]];
  }});
Object.defineProperty(MockTemplateResolver.prototype.overrideTemplateDirective, "parameters", {get: function() {
    return [[Type], [Type], [Type]];
  }});
Object.defineProperty(MockTemplateResolver.prototype.resolve, "parameters", {get: function() {
    return [[Type]];
  }});
Object.defineProperty(MockTemplateResolver.prototype._checkOverrideable, "parameters", {get: function() {
    return [[Type]];
  }});
//# sourceMappingURL=template_resolver_mock.js.map

//# sourceMappingURL=./template_resolver_mock.map