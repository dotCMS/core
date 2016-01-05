import {bootstrap} from 'angular2/platform/browser'
import {Attribute, Component, View} from 'angular2/core'
import {Dropdown, InputOption} from './dropdown'
import {Observable} from 'rxjs/Rx'

@Component({
  selector: 'demo'
})
@View({
  directives: [Dropdown, InputOption],
  template: `<div class="ui three column grid">
  <!--<div class="column">-->
    <!--<h4 class="ui top attached inverted header">Default</h4>-->
    <!--<div class="ui attached segment">-->
      <!--<cw-input-dropdown></cw-input-dropdown>-->
    <!--</div>-->
  <!--</div>-->
  <div class="column">
    <h4 class="ui top attached inverted header">Simple select, no default selection.</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [value]="demo2.value" placeholder="{{demo2.placeholder | async}}">
        <cw-input-option *ngFor="#opt of demo2.options" [value]="opt.value" [label]="opt.label | async" [icon]="opt.icon"></cw-input-option>
      </cw-input-dropdown>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Simple select, default selection of 'Male'.</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [value]="demo3.value" placeholder="{{demo3.placeholder | async}}">
        <cw-input-option *ngFor="#opt of demo3.options" [value]="opt.value" [label]="opt.label | async" [icon]="opt.icon"></cw-input-option>
      </cw-input-dropdown>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Default selection with delayed label update</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [value]="demo4.value" placeholder="{{demo4.placeholder | async}}">
        <cw-input-option *ngFor="#opt of demo4.options" [value]="opt.value" [label]="opt.label | async" [icon]="opt.icon"></cw-input-option>
      </cw-input-dropdown>
    </div>
  </div>
  <!--<div class="column">-->
    <!--<h4 class="ui top attached inverted header">Change the Text</h4>-->
    <!--<div class="ui attached segment">-->
      <!--<cw-input-dropdown [model]="demo4.model" (change)="demo4OnChange($event)"></cw-input-dropdown>-->
      <!--<div>Selected Ids: <em>{{demo4.selected}}</em></div>-->
    <!--</div>-->
  <!--</div>-->
  <!--<div class="column">-->
    <!--<h4 class="ui top attached inverted header">Notify on change</h4>-->
    <!--<div class="ui buttom attached segment">-->
      <!--&lt;!&ndash;<cw-input-dropdown [value]="changeDemoValue" (change)="changeDemoValue = $event"></cw-input-dropdown>&ndash;&gt;-->
      <!--&lt;!&ndash;<span> The value is: {{changeDemoValue}}</span>&ndash;&gt;-->
    <!--</div>-->
  <!--</div>-->
</div>
  `
})
export class App {
  demo2:any
  demo3:any
  demo4:any

  constructor(@Attribute('id') id:string) {
    this.initDemo2()
    this.initDemo3()
    this.initDemo4()
  }

  initDemo2() {
    this.demo2 = {
      name: "field-" + new Date().getTime() + Math.floor(Math.random() * 1000),
      placeholder: Observable.of("Gender"),
      value: null,
      options: [
        //{valueId: '', value: '', label:  Observable.of('Gender')},
        {value: 'M', label:  Observable.of('Male'), icon: 'male icon' },
        {value: 'F', label:  this.delayedValue('Female', 2000), icon: 'female icon' }
      ]
    }
  }

  initDemo3() {
    this.demo3 = {
      name: "field-" + new Date().getTime() + Math.floor(Math.random() * 1000),
      placeholder: Observable.of("Gender"),
      value: null,
      options: [
        //{valueId: '', value: '', label:  Observable.of('Gender')},
        {value: 'M', label:  Observable.of('Male'), icon: 'male icon' },
        {value: 'F', label:  this.delayedValue('Female', 2000), icon: 'female icon' }
      ]
    }

    this.delayedValue('M', 2000).subscribe((v)=>{
      console.log("Retrieved value for demo3:", v)
      this.demo3.value = v
    })

  }

  initDemo4() {
    this.demo4 = {
      name: "field-" + new Date().getTime() + Math.floor(Math.random() * 1000),
      placeholder: Observable.of("Select a make"),
      value: null,
      options: [
        //{valueId: '', value: '', label:  Observable.of('Gender')},
        {value: 'BMW', label:  this.delayedValue('BMW', 5000), icon: 'car icon' },
        {value: 'Ford', label:  Observable.of('Ford'), icon: 'car icon' },
        {value: 'GMC', label:  Observable.of('General Motors'), icon: 'car icon' },
        {value: 'Toyota', label:  Observable.of('Toyota'), icon: 'car icon' },
      ]
    }

    this.delayedValue('BMW', 3000).subscribe((v)=>{
      console.log("Retrieved value for demo3:", v)
      this.demo4.value = v
    })

  }


  delayedValue(value:string, delay:number){
    return Observable.timer(delay).map(x => value)
  }

  //initDemo3() {
  //  this.demo3 = new DropdownModel(null,Observable.of("Color"), ["Y"], [
  //    new DropdownOption('R', {x: 'red'}, Observable.of('Red'), 'asterisk'),
  //    new DropdownOption('Y', 'yellow', Observable.of('Yellow'), 'certificate'),
  //    new DropdownOption('G', 92, Observable.of('Green'), 'circle'),
  //    new DropdownOption('B', 'blue', Observable.of('Blue'), 'square'),
  //    new DropdownOption('P', 'purple', Observable.of('Purple'), 'cube')], false, 0, 2);
  //}
  //
  //initDemo4() {
  //  let model = new DropdownModel(null, Observable.of("Color"), [], [
  //    new DropdownOption('R', 'red', Observable.of('Red'), 'asterisk'),
  //    new DropdownOption('Y', 'yellow', Observable.of('Yellow'), 'certificate'),
  //    new DropdownOption('G', 'green', Observable.of('Green'), 'circle'),
  //    new DropdownOption('B', 'blue', Observable.of('Blue'), 'square'),
  //    new DropdownOption('P', 'purple', Observable.of('Purple'), 'cube')], false, 0, 4)
  //
  //
  //  this.demo4 = {
  //    model: model,
  //    selected: []
  //  };
  //}

  demo4OnChange(event) {
    let dd = event.target.model
    console.log(dd)
  }

}
