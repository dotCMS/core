/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Attribute, Component, View} from 'angular2/angular2';
import {CwIpAddressInput} from './ip-address';

@Component({
  selector: 'demo'
})
@View({
  directives: [CwIpAddressInput],
  template: `
    <div class="panel panel-default">
      <div class="panel-heading">Empty</div>
      <div class="panel-body">
        <cw-ip-address-input></cw-ip-address-input>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">With initial value</div>
      <div class="panel-body">
          <!-- Using string concatenation to show that value is evaluated.  -->
          <cw-ip-address-input [value]="'192' + '.168.1.0'"></cw-ip-address-input>
      </div>
    </div>
  `
})
class App  {

  constructor( @Attribute('id') id:string) {
  }


  setValue(event){

  }
}



export function main() {
  let app = bootstrap(App)
  app.then( (appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}
