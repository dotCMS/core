/// <reference types="googlemaps" />
import {
    Component,
    ChangeDetectionStrategy,
    inject,
    input,
    output,
    effect,
    signal
} from '@angular/core';

import { SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { LoggerService } from '@dotcms/dotcms-js';

import { GCircle } from '../../../models/gcircle.model';
import { GoogleMapService } from '../../../services/maps/GoogleMapService';

let mapIdCounter = 1;

@Component({
    selector: 'dot-area-picker-dialog-component',
    templateUrl: './dot-area-picker-dialog.component.html',
    styleUrls: ['./dot-area-picker-dialog.component.scss'],
    imports: [DialogModule, ButtonModule, SharedModule],
    changeDetection: ChangeDetectionStrategy.Default
})
export class DotAreaPickerDialogComponent {
    private readonly mapsService = inject(GoogleMapService);
    private readonly logger = inject(LoggerService);

    // Inputs
    readonly $apiKey = input<string>('', { alias: 'apiKey' });
    readonly $headerText = input<string>('', { alias: 'headerText' });
    readonly $hidden = input<boolean>(false, { alias: 'hidden' });
    readonly $circle = input<GCircle>(
        { center: { lat: 38.8977, lng: -77.0365 }, radius: 50000 },
        { alias: 'circle' }
    );

    // Outputs
    readonly close = output<{ isCanceled: boolean }>();
    readonly cancel = output<boolean>();
    readonly circleUpdate = output<GCircle>();

    // Internal state
    readonly currentCircle = signal<GCircle>({
        center: { lat: 38.8977, lng: -77.0365 },
        radius: 50000
    });
    map: google.maps.Map | null = null;
    mapId = 'map_' + mapIdCounter++;

    private prevCircle: GCircle;
    private previousHidden: boolean | null = null;

    constructor() {
        this.logger.debug('DotAreaPickerDialogComponent', 'constructor', this.mapId);

        // Watch for hidden changes
        effect(() => {
            const hidden = this.$hidden();
            const circle = this.$circle();

            // Initialize current circle from input
            if (circle) {
                this.currentCircle.set(circle);
            }

            // Handle visibility changes
            if (this.previousHidden !== null) {
                if (!hidden && this.map == null) {
                    this.mapsService.mapsApi$.subscribe({
                        complete: () => {
                            this.readyMap();
                        }
                    });
                }

                if (hidden && this.map) {
                    this.logger.debug(
                        'DotAreaPickerDialogComponent',
                        'effect',
                        'hiding map: ',
                        this.map.getDiv().getAttribute('id')
                    );
                    this.map = null;
                }

                if (!hidden && this.map) {
                    this.logger.debug(
                        'DotAreaPickerDialogComponent',
                        'effect',
                        'showing map: ',
                        this.map.getDiv().getAttribute('id')
                    );
                }
            }

            this.previousHidden = hidden;
        });
    }

    readyMap(): void {
        const el = document.getElementById(this.mapId);
        if (!el) {
            window.setTimeout(() => this.readyMap(), 10);
        } else {
            const circle = this.currentCircle();
            this.prevCircle = circle;
            this.map = new google.maps.Map(el, {
                center: new google.maps.LatLng(circle.center.lat, circle.center.lng),
                mapTypeId: google.maps.MapTypeId.TERRAIN,
                zoom: 7
            });

            const mapCircle = new google.maps.Circle({
                center: new google.maps.LatLng(circle.center.lat, circle.center.lng),
                editable: true,
                fillColor: '#1111FF',
                fillOpacity: 0.35,
                map: this.map,
                radius: circle.radius,
                strokeColor: '#1111FF',
                strokeOpacity: 0.8,
                strokeWeight: 2
            });

            this.map.addListener('click', (e) => {
                mapCircle.setCenter(e.latLng);
                this.map.panTo(e.latLng);
                const ll = mapCircle.getCenter();
                const center = { lat: ll.lat(), lng: ll.lng() };
                this.currentCircle.set({ center, radius: mapCircle.getRadius() });
            });

            google.maps.event.addListener(mapCircle, 'radius_changed', () => {
                const current = this.currentCircle();
                this.logger.debug('radius changed', mapCircle.getRadius(), current.radius);
                const ll = mapCircle.getCenter();
                const center = { lat: ll.lat(), lng: ll.lng() };
                this.currentCircle.set({ center, radius: mapCircle.getRadius() });
            });

            google.maps.event.addListener(mapCircle, 'center_changed', () => {
                const ll = mapCircle.getCenter();
                const center = { lat: ll.lat(), lng: ll.lng() };
                this.logger.debug('center changed', center);
                this.currentCircle.set({ center, radius: mapCircle.getRadius() });
            });
        }
    }

    onOkAction(): void {
        const circle = this.currentCircle();
        this.prevCircle = circle;
        this.circleUpdate.emit(circle);
    }

    onCancelAction(): void {
        this.currentCircle.set(this.prevCircle);
        this.cancel.emit(false);
    }
}
