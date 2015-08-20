/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Attribute, Component, View} from 'angular2/angular2';
import {CwCidrInput} from './cidr';

@Component({
  selector: 'demo'
})
@View({
  directives: [CwCidrInput],
  template: `
    <div class="row">
      <div class="col-sm-6">
        <cw-cidr-input ></cw-cidr-input>
      </div>
    </div>
  `
})
class App  {

  constructor( @Attribute('id') id:string) {
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
