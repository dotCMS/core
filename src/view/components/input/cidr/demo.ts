/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import {bootstrap, Attribute, Component, View} from 'angular2/angular2';
import {CwCidrInput} from './cidr';

@Component({
  selector: 'demo'
})
@View({
  directives: [CwCidrInput],
  template: `
    <style>
      .demo-input {
        color: green;
      }

      .demo-input:invalid {
        color: red;
      }
    </style>
    <div class="panel panel-default">
      <div class="panel-heading">Empty</div>
      <div class="panel-body">
        <cw-cidr-input></cw-cidr-input>
      </div>
    </div>
    <div class="panel panel-default">
      <div class="panel-heading">With initial value</div>
      <div class="panel-body">
          <!-- Using string concatenation to show that value is evaluated.  -->
          <cw-cidr-input [value]="'192' + '.168.1.0/24'"></cw-cidr-input>
      </div>
      <div class="row">
        <div class="col-sm-2"> <input class="demo-input" type="url"/> </div>
      </div>
    </div>
  `
})
class App {

  constructor(@Attribute('id') id:string) {
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
