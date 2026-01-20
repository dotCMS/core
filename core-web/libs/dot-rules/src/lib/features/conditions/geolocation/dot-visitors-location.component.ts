import { Observable, from, of } from 'rxjs';

import { AsyncPipe, DecimalPipe } from '@angular/common';
import {
    Component,
    ChangeDetectionStrategy,
    inject,
    input,
    output,
    effect,
    signal
} from '@angular/core';
import { UntypedFormControl, FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotAreaPickerDialogComponent } from './dot-area-picker-dialog.component';

import { GCircle } from '../../../models/gcircle.model';

type DistanceUnit = 'km' | 'm' | 'mi';

interface ComparisonOption {
    label: string | Observable<string>;
    value: string;
}

const UNIT_CONVERSIONS: Record<DistanceUnit, Record<DistanceUnit, (len: number) => number>> = {
    km: {
        km: (len: number) => len,
        m: (len: number) => len * 1000,
        mi: (len: number) => len / 1.60934
    },
    m: {
        km: (len: number) => len / 1000,
        m: (len: number) => len,
        mi: (len: number) => len / 1609.34
    },
    mi: {
        km: (len: number) => len / 1.60934,
        m: (len: number) => len * 1609.34,
        mi: (len: number) => len
    }
};

@Component({
    selector: 'dot-visitors-location-component',
    templateUrl: './dot-visitors-location.component.html',
    styleUrl: './dot-visitors-location.component.scss',
    imports: [
        AsyncPipe,
        DecimalPipe,
        FormsModule,
        InputTextModule,
        ButtonModule,
        SelectModule,
        DotAreaPickerDialogComponent
    ],
    providers: [DecimalPipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotVisitorsLocationComponent {
    private readonly decimalPipe = inject(DecimalPipe);
    private readonly logger = inject(LoggerService);

    // Inputs
    readonly $circle = input<GCircle>(
        { center: { lat: 38.89, lng: -77.04 }, radius: 10000 },
        { alias: 'circle' }
    );
    readonly $comparisonValue = input<string>('', { alias: 'comparisonValue' });
    readonly $comparisonControl = input<UntypedFormControl>(undefined, {
        alias: 'comparisonControl'
    });
    readonly $comparisonOptions = input<ComparisonOption[]>([], { alias: 'comparisonOptions' });
    readonly $fromLabel = input<string>('of', { alias: 'fromLabel' });
    readonly $changedHook = input<number>(0, { alias: 'changedHook' });
    readonly $preferredUnit = input<DistanceUnit>('m', { alias: 'preferredUnit' });

    // Outputs
    readonly areaChange = output<GCircle>();
    readonly comparisonChange = output<string>();

    // State
    readonly showingMap = signal(false);
    comparisonDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    constructor() {
        this.logger.info('DotVisitorsLocationComponent', 'constructor');

        // React to comparisonOptions changes
        effect(() => {
            const options = this.$comparisonOptions();
            this.logger.info('DotVisitorsLocationComponent', 'comparisonOptions changed', options);

            if (options && options.length > 0) {
                this.buildComparisonDropdownOptions(options);
            }
        });
    }

    private buildComparisonDropdownOptions(options: ComparisonOption[]): void {
        this.comparisonDropdownOptions$ = from(options).pipe(
            mergeMap((item: ComparisonOption) => {
                if (item.label && typeof item.label !== 'string' && 'pipe' in item.label) {
                    return item.label.pipe(
                        map((text: string) => ({
                            label: text,
                            value: item.value
                        }))
                    );
                }

                return of({
                    label: item.label as string,
                    value: item.value
                });
            }),
            toArray(),
            startWith([]),
            shareReplay(1)
        );
    }

    onComparisonChange(value: string): void {
        this.comparisonChange.emit(value);
    }

    getFormattedLatLong(): string {
        const circle = this.$circle();
        const lat = circle.center.lat;
        const lng = circle.center.lng;
        const latStr = this.decimalPipe.transform(parseFloat(lat + ''), '1.6-6');
        const lngStr = this.decimalPipe.transform(parseFloat(lng + ''), '1.6-6');

        return `${latStr}, ${lngStr}`;
    }

    getRadiusInPreferredUnit(): number {
        const radius = this.$circle().radius;
        const unit = this.$preferredUnit();
        this.logger.info('DotVisitorsLocationComponent', 'getRadiusInPreferredUnit', radius);

        return UNIT_CONVERSIONS.m[unit](radius);
    }

    toggleMap(): void {
        this.showingMap.update((showing) => !showing);
    }

    onMapUpdate(circle: GCircle): void {
        this.showingMap.set(false);
        this.areaChange.emit(circle);
    }
}
