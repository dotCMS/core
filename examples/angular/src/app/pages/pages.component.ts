import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { NavigationEnd } from '@angular/router';
import { filter, startWith, tap } from 'rxjs/operators';

import { switchMap } from 'rxjs/operators';

import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';
import { HeaderComponent } from './components/header/header.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { FooterComponent } from './components/footer/footer.component';
import { PageService } from './services/page.service';
import { CLIENT_ACTIONS, postMessageToEditor } from '@dotcms/client';
import { getUVEState } from '@dotcms/uve';
import { DYNAMIC_COMPONENTS } from './components';
import { DotCMSEditablePageService, DotCMSLayoutBodyComponent } from '@dotcms/angular/next';
import { DotcmsNavigationItem, DotCMSPageAsset, DotCMSComposedPageResponse } from '@dotcms/types';
import { FooterContent } from '../shared/models';
import { BASE_EXTRA_QUERIES } from '../shared/constants';

type ComposedPageResponse = DotCMSComposedPageResponse<{
    pageAsset: DotCMSPageAsset;
    content: FooterContent;
}>;

export type PageError = {
    message: string;
    status: number | string;
};

type PageRender = {
    pageResponse?: ComposedPageResponse | null;
    nav?: DotcmsNavigationItem;
    error?: PageError;
    status: 'idle' | 'success' | 'error' | 'loading';
};

@Component({
    selector: 'app-dotcms-page',
    standalone: true,
    imports: [
        DotCMSLayoutBodyComponent,
        HeaderComponent,
        NavigationComponent,
        FooterComponent,
        ErrorComponent,
        LoadingComponent
    ],

    templateUrl: './pages.component.html',
    styleUrl: './pages.component.css'
})
export class DotCMSPagesComponent implements OnInit {
    readonly #route = inject(ActivatedRoute);
    readonly #destroyRef = inject(DestroyRef);
    readonly #router = inject(Router);
    readonly #pageService = inject(PageService);

    readonly #editablePageService = inject(DotCMSEditablePageService);

    protected readonly $context = signal<PageRender>({
        status: 'idle'
    });
    protected readonly components = signal<any>(DYNAMIC_COMPONENTS);

    ngOnInit() {
        this.#router.events
            .pipe(
                filter((event): event is NavigationEnd => event instanceof NavigationEnd),
                startWith(null), // Trigger initial load
                tap(() => this.#setLoading()),
                switchMap(() =>
                    this.#pageService.getPageAndNavigation<DotCMSPageAsset, FooterContent>(
                        this.#route,
                        BASE_EXTRA_QUERIES
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ response, error, nav }) => {
                if (error) {
                    this.#setError(error);
                    return;
                }

                if (getUVEState()) {
                    this.#editablePageService
                        .listen(response)
                        .pipe(takeUntilDestroyed(this.#destroyRef))
                        .subscribe((page) => {
                            this.#updatePageContent(page as ComposedPageResponse);
                        });
                }

                this.#setPageContent(response, nav);
            });
    }

    #updatePageContent(page?: ComposedPageResponse | null) {
        this.$context.update((state) => ({
            ...state,
            pageResponse: page
        }));
    }

    #setPageContent(page?: ComposedPageResponse, nav?: DotcmsNavigationItem) {
        this.$context.set({
            pageResponse: page,
            nav,
            status: 'success',
            error: undefined
        });
    }

    #setLoading() {
        this.$context.update((state) => ({
            ...state,
            status: 'loading',
            error: undefined
        }));
    }

    #setError(error: PageError) {
        this.$context.update((state) => ({
            ...state,
            error: error,
            status: 'error'
        }));

        /**
         * Send a message to the editor to let it know that the client is ready.
         * This is a temporary workaround to avoid the editor to be stuck in the loading state.
         * This will be removed once the editor is able to detect when the client is ready without use DotcmsLayoutComponent.
         */

        // REMIND ME TO REVISIT THIS
        postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY, payload: {} });
    }
}
