import {isListLikeIterable,
  iterateListLike,
  ListWrapper,
  MapWrapper} from 'angular2/src/facade/collection';
import {int,
  isBlank,
  isPresent,
  stringify,
  getMapKey,
  looseIdentical} from 'angular2/src/facade/lang';
import {WrappedValue,
  Pipe} from './pipe';
export class IterableChangesFactory {
  supports(obj) {
    return IterableChanges.supportsObj(obj);
  }
  create(cdRef) {
    return new IterableChanges();
  }
}
export class IterableChanges extends Pipe {
  constructor() {
    super();
    this._collection = null;
    this._length = null;
    this._linkedRecords = null;
    this._unlinkedRecords = null;
    this._previousItHead = null;
    this._itHead = null;
    this._itTail = null;
    this._additionsHead = null;
    this._additionsTail = null;
    this._movesHead = null;
    this._movesTail = null;
    this._removalsHead = null;
    this._removalsTail = null;
  }
  static supportsObj(obj) {
    return isListLikeIterable(obj);
  }
  supports(obj) {
    return IterableChanges.supportsObj(obj);
  }
  get collection() {
    return this._collection;
  }
  get length() {
    return this._length;
  }
  forEachItem(fn) {
    var record;
    for (record = this._itHead; record !== null; record = record._next) {
      fn(record);
    }
  }
  forEachPreviousItem(fn) {
    var record;
    for (record = this._previousItHead; record !== null; record = record._nextPrevious) {
      fn(record);
    }
  }
  forEachAddedItem(fn) {
    var record;
    for (record = this._additionsHead; record !== null; record = record._nextAdded) {
      fn(record);
    }
  }
  forEachMovedItem(fn) {
    var record;
    for (record = this._movesHead; record !== null; record = record._nextMoved) {
      fn(record);
    }
  }
  forEachRemovedItem(fn) {
    var record;
    for (record = this._removalsHead; record !== null; record = record._nextRemoved) {
      fn(record);
    }
  }
  transform(collection) {
    if (this.check(collection)) {
      return WrappedValue.wrap(this);
    } else {
      return this;
    }
  }
  check(collection) {
    this._reset();
    var record = this._itHead;
    var mayBeDirty = false;
    var index;
    var item;
    if (ListWrapper.isList(collection)) {
      var list = collection;
      this._length = collection.length;
      for (index = 0; index < this._length; index++) {
        item = list[index];
        if (record === null || !looseIdentical(record.item, item)) {
          record = this._mismatch(record, item, index);
          mayBeDirty = true;
        } else if (mayBeDirty) {
          record = this._verifyReinsertion(record, item, index);
        }
        record = record._next;
      }
    } else {
      index = 0;
      iterateListLike(collection, (item) => {
        if (record === null || !looseIdentical(record.item, item)) {
          record = this._mismatch(record, item, index);
          mayBeDirty = true;
        } else if (mayBeDirty) {
          record = this._verifyReinsertion(record, item, index);
        }
        record = record._next;
        index++;
      });
      this._length = index;
    }
    this._truncate(record);
    this._collection = collection;
    return this.isDirty;
  }
  get isDirty() {
    return this._additionsHead !== null || this._movesHead !== null || this._removalsHead !== null;
  }
  _reset() {
    if (this.isDirty) {
      var record;
      var nextRecord;
      for (record = this._previousItHead = this._itHead; record !== null; record = record._next) {
        record._nextPrevious = record._next;
      }
      for (record = this._additionsHead; record !== null; record = record._nextAdded) {
        record.previousIndex = record.currentIndex;
      }
      this._additionsHead = this._additionsTail = null;
      for (record = this._movesHead; record !== null; record = nextRecord) {
        record.previousIndex = record.currentIndex;
        nextRecord = record._nextMoved;
      }
      this._movesHead = this._movesTail = null;
      this._removalsHead = this._removalsTail = null;
    }
  }
  _mismatch(record, item, index) {
    var previousRecord;
    if (record === null) {
      previousRecord = this._itTail;
    } else {
      previousRecord = record._prev;
      this._remove(record);
    }
    record = this._linkedRecords === null ? null : this._linkedRecords.get(item, index);
    if (record !== null) {
      this._moveAfter(record, previousRecord, index);
    } else {
      record = this._unlinkedRecords === null ? null : this._unlinkedRecords.get(item);
      if (record !== null) {
        this._reinsertAfter(record, previousRecord, index);
      } else {
        record = this._addAfter(new CollectionChangeRecord(item), previousRecord, index);
      }
    }
    return record;
  }
  _verifyReinsertion(record, item, index) {
    var reinsertRecord = this._unlinkedRecords === null ? null : this._unlinkedRecords.get(item);
    if (reinsertRecord !== null) {
      record = this._reinsertAfter(reinsertRecord, record._prev, index);
    } else if (record.currentIndex != index) {
      record.currentIndex = index;
      this._addToMoves(record, index);
    }
    return record;
  }
  _truncate(record) {
    while (record !== null) {
      var nextRecord = record._next;
      this._addToRemovals(this._unlink(record));
      record = nextRecord;
    }
    if (this._unlinkedRecords !== null) {
      this._unlinkedRecords.clear();
    }
    if (this._additionsTail !== null) {
      this._additionsTail._nextAdded = null;
    }
    if (this._movesTail !== null) {
      this._movesTail._nextMoved = null;
    }
    if (this._itTail !== null) {
      this._itTail._next = null;
    }
    if (this._removalsTail !== null) {
      this._removalsTail._nextRemoved = null;
    }
  }
  _reinsertAfter(record, prevRecord, index) {
    if (this._unlinkedRecords !== null) {
      this._unlinkedRecords.remove(record);
    }
    var prev = record._prevRemoved;
    var next = record._nextRemoved;
    if (prev === null) {
      this._removalsHead = next;
    } else {
      prev._nextRemoved = next;
    }
    if (next === null) {
      this._removalsTail = prev;
    } else {
      next._prevRemoved = prev;
    }
    this._insertAfter(record, prevRecord, index);
    this._addToMoves(record, index);
    return record;
  }
  _moveAfter(record, prevRecord, index) {
    this._unlink(record);
    this._insertAfter(record, prevRecord, index);
    this._addToMoves(record, index);
    return record;
  }
  _addAfter(record, prevRecord, index) {
    this._insertAfter(record, prevRecord, index);
    if (this._additionsTail === null) {
      this._additionsTail = this._additionsHead = record;
    } else {
      this._additionsTail = this._additionsTail._nextAdded = record;
    }
    return record;
  }
  _insertAfter(record, prevRecord, index) {
    var next = prevRecord === null ? this._itHead : prevRecord._next;
    record._next = next;
    record._prev = prevRecord;
    if (next === null) {
      this._itTail = record;
    } else {
      next._prev = record;
    }
    if (prevRecord === null) {
      this._itHead = record;
    } else {
      prevRecord._next = record;
    }
    if (this._linkedRecords === null) {
      this._linkedRecords = new _DuplicateMap();
    }
    this._linkedRecords.put(record);
    record.currentIndex = index;
    return record;
  }
  _remove(record) {
    return this._addToRemovals(this._unlink(record));
  }
  _unlink(record) {
    if (this._linkedRecords !== null) {
      this._linkedRecords.remove(record);
    }
    var prev = record._prev;
    var next = record._next;
    if (prev === null) {
      this._itHead = next;
    } else {
      prev._next = next;
    }
    if (next === null) {
      this._itTail = prev;
    } else {
      next._prev = prev;
    }
    return record;
  }
  _addToMoves(record, toIndex) {
    if (record.previousIndex === toIndex) {
      return record;
    }
    if (this._movesTail === null) {
      this._movesTail = this._movesHead = record;
    } else {
      this._movesTail = this._movesTail._nextMoved = record;
    }
    return record;
  }
  _addToRemovals(record) {
    if (this._unlinkedRecords === null) {
      this._unlinkedRecords = new _DuplicateMap();
    }
    this._unlinkedRecords.put(record);
    record.currentIndex = null;
    record._nextRemoved = null;
    if (this._removalsTail === null) {
      this._removalsTail = this._removalsHead = record;
      record._prevRemoved = null;
    } else {
      record._prevRemoved = this._removalsTail;
      this._removalsTail = this._removalsTail._nextRemoved = record;
    }
    return record;
  }
  toString() {
    var record;
    var list = [];
    for (record = this._itHead; record !== null; record = record._next) {
      ListWrapper.push(list, record);
    }
    var previous = [];
    for (record = this._previousItHead; record !== null; record = record._nextPrevious) {
      ListWrapper.push(previous, record);
    }
    var additions = [];
    for (record = this._additionsHead; record !== null; record = record._nextAdded) {
      ListWrapper.push(additions, record);
    }
    var moves = [];
    for (record = this._movesHead; record !== null; record = record._nextMoved) {
      ListWrapper.push(moves, record);
    }
    var removals = [];
    for (record = this._removalsHead; record !== null; record = record._nextRemoved) {
      ListWrapper.push(removals, record);
    }
    return "collection: " + list.join(', ') + "\n" + "previous: " + previous.join(', ') + "\n" + "additions: " + additions.join(', ') + "\n" + "moves: " + moves.join(', ') + "\n" + "removals: " + removals.join(', ') + "\n";
  }
}
Object.defineProperty(IterableChanges.prototype.forEachItem, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(IterableChanges.prototype.forEachPreviousItem, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(IterableChanges.prototype.forEachAddedItem, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(IterableChanges.prototype.forEachMovedItem, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(IterableChanges.prototype.forEachRemovedItem, "parameters", {get: function() {
    return [[Function]];
  }});
Object.defineProperty(IterableChanges.prototype._mismatch, "parameters", {get: function() {
    return [[CollectionChangeRecord], [], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._verifyReinsertion, "parameters", {get: function() {
    return [[CollectionChangeRecord], [], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._truncate, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
Object.defineProperty(IterableChanges.prototype._reinsertAfter, "parameters", {get: function() {
    return [[CollectionChangeRecord], [CollectionChangeRecord], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._moveAfter, "parameters", {get: function() {
    return [[CollectionChangeRecord], [CollectionChangeRecord], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._addAfter, "parameters", {get: function() {
    return [[CollectionChangeRecord], [CollectionChangeRecord], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._insertAfter, "parameters", {get: function() {
    return [[CollectionChangeRecord], [CollectionChangeRecord], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._remove, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
Object.defineProperty(IterableChanges.prototype._unlink, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
Object.defineProperty(IterableChanges.prototype._addToMoves, "parameters", {get: function() {
    return [[CollectionChangeRecord], [int]];
  }});
Object.defineProperty(IterableChanges.prototype._addToRemovals, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
export class CollectionChangeRecord {
  constructor(item) {
    this.currentIndex = null;
    this.previousIndex = null;
    this.item = item;
    this._nextPrevious = null;
    this._prev = null;
    this._next = null;
    this._prevDup = null;
    this._nextDup = null;
    this._prevRemoved = null;
    this._nextRemoved = null;
    this._nextAdded = null;
    this._nextMoved = null;
  }
  toString() {
    return this.previousIndex === this.currentIndex ? stringify(this.item) : stringify(this.item) + '[' + stringify(this.previousIndex) + '->' + stringify(this.currentIndex) + ']';
  }
}
class _DuplicateItemRecordList {
  constructor() {
    this._head = null;
    this._tail = null;
  }
  add(record) {
    if (this._head === null) {
      this._head = this._tail = record;
      record._nextDup = null;
      record._prevDup = null;
    } else {
      this._tail._nextDup = record;
      record._prevDup = this._tail;
      record._nextDup = null;
      this._tail = record;
    }
  }
  get(item, afterIndex) {
    var record;
    for (record = this._head; record !== null; record = record._nextDup) {
      if ((afterIndex === null || afterIndex < record.currentIndex) && looseIdentical(record.item, item)) {
        return record;
      }
    }
    return null;
  }
  remove(record) {
    var prev = record._prevDup;
    var next = record._nextDup;
    if (prev === null) {
      this._head = next;
    } else {
      prev._nextDup = next;
    }
    if (next === null) {
      this._tail = prev;
    } else {
      next._prevDup = prev;
    }
    return this._head === null;
  }
}
Object.defineProperty(_DuplicateItemRecordList.prototype.add, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
Object.defineProperty(_DuplicateItemRecordList.prototype.get, "parameters", {get: function() {
    return [[], [int]];
  }});
Object.defineProperty(_DuplicateItemRecordList.prototype.remove, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
class _DuplicateMap {
  constructor() {
    this.map = MapWrapper.create();
  }
  put(record) {
    var key = getMapKey(record.item);
    var duplicates = MapWrapper.get(this.map, key);
    if (!isPresent(duplicates)) {
      duplicates = new _DuplicateItemRecordList();
      MapWrapper.set(this.map, key, duplicates);
    }
    duplicates.add(record);
  }
  get(value, afterIndex = null) {
    var key = getMapKey(value);
    var recordList = MapWrapper.get(this.map, key);
    return isBlank(recordList) ? null : recordList.get(value, afterIndex);
  }
  remove(record) {
    var key = getMapKey(record.item);
    var recordList = MapWrapper.get(this.map, key);
    if (recordList.remove(record)) {
      MapWrapper.delete(this.map, key);
    }
    return record;
  }
  get isEmpty() {
    return MapWrapper.size(this.map) === 0;
  }
  clear() {
    MapWrapper.clear(this.map);
  }
  toString() {
    return '_DuplicateMap(' + stringify(this.map) + ')';
  }
}
Object.defineProperty(_DuplicateMap.prototype.put, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
Object.defineProperty(_DuplicateMap.prototype.remove, "parameters", {get: function() {
    return [[CollectionChangeRecord]];
  }});
//# sourceMappingURL=iterable_changes.js.map

//# sourceMappingURL=./iterable_changes.map