import {
  Component,
  InjectionToken,
  OnInit,
  inject,
  signal,
} from '@angular/core';

import { GenericContentlet } from '../../utils';
import { ContentletsComponent } from '../contentlets/contentlets.component';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { DOTCMS_CLIENT_TOKEN } from '@dotcms/angular';
import { DotCmsClient } from '@dotcms/client';

@Component({
  selector: 'app-blogs',
  standalone: true,
  imports: [ContentletsComponent],
  template: ` <div class="flex flex-col">
    <h2 class="text-2xl font-bold mb-7 text-black">Latest Blog Posts</h2>
    @if (!!blogs().length) {
    <app-contentlets [contentlets]="blogs()" />
    }
  </div>`,
})
export class BlogsComponent implements OnInit {
  // TODO: WE NEED TO FIX THIS SOMEHOW
  // IF WE DONT DO THIS WE WILL GET A TYPE ERROR ON DEVELOPMENT BECAUSE OF TYPES FROM OUR SYM LINK, COULD BE ANGULAR VERSIONS
  private readonly client = inject<DotCmsClient>(
    DOTCMS_CLIENT_TOKEN as unknown as InjectionToken<DotCmsClient>
  );

  readonly blogs = signal<Contentlet<GenericContentlet>[]>([]);

  ngOnInit(): void {
    this.client.content
      .getCollection<GenericContentlet>('Blog')
      .limit(3)
      .sortBy([
        {
          field: 'modDate',
          order: 'desc',
        },
      ])
      .then((response) => {
        this.blogs.set(response.contentlets);
      })
      .catch((error) => {
        console.error('Error fetching Blogs', error);
      });
  }
}
