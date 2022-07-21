import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { UntypedFormControl } from '@angular/forms';
import { LoggerService } from '@dotcms/dotcms-js';
import { GCircle } from '../../models/gcircle.model';

const UNITS = {
    km: {
        km: (len) => len,
        m: (len) => len * 1000,
        mi: (len) => len / 1.60934
    },
    m: {
        km: (len) => len / 1000,
        m: (len) => len,
        mi: (len) => len / 1609.34
    },
    mi: {
        km: (len) => len / 1.60934,
        m: (len) => len * 1609.34,
        mi: (len) => len
    }
};

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DecimalPipe],
    selector: 'cw-visitors-location-component',
    template: `<div
            flex
            layout="row"
            class="cw-visitors-location cw-condition-component-body"
            *ngIf="comparisonDropdown != null"
        >
            <cw-input-dropdown
                flex
                class="cw-input"
                [options]="comparisonDropdown.options"
                [formControl]="comparisonDropdown.control"
                [required]="true"
                [class.cw-comparator-selector]="true"
                (onDropDownChange)="comparisonChange.emit($event)"
                placeholder="{{ comparisonDropdown.placeholder }}"
            >
            </cw-input-dropdown>
            <div flex layout-fill layout="row" layout-align="start center" class="cw-input">
                <input
                    pInputText
                    class="cw-latLong"
                    [value]="getRadiusInPreferredUnit() | number: '1.0-0'"
                    [readonly]="true"
                />
                <label class="cw-input-label-right">{{ preferredUnit }}</label>
            </div>
            <div flex layout-fill layout="row" layout-align="start center" class="cw-input">
                <label class="cw-input-label-left">{{ fromLabel }}</label>
                <input pInputText class="cw-radius" [value]="getLatLong()" [readonly]="true" />
            </div>
            <div flex layout="column" class="cw-input cw-last">
                <button
                    pButton
                    class="p-button-secondary"
                    icon="pi pi-plus"
                    label="Show Map"
                    aria-label="Show Map"
                    (click)="toggleMap()"
                ></button>
            </div>
        </div>
        <cw-area-picker-dialog-component
            [headerText]="'Select an area'"
            [hidden]="!showingMap"
            [circle]="circle"
            (circleUpdate)="onUpdate($event)"
            (cancel)="showingMap = !showingMap"
        ></cw-area-picker-dialog-component> `
})
export class VisitorsLocationComponent {
    @Input() circle: GCircle = { center: { lat: 38.89, lng: -77.04 }, radius: 10000 };
    @Input() comparisonValue: string;
    @Input() comparisonControl: UntypedFormControl;
    @Input() comparisonOptions: {}[];
    @Input() fromLabel = 'of';
    @Input() changedHook = 0;
    @Input() preferredUnit = 'm';

    @Output() areaChange: EventEmitter<GCircle> = new EventEmitter(false);
    @Output() comparisonChange: EventEmitter<string> = new EventEmitter(false);

    showingMap = false;
    comparisonDropdown: any;

    constructor(public decimalPipe: DecimalPipe, private loggerService: LoggerService) {
        loggerService.info('VisitorsLocationComponent', 'constructor');
    }

    ngOnChanges(change): void {
        this.loggerService.info('VisitorsLocationComponent', 'ngOnChanges', change);

        if (change.comparisonOptions) {
            this.comparisonDropdown = {
                control: this.comparisonControl,
                name: 'comparison',
                options: this.comparisonOptions,
                placeholder: '',
                value: this.comparisonValue
            };
        }
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
        this.loggerService.info('VisitorsLocationComponent', 'getRadiusInPreferredUnit', r);
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
