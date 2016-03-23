import {Attribute, Component} from 'angular2/core'
import {GoogleMapDialogComponent} from "./google-map-dialog.component";

@Component({
  selector: 'demo',
  directives: [GoogleMapDialogComponent],
  template: `
    <cw-google-map-dialog-component [hidden]="!showingMap" [apiKey]="apiKey"></cw-google-map-dialog-component>
    <button class="ui button cw-button-add" aria-label="Show Map" (click)="toggleMap()">
        <i class="plus icon" aria-hidden="true"></i>Show Map
      </button>
  `
})
export class App {

  showingMap:boolean = false
  apiKey:string

  constructor(@Attribute('id') id:string) {
  }


  toggleMap(){
    this.showingMap = !this.showingMap
    this.apiKey = "AIzaSyBqi1S9mgFHW7J-PkAp1hd1VWRKILgkL-8"
    console.log("App", "toggleMap", this.showingMap)
  }

}
