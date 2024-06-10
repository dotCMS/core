import { environment } from '../../../../environments/environment';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DOTCMS_CLIENT_TOKEN } from '../../../client-token/dotcms-client';
import { BlogsComponent } from '../../../components/blogs/blogs.component';
import { DestinationsComponent } from '../../../components/destinations/destinations.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [BlogsComponent, DestinationsComponent],
  template: `<footer class="p-4 text-white bg-red-100 py-24">
    <div
      class="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24"
    >
      <div class="flex gap-7 flex-col">
        <h2 class="text-2xl font-bold text-black">About us</h2>
        <p class="text-sm text-zinc-800">
          We are TravelLux, a community of dedicated travel experts,
          journalists, and bloggers. Our aim is to offer you the best insight on
          where to go for your travel as well as to give you amazing
          opportunities with free benefits and bonuses for registered clients.
        </p>
        <img />
        <img
          [src]="
            environment.dotcmsUrl +
            '/contentAsset/image/82da90eb-044d-44cc-a71b-86f79820b61b/fileAsset'
          "
          height="53"
          width="221"
          alt="TravelLux logo"
        />
      </div>
      <app-blogs />
      <app-destinations />
    </div>
  </footer>`,
  styleUrl: './footer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  protected readonly client = inject(DOTCMS_CLIENT_TOKEN);

  protected readonly environment = environment;

  constructor() {
    this.client.content
      .getCollection('Blog')
      .limit(3)
      .sortBy([
        {
          field: 'modDate',
          order: 'desc',
        },
      ])
      .then((response) => {
        console.log(response);
      });
  }
}
