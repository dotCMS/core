import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { Location } from '@angular/common';
import { DotCMSClient } from '@dotcms/angular';
import { ActivatedRoute } from '@angular/router';

import { DotCMSPageAsset } from '@dotcms/types';

import { ErrorComponent } from '../../shared/components/error/error.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { SearchComponent } from './components/search/search.component';
import { BlogCardComponent } from './components/blog-card/blog-card.component';
import { EditablePageService } from '../../services/editable-page.service';
import { DYNAMIC_COMPONENTS } from '../../shared/dynamic-components';
import { buildExtraQuery } from '../../shared/queries';
import { ExtraContent, Blog } from '../../shared/contentlet.model';

type DotCMSPage = {
  pageAsset: DotCMSPageAsset;
  content: ExtraContent;
};

const LIMIT_BLOGS = 10;

@Component({
  selector: 'app-blog-listing',
  imports: [
    HeaderComponent,
    NavigationComponent,
    ErrorComponent,
    LoadingComponent,
    SearchComponent,
    BlogCardComponent,
  ],
  providers: [EditablePageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './blog-listing.component.html',
})
export class BlogListingComponent {
  readonly #editablePageService =
    inject<EditablePageService<DotCMSPage>>(EditablePageService);
  // Use proper client injection via token
  readonly #client = inject(DotCMSClient);
  readonly #route = inject(ActivatedRoute);
  readonly #location = inject(Location);

  searchQuery = signal(this.#route.snapshot.queryParamMap.get('search') || '');
  filteredBlogs = signal<Blog[]>([]);

  readonly components = DYNAMIC_COMPONENTS;

  readonly year = new Date().getFullYear();

  readonly navigation = computed(
    () => this.$pageState().pageResponse?.content?.navigation?.children || [],
  );

  $pageState = this.#editablePageService.initializePage({
    graphql: {
      ...buildExtraQuery({
        limitBlogs: LIMIT_BLOGS,
      }),
    },
  });

  constructor() {
    effect(() => {
      this.updateFilteredBlogs();
    });
  }

  onSearchQueryChange(query: string): void {
    this.searchQuery.set(query);
  }

  private updateFilteredBlogs(): void {
    const query = this.searchQuery();
    const pageState = this.$pageState();

    if (!pageState.pageResponse?.content) {
      return;
    }

    const blogs = pageState.pageResponse.content.blogs || [];

    if (!query.length) {
      this.#location.go(`/blog`);
      this.filteredBlogs.set(blogs);
      return;
    }

    this.#location.go(`/blog?search=${query}`);
    // Use the properly injected DotCMS client to search
    this.#client.content
      .getCollection('Blog')
      .limit(LIMIT_BLOGS)
      .query((qb) => qb.field('title').equals(`${query}*`))
      .sortBy([
        {
          field: 'Blog.postingDate',
          order: 'desc',
        },
      ])
      .then((response: any) => {
        this.filteredBlogs.set(response.contentlets);
      })
      .catch(() => {
        // Fallback to client-side filtering if the API call fails
        const filteredResults = blogs.filter((blog) =>
          blog.title.toLowerCase().startsWith(query.toLowerCase()),
        );
        this.filteredBlogs.set(filteredResults);
      });
  }
}
