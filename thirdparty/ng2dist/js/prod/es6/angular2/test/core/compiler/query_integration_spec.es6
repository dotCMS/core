import {AsyncTestCompleter,
  beforeEach,
  ddescribe,
  describe,
  el,
  expect,
  iit,
  inject,
  IS_NODEJS,
  it,
  xit} from 'angular2/test_lib';
import {TestBed} from 'angular2/src/test_lib/test_bed';
import {QueryList} from 'angular2/src/core/compiler/query_list';
import {Query} from 'angular2/src/core/annotations_impl/di';
import {If,
  For} from 'angular2/angular2';
import {Component,
  Directive} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {BrowserDomAdapter} from 'angular2/src/dom/browser_adapter';
export function main() {
  BrowserDomAdapter.makeCurrent();
  describe('Query API', () => {
    it('should contain all directives in the light dom', inject([TestBed, AsyncTestCompleter], (tb, async) => {
      var template = '<div text="1"></div>' + '<needs-query text="2"><div text="3"></div></needs-query>' + '<div text="4"></div>';
      tb.createView(MyComp, {html: template}).then((view) => {
        view.detectChanges();
        expect(view.rootNodes).toHaveText('2|3|');
        async.done();
      });
    }));
    it('should reflect dynamically inserted directives', inject([TestBed, AsyncTestCompleter], (tb, async) => {
      var template = '<div text="1"></div>' + '<needs-query text="2"><div *if="shouldShow" [text]="\'3\'"></div></needs-query>' + '<div text="4"></div>';
      tb.createView(MyComp, {html: template}).then((view) => {
        view.detectChanges();
        expect(view.rootNodes).toHaveText('2|');
        view.context.shouldShow = true;
        view.detectChanges();
        view.detectChanges();
        expect(view.rootNodes).toHaveText('2|3|');
        async.done();
      });
    }));
    it('should reflect moved directives', inject([TestBed, AsyncTestCompleter], (tb, async) => {
      var template = '<div text="1"></div>' + '<needs-query text="2"><div *for="var i of list" [text]="i"></div></needs-query>' + '<div text="4"></div>';
      tb.createView(MyComp, {html: template}).then((view) => {
        view.detectChanges();
        view.detectChanges();
        expect(view.rootNodes).toHaveText('2|1d|2d|3d|');
        view.context.list = ['3d', '2d'];
        view.detectChanges();
        view.detectChanges();
        expect(view.rootNodes).toHaveText('2|3d|2d|');
        async.done();
      });
    }));
  });
}
class NeedsQuery {
  constructor(query) {
    this.query = query;
  }
}
Object.defineProperty(NeedsQuery, "annotations", {get: function() {
    return [new Component({selector: 'needs-query'}), new View({
      directives: [For],
      template: '<div *for="var dir of query">{{dir.text}}|</div>'
    })];
  }});
Object.defineProperty(NeedsQuery, "parameters", {get: function() {
    return [[QueryList, new Query(TextDirective)]];
  }});
var _constructiontext = 0;
class TextDirective {
  constructor() {}
}
Object.defineProperty(TextDirective, "annotations", {get: function() {
    return [new Directive({
      selector: '[text]',
      properties: {'text': 'text'}
    })];
  }});
class MyComp {
  constructor() {
    this.shouldShow = false;
    this.list = ['1d', '2d', '3d'];
  }
}
Object.defineProperty(MyComp, "annotations", {get: function() {
    return [new Component({selector: 'my-comp'}), new View({directives: [NeedsQuery, TextDirective, If, For]})];
  }});
//# sourceMappingURL=query_integration_spec.js.map

//# sourceMappingURL=./query_integration_spec.map