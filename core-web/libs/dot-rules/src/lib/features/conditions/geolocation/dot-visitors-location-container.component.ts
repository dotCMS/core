import { Observable, BehaviorSubject } from 'rxjs';

import { AsyncPipe, DecimalPipe } from '@angular/common';
import { Component, ChangeDetectionStrategy, inject, input, output, effect } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotVisitorsLocationComponent } from './dot-visitors-location.component';

import { GCircle } from '../../../models/gcircle.model';
import { ServerSideFieldModel } from '../../../services/api/serverside-field/ServerSideFieldModel';
import { I18nService } from '../../../services/i18n/i18n.service';

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
    selector: 'dot-visitors-location-container',
    templateUrl: './dot-visitors-location-container.component.html',
    imports: [AsyncPipe, DotVisitorsLocationComponent],
    providers: [DecimalPipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotVisitorsLocationContainerComponent {
    private readonly i18nService = inject(I18nService);
    private readonly decimalPipe = inject(DecimalPipe);
    private readonly logger = inject(LoggerService);

    // Inputs
    readonly $componentInstance = input.required<ServerSideFieldModel>({
        alias: 'componentInstance'
    });

    // Outputs
    readonly parameterValuesChange = output<{ name: string; value: string }[]>();

    // State
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

    private i18nCache: { [key: string]: Observable<string> } = {};
    private currentInstanceKey: string | null = null;

    constructor() {
        this.i18nService.get(I8N_BASE).subscribe({
            error: (e) => {
                this.logger.error(
                    'DotVisitorsLocationContainerComponent',
                    'Error loading resources',
                    e
                );
            }
        });

        this.circle$.subscribe({
            error: (e) => {
                this.logger.error(
                    'DotVisitorsLocationContainerComponent',
                    'Error updating area',
                    e
                );
            }
        });

        // Watch for componentInstance changes
        effect(() => {
            const instance = this.$componentInstance();
            if (instance && instance.type?.key !== this.currentInstanceKey) {
                this.currentInstanceKey = instance.type?.key || null;
                this.initializeFromInstance(instance);
            }
        });
    }

    private initializeFromInstance(instance: ServerSideFieldModel): void {
        const temp: VisitorsLocationParams =
            instance.parameters as unknown as VisitorsLocationParams;
        const params: VisitorsLocationParams = temp as VisitorsLocationParams;
        const comparisonDef = instance.parameterDefs['comparison'];

        const opts = comparisonDef.inputType['options'];
        const i18nBaseKey = comparisonDef.i18nBaseKey || instance.type.i18nKey;
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
        this.comparisonControl = ServerSideFieldModel.createNgControl(instance, 'comparison');

        this.lat = parseFloat(params.latitude.value) || this.lat;
        this.lng = parseFloat(params.longitude.value) || this.lng;
        this.radius = parseFloat(params.radius.value) || 50000;
        this.preferredUnit =
            params.preferredDisplayUnits.value ||
            instance.parameterDefs['preferredDisplayUnits'].defaultValue;

        this.circle$.next({ center: { lat: this.lat, lng: this.lng }, radius: this.radius });
    }

    rsrc(subkey: string): Observable<string> {
        let x = this.i18nCache[subkey];
        if (!x) {
            x = this.i18nService.get(subkey);
            this.i18nCache[subkey] = x;
        }

        return x;
    }

    onComparisonChange(value: string): void {
        this.parameterValuesChange.emit([{ name: 'comparison', value }]);
    }

    onUpdate(circle: GCircle): void {
        this.logger.info('DotVisitorsLocationContainerComponent', 'onUpdate', circle);
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
