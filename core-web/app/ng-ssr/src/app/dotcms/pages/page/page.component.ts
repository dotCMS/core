
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { DotCMSClient, DotCMSLayoutBodyComponent, DynamicComponentEntity } from '@dotcms/angular';
import { DotCMSNavigationItem, DotCMSPageAsset } from '@dotcms/types';

export const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
    Activity: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    Banner: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    Image: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    webPageContent: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    Product: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    BannerCarousel: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    VtlInclude: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    CategoryFilter: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    StoreProductList: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    SimpleWidget: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    ),
    PageForm: import('../../components/contentlet/contentlet.component').then(
        (c) => c.ContentletComponent
    )
};

@Component({
    selector: 'app-page',
    imports: [CommonModule, DotCMSLayoutBodyComponent],
    templateUrl: './page.component.html',
    styleUrl: './page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageComponent {
    private readonly client = inject(DotCMSClient);

    private router = inject(Router);

    currentRoute = signal(this.router.url);

    navigation = signal<DotCMSNavigationItem[]>([]);
    pageAsset = signal<DotCMSPageAsset | null>(null);

    components = signal<{ [key: string]: DynamicComponentEntity }>(DYNAMIC_COMPONENTS);

    ngOnInit() {
        const route = this.currentRoute();

        this.client.page
            .get<{ content: { navigation: DotCMSNavigationItem } }>(route, {
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
            })
            .then((response) => {
                this.navigation.set(response.content?.navigation.children || []);
                this.pageAsset.set(response.pageAsset);
            });
    }
}
