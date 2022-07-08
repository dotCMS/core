import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { UntypedFormControl } from '@angular/forms';
import { ServerSideFieldModel } from '../../services/ServerSideFieldModel';
import { Observable, BehaviorSubject } from 'rxjs';
import { I18nService } from '../.././services/system/locale/I18n';
import { LoggerService } from '@dotcms/dotcms-js';
import { GCircle } from '../../models/gcircle.model';

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
    ></cw-visitors-location-component> `
})
export class VisitorsLocationContainer {
    @Input() componentInstance: ServerSideFieldModel;

    @Output()
    parameterValuesChange: EventEmitter<{ name: string; value: string }[]> = new EventEmitter(
        false
    );

    circle$: BehaviorSubject<GCircle> = new BehaviorSubject({
        center: { lat: 38.89, lng: -77.04 },
        radius: 10000
    });
    apiKey: string;
    preferredUnit = 'm';

    lat = 0;
    lng = 0;
    radius = 50000;
    comparisonValue = 'within';
    comparisonControl: UntypedFormControl;
    comparisonOptions: { value: string; label: Observable<string>; icon: string }[];
    fromLabel = 'of';

    private _rsrcCache: { [key: string]: Observable<string> };

    constructor(
        public resources: I18nService,
        public decimalPipe: DecimalPipe,
        private loggerService: LoggerService
    ) {
        resources.get(I8N_BASE).subscribe((_rsrc) => {});
        this._rsrcCache = {};

        this.circle$.subscribe(
            (_e) => {},
            (e) => {
                loggerService.error('VisitorsLocationContainer', 'Error updating area', e);
            },
            () => {}
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
            const temp: any = this.componentInstance.parameters;
            const params: VisitorsLocationParams = temp as VisitorsLocationParams;
            const comparisonDef = this.componentInstance.parameterDefs['comparison'];

            const opts = comparisonDef.inputType['options'];
            const i18nBaseKey = comparisonDef.i18nBaseKey || this.componentInstance.type.i18nKey;
            const rsrcKey = i18nBaseKey + '.inputs.comparison.';
            const optsAry = Object.keys(opts).map((key) => {
                const sOpt = opts[key];
                return {
                    value: sOpt.value,
                    label: this.rsrc(rsrcKey + sOpt.i18nKey),
                    icon: sOpt.icon
                };
            });

            this.comparisonValue = params.comparison.value || comparisonDef.defaultValue;
            this.comparisonOptions = optsAry;
            this.comparisonControl = ServerSideFieldModel.createNgControl(
                this.componentInstance,
                'comparison'
            );

            this.lat = parseFloat(params.latitude.value) || this.lat;
            this.lng = parseFloat(params.longitude.value) || this.lng;
            this.radius = parseFloat(params.radius.value) || 50000;
            this.preferredUnit =
                params.preferredDisplayUnits.value ||
                this.componentInstance.parameterDefs['preferredDisplayUnits'].defaultValue;

            this.circle$.next({ center: { lat: this.lat, lng: this.lng }, radius: this.radius });
        }
    }

    onComparisonChange(value: string): void {
        this.parameterValuesChange.emit([{ name: 'comparison', value }]);
    }

    onUpdate(circle: GCircle): void {
        this.loggerService.info('App', 'onUpdate', circle);
        this.parameterValuesChange.emit([
            { name: 'latitude', value: circle.center.lat + '' },
            { name: 'longitude', value: circle.center.lng + '' },
            { name: 'radius', value: circle.radius + '' }
        ]);

        this.lat = circle.center.lat;
        this.lng = circle.center.lng;
        this.radius = circle.radius;
        this.circle$.next(circle);
    }
}
