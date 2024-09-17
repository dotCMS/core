import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { DotCmsClient } from '@dotcms/client';
import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';
import { DOTCMS_CLIENT_TOKEN } from '../../../../app.config';
import { GenericContentlet } from '../..';

@Component({
  selector: 'app-blogs',
  standalone: true,
  imports: [ContentletsWrapperComponent],
  template: ` <div class="flex flex-col">
    <h2 class="mb-7 text-2xl font-bold text-black">Latest Blog Posts</h2>
    @if (!!blogs().length) {
    <app-contentlets-wrapper [contentlets]="blogs()" />
    }
  </div>`,
})
export class BlogsComponent implements OnInit {
  private readonly client = inject<DotCmsClient>(DOTCMS_CLIENT_TOKEN);

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
