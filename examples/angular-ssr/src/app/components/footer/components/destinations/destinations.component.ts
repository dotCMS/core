import { Component, input } from '@angular/core';

import { RecommendedCardComponent } from '../recommended-card/recommended-card.component';

import { Destination } from '../../../../dotcms/types/contentlet.model';

@Component({
  selector: 'app-destinations',
  imports: [RecommendedCardComponent],
  template: ` <div class="flex flex-col">
    <h2 class="mb-7 text-2xl font-bold text-black">Popular Destinations</h2>
    <div class="flex flex-col gap-5">
      @for (destination of destinations(); track $index) {
        <app-recommended-card [contentlet]="destination" />
      }
    </div>
  </div>`,
})
export class DestinationsComponent {
  destinations = input<Destination[]>([]);
}
