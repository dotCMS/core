import {Component} from 'angular2/core'
import {VisitorsLocationComponent} from "./visitors-location.component";

@Component({
  selector: 'demo',
  directives: [VisitorsLocationComponent],
  template: `
    <cw-visitors-location-component></cw-visitors-location-component>
  `
})
export class App {

  constructor() {
  } 



}
