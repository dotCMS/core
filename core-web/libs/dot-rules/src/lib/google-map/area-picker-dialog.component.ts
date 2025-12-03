/// <reference types="googlemaps" />
// import {} from '@types/googlemaps';
import {
    Component,
    ChangeDetectionStrategy,
    Input,
    Output,
    EventEmitter,
    OnChanges,
    inject
} from '@angular/core';

import { LoggerService } from '@dotcms/dotcms-js';

import { GCircle } from '../models/gcircle.model';
import { GoogleMapService } from '../services/GoogleMapService';

let mapIdCounter = 1;

@Component({
    changeDetection: ChangeDetectionStrategy.Default,
    selector: 'cw-area-picker-dialog-component',
    styles: [
        `
            .g-map {
                height: 500px;
                width: 100%;
            }
        `
    ],
    template: `
        <cw-modal-dialog
            (ok)="onOkAction($event)"
            (cancel)="onCancelAction($event)"
            [headerText]="headerText"
            [hidden]="hidden"
            [okEnabled]="true">
            @if (!hidden) {
                <div class="cw-dialog-body">
                    @if (!hidden) {
                        <div class="g-map" id="{{ mapId }}"></div>
                    }
                </div>
            }
        </cw-modal-dialog>
    `,
    standalone: false
})
export class AreaPickerDialogComponent implements OnChanges {
    mapsService = inject(GoogleMapService);
    private loggerService = inject(LoggerService);

    @Input() apiKey = '';
    @Input() headerText = '';
    @Input() hidden = false;
    @Input() circle: GCircle = { center: { lat: 38.8977, lng: -77.0365 }, radius: 50000 };

    @Output() close: EventEmitter<{ isCanceled: boolean }> = new EventEmitter(false);
    @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
    @Output() circleUpdate: EventEmitter<GCircle> = new EventEmitter(false);

    map: google.maps.Map;

    mapId = 'map_' + mapIdCounter++;
    waitCount = 0;

    private _prevCircle: GCircle;

    constructor() {
        this.loggerService.debug('AreaPickerDialogComponent', 'constructor', this.mapId);
    }

    ngOnChanges(change): void {
        if (!this.hidden && this.map == null) {
            this.mapsService.mapsApi$.subscribe(
                (_x) => {},
                () => {},
                () => {
                    this.readyMap();
                }
            );
        }

        if (change.hidden && this.hidden && this.map) {
            this.loggerService.debug(
                'AreaPickerDialogComponent',
                'ngOnChanges',
                'hiding map: ',
                this.map.getDiv().getAttribute('id'),
                this.map.getDiv()['style']['height']
            );
            /**
             *
             * Angular2 has a bug? Google Maps? Chrome? For whatever reason, loading a second map without forcing a reload
             * will cause the first map loaded to always display, despite the maps actually living in separate
             * divs, and the 'hidden' map divs actually not being in the active DOM (they have been cut out / moved into the
             * shadow dom by the ngIf).
             */
            this.map = null;
        }

        if (change.hidden && !this.hidden && this.map) {
            this.loggerService.debug(
                'AreaPickerDialogComponent',
                'ngOnChanges',
                'showing map: ',
                this.map.getDiv().getAttribute('id')
            );
        }
    }

    readyMap(): void {
        const el = document.getElementById(this.mapId);
        if (!el) {
            window.setTimeout(() => this.readyMap(), 10);
        } else {
            this._prevCircle = this.circle;
            this.map = new google.maps.Map(el, {
                center: new google.maps.LatLng(this.circle.center.lat, this.circle.center.lng),
                mapTypeId: google.maps.MapTypeId.TERRAIN,
                zoom: 7
            });

            const circle = new google.maps.Circle({
                center: new google.maps.LatLng(this.circle.center.lat, this.circle.center.lng),
                editable: true,
                fillColor: '#1111FF',
                fillOpacity: 0.35,
                map: this.map,
                radius: this.circle.radius,
                strokeColor: '#1111FF',
                strokeOpacity: 0.8,
                strokeWeight: 2
            });

            this.map.addListener('click', (e) => {
                circle.setCenter(e.latLng);
                this.map.panTo(e.latLng);
                const ll = circle.getCenter();
                const center = { lat: ll.lat(), lng: ll.lng() };
                this.circle = { center, radius: circle.getRadius() };
            });

            google.maps.event.addListener(circle, 'radius_changed', () => {
                this.loggerService.debug('radius changed', circle.getRadius(), this.circle.radius);
                const ll = circle.getCenter();
                const center = { lat: ll.lat(), lng: ll.lng() };
                this.circle = { center, radius: circle.getRadius() };
                this.loggerService.debug(
                    'radius changed to',
                    circle.getRadius(),
                    this.circle.radius
                );
            });
        }
    }

    onOkAction(_event): void {
        this._prevCircle = this.circle;
        this.circleUpdate.emit(this.circle);
    }

    onCancelAction(_event): void {
        this.circle = this._prevCircle;
        this.cancel.emit(false);
    }
}
