import {isPresent,
  isBlank,
  BaseException} from 'angular2/src/facade/lang';
import {ListWrapper,
  MapWrapper,
  Set,
  SetWrapper,
  List} from 'angular2/src/facade/collection';
import {DOM} from 'angular2/src/dom/dom_adapter';
import {ASTWithSource,
  AST,
  AstTransformer,
  AccessMember,
  LiteralArray,
  ImplicitReceiver} from 'angular2/change_detection';
import {RenderProtoView} from './proto_view';
import {ElementBinder,
  Event} from './element_binder';
import {setterFactory} from './property_setter_factory';
import * as api from '../../api';
import * as directDomRenderer from '../direct_dom_renderer';
import {NG_BINDING_CLASS,
  EVENT_TARGET_SEPARATOR} from '../util';
export class ProtoViewBuilder {
  constructor(rootElement) {
    this.rootElement = rootElement;
    this.elements = [];
    this.variableBindings = MapWrapper.create();
    this.imperativeRendererId = null;
  }
  setImperativeRendererId(id) {
    this.imperativeRendererId = id;
    return this;
  }
  bindElement(element, description = null) {
    var builder = new ElementBinderBuilder(this.elements.length, element, description);
    ListWrapper.push(this.elements, builder);
    DOM.addClass(element, NG_BINDING_CLASS);
    return builder;
  }
  bindVariable(name, value) {
    MapWrapper.set(this.variableBindings, value, name);
  }
  build() {
    var renderElementBinders = [];
    var apiElementBinders = [];
    ListWrapper.forEach(this.elements, (ebb) => {
      var propertySetters = MapWrapper.create();
      var apiDirectiveBinders = ListWrapper.map(ebb.directives, (dbb) => {
        ebb.eventBuilder.merge(dbb.eventBuilder);
        MapWrapper.forEach(dbb.hostPropertyBindings, (_, hostPropertyName) => {
          MapWrapper.set(propertySetters, hostPropertyName, setterFactory(hostPropertyName));
        });
        return new api.DirectiveBinder({
          directiveIndex: dbb.directiveIndex,
          propertyBindings: dbb.propertyBindings,
          eventBindings: dbb.eventBindings,
          hostPropertyBindings: dbb.hostPropertyBindings
        });
      });
      MapWrapper.forEach(ebb.propertyBindings, (_, propertyName) => {
        MapWrapper.set(propertySetters, propertyName, setterFactory(propertyName));
      });
      var nestedProtoView = isPresent(ebb.nestedProtoView) ? ebb.nestedProtoView.build() : null;
      var parentIndex = isPresent(ebb.parent) ? ebb.parent.index : -1;
      ListWrapper.push(apiElementBinders, new api.ElementBinder({
        index: ebb.index,
        parentIndex: parentIndex,
        distanceToParent: ebb.distanceToParent,
        directives: apiDirectiveBinders,
        nestedProtoView: nestedProtoView,
        propertyBindings: ebb.propertyBindings,
        variableBindings: ebb.variableBindings,
        eventBindings: ebb.eventBindings,
        textBindings: ebb.textBindings,
        readAttributes: ebb.readAttributes
      }));
      ListWrapper.push(renderElementBinders, new ElementBinder({
        textNodeIndices: ebb.textBindingIndices,
        contentTagSelector: ebb.contentTagSelector,
        parentIndex: parentIndex,
        distanceToParent: ebb.distanceToParent,
        nestedProtoView: isPresent(nestedProtoView) ? nestedProtoView.render.delegate : null,
        componentId: ebb.componentId,
        eventLocals: new LiteralArray(ebb.eventBuilder.buildEventLocals()),
        localEvents: ebb.eventBuilder.buildLocalEvents(),
        globalEvents: ebb.eventBuilder.buildGlobalEvents(),
        propertySetters: propertySetters
      }));
    });
    return new api.ProtoViewDto({
      render: new directDomRenderer.DirectDomProtoViewRef(new RenderProtoView({
        element: this.rootElement,
        elementBinders: renderElementBinders,
        imperativeRendererId: this.imperativeRendererId
      })),
      elementBinders: apiElementBinders,
      variableBindings: this.variableBindings
    });
  }
}
Object.defineProperty(ProtoViewBuilder.prototype.setImperativeRendererId, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class ElementBinderBuilder {
  constructor(index, element, description) {
    this.element = element;
    this.index = index;
    this.parent = null;
    this.distanceToParent = 0;
    this.directives = [];
    this.nestedProtoView = null;
    this.propertyBindings = MapWrapper.create();
    this.variableBindings = MapWrapper.create();
    this.eventBindings = ListWrapper.create();
    this.eventBuilder = new EventBuilder();
    this.textBindings = [];
    this.textBindingIndices = [];
    this.contentTagSelector = null;
    this.componentId = null;
    this.readAttributes = MapWrapper.create();
  }
  setParent(parent, distanceToParent) {
    this.parent = parent;
    if (isPresent(parent)) {
      this.distanceToParent = distanceToParent;
    }
    return this;
  }
  readAttribute(attrName) {
    if (isBlank(MapWrapper.get(this.readAttributes, attrName))) {
      MapWrapper.set(this.readAttributes, attrName, DOM.getAttribute(this.element, attrName));
    }
  }
  bindDirective(directiveIndex) {
    var directive = new DirectiveBuilder(directiveIndex);
    ListWrapper.push(this.directives, directive);
    return directive;
  }
  bindNestedProtoView(rootElement) {
    if (isPresent(this.nestedProtoView)) {
      throw new BaseException('Only one nested view per element is allowed');
    }
    this.nestedProtoView = new ProtoViewBuilder(rootElement);
    return this.nestedProtoView;
  }
  bindProperty(name, expression) {
    MapWrapper.set(this.propertyBindings, name, expression);
    setterFactory(name);
  }
  bindVariable(name, value) {
    if (isPresent(this.nestedProtoView)) {
      this.nestedProtoView.bindVariable(name, value);
    } else {
      MapWrapper.set(this.variableBindings, value, name);
    }
  }
  bindEvent(name, expression, target = null) {
    ListWrapper.push(this.eventBindings, this.eventBuilder.add(name, expression, target));
  }
  bindText(index, expression) {
    ListWrapper.push(this.textBindingIndices, index);
    ListWrapper.push(this.textBindings, expression);
  }
  setContentTagSelector(value) {
    this.contentTagSelector = value;
  }
  setComponentId(componentId) {
    this.componentId = componentId;
  }
}
Object.defineProperty(ElementBinderBuilder.prototype.setParent, "parameters", {get: function() {
    return [[ElementBinderBuilder], []];
  }});
Object.defineProperty(ElementBinderBuilder.prototype.readAttribute, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ElementBinderBuilder.prototype.bindDirective, "parameters", {get: function() {
    return [[assert.type.number]];
  }});
Object.defineProperty(ElementBinderBuilder.prototype.setContentTagSelector, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ElementBinderBuilder.prototype.setComponentId, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class DirectiveBuilder {
  constructor(directiveIndex) {
    this.directiveIndex = directiveIndex;
    this.propertyBindings = MapWrapper.create();
    this.hostPropertyBindings = MapWrapper.create();
    this.eventBindings = ListWrapper.create();
    this.eventBuilder = new EventBuilder();
  }
  bindProperty(name, expression) {
    MapWrapper.set(this.propertyBindings, name, expression);
  }
  bindHostProperty(name, expression) {
    MapWrapper.set(this.hostPropertyBindings, name, expression);
  }
  bindEvent(name, expression, target = null) {
    ListWrapper.push(this.eventBindings, this.eventBuilder.add(name, expression, target));
  }
}
export class EventBuilder extends AstTransformer {
  constructor() {
    super();
    this.locals = [];
    this.localEvents = [];
    this.globalEvents = [];
    this._implicitReceiver = new ImplicitReceiver();
  }
  add(name, source, target) {
    var adjustedAst = source.ast;
    var fullName = isPresent(target) ? target + EVENT_TARGET_SEPARATOR + name : name;
    var result = new api.EventBinding(fullName, new ASTWithSource(adjustedAst, source.source, source.location));
    var event = new Event(name, target, fullName);
    if (isBlank(target)) {
      ListWrapper.push(this.localEvents, event);
    } else {
      ListWrapper.push(this.globalEvents, event);
    }
    return result;
  }
  visitAccessMember(ast) {
    var isEventAccess = false;
    var current = ast;
    while (!isEventAccess && (current instanceof AccessMember)) {
      if (current.name == '$event') {
        isEventAccess = true;
      }
      current = current.receiver;
    }
    if (isEventAccess) {
      ListWrapper.push(this.locals, ast);
      var index = this.locals.length - 1;
      return new AccessMember(this._implicitReceiver, `${index}`, (arr) => arr[index], null);
    } else {
      return ast;
    }
  }
  buildEventLocals() {
    return this.locals;
  }
  buildLocalEvents() {
    return this.localEvents;
  }
  buildGlobalEvents() {
    return this.globalEvents;
  }
  merge(eventBuilder) {
    this._merge(this.localEvents, eventBuilder.localEvents);
    this._merge(this.globalEvents, eventBuilder.globalEvents);
    ListWrapper.concat(this.locals, eventBuilder.locals);
  }
  _merge(host, tobeAdded) {
    var names = ListWrapper.create();
    for (var i = 0; i < host.length; i++) {
      ListWrapper.push(names, host[i].fullName);
    }
    for (var j = 0; j < tobeAdded.length; j++) {
      if (!ListWrapper.contains(names, tobeAdded[j].fullName)) {
        ListWrapper.push(host, tobeAdded[j]);
      }
    }
  }
}
Object.defineProperty(EventBuilder.prototype.add, "parameters", {get: function() {
    return [[assert.type.string], [ASTWithSource], [assert.type.string]];
  }});
Object.defineProperty(EventBuilder.prototype.visitAccessMember, "parameters", {get: function() {
    return [[AccessMember]];
  }});
Object.defineProperty(EventBuilder.prototype.merge, "parameters", {get: function() {
    return [[EventBuilder]];
  }});
Object.defineProperty(EventBuilder.prototype._merge, "parameters", {get: function() {
    return [[assert.genericType(List, Event)], [assert.genericType(List, Event)]];
  }});
//# sourceMappingURL=proto_view_builder.js.map

//# sourceMappingURL=./proto_view_builder.map