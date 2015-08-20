/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Attribute, Component, View} from 'angular2/angular2';
import {CwCidrInput} from './cidr/cidr';
import {CwIpAddressInputContainer, CwIpAddressInput} from './ip-address/ip-address';

@Component({
  selector: 'demo'
})
@View({
  directives: [CwIpAddressInput, CwCidrInput ],
  template: `
    <div class="row">
      <div class="col-sm-6">
        <span>CIDR Input:</span>
      </div>
      <div class="col-sm-6">
        <cw-cidr-input></cw-cidr-input>
      </div>
    </div>
    <div class="row">
    <div class="col-sm-6">
        <span>IP Address Input:</span>
      </div>
      <div class="col-sm-6">
        <cw-ip-address-input></cw-ip-address-input>
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
