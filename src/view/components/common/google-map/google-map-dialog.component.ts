import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../modal-dialog/dialog-component";
import {IBundle} from "../../../../api/rule-engine/Rule";
@Component({
  selector: 'cw-google-map-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent],
  template: `<cw-modal-dialog
    [headerText]="'Map!'"
    [hidden]="hidden"
    [okEnabled]="selectedBundle != null"
    (ok)="areaSelect.emit($event)"
    (cancel)="cancel.emit()">
  <div id="map">Loading really, {{apiKey}}, {{hidden}}</div>
  <div *ngIf="apiKey != null">Fooo bar
  <script>
      var map;
      function initMap() {
        map = new google.maps.Map(document.getElementById('map'), {
          center: {lat: -34.397, lng: 150.644},
          zoom: 8
        });
      }
  </script>
  <script src="https://maps.googleapis.com/maps/api/js?key={{apiKey}}&callback=initMap" async defer></script>
</div>
</cw-modal-dialog>`
  , changeDetection: ChangeDetectionStrategy.Default
})
export class GoogleMapDialogComponent {
  @Input() apiKey:string = null
  @Input() hidden:boolean = false

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() areaSelect:EventEmitter<IBundle> = new EventEmitter(false)


  constructor() { }

  ngOnChange(change){
    console.log("GoogleMapDialogComponent", "ngOnChange", change)
    if(change.apiKey && this.apiKey){
      console.log("GoogleMapDialogComponent", "ngOnChange", "Should be live!")
    }

    

  }

}

