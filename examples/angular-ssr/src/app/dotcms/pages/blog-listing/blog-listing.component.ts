import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  signal,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DotCMSEditablePageService, DynamicComponentEntity } from '@dotcms/angular';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Location } from '@angular/common';
import { filter, map, startWith, switchMap } from 'rxjs/operators';

import { DotCMSComposedPageResponse, DotCMSPageAsset } from '@dotcms/types';
import { SearchComponent } from './components/search/search.component';
import { BlogCardComponent } from './components/blog-card/blog-card.component';
import { Blog } from '../../types/contentlet.model';
import { LoadingComponent } from '../../../components/loading/loading.component';

// Constants
const BLOG_LIMIT = 10;
const BLOG_CONTENT_TYPE = 'Blog';
const BLOG_ROUTE = '/blog';

// Types
interface PageResponse {
  pageAsset: DotCMSPageAsset;
  content: {
    blogs: Blog[];
  };
}

interface GraphQLQuery {
  graphql: {
    content: {
      blogs: string;
    };
  };
}

@Component({
  selector: 'app-blog-listing',
  imports: [SearchComponent, BlogCardComponent, LoadingComponent],
  providers: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './blog-listing.component.html',
})
export class BlogListingComponent {
  // Injected services
  readonly #route = inject(ActivatedRoute);
  readonly #router = inject(Router);
  readonly #location = inject(Location);
  private readonly http = inject(HttpClient);
  private readonly editablePageService = inject(DotCMSEditablePageService);
  private readonly destroyRef = inject(DestroyRef);

  // Signals
  searchQuery = signal(this.#route.snapshot.queryParamMap.get('search') || '');
  filteredBlogs = signal<Blog[]>([]);

  // Dynamic components
  readonly components: { [key: string]: DynamicComponentEntity } = {
    SimpleWidget: import('../../components/simple-widget/simple-widget.component').then(
      (c) => c.SimpleWidgetComponent
    ),
  };

  // Computed properties
  readonly year = new Date().getFullYear();

  constructor() {
    effect(() => {
      this.updateFilteredBlogs();
    });
  }

  ngOnInit(): void {
    const route = this.#router.url.split('?')[0] || '/';
    const pageParams = this.createPageParams();

    this.#router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects),
        startWith(route),
        switchMap((url: string) =>
          this.http.post<DotCMSComposedPageResponse<PageResponse>>('/data/page', {
            url,
            params: pageParams,
          })
        )
      )
      .pipe(switchMap((response) => this.editablePageService.listen<PageResponse>(response)))
      .pipe(filter(Boolean))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response: DotCMSComposedPageResponse<PageResponse>) => {
          this.filteredBlogs.set(response?.content?.blogs || []);
        },
        error: (error) => {
          console.error('Error in page data stream:', error);
        },
      });
  }

  onSearchQueryChange(query: string): void {
    this.searchQuery.set(query);
  }

  // Private helper methods
  private createPageParams(searchQuery: string | null = null): GraphQLQuery {
    const graphqlQuery = this.buildBlogSearchQuery(searchQuery);

    return {
      graphql: {
        content: {
          blogs: graphqlQuery,
        },
      },
    };
  }

  private buildBlogSearchQuery(searchQuery: string | null = null): string {
    const queryString = searchQuery ? ` +(title:${searchQuery}*)` : '';

    return `
      search(query: "+contenttype:${BLOG_CONTENT_TYPE}${queryString} +live:true", limit: ${BLOG_LIMIT}) {
        title
        identifier
        ... on Blog {
          inode
          image {
            fileName
            fileAsset {
              versionPath
            }
          }
          urlMap
          modDate
          urlTitle
          teaser
          author {
            firstName
            lastName
            inode
          }
        }
      }`;
  }

  private updateFilteredBlogs(): void {
    const query = this.searchQuery();
    const url = !query.length ? BLOG_ROUTE : `${BLOG_ROUTE}?search=${query}`;
    this.#location.go(url);

    const pageParams = this.createPageParams(query);

    this.http
      .post<DotCMSComposedPageResponse<PageResponse>>('/data/page', {
        url: BLOG_ROUTE,
        params: pageParams,
      })
      .subscribe({
        next: (response) => {
          this.filteredBlogs.set(response?.content?.blogs || []);
        },
        error: (error) => {
          console.error('Error fetching filtered blogs:', error);
        },
      });
  }
}
