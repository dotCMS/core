import {Component, Optional} from '@angular/core'
import { FormBuilder, NgControl, FormControl, Validators, FormGroup } from '@angular/forms'
import {InputText} from "./input-text";

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
  directives: [InputText, FormGroup],
  template: `<div flex layout="row" layout-align="center center">
  <div flex="40" layout="row" layout-wrap layout-align="center center">
    <form (ngSubmit)="onSubmit()" [ngFormModel]="model" #hf="ngForm">
      <div flex="100" layout="row">{{diagnostic}}</div>
      <div flex="100" layout-wrap layout="row" class="ui attached segment">
        <label flex="100" for="name">Name</label>
        <cw-input-text flex="100" required  ngControl="name" #fName="ngForm"> </cw-input-text>
        <div flex="50" [hidden]="fName.valid" class="name red basic label">Name is required</div>
      </div>
      <div flex="100" layout-wrap layout="row" class="ui attached segment">
        <label flex="100" for="alterEgo">Alter Ego</label>
        <cw-input-text flex="100" class="ui  icon input" ngControl="alterEgo"> </cw-input-text>
      </div>
      <div flex="100" layout="row" layout-wrap class="ui attached segment">
        <label flex="100" for="power">Hero Power</label>
        <select flex="100" class="ui icon input" required ngControl="power" #fPower="ngForm">
          <option *ngFor="#p of powers" [value]="p">{{p}}</option>
        </select>
        <div flex="100" [hidden]="fPower.valid" [class.ui]="!fPower.valid" class="red basic label"> Power is required</div>
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
  hero = new Hero(18, 'Dr IQ', "def",  this.powers[0], 'Chuck Overstreet');
  submitted = false;

  model:any



  constructor(fb:FormBuilder) {
    this.model = fb.group({
      name: new FormControl(this.hero.name, Validators.minLength(5)),
      alterEgo: new FormControl(this.hero.alterEgo, Validators.required),
      power: new FormControl(this.hero.power)
    })
  }

  onSubmit() {
    this.submitted = true;
  }

  // TODO: Remove this when we're done
  get diagnostic() {
    return JSON.stringify(this.hero);
  }
}




@Component({
  selector: 'demo',
  directives: [InputText, FormGroup, HeroFormComponent],
  template: `<div [ngFormModel]="model">
  <div style="margin-top:5em;margin-bottom:5em">
    <hero-form></hero-form>
  </div>
  <div flex layout="row" layout-wrap>
    <div flex="33">
      <h4 class="ui top attached inverted header">Value</h4>
      <div class="ui attached segment">
        <cw-input-text ngControl="demoOneCtrl"></cw-input-text>
      </div>
    </div>
  </div>
</div>

  `
})
export class App {

  model:any

  constructor(fb:FormBuilder) {
    this.model = fb.group({
      demoOneCtrl: new FormControl('test', Validators.minLength(5)),
      someDate: new FormControl(new Date().toISOString().split('T')[0], (c) => {
        let v:any;
        try {
          let d = new Date(c.value)
          console.log('The date is ', d)
          if (d < new Date()) {
            v = {hateful: true}
          }
        } catch (e) {
          v = {broken: true}
        }
        return v
      })
    })
  }

}

