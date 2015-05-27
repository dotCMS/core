import { MapWrapper, StringMapWrapper } from 'angular2/src/facade/collection';
export { SetterFn, GetterFn, MethodFn } from './types';
// HACK: workaround for Traceur behavior.
// It expects all transpiled modules to contain this marker.
// TODO: remove this when we no longer use traceur
export var __esModule = true;
export class Reflector {
    constructor(reflectionCapabilities) {
        this._typeInfo = MapWrapper.create();
        this._getters = MapWrapper.create();
        this._setters = MapWrapper.create();
        this._methods = MapWrapper.create();
        this.reflectionCapabilities = reflectionCapabilities;
    }
    registerType(type, typeInfo) {
        MapWrapper.set(this._typeInfo, type, typeInfo);
    }
    registerGetters(getters) {
        _mergeMaps(this._getters, getters);
    }
    registerSetters(setters) {
        _mergeMaps(this._setters, setters);
    }
    registerMethods(methods) {
        _mergeMaps(this._methods, methods);
    }
    factory(type) {
        if (MapWrapper.contains(this._typeInfo, type)) {
            return MapWrapper.get(this._typeInfo, type)["factory"];
        }
        else {
            return this.reflectionCapabilities.factory(type);
        }
    }
    parameters(typeOfFunc) {
        if (MapWrapper.contains(this._typeInfo, typeOfFunc)) {
            return MapWrapper.get(this._typeInfo, typeOfFunc)["parameters"];
        }
        else {
            return this.reflectionCapabilities.parameters(typeOfFunc);
        }
    }
    annotations(typeOfFunc) {
        if (MapWrapper.contains(this._typeInfo, typeOfFunc)) {
            return MapWrapper.get(this._typeInfo, typeOfFunc)["annotations"];
        }
        else {
            return this.reflectionCapabilities.annotations(typeOfFunc);
        }
    }
    getter(name) {
        if (MapWrapper.contains(this._getters, name)) {
            return MapWrapper.get(this._getters, name);
        }
        else {
            return this.reflectionCapabilities.getter(name);
        }
    }
    setter(name) {
        if (MapWrapper.contains(this._setters, name)) {
            return MapWrapper.get(this._setters, name);
        }
        else {
            return this.reflectionCapabilities.setter(name);
        }
    }
    method(name) {
        if (MapWrapper.contains(this._methods, name)) {
            return MapWrapper.get(this._methods, name);
        }
        else {
            return this.reflectionCapabilities.method(name);
        }
    }
}
function _mergeMaps(target, config) {
    StringMapWrapper.forEach(config, (v, k) => MapWrapper.set(target, k, v));
}
//# sourceMappingURL=reflector.js.map