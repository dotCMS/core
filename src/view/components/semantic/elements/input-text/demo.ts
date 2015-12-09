import {bootstrap, Attribute, Component, View, CORE_DIRECTIVES, FORM_DIRECTIVES} from 'angular2/angular2'
import {InputText, InputTextModel} from './input-text'


export class Hero {
  constructor(public id:number,
              public name:string,
              public power:string,
              public alterEgo?:string) {
  }
}


@Component({
  selector: 'hero-form',
  directives: [CORE_DIRECTIVES, FORM_DIRECTIVES],
  styles: [`
  .ng-valid[required] {
  border-left: 5px solid #42A948; /* green */
}
.ng-invalid {
  border-left: 5px solid #a94442; /* red */
}`],
  template: `<div flex layout="row" layout-align="center-center">
  <div flex="40" layout="row" layout-wrap layout-align="center-center">
  <form (ng-submit)="onSubmit()" #hf="form">
    <div flex="100" layout="row">{{diagnostic}}</div>
    <div flex="100" layout-wrap  layout="row" class="ui attached segment">
      <label flex="100" for="name">Name</label>
      <input flex="100" type="text" class="form-control" required [(ng-model)]="model.name" ng-control="name" #name="form" #spy>
      <div flex="100">TODO: remove this: {{spy.className}}</div>
      <div flex="50" [hidden]="name.valid" [class.ui]="!name.valid" class="red basic label">Name is required</div>
    </div>
    <div flex="100" layout-wrap layout="row" class="ui attached segment">
      <label flex="100" for="alterEgo">Alter Ego</label>
      <input flex="100" type="text" class="ui  icon input" [(ng-model)]="model.alterEgo" ng-control="alter-ego">
    </div>
    <div flex="100" layout="row" layout-wrap class="ui attached segment">
      <label flex="100" for="power">Hero Power</label>
      <select flex="100" class="ui icon input" required [(ng-model)]="model.power" ng-control="power" #power="form">
        <option *ng-for="#p of powers" [value]="p">{{p}}</option>
      </select>
      <div flex="100" [hidden]="power.valid" [class.ui]="!power.valid" class="red basic label"> Power is required </div>
    </div>

    <button type="submit" class="btn btn-default" [disabled]="!hf.form.valid">Submit</button>
    </form>
  </div>
</div>
</div>
  `
})
export class HeroFormComponent {
  powers = ['Really Smart', 'Super Flexible',
    'Super Hot', 'Weather Changer'];
  model = new Hero(18, 'Dr IQ', this.powers[0], 'Chuck Overstreet');
  submitted = false;

  onSubmit() {
    this.submitted = true;
  }

  // TODO: Remove this when we're done
  get diagnostic() {
    return JSON.stringify(this.model);
  }
}


@Component({
  selector: 'demo'
})
@View({
  directives: [InputText, HeroFormComponent],
  template: `<div flex layout="row" layout-wrap>
  <div flex="33">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-text></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Value</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoValue"></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Disabled</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoDisabled"></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Error</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoError" (change)="customChange($event)"></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Icon</h4>
    <div class="ui attached segment">
      <cw-input-text [model]="demoIcon"></cw-input-text>
    </div>
  </div>
</div>
<div style="margin-top:5em;">
  <hero-form></hero-form>
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
      if (!newValue) {
        throw new Error("Required Field")
      }
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

  customChange(event) {
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
