import { Component, input } from '@angular/core';

import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';

import { Destination } from '../../../../shared/models';

@Component({
    selector: 'app-destinations',
    standalone: true,
    imports: [ContentletsWrapperComponent],
    template: ` <div class="flex flex-col">
        <h2 class="mb-7 text-2xl font-bold text-black">Popular Destinations</h2>
        @if (!!destinations().length) {
        <app-contentlets-wrapper [contentlets]="destinations()" />
        }
    </div>`
})
export class DestinationsComponent {
    destinations = input<Destination[]>([]);
}
