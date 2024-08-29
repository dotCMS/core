import {
  Component,
  DestroyRef,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { NavigationEnd } from '@angular/router';
import { filter, startWith, tap } from 'rxjs/operators';

import { DYNAMIC_COMPONENTS } from '../utils';

import {
  DotcmsLayoutComponent,
  DotcmsNavigationItem,
  DotCMSPageAsset,
} from '@dotcms/angular';
import { JsonPipe } from '@angular/common';
import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';
import { map, switchMap } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { from } from 'rxjs';
import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';
import { HeaderComponent } from './components/header/header.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { FooterComponent } from './components/footer/footer.component';

export type PageError = {
  message: string;
  status: number;
};

type PageRender = {
  page: DotCMSPageAsset | null;
  nav: DotcmsNavigationItem | null;
  error: PageError | null;
  status: 'idle' | 'success' | 'error' | 'loading';
};

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [
    DotcmsLayoutComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
    JsonPipe,
    ErrorComponent,
    LoadingComponent,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  protected readonly context = signal<PageRender>({
    page: null,
    nav: null,
    error: null,
    status: 'idle',
  });
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  protected readonly editorCofig = { params: { depth: '2' } };
  protected slug: string | null = null;

  ngOnInit() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        startWith(null), // Trigger initial load
        tap(() => {
          this.context.update((state) => ({ ...state, status: 'loading' }));
        }),
        switchMap(() => {
          const queryParams = this.route.snapshot.queryParamMap;
          const url = this.route.snapshot.url.map((segment) => segment.path).join('/')
          const path = queryParams.get('path') || url || '/';

          const pageParams = getPageRequestParams({
            path,
            params: queryParams,
          });
          const pagePromise = this.client.page
            .get(pageParams)
            .catch((error) => ({
              error: {
                message: error.message,
                status: error.status,
              },
            })) as Promise<DotCMSPageAsset | { error: PageError }>;

          const navParams = {
            path: '/',
            depth: 2,
            languageId: parseInt(queryParams.get('languageId') || '1'),
          };
          const navPromise = this.client.nav
            .get(navParams)
            .then((response) => (response as any).entity)
            .catch((error) => null) as Promise<DotcmsNavigationItem | null>;

          return from(Promise.all([pagePromise, navPromise]));
        }),
        map(([page, navResponse]) => ({
          page,
          nav: navResponse,
        })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(
        ({
          page,
          nav,
        }: {
          page: DotCMSPageAsset | { error: PageError };
          nav: DotcmsNavigationItem | null;
        }) => {
          if ('error' in page) {
            this.context.update((state) => ({
              page: null,
              nav: null,
              error: page.error,
              status: 'error',
            }));
          } else {
            this.context.update((state) => ({
              error: null,
              page,
              nav,
              status: 'success',
            }));
          }
        }
      );
  }

  ngOnDestroy() {
    // this.client.editor.off('changes');
  }
}
