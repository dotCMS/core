/// <reference path="../../../../../thirdparty/angular2/bundles/typings/angular2/angular2.d.ts" />

import {bootstrap, Component, View, Attribute} from 'angular2/angular2';
import {SetSessionValueAction} from './set-session-value-action';

@Component({
  selector: 'cw-set-session-value-action-demo'
})
@View({
  directives: [SetSessionValueAction],
  template: `
    <div class="panel panel-default">
      <div class="panel-heading">1) No values set.</div>
      <div class="panel-body">
          <cw-set-session-value-action></cw-set-session-value-action>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">2) With initial value set for session-header-key</div>
      <div class="panel-body">
        <cw-set-session-value-action session-key="Hola"></cw-set-session-value-action>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">3) With initial value set for session-header-key and session-value</div>
      <div class="panel-body">
        <div class="row">
          <cw-set-session-value-action class="col-sm-12"
          [session-key]="demo['3'].sessionKey"
          [session-value]="demo['3'].sessionValue"
          (change)="update(3, $event)"
          ></cw-set-session-value-action>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['3'].sessionKey}}</div><div class="col-sm-3">{{demo['3'].sessionValue}}</div>
        </div>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">4) With initial value set for all values.</div>
        <div class="panel-body">
        <div class="row">
          <cw-set-session-value-action class="col-sm-12"
          [session-key]="demo['4'].sessionKey"
          [session-value]="demo['4'].sessionValue"
          (change)="update(4, $event)"
          ></cw-set-session-value-action>
        </div>
        <div class="row">
          <div class="col-sm-4">{{demo['4'].sessionKey}}</div><div class="col-sm-3">{{demo['4'].sessionValue}}</div>
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
        sessionKey: 'The most popular number in history',
        sessionValue: '40 + 2'
      },
      '4': {
        sessionKey: 'life, the universe and everything',
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
