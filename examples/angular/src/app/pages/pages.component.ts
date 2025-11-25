import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { NavigationEnd } from '@angular/router';
import { filter, startWith, tap } from 'rxjs/operators';

import {
  DotcmsLayoutComponent,
  DotcmsNavigationItem,
  DotCMSPageAsset,
} from '@dotcms/angular';
import { switchMap } from 'rxjs/operators';

import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';
import { HeaderComponent } from './components/header/header.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { FooterComponent } from './components/footer/footer.component';
import { PageService } from './services/page.service';
import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';
import { DYNAMIC_COMPONENTS } from './components';

export type PageError = {
  message: string;
  status: number | string;
};

type PageRender = {
  page: DotCMSPageAsset | null;
  nav: DotcmsNavigationItem | null;
  error: PageError | null;
  status: 'idle' | 'success' | 'error' | 'loading';
};


@Component({
  selector: 'app-dotcms-page',
  standalone: true,
  imports: [
    DotcmsLayoutComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
    ErrorComponent,
    LoadingComponent,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit {
  readonly #route = inject(ActivatedRoute);
  readonly #destroyRef = inject(DestroyRef);
  readonly #router = inject(Router);
  readonly #pageService = inject(PageService);
  protected readonly $context = signal<PageRender>({
    page: null,
    nav: null,
    error: null,
    status: 'idle',
  });
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);

  // This should be PageApiOptions from @dotcms/client
  protected readonly editorCofig: any = { params: { depth: 2 } };

  ngOnInit() {
    this.#router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        startWith(null), // Trigger initial load
        tap(() => this.#setLoading()),
        switchMap(() => this.#pageService.getPageAndNavigation(this.#route, this.editorCofig)),
        takeUntilDestroyed(this.#destroyRef)
      )
      .subscribe(
        ({ page, nav }: {
          page: DotCMSPageAsset | { error: PageError };
          nav: DotcmsNavigationItem | null;
        }) => {
          if ('error' in page) {
            this.#setError(page.error);
          } else {
            const { vanityUrl } = page;

            if (vanityUrl?.permanentRedirect || vanityUrl?.temporaryRedirect) {
              this.#router.navigate([vanityUrl.forwardTo]);
              return;
            }

            this.#setPageContent(page, nav);
          }
        }
      );
  }

  #setPageContent(page: DotCMSPageAsset, nav: DotcmsNavigationItem | null) {
    this.$context.update((state) => ({
      status: 'success',
      page,
      nav,
      error: null,
    }));
  }

  #setLoading() {
    this.$context.update((state) => ({
      ...state,
      status: 'loading',
      error: null,
    }));
  }

  #setError(error: PageError) {
    this.$context.update((state) => ({
      ...state,
      error: error,
      status: 'error',
    }));

    /**
     * Send a message to the editor to let it know that the client is ready.
     * This is a temporary workaround to avoid the editor to be stuck in the loading state.
     * This will be removed once the editor is able to detect when the client is ready without use DotcmsLayoutComponent.
     */
    postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: {} });
  }
}
