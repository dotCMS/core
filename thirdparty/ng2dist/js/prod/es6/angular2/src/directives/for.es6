import {Directive} from 'angular2/src/core/annotations_impl/annotations';
import {ViewContainerRef} from 'angular2/src/core/compiler/view_container_ref';
import {ViewRef,
  ProtoViewRef} from 'angular2/src/core/compiler/view_ref';
import {isPresent,
  isBlank} from 'angular2/src/facade/lang';
import {ListWrapper} from 'angular2/src/facade/collection';
export class For {
  constructor(viewContainer, protoViewRef) {
    this.viewContainer = viewContainer;
    this.protoViewRef = protoViewRef;
  }
  set iterableChanges(changes) {
    if (isBlank(changes)) {
      this.viewContainer.clear();
      return ;
    }
    var recordViewTuples = [];
    changes.forEachRemovedItem((removedRecord) => ListWrapper.push(recordViewTuples, new RecordViewTuple(removedRecord, null)));
    changes.forEachMovedItem((movedRecord) => ListWrapper.push(recordViewTuples, new RecordViewTuple(movedRecord, null)));
    var insertTuples = For.bulkRemove(recordViewTuples, this.viewContainer);
    changes.forEachAddedItem((addedRecord) => ListWrapper.push(insertTuples, new RecordViewTuple(addedRecord, null)));
    For.bulkInsert(insertTuples, this.viewContainer, this.protoViewRef);
    for (var i = 0; i < insertTuples.length; i++) {
      this.perViewChange(insertTuples[i].view, insertTuples[i].record);
    }
  }
  perViewChange(view, record) {
    view.setLocal('\$implicit', record.item);
    view.setLocal('index', record.currentIndex);
  }
  static bulkRemove(tuples, viewContainer) {
    tuples.sort((a, b) => a.record.previousIndex - b.record.previousIndex);
    var movedTuples = [];
    for (var i = tuples.length - 1; i >= 0; i--) {
      var tuple = tuples[i];
      if (isPresent(tuple.record.currentIndex)) {
        tuple.view = viewContainer.detach(tuple.record.previousIndex);
        ListWrapper.push(movedTuples, tuple);
      } else {
        viewContainer.remove(tuple.record.previousIndex);
      }
    }
    return movedTuples;
  }
  static bulkInsert(tuples, viewContainer, protoViewRef) {
    tuples.sort((a, b) => a.record.currentIndex - b.record.currentIndex);
    for (var i = 0; i < tuples.length; i++) {
      var tuple = tuples[i];
      if (isPresent(tuple.view)) {
        viewContainer.insert(tuple.view, tuple.record.currentIndex);
      } else {
        tuple.view = viewContainer.create(protoViewRef, tuple.record.currentIndex);
      }
    }
    return tuples;
  }
}
Object.defineProperty(For, "annotations", {get: function() {
    return [new Directive({
      selector: '[for][of]',
      properties: {'iterableChanges': 'of | iterableDiff'}
    })];
  }});
Object.defineProperty(For, "parameters", {get: function() {
    return [[ViewContainerRef], [ProtoViewRef]];
  }});
class RecordViewTuple {
  constructor(record, view) {
    this.record = record;
    this.view = view;
  }
}
//# sourceMappingURL=for.js.map

//# sourceMappingURL=./for.map