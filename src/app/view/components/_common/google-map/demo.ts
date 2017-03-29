import {Attribute, Component} from '@angular/core';
import {AreaPickerDialogComponent} from './area-picker-dialog.component';
import {GCircle} from '../../../../api/maps/GoogleMapService';

@Component({
  selector: 'demo',
  template: `
    <cw-google-map-dialog-component 
    [hidden]="!showingMap" 
    [apiKey]="apiKey"
    [circle]="circle"
    (circleUpdate)="onUpdate($event)"
    ></cw-google-map-dialog-component>
    <button class="ui button cw-button-add" aria-label="Show Map" (click)="toggleMap()">
        <i class="plus icon" aria-hidden="true"></i>Show Map
      </button>
  `
})
export class App {

  showingMap = false;
  apiKey: string;
  circle: GCircle = {center: {lat: 38.89, lng: -77.04}, radius: 10000};

  constructor(@Attribute('id') id: string) {
  }

  toggleMap(): void {
    this.showingMap = !this.showingMap;
    // this.apiKey = "AIzaSyBqi1S9mgFHW7J-PkAp1hd1VWRKILgkL-8"
    this.apiKey = '';
    console.log('App', 'toggleMap', this.showingMap);
  }

  onUpdate(circle: GCircle): void {
    this.showingMap = false;
    console.log('App', 'onUpdate', circle);
  }

}
