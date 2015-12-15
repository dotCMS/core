import {bootstrap, Attribute, Component, View} from 'angular2/angular2'
import {Dropdown, DropdownModel, DropdownOption} from './dropdown'


@Component({
  selector: 'demo'
})
@View({
  directives: [Dropdown],
  template: `<div class="ui three column grid">
  <div class="column">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-dropdown></cw-input-dropdown>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Simple select, no default selection.</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [model]="demo2"></cw-input-dropdown>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Select Multiple, 'Yellow' selected by default.</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [model]="demo3" ></cw-input-dropdown>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Change the Text</h4>
    <div class="ui attached segment">
      <cw-input-dropdown [model]="demo4.model" (change)="demo4OnChange($event)"></cw-input-dropdown>
      <div>Selected Ids: <em>{{demo4.selected}}</em></div>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Notify on change</h4>
    <div class="ui buttom attached segment">
      <!--<cw-input-dropdown [value]="changeDemoValue" (change)="changeDemoValue = $event"></cw-input-dropdown>-->
      <!--<span> The value is: {{changeDemoValue}}</span>-->
    </div>
  </div>
</div>
  `
})
class App {
  demo2:DropdownModel
  demo3:DropdownModel
  demo4:any

  constructor(@Attribute('id') id:string) {
    this.initDemo2()
    this.initDemo3()
    this.initDemo4()
  }

  initDemo2() {
    this.demo2 = new DropdownModel(
        "field-" + new Date().getTime() + Math.floor(Math.random() * 1000),
        "Gender",
        [], [
          new DropdownOption('M', 100, 'Male', 'male'),
          new DropdownOption('F', 42, 'Female', 'female')
        ]);
  }

  initDemo3() {
    this.demo3 = new DropdownModel(null, "Color", ["Y"], [
      new DropdownOption('R', {x: 'red'}, 'Red', 'asterisk'),
      new DropdownOption('Y', 'yellow', 'Yellow', 'certificate'),
      new DropdownOption('G', 92, 'Green', 'circle'),
      new DropdownOption('B', 'blue', 'Blue', 'square'),
      new DropdownOption('P', 'purple', 'Purple', 'cube')], false, 0, 2);
  }

  initDemo4() {
    let model = new DropdownModel(null, "Color", [], [
      new DropdownOption('R', 'red', 'Red', 'asterisk'),
      new DropdownOption('Y', 'yellow', 'Yellow', 'certificate'),
      new DropdownOption('G', 'green', 'Green', 'circle'),
      new DropdownOption('B', 'blue', 'Blue', 'square'),
      new DropdownOption('P', 'purple', 'Purple', 'cube')], false, 0, 4)


    this.demo4 = {
      model: model,
      selected: []
    };
  }

  demo4OnChange(event) {
    let dd:DropdownModel = event.target.model
    this.demo4.selected = dd.selected.join(',')
    console.log(dd)
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
