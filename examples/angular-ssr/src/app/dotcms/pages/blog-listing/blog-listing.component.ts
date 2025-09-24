import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Location } from '@angular/common';
import {
  AngularDotCMSClient,
  DotCMSEditablePageService,
  DynamicComponentEntity,
} from '@dotcms/angular';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';

import { DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset } from '@dotcms/types';

import { SearchComponent } from './components/search/search.component';
import { BlogCardComponent } from './components/blog-card/blog-card.component';
import { Blog } from '../../types/contentlet.model';
import { filter, map, startWith, switchMap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';

type PageResponse = { content: { navigation: DotCMSNavigationItem; blogs: Blog[] } };

@Component({
  selector: 'app-blog-listing',
  imports: [SearchComponent, BlogCardComponent],
  providers: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './blog-listing.component.html',
})
export class BlogListingComponent {
  // Use proper client injection via token
  readonly #route = inject(ActivatedRoute);
  readonly #router = inject(Router);
  // readonly #location = inject(Location);

  private readonly http = inject(HttpClient);


  pageAsset = signal<DotCMSPageAsset | null>(null);

  searchQuery = signal(this.#route.snapshot.queryParamMap.get('search') || '');
  filteredBlogs = signal<Blog[]>([]);

  readonly components: { [key: string]: DynamicComponentEntity } = {
    Activity: import('../../components/activity/activity.component').then(
      (c) => c.ActivityComponent
    ),
    Banner: import('../../components/banner/banner.component').then((c) => c.BannerComponent),
    Image: import('../../components/image/image.component').then((c) => c.ImageComponent),
    webPageContent: import('../../components/web-page-content/web-page-content.component').then(
      (c) => c.WebPageContentComponent
    ),
    Product: import('../../components/product/product.component').then((c) => c.ProductComponent),
    BannerCarousel: import('../../components/banner-carousel/banner-carousel.component').then(
      (c) => c.BannerCarouselComponent
    ),
    VtlInclude: import('../../components/vtl-include/vtl-include.component').then(
      (c) => c.VtlIncludeComponent
    ),
    CategoryFilter: import('../../components/category-filter/category-filter.component').then(
      (c) => c.CategoryFilterComponent
    ),
    StoreProductList: import(
      '../../components/store-product-list/store-product-list.component'
    ).then((c) => c.StoreProductListComponent),
    SimpleWidget: import('../../components/simple-widget/simple-widget.component').then(
      (c) => c.SimpleWidgetComponent
    ),
    PageForm: import('../../components/page-form/page-form.component').then(
      (c) => c.PageFormComponent
    ),
  };

  readonly year = new Date().getFullYear();


  private readonly editablePageService = inject(DotCMSEditablePageService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      this.updateFilteredBlogs();
    });
  }

  ngOnInit() {
    const route = this.#router.url.split('?')[0] || '/';

    const pageParams = {
      graphql: {
        content: {
          blogs: `
          search(query: "+contenttype:Blog +live:true", limit: 10) {
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
          }`
        },
      },
    };

    // Convert promise to observable and merge with editable page service
    this.#router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects),
        startWith(route),
        switchMap((url: string) =>
          this.http.post<DotCMSComposedPageResponse<PageResponse>>('/api/page', { url, params: pageParams })
        )
      )
      .pipe(switchMap((response) => this.editablePageService.listen<PageResponse>(response)))
      .pipe(filter(Boolean))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (
          response: DotCMSComposedPageResponse<{
            content: { navigation: DotCMSNavigationItem; blogs: Blog[] };
          }>
        ) => {
          this.pageAsset.set(response?.pageAsset);
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

  private updateFilteredBlogs(): void {
    const query = this.searchQuery();

    console.log('updateFilteredBlogs');
    // if (!query.length) {
    //   this.#location.go(`/blog`);
    //   this.filteredBlogs.set(blogs);
    //   return;
    // }
    // this.#location.go(`/blog?search=${query}`);
    // // Use the properly injected DotCMS client to search
    // this.#client.content
    //   .getCollection('Blog')
    //   .limit(LIMIT_BLOGS)
    //   .query((qb) => qb.field('title').equals(`${query}*`))
    //   .sortBy([
    //     {
    //       field: 'Blog.postingDate',
    //       order: 'desc',
    //     },
    //   ])
    //   .then((response: any) => {
    //     this.filteredBlogs.set(response.contentlets);
    //   })
    //   .catch(() => {
    //     // Fallback to client-side filtering if the API call fails
    //     const filteredResults = blogs.filter((blog) =>
    //       blog.title.toLowerCase().startsWith(query.toLowerCase()),
    //     );
    //     this.filteredBlogs.set(filteredResults);
    //   });
  }
}
