import { Observable, from, of } from 'rxjs';

import { AsyncPipe, DecimalPipe } from '@angular/common';
import {
    Component,
    Input,
    Output,
    EventEmitter,
    ChangeDetectionStrategy,
    inject,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { UntypedFormControl, FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotAreaPickerDialogComponent } from '../../components/dot-area-picker-dialog/dot-area-picker-dialog.component';
import { GCircle } from '../../models/gcircle.model';

type UnitKey = 'km' | 'm' | 'mi';

interface ComparisonOption {
    label: string | Observable<string>;
    value: string;
}

const UNITS: Record<UnitKey, Record<UnitKey, (len: number) => number>> = {
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
export class DotVisitorsLocationComponent implements OnChanges {
    decimalPipe = inject(DecimalPipe);
    private loggerService = inject(LoggerService);

    @Input() circle: GCircle = { center: { lat: 38.89, lng: -77.04 }, radius: 10000 };
    @Input() comparisonValue: string;
    @Input() comparisonControl: UntypedFormControl;
    @Input() comparisonOptions: ComparisonOption[];
    @Input() fromLabel = 'of';
    @Input() changedHook = 0;
    @Input() preferredUnit: UnitKey = 'm';

    @Output() areaChange: EventEmitter<GCircle> = new EventEmitter(false);
    @Output() comparisonChange: EventEmitter<string> = new EventEmitter(false);

    showingMap = false;
    comparisonDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    constructor() {
        const loggerService = this.loggerService;

        loggerService.info('DotVisitorsLocationComponent', 'constructor');
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.loggerService.info('DotVisitorsLocationComponent', 'ngOnChanges', changes);

        if (changes['comparisonOptions'] && this.comparisonOptions) {
            this.comparisonDropdownOptions$ = from(this.comparisonOptions).pipe(
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
    }

    onComparisonChange(value: string): void {
        this.comparisonChange.emit(value);
    }

    getLatLong(): string {
        const lat = this.circle.center.lat;
        const lng = this.circle.center.lng;
        const latStr = this.decimalPipe.transform(parseFloat(lat + ''), '1.6-6');
        const lngStr = this.decimalPipe.transform(parseFloat(lng + ''), '1.6-6');

        return latStr + ', ' + lngStr;
    }

    getRadiusInPreferredUnit(): number {
        const r = this.circle.radius;
        this.loggerService.info('DotVisitorsLocationComponent', 'getRadiusInPreferredUnit', r);

        return UNITS.m[this.preferredUnit](r);
    }

    toggleMap(): void {
        this.showingMap = !this.showingMap;
    }

    onUpdate(circle: GCircle): void {
        this.showingMap = false;
        this.areaChange.emit(circle);
    }
}
