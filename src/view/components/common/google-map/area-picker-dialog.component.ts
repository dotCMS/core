import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../modal-dialog/dialog-component";
import {BehaviorSubject} from "rxjs/Rx";


export interface GCircle {
  center:{lat:number, lng:number}
  radius:number
}

window['mapsApi$'] = new BehaviorSubject(null)

window['mapsApiReady'] = function(map, b, c){
  window['mapsApi$'].next(google)
  window['mapsApi$'].complete()

}

@Component({
  selector: 'cw-google-map-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent],
  template: `<cw-modal-dialog 
                 [headerText]="'Map!'"
                 [hidden]="hidden"
                 [okEnabled]="selectedBundle != null"
                 (ok)="circleUpdate.emit(circle)"
                 (cancel)="cancel.emit()">
  <div *ngIf="!hidden" class="cw-dialog-body">
    <div id="map" class="g-map"> </div>
  </div>
</cw-modal-dialog>`,
  styles: [`
  .g-map {
    height:100%;
    width:100%;
  }`]
  , changeDetection: ChangeDetectionStrategy.Default
})
export class AreaPickerDialogComponent {
  @Input() apiKey:string = null
  @Input() hidden:boolean = false
  @Input() circle:GCircle = {center: {lat:55, lng: 0}, radius: 50000}

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() circleUpdate:EventEmitter<GCircle> = new EventEmitter(false)


  mapsApi$:BehaviorSubject<any>
  map:google.maps.Map
  apiReady:boolean = false

  constructor() {
    this.mapsApi$ = window['mapsApi$']
    this.mapsApi$.subscribe((gMapApi)=>{
      if(gMapApi != null){
        this.apiReady = true
        this.readyMap()
      }
    })
  }

  ngOnChanges(change) {
    console.log("AreaPickerDialogComponent", "ngOnChange", change)
    if(!this.hidden && this.apiKey != null && this.map == null){
      this.loadMapsAPI()
    }
  }

  readyMap(){
    this.map = new google.maps.Map(document.getElementById('map'), {
      zoom: 5,
      center: new google.maps.LatLng(this.circle.center.lat, this.circle.center.lng),
      mapTypeId: google.maps.MapTypeId.TERRAIN
    });

    var circle = new google.maps.Circle({
      strokeColor: '#1111FF',
      strokeOpacity: 0.8,
      strokeWeight: 2,
      fillColor: '#1111FF',
      fillOpacity: 0.35,
      map: this.map,
      center: new google.maps.LatLng(this.circle.center.lat, this.circle.center.lng),
      radius: this.circle.radius,
      editable: true
    })

    this.map.addListener('click', (e) => {
      circle.setCenter(e.latLng)
      this.map.panTo(e.latLng)
      let ll = circle.getCenter()
      let center = {lat:ll.lat(), lng:ll.lng()}
      this.circle = {center, radius: circle.getRadius()}
    });

    google.maps.event.addListener(circle, 'radius_changed', () => {
      console.log('radius changed', circle.getRadius(), this.circle.radius)
      let ll = circle.getCenter()
      let center = {lat:ll.lat(), lng:ll.lng()}
      this.circle = {center, radius: circle.getRadius()}
      console.log('radius changed to', circle.getRadius(), this.circle.radius)

    })
  }

  loadMapsAPI() {
    this.addScript(`https://maps.googleapis.com/maps/api/js?key=${this.apiKey}&sensor=false&callback=mapsApiReady`);
  }

  addScript(url, callback?) {
    var script = document.createElement('script');
    if (callback) {
      script.onload = callback;
    }
    script.type = 'text/javascript';
    script.src = url;
    document.body.appendChild(script);
  }
}

