import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';

import { GenericContentlet } from '../..';
import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { DotCmsClient } from '@dotcms/client';
import { DOTCMS_CLIENT_TOKEN } from '../../../../app.config';


@Component({
  selector: 'app-destinations',
  standalone: true,
  imports: [ContentletsWrapperComponent],
  template: ` <div class="flex flex-col">
    <h2 class="mb-7 text-2xl font-bold text-black">Popular Destinations</h2>
    @if (!!destinations().length) {
    <app-contentlets-wrapper [contentlets]="destinations()" />
    }
  </div>`,
})
export class DestinationsComponent implements OnInit {
  private readonly client = inject<DotCmsClient>(DOTCMS_CLIENT_TOKEN);

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
