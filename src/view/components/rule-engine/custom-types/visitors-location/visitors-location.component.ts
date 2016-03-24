import {Component, Input, Output, EventEmitter, ChangeDetectionStrategy} from 'angular2/core';
import {FORM_DIRECTIVES, CORE_DIRECTIVES} from "angular2/common";
import {GCircle, AreaPickerDialogComponent} from "../../../../../view/components/common/google-map/area-picker-dialog.component";
import {ServerSideFieldModel} from "../../../../../api/rule-engine/ServerSideFieldModel";
import {Observable} from "rxjs/Observable";
import {I18nService} from "../../../../../api/system/locale/I18n";
import {Dropdown, InputOption} from "../../../semantic/modules/dropdown/dropdown";

interface Param<T> {
  key:string
  priority?:number
  value:T
}

interface VisitorsLocationParams {
  comparison:Param<string>
  latitude:Param<number>
  longitude:Param<number>
  radius:Param<number>
  radiusUnits:Param<string>
}

const UNITS = {
  MILES: {
    toMeters: ((len) => len * 1609.34),
    fromMeters: ((len) => len / 1609.34),
    toKm: ((len) => len / 1.60934),
    fromKm: ((len) => len / 1.60934)
  },
  KILOMETERS: {
    toMeters: ((len) => len * 1000),
    fromMeters: ((len) => len / 1000),
    toMiles: ((len) => len / 1.60934 ),
    fromMiles: ((len) => len * 1.60934 ),
  }

}
const I8N_BASE:string = 'api.sites.ruleengine'
@Component({
  selector: 'cw-visitors-location-component',
  directives: [FORM_DIRECTIVES, CORE_DIRECTIVES, AreaPickerDialogComponent, Dropdown, InputOption],
  template: `<div flex layout="row" class="cw-condition-component-body" *ngIf="comparisonDropdown != null">
  <cw-input-dropdown flex
                     class="cw-input"
                     [ngFormControl]="comparisonDropdown.control"
                     [required]="true"
                     [class.cw-comparator-selector]="true"
                     (change)="onComparisonChange($event)"
                     placeholder="{{comparisonDropdown.placeholder}}">
    <cw-input-option *ngFor="#opt of comparisonDropdown.options"
        [value]="opt.value"
        [label]="opt.label | async"
        icon="{{opt.icon}}"></cw-input-option>
  </cw-input-dropdown>
  <button class="ui button cw-button-add" aria-label="Show Map" (click)="toggleMap()">
    <i class="plus icon" aria-hidden="true"></i>Show Map
  </button>
</div>
<cw-google-map-dialog-component
    [hidden]="!showingMap"
    [apiKey]="apiKey"
    [circle]="circle"
    (circleUpdate)="onUpdate($event)"
></cw-google-map-dialog-component>
`,  changeDetection: ChangeDetectionStrategy.OnPush,

})
export class VisitorsLocationComponent {
  @Input() componentInstance:ServerSideFieldModel
  @Output() parameterValueChange:EventEmitter<{name:string, value:string}> = new EventEmitter(false)

  comparisonDropdown:any

  showingMap:boolean = false
  apiKey:string
  circle:GCircle = {center: {lat:38.89, lng: -77.04}, radius: 10000}

  private _rsrcCache:{[key:string]:Observable<string>}


  constructor(public resources:I18nService) {
    resources.get(I8N_BASE).subscribe((rsrc)=> { })
    this._rsrcCache = {}

  }

  rsrc(subkey:string) {
    let x = this._rsrcCache[subkey]
    if(!x){
      x = this.resources.get(subkey)
      this._rsrcCache[subkey] = x
    }
    return x
  }

  ngOnChanges(change){
    if(change.componentInstance && this.componentInstance != null){
      let temp:any = this.componentInstance.parameters
      let params:VisitorsLocationParams = temp as VisitorsLocationParams
      let comparisonDef = this.componentInstance.parameterDefs['comparison']

      let opts = comparisonDef.inputType['options']
      let i18nBaseKey = comparisonDef.i18nBaseKey || this.componentInstance.type.i18nKey
      let rsrcKey = i18nBaseKey + '.inputs.comparison.'
      let optsAry = Object.keys(opts).map((key)=> {
        let sOpt = opts[key]
        let ddOpt = {value: sOpt.value, label: this.rsrc(rsrcKey + sOpt.i18nKey), icon:sOpt.icon }
        return ddOpt
      })

      this.comparisonDropdown = {
        name: 'comparison',
        control: ServerSideFieldModel.createNgControl(this.componentInstance, 'comparison'),
        placeholder: '',
        value: params.comparison.value || comparisonDef.defaultValue,
        options: optsAry
      }


      console.log("VisitorsLocationComponent", "ngOnChanges", "dd=>", this.comparisonDropdown)

      let lat = params.latitude.value || this.circle.center.lat
      let lng = params.longitude.value || this.circle.center.lng
      let radius = params.radius.value || 100
      let unit = params.radiusUnits.value || 'MILES'
      radius = UNITS[unit].toMeters(radius)
      this.circle = { center: {lat, lng}, radius}
    }
  }

  onComparisonChange(value:string){
    debugger
  }

  toggleMap(){
    this.showingMap = !this.showingMap
    // this.apiKey = "AIzaSyBqi1S9mgFHW7J-PkAp1hd1VWRKILgkL-8"
    this.apiKey = ""
    console.log("App", "toggleMap", this.showingMap)
  }

  onUpdate(circle:GCircle){
    this.showingMap = false
    console.log("App", "onUpdate", circle)
  }


  delayedValue(value:string, delay:number){
    return Observable.timer(delay).map(x => value)
  }

}


