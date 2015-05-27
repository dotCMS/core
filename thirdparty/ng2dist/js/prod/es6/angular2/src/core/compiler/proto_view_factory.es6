import {Injectable} from 'angular2/di';
import {List,
  ListWrapper,
  MapWrapper} from 'angular2/src/facade/collection';
import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import {reflector} from 'angular2/src/reflection/reflection';
import {ChangeDetection,
  DirectiveIndex,
  BindingRecord,
  DirectiveRecord,
  ProtoChangeDetector} from 'angular2/change_detection';
import {Component} from '../annotations_impl/annotations';
import * as renderApi from 'angular2/src/render/api';
import {AppProtoView} from './view';
import {ProtoElementInjector,
  DirectiveBinding} from './element_injector';
class BindingRecordsCreator {
  constructor() {
    this._directiveRecordsMap = MapWrapper.create();
    this._textNodeIndex = 0;
  }
  getBindingRecords(elementBinders, sortedDirectives) {
    var bindings = [];
    for (var boundElementIndex = 0; boundElementIndex < elementBinders.length; boundElementIndex++) {
      var renderElementBinder = elementBinders[boundElementIndex];
      bindings = ListWrapper.concat(bindings, this._createTextNodeRecords(renderElementBinder));
      bindings = ListWrapper.concat(bindings, this._createElementPropertyRecords(boundElementIndex, renderElementBinder));
      bindings = ListWrapper.concat(bindings, this._createDirectiveRecords(boundElementIndex, sortedDirectives[boundElementIndex]));
    }
    return bindings;
  }
  getDirectiveRecords(sortedDirectives) {
    var directiveRecords = [];
    for (var elementIndex = 0; elementIndex < sortedDirectives.length; ++elementIndex) {
      var dirs = sortedDirectives[elementIndex].directives;
      for (var dirIndex = 0; dirIndex < dirs.length; ++dirIndex) {
        ListWrapper.push(directiveRecords, this._getDirectiveRecord(elementIndex, dirIndex, dirs[dirIndex]));
      }
    }
    return directiveRecords;
  }
  _createTextNodeRecords(renderElementBinder) {
    if (isBlank(renderElementBinder.textBindings))
      return [];
    return ListWrapper.map(renderElementBinder.textBindings, (b) => BindingRecord.createForTextNode(b, this._textNodeIndex++));
  }
  _createElementPropertyRecords(boundElementIndex, renderElementBinder) {
    var res = [];
    MapWrapper.forEach(renderElementBinder.propertyBindings, (astWithSource, propertyName) => {
      ListWrapper.push(res, BindingRecord.createForElement(astWithSource, boundElementIndex, propertyName));
    });
    return res;
  }
  _createDirectiveRecords(boundElementIndex, sortedDirectives) {
    var res = [];
    for (var i = 0; i < sortedDirectives.renderDirectives.length; i++) {
      var directiveBinder = sortedDirectives.renderDirectives[i];
      MapWrapper.forEach(directiveBinder.propertyBindings, (astWithSource, propertyName) => {
        var setter = reflector.setter(propertyName);
        var directiveRecord = this._getDirectiveRecord(boundElementIndex, i, sortedDirectives.directives[i]);
        var b = BindingRecord.createForDirective(astWithSource, propertyName, setter, directiveRecord);
        ListWrapper.push(res, b);
      });
      MapWrapper.forEach(directiveBinder.hostPropertyBindings, (astWithSource, propertyName) => {
        var dirIndex = new DirectiveIndex(boundElementIndex, i);
        var b = BindingRecord.createForHostProperty(dirIndex, astWithSource, propertyName);
        ListWrapper.push(res, b);
      });
    }
    return res;
  }
  _getDirectiveRecord(boundElementIndex, directiveIndex, binding) {
    var id = boundElementIndex * 100 + directiveIndex;
    if (!MapWrapper.contains(this._directiveRecordsMap, id)) {
      var changeDetection = binding.changeDetection;
      MapWrapper.set(this._directiveRecordsMap, id, new DirectiveRecord(new DirectiveIndex(boundElementIndex, directiveIndex), binding.callOnAllChangesDone, binding.callOnChange, changeDetection));
    }
    return MapWrapper.get(this._directiveRecordsMap, id);
  }
}
Object.defineProperty(BindingRecordsCreator.prototype.getBindingRecords, "parameters", {get: function() {
    return [[assert.genericType(List, renderApi.ElementBinder)], [assert.genericType(List, SortedDirectives)]];
  }});
Object.defineProperty(BindingRecordsCreator.prototype.getDirectiveRecords, "parameters", {get: function() {
    return [[assert.genericType(List, SortedDirectives)]];
  }});
Object.defineProperty(BindingRecordsCreator.prototype._createTextNodeRecords, "parameters", {get: function() {
    return [[renderApi.ElementBinder]];
  }});
Object.defineProperty(BindingRecordsCreator.prototype._createElementPropertyRecords, "parameters", {get: function() {
    return [[assert.type.number], [renderApi.ElementBinder]];
  }});
Object.defineProperty(BindingRecordsCreator.prototype._createDirectiveRecords, "parameters", {get: function() {
    return [[assert.type.number], [SortedDirectives]];
  }});
Object.defineProperty(BindingRecordsCreator.prototype._getDirectiveRecord, "parameters", {get: function() {
    return [[assert.type.number], [assert.type.number], [DirectiveBinding]];
  }});
export class ProtoViewFactory {
  constructor(changeDetection) {
    this._changeDetection = changeDetection;
  }
  createProtoView(parentProtoView, componentBinding, renderProtoView, directives) {
    var elementBinders = renderProtoView.elementBinders;
    var sortedDirectives = ListWrapper.map(elementBinders, (b) => new SortedDirectives(b.directives, directives));
    var variableBindings = this._createVariableBindings(renderProtoView);
    var protoLocals = this._createProtoLocals(renderProtoView);
    var variableNames = this._createVariableNames(parentProtoView, protoLocals);
    var protoChangeDetector = this._createProtoChangeDetector(elementBinders, sortedDirectives, componentBinding, variableNames);
    var protoView = new AppProtoView(renderProtoView.render, protoChangeDetector, variableBindings, protoLocals, variableNames);
    this._createElementBinders(protoView, elementBinders, sortedDirectives);
    this._bindDirectiveEvents(protoView, sortedDirectives);
    return protoView;
  }
  _createProtoLocals(renderProtoView) {
    var protoLocals = MapWrapper.create();
    MapWrapper.forEach(renderProtoView.variableBindings, (mappedName, varName) => {
      MapWrapper.set(protoLocals, mappedName, null);
    });
    return protoLocals;
  }
  _createVariableBindings(renderProtoView) {
    var variableBindings = MapWrapper.create();
    MapWrapper.forEach(renderProtoView.variableBindings, (mappedName, varName) => {
      MapWrapper.set(variableBindings, varName, mappedName);
    });
    return variableBindings;
  }
  _createVariableNames(parentProtoView, protoLocals) {
    var variableNames = isPresent(parentProtoView) ? ListWrapper.clone(parentProtoView.variableNames) : [];
    MapWrapper.forEach(protoLocals, (v, local) => {
      ListWrapper.push(variableNames, local);
    });
    return variableNames;
  }
  _createProtoChangeDetector(elementBinders, sortedDirectives, componentBinding, variableNames) {
    var bindingRecordsCreator = new BindingRecordsCreator();
    var bindingRecords = bindingRecordsCreator.getBindingRecords(elementBinders, sortedDirectives);
    var directiveRecords = bindingRecordsCreator.getDirectiveRecords(sortedDirectives);
    var changeDetection = null;
    var name = 'root';
    if (isPresent(componentBinding)) {
      var componentAnnotation = componentBinding.annotation;
      changeDetection = componentAnnotation.changeDetection;
      name = 'dummy';
    }
    return this._changeDetection.createProtoChangeDetector(name, bindingRecords, variableNames, directiveRecords, changeDetection);
  }
  _createElementBinders(protoView, elementBinders, sortedDirectives) {
    for (var i = 0; i < elementBinders.length; i++) {
      var renderElementBinder = elementBinders[i];
      var dirs = sortedDirectives[i];
      var parentPeiWithDistance = this._findParentProtoElementInjectorWithDistance(i, protoView.elementBinders, elementBinders);
      var protoElementInjector = this._createProtoElementInjector(i, parentPeiWithDistance, dirs, renderElementBinder);
      this._createElementBinder(protoView, i, renderElementBinder, protoElementInjector, dirs);
    }
  }
  _findParentProtoElementInjectorWithDistance(binderIndex, elementBinders, renderElementBinders) {
    var distance = 0;
    do {
      var renderElementBinder = renderElementBinders[binderIndex];
      binderIndex = renderElementBinder.parentIndex;
      if (binderIndex !== -1) {
        distance += renderElementBinder.distanceToParent;
        var elementBinder = elementBinders[binderIndex];
        if (isPresent(elementBinder.protoElementInjector)) {
          return new ParentProtoElementInjectorWithDistance(elementBinder.protoElementInjector, distance);
        }
      }
    } while (binderIndex !== -1);
    return new ParentProtoElementInjectorWithDistance(null, -1);
  }
  _createProtoElementInjector(binderIndex, parentPeiWithDistance, sortedDirectives, renderElementBinder) {
    var protoElementInjector = null;
    var hasVariables = MapWrapper.size(renderElementBinder.variableBindings) > 0;
    if (sortedDirectives.directives.length > 0 || hasVariables) {
      protoElementInjector = new ProtoElementInjector(parentPeiWithDistance.protoElementInjector, binderIndex, sortedDirectives.directives, isPresent(sortedDirectives.componentDirective), parentPeiWithDistance.distance);
      protoElementInjector.attributes = renderElementBinder.readAttributes;
      if (hasVariables) {
        protoElementInjector.exportComponent = isPresent(sortedDirectives.componentDirective);
        protoElementInjector.exportElement = isBlank(sortedDirectives.componentDirective);
        var exportImplicitName = MapWrapper.get(renderElementBinder.variableBindings, '\$implicit');
        if (isPresent(exportImplicitName)) {
          protoElementInjector.exportImplicitName = exportImplicitName;
        }
      }
    }
    return protoElementInjector;
  }
  _createElementBinder(protoView, boundElementIndex, renderElementBinder, protoElementInjector, sortedDirectives) {
    var parent = null;
    if (renderElementBinder.parentIndex !== -1) {
      parent = protoView.elementBinders[renderElementBinder.parentIndex];
    }
    var elBinder = protoView.bindElement(parent, renderElementBinder.distanceToParent, protoElementInjector, sortedDirectives.componentDirective);
    protoView.bindEvent(renderElementBinder.eventBindings, boundElementIndex, -1);
    MapWrapper.forEach(renderElementBinder.variableBindings, (mappedName, varName) => {
      MapWrapper.set(protoView.protoLocals, mappedName, null);
    });
    return elBinder;
  }
  _bindDirectiveEvents(protoView, sortedDirectives) {
    for (var boundElementIndex = 0; boundElementIndex < sortedDirectives.length; ++boundElementIndex) {
      var dirs = sortedDirectives[boundElementIndex].renderDirectives;
      for (var i = 0; i < dirs.length; i++) {
        var directiveBinder = dirs[i];
        protoView.bindEvent(directiveBinder.eventBindings, boundElementIndex, i);
      }
    }
  }
}
Object.defineProperty(ProtoViewFactory, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(ProtoViewFactory, "parameters", {get: function() {
    return [[ChangeDetection]];
  }});
Object.defineProperty(ProtoViewFactory.prototype.createProtoView, "parameters", {get: function() {
    return [[AppProtoView], [DirectiveBinding], [renderApi.ProtoViewDto], [assert.genericType(List, DirectiveBinding)]];
  }});
Object.defineProperty(ProtoViewFactory.prototype._bindDirectiveEvents, "parameters", {get: function() {
    return [[], [assert.genericType(List, SortedDirectives)]];
  }});
class SortedDirectives {
  constructor(renderDirectives, allDirectives) {
    this.renderDirectives = [];
    this.directives = [];
    this.componentDirective = null;
    ListWrapper.forEach(renderDirectives, (renderDirectiveBinder) => {
      var directiveBinding = allDirectives[renderDirectiveBinder.directiveIndex];
      if (directiveBinding.annotation instanceof Component) {
        this.componentDirective = directiveBinding;
        ListWrapper.insert(this.renderDirectives, 0, renderDirectiveBinder);
        ListWrapper.insert(this.directives, 0, directiveBinding);
      } else {
        ListWrapper.push(this.renderDirectives, renderDirectiveBinder);
        ListWrapper.push(this.directives, directiveBinding);
      }
    });
  }
}
class ParentProtoElementInjectorWithDistance {
  constructor(protoElementInjector, distance) {
    this.protoElementInjector = protoElementInjector;
    this.distance = distance;
  }
}
Object.defineProperty(ParentProtoElementInjectorWithDistance, "parameters", {get: function() {
    return [[ProtoElementInjector], [assert.type.number]];
  }});
//# sourceMappingURL=proto_view_factory.js.map

//# sourceMappingURL=./proto_view_factory.map