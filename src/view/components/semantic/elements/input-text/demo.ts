import {bootstrap, Attribute, Component, View} from 'angular2/angular2'
import {InputText, InputTextModel} from './input-text'


@Component({
  selector: 'demo'
})
@View({
  directives: [InputText],
  template: `<div class="ui three column grid">
  <div class="column">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-text></cw-input-text>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Value</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoValue"></cw-input-text>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Disabled</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoDisabled"></cw-input-text>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Error</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoError" (change)="customChange($event)"></cw-input-text>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Icon</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoIcon"></cw-input-text>
    </div>
  </div>
</div>
  `
})
class App {
  demoValue:InputTextModel
  demoDisabled:InputTextModel
  demoError:InputTextModel
  demoIcon:InputTextModel

  constructor(@Attribute('id') id:string) {
    this.initDemoValue()
    this.initDemoDisabled()
    this.initDemoError()
    this.initDemoIcon()
  }

  initDemoValue() {
    let model = new InputTextModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.value = "Costa Rica"

    this.demoValue = model;
  }

  initDemoDisabled() {
    let model = new InputTextModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.disabled = "true"
    model.placeholder = "Disabled"

    this.demoDisabled = model;
  }

  initDemoError() {
    let model = new InputTextModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.value = "Required Field"
    model.validate = (newValue:string)=> {
      if(!newValue){ throw new Error("Required Field") }
    }

    this.demoError = model;
  }

  initDemoIcon() {
    let model = new InputTextModel()
    model.name = "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    model.icon = "icon circular search link"
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
