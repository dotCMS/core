import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { DotCMSClient } from '@dotcms/angular';
import { DotCMSNavigationItem, DotCMSPage } from '@dotcms/types';

@Component({
    selector: 'app-page',
    imports: [CommonModule],
    templateUrl: './page.component.html',
    styleUrl: './page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageComponent {
    private readonly client = inject(DotCMSClient);

    private router = inject(Router);

    currentRoute = signal(this.router.url);

    navigation = signal<DotCMSNavigationItem[]>([]);
    page = signal<DotCMSPage | null>(null);

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
                this.page.set(response.pageAsset.page);
            });
    }
}
