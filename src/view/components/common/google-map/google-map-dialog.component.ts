import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import { ANGULAR2_GOOGLE_MAPS_DIRECTIVES } from 'angular2-google-maps/core';
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../modal-dialog/dialog-component";

// just an interface for type safety.
interface marker {
  lat: number;
  lng: number;
  label?: string;

}


@Component({
  selector: 'cw-google-map-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent, ANGULAR2_GOOGLE_MAPS_DIRECTIVES],
  template: `<cw-modal-dialog
    [headerText]="'Map!'"
    [hidden]="hidden"
    [okEnabled]="selectedBundle != null"
    (ok)="areaSelect.emit($event)"
    (cancel)="cancel.emit()">
  <sebm-google-map class="foo"
      [latitude]="lat" 
      [longitude]="lng"
      [zoom]="zoom">
    
      <sebm-google-map-marker 
        *ngFor="#m of markers; #i = index"
        (markerClick)="clickedMarker(i)"
        [latitude]="m.lat"
        [longitude]="m.lng"
        [label]="m.label"></sebm-google-map-marker>
    
    </sebm-google-map>
</cw-modal-dialog>`,
  styles: [`
  .foo {
    height:300px;
  }`]
  , changeDetection: ChangeDetectionStrategy.Default
})
export class GoogleMapDialogComponent {
  @Input() apiKey:string = null
  @Input() hidden:boolean = false

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() areaSelect:EventEmitter<{center:string, radius:number}> = new EventEmitter(false)

// google maps zoom level
  zoom: number = 8;

  // initial center position for the map
  lat: number = 51.673858;
  lng: number = 7.815982;


  clickedMarker(markerIndex: number) {
    console.log("removing element " + markerIndex);
    this.markers.splice(markerIndex,1);
  }

  markers: marker[] = [
    {
      lat: 51.673858,
      lng: 7.815982,
      label: 'A'
    },
    {
      lat: 51.373858,
      lng: 7.215982,
      label: 'B'
    },
    {
      lat: 51.723858,
      lng: 7.895982,
      label: 'C'
    }
  ]
  constructor() { }

  ngOnChange(change){
    console.log("GoogleMapDialogComponent", "ngOnChange", change)
    if(change.apiKey && this.apiKey){
      console.log("GoogleMapDialogComponent", "ngOnChange", "Should be live!")
    }



  }

}

