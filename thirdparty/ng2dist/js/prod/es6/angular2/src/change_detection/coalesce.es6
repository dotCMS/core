import {isPresent} from 'angular2/src/facade/lang';
import {List,
  ListWrapper,
  Map,
  MapWrapper} from 'angular2/src/facade/collection';
import {RECORD_TYPE_SELF,
  ProtoRecord} from './proto_record';
export function coalesce(records) {
  var res = ListWrapper.create();
  var indexMap = MapWrapper.create();
  for (var i = 0; i < records.length; ++i) {
    var r = records[i];
    var record = _replaceIndices(r, res.length + 1, indexMap);
    var matchingRecord = _findMatching(record, res);
    if (isPresent(matchingRecord) && record.lastInBinding) {
      ListWrapper.push(res, _selfRecord(record, matchingRecord.selfIndex, res.length + 1));
      MapWrapper.set(indexMap, r.selfIndex, matchingRecord.selfIndex);
    } else if (isPresent(matchingRecord) && !record.lastInBinding) {
      MapWrapper.set(indexMap, r.selfIndex, matchingRecord.selfIndex);
    } else {
      ListWrapper.push(res, record);
      MapWrapper.set(indexMap, r.selfIndex, record.selfIndex);
    }
  }
  return res;
}
Object.defineProperty(coalesce, "parameters", {get: function() {
    return [[assert.genericType(List, ProtoRecord)]];
  }});
function _selfRecord(r, contextIndex, selfIndex) {
  return new ProtoRecord(RECORD_TYPE_SELF, "self", null, [], r.fixedArgs, contextIndex, r.directiveIndex, selfIndex, r.bindingRecord, r.expressionAsString, r.lastInBinding, r.lastInDirective);
}
Object.defineProperty(_selfRecord, "parameters", {get: function() {
    return [[ProtoRecord], [assert.type.number], [assert.type.number]];
  }});
function _findMatching(r, rs) {
  return ListWrapper.find(rs, (rr) => rr.mode === r.mode && rr.funcOrValue === r.funcOrValue && rr.contextIndex === r.contextIndex && ListWrapper.equals(rr.args, r.args));
}
Object.defineProperty(_findMatching, "parameters", {get: function() {
    return [[ProtoRecord], [assert.genericType(List, ProtoRecord)]];
  }});
function _replaceIndices(r, selfIndex, indexMap) {
  var args = ListWrapper.map(r.args, (a) => _map(indexMap, a));
  var contextIndex = _map(indexMap, r.contextIndex);
  return new ProtoRecord(r.mode, r.name, r.funcOrValue, args, r.fixedArgs, contextIndex, r.directiveIndex, selfIndex, r.bindingRecord, r.expressionAsString, r.lastInBinding, r.lastInDirective);
}
Object.defineProperty(_replaceIndices, "parameters", {get: function() {
    return [[ProtoRecord], [assert.type.number], [Map]];
  }});
function _map(indexMap, value) {
  var r = MapWrapper.get(indexMap, value);
  return isPresent(r) ? r : value;
}
Object.defineProperty(_map, "parameters", {get: function() {
    return [[Map], [assert.type.number]];
  }});
//# sourceMappingURL=coalesce.js.map

//# sourceMappingURL=./coalesce.map