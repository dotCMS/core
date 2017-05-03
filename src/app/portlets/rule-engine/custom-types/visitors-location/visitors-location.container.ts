import {Component, Input, Output, EventEmitter, ChangeDetectionStrategy} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import {FormControl} from '@angular/forms';
import {ServerSideFieldModel} from '../../../../api/rule-engine/ServerSideFieldModel';
import {Observable} from 'rxjs/Observable';
import {I18nService} from '../../../../api/system/locale/I18n';
import {BehaviorSubject} from 'rxjs/Rx';
import {GCircle} from '../../../../api/maps/GoogleMapService';

interface Param<T> {
  key: string;
  priority?: number;
  value: T;
}

interface VisitorsLocationParams {
  comparison: Param<string>;
  latitude: Param<string>;
  longitude: Param<string>;
  radius: Param<string>;
  preferredDisplayUnits: Param<string>;
}

// tslint:disable-next-line:no-unused-variable
const UNITS = {
  km: {
    toKm: ((len) => len),
    toMeters: ((len) => len * 1000),
    toMiles: ((len) => len / 1.60934 ),
  },
  m: {
    toKm: ((len) => len / 1000),
    toMeters: ((len) => len ),
    toMiles: ((len) => len / 1609.34 ),
  },
  mi: {
    toKm: ((len) => len / 1.60934),
    toMeters: ((len) => len * 1609.34),
    toMiles: ((len) => len  ),
  },
};
const I8N_BASE = 'api.sites.ruleengine';
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DecimalPipe],
  selector: 'cw-visitors-location-container',
  template: `<cw-visitors-location-component 
    [circle]="circle$ | async"
    [preferredUnit]="preferredUnit"
    [comparisonValue]="comparisonValue"
    [comparisonControl]="comparisonControl"
    [comparisonOptions]="comparisonOptions"
    [fromLabel]="fromLabel"
    (comparisonChange)="onComparisonChange($event)"
    (areaChange)="onUpdate($event)"
></cw-visitors-location-component>
`
})
export class VisitorsLocationContainer {
  @Input() componentInstance: ServerSideFieldModel;

  @Output() parameterValuesChange: EventEmitter<{name: string, value: string}[]> = new EventEmitter(false);

  circle$: BehaviorSubject<GCircle> = new BehaviorSubject({center: {lat: 38.89, lng: -77.04}, radius: 10000});
  apiKey: string;
  preferredUnit = 'm';

  lat = 0;
  lng = 0;
  radius = 50000;
  comparisonValue = 'within';
  comparisonControl: FormControl;
  comparisonOptions: {value: string, label: Observable<string>, icon: string }[];
  fromLabel = 'of';

  private _rsrcCache: {[key: string]: Observable<string>};

  constructor(public resources: I18nService, public decimalPipe: DecimalPipe) {
    resources.get(I8N_BASE).subscribe((rsrc) => { });
    this._rsrcCache = {};

    this.circle$.subscribe((e) => {
        }, (e) => {
          console.error('VisitorsLocationContainer', 'Error updating area', e);
        }, () => { }
    );
  }

  rsrc(subkey: string): Observable<string> {
    let x = this._rsrcCache[subkey];
    if (!x) {
      x = this.resources.get(subkey);
      this._rsrcCache[subkey] = x;
    }
    return x;
  }

  ngOnChanges(change): void {
    if (change.componentInstance && this.componentInstance != null) {
      let temp: any = this.componentInstance.parameters;
      let params: VisitorsLocationParams = temp as VisitorsLocationParams;
      let comparisonDef = this.componentInstance.parameterDefs['comparison'];

      let opts = comparisonDef.inputType['options'];
      let i18nBaseKey = comparisonDef.i18nBaseKey || this.componentInstance.type.i18nKey;
      let rsrcKey = i18nBaseKey + '.inputs.comparison.';
      let optsAry = Object.keys(opts).map((key) => {
        let sOpt = opts[key];
        return {value: sOpt.value, label: this.rsrc(rsrcKey + sOpt.i18nKey), icon: sOpt.icon };
      });

      this.comparisonValue = params.comparison.value || comparisonDef.defaultValue;
      this.comparisonOptions = optsAry;
      this.comparisonControl = ServerSideFieldModel.createNgControl(this.componentInstance, 'comparison');

      this.lat = parseFloat(params.latitude.value) || this.lat;
      this.lng = parseFloat(params.longitude.value) || this.lng;
      this.radius = parseFloat(params.radius.value) || 50000;
      this.preferredUnit = params.preferredDisplayUnits.value || this.componentInstance.parameterDefs['preferredDisplayUnits'].defaultValue;

      this.circle$.next({ center: {lat: this.lat, lng: this.lng}, radius: this.radius});
    }
  }

  onComparisonChange(value: string): void {
    this.parameterValuesChange.emit([{name: 'comparison', value}]);
  }

  onUpdate(circle: GCircle): void {
    console.log('App', 'onUpdate', circle);
    this.parameterValuesChange.emit([
      {name: 'latitude', value: circle.center.lat + ''},
      {name: 'longitude', value: circle.center.lng + ''},
      {name: 'radius', value: circle.radius + ''}]);

    this.lat = circle.center.lat;
    this.lng = circle.center.lng;
    this.radius = circle.radius;
    this.circle$.next(circle);
  }
}