import { filter, from, switchMap } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    inject,
    OnInit,
    signal
} from '@angular/core';
import { Router } from '@angular/router';

import {
    DotCMSClient,
    DotCMSEditablePageService,
    DotCMSLayoutBodyComponent,
    DynamicComponentEntity
} from '@dotcms/angular';
import { DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset } from '@dotcms/types';

import { HeaderComponent } from '../../../components/header/header.component';
import { NavigationComponent } from '../../../components/navigation/navigation.component';


export const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
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
    // VtlInclude: import('../../components/vtl-include/vtl-include.component').then(
    //     (c) => c.VtlIncludeComponent
    // ),
    CategoryFilter: import('../../components/category-filter/category-filter.component').then(
        (c) => c.CategoryFilterComponent
    ),
    StoreProductList: import(
        '../../components/store-product-list/store-product-list.component'
    ).then((c) => c.StoreProductListComponent),
    SimpleWidget: import('../../components/simple-widget/simple-widget.component').then(
        (c) => c.SimpleWidgetComponent
    ),
    // PageForm: import('../../components/page-form/page-form.component').then(
    //     (c) => c.PageFormComponent
    // )
};

type PageResponse = { content: { navigation: DotCMSNavigationItem } };

@Component({
    selector: 'app-page',
    imports: [CommonModule, DotCMSLayoutBodyComponent, HeaderComponent, NavigationComponent],
    providers: [DotCMSEditablePageService],
    templateUrl: './page.component.html',
    styleUrl: './page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageComponent implements OnInit {
    private readonly client = inject(DotCMSClient);

    private router = inject(Router);

    private readonly editablePageService = inject(DotCMSEditablePageService);

    currentRoute = signal(this.router.url);

    navigation = signal<DotCMSNavigationItem[]>([]);
    pageAsset = signal<DotCMSPageAsset | null>(null);

    components = signal<{ [key: string]: DynamicComponentEntity }>(DYNAMIC_COMPONENTS);

    ngOnInit() {
        const route = this.currentRoute().split('?')[0] || '/';

        const pageParams = {
            graphql: {
                content: {
                    navigation: `
                        DotNavigation(uri: "/", depth: 2) {
                            children {
                                folder
                                href
                                title
                            }
                        }
                    `
                }
            }
        };

        // Convert promise to observable and merge with editable page service
        from(
            this.client.page.get<PageResponse>(
                route,
                pageParams
            )
        )
            .pipe(
                switchMap((response) =>
                    this.editablePageService.listen<PageResponse>(response)
                )
            )
            .pipe(filter(Boolean))
            .subscribe({
                next: (
                    response: DotCMSComposedPageResponse<{
                        content: { navigation: DotCMSNavigationItem };
                    }>
                ) => {
                    this.navigation.set(response?.content?.navigation.children || []);
                    this.pageAsset.set(response?.pageAsset);
                },
                error: (error) => {
                    console.error('Error in page data stream:', error);
                }
            });
    }
}
