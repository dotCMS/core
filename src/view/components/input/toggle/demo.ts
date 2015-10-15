/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {bootstrap, Attribute, Component, View} from 'angular2/angular2'
import {InputToggle} from './InputToggle'
//import "jquery/jquery"
//import "/thirdparty/semantic/dist/semantic.min.js"


@Component({
  selector: 'demo'
})
@View({
  directives: [InputToggle],
  template: `
  <div class="ui three column grid">
  <div class="column">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui buttom attached segment">
      <cw-toggle-input></cw-toggle-input>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Value=true</h4>
    <div class="ui buttom attached segment">
      <cw-toggle-input [value]="true"></cw-toggle-input>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Value=false</h4>
    <div class="ui buttom attached segment">
      <cw-toggle-input [value]="false"></cw-toggle-input>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Change the Text</h4>
    <div class="ui buttom attached segment">
      <cw-toggle-input [value]="true" onText="Or" offText="And"></cw-toggle-input>
      <cw-toggle-input [value]="false" onText="Or" offText="And"></cw-toggle-input>
    </div>
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
