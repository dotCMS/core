import {ListWrapper,
  MapWrapper} from 'angular2/src/facade/collection';
import {isPresent} from 'angular2/src/facade/lang';
import {DirectiveMetadata} from 'angular2/src/render/api';
export function directiveMetadataToMap(meta) {
  return MapWrapper.createFromPairs([['id', meta.id], ['selector', meta.selector], ['compileChildren', meta.compileChildren], ['hostListeners', _cloneIfPresent(meta.hostListeners)], ['hostProperties', _cloneIfPresent(meta.hostProperties)], ['properties', _cloneIfPresent(meta.properties)], ['readAttributes', _cloneIfPresent(meta.readAttributes)], ['type', meta.type], ['version', 1]]);
}
Object.defineProperty(directiveMetadataToMap, "parameters", {get: function() {
    return [[DirectiveMetadata]];
  }});
export function directiveMetadataFromMap(map) {
  return new DirectiveMetadata({
    id: MapWrapper.get(map, 'id'),
    selector: MapWrapper.get(map, 'selector'),
    compileChildren: MapWrapper.get(map, 'compileChildren'),
    hostListeners: _cloneIfPresent(MapWrapper.get(map, 'hostListeners')),
    hostProperties: _cloneIfPresent(MapWrapper.get(map, 'hostProperties')),
    properties: _cloneIfPresent(MapWrapper.get(map, 'properties')),
    readAttributes: _cloneIfPresent(MapWrapper.get(map, 'readAttributes')),
    type: MapWrapper.get(map, 'type')
  });
}
Object.defineProperty(directiveMetadataFromMap, "parameters", {get: function() {
    return [[Map]];
  }});
function _cloneIfPresent(o) {
  if (!isPresent(o))
    return null;
  return ListWrapper.isList(o) ? ListWrapper.clone(o) : MapWrapper.clone(o);
}
//# sourceMappingURL=convert.js.map

//# sourceMappingURL=./convert.map