import {MapWrapper} from 'angular2/src/facade/collection';
import {stringify} from 'angular2/src/facade/lang';
export class Key {
  constructor(token, id) {
    this.token = token;
    this.id = id;
  }
  get displayName() {
    return stringify(this.token);
  }
  static get(token) {
    return _globalKeyRegistry.get(token);
  }
  static get numberOfKeys() {
    return _globalKeyRegistry.numberOfKeys;
  }
}
export class KeyRegistry {
  constructor() {
    this._allKeys = MapWrapper.create();
  }
  get(token) {
    if (token instanceof Key)
      return token;
    if (MapWrapper.contains(this._allKeys, token)) {
      return MapWrapper.get(this._allKeys, token);
    }
    var newKey = new Key(token, Key.numberOfKeys);
    MapWrapper.set(this._allKeys, token, newKey);
    return newKey;
  }
  get numberOfKeys() {
    return MapWrapper.size(this._allKeys);
  }
}
var _globalKeyRegistry = new KeyRegistry();
//# sourceMappingURL=key.js.map

//# sourceMappingURL=./key.map