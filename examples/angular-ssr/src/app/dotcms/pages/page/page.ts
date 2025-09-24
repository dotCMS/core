import { filter, from, map, startWith, switchMap } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';

import {
  AngularDotCMSClient,
  DotCMSEditablePageService,
  DotCMSLayoutBodyComponent,
  DynamicComponentEntity,
} from '@dotcms/angular';
import { DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset } from '@dotcms/types';

import { HeaderComponent } from '../../../components/header/header.component';
import { NavigationComponent } from '../../../components/navigation/navigation.component';

const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
  Activity: import('../../components/activity/activity.component').then((c) => c.ActivityComponent),
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
  StoreProductList: import('../../components/store-product-list/store-product-list.component').then(
    (c) => c.StoreProductListComponent
  ),
  SimpleWidget: import('../../components/simple-widget/simple-widget.component').then(
    (c) => c.SimpleWidgetComponent
  ),
  PageForm: import('../../components/page-form/page-form.component').then(
    (c) => c.PageFormComponent
  ),
};

type PageResponse = { content: { navigation: DotCMSNavigationItem } };

@Component({
  selector: 'app-page',
  imports: [
    CommonModule,
    DotCMSLayoutBodyComponent
  ],
  providers: [DotCMSEditablePageService],
  templateUrl: './page.html',
  styleUrl: './page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageComponent implements OnInit {
  private readonly client = inject(AngularDotCMSClient);
  private readonly http = inject(HttpClient);

  private router = inject(Router);

  private readonly editablePageService = inject(DotCMSEditablePageService);

  private readonly destroyRef = inject(DestroyRef);

  pageAsset = signal<DotCMSPageAsset | null>(null);

  components = signal<{ [key: string]: DynamicComponentEntity }>(DYNAMIC_COMPONENTS);

  ngOnInit() {
    console.log('PageComponent ngOnInit');
    const route = this.router.url.split('?')[0] || '/';

    // Convert promise to observable and merge with editable page service
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects),
        startWith(route),
        switchMap((url: string) =>
          this.http.post<DotCMSComposedPageResponse<PageResponse>>('/api/page', { url })
        )
      )
      .pipe(switchMap((response) => this.editablePageService.listen<PageResponse>(response)))
      .pipe(filter(Boolean))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (
          response: DotCMSComposedPageResponse<{
            content: { navigation: DotCMSNavigationItem };
          }>
        ) => {
          this.pageAsset.set(response?.pageAsset);
        },
        error: (error) => {
          console.error('Error in page data stream:', error);
        },
      });
  }
}
