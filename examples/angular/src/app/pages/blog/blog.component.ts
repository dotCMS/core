import { Component, DestroyRef, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { PageService } from '../services/page.service';
import { DOTCMS_CLIENT_TOKEN } from '../../app.config';
import { startWith, switchMap } from 'rxjs/operators';
import { filter } from 'rxjs/operators';
import { DYNAMIC_COMPONENTS } from '../components';
import { tap } from 'rxjs/operators';
import { getUVEState } from '@dotcms/uve';
import { DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { postMessageToEditor } from '@dotcms/client';
import { CLIENT_ACTIONS } from '@dotcms/client';
import { HeaderComponent } from "../components/header/header.component";
import { NavigationComponent } from "../components/navigation/navigation.component";
import { LoadingComponent } from "../components/loading/loading.component";
import { ErrorComponent } from "../components/error/error.component";
import { FooterComponent } from "../components/footer/footer.component";
import { BlogPostComponent } from './post/blog-post/blog-post.component';
import { Contentlet } from '../../../../../../core-web/dist/libs/sdk/client/src/lib/client/content/shared/types';
import { Block } from '@angular/compiler';


export type PageError = {
    message: string;
    status: number | string;
};

type PageRender = {
    // TODO: Centralize the type of the page asset
    page: DotCMSPageAsset & { urlContentMap?: Contentlet<{ blogContent: Block }> } | null;
    nav: DotcmsNavigationItem | null;
    error: PageError | null;
    status: 'idle' | 'success' | 'error' | 'loading';
};

@Component({
    selector: 'app-blog',
    standalone: true,
    imports: [HeaderComponent, NavigationComponent, LoadingComponent, ErrorComponent, FooterComponent, BlogPostComponent],
    templateUrl: './blog.component.html',
    styleUrl: './blog.component.css'
})
export class BlogComponent {
    readonly #route = inject(ActivatedRoute);
    readonly #destroyRef = inject(DestroyRef);
    readonly #router = inject(Router);
    readonly #pageService = inject(PageService);
    readonly #client = inject(DOTCMS_CLIENT_TOKEN);

    protected readonly $context = signal<PageRender>({
        page: null,
        nav: null,
        error: null,
        status: 'idle'
    });
    protected readonly components = signal<any>(DYNAMIC_COMPONENTS);

    // This should be PageApiOptions from @dotcms/client
    protected readonly editorConfig: any = { params: { depth: 2 } };

    ngOnInit() {
        console.log('ngOnInit BlogComponent');
        if (getUVEState()) {
            this.#listenToEditorChanges();
        }

        this.#router.events
            .pipe(
                filter((event): event is NavigationEnd => event instanceof NavigationEnd),
                startWith(null), // Trigger initial load
                tap(() => this.#setLoading()),
                switchMap(() =>

                    this.#pageService.getPageAndNavigation(this.#route, this.editorConfig)
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ page = {}, nav, error }) => {
                if (error) {
                    this.#setError(error);
                    return;
                }

                const { vanityUrl } = page || {};

                if (vanityUrl?.permanentRedirect || vanityUrl?.temporaryRedirect) {
                    this.#router.navigate([vanityUrl.forwardTo]);
                    return;
                }   

                console.log('page', page?.urlContentMap);

                this.#setPageContent(page as DotCMSPageAsset, nav);
            });
    }

    #setPageContent(page: DotCMSPageAsset, nav: DotcmsNavigationItem | null) {
        this.$context.set({
            status: 'success',
            page: page as DotCMSPageAsset & { urlContentMap?: Contentlet<{ blogContent: Block }> },
            nav,
            error: null
        });
    }

    #setLoading() {
        this.$context.update((state) => ({
            ...state,
            status: 'loading',
            error: null
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
        postMessageToEditor({ action: CLIENT_ACTIONS.CLIENT_READY, payload: {} });
    }

    #listenToEditorChanges() {
        this.#client.editor.on('changes', (page) => {
            if (!page) {
                return;
            }
            this.$context.update((state) => ({
                ...state,
                page: page as DotCMSPageAsset & { urlContentMap?: Contentlet<{ blogContent: Block }> },
                status: 'success',
                error: null
            }));
        });
    }
}
