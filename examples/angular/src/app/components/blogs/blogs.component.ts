import { Component, inject, signal } from '@angular/core';
import { DOTCMS_CLIENT_TOKEN } from '../../client-token/dotcms-client';
import { GenericContentlet } from '../../utils';
import { ContentletsComponent } from '../contentlets/contentlets.component';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';

@Component({
  selector: 'app-blogs',
  standalone: true,
  imports: [ContentletsComponent],
  template: ` <div class="flex flex-col">
    <h2 class="text-2xl font-bold mb-7 text-black">Latest Blog Posts</h2>
    @if(!!blogs().length) { <app-contentlets [contentlets]="blogs()" /> }
  </div>`,
})
export class BlogsComponent {
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  readonly blogs = signal<Contentlet<GenericContentlet>[]>([]);

  constructor() {
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
      });
  }
}
