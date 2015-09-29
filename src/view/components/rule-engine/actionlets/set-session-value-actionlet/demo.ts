/// <reference path="../../../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Component, View, Attribute} from 'angular2/angular2';
import {SetSessionValueActionlet} from './set-session-value-actionlet';

@Component({
  selector: 'cw-set-session-value-actionlet-demo'
})
@View({
  directives: [SetSessionValueActionlet],
  template: `
    <div class="panel panel-default">
      <div class="panel-heading">1) No values set.</div>
      <div class="panel-body">
          <cw-set-session-value-actionlet></cw-set-session-value-actionlet>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">2) With initial value set for session-header-key</div>
      <div class="panel-body">
        <cw-set-session-value-actionlet session-key-value="Hola"></cw-set-session-value-actionlet>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">3) With initial value set for session-header-key and session-value</div>
      <div class="panel-body">
        <div class="row">
          <cw-set-session-value-actionlet class="col-sm-12"
          [session-key-value]="demo['3'].sessionKeyValue"
          [session-value]="demo['3'].sessionValue"
          (change)="update(3, $event)"
          ></cw-set-session-value-actionlet>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['3'].sessionKeyValue}}</div><div class="col-sm-3">{{demo['3'].sessionValue}}</div>
        </div>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">4) With initial value set for all values.</div>
        <div class="panel-body">
        <div class="row">
          <cw-set-session-value-actionlet class="col-sm-12" 
          [session-key-value]="demo['4'].sessionKeyValue"
          [session-value]="demo['4'].sessionValue"
          (change)="update(4, $event)"
          ></cw-set-session-value-actionlet>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['4'].sessionKeyValue}}</div><div class="col-sm-3">{{demo['4'].sessionValue}}</div>
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
        sessionKeyValue: 'The most popular number in history',
        sessionValue: '40 + 2'
      },
      '4': {
        sessionKeyValue: 'life, the universe and everything',
        sessionValue: '42'
      }

    };
  }


  update(demoId:string, event:any){
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
