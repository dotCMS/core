import { Component, OnInit, inject, signal } from '@angular/core';
import { DOTCMS_CLIENT_TOKEN } from '../../client-token/dotcms-client';
import { GenericContentlet } from '../../utils';
import { ContentletsComponent } from '../contentlets/contentlets.component';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';

@Component({
  selector: 'app-destinations',
  standalone: true,
  imports: [ContentletsComponent],
  template: ` <div class="flex flex-col">
    <h2 class="text-2xl font-bold mb-7 text-black">Popular Destinations</h2>
    @if(!!destinations().length) {
    <app-contentlets [contentlets]="destinations()" /> }
  </div>`,
})
export class DestinationsComponent implements OnInit {
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  readonly destinations = signal<Contentlet<GenericContentlet>[]>([]);

  ngOnInit() {
    this.client.content
      .getCollection<GenericContentlet>('Destination')
      .limit(3)
      .sortBy([
        {
          field: 'modDate',
          order: 'desc',
        },
      ])
      .then((response) => {
        this.destinations.set(response.contentlets);
      })
      .catch((error) => {
        console.error('Error fetching Destinations', error);
      });
  }
}
