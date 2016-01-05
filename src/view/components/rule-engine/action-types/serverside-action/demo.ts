import {bootstrap} from 'angular2/bootstrap';
import {Component, View, Attribute} from 'angular2/core';
import {ServersideAction} from './serverside-action';

@Component({
  selector: 'cw-request-header-demo'
})
@View({
  directives: [ServersideAction],
  template: `
    <div class="panel panel-default">
      <div class="panel-heading">1) No values set.</div>
      <div class="panel-body">
          <cw-request-header-condition></cw-request-header-condition>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">2) With initial value set for header-key</div>
      <div class="panel-body">
        <cw-request-header-condition header-key-value="User-Agent"></cw-request-header-condition>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">3) With initial value set for header-key and comparator-value</div>
      <div class="panel-body">
        <div class="row">
          <cw-request-header-condition class="col-sm-12"
          [header-key-value]="demo['3'].headerKeyValue"
          [comparator-value]="demo['3'].comparatorValue"
          (change)="updateConditionlet(3, $event)"
          ></cw-request-header-condition>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['3'].headerKeyValue}}</div><div class="col-sm-3">{{demo['3'].comparatorValue}}</div><div class="col-sm-4">{{demo['3'].comparisonValues}}</div>
        </div>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">4) With initial value set for all values.</div>
        <div class="panel-body">
        <div class="row">
          <cw-request-header-condition class="col-sm-12"
          [header-key-value]="demo['4'].headerKeyValue"
          [comparator-value]="demo['4'].comparatorValue"
          [comparison-values]="demo['4'].comparisonValues"
          (change)="updateConditionlet(4, $event)"
          ></cw-request-header-condition>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['4'].headerKeyValue}}</div><div class="col-sm-3">{{demo['4'].comparatorValue}}</div><div class="col-sm-4">{{demo['4'].comparisonValues}}</div>
        </div>
      </div>
    </div>

  `
})
class App {
  demo:any;

  constructor(@Attribute('id') id:string) {

    this.demo = {
      '1': {},
      '2': {},
      '3': {
        headerKeyValue: 'Content-Type',
        comparatorValue: 'startsWith',
        comparisonValues: []
      },
      '4': {
        headerKeyValue: 'Accept-Language',
        comparatorValue: 'contains',
        comparisonValues: ['en', 'enUS']
      }

    };
  }


  updateConditionlet(demoId:string, event:any){
    console.log("Updated: ", demoId, event)
    this.demo[demoId][event.valueField] = event.value[event.valueField];
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
