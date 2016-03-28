import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../modal-dialog/dialog-component";
import {GoogleMapService, GCircle} from "../../../../api/maps/GoogleMapService";


var mapIdCounter = 1;

@Component({
  selector: 'cw-area-picker-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent],
  template: `<cw-modal-dialog 
                 [headerText]="headerText"
                 [hidden]="hidden"
                 [okEnabled]="selectedBundle != null"
                 (ok)="circleUpdate.emit(circle)"
                 (cancel)="cancel.emit()">
  <div *ngIf="!hidden" class="cw-dialog-body">
    <div id="{{mapId}}" class="g-map" *ngIf="!hidden" > </div>
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
  @Input() headerText:string = ''
  @Input() hidden:boolean = false
  @Input() circle:GCircle = {center: {lat:55, lng: 0}, radius: 50000}

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() circleUpdate:EventEmitter<GCircle> = new EventEmitter(false)

  map:google.maps.Map

  mapId = 'map_' + mapIdCounter++;

  constructor(public mapsService:GoogleMapService) {
    console.log("AreaPickerDialogComponent", "constructor", this.mapId)
  }

  ngOnChanges(change) {
    console.log("AreaPickerDialogComponent", "ngOnChange", change, this.mapId)
    if(!this.hidden && this.map == null){
      this.mapsService.mapsApi$.subscribe((x)=>{
        console.log("AreaPickerDialogComponent", "subscribed", this.mapId, x.ready)
      }, ()=>{}, (  ) => {
        console.log("AreaPickerDialogComponent", "done", this.mapId)
        if(this.mapsService.apiReady){
          this.readyMap()
        }
      })
      this.mapsService.loadApi()
    }
    if(change.hidden && this.hidden && this.map){
      console.log("AreaPickerDialogComponent", "ngOnChanges",
          'hiding map: ',
          this.map.getDiv().getAttribute('id'),
          this.map.getDiv()['style']['height']
      )
      /**
       * 
       * Angular2 has a bug? Google Maps? Chrome? For whatever reason, loading a second map without forcing a reload
       * will cause the first map loaded to always display, despite the maps actually living in separate
       * divs, and the 'hidden' map divs actually not being in the active DOM (they have been cut out / moved into the
       * shadow dom by the ngIf). 
       */
      this.map = null 
    }
    if(change.hidden && !this.hidden && this.map){
      console.log("AreaPickerDialogComponent", "ngOnChanges", 'showing map: ', this.map.getDiv().getAttribute('id'))
    }
  }

  waitCount:number  = 0
  readyMap() {
    console.log("AreaPickerDialogComponent", "readyMap", this.mapId, this.circle.radius)
    let el = document.getElementById(this.mapId)
    if (!el) {
      console.log("AreaPickerDialogComponent", "readyMap", "waiting....", this.waitCount++)
      window.setTimeout(()=> this.readyMap(), 10)
    } else {
      this.map = new google.maps.Map(el, {
        zoom: 7,
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
        let center = {lat: ll.lat(), lng: ll.lng()}
        this.circle = {center, radius: circle.getRadius()}
      });

      google.maps.event.addListener(circle, 'radius_changed', () => {
        console.log('radius changed', circle.getRadius(), this.circle.radius)
        let ll = circle.getCenter()
        let center = {lat: ll.lat(), lng: ll.lng()}
        this.circle = {center, radius: circle.getRadius()}
        console.log('radius changed to', circle.getRadius(), this.circle.radius)
      })
    }
  }

}

