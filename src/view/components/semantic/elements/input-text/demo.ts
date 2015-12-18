import {bootstrap, Attribute, Component, View, CORE_DIRECTIVES, FORM_DIRECTIVES} from 'angular2/angular2'
import {InputText} from './input-text'

export class Hero {

  public id:number
  public name:string
  public inputname:string
  public power:string
  public alterEgo:string

  constructor(id:number, name:string, inputname:string, power:string, alterEgo?:string) {
    this.id = id;
    this.name = name;
    this.inputname = inputname;
    this.power = power;
    this.alterEgo = alterEgo;
    console.log("New Hero", this.inputname,' ==> ', inputname)
  }

}

@Component({
  selector: 'hero-form',
  directives: [InputText, CORE_DIRECTIVES, FORM_DIRECTIVES],
  styles: [`
  input.ng-valid[required], input.ng-valid[required] {
  border-left: 5px solid #42A948; /* green */
}
.ng-invalid {
  border-left: 5px solid #a94442; /* red */
}`],
  template: `<div flex layout="row" layout-align="center-center">
  <div flex="40" layout="row" layout-wrap layout-align="center-center">
    <form (ngSubmit)="onSubmit()" #hf="ngForm">
      <div flex="100" layout="row">{{diagnostic}}</div>
      <div flex="100" layout-wrap layout="row" class="ui attached segment">
        <label flex="100" for="name">Name</label>
        <input flex="100" type="text" required [(ngModel)]="model.name" ngControl="name" #name="ngForm" #spy>
        <div flex="50" [hidden]="hf.form.valid" class="name red basic label">Name is required</div>
      </div>
      <div flex="100" layout-wrap layout="row" class="ui attached segment">
        <label flex="100" for="name">Text-Input Name</label>
        <cw-input-text flex="100" [name]="model.inputname.name" required [(ngModel)]="model.inputname" ngControl="inputname" #inputname="ngForm"></cw-input-text>
        <div flex="50" [hidden]="inputname.valid" [class.ui]="!inputname.valid" class="inputname red basic label">Input-Name is
          required
        </div>
      </div>
      <div flex="100" layout-wrap layout="row" class="ui attached segment">
        <label flex="100" for="alterEgo">Alter Ego</label>
        <input flex="100" type="text" class="ui  icon input" [(ngModel)]="model.alterEgo" ngControl="alter-ego">
      </div>
      <div flex="100" layout="row" layout-wrap class="ui attached segment">
        <label flex="100" for="power">Hero Power</label>
        <select flex="100" class="ui icon input" required [(ngModel)]="model.power" ngControl="power" #power="ngForm">
          <option *ngFor="#p of powers" [value]="p">{{p}}</option>
        </select>
        <div flex="100" [hidden]="power.valid" [class.ui]="!power.valid" class="red basic label"> Power is required</div>
      </div>
      <button type="submit" class="btn btn-default" [disabled]="!hf.form.valid">Submit</button>
    </form>
  </div>
</div>
  `
})
export class HeroFormComponent {
  powers = ['Really Smart', 'Super Flexible',
    'Super Hot', 'Weather Changer'];
  model = new Hero(18, 'Dr IQ', "def",  this.powers[0], 'Chuck Overstreet');
  submitted = false;

  onSubmit() {
    this.submitted = true;
  }

  // TODO: Remove this when we're done
  get diagnostic() {
    return JSON.stringify(this.model);
  }
}


class InputTextModel {
  name:string
  value:string
  placeholder:string
  disabled:boolean
  icon:string

  constructor(name:string = null,
              placeholder:string = '',
              disabled:boolean = null,
              icon:string = '') {
    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.disabled = disabled
    this.icon = icon || ''
    if(this.icon.indexOf(' ') == -1 && this.icon.length > 0){
      this.icon = (this.icon + ' icon').trim()
    }
  }

}


@Component({
  selector: 'demo'
})
@View({
  directives: [InputText, HeroFormComponent],
  template: `
  <div style="margin-top:5em;margin-bottom:5em">
    <hero-form></hero-form>
  </div>
  <div flex layout="row" layout-wrap>
  <div flex="33">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-text></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Value</h4>
    <div class="ui attached segment">
      <cw-input-text [value]="demoValue.value"></cw-input-text>
    </div>
  </div>
  <div flex="33">
    <h4 class="ui top attached inverted header">Disabled</h4>
    <div class="ui attached segment">
      <cw-input-text [disabled]="demoDisabled.disabled" [value]="demoDisabled.value"></cw-input-text>
    </div>
  </div>
  <div flex="33">
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
  demoIcon:InputTextModel

  constructor(@Attribute('id') id:string) {
    this.initDemoValue()
    this.initDemoDisabled()
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
    model.disabled = true
    model.placeholder = "Disabled"

    this.demoDisabled = model;
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
