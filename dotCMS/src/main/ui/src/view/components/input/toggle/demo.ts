import {bootstrap} from '@angular/bootstrap'
import {Attribute, Component, View} from '@angular/core'
import {InputToggle} from './InputToggle'


@Component({
  selector: 'demo',
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
  <div class="column">
    <h4 class="ui top attached inverted header">Notify on change</h4>
    <div class="ui buttom attached segment">
      <cw-toggle-input [value]="changeDemoValue" (change)="changeDemoValue = $event"></cw-toggle-input>
      <span> The value is: {{changeDemoValue}}</span>
    </div>
  </div>
</div>
  `
})
class App {
  changeDemoValue: boolean
  constructor(@Attribute('id') id:string) {
    this.changeDemoValue = true
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
