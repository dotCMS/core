/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Component, View, Attribute} from 'angular2/angular2';
import {RequestHeaderConditionlet} from './request-header-conditionlet';

@Component({
  selector: 'cw-request-header-conditionlet-demo'
})
@View({
  directives: [RequestHeaderConditionlet],
  template: `
    <div class="panel panel-default">
      <div class="panel-heading">1) No values set.</div>
      <div class="panel-body">
          <cw-request-header-conditionlet></cw-request-header-conditionlet>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">2) With initial value set for header-key</div>
      <div class="panel-body">
        <cw-request-header-conditionlet header-key-value="User-Agent"></cw-request-header-conditionlet>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">3) With initial value set for header-key and comparator-value</div>
      <div class="panel-body">
        <div class="row">
          <cw-request-header-conditionlet class="col-sm-12" 
          [header-key-value]="demo3HeaderKey" 
          [comparator-value]="demo3Comparator"
          (header-key-change)="updateHeaderKey('demo3', $event)"
          (comparator-change)="updateComparator('demo3', $event)"
          (comparison-values-change)="updateComparisonValues('demo3', $event)"
          ></cw-request-header-conditionlet>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo3HeaderKey}}</div><div class="col-sm-3">{{demo3Comparator}}</div><div class="col-sm-4">{{demo3ComparisonValues}}</div>
        </div>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">4) With initial value set for all values.</div>
        <div class="panel-body">
        <div class="row">
          <cw-request-header-conditionlet class="col-sm-12" 
          [header-key-value]="demo4HeaderKey" 
          [comparator-value]="demo4Comparator"
          [comparison-values]="demo4ComparisonValues"
          (change)="logModelChange(demo4, $event)"
          (header-key-change)="updateHeaderKey('demo4', $event)"
          (comparator-change)="updateComparator('demo4', $event)"
          (comparison-values-change)="updateComparisonValues('demo4', $event)"
          ></cw-request-header-conditionlet>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo4HeaderKey}}</div><div class="col-sm-3">{{demo4Comparator}}</div><div class="col-sm-4">{{demo4ComparisonValues}}</div>
        </div>
      </div>
    </div>

  `
})
class App {
  demo3HeaderKey:string = 'Content-Type';
  demo3Comparator:string = 'startsWith';
  demo3ComparisonValues:string;
  demo4HeaderKey:string = 'Accept-Language';
  demo4Comparator:string = 'endsWith';
  demo4ComparisonValues:Array<string> = ['en', 'enUS'];

  constructor(@Attribute('id') id:string) {
  }

  logModelChange(demoId:string, event:Event){
    console.log("Updated: ", demoId, event)
  }

  updateHeaderKey(demoId:string, event:any){
    this[demoId + 'HeaderKey'] = event.isNow
  }

  updateComparator(demoId:string, event:any){
    this[demoId + 'Comparator'] = event.isNow
  }

  updateComparisonValues(demoId:string, event:any){
    this[demoId + 'ComparisonValues'] = event.isNow
  }
}


export function main() {
  let app = bootstrap(App)
  app.then((appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}
