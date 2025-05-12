import { Component, OnInit, inject, input, signal } from '@angular/core';

import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';

import { Destination } from '../../../../shared/models';

@Component({
    selector: 'app-destinations',
    standalone: true,
    imports: [ContentletsWrapperComponent],
    template: ` <div class="flex flex-col">
        <h2 class="mb-7 text-2xl font-bold text-black">Popular Destinations</h2>
        @if (!!cleanedDestinations.length) {
        <app-contentlets-wrapper [contentlets]="cleanedDestinations" />
        }
    </div>`
})
export class DestinationsComponent {
    destinations = input<Destination[]>([]);

    get cleanedDestinations() {
        return this.destinations().map((destination) => destination._map);
    }
}
