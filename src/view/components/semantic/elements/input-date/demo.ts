import {bootstrap} from 'angular2/bootstrap'
import {Attribute, Component, View} from 'angular2/core'
import {CORE_DIRECTIVES, FORM_DIRECTIVES} from 'angular2/common'

import {InputDate, InputDateModel} from './input-date'

@Component({
  selector: 'demo'
})
@View({
  directives: [InputDate],
  template: `<div class="ui three column grid">
  <div class="column">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-date></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">datetime-local</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoValue"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Disabled</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoDisabled"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Required and bigger than 5</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoError" (change)="customChange($event)"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Icon</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoIcon"></cw-input-date>
    </div>
  </div>
</div>
  `
})
class App {
  demoValue:InputDateModel
  demoDisabled:InputDateModel
  demoError:InputDateModel
  demoIcon:InputDateModel

  constructor(@Attribute('id') id:string) {
    this.initDemoValue()
    this.initDemoDisabled()
    this.initDemoError()
    this.initDemoIcon()
  }

  initDemoValue() {
    let model = new InputDateModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.value = "Costa Rica"
    model.type = 'datetime-local'

    this.demoValue = model;
  }

  initDemoDisabled() {
    let model = new InputDateModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.disabled = "true"
    model.placeholder = "Disabled"

    this.demoDisabled = model;
  }

  initDemoError() {
    let model = new InputDateModel()
    model.type = 'time'
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.value = "Required Field"
    model.validate = (newValue:string)=> {
      var biggerThanFive = /^([5-9]|0[5-9]|1[0-9]|2[0-3]):[0-5][0-9]$/;
      if(!newValue){
        throw new Error("Required Field")
      } else if (!biggerThanFive.test(newValue)) {
        throw new Error("Time should be bigger than 5AM")
      }
    }

    this.demoError = model;
  }

  initDemoIcon() {
    let model = new InputDateModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.icon = "icon circular calendar link"
    model.placeholder = "Icon"

    this.demoIcon = model;
  }

  customChange(event){
    console.log("Value of field: " + event.target.value)
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
